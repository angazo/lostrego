## Context

La librería `lostrego` expone el contrato público en `core` (`PacketCaptures.openLive(...)`, `PacketCapture`, `PacketListener`, `Packet`) y el backend libpcap ya está operativo. Este change introduce dos módulos de aplicación que consumen la librería en el *module path*: `lostrego-spy-common` (lógica reutilizable) y `lostrego-spy` (CLI de consola). El objetivo de prestaciones de la librería condiciona el diseño: ninguno de los dos módulos debe añadir latencia ni consumo de memoria al *hot path* de captura. Ver `proposal.md` para la motivación y `specs/` para los contratos de comportamiento.

## Goals / Non-Goals

**Goals:**
- Una CLI de análisis de tráfico utilizable y rápida de arrancar, que ejercite la librería de extremo a extremo.
- Una capa de análisis (`lostrego-spy-common`) reutilizable desde una futura interfaz gráfica, sin lógica de consola ni de I/O acoplada.
- Salida por línea, estilo `tcpdump`, con resumen L2-L4 básico (Ethernet/IPv4/IPv6/TCP/UDP).
- Apagado limpio (Ctrl+C) con estadísticas.
- Distribución ligera (ejecutable vía `jpackage`/`jlink`).

**Non-Goals:**
- Disección completa de protocolos (app layer, payloads): depende de la capa de parseo del issue #11.
- Enumeración de dispositivos (`listDevices`): depende del issue #8.
- Captura offline desde fichero: depende del issue #7.
- Resolución de nombres de host/OTS o agregación estadística avanzada.
- Implementar ahora la interfaz gráfica: solo se deja la arquitectura preparada para reutilizar `lostrego-spy-common`.

## Decisions

### D1: Java puro + picocli (sin Spring ni Quarkus)

`lostrego-spy` es una CLI que se lanza por sesión y con un *hot path* de captura sensible. Se descarta cualquier framework de DI/web:

- **Spring Boot**: arranque de segundos (contexto/DI), gran huella de memoria y distribución, y su valor (web/actuator) no aplica a un sniffer; su imagen nativa roza con FFM.
- **Quarkus**: arranque nativo rápido y baja memoria, pero sigue metiendo un framework entre el código y el *hot path*; la imagen nativa exige tooling GraalVM/Mandrel + configuración de reflexión, y FFM en nativo añade fricción; el árbol de dependencias es desproporcionado para lo que se necesita.

**Decisión:** aplicación Java pura. Para el parseo de argumentos se usa **picocli** (librería ligera de CLI, módulo JPMS `info.picocli`, no es un framework): da parsing, validación y ayuda sin coste de arranque apreciable.

### D2: Módulo JPMS y declaración del *native access*

La librería llama a la FFM API (métodos *restricted*), por lo que el JVM debe habilitar el acceso nativo para el módulo `com.angazo.lostrego`. El JDK 25 no permite declararlo en `module-info.java` (solo existe el flag de lanzamiento), así que la forma **elegante y transparente** es declararlo **una sola vez** en el build del módulo ejecutable mediante el plugin `application` de Gradle (`applicationDefaultJvmArgs = ['--enable-native-access=com.angazo.lostrego']`). Ese valor es la fuente de verdad única: se aplica al task `run` y a los scripts generados por `installDist`/`distZip`; cuando se añada un empaquetado `jpackage`, reutilizará el mismo valor en `--java-options`.

Este change absorbe el antiguo issue #15: la decisión se resuelve y documenta aquí. Si más adelante hubiera que comunicárselo a consumidores externos (p. ej. con la publicación a Maven Central), se abrirá un issue específico.

### D3: Separación en `lostrego-spy-common` y `lostrego-spy`

Se reparten las responsabilidades en dos módulos para que la futura interfaz gráfica reutilice la lógica de análisis sin arrastrar nada de consola:

- **`lostrego-spy-common`** (módulo JPMS `com.angazo.lostrego.spy.common`, paquetes `com.angazo.lostrego.spy.common` y `.protocol`): depende solo de `com.angazo.lostrego`. Contiene el modelo de análisis inmutable, el disector de protocolos (árbol de `Layer` + `LayerVisitor`), la configuración de captura agnóstica de la interfaz (`CaptureSettings`), y la orquestación de captura (`CaptureRunner`). **No** conoce picocli, la consola ni ningún medio de salida.
- **`lostrego-spy`** (módulo JPMS `com.angazo.lostrego.spy`, paquete `com.angazo.lostrego.spy`): depende de `lostrego-spy-common` y de `info.picocli`. Contiene el comando picocli, el renderizado a texto (línea + hex) y el manejo de Ctrl+C.

