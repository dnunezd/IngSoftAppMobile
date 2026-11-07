# Spec Delta

## Purpose

Permite a cualquier usuario, Visitante o Coleccionista, consultar la información detallada de un músico o una banda, con sus integrantes, álbumes y premios, y pasar de ahí al detalle de un álbum.

## ADDED Requirements

### Requirement: Acceso al detalle desde el listado
Al seleccionar un artista del listado de artistas, la app SHALL mostrar el detalle de ese artista, consultado al backend según su tipo y su identificador. La pantalla MUST estar disponible tanto para el Visitante como para el Coleccionista y MUST ofrecer la opción de volver al listado.

#### Scenario: Seleccionar un músico
- **WHEN** el usuario toca la fila del músico "Rubén Blades Bellido de Luna"
- **THEN** la app muestra el detalle de ese músico, consultado con `GET /musicians/{id}`

#### Scenario: Seleccionar una banda
- **WHEN** el usuario toca la fila de la banda "Queen"
- **THEN** la app muestra el detalle de esa banda, consultado con `GET /bands/{id}`

#### Scenario: Músico y banda con el mismo identificador
- **WHEN** un músico y una banda tienen el mismo `id` y el usuario selecciona la banda
- **THEN** la app muestra el detalle de la banda y no el del músico

#### Scenario: Volver al listado
- **WHEN** el usuario está en el detalle de un artista y presiona "Atrás"
- **THEN** la app vuelve al listado de artistas con la misma búsqueda y sin repetir la consulta del listado

### Requirement: Información principal del artista
El detalle SHALL mostrar la imagen, el nombre y la descripción del artista, y una etiqueta que indique "Músico" o "Banda" según su tipo.

#### Scenario: Datos de un artista
- **WHEN** se abre el detalle de la banda "Queen"
- **THEN** se muestran su imagen, el nombre "Queen", su descripción y la etiqueta "Banda"

### Requirement: Fecha de nacimiento del músico
Si el artista es un músico, el detalle SHALL mostrar su fecha de nacimiento en español, tomando el día en UTC para que no cambie según la zona horaria del dispositivo.

#### Scenario: Fecha de nacimiento
- **WHEN** se abre el detalle de un músico con `birthDate` "1948-07-16T00:00:00.000Z"
- **THEN** se muestra la fecha de nacimiento "16 de julio de 1948", incluso en un dispositivo con zona horaria al oeste de UTC

### Requirement: Fecha de creación e integrantes de la banda
Si el artista es una banda, el detalle SHALL mostrar su fecha de creación en español y el listado de sus integrantes con imagen y nombre, indicando cuántos son. Si no tiene integrantes, la sección MUST mostrar el mensaje "Sin integrantes".

#### Scenario: Banda con integrantes
- **WHEN** se abre el detalle de una banda con `creationDate` "1970-01-01T00:00:00.000Z" y dos integrantes
- **THEN** se muestra la fecha de creación "1 de enero de 1970"
- **AND** se muestran los dos integrantes con su imagen y su nombre, con el contador "2 integrantes"

#### Scenario: Banda sin integrantes
- **WHEN** se abre el detalle de una banda sin integrantes
- **THEN** la sección de integrantes muestra "Sin integrantes" y no muestra contador

#### Scenario: Los integrantes no son seleccionables
- **WHEN** el usuario toca un integrante de la banda
- **THEN** la app permanece en el detalle del artista

#### Scenario: El músico no tiene sección de integrantes
- **WHEN** se abre el detalle de un músico
- **THEN** no se muestra la sección de integrantes

### Requirement: Álbumes del artista
El detalle SHALL mostrar el listado de álbumes del artista con portada, nombre y año de lanzamiento, ordenados del más reciente al más antiguo, con los que no tienen fecha al final, e indicando cuántos son. Si el artista no tiene álbumes, la sección MUST mostrar el mensaje "Sin álbumes".

#### Scenario: Artista con álbumes
- **WHEN** el artista tiene los álbumes "Buscando América" (1984) y "Siembra" (1978)
- **THEN** la sección muestra "Buscando América" y luego "Siembra", cada uno con su portada y su año
- **AND** el contador indica "2 álbumes"

#### Scenario: Un solo álbum
- **WHEN** el artista tiene un solo álbum
- **THEN** el contador indica "1 álbum"

#### Scenario: Artista sin álbumes
- **WHEN** el artista no tiene álbumes
- **THEN** la sección de álbumes muestra "Sin álbumes" y no muestra contador

### Requirement: Navegación al detalle del álbum
Al seleccionar un álbum del artista, la app SHALL navegar al detalle de ese álbum. Al volver, la app MUST regresar al detalle del artista sin repetir su consulta.

#### Scenario: Seleccionar un álbum
- **WHEN** el usuario toca el álbum "A Night at the Opera" en el detalle de "Queen"
- **THEN** la app muestra el detalle de ese álbum

