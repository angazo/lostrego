## Context

El módulo `com.angazo.lostrego` es un placeholder (`module-info.java` vacío) creado en el change `is1-gradle-ci`. No hay código de librería todavía. Este change define únicamente el API pública de `core` y el contrato SPI, sin tocar código nativo. Ver `proposal.md` para la motivación y `specs/packet-capture/spec.md` para el contrato de comportamiento.

## Goals / Non-Goals

**Goals:**
- Un API pública que un backend (libpcap, pdpk, npcap) pueda implementar sin que el usuario perciba la diferencia.
- Un modelo de paquete inmutable, crudo y autónomo (sin memoria nativa expuesta).
- Un ciclo de vida de captura asíncrono por callback, seguro y re-arrancable.
- Selección de backend desacoplada de `core` (SPI + `ServiceLoader`).

**Non-Goals:**
- Implementar ningún backend nativo (libpcap queda para el siguiente change).
- Parseo de protocolos (Ethernet/IP/TCP/…): capa superior futura.
- Captura offline (`openOffline` desde `.pcap`): futuro.
- Enumeración de dispositivos (`listDevices`): futuro, depende del backend.

## Decisions

### D1: API pública en `core`, SPI interno en `core.spi` (no exportado)

El contrato público vive en `com.angazo.lostrego.core`. El SPI (`CaptureProvider`) vive en `com.angazo.lostrego.core.spi` y **no** se exporta en `module-info.java`: es un contrato interno entre `core` y los backends, que residen en el mismo módulo y por tanto pueden implementarlo sin necesidad de exportarlo.

**Alternativas consideradas:**
- Exportar `core.spi`: expondría un contrato de implementación a los usuarios finales, ensuciando la superficie pública. Descartado.
- SPI como paquete `backend.spi`: mezcla la responsabilidad de `core` (definir el contrato) con la de los backends. Descartado.

### D2: Ciclo de vida asíncrono por callback (`start`/`stop`/`close`)

`start(listener)` lanza un hilo interno de captura y retorna inmediatamente; `stop()` detiene y espera (join); `close()` fuerza `stop()` y libera recursos, de forma idempotente. El usuario no bloquea nunca su hilo.

**Alternativas consideradas:**
- Modelo síncrono `loop(listener)` (estilo libpcap): bloquea el hilo del usuario, menos idiomático en Java. Descartado como primer API (podría añadirse luego como variante).
- Modelo pull (`nextPacket()`): cómodo pero no encaja con el *push* natural de la NIC y con el patrón de callback de libpcap/DPDK. Descartado para este change.

### D3: `AbstractPacketCapture` en `core.spi` con la máquina de estados común

Para que cada backend no reinvente el ciclo de vida, `core.spi` provee `AbstractPacketCapture` (implementa `PacketCapture`) con la máquina de estados `READY → RUNNING → STOPPED/FAILED → CLOSED`, la coordinación `start`/`stop`/`close` y la lógica de capturar excepciones del listener. Los backends solo implementan *hooks* (`doStart`, `doStop`, `doClose`, `doStatistics`).

**Alternativas consideradas:**
- Dejar el ciclo de vida a cada backend: duplica lógica sutil (estados, concurrencia, idempotencia de `close`) en tres sitios. Descartado.
- Interface con métodos por defecto: no permite estado compartido. Descartado.

### D4: Backpressure directo (callback inline)

El listener se invoca directamente en el hilo de captura, sin cola intermedia. Un listener lento provoca descarte en el buffer del kernel/NIC. Es la semántica natural de libpcap y DPDK, con mínima latencia y cero allocations extra.

**Alternativas consideradas:**
- Cola intermedia productor/consumidor: desacopla pero añade allocations y latencia, contrario al objetivo de altas prestaciones. Descartado.

### D5: `Packet` inmutable con payload copiado (`byte[]`)

`Packet` es un `record` con `CaptureTimestamp`, `originalLength`, `capturedLength`, `LinkType` y `byte[] payload`. El payload se copia a `byte[]` antes de cruzar la frontera del backend; nunca se expone un `MemorySegment` (cumple AGENTS.md). El constructor compacto clona el array de entrada.

**Alternativas consideradas:**
- Exponer `MemorySegment`/`ByteBuffer` nativo: viola la regla de "no exponer segments fuera del backend" y complica el ownership de arenas. Descartado.
- `ByteBuffer` read-only sobre el array: inmutabilidad estricta con coste cero, pero añade una indirección; se prefiere `byte[]` por simplicidad. Rechazado.

**Trade-off de inmutabilidad del `payload()`:** el accessor devuelve el array interno. El constructor clona la entrada (nadie externo puede mutar el array tras construirlo), pero un llamador podría mutar el array devuelto. Se documenta que el array es propiedad del `Packet` y no debe mutarse. Clonar en cada acceso costaría O(n) por paquete, inaceptable para el objetivo de prestaciones.

### D6: `CaptureTimestamp` como `record(seconds, nanosOfSecond)` + `toInstant()`

