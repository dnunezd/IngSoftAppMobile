# Spec Delta

## MODIFIED Requirements

### Requirement: Indicador de progreso al cargar coleccionistas
Mientras se obtiene el listado de coleccionistas y no hay un listado guardado en el dispositivo, la app SHALL mostrar un indicador de progreso en lugar de una pantalla en blanco. Si hay un listado guardado, la app MUST mostrarlo en lugar del indicador de pantalla completa.

#### Scenario: Carga en curso
- **WHEN** el usuario elige "Coleccionista", no hay un listado guardado y la respuesta del backend aún no llega
- **THEN** se muestra un indicador de progreso
- **AND** no se muestra una pantalla en blanco

#### Scenario: Carga en curso con listado guardado
- **WHEN** el usuario elige "Coleccionista", hay un listado guardado y la respuesta del backend aún no llega
- **THEN** se muestran los coleccionistas guardados con un indicador de actualización
- **AND** no se muestra el indicador de progreso de pantalla completa

### Requirement: Error al cargar coleccionistas
Si el listado de coleccionistas no se puede cargar por un problema de conexión o del servidor y no hay un listado guardado en el dispositivo, la app SHALL mostrar el mensaje "No pudimos cargar los coleccionistas" con un botón "Reintentar", MUST NOT cerrarse y MUST mantener disponible la opción "Visitante".

#### Scenario: Falla de conexión
- **WHEN** el usuario elige "Coleccionista", no hay un listado guardado y no hay conexión con el backend
- **THEN** se muestra "No pudimos cargar los coleccionistas" con el botón "Reintentar"
- **AND** la app no se cierra
- **AND** la opción "Visitante" sigue disponible

#### Scenario: Error del servidor
- **WHEN** no hay un listado guardado y el backend responde con un error del servidor al pedir los coleccionistas
- **THEN** se muestra "No pudimos cargar los coleccionistas" con el botón "Reintentar"
- **AND** la app no se cierra

#### Scenario: Recuperación con Reintentar
- **WHEN** el usuario pulsa "Reintentar" tras un error y esta vez el backend responde con coleccionistas
- **THEN** se muestra primero el indicador de progreso y luego el listado de coleccionistas

#### Scenario: Reintento fallido
- **WHEN** el usuario pulsa "Reintentar" y la consulta vuelve a fallar
- **THEN** se vuelve a mostrar el mensaje de error con el botón "Reintentar"

#### Scenario: Visitante tras un error
- **WHEN** se muestra el error de carga de coleccionistas y el usuario elige ingresar como visitante
- **THEN** la app navega a la pantalla de inicio con rol de visitante

## ADDED Requirements

### Requirement: Coleccionistas guardados en el dispositivo
La app SHALL guardar en el dispositivo el último listado de coleccionistas recibido con éxito y mostrarlo al abrir el selector mientras lo consulta. Cada consulta exitosa MUST reemplazar por completo lo guardado, y lo guardado MUST conservarse al cerrar y volver a abrir la app.

#### Scenario: Abrir el selector sin conexión con listado guardado
- **WHEN** el usuario ya vio el listado de coleccionistas en una apertura anterior de la app
- **AND** elige "Coleccionista" sin conexión o con el servidor caído
- **THEN** se muestran los coleccionistas guardados en el mismo orden en que llegaron del backend
- **AND** se informa brevemente "No pudimos actualizar los coleccionistas", una sola vez
- **AND** no se muestra "No pudimos cargar los coleccionistas"

#### Scenario: Ingresar con un coleccionista guardado
- **WHEN** el selector muestra el listado guardado sin conexión
- **AND** el usuario busca y elige un coleccionista
- **THEN** la app navega a la pantalla de inicio con el rol de coleccionista y su nombre

#### Scenario: Listado actualizado con conexión
- **WHEN** hay un listado guardado y el backend responde con coleccionistas distintos
- **THEN** se muestran primero los coleccionistas guardados
- **AND** al terminar la consulta se muestran los de la respuesta

#### Scenario: Coleccionista eliminado en el backend
- **WHEN** un coleccionista guardado ya no está en la respuesta del backend
- **THEN** deja de mostrarse después de la consulta
- **AND** no vuelve a aparecer en la siguiente apertura sin conexión

#### Scenario: El backend responde sin coleccionistas
- **WHEN** hay un listado guardado y el backend responde una lista vacía
- **THEN** se muestra "Aún no hay coleccionistas registrados"
- **AND** la siguiente apertura sin conexión muestra "No pudimos cargar los coleccionistas"

#### Scenario: Falla el almacenamiento del dispositivo
- **WHEN** no se puede leer ni escribir el listado guardado
- **THEN** el selector se comporta como si no hubiera nada guardado
- **AND** la app no se cierra
