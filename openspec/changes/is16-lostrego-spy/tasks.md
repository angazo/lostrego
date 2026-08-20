## 1. Módulos y build

- [x] 1.1 Añadir `lostrego-spy-common` y `lostrego-spy` a `src/settings.gradle` con sus `build.gradle` (dependencias y plugin `application` en `lostrego-spy`)
- [x] 1.2 Añadir `picocli` al catálogo `src/gradle/libs.versions.toml`
- [x] 1.3 Crear `module-info.java` de `lostrego-spy-common` (`requires com.angazo.lostrego`)
- [x] 1.4 Crear `module-info.java` de `lostrego-spy` (`requires com.angazo.lostrego.spy.common`, `requires info.picocli`) y declarar `--enable-native-access=com.angazo.lostrego` vía `applicationDefaultJvmArgs`

## 2. Lógica reutilizable (`lostrego-spy-common`)

- [x] 2.1 Crear el modelo de análisis inmutable (`TrafficRecord`: timestamp, longitudes, protocolo, direcciones, puertos, flags)
- [x] 2.2 Implementar el parseo L2-L4 (Ethernet → IPv4/IPv6 → TCP/UDP) sobre el `Packet` de la librería
- [x] 2.3 Implementar la orquestación de captura que abre la sesión y entrega los análisis a un consumidor funcional
- [x] 2.4 Añadir soporte de límite de paquetes y exposición de estadísticas al finalizar

## 3. Aplicación de consola (`lostrego-spy`)

- [x] 3.1 Definir el comando picocli con las opciones (device, filter, count, promiscuous, verbose/hex, version/help)
- [x] 3.2 Implementar el punto de entrada `main` con ayuda/versión y códigos de salida ante errores
- [x] 3.3 Implementar el renderizado a consola de los análisis (línea por paquete + volcado hex con `-x`)
- [x] 3.4 Implementar el apagado limpio (Ctrl+C) con cierre de sesión y estadísticas

## 4. Tests

- [x] 4.1 Tests unitarios del parseo L2-L4 con paquetes sintéticos (en `common`)
- [x] 4.2 Tests unitarios del modelo de análisis (inmutabilidad, campos no resueltos)
- [x] 4.3 Tests unitarios del renderizado de línea/hex (en `lostrego-spy`)
- [x] 4.4 Test de integración end-to-end con captura offline o backend falso (sin privilegios)
- [x] 4.5 `./gradlew build` en verde desde `src/`
