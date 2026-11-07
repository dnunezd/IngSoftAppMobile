# Design

## Context

Ver `proposal.md` para la motivación. La app sigue MVVM con Repository, y todo el acceso de red pasa por `ServiceAdapter`, que convierte fallos de red, HTTP y lectura en `ErrorDeCarga`. El detalle de álbum (`AlbumRepository`, `AlbumViewModel`, `PantallaDetalleAlbum`) es el patrón más cercano. La HU03 ya dejó la ruta `artista/{tipo}/{id}?nombre={nombre}` con el destino `PantallaArtistaProvisional`.

Contrato real comprobado contra el backend desplegado (`https://backvynils-q6yc.onrender.com`, 2026-11-04):

- `GET /musicians/{id}` devuelve `id, name, image, description, birthDate, albums, performerPrizes`.
- `GET /bands/{id}` devuelve `id, name, image, description, creationDate, albums, musicians, performerPrizes`.
- `albums[]` trae `id, name, cover, releaseDate` (y otros campos que no se usan). Un id inexistente responde 404 en ambos recursos.
- `performerPrizes[]` del artista solo trae `id` y `premiationDate`, **sin el premio**. `GET /performerprizes` devuelve todas las premiaciones como `{id, premiationDate, prize: {id, organization, name, description}}`. Se cruzan por el `id` de la premiación, que no es el `id` del premio.

En ese backend, la única banda (Queen) tiene `musicians: []`. La forma de un integrante se comprobó el 2026-11-04 en BackVynils local, asociando temporalmente un músico a Queen: `musicians[]` trae `id, name, image, description, birthDate`, sin `albums` ni `performerPrizes`. La app solo usa `id, name, image`.

## Goals / Non-Goals

**Goals:**
- Reemplazar el destino provisional sin tocar el listado de la HU03 y reutilizando lo que ya existe (adaptador, formato de fecha, Coil, etiqueta de tipo).
- Dejar el estado del detalle independiente de Compose y fácil de probar.
- Dejar un sitio claro para el botón de favoritos de la HU10, sin adelantar su lógica.

**Non-Goals:**
- Hacer seleccionables a los integrantes: ningún criterio lo pide y el mockup no muestra indicador de selección.
- Agregar lógica de favoritos o usar la sesión en esta pantalla (HU10).
- Generalizar `ArtistRepository` o unificarlo con los modelos de `AlbumRepository`.

## Decisions

### D1. Consulta en dos pasos, con premios solo si hacen falta

`ArtistRepository` agrega `obtenerDetalle(tipo, id)`. Primero consulta el recurso según el tipo y, solo si el artista tiene premiaciones, consulta `GET /performerprizes` y las cruza por `id`. Todo corre dentro de una sola llamada a `adaptador.llamar`, así que cualquier fallo es un único `ErrorDeCarga`.

*Alternativa descartada:* consultar ambos recursos siempre en paralelo, como hace el listado. Cargaría el catálogo completo de premiaciones sin necesidad y haría que un artista sin premios fallara por un recurso que no usa. El caso común (sin premios) queda con una sola petición, a costa de una espera secuencial en el otro caso.

*Alternativa descartada:* mostrar el detalle sin premios si `/performerprizes` falla. Una sección "Sin premios" sería falsa, y una lista parcial contradice lo que ya decidió el listado de la HU03.

### D2. Un solo estado de error; el 404 no se distingue

El criterio pide el mismo mensaje y "Reintentar" para "ya no existe" y para "error al cargar". Por eso el repositorio no define algo como `AlbumNoDisponible`, y `ArtistApi` declara los tipos directamente, sin `Response<T>`: `ServiceAdapter` ya convierte el 404 y el cuerpo nulo en `ErrorDeCarga`. El estado es `Cargando | Exito(detalle) | Error`, con `reintentar()` activo solo desde `Error`.

### D3. Modelo de detalle único con campos según el tipo

`ArtistaDetalle` tiene `id, nombre, imagen, descripcion?, tipo, fecha?, integrantes, albumes, premios`. El tipo decide la etiqueta de la fecha ("Fecha de nacimiento" o "Fecha de creación") y si la sección de integrantes aparece. Las dos respuestas se mapean a este único modelo.

