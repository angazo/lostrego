## Context

`core` ya define el contrato SPI que debe implementar cada backend (`CaptureProvider`) y la máquina de estados común (`AbstractPacketCapture` con hooks `doStart`/`doStop`/`doClose`/`doStatistics`). El módulo JPMS único `com.angazo.lostrego` exporta solo `core` y declara `uses CaptureProvider`; falta el primer `provides`. Todo el código nativo debe canalizarse por la FFM API (`java.lang.foreign.*`) sin JNI/JNA, y ningún `MemorySegment` puede salir del paquete de backend. Ver `proposal.md` para la motivación y `specs/backend/libpcap/spec.md` para el contrato de comportamiento.

## Goals / Non-Goals

**Goals:**
- Implementar el backend libpcap completo (provider + captura + bindings FFM) honrando todas las opciones de `CaptureConfig`.
- Traducción correcta de `struct pcap_pkthdr`/`struct timeval` al modelo `core`, con payload copiado a `byte[]`.
- Parada oportuna de la captura sin esperar el timeout completo de lectura.
- Tests reproducibles (archivo `.pcap`) y una captura viva protegida por condiciones de plataforma/privilegios.

**Non-Goals:**
- Enumeración de dispositivos (`pcap_findalldevs`): issue de backlog aparte.
- Captura offline como API pública (`openOffline`): `pcap_open_offline` se vincula solo para los tests.
- Escritura de capturas (`pcap_dump`), modo burst (`pcap_dispatch`), o `pcap_next`/captura multihilo.
- macOS en CI: sigue pendiente (backlog); el diseño es portable pero solo se prueba en Linux por ahora.

## Decisions

### D1: Estructura del paquete `backend.libpcap`

Tres piezas:
- `LibpcapProvider` (pública, no exportada): implementa `CaptureProvider`. `name()` → `"libpcap"`; `isSupported()` delega en la disponibilidad de la librería; `openLive()` construye un `LibpcapCapture`.
- `LibpcapCapture` (package-private): extiende `AbstractPacketCapture` e implementa los hooks.
- `LibpcapNative` (package-private): posee la carga de la librería, los `MethodHandle`s y los `StructLayout`s. Es el único punto donde se toca `java.lang.foreign`.

**Alternativas consideradas:**
- Todo en una sola clase: mezcla provider, ciclo de vida y bindings; menos mantenible. Descartado.
- Exponer `LibpcapNative` como API: violaría la transparencia de backend. Descartado.

### D2: Carga de la librería y detección de disponibilidad

Carga con `SymbolLookup.libraryLookup` probando varios nombres en orden (`pcap`, `libpcap.so`, `libpcap.dylib`) usando `System.mapLibraryName("pcap")` como base; en Linux la resolución habitual es `libpcap.so.1`. `isSupported()` devuelve `true` solo si la carga y la búsqueda de los símbolos necesarios tienen éxito, y `false` (sin lanzar) en caso contrario, cumpliendo así que un backend ausente no impida a los demás.

**Alternativas consideradas:**
- `System.loadLibrary("pcap")`: lanza `UnsatisfiedLinkError` si falta, obligando a `try/catch` en el inicializador estático; menos control sobre los nombres de fichero. Rechazado en favor de `SymbolLookup`, que además encaja con la regla de AGENTS.md.

### D3: API de apertura (`pcap_open_live` vs `pcap_create`/`pcap_activate`)

Se usa `pcap_open_live(device, snaplen, promisc, to_ms, errbuf)` como vía principal (cubre device/snaplen/promiscuo/timeout en una llamada) y, después de abrir, `pcap_set_buffer_size` y `pcap_set_immediate_mode` para las opciones avanzadas. Ambas funciones son aplicables a un handle ya activado en Linux, que es el único SO donde se prueba hoy.

**Alternativas consideradas:**
- `pcap_create` + setters + `pcap_activate`: API "moderna" más verbosa (5-6 bindings extra) sin beneficio observable para este contrato. Rechazado por complejidad; se puede migrar si macOS lo exigiera.

### D4: Layouts de estructuras y portabilidad de `timeval`

Se definen `StructLayout`s para `pcap_pkthdr`, `pcap_stat` y `bpf_program`. `struct pcap_pkthdr` = `{ timeval ts; uint32 caplen; uint32 len; }`; `struct timeval` difiere por plataforma: en Linux x86_64 ambos campos son `long` (8+8 bytes); en macOS `tv_sec` es `long` (8) y `tv_usec` es `int` (4, con padding). Se selecciona el layout de `timeval` según el sistema operativo (`os.name`/`os.arch`), y el layout de `pcap_pkthdr` se compone en consecuencia. `caplen`/`len` son `uint32` en ambas plataformas.

**Alternativas consideradas:**
- Leer `timeval` byte a byte: frágil y poco legible. Descartado.
- Usar solo `C_LONG`/`C_INT` sin rama: rompería macOS (leería bytes de padding como parte de `tv_usec`). Descartado.

### D5: Estrategia de arenas y ownership

