## Why

El proyecto carece de cualquier estructura de build. Sin un esqueleto Gradle y un pipeline de CI, no se puede compilar, ejecutar tests ni validar cambios de forma automatizada. Esta es la primera pieza de infraestructura, necesaria antes de escribir una sola línea de código de la librería.

## What Changes

- Crear el wrapper de Gradle (`src/gradlew`, `src/gradlew.bat`, `src/gradle/wrapper/`) con Gradle 9.6.x
- Crear `src/settings.gradle` con el módulo `lostrego` y placeholder para `lostrego-app`
- Crear `src/build.gradle` raíz con configuración común: Java 25 toolchain, JUnit 5, grupo `com.angazo`
- Crear `src/gradle.properties` con opciones de JVM para Gradle
- Crear `.gitignore` con entradas para Gradle, IDE, OS, Java (excepto wrapper jar)
- Crear la estructura de directorios del módulo `src/lostrego/src/main/java/com/angazo/lostrego/` con los paquetes `core`, `backend/libpcap`, `backend/pdpk`, `backend/npcap`
- Crear `module-info.java` para el módulo de librería (placeholder, se rellenará cuando se defina la API)
- Crear `.github/workflows/project-ci.yml` con matriz Linux + Windows, instalación de `libpcap-dev` en Linux, ejecución de `./gradlew build` desde `src/` solo en PRs a `main`

## Capabilities

### New Capabilities

Ninguna. Este change es puramente de infraestructura (build + CI), no introduce comportamiento de librería.

### Modified Capabilities

Ninguna.

## Impact

- `.github/workflows/project-ci.yml`: nuevo workflow que se ejecuta en cada PR a `main`
- `build.gradle`, `settings.gradle`, `gradle.properties`, `gradle/`, `gradlew`, `gradlew.bat`: sistema de build
- `lostrego/`: nuevo módulo con estructura de paquetes vacía
- `.gitignore`: se actualiza con reglas de Gradle, IDE y Java