*Alternativa descartada:* una jerarquía sellada `Musico | Banda`. La pantalla solo difiere en un rótulo y una sección, así que añadiría ramas sin aportar seguridad real.

El nombre `ArtistaDetalle` evita chocar con `Artista` (los intérpretes de un álbum) y con `Artist` (la fila del listado), que se mantienen como están.

### D4. Conversión tolerante, a diferencia del detalle de álbum

El detalle de álbum exige todos sus campos y falla con cualquiera ausente. Aquí solo son obligatorios `id` y `name` del artista, porque el backend compartido contiene datos de prueba de otros equipos ("pruebaaaaa 11") y un elemento malo no debe tumbar toda la pantalla. Los elementos incompletos se omiten como en el catálogo (HU01) y el listado (HU03). Una fecha que no se pueda interpretar se trata como ausente, capturando el `ParseException` de `formatearFechaLanzamiento`.

Las fechas se formatean con esa función, que ya maneja timestamps ISO en UTC y funciona en API 23; se reutiliza sin renombrarla para no tocar la HU02. El año sale de los cuatro primeros dígitos de la fecha, igual que `anioDe` en `AlbumRepository`.

### D5. Orden del más reciente al más antiguo

Álbumes y premios se ordenan por fecha descendente, con los que no tienen fecha al final, porque el orden del backend no está definido y se necesita uno estable. Es una decisión de este cambio: ningún criterio define el orden. Se aparta de Figma, donde Beatles, Queen, Joni Mitchell y Rubén Blades van en ascendente y Beethoven no, con datos de ejemplo, así que se anota para actualizar los mockups. Los integrantes conservan el orden del backend.

### D6. Una sola columna perezosa con la jerarquía de los mockups

`PantallaDetalleArtista` delega los estados como `PantallaDetalleAlbum`, y el éxito se dibuja en una `LazyColumn`, porque un artista puede tener muchos álbumes (Juanes tiene 15 en el backend). La referencia son las pantallas "HU04 · Detalle de artista" de Figma (archivo Vinilos App), porque la definición de terminado pide coincidir con el wireframe:

- **Tarjeta:** imagen circular, nombre, etiqueta, bloque con ícono de calendario, rótulo en mayúsculas ("FECHA DE NACIMIENTO" o "FECHA DE CREACIÓN") y fecha, y descripción.
- **Integrantes (solo banda):** fila desplazable horizontal con círculo y nombre centrado de hasta dos líneas, sin flecha porque no son seleccionables.
- **Álbumes:** tarjeta con miniatura cuadrada redondeada, nombre, año y flecha.
- **Premios:** tarjeta con trofeo en círculo, nombre, organización y fecha.
- **Sección con elementos:** título con el contador a la derecha.
- **Sección vacía:** solo el título, sin contador, y un bloque gris con ícono y mensaje. "Sin integrantes" usa este mismo bloque, aunque Figma no tiene pantalla para ese caso.
- **Carga:** indicador centrado con "Cargando artista...", una tarjeta de relleno y tres filas de relleno con miniatura cuadrada. Se dibuja aparte, porque `FilaEsqueleto` del listado usa un círculo y otra forma.

Del listado de la HU03 se comparten, sin cambiar su aspecto (pasan de `private` a `internal`, con un parámetro para el texto donde haga falta):

- `Encabezado`: flecha de atrás y título, aquí "Artista". El avatar de Figma se omite, como en el listado, porque ningún criterio le define comportamiento. Se anota como diferencia con el wireframe.
- `Error`: círculo con "!", mensaje y botón con ícono de actualizar.
- `EtiquetaTipo`.

La imagen del artista usa `ic_artista_por_defecto` o `ic_banda_por_defecto`, como el listado. La miniatura del álbum no usa `PortadaAlbum`, cuyo respaldo es la ilustración `inicio_albumes`: Figma pide un ícono de disco sobre fondo gris, así que se usa `AsyncImage` con `ic_cat_album`, el recurso de disco ya existente.

