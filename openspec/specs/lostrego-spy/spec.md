# lostrego-spy Specification

## Purpose

Define el comportamiento de `lostrego-spy`, la aplicación de consola de análisis de tráfico construida sobre la librería lostrego: su interfaz de línea de comandos, la captura en vivo y presentación de paquetes, y el apagado limpio con estadísticas.

## Requirements

### Requirement: Interfaz de línea de comandos

La aplicación SHALL aceptar opciones de línea de comandos para seleccionar el dispositivo, el filtro BPF, el límite de paquetes, el modo promiscuo, la salida verbosa/hex, y SHALL mostrar la ayuda y la versión.

#### Scenario: Ayuda y versión

- **WHEN** el usuario invoca la aplicación con `--help` o `--version`
- **THEN** se muestra la ayuda o la versión y la aplicación termina con éxito sin capturar

#### Scenario: Opciones de captura

- **WHEN** el usuario invoca la aplicación con dispositivo, filtro y límite de paquetes
- **THEN** esos valores se aplican a la captura abierta

### Requirement: Captura en vivo y presentación de paquetes

La aplicación SHALL abrir una captura en vivo con la configuración indicada y SHALL presentar, para cada paquete capturado, una línea con su timestamp, longitud y un resumen de las capas de enlace y red.

#### Scenario: Captura y presentación

- **WHEN** la aplicación captura paquetes en una interfaz con tráfico
- **THEN** cada paquete se imprime como una línea con timestamp, longitudes y resumen L2-L4

#### Scenario: Límite de paquetes

- **WHEN** se indica un límite de paquetes (`-c`)
- **THEN** la aplicación se detiene tras capturar ese número de paquetes

### Requirement: Salida verbosa y volcado hexadecimal

La aplicación SHALL permitir mostrar, opcionalmente, el contenido del paquete en hexadecimal junto a la línea de resumen.

#### Scenario: Volcado hexadecimal

- **WHEN** el usuario activa la opción de volcado (`-x`)
- **THEN** la salida incluye los bytes del paquete en formato hexadecimal

### Requirement: Apagado limpio con estadísticas

La aplicación SHALL detener la captura y liberar los recursos al recibir una señal de interrupción (Ctrl+C), y SHALL mostrar las estadísticas de captura al finalizar.

#### Scenario: Interrupción de la captura

- **WHEN** el usuario interrumpe la captura con Ctrl+C
- **THEN** la captura se detiene de forma limpia, se liberan los recursos y se muestran las estadísticas

### Requirement: Errores informativos

La aplicación SHALL terminar con un mensaje de error claro (y código de salida distinto de cero) cuando el backend no esté disponible, el dispositivo no exista o el filtro no sea válido.

#### Scenario: Apertura fallida

- **WHEN** la captura no puede abrirse (backend ausente, dispositivo inválido o filtro malformado)
- **THEN** la aplicación muestra un mensaje informativo y termina con código de error
