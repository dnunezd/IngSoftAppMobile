# Spec Delta

## Purpose

Define cómo se mantiene al día lo que la app guarda en el dispositivo de cada consulta al backend: qué refleja lo guardado, cuándo se escribe y cuándo no, y qué pasa si guardar falla. Cada pantalla define qué guarda; esta capacidad reúne las reglas comunes a todas.

## ADDED Requirements

### Requirement: Lo guardado refleja la última respuesta
Después de cada consulta exitosa que la app guarda en el dispositivo (catálogo y detalle de álbumes, listado y detalle de artistas, coleccionistas y favoritos), lo guardado de esa consulta SHALL quedar igual a la respuesta: los elementos nuevos o modificados se guardan y los que ya no llegan se borran.

#### Scenario: Un álbum cambió en el backend
- **WHEN** el catálogo está guardado y en el backend cambió el nombre de un álbum
- **AND** el usuario desliza para actualizar con conexión
- **THEN** lo guardado tiene el nombre nuevo y los demás álbumes siguen guardados

#### Scenario: Un álbum se borró en el backend
- **WHEN** el catálogo está guardado y en el backend se borró un álbum
- **AND** el catálogo se consulta con éxito
- **THEN** ese álbum y su detalle dejan de estar guardados

#### Scenario: Primera consulta después de abrir la app
- **WHEN** la app se abre con datos guardados de una sesión anterior y la consulta responde con éxito
- **THEN** lo guardado queda igual a la respuesta, incluido el borrado de lo que ya no llega

### Requirement: Una respuesta sin cambios no escribe
Si la respuesta de una consulta es igual a lo que ya está guardado, la app MUST NOT escribir en el almacenamiento del dispositivo. Se reconoce como igual si el backend la identifica con la misma versión que la última respuesta guardada, o si su contenido coincide con lo guardado.

#### Scenario: Recarga sin cambios
- **WHEN** el catálogo ya se guardó en esta sesión y el usuario desliza para actualizar sin que nada haya cambiado en el backend
- **THEN** la app muestra el catálogo y no escribe en el almacenamiento del dispositivo

#### Scenario: Misma información con otra versión
- **WHEN** el backend responde con una versión distinta de la guardada pero con el mismo contenido
- **THEN** la app no escribe en el almacenamiento del dispositivo

#### Scenario: Detalle armado con varias respuestas
- **WHEN** el detalle de un artista con premios se arma con la respuesta del artista y con la de las premiaciones
- **AND** la respuesta del artista no cambió, pero en la de las premiaciones cambió el nombre de un premio de ese artista
- **THEN** la app guarda el detalle del artista con el premio actualizado

### Requirement: Un fallo al guardar se reintenta
Si la app no pudo guardar una respuesta en el dispositivo, la siguiente consulta exitosa de los mismos datos SHALL guardarla, aunque la respuesta no haya cambiado. Un fallo al guardar MUST NOT cambiar lo que se muestra.

#### Scenario: El almacenamiento falla y luego se recupera
- **WHEN** guardar el catálogo falla y el usuario vuelve a consultarlo sin que haya cambiado en el backend
- **THEN** la app guarda el catálogo en esa segunda consulta

#### Scenario: El fallo no afecta la pantalla
- **WHEN** guardar el catálogo falla
- **THEN** el catálogo se muestra con la respuesta del backend, como si se hubiera guardado

### Requirement: Guardar el álbum creado
Cuando el backend confirma la creación de un álbum, la app SHALL guardar ese álbum en el catálogo guardado y, si la respuesta trae los datos completos, también su detalle, sin esperar a la siguiente consulta del catálogo.

#### Scenario: Crear y perder la conexión
- **WHEN** el usuario crea un álbum y se pierde la conexión antes de que se vuelva a consultar el catálogo
- **AND** el usuario abre el catálogo sin conexión
- **THEN** el álbum creado aparece entre los álbumes guardados

#### Scenario: Respuesta de creación incompleta
- **WHEN** el backend confirma la creación pero su respuesta no trae el nombre o el género del álbum
- **THEN** la creación se informa como exitosa y el álbum se guarda cuando llega la siguiente consulta del catálogo
