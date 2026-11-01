# Spec Delta

## ADDED Requirements

### Requirement: Listado unificado de músicos y bandas
La pantalla de artistas SHALL mostrar en un solo listado todos los músicos y todas las bandas registrados en el catálogo, consultados al backend con `GET /musicians` y `GET /bands`. Cada fila MUST mostrar la imagen del artista, su nombre y la etiqueta "Músico" o "Banda" según su tipo.

#### Scenario: Músicos y bandas en el mismo listado
- **WHEN** el backend tiene el músico "Rubén Blades Bellido de Luna" y la banda "Queen"
- **AND** el usuario abre el listado de artistas
- **THEN** el listado muestra ambos artistas
- **AND** "Rubén Blades Bellido de Luna" tiene la etiqueta "Músico" y "Queen" la etiqueta "Banda"

#### Scenario: Artistas con el mismo identificador
- **WHEN** un músico y una banda tienen el mismo `id` en el backend
- **THEN** el listado muestra los dos como artistas distintos

#### Scenario: Elementos incompletos del backend
- **WHEN** el backend devuelve un artista sin `id` o sin nombre
- **THEN** ese elemento no se muestra y el resto del listado sí

### Requirement: Orden alfabético
El listado SHALL mostrar los artistas ordenados alfabéticamente por nombre, sin distinguir mayúsculas, minúsculas ni tildes, sin importar si son músicos o bandas.

#### Scenario: Orden mezclando músicos y bandas
- **WHEN** el catálogo tiene la banda "Queen", el músico "Rubén Blades Bellido de Luna" y el músico "Ástor Piazzolla"
- **THEN** el orden del listado es "Ástor Piazzolla", "Queen", "Rubén Blades Bellido de Luna"

### Requirement: Búsqueda por nombre
La pantalla SHALL mostrar una barra de búsqueda con el texto de ayuda "Buscar artista por nombre..." que filtra el listado por nombre a medida que el usuario escribe. El filtro MUST encontrar el texto en cualquier parte del nombre, sin distinguir mayúsculas, minúsculas ni tildes, y un texto vacío o de solo espacios MUST mostrar todos los artistas. El encabezado de la lista MUST mostrar cuántos artistas están visibles.

#### Scenario: Filtrar mientras se escribe
- **WHEN** el usuario escribe "que" en la barra de búsqueda
- **THEN** el listado muestra solo "Queen"
- **AND** el contador indica 1 artista

#### Scenario: Sin distinguir mayúsculas ni tildes
- **WHEN** el usuario escribe "RUBEN"
- **THEN** el listado muestra "Rubén Blades Bellido de Luna"

#### Scenario: Borrar la búsqueda
- **WHEN** el usuario borra el texto de la barra de búsqueda
- **THEN** el listado vuelve a mostrar todos los artistas

### Requirement: Imagen por defecto
Si la imagen de un artista no se puede cargar, o el artista no tiene imagen, la fila SHALL mostrar una imagen por defecto y el resto del listado MUST seguir funcionando.

#### Scenario: Imagen que no carga
- **WHEN** la URL de la imagen de un artista no responde o no es una imagen válida
- **THEN** la fila de ese artista muestra la imagen por defecto con su nombre y su etiqueta

### Requirement: Estados de carga, vacío y error
Mientras se consultan los artistas, la pantalla SHALL mostrar un indicador de carga. Si no hay artistas registrados, o ninguno coincide con la búsqueda, MUST mostrar el mensaje "No se encontraron artistas". Si la consulta falla por un problema de conexión o del servidor, MUST mostrar el mensaje "No se pudo cargar el listado de artistas" y el botón "Reintentar", y la app MUST NOT cerrarse.

#### Scenario: Indicador mientras carga
- **WHEN** el usuario abre el listado y el backend aún no responde
- **THEN** se muestra un indicador de carga en lugar de una pantalla vacía

#### Scenario: Catálogo sin artistas
- **WHEN** el backend no tiene músicos ni bandas
- **THEN** se muestra "No se encontraron artistas"

#### Scenario: Búsqueda sin coincidencias
- **WHEN** el usuario escribe un texto que no coincide con ningún artista
- **THEN** se muestra "No se encontraron artistas"
- **AND** la barra de búsqueda sigue visible con el texto escrito

#### Scenario: Error de carga
- **WHEN** la consulta de músicos o la de bandas falla por falta de conexión o un error del servidor
- **THEN** se muestra "No se pudo cargar el listado de artistas" y el botón "Reintentar"
- **AND** no se muestra un listado parcial

#### Scenario: Reintentar tras un error
- **WHEN** el usuario pulsa "Reintentar" y el backend ya responde
- **THEN** se muestra el indicador de carga y luego el listado de artistas

### Requirement: Navegación al detalle del artista
Al seleccionar un artista del listado, la app SHALL navegar al detalle de ese artista (HU04). Mientras la HU04 no esté implementada, el detalle MUST ser un destino provisional que muestra el nombre y el tipo del artista elegido, con la opción de volver al listado.

#### Scenario: Seleccionar un artista
- **WHEN** el usuario toca la fila de "Queen"
- **THEN** la app muestra el detalle del artista "Queen" de tipo "Banda"

#### Scenario: Volver al listado
- **WHEN** el usuario está en el detalle de un artista y presiona "Atrás"
- **THEN** la app vuelve al listado de artistas con la misma búsqueda y sin repetir la consulta

### Requirement: Conservación del estado al rotar
La pantalla de artistas SHALL conservar el listado cargado y el texto de búsqueda al girar el dispositivo, sin volver a consultar al backend.

#### Scenario: Rotación con búsqueda
- **WHEN** el usuario escribe "que" y gira el dispositivo
- **THEN** la barra de búsqueda conserva "que", el listado sigue filtrado y no se repite la consulta
