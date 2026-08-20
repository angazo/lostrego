## Purpose

Define la capa de análisis reutilizable (`lostrego-spy-common`) construida sobre la librería lostrego: un modelo de análisis inmutable, el parseo de las capas de enlace y red de cada paquete, y la orquestación de una captura que entrega esos análisis a un consumidor sin asumir ninguna interfaz concreta (consola o interfaz gráfica).

## ADDED Requirements

### Requirement: Modelo de análisis inmutable y autónomo

La capa SHALL representar cada paquete analizado como un objeto inmutable y autónomo con su número de secuencia de lectura, timestamp, longitudes, protocolo, direcciones origen/destino y, cuando aplique, puertos y flags, sin referencia a memoria nativa ni al backend.

#### Scenario: Registro de análisis

- **WHEN** se analiza un paquete capturado
- **THEN** se obtiene un registro inmutable con los campos de metadatos y cabeceras disponibles

#### Scenario: Número de secuencia de lectura

- **WHEN** se capturan paquetes consecutivos en una misma sesión
- **THEN** cada registro lleva un número de secuencia creciente que refleja el orden de lectura de los paquetes

### Requirement: Parseo de capas de enlace y red

La capa SHALL parsear las cabeceras Ethernet, IPv4/IPv6 y TCP/UDP de un paquete crudo para extraer el protocolo, las direcciones de origen y destino y, en su caso, los puertos y flags, sin interpretar el payload.

#### Scenario: Paquete IPv4/TCP

- **WHEN** se analiza un paquete Ethernet que encapsula IPv4 y TCP
- **THEN** el registro refleja el protocolo, las direcciones IPv4 y los puertos TCP

#### Scenario: Protocolo no reconocido

- **WHEN** se analiza un paquete cuya cabecera no es de un protocolo soportado
- **THEN** el registro conserva la información disponible (p. ej. el tipo de capa de enlace) y marca el resto como no resuelto sin lanzar error

### Requirement: Orquestación de captura independiente de la interfaz

La capa SHALL abrir una captura en vivo con una configuración dada y entregar los paquetes analizados a un consumidor proporcionado por el llamador, sin realizar por sí misma ninguna escritura en consola ni asumir un medio de salida concreto.

#### Scenario: Entrega a un consumidor

- **WHEN** se inicia una captura a través de la capa con un consumidor de análisis
- **THEN** cada paquete capturado se analiza y se entrega al consumidor en el hilo de captura

#### Scenario: Límite de paquetes

- **WHEN** se configura un límite de paquetes
- **THEN** la captura se detiene automáticamente al alcanzarlo

### Requirement: Estadísticas al finalizar la captura

La capa SHALL exponer las estadísticas de captura (recibidos, descartados y descartados por la interfaz) al finalizar la captura.

#### Scenario: Consulta de estadísticas

- **WHEN** la captura finaliza (por límite o por detención)
- **THEN** el llamador puede obtener las estadísticas de la sesión
