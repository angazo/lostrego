## Context

El proyecto está vacío, sin estructura de build ni CI. Se necesita un sistema de build que compile Java 25, ejecute tests con JUnit 5, y exponga la FFM API (incluida en el JDK, sin dependencias externas). Ver `proposal.md` para la motivación.

Las referencias principales son la configuración Gradle del proyecto arume (mismo desarrollador, mismas convenciones) y la documentación oficial de `gradle/actions/setup-gradle@v4`.

## Goals / Non-Goals

**Goals:**
- Proyecto Gradle funcional con Java 25 toolchain y JUnit 5
- CI en GitHub Actions con matriz Linux + Windows, solo en PRs a `main`
- En Linux, instalar `libpcap-dev` como dependencia de build
- Estructura de paquetes vacía bajo `lostrego/src/main/java/com/angazo/lostrego/`
- `module-info.java` placeholder
- `.gitignore` con reglas Gradle/IDE/Java

**Non-Goals:**
- Escribir código fuente de la librería (eso será el issue #2, API pública)
- Configurar publicación a Maven Central o similar
- Añadir dependencias más allá de JUnit 5
- macOS en la matriz de CI (se añadirá cuando haya tests de integración con backends)

## Decisions

### D1: Gradle 9.6.x con Java 25 toolchain

Se usa la misma versión de Gradle que el proyecto arume (9.6.1), que ya ha sido validada con Java 25. La toolchain (`java.toolchain`) delega la resolución del JDK en Gradle, que lo descarga automáticamente si no está instalado.

**Alternativas consideradas:**
- Gradle 8.x: no soporta oficialmente Java 25; descartado.
- `JAVA_HOME` explícito en el workflow: más frágil, la toolchain de Gradle es más portable.

### D2: Version catalog (`gradle/libs.versions.toml`)

Se usa el catálogo de versiones de Gradle para centralizar las dependencias, siguiendo la práctica del proyecto arume. Inicialmente solo contiene JUnit 5.

**Alternativas consideradas:**
- Dependencias inline en `build.gradle`: menos mantenible a largo plazo.
- BOM de JUnit: innecesario para una sola dependencia; se añadirá si crece.

### D3: Proyecto Gradle bajo `src/`, módulo único con paquete base `com.angazo.lostrego`

Siguiendo el patrón del proyecto arume, todos los ficheros de build (`build.gradle`, `settings.gradle`, `gradlew`, `gradle/`) y los módulos viven bajo `src/`. El `settings.gradle` incluye `lostrego` y deja comentado `lostrego-app` para el futuro. La CI usa `working-directory: src` para ejecutar los comandos Gradle.

### D4: module-info.java como placeholder

Se crea un `module-info.java` con `module com.angazo.lostrego { }` vacío. Se irá rellenando conforme se defina la API pública y los `exports`. Esto evita que futuros `requires` se olviden al añadir dependencias.

### D5: CI solo en PRs, no en push a main

Siguiendo el patrón de arume y lo documentado en AGENTS.md, el workflow se dispara con `pull_request: branches: [main]`, no con `push`. Esto evita builds redundantes (el merge ya fue validado en la PR).

### D6: Matriz Linux + Windows

Se usa `matrix.os: [ubuntu-latest, windows-latest]`. En Linux se instala `libpcap-dev` con `apt-get`. Windows no necesita npcap instalado para compilar (solo para tests de integración, que aún no existen). macOS se pospone — se añadirá al primer backend que lo soporte.

### D7: `gradle/actions/setup-gradle@v4` para cache, `working-directory: src` para Gradle

Se usa la acción oficial de Gradle para configurar el build, que incluye caché de dependencias y wrappers. Todos los comandos Gradle se ejecutan con `working-directory: src`, ya que los ficheros de build viven bajo `src/` (patrón de arume).

### D8: Sin xvfb-run

A diferencia de arume (que necesita Xvfb para JavaFX), lostrego es una librería de backend sin GUI. No se necesita `xvfb-run`.

## Risks / Trade-offs

- **[Riesgo] Gradle 9.6.1 podría no estar disponible en el registry de `gradle/actions/setup-gradle@v4`** → Mitigación: el wrapper descarga Gradle automáticamente; la acción solo configura el entorno.
- **[Riesgo] `libpcap-dev` solo se instala en Linux, no en Windows** → Aceptado: en Windows el backend es npcap. Los tests de integración de libpcap usarán `@EnabledOnOs(LINUX)`.
- **[Trade-off] Sin macOS en la matriz** → La compilación de código Java puro (core) se valida en Linux y Windows; macOS se añadirá cuando haya tests de integración que lo requieran.
