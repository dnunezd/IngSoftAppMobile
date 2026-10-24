# Spec Delta

## Purpose

Define cómo la app accede al backend BackVynils: un único punto de acceso compartido por todas las entidades que traduce de forma uniforme los fallos de red y del servidor, de modo que cada historia agregue datos sin repetir esa lógica.

## ADDED Requirements

### Requirement: Punto único de acceso al backend
La app SHALL realizar todas las consultas al backend a través de un único punto de acceso configurado con la URL base del backend, compartido por los datos de todas las entidades (coleccionistas, álbumes, artistas).

#### Scenario: Una sola configuración de red para toda la app
- **WHEN** la app arranca con una URL base del backend
- **THEN** las consultas de cualquier entidad usan esa misma URL base y la misma configuración de tiempos de espera

#### Scenario: Nueva entidad sin reconfigurar la red
- **WHEN** se agrega el acceso a los datos de una nueva entidad del backend
- **THEN** reutiliza el punto de acceso existente sin crear otro cliente de red

### Requirement: Datos correctos sin alteración
El punto de acceso SHALL entregar tal cual la respuesta exitosa del backend a quien la pidió, y SHALL reportar el resultado como éxito o como error, nunca lanzar fallos de red hacia quien llama.

#### Scenario: Respuesta exitosa
- **WHEN** el backend responde con éxito a una consulta
- **THEN** quien consultó recibe los datos de la respuesta como un resultado exitoso

### Requirement: Error de carga uniforme
Ante cualquier fallo al consultar el backend, el punto de acceso SHALL reportar un único tipo de error de carga, sin importar la causa, para que las pantallas muestren el mismo mensaje y ofrezcan "Reintentar".

#### Scenario: Sin conexión
- **WHEN** la consulta no puede conectarse al backend
- **THEN** se reporta el error de carga

#### Scenario: Tiempo agotado
- **WHEN** el backend no responde dentro del tiempo de espera
- **THEN** se reporta el error de carga

#### Scenario: Error del servidor o recurso inexistente
- **WHEN** el backend responde con un código de error (por ejemplo 404 o 500)
- **THEN** se reporta el error de carga

#### Scenario: Respuesta ilegible
- **WHEN** el backend responde con un cuerpo que no se puede interpretar
- **THEN** se reporta el error de carga

#### Scenario: Reintento tras un fallo
- **WHEN** una consulta falló y el backend vuelve a estar disponible
- **THEN** repetir la consulta devuelve los datos sin necesidad de reiniciar la app

### Requirement: La cancelación no es un error
Cuando se cancela una consulta en curso (por ejemplo al salir de la pantalla), el punto de acceso SHALL propagar la cancelación y NOT reportarla como error de carga.

#### Scenario: Pantalla cerrada durante la consulta
- **WHEN** la pantalla que pidió los datos se cierra mientras la consulta está en curso
- **THEN** la cancelación se propaga y no se muestra ningún mensaje de error
