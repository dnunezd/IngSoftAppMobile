# Spec Delta

## Purpose

Permite a un coleccionista agregar un álbum existente del catálogo a un artista (músico o banda) desde su detalle, para actualizar la información del artista (HU15).

## ADDED Requirements

### Requirement: Botón "Agregar álbum" solo para el Coleccionista
El detalle de un artista SHALL mostrar el botón "Agregar álbum" cuando el usuario ingresó como Coleccionista y el detalle está cargado. Si el usuario ingresó como Visitante, el detalle SHALL NOT mostrar ese botón.

#### Scenario: Coleccionista en el detalle de un artista
- **WHEN** un Coleccionista abre el detalle de un músico o de una banda
- **THEN** se muestra el botón "Agregar álbum"

#### Scenario: Visitante en el detalle de un artista
- **WHEN** un usuario que ingresó como Visitante abre el detalle de un artista
- **THEN** no se muestra el botón "Agregar álbum"

### Requirement: Ventana de selección con el nombre del artista
Al pulsar "Agregar álbum", la app SHALL abrir una ventana que muestre el nombre del artista al que se le va a agregar el álbum, una barra de búsqueda y el listado de álbumes del catálogo con la portada, el nombre y el año de cada uno.

#### Scenario: Abrir la ventana
- **WHEN** el Coleccionista pulsa "Agregar álbum" en el detalle de "Rubén Blades Bellido de Luna"
- **THEN** se abre una ventana con el título "Asociar álbum a Rubén Blades Bellido de Luna"
- **AND** la ventana muestra una barra de búsqueda
- **AND** cada álbum del listado muestra su portada, su nombre y su año

#### Scenario: Álbum sin portada
- **WHEN** un álbum del listado no tiene portada o no se puede cargar
- **THEN** se muestra la ilustración local de álbum en su lugar

### Requirement: Búsqueda por nombre
La barra de búsqueda SHALL filtrar el listado por el nombre del álbum sin distinguir mayúsculas ni tildes. Si ningún álbum coincide, la ventana SHALL mostrar "No encontramos álbumes con esa búsqueda".

#### Scenario: Filtrar por nombre
- **WHEN** el Coleccionista escribe "opera" en la barra de búsqueda
- **THEN** el listado muestra "A Night at the Opera"
- **AND** no muestra los álbumes cuyo nombre no contiene ese texto

#### Scenario: Búsqueda sin resultados
- **WHEN** el Coleccionista escribe un texto que no coincide con ningún álbum
- **THEN** se muestra "No encontramos álbumes con esa búsqueda"

### Requirement: Álbumes ya asociados deshabilitados
Los álbumes que ya están asociados al artista SHALL mostrarse deshabilitados con la etiqueta "Ya asociado" y SHALL NOT poder seleccionarse.

#### Scenario: Álbum ya asociado
- **WHEN** la ventana se abre para un artista que ya tiene asociado "Buscando América"
- **THEN** "Buscando América" se muestra deshabilitado con la etiqueta "Ya asociado"
- **AND** al tocarlo no queda seleccionado

### Requirement: Selección de un solo álbum
La ventana SHALL permitir seleccionar un solo álbum a la vez: elegir otro álbum SHALL reemplazar la selección anterior.

#### Scenario: Cambiar la selección
- **WHEN** el Coleccionista selecciona "A Night at the Opera" y luego "A Day at the Races"
- **THEN** solo "A Day at the Races" queda seleccionado

#### Scenario: La selección se conserva al filtrar
- **WHEN** el Coleccionista selecciona un álbum y luego escribe una búsqueda que lo deja fuera del listado
- **THEN** el álbum sigue seleccionado y el botón "Asociar" sigue habilitado

### Requirement: Botón "Asociar" deshabilitado sin selección
La ventana SHALL mostrar el botón "Asociar", que SHALL permanecer deshabilitado mientras no haya un álbum seleccionado y mientras se está enviando la asociación.

#### Scenario: Sin selección
- **WHEN** la ventana se abre y el Coleccionista no ha seleccionado ningún álbum
- **THEN** el botón "Asociar" está deshabilitado

#### Scenario: Con selección
- **WHEN** el Coleccionista selecciona un álbum disponible
- **THEN** el botón "Asociar" se habilita

#### Scenario: Envío en curso
- **WHEN** el Coleccionista pulsa "Asociar"
- **AND** la respuesta del backend aún no llega
- **THEN** el botón "Asociar" está deshabilitado y no se envía una segunda asociación al tocarlo de nuevo

### Requirement: Asociar el álbum
Al pulsar "Asociar", la app SHALL asociar el álbum seleccionado al artista sin quitarle el álbum a sus otros intérpretes. Si el backend lo confirma, la app SHALL cerrar la ventana, mostrar el mensaje "Álbum asociado" e incluir el álbum en la lista de álbumes del detalle del artista.