Se crea **un** `Arena.ofShared()` por sesión en `openLive`, cerrado en `doClose()`. Se elige compartida (no confinada) porque `openLive`, el hilo de captura y `close()` pueden ejecutarse en hilos distintos. El arena aloja: el buffer de error (`errbuf`, 256 bytes) durante la apertura, el `bpf_program` durante el filtrado, los dos slots `ADDRESS` usados como punteros de salida de `pcap_next_ex` y el `pcap_stat` de las estadísticas. Nada de esto cruza la frontera del paquete: el payload y el header se leen y se copian a tipos Java en cada iteración, y el puntero `pcap_t*` se trata como valor opaco.

**Alternativas consideradas:**
- `Arena.ofConfined()`: lanza `WrongThreadException` si `close()` ocurre en otro hilo. Rechazado.
- Varios arenas de vida corta: el bucle reutiliza los slots de salida, así que conviene un solo arena de sesión. Rechazado por innecesario.

### D6: Conversión de timestamp y payload

`tv_usec` (microsegundos) se convierte a nanosegundos multiplicando por 1000 en `CaptureTimestamp(tv_sec, tv_usec * 1000)`. `caplen` → `capturedLength`, `len` → `originalLength`, `pcap_datalink()` → `LinkType.of(code)`, y el payload se copia a `byte[]` vía `MemorySegment.copy` a un array Java antes de llamar a `deliver(...)`. El `Packet` resultante es autónomo e inmutable.

### D7: Bucle de captura y parada (`pcap_next_ex` + `pcap_breakloop`)

El bucle de `doStart()` llama a `pcap_next_ex` y distingue sus códigos de retorno: `1` entrega paquete; `0` (timeout) comprueba `isStopRequested()` y continúa; `-2` (breakloop/EOF) termina limpiamente; `-1` lanza `CaptureException` con `pcap_geterr()`. `doStop()` invoca `pcap_breakloop(handle)`, de modo que una parada no espere el timeout de lectura completo (con el timeout por defecto de 1 s el bucle despierta igualmente, pero `breakloop` hace la parada inmediata).

### D8: Informe de errores nativos

Los fallos de apertura se leen del `errbuf`; los fallos de bucle/estadísticas de `pcap_geterr(handle)`. En ambos casos el mensaje nativo se envuelve en `CaptureException`, que la máquina de estados de `AbstractPacketCapture` relanza desde `stop()`/`close()` cuando corresponde.

### D9: Filtro BPF (`pcap_compile` + `pcap_setfilter`)

Si `config.filter()` no es nulo, se compila con `pcap_compile(handle, &prog, filter, /*optimize*/ 1, /*netmask*/ PCAP_NETMASK_UNKNOWN)` y se instala con `pcap_setfilter`. La netmask se fija a `0xffffffff` para evitar `pcap_lookupnet`. Tras `pcap_setfilter`, `pcap_freecode(&prog)` libera la memoria del programa; todo el ciclo compile→install→free queda contenido en `openLive`. Un filtro inválido produce error en `pcap_compile` y se traduce a `CaptureException`.

### D10: Estrategia de tests

- **Parseo determinista (sin privilegios):** `pcap_open_offline` (vinculado solo para tests) lee un `.pcap` de `src/test/resources/` y verifica la traducción de timestamp/caplen/len/linkType/payload. No requiere root.
- **Captura viva:** un test abre `lo` y, en paralelo, un socket UDP envía un datagrama a `127.0.0.1`; se verifica que llega un paquete. Se protege con condiciones JUnit: `@EnabledOnOs({LINUX, MAC})`, presencia de librería y privilegios (root o `CAP_NET_RAW`), usando `Assumptions.assumeTrue` para saltar silenciosamente.
- **Disponibilidad:** test de que `isSupported()` no lanza y es coherente con la presencia de la librería.

### D11: Registro en el módulo

`module-info.java` añade `provides com.angazo.lostrego.core.spi.CaptureProvider with com.angazo.lostrego.backend.libpcap.LibpcapProvider`. La factoría `PacketCaptures` descubrirá el backend vía `ServiceLoader` sin cambios en `core`.

## Risks / Trade-offs

- **[Riesgo] La captura viva requiere privilegios (root/CAP_NET_RAW)** → Mitigación: el test se salta con `Assumptions` si no hay privilegios; el parseo offline garantiza cobertura reproducible en CI sin privilegios.
- **[Riesgo] `pcap_breakloop` tiene semántica dependiente de plataforma** (puede no interrumpir una `pcap_next_ex` ya bloqueada en algunos sistemas) → Mitigación: el timeout de lectura acota el tiempo máximo de parada; en Linux se verifica que `breakloop` acorta la espera.
- **[Riesgo] macOS no se prueba en CI** (pendiente de backlog) → Mitigación: el layout de `timeval` ya está ramificado por SO, pero se marca como verificación futura.
- **[Riesgo] `statistics()` sobre una sesión cerrada apuntaría a un handle liberado** → Mitigación: `LibpcapCapture` guarda el estado de cierre y devuelve contadores a cero (o lanza `IllegalStateException`) si el handle ya no es válido.
- **[Trade-off] `pcap_set_buffer_size`/`pcap_set_immediate_mode` se aplican tras activar** (no antes, como recomienda la API moderna) → Aceptado por simplicidad; ambas son compatibles con handles activados en Linux.

## Migration Plan

No aplica: no hay código existente que migrar; se añade un paquete nuevo y una cláusula `provides` en `module-info.java`.

## Open Questions

Ninguna que afecte al contrato, al enfoque o al desglose de tareas.