El botón de la HU10 irá a todo el ancho, debajo de la tarjeta principal, según sus pantallas de Figma, sin dejar un parámetro preparado desde ahora.

Las etiquetas `TagDetalleArtista`, `TagNombreDetalleArtista` y `TagTipoDetalleArtista` se mantienen con el mismo valor para minimizar el impacto en las pruebas existentes.

### D7. Ruta y ciclo de vida

`Rutas.DetalleArtista` pasa a `artista/{tipo}/{id}`, sin `?nombre=`. El listado solo deja de pasar el nombre; el resto no cambia. El ViewModel se crea con `viewModel(factory)` dentro del destino, igual que `AlbumViewModel`, de modo que queda ligado a la entrada de navegación: sobrevive a la rotación y a la ida y vuelta al detalle de un álbum, sin repetir la consulta. Seleccionar un álbum usa `launchSingleTop`, como el resto de la app.

### D8. Pruebas

Se sigue el orden de pruebas primero usado en la HU02: la prueba se escribe y se comprueba fallida antes del comportamiento.

- `ArtistRepository` con `MockWebServer`: rutas por tipo, mapeo, cruce de premios, ausencia de la segunda petición sin premios, omisiones, orden, 404, 500, desconexión, fallo solo de premios y cancelación.
- `ArtistaDetalleViewModel`: transiciones, reintento y que un resultado tardío no reemplace la consulta vigente.
- UI: estados, secciones por tipo, mensajes de vacío, contadores y singular/plural.
- Navegación: desde el listado, hacia el álbum y de vuelta, sin repetir consultas.

## Risks / Trade-offs

- **[El criterio de integrantes no se puede comprobar contra el backend desplegado, porque Queen no tiene integrantes]** → El mapeo se cubre con `MockWebServer`, usando la forma comprobada en el backend local. La presentación de los integrantes no tiene prueba automatizada en este cambio. Las E2E (T04-5) deben tenerlo en cuenta y no asumir integrantes en Queen. No se escribe en el backend compartido para crear esos datos.
- **[`GET /performerprizes` trae todas las premiaciones del catálogo]** → Hoy son pocas, y solo se pide cuando el artista tiene premios. El backend no ofrece una consulta por artista.
- **[Desde un álbum que ya no existe, el botón dice "Volver al catálogo" aunque se llegó desde un artista]** → El botón sí regresa a la pantalla anterior (el artista); solo el texto queda impreciso, en un caso poco frecuente. Corregirlo cambiaría la pantalla y las pruebas de la HU02, por lo que queda fuera de este cambio y se reporta al equipo.
- **[`ArtistasE2ETest` (FN03) y `VinilosNavHostTest` dependen del destino provisional]** → Se adaptan en este cambio: deben esperar a que termine la carga antes de comprobar el nombre, y dejar de buscar el texto "Detalle del artista". `RepositorioArtistasFalso` necesita implementar la consulta nueva. Esto toca una suite que mantiene Michael Mora, así que conviene avisarle.
- **[Conflicto con la HU10, que trabaja sobre la misma pantalla en paralelo]** → Integrar primero este cambio y avisar a Lorena Nossa de dónde queda el sitio del botón.
- **[Las pantallas de la HU10 en Figma usan otro diseño del detalle: título "Detalle Del Artista", biografía, conteo de Grammys, discografía en carrusel y el texto "Agregar a Favoritos", mientras su criterio dice "Agregar Favorito"]** → Este cambio sigue las pantallas de la HU04, que además coinciden con sus criterios. Se reporta al equipo para que la HU10 parta de esta pantalla y no de ese diseño.
- **[Los datos de Figma son de ejemplo y no coinciden con el backend: Queen aparece con 4 integrantes y creada el 1 de junio de 1970, y en el backend no tiene integrantes y su fecha es 1 de enero de 1970]** → Las pruebas toman los datos esperados del backend o de datos falsos propios, nunca de Figma.
- **[Mayor carga en API 23 por `LazyColumn` con imágenes]** → Mismo patrón ya verificado en el detalle de álbum y el listado.

## Open Questions

Ninguna. Las decisiones sobre lo que los criterios no definen se confirmaron el 2026-11-04.