#### Scenario: Volver desde el álbum
- **WHEN** el usuario presiona "Atrás" en el detalle del álbum al que llegó desde un artista
- **THEN** la app vuelve al detalle de ese artista sin repetir la consulta

### Requirement: Premios del artista
El detalle SHALL mostrar el listado de premios del artista con el nombre del premio, la organización y la fecha de premiación en español, ordenados del más reciente al más antiguo, con los que no tienen fecha al final, e indicando cuántos son. Si el artista no tiene premios, la sección MUST mostrar el mensaje "Sin premios".

#### Scenario: Artista con premios
- **WHEN** el artista recibió el "Grammy Award" de la "National Academy of Recording Arts & Sciences" con `premiationDate` "1978-12-10T00:00:00.000Z"
- **THEN** la sección muestra el nombre del premio, la organización y "10 de diciembre de 1978"
- **AND** el contador indica "1 premio"

#### Scenario: Orden de los premios
- **WHEN** el artista tiene un premio de 1978 y otro de 1985
- **THEN** se muestra primero el de 1985

#### Scenario: Artista sin premios
- **WHEN** el artista no tiene premios
- **THEN** la sección de premios muestra "Sin premios" y no muestra contador

### Requirement: Imagen por defecto
Si la imagen del artista, la de un integrante o la portada de un álbum falta o no se puede cargar, la pantalla SHALL mostrar una imagen por defecto y el resto del detalle MUST seguir funcionando.

#### Scenario: Imagen del artista que no carga
- **WHEN** la URL de la imagen del artista no responde o no es una imagen válida
- **THEN** se muestra la imagen por defecto junto con el resto de los datos del artista

#### Scenario: Portada sin URL
- **WHEN** un álbum del artista no tiene portada
- **THEN** su fila muestra la imagen por defecto con su nombre y su año

### Requirement: Indicador de carga
Mientras se consulta la información del artista, la pantalla SHALL mostrar un indicador de carga en lugar de una pantalla vacía, con la opción de volver al listado disponible.

#### Scenario: Indicador mientras carga
- **WHEN** el usuario abre el detalle y el backend aún no responde
- **THEN** se muestra un indicador de carga
- **AND** presionar "Atrás" vuelve al listado

### Requirement: Error de carga con reintento
Si el artista ya no existe o la consulta falla por conexión, servidor o respuesta ilegible, la pantalla SHALL mostrar "No se pudo cargar el artista" y el botón "Reintentar", sin datos parciales. La app MUST NOT cerrarse.

#### Scenario: El artista ya no existe
- **WHEN** el backend responde 404 a la consulta del artista
- **THEN** se muestra "No se pudo cargar el artista" y el botón "Reintentar"

#### Scenario: Falla la conexión
- **WHEN** no hay conexión con el backend o este responde con un error del servidor
- **THEN** se muestra "No se pudo cargar el artista" y el botón "Reintentar"

#### Scenario: Falla la consulta de premios
- **WHEN** el artista tiene premios, su consulta responde bien y falla la consulta de los datos de esos premios
- **THEN** se muestra "No se pudo cargar el artista" y el botón "Reintentar"
- **AND** no se muestra un detalle parcial

#### Scenario: Reintentar tras un error
- **WHEN** el usuario pulsa "Reintentar" y el backend ya responde
- **THEN** se muestra el indicador de carga y luego el detalle del artista

### Requirement: Datos incompletos del backend
Un artista sin identificador o sin nombre SHALL tratarse como error de carga. Un álbum o un integrante sin identificador o sin nombre, o un premio cuyo nombre u organización no se puedan obtener, MUST omitirse sin afectar a los demás. Una descripción o una fecha ausente o malformada MUST omitirse sin mostrar texto de relleno.

#### Scenario: Artista sin nombre
- **WHEN** el backend devuelve el artista sin `name`
- **THEN** se muestra "No se pudo cargar el artista" y el botón "Reintentar"

#### Scenario: Elemento incompleto en una sección
- **WHEN** el backend devuelve un álbum sin nombre junto a otros válidos
- **THEN** ese álbum no se muestra, los demás sí, y el contador solo cuenta los visibles

#### Scenario: Premio sin datos disponibles
- **WHEN** una premiación del artista no aparece en la respuesta de `GET /performerprizes`
- **THEN** esa premiación no se muestra y las demás sí

#### Scenario: Fecha malformada
- **WHEN** la fecha de nacimiento de un músico falta o no tiene un formato reconocible
- **THEN** el detalle se muestra sin la fecha de nacimiento

#### Scenario: Álbum sin fecha de lanzamiento
- **WHEN** un álbum del artista no tiene fecha de lanzamiento reconocible
- **THEN** se muestra con su portada y su nombre, sin año, después de los álbumes con fecha

### Requirement: Conservación del estado al rotar
La pantalla SHALL conservar la información cargada al girar el dispositivo, sin volver a consultar al backend.

#### Scenario: Rotación con el detalle cargado
- **WHEN** el usuario gira el dispositivo en el detalle de un artista
- **THEN** se sigue mostrando la misma información y no se repite la consulta
