## Why

La librería `lostrego` ya captura paquetes (API `core` + backend libpcap), pero no existe aún ninguna forma de consumirla desde fuera: falta una herramienta real que la ejercite y demuestre su utilidad. `lostrego-spy` es una aplicación de consola de análisis de tráfico (en la línea de `tshark`/`tcpdump`) que sirve a la vez como primer consumidor real de la librería —validación extremo a extremo del contrato público— y como utilidad práctica de inspección de red.

Para no duplicar esfuerzos en el futuro, la lógica de análisis que vive *sobre* la librería se aísla en un módulo reutilizable (`lostrego-spy-common`), de modo que una futura aplicación de interfaz gráfica pueda consumir exactamente la misma lógica sin depender de la consola.

## What Changes

- Nuevo módulo Gradle `lostrego-spy-common` que depende de `lostrego` y contiene toda la lógica independiente de la interfaz: modelo de análisis inmutable, parseo L2-L4 (Ethernet → IPv4/IPv6 → TCP/UDP) y orquestación de la captura que entrega registros analizados a un consumidor.
- Nuevo módulo Gradle `lostrego-spy` (aplicación de consola) que depende de `lostrego-spy-common` y añade lo específico de una CLI: parseo de argumentos con picocli, renderizado de los análisis a texto y gestión del apagado.
- CLI con opciones: dispositivo (`-i`), filtro BPF (`-f`), límite de paquetes (`-c`), modo promiscuo, salida verbosa/hex (`-v`/`-x`), versión y ayuda.
- Captura en vivo mediante `PacketCaptures.openLive(...)` y salida de una línea por paquete (timestamp, longitud y resumen L2-L4 básico).
- Apagado limpio con Ctrl+C, mostrando las estadísticas de captura al finalizar.
- Implementación en **Java puro + picocli** (sin Spring/Quarkus); ver `design.md` D1 para el análisis de pros/contras.

## Capabilities

### New Capabilities

- `lostrego-spy-common`: capa de análisis reutilizable sobre la librería — modelo de análisis inmutable, parseo L2-L4 y orquestación de captura que entrega registros analizados, sin asumir ninguna interfaz concreta (consola o UI).
- `lostrego-spy`: aplicación de consola de análisis de tráfico — interfaz de línea de comandos, apertura de captura en vivo, presentación de los análisis en texto y apagado limpio con estadísticas.

### Modified Capabilities

Ninguna. La librería no cambia: estos módulos son consumidores nuevos, no una modificación del contrato existente.

## Impact

- `src/settings.gradle`: añade `include "lostrego-spy-common"` e `include "lostrego-spy"`.
- Nuevo módulo `src/lostrego-spy-common/` con su `build.gradle`, `module-info.java` (`requires com.angazo.lostrego`) y fuentes/tests.
- Nuevo módulo `src/lostrego-spy/` con su `build.gradle`, `module-info.java` (`requires com.angazo.lostrego.spy.common`, `requires info.picocli`) y fuentes/tests.
- Nueva dependencia `info.picocli:picocli` (solo en `lostrego-spy`), centralizada en el catálogo `src/gradle/libs.versions.toml`.
- Sin cambios en la librería `lostrego` ni en la CI: los módulos compilan y testean dentro de la matriz existente; sus tests no requieren privilegios (usan captura offline o un backend falso).
- Configuración del *native access* como decisión propia de este change (absorbe el issue #15): el flag `--enable-native-access=com.angazo.lostrego` se declara una única vez en el build de `lostrego-spy` y se aplica a `run` y a los scripts de lanzamiento generados.