Se representa el timestamp con dos campos, mapeando 1:1 las fuentes nativas: `struct timeval` (libpcap/npcap: `sec` + `usec×1000`) y el nanosegundo del mbuf de DPDK/PDPK. El constructor compacto normaliza `nanosOfSecond` a `[0, 999_999_999]`. `toInstant()` es una conveniencia opcional.

**Alternativas consideradas:**
- `Instant` directamente: implicaría semántica de *wall-clock* (época Unix) que los relojes de NIC de DPDK no garantizan. Descartado como representación primaria.
- `long` de nanosegundos desde época: pierde legibilidad y no distingue resolución real. Descartado.

### D7: `LinkType` como tipo valor (estilo Pcap4J) que conserva el código DLT

`LinkType` es una clase final inmutable con constantes para los DLTs comunes (ETHERNET, LINUX_SLL, IPV4, …) y un método estático `of(int code)` que devuelve la constante conocida o, si el código no está previsto, una instancia que conserva el código crudo. `isKnown()` y `name()` permiten distinguir e inspeccionar ambos casos; dos instancias con el mismo código son iguales.

**Alternativas consideradas:**
- Enum: un enum no puede portar un código arbitrario por instancia, por lo que un DLT no previsto perdería su identificador original. Descartado.
- Solo `int` crudo: menos legible, sin tipado seguro ni nombres. Descartado.

### D8: `CaptureStatistics` con tres contadores

`record CaptureStatistics(long received, long dropped, long interfaceDropped)`, espejo de `pcap_stats`. PDPK/DPDK exponen métricas adicionales; se ampliarán en un change futuro sin romper este contrato.

### D9: `CaptureConfig` inmutable con builder

`CaptureConfig` es una clase final inmutable construida con un builder que aplica valores por defecto a todas las opciones (device, promiscuous, snaplen, timeout, bufferSize, filter, immediateMode). El builder valida los campos (p. ej. snaplen > 0).

**Alternativas consideradas:**
- `record` con 7 campos: poco ergonómico y sin defaults claros. Descartado.
- Constructor telescópico: ilegible. Descartado.

### D10: `CaptureException` unchecked

`CaptureException extends RuntimeException`, única excepción pública de la librería. Se elige unchecked porque el listener (`onPacket`) no puede declarar excepciones comprobadas sin perjudicar la ergonomía de la lambda, y porque los fallos de captura en un hilo interno se propagan de forma asíncrona.

**Alternativas consideradas:**
- Checked exception: obligaría a `try/catch` en todo uso y no encaja con callbacks funcionales. Descartado.

### D11: Política de error: excepción → detener y relanzar en `stop()`/`close()`

Si el listener lanza, o la captura nativa falla, `AbstractPacketCapture` marca la sesión como `FAILED`, detiene el bucle y guarda la causa. La causa se relanza envuelta en `CaptureException` desde `stop()` o `close()`, de modo que no se pierda y encaje con `try-with-resources`.

**Alternativas consideradas:**
- Loggear y continuar: si la captura rompe no tiene sentido seguir. Descartado.
- Silenciar: pierde información de fallo. Descartado.

### D12: `stop()` bloqueante con timeout de join

`stop()` solicita detener el bucle y espera a que el hilo interno termine (join con timeout razonable). Apagado determinista y fácil de testear.

### D13: Selección de backend con `ServiceLoader` + `provides`/`uses`

`PacketCaptures` descubre los proveedores con `ServiceLoader.load(CaptureProvider.class)`. Al ser un único módulo JPMS, los proveedores se declaran con `provides`/`uses` en `module-info.java` (no `META-INF/services`). `isSupported()` hace de puerta: un backend con librería nativa ausente devuelve `false` y no impide a los demás.

**Alternativas consideradas:**
- Lista fija en la factoría (`List.of(new LibpcapProvider(), …)`): más simple pero acopla `core` a conocer los backends. Rechazado en favor del desacople.

## Risks / Trade-offs

- **[Riesgo] No hay ningún backend todavía** → no se puede probar la captura real; los tests se apoyan en un `CaptureProvider` falso en memoria. Mitigación: el SPI y la máquina de estados se prueban de forma aislada; el primer backend real (libpcap) validará el contrato de extremo a extremo.
- **[Riesgo] El contrato del SPI podría no ajustarse a las peculiaridades de algún backend** (p. ej. PDPK multi-cola, *bursts*) → Mitigación: el SPI se mantiene mínimo (`openLive`); las extensiones específicas (bursts, colas) se añadirán en un change futuro sin romper el API pública.
- **[Trade-off] `payload()` devuelve el array interno** → la inmutabilidad es por convención, no estricta. Aceptado a cambio de no clonar por paquete (prestaciones).
- **[Trade-off] Callback en hilo interno** → el usuario debe ser consciente de la concurrencia si su listener accede a estado compartido. Se documenta.

## Migration Plan

No aplica: no hay código existente que migrar; este es el primer código de comportamiento de la librería.

## Open Questions

Ninguna que afecte al contrato, al enfoque o al desglose de tareas.
