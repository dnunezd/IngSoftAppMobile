# Spec Delta

## ADDED Requirements

### Requirement: Revalidar las consultas repetidas
El punto de acceso SHALL conservar en el dispositivo la última respuesta de cada consulta de lectura junto con la versión que le asignó el backend. Al repetir la consulta, MUST enviar esa versión para preguntar si cambió. Si el backend confirma que no cambió, SHALL entregar la respuesta conservada como un resultado exitoso, igual que una respuesta completa.

#### Scenario: Consulta repetida sin cambios
- **WHEN** el backend respondió a `GET /albums` con la versión `W/"abc"` y la app repite la consulta
- **THEN** la petición lleva la cabecera `If-None-Match: W/"abc"`
- **AND** si el backend responde 304 sin cuerpo, quien consultó recibe los mismos álbumes como resultado exitoso

#### Scenario: Consulta repetida con cambios
- **WHEN** la app repite una consulta y el backend responde 200 con un cuerpo nuevo
- **THEN** quien consultó recibe los datos nuevos
- **AND** la siguiente repetición envía la versión de esa respuesta nueva

#### Scenario: Respuesta sin versión
- **WHEN** el backend responde sin cabecera `ETag`
- **THEN** la siguiente repetición de esa consulta descarga el cuerpo completo, como antes

#### Scenario: Versión disponible para quien consulta
- **WHEN** una consulta de lectura responde con éxito, con cuerpo completo o con 304
- **THEN** quien consultó recibe también la versión de la respuesta, para saber si cambió respecto de lo que ya guardó

### Requirement: Una respuesta conservada se usa solo con confirmación del backend
El punto de acceso MUST NOT entregar una respuesta conservada sin preguntarle antes al backend, aunque el backend indique que la respuesta se puede reutilizar durante un tiempo. Si no hay conexión, SHALL reportar el error de carga como hoy. Los datos sin conexión vienen de lo que cada pantalla guarda en el dispositivo.

#### Scenario: Sin conexión con una respuesta conservada
- **WHEN** la app ya consultó `GET /albums` y repite la consulta sin conexión
- **THEN** se reporta el error de carga
- **AND** la pantalla sigue el comportamiento sin conexión que ya tiene: muestra lo guardado y avisa que no se pudo actualizar

#### Scenario: El backend permite reutilizar la respuesta
- **WHEN** el backend responde con `Cache-Control: max-age=600` y la app repite la consulta un minuto después
- **THEN** la petición llega al backend con `If-None-Match`

#### Scenario: Después de crear un álbum
- **WHEN** el usuario crea un álbum y luego se consulta `GET /albums`
- **THEN** quien consultó recibe el catálogo que incluye el álbum creado