#### Scenario: Asociar un álbum a un músico
- **WHEN** el Coleccionista selecciona "A Night at the Opera" en la ventana de "Rubén Blades Bellido de Luna" y pulsa "Asociar"
- **AND** el backend confirma la asociación
- **THEN** la ventana se cierra
- **AND** se muestra el mensaje "Álbum asociado"
- **AND** la lista de álbumes del detalle incluye "A Night at the Opera"

#### Scenario: Asociar un álbum a una banda conserva sus otros intérpretes
- **WHEN** el Coleccionista asocia "Buscando América" a la banda "Queen"
- **THEN** la lista de álbumes de "Queen" incluye "Buscando América"
- **AND** "Buscando América" sigue asociado a "Rubén Blades Bellido de Luna"
- **AND** "Queen" conserva los álbumes que ya tenía

### Requirement: Cancelar sin cambios
La ventana SHALL ofrecer la opción "Cancelar", que SHALL cerrarla sin asociar ningún álbum.

#### Scenario: Cancelar con un álbum seleccionado
- **WHEN** el Coleccionista selecciona un álbum y pulsa "Cancelar"
- **THEN** la ventana se cierra
- **AND** no se envía ninguna asociación y la lista de álbumes del artista no cambia

#### Scenario: Volver a abrir después de cancelar
- **WHEN** el Coleccionista cancela y vuelve a pulsar "Agregar álbum"
- **THEN** la ventana se abre sin álbum seleccionado y con la búsqueda vacía

### Requirement: Sin álbumes disponibles
Si no existe ningún álbum disponible para asociar (el catálogo está vacío o todos sus álbumes ya están asociados al artista), la ventana SHALL mostrar el mensaje "No hay álbumes para asociar" y el botón "Asociar" SHALL quedar deshabilitado.

#### Scenario: Todos los álbumes ya asociados
- **WHEN** el Coleccionista abre la ventana de un artista que ya tiene asociados todos los álbumes del catálogo
- **THEN** se muestra "No hay álbumes para asociar"
- **AND** el botón "Asociar" está deshabilitado

#### Scenario: Catálogo vacío
- **WHEN** el catálogo no tiene álbumes
- **THEN** se muestra "No hay álbumes para asociar"

### Requirement: Error al asociar
Si la asociación falla por un problema de conexión o del servicio, o porque el artista o el álbum ya no existen, la app SHALL mostrar el mensaje "No se pudo asociar el álbum, intenta de nuevo", SHALL mantener la ventana abierta y SHALL conservar la selección para poder reintentar.

#### Scenario: Falla de red al asociar
- **WHEN** el Coleccionista pulsa "Asociar"
- **AND** la petición falla por falta de conexión o un error del servidor
- **THEN** se muestra "No se pudo asociar el álbum, intenta de nuevo"
- **AND** la ventana sigue abierta con el mismo álbum seleccionado
- **AND** no se muestra el mensaje "Álbum asociado"

#### Scenario: Reintentar después del error
- **WHEN** después del error el Coleccionista pulsa "Asociar" de nuevo
- **AND** el backend confirma la asociación
- **THEN** la ventana se cierra y se muestra "Álbum asociado"

### Requirement: Carga y error del listado de álbumes
Mientras se consulta el catálogo, la ventana SHALL mostrar un indicador de carga. Si hay un catálogo guardado en el dispositivo, la ventana SHALL mostrarlo mientras consulta y conservarlo si la consulta falla. Si la consulta falla y no hay catálogo guardado, la ventana SHALL mostrar "No pudimos cargar los álbumes" con la opción "Reintentar".

#### Scenario: Error sin catálogo guardado
- **WHEN** el Coleccionista abre la ventana sin conexión y sin catálogo guardado
- **THEN** se muestra "No pudimos cargar los álbumes" con el botón "Reintentar"

#### Scenario: Sin conexión con catálogo guardado
- **WHEN** el Coleccionista abre la ventana sin conexión y hay un catálogo guardado
- **THEN** la ventana muestra los álbumes guardados sin mensaje de error

### Requirement: Conservar la ventana al rotar
Al rotar la pantalla, la app SHALL conservar la ventana abierta, el texto de búsqueda, el álbum seleccionado y el mensaje de error si lo había, sin volver a consultar el catálogo.

#### Scenario: Rotar con la ventana abierta
- **WHEN** el Coleccionista tiene la ventana abierta con una búsqueda escrita y un álbum seleccionado
- **AND** rota la pantalla
- **THEN** la ventana sigue abierta con la misma búsqueda y el mismo álbum seleccionado
