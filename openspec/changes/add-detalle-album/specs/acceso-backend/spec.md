# Spec Delta

## Purpose

Definir el punto común de acceso al backend que reutilizan los repositorios, manteniendo la información HTTP necesaria para que cada repositorio interprete los errores pertinentes a su entidad.

## ADDED Requirements

### Requirement: Punto único de acceso de red compartido
La app SHALL usar una única instancia de Service Adapter para configurar el cliente de red y ejecutar llamadas al backend desde todos los repositorios. Un repositorio MUST NOT crear por su cuenta otro cliente o configuración de red para consumir el mismo backend.

#### Scenario: Repositorios comparten configuración
- **WHEN** se consultan diferentes entidades del backend
- **THEN** sus repositorios usan el mismo Service Adapter y la misma configuración de red

#### Scenario: Añadir un repositorio
- **WHEN** se agrega un repositorio para una nueva entidad
- **THEN** consume su interfaz Retrofit a través del Service Adapter existente

### Requirement: Conservar el status HTTP para clasificación por entidad
El Service Adapter SHALL hacer disponible el código de estado HTTP de las respuestas no exitosas, o una representación tipada equivalente. MUST permitir que el repositorio distinga HTTP 404 de un fallo de transporte y de otros errores HTTP.

#### Scenario: Respuesta 404
- **WHEN** una llamada responde HTTP 404
- **THEN** el repositorio recibe el status o error tipado que identifica el 404
- **AND** puede traducirlo al estado de dominio correspondiente sin confundirlo con falta de conexión

#### Scenario: Otro error HTTP
- **WHEN** una llamada responde con un error HTTP distinto de 404
- **THEN** el repositorio puede tratarlo como un fallo de carga recuperable según el comportamiento de la entidad

### Requirement: Uniformidad de errores de transporte y cancelación
El Service Adapter SHALL reportar de forma uniforme los fallos de conexión, timeout y respuesta ilegible, sin ocultar el status HTTP que necesita el repositorio. SHALL propagar la cancelación de corrutinas y MUST NOT convertirla en un error visible de carga.

#### Scenario: Error de conexión o timeout
- **WHEN** una llamada no puede conectarse o excede el tiempo de espera
- **THEN** el Service Adapter devuelve un error de carga que el repositorio puede propagar

#### Scenario: Respuesta ilegible
- **WHEN** una respuesta exitosa tiene un cuerpo que no se puede interpretar
- **THEN** el Service Adapter devuelve un error de carga

#### Scenario: Cancelar una llamada
- **WHEN** se cancela la corrutina que realiza una llamada al backend
- **THEN** la cancelación se propaga y no se presenta como error de conexión
