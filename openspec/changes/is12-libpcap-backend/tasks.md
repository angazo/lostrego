## 1. Bindings FFM (LibpcapNative)

- [x] 1.1 Crear `LibpcapNative` con carga de la librería nativa (`SymbolLookup.libraryLookup` con fallback de nombres) y flag `isAvailable()`
- [x] 1.2 Declarar `MethodHandle`s para `pcap_open_live`, `pcap_next_ex`, `pcap_close`, `pcap_breakloop`, `pcap_stats`, `pcap_datalink`, `pcap_set_buffer_size`, `pcap_set_immediate_mode`, `pcap_compile`, `pcap_setfilter`, `pcap_freecode`, `pcap_geterr` y `pcap_lib_version`
- [x] 1.3 Definir `StructLayout` de `pcap_pkthdr` y `timeval` con rama por SO (Linux vs macOS)
- [x] 1.4 Definir `StructLayout` de `pcap_stat` y `bpf_program` + constantes (PCAP_ERRBUF_SIZE, PCAP_NETMASK_UNKNOWN, códigos de retorno de `pcap_next_ex`)
- [x] 1.5 Añadir `MethodHandle` de `pcap_open_offline` (solo para tests)

## 2. Implementación del backend

- [x] 2.1 Crear `LibpcapCapture` (extends `AbstractPacketCapture`) con arena de sesión `Arena.ofShared` y apertura del handle en `openLive`
- [x] 2.2 Implementar `doStart()` con bucle `pcap_next_ex` que distingue retorno `1`/`0`/`-1`/`-2` y entrega paquetes vía `deliver(...)`
- [x] 2.3 Implementar la traducción del header a `Packet` (timestamp usec→nanos, caplen/len, `LinkType.of`, payload a `byte[]`)
- [x] 2.4 Implementar `doStop()` con `pcap_breakloop` y `doClose()` con `pcap_close` + cierre del arena (idempotente)
- [x] 2.5 Implementar `doStatistics()` con `pcap_stats` (contadores a cero si el handle está cerrado)
- [x] 2.6 Aplicar `bufferSize`/`immediateMode` y compilar/instalar/liberar el filtro BPF (`pcap_compile`/`pcap_setfilter`/`pcap_freecode`)
- [x] 2.7 Traducir errores nativos (`errbuf`/`pcap_geterr`) a `CaptureException` con mensaje informativo
- [x] 2.8 Crear `LibpcapProvider` (`name()` = `"libpcap"`, `isSupported()`, `openLive()`)

## 3. Registro en el módulo

- [x] 3.1 Añadir `provides ... CaptureProvider with ... LibpcapProvider` en `module-info.java`

## 4. Tests del backend

- [x] 4.1 Generar/incorporar un archivo `.pcap` de ejemplo en `src/test/resources/`
- [x] 4.2 Test de disponibilidad (`isSupported()` no lanza y es coherente con la presencia de la librería)
- [x] 4.3 Test de parseo offline: `pcap_open_offline` sobre el `.pcap` verifica timestamp/caplen/len/linkType/payload
- [x] 4.4 Test de captura viva en loopback con socket UDP, protegido por `@EnabledOnOs` + `Assumptions` de librería y privilegios
- [x] 4.5 Test de ciclo de vida del backend (start/stop/close, estadísticas) reutilizando el `.pcap` offline
- [x] 4.6 `./gradlew build` en verde desde `src/` (los tests nativos se saltan donde no aplican)
