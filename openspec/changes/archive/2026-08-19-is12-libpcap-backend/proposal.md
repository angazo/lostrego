## Why

El change `is5-core-api` dejó fijado el contrato público de captura (`core`) y el SPI (`CaptureProvider`), pero ningún backend real lo implementa: hoy no es posible capturar un solo paquete de red con la librería. libpcap es el primer backend de la Fase 1 y el más universal (Linux y macOS), por lo que sirve a la vez como entrega funcional y como validación extremo a extremo del enfoque FFM (Foreign Function & Memory) sobre una librería nativa real.

## What Changes

- Nuevo paquete `com.angazo.lostrego.backend.libpcap` (interno, no exportado) con:
  - `LibpcapProvider` — implementación de `CaptureProvider` (nombre `"libpcap"`, `isSupported()` y `openLive()`).
  - `LibpcapCapture` — implementación de `AbstractPacketCapture` con los hooks de captura, parada, cierre y estadísticas.
  - Bindings FFM sobre libpcap (`LibpcapNative`): carga de la librería nativa, `MethodHandle`s para las funciones necesarias y layouts de las estructuras (`pcap_pkthdr`, `timeval`, `pcap_stat`, `bpf_program`).
- Traducción de `struct pcap_pkthdr`/`struct timeval` al modelo `core` (`CaptureTimestamp` con microsegundos→nanosegundos, `originalLength`/`capturedLength`, `LinkType` vía `pcap_datalink`), copiando el payload a `byte[]` antes de entregarlo.
- Aplicación de la configuración de `CaptureConfig`: `device`, `promiscuous`, `snaplen`, `timeoutMillis`, `bufferSize` (si ≠ 0), `immediateMode` y `filter` (filtro BPF compilado con `pcap_compile`/`pcap_setfilter`).
- `module-info.java`: `provides CaptureProvider with LibpcapProvider`.
- Tests condicionales del backend: parseo reproducible de un archivo `.pcap` de ejemplo (`pcap_open_offline`, usado solo para tests), y una captura viva sobre loopback protegida por condiciones JUnit de plataforma, presencia de librería y privilegios (root/CAP_NET_RAW).

## Capabilities

### New Capabilities

- `backend/libpcap`: backend de captura sobre libpcap mediante FFM — disponibilidad de la librería nativa, apertura de captura en vivo aplicando la configuración, entrega de paquetes traducidos al modelo `core`, parada y liberación de recursos, y estadísticas.

### Modified Capabilities

Ninguna. El contrato público de `packet-capture` no cambia: este backend lo implementa sin alterar sus requisitos.

## Impact

- `src/lostrego/src/main/java/com/angazo/lostrego/backend/libpcap/`: paquete nuevo con el provider, la captura y los bindings FFM.
- `src/lostrego/src/main/java/module-info.java`: añade `provides com.angazo.lostrego.core.spi.CaptureProvider with com.angazo.lostrego.backend.libpcap.LibpcapProvider`.
- `src/lostrego/src/test/`: nuevos tests condicionales del backend + archivo `.pcap` de ejemplo en `src/test/resources/`.
- Sin dependencias externas nuevas: solo JDK (FFM API) y libpcap, ya instalada en CI Linux (`libpcap-dev`).
- Sin cambios en CI: los tests de backend se saltan en Windows (sin libpcap) y en entornos sin privilegios.
