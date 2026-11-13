# Spec Delta

## MODIFIED Requirements

### Requirement: Estados de carga, vacío y error
Mientras se consultan los artistas y no hay un listado guardado en el dispositivo, la pantalla SHALL mostrar un indicador de carga. Si no hay artistas registrados, o ninguno coincide con la búsqueda, MUST mostrar el mensaje "No se encontraron artistas". Si la consulta falla por un problema de conexión o del servidor y no hay un listado guardado, MUST mostrar el mensaje "No se pudo cargar el listado de artistas" y el botón "Reintentar", y la app MUST NOT cerrarse.

#### Scenario: Indicador mientras carga
- **WHEN** el usuario abre el listado, no hay un listado guardado y el backend aún no responde
- **THEN** se muestra un indicador de carga en lugar de una pantalla vacía

#### Scenario: Catálogo sin artistas
- **WHEN** el backend no tiene músicos ni bandas
- **THEN** se muestra "No se encontraron artistas"

#### Scenario: Búsqueda sin coincidencias
- **WHEN** el usuario escribe un texto que no coincide con ningún artista
- **THEN** se muestra "No se encontraron artistas"
- **AND** la barra de búsqueda sigue visible con el texto escrito

#### Scenario: Error de carga
- **WHEN** no hay un listado guardado y la consulta de músicos o la de bandas falla por falta de conexión o un error del servidor
- **THEN** se muestra "No se pudo cargar el listado de artistas" y el botón "Reintentar"
- **AND** no se muestra un listado parcial

#### Scenario: Reintentar tras un error
- **WHEN** el usuario pulsa "Reintentar" y el backend ya responde
- **THEN** se muestra el indicador de carga y luego el listado de artistas

## ADDED Requirements

### Requirement: Listado de artistas guardado en el dispositivo
La app SHALL guardar en el dispositivo el último listado completo de músicos y bandas recibido con éxito y mostrarlo al abrir el listado mientras lo consulta. Cada consulta exitosa MUST reemplazar por completo lo guardado, y lo guardado MUST conservarse al cerrar y volver a abrir la app.

#### Scenario: Abrir sin conexión con listado guardado
- **WHEN** el usuario ya vio el listado de artistas en una apertura anterior de la app
- **AND** lo abre sin conexión o con el servidor caído
- **THEN** se muestran los músicos y bandas guardados en orden alfabético
- **AND** la búsqueda por nombre funciona sobre ellos
- **AND** se informa brevemente "No se pudo actualizar el listado de artistas", una sola vez
- **AND** no se muestra "No se pudo cargar el listado de artistas"

#### Scenario: Carga en curso con listado guardado
- **WHEN** hay un listado guardado y el backend aún no responde
- **THEN** se muestran los artistas guardados con un indicador de actualización
- **AND** no se muestra el indicador de carga de pantalla completa

#### Scenario: Músico y banda con el mismo identificador
- **WHEN** el listado guardado tiene un músico y una banda con el mismo identificador
- **THEN** los dos se muestran sin reemplazarse uno al otro

#### Scenario: Artista eliminado en el backend
- **WHEN** un artista guardado ya no está en la respuesta del backend
- **THEN** deja de mostrarse después de la consulta
- **AND** no vuelve a aparecer en la siguiente apertura sin conexión

#### Scenario: Falla una de las dos consultas
- **WHEN** hay un listado guardado y la consulta de músicos o la de bandas falla
- **THEN** se siguen mostrando los artistas guardados con el aviso
- **AND** lo guardado no cambia

#### Scenario: Falla el almacenamiento del dispositivo
- **WHEN** no se puede leer ni escribir el listado guardado
- **THEN** el listado se comporta como si no hubiera nada guardado
- **AND** la app no se cierra