**Alternativas consideradas:**
- Un solo módulo con la lógica acoplada a la consola: la futura UI tendría que reescribir o arrastrar dependencias de consola. Rechazado.
- Poner la lógica de análisis en la propia librería `lostrego`: duplicaría el trabajo del issue #11 y ensuciaría el contrato público. Rechazado.

### D4: Contrato de salida en `common` (interfaz funcional consumidora)

`lostrego-spy-common` define un contrato funcional (p. ej. `TrafficConsumer`/`Consumer<TrafficRecord>`) al que entrega los análisis. El módulo no escribe nunca por sí mismo a `System.out` ni a ningún destino: el llamador (consola hoy, UI mañana) aporta el consumidor. Así `common` queda libre de I/O y 100 % reutilizable.

### D5: Disección como árbol de protocolos (disectors) en `common`

El parseo vive en `lostrego-spy-common`, en un paquete `protocol`, sin modificar la librería. Un paquete se ve como un **árbol de capas**: la interfaz `Layer` (`protocol()`, `payload()`, `accept(visitor)`) la implementa cada protocolo (hoy Ethernet → IPv4/IPv6 → TCP/UDP, más `UnknownLayer`), y cada capa guarda **sus propios campos tipados**. El parser sabe qué protocolos cuelgan de cada payload (ethertype, número de protocolo) y construye el árbol recursivamente. La disección rica (payloads, protocolos de aplicación, SCTP/Diameter) se añadirá como nuevas capas sin tocar el contrato.

**Alternativas consideradas:**
- Solo volcado crudo sin parseo: no cumple el objetivo de "herramienta de análisis". Rechazado.
- Parseo en el módulo de consola: impediría reutilizarlo desde la UI. Rechazado.
- Campos genéricos `Map<String,Object>` por capa: cómodo de recorrer, pero pierde tipado; se prefieren campos tipados y, para recorrer el árbol, el patrón visitor (`LayerVisitor`), donde cada frontend implementa su formatter (texto, JSON, ...).

### D6: Pipeline inline captura → análisis → entrega

La orquestación de captura (en `common`) instala un `PacketListener` que analiza y entrega al consumidor directamente en el hilo interno de captura de la librería, sin colas intermedias. Se preserva la semántica de backpressure de la librería (un consumidor lento descarta en el buffer del kernel/NIC, como en `tcpdump`).

**Alternativas consideradas:**
- Cola productor/consumidor + hilo de escritura: desacopla pero añade allocations y latencia. Rechazado para la primera versión.

### D7: Formato de salida (en `lostrego-spy`)

El módulo de consola renderiza cada análisis como una línea: `<seq>  HH:mm:ss.ffffff  <proto>  <src> → <dst>  len=<orig>/<caplen>`, y con `-x` un volcado hex/ascii debajo. La línea se construye recorriendo el árbol con un `LayerVisitor` (`Summary`) que reduce el árbol al resumen de una línea; un futuro renderer JSON/UI implementará otro `LayerVisitor` sobre el mismo árbol, sin tocar el parser.

### D8: Apagado limpio (en `lostrego-spy`)

La consola instala un *shutdown hook* (o maneja `SIGINT`) que llama a `stop()` y `close()` sobre la sesión, y luego imprime `CaptureStatistics`. El hook debe ser idempotente y no bloquear indefinidamente. La orquestación de `common` expone la detención, pero es la consola quien decide cuándo invocarla.

### D9: Tests sin privilegios

- Unitarios del parseo L2-L4 y del modelo de análisis (en `common`), con paquetes sintéticos en memoria.
- Unitarios del renderizado de línea/hex (en `lostrego-spy`).
- Integración con la librería usando captura offline (el `.pcap` de ejemplo ya existente) o un `CaptureProvider` falso, de modo que los tests corran en CI sin root.

## Risks / Trade-offs

- **[Riesgo] El resumen L2-L4 inline en `common` puede divergir de la futura capa de parseo (#11)** → Mitigación: se documenta como provisional; cuando #11 exista, `common` migrará a esa capa sin cambiar su contrato.
- **[Riesgo] El `run` task necesita el flag de native access** → Mitigación: se declara una sola vez en `applicationDefaultJvmArgs` (D2), cubriendo `run` y los scripts generados.
- **[Trade-off] Salida síncrona en el hilo de captura** → un consumidor lento puede provocar descartes; es la semántica natural y documentada de la librería.
- **[Trade-off] Frontera `common`/`spy` demasiado rígida** → si un detalle resulta ser más reutilizable o más específico de lo previsto, se mueve entre módulos en un change posterior sin romper el contrato de la CLI.

## Migration Plan

No aplica: módulos nuevos, sin código existente que migrar.

## Open Questions

Ninguna que afecte al contrato, al enfoque o al desglose de tareas.
