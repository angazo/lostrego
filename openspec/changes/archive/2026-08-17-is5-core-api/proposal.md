## Why

El proyecto tiene un esqueleto Gradle funcional (change `is1-gradle-ci`) pero ninguna línea de código de librería. Antes de implementar ningún backend nativo (libpcap, pdpk, npcap) necesitamos fijar la API pública de captura: un contrato en `core` que sea totalmente transparente a los backends y que no dependa de ninguna librería nativa. Sin este contrato, cada backend inventaría su propia forma de entregar paquetes y el objetivo de "un único API, varios backends" sería inviable.

## What Changes

- Nuevo paquete `com.angazo.lostrego.core` con la API pública:
  - `PacketCapture` — interfaz de sesión de captura (ciclo de vida asíncrono por callback).
  - `PacketListener` — interfaz funcional que recibe cada paquete.
  - `Packet` — modelo de paquete inmutable, crudo y neutral respecto al backend.
  - `CaptureTimestamp` — timestamp de captura en `(segundos, nanosegundos)`.
  - `LinkType` — tipo de capa de enlace (tipo valor con código DLT, preservando códigos no reconocidos).
  - `CaptureStatistics` — contadores recibidos/descartados/descartados-por-interfaz.
  - `CaptureConfig` — configuración de captura en vivo mediante builder.
  - `CaptureException` — excepción unificada de la librería.
  - `PacketCaptures` — factoría de entrada que resuelve el backend en runtime.
- Nuevo paquete interno `com.angazo.lostrego.core.spi` con `CaptureProvider` (contrato SPI que implementará cada backend). Este paquete **no** se exporta.
- `module-info.java`: `exports` de `core`, declaración `uses`/`provides` de `CaptureProvider`.
- Tests unitarios de `core` (Java puro, sin dependencias nativas) usando un `CaptureProvider` falso en memoria.

## Capabilities

### New Capabilities

- `packet-capture`: API pública de captura de paquetes, independiente de backend — ciclo de vida de la sesión de captura (callback asíncrono), modelo de paquete inmutable y crudo, y selección de backend en runtime vía SPI (`CaptureProvider`) y `ServiceLoader`.

### Modified Capabilities

Ninguna. No existe aún baseline de specs; esta es la primera capacidad de comportamiento de la librería.

## Impact

- `src/lostrego/src/main/java/module-info.java`: pasa de placeholder a declarar `exports com.angazo.lostrego.core`, `uses` y `provides` de `com.angazo.lostrego.core.spi.CaptureProvider`.
- `src/lostrego/src/main/java/com/angazo/lostrego/core/` y `core/spi/`: nuevos paquetes con los tipos públicos y el contrato SPI.
- `src/lostrego/src/test/`: nuevos tests unitarios de `core` (ninguna dependencia nativa; se ejecutan en cualquier plataforma).
- Sin dependencias externas nuevas (solo JDK; JUnit 5 ya está en el catálogo para los tests).
- Sin cambios en CI: los tests son Java puro y corren en la matriz Linux + Windows existente.
- No se implementa ningún backend nativo en este change (libpcap queda para el siguiente).
