# Spec Delta

## MODIFIED Requirements

### Requirement: Indicador de carga
Mientras la app consulta el detalle del álbum y no hay un detalle guardado en el dispositivo, SHALL mostrar un indicador de progreso y MUST NOT mostrar una pantalla en blanco. Si hay un detalle guardado, la app MUST mostrarlo en lugar del indicador de pantalla completa.

#### Scenario: Carga en curso
- **WHEN** se solicita el detalle, no hay un detalle guardado y todavía no llega la respuesta
- **THEN** se muestra un indicador de progreso

#### Scenario: Carga en curso con detalle guardado
- **WHEN** se solicita el detalle, hay un detalle guardado y todavía no llega la respuesta
- **THEN** se muestra el detalle guardado con un indicador de actualización
- **AND** no se muestra el indicador de progreso de pantalla completa

### Requirement: Error de conexión o servidor
Si falla la consulta del detalle por conexión o por un error del servidor y no hay un detalle guardado en el dispositivo, la app SHALL mostrar "No pudimos cargar el álbum" con un botón "Reintentar", y MUST NOT cerrarse. Estos errores SHALL distinguirse de la respuesta 404.

#### Scenario: Error de conexión o servidor
- **WHEN** no hay un detalle guardado y la consulta falla porque no hay conexión, vence el tiempo de espera o el servidor responde con un error distinto de 404
- **THEN** se muestra "No pudimos cargar el álbum" y el botón "Reintentar"
- **AND** la app no se cierra

#### Scenario: Reintentar la consulta
- **WHEN** el usuario pulsa "Reintentar"
- **THEN** la app vuelve a mostrar el indicador de progreso
- **AND** vuelve a consultar el mismo álbum

#### Scenario: El reintento tiene éxito
- **WHEN** una consulta reintentada recibe correctamente el detalle del álbum
- **THEN** se muestra el detalle

## ADDED Requirements

### Requirement: Detalle de álbum guardado en el dispositivo
La app SHALL guardar en el dispositivo el detalle completo de cada álbum válido que reciba, tanto al consultar el catálogo como al abrir su detalle, y mostrarlo al abrir el detalle mientras lo consulta. Cada consulta exitosa del catálogo MUST reemplazar por completo los detalles guardados, y lo guardado MUST conservarse al cerrar y volver a abrir la app.

#### Scenario: Abrir sin conexión un álbum del catálogo guardado
- **WHEN** el usuario vio el catálogo con conexión en una apertura anterior de la app, sin abrir el detalle de un álbum
- **AND** abre ese detalle sin conexión o con el servidor caído
- **THEN** se muestran su portada, nombre, fecha de lanzamiento, canciones, artistas, comentarios y promedio de calificaciones
- **AND** se informa brevemente "No pudimos actualizar el álbum", una sola vez
- **AND** no se muestra "No pudimos cargar el álbum"

#### Scenario: Detalle actualizado con conexión
- **WHEN** hay un detalle guardado y el backend responde con datos distintos para ese álbum
- **THEN** se muestra primero el detalle guardado
- **AND** al terminar la consulta se muestra el de la respuesta

#### Scenario: Álbum eliminado con detalle guardado
- **WHEN** hay un detalle guardado y `GET /albums/{id}` responde HTTP 404
- **THEN** se muestra "El álbum ya no está disponible" con la opción de regresar al catálogo
- **AND** la siguiente apertura sin conexión de ese álbum no muestra el detalle guardado

#### Scenario: Álbum que ya no viene en el catálogo
- **WHEN** un álbum con detalle guardado ya no está en la respuesta del catálogo
- **THEN** su detalle deja de estar guardado

#### Scenario: Álbum con datos incompletos
- **WHEN** el backend entrega un álbum al que le falta un dato obligatorio del detalle
- **THEN** ese detalle no se guarda
- **AND** abrirlo sin conexión muestra "No pudimos cargar el álbum" con "Reintentar"

#### Scenario: Falla el almacenamiento del dispositivo
- **WHEN** no se puede leer ni escribir el detalle guardado
- **THEN** el detalle se comporta como si no hubiera nada guardado
- **AND** la app no se cierra
