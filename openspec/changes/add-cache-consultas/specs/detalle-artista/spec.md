# Spec Delta

## MODIFIED Requirements

### Requirement: Indicador de carga
Mientras se consulta la información del artista y no hay un detalle guardado en el dispositivo, la pantalla SHALL mostrar un indicador de carga en lugar de una pantalla vacía, con la opción de volver al listado disponible. Si hay un detalle guardado, la pantalla MUST mostrarlo en lugar del indicador de pantalla completa.

#### Scenario: Indicador mientras carga
- **WHEN** el usuario abre el detalle, no hay un detalle guardado y el backend aún no responde
- **THEN** se muestra un indicador de carga
- **AND** presionar "Atrás" vuelve al listado

#### Scenario: Carga en curso con detalle guardado
- **WHEN** el usuario abre el detalle, hay un detalle guardado y el backend aún no responde
- **THEN** se muestra el detalle guardado con un indicador de actualización
- **AND** no se muestra el indicador de carga de pantalla completa

### Requirement: Error de carga con reintento
Si el artista ya no existe, la pantalla SHALL mostrar "No se pudo cargar el artista" y el botón "Reintentar", aunque haya un detalle guardado. Si la consulta falla por conexión, servidor o respuesta ilegible y no hay un detalle guardado, MUST mostrar el mismo mensaje y botón, sin datos parciales. La app MUST NOT cerrarse.

#### Scenario: El artista ya no existe
- **WHEN** el backend responde 404 a la consulta del artista, haya o no un detalle guardado
- **THEN** se muestra "No se pudo cargar el artista" y el botón "Reintentar"

#### Scenario: Falla la conexión
- **WHEN** no hay un detalle guardado y no hay conexión con el backend o este responde con un error del servidor
- **THEN** se muestra "No se pudo cargar el artista" y el botón "Reintentar"

#### Scenario: Falla la consulta de premios
- **WHEN** no hay un detalle guardado, el artista tiene premios, su consulta responde bien y falla la consulta de los datos de esos premios
- **THEN** se muestra "No se pudo cargar el artista" y el botón "Reintentar"
- **AND** no se muestra un detalle parcial

#### Scenario: Reintentar tras un error
- **WHEN** el usuario pulsa "Reintentar" y el backend ya responde
- **THEN** se muestra el indicador de carga y luego el detalle del artista

## ADDED Requirements

### Requirement: Detalle de artista guardado en el dispositivo
La app SHALL guardar en el dispositivo el detalle completo de cada artista que el usuario abre con éxito, incluidos sus integrantes, álbumes y premios, y mostrarlo la siguiente vez que lo abra mientras lo consulta. Cada consulta exitosa MUST reemplazar el detalle guardado de ese artista, y lo guardado MUST conservarse al cerrar y volver a abrir la app. La app MUST NOT descargar detalles que el usuario no abrió.

#### Scenario: Abrir sin conexión un artista ya visto
- **WHEN** el usuario abrió el detalle de un artista con conexión en una apertura anterior de la app
- **AND** lo vuelve a abrir sin conexión o con el servidor caído
- **THEN** se muestran su nombre, imagen, descripción, fecha, integrantes, álbumes y premios guardados
- **AND** se informa brevemente "No se pudo actualizar el artista", una sola vez
- **AND** no se muestra "No se pudo cargar el artista"

#### Scenario: Abrir sin conexión un artista nunca visto
- **WHEN** el usuario abre sin conexión el detalle de un artista que nunca abrió con conexión
- **THEN** se muestra "No se pudo cargar el artista" y el botón "Reintentar"

#### Scenario: Músico y banda con el mismo identificador
- **WHEN** hay guardados el detalle de un músico y el de una banda con el mismo identificador
- **THEN** cada uno muestra su propio detalle

#### Scenario: Artista eliminado con detalle guardado
- **WHEN** hay un detalle guardado y el backend responde 404 a la consulta de ese artista
- **THEN** se muestra "No se pudo cargar el artista" y el botón "Reintentar"
- **AND** la siguiente apertura sin conexión de ese artista no muestra el detalle guardado

#### Scenario: Falla el almacenamiento del dispositivo
- **WHEN** no se puede leer ni escribir el detalle guardado
- **THEN** el detalle se comporta como si no hubiera nada guardado
- **AND** la app no se cierra
