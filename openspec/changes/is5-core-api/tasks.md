## 1. Modelo de paquete (core)

- [x] 1.1 Crear `CaptureTimestamp` (record con `seconds`/`nanosOfSecond`, constructor compacto que normaliza, y `toInstant()`)
- [x] 1.2 Crear `LinkType` (enum de DLTs comunes + `UNKNOWN` portador del código crudo)
- [x] 1.3 Crear `CaptureStatistics` (record `received`/`dropped`/`interfaceDropped`)
- [x] 1.4 Crear `Packet` (record inmutable con `CaptureTimestamp`, `originalLength`, `capturedLength`, `LinkType`, `byte[] payload` con clonado defensivo en el constructor)

## 2. API de captura (core)

- [x] 2.1 Crear `PacketListener` (interfaz funcional `void onPacket(Packet)`)
- [x] 2.2 Crear `CaptureException` (subclase de `RuntimeException`)
- [x] 2.3 Crear `PacketCapture` (interfaz `AutoCloseable` con `start`, `stop`, `isRunning`, `statistics`, `close`)

## 3. Configuración (core)

- [x] 3.1 Crear `CaptureConfig` (clase final inmutable) con builder que aplica defaults y valida campos (device, promiscuous, snaplen, timeout, bufferSize, filter, immediateMode)

## 4. SPI y factoría (core.spi)

- [x] 4.1 Crear `CaptureProvider` (interfaz SPI: `name`, `isSupported`, `openLive`)
- [x] 4.2 Crear `AbstractPacketCapture` (máquina de estados `READY/RUNNING/STOPPED/FAILED/CLOSED`, hilo interno, hooks `doStart`/`doStop`/`doClose`/`doStatistics`, captura de excepción del listener y relanzado en `stop()`/`close()`)
- [x] 4.3 Crear `PacketCaptures` (factoría con `openLive(config)` y `openLive(config, backend)` vía `ServiceLoader`)

## 5. Módulo JPMS

- [x] 5.1 Actualizar `module-info.java`: `exports com.angazo.lostrego.core`, `uses`/`provides` de `com.angazo.lostrego.core.spi.CaptureProvider`

## 6. Tests unitarios (core, sin dependencias nativas)

- [x] 6.1 Tests de `CaptureTimestamp` (normalización de `nanosOfSecond`, `toInstant`)
- [x] 6.2 Tests de `Packet` (inmutabilidad/clonado del payload, longitudes)
- [x] 6.3 Tests de `LinkType` (código DLT conocido y preservación del código desconocido)
- [x] 6.4 Tests de `CaptureConfig` builder (defaults y validación)
- [x] 6.5 Tests de `PacketCaptures` con un `CaptureProvider` falso (selección por nombre, ausencia de backend, transparencia)
- [x] 6.6 Tests del ciclo de vida con un backend falso (`start` no bloqueante, `stop` bloqueante, `close` idempotente, re-arranque, excepción del listener → `CaptureException` en `stop()`/`close()`)
- [x] 6.7 `./gradlew build` en verde desde `src/` (Linux + Windows en CI)
