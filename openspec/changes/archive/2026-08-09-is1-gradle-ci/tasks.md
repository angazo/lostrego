## 1. Gradle wrapper y configuración base

- [x] 1.1 Generar el wrapper de Gradle 9.6.1 en `src/` (`src/gradlew`, `src/gradlew.bat`, `src/gradle/wrapper/`)
- [x] 1.2 Crear `src/gradle.properties` con `org.gradle.jvmargs=-Xmx2g -Dfile.encoding=UTF-8`
- [x] 1.3 Crear `src/gradle/libs.versions.toml` con versión y librería de JUnit 5 (`junit-jupiter:5.12.2`)
- [x] 1.4 Crear `src/settings.gradle` con `rootProject.name = "lostrego"`, incluir módulo `lostrego` y placeholder comentado para `lostrego-app`
- [x] 1.5 Crear `src/build.gradle` raíz: grupo `com.angazo`, versión `0.1.0`, Java 25 toolchain, JUnit 5 platform, repositorio Maven Central

## 2. Estructura de directorios del módulo

- [x] 2.1 Crear directorios de paquetes: `src/lostrego/src/main/java/com/angazo/lostrego/core/`, `backend/libpcap/`, `backend/pdpk/`, `backend/npcap/`
- [x] 2.2 Crear directorios de tests: `src/lostrego/src/test/java/com/angazo/lostrego/`
- [x] 2.3 Crear `src/lostrego/src/test/resources/` para futuros archivos `.pcap`
- [x] 2.4 Crear `src/lostrego/src/main/java/module-info.java` con `module com.angazo.lostrego { }` vacío

## 3. .gitignore

- [x] 3.1 Crear `.gitignore` en la raíz del repo con reglas: `.gradle/`, `build/`, `src/build/`, `src/**/build/`, IDE (`.idea/`, `.vscode/`, `*.iml`), OS (`.DS_Store`, `Thumbs.db`, `*~`), Java (`*.class`, `*.jar` excepto `src/gradle/wrapper/gradle-wrapper.jar`, `*.war`, `*.nar`, `*.ear`), logs (`*.log`), tools (`.opencode/`, `bin/`)

## 4. CI con GitHub Actions

- [x] 4.1 Crear `.github/workflows/project-ci.yml`
- [x] 4.2 Configurar trigger: `pull_request: branches: [main]` (sin `push`)
- [x] 4.3 Configurar job `build` con matriz `os: [ubuntu-latest, windows-latest]`
- [x] 4.4 Steps: `actions/checkout@v4`, `actions/setup-java@v4` (temurin, Java 25), `gradle/actions/setup-gradle@v4`
- [x] 4.5 Step condicional Linux: `sudo apt-get install -y libpcap-dev`
- [x] 4.6 Step: `./gradlew build` con `working-directory: src`

## 5. Validación

- [x] 5.1 Ejecutar `./gradlew build` desde `src/` localmente y verificar que compila sin errores
- [x] 5.2 Verificar que `src/gradle/wrapper/gradle-wrapper.jar` está en el repositorio (necesario para CI)
- [x] 5.3 Verificar que los directorios de tests existen y están vacíos (Gradle los reconoce como source sets)
