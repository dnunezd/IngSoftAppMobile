# Design

## Context

Base: `origin/develop` en `e756226` (incluye HU09, #112). La rama `feature/hu15-agregar-album-artista` sale de ese commit.

El detalle del artista (`PantallaDetalleArtista`, HU04) ya carga `ArtistaDetalle`, cuya lista `albumes` (id, nombre, portada, año) sale de `GET /musicians/{id}` o `GET /bands/{id}`. El catálogo (`AlbumRepository.obtenerAlbumes`, HU01) ya entrega `AlbumResumen` con portada, nombre y año, y guarda una copia en Room (`obtenerAlbumesGuardados`). El destino `artista/{tipo}/{id}` de `VinilosNavHost` ya distingue al Coleccionista (crea `FavoritoViewModel`) del Visitante.

El backend local (BackVynils) no se modifica. Comportamiento verificado el 2026-11-19 en un contenedor aislado con `sql/data.sql`:

| Llamada | Resultado |
|---|---|
| `POST /musicians/{id}/albums/{albumId}` | 200, devuelve el álbum. Agrega el músico a los intérpretes sin quitar a nadie; repetirla no duplica. |
| `POST /bands/{id}/albums/{albumId}` | 200, pero deja a la banda como **única** intérprete del álbum (`album.performers = [band]`). |
| `PUT /bands/{id}/albums` con `[{id}, …]` | 200, devuelve la banda. Reemplaza solo las filas de esa banda; los demás intérpretes del álbum se conservan. Con `[]` o `{}` le quita todos los álbumes. |
| `DELETE /musicians|bands/{id}/albums/{albumId}` | 204, pero le quita **todos** los álbumes al artista. |
| Artista o álbum inexistente | 404 con mensaje. |

## Goals / Non-Goals

**Goals:**
- Cumplir los 11 criterios de aceptación de HU15 para músicos y bandas, sin dañar asociaciones existentes.
- Reutilizar los patrones del proyecto: `ServiceAdapter.llamar`, repositorios detrás de interfaces, `UiState` sellados, ViewModel por destino, caché del catálogo y pantallas sin estado con `Tag*`.
- Mantener la estética de la app (paleta de `ui/theme/Color.kt`, tarjetas redondeadas, botones en píldora).

**Non-Goals:**
- Pruebas E2E (T15-5, otro integrante), internacionalización, quitar asociaciones y los elementos decorativos del mockup que no piden los criterios.

## Decisions

### D1. Endpoint según el tipo de artista, detrás de un solo método en `ArtistRepository`
`ArtistRepository` gana `suspend fun asociarAlbum(tipo: ArtistType, idArtista: Int, idAlbum: Int): Result<Unit>`:
- **Músico:** `POST /musicians/{id}/albums/{albumId}`.
- **Banda:** `GET /bands/{id}` justo antes, y `PUT /bands/{id}/albums` con los ids actuales más el nuevo. La lista se lee de la red y no de Room ni del detalle en pantalla, para no borrar asociaciones hechas por otro usuario. La caché HTTP del `ServiceAdapter` siempre revalida (`max-age=0`), así que ese `GET` nunca usa una respuesta vieja: o responde el backend o falla.
- **Salvaguarda:** si el `GET` falla, o la lista resultante no contiene el álbum nuevo o queda vacía, no se envía el `PUT` y se devuelve el error. Nunca se envía una lista vacía.
- Cualquier error (red, 4xx incluido 404, 5xx, JSON) llega como `ErrorDeCarga` y la UI muestra el mismo mensaje del criterio; no hay estado aparte para 404.
- Alternativas descartadas: `POST /bands/.../albums/...` (destructivo) y ocultar el botón en bandas (innecesario: el `PUT` funciona).

Se extiende `ArtistRepository` en vez de crear un repositorio aparte (como hizo HU09 con `ComentarioRepository`) porque la asociación es una operación sobre el artista y así no cambian `AppContainer`, `MainActivity` ni la firma de `VinilosNavHost`, que construyen `VinilosNavHostTest` y la E2E `AlbumDetalleBackendRealTest`. El costo es implementar el método nuevo en los falsos que implementan la interfaz directamente: `RepositorioArtistasFalso` (androidTest), `ArtistRepositoryFalso` de `ArtistaDetalleViewModelTest` y `RepositorioArtistasFalso` de `ArtistsViewModelTest`; y los dos métodos Retrofit en la `ArtistApi` falsa de `RepositoriosDispatcherTest`. Las E2E `ArtistaDetalleE2ETest` y `ArtistasE2ETest` delegan con `by real` y no cambian.

Los métodos Retrofit van en `ArtistApi` (`asociarAlbumAMusico`, `reemplazarAlbumesDeBanda`) porque cuelgan de `/musicians` y `/bands`. El cuerpo del `PUT` es `List<IdAlbumDto>` con `data class IdAlbumDto(val id: Int)`. Las respuestas se leen como `Response<Unit>`/`Response<ResponseBody>`: solo importa el código.

### D2. `AsociarAlbumViewModel` propio, ligado al destino del artista
Se crea en `VinilosNavHost` solo cuando la sesión es `Session.Collector`, con `key = "asociar-$tipo-$id"`, igual que `FavoritoViewModel`. Recibe `tipo`, `idArtista`, `AlbumRepository` y `ArtistRepository`. Por vivir en el `ViewModelStore` de la entrada de navegación sobrevive a la rotación y a la ida y vuelta a un álbum.

Estado (`AsociarAlbumUiState`, una `data class` con un listado sellado):
- `abierta: Boolean`, `busqueda: String`, `seleccionado: Int?`, `asociando: Boolean`, `errorAlAsociar: Boolean`, `asociados: Set<Int>`.
- `listado`: `Cargando` | `Listo(albumes: List<AlbumResumen>)` | `Error`.

Eventos:
- `abrir(asociados: Set<Int>)`: los ids vienen de `ArtistaDetalle.albumes` en pantalla. Limpia búsqueda, selección y error, y carga el catálogo (D3).
- `buscar(texto)`, `seleccionar(id)` (ignorado si el id está en `asociados` o si `asociando`), `cancelar()` (cierra y limpia), `reintentarCarga()`, `asociar()`.
- `asociar()`: ignora toques mientras `asociando`; en éxito cierra la ventana y emite `albumAsociado: AlbumResumen?` (consumible con `asociadoNotificado()`); en fallo deja `errorAlAsociar = true` con la ventana abierta y la selección intacta. Al tocar otro álbum o volver a pulsar "Asociar" se borra el error.

Visitante: no se crea el ViewModel ni se pasa el estado a la pantalla, así que el botón no existe (mismo patrón que favoritos).

### D3. Carga del listado dentro de la ventana
Mismo enfoque que el catálogo (add-cache-consultas):
1. Si hay catálogo guardado (`obtenerAlbumesGuardados()`), se muestra de inmediato.
2. Se consulta `obtenerAlbumes()`; si responde, reemplaza lo mostrado.
3. Si falla y había guardado, se queda con lo guardado sin mostrar error. Si no había nada, `listado = Error` con "Reintentar".

El orden es el del catálogo (por nombre). No se implementa "Orden cronológico".

### D4. Filtro y mensajes de vacío
- Filtro por nombre con `normalizar()` de `Busqueda.kt` (sin tildes ni mayúsculas), sobre todos los álbumes, incluidos los ya asociados.
- **"No hay álbumes para asociar"**: cuando el catálogo está vacío o todos sus álbumes ya están asociados. Si hay ya asociados, se siguen viendo debajo deshabilitados (criterio 4).
- **"No encontramos álbumes con esa búsqueda"**: cuando el texto no coincide con ningún álbum. Se separa del anterior para que la prueba del criterio no pase por accidente con una búsqueda vacía.
- Si el álbum elegido deja de verse (por la búsqueda o porque el catálogo actualizado ya no lo trae), la selección se quita y "Asociar" se deshabilita; `puedeAsociar` además exige que el elegido esté entre los visibles. *Cambio de la revisión del PR #114 (Daniel Nuñez):* la versión inicial conservaba la selección oculta, lo que permitía asociar un álbum que no se veía, y esa escritura no se puede deshacer de forma segura en el backend.
- `visibles` se calcula una sola vez por estado (`by lazy`), porque la ventana lo lee varias veces en cada recomposición.

### D5. Después de asociar: actualizar la discografía sin depender de otra consulta
`ArtistaDetalleViewModel` gana `albumAsociado(album: AlbumResumen)`, con el mismo enfoque que `AlbumViewModel.actualizar()` de HU09 (volver a consultar sin quitar de la vista lo que ya se muestra):
1. Si el estado es `Exito`, agrega el álbum (convertido a `AlbumArtista`) **al final** de `detalle.albumes` si no estaba, y activa `mostrarAlbumAsociado = true` para el aviso. El orden lo fija la consulta del paso 2, con el criterio del repositorio (fecha completa); así hay un solo criterio de orden (revisión del PR #114).
2. Vuelve a consultar `obtenerDetalle(tipo, id)` en segundo plano, lo que también actualiza Room. Si esa consulta falla, se conserva la lista con el álbum agregado y no se muestra el aviso de error de actualización (la asociación sí ocurrió).
3. `albumAsociadoMostrado()` apaga el aviso.

A diferencia de HU09, no hace falta pasar el resultado por `savedStateHandle`: la ventana vive en el mismo destino que el detalle.

`VinilosNavHost` conecta los dos ViewModels: un `LaunchedEffect` observa `albumAsociado` del `AsociarAlbumViewModel`, llama a `ArtistaDetalleViewModel.albumAsociado(...)` y luego a `asociadoNotificado()`. El aviso "Álbum asociado" sale en el `SnackbarHost` que ya tiene `PantallaDetalleArtista`.

La copia en Room del detalle de ese **álbum** (HU02) queda desactualizada hasta que se vuelva a abrir; al abrirlo se muestra lo guardado y se actualiza solo, como ya ocurre.

### D6. Sin conexión
No se guardan asociaciones pendientes para enviarlas después: sin conexión el `POST`/`PUT` falla y se muestra el mensaje de error con la selección conservada. El listado puede venir del catálogo guardado (D3).

### D7. Interfaz y estética
Regla de color, alineada con el PR #113 (`corregir-accesibilidad-scanner`, abierto contra `develop`): los rellenos con texto blanco y los textos naranja usan `MaterialTheme.colorScheme.primary`, no `Naranja` directamente. Hoy `primary` es `Naranja`; con #113 pasa a `NaranjaContraste` (`#D13A12`, ≥ 4,5:1), así que la ventana cumple contraste sin volver a tocarla. El borde y el `RadioButton` de la fila elegida también usan `primary`: `Naranja` sobre `NaranjaSuave` da 2,73:1, por debajo del 3:1 de los componentes de interfaz; con `NaranjaContraste` da 4,0:1 (revisión del PR #114). Ningún texto se atenúa con transparencia.

Pantalla sin estado `VentanaAsociarAlbum(estado, nombreArtista, callbacks…)` en `ui/artistas/VentanaAsociarAlbum.kt`, mostrada con `ModalBottomSheet` de Material 3 (el primero de la app; contenedor `Superficie`, esquinas superiores de 28 dp y la barra de arrastre por defecto). El botón "Agregar álbum" va en el título de la sección de álbumes de `PantallaDetalleArtista`, solo con el detalle cargado.

| Elemento | Criterio | Estilo (paleta de `Color.kt`) |
|---|---|---|
| Botón "Agregar álbum" | 2, 11 | Píldora `primary` con texto blanco e ícono `Add`, alineado a la derecha del título "Álbumes" |
| Título "Asociar álbum a {nombre}" | 1 | `TextoPrincipal`, `titleLarge` en negrita; botón circular de cierre (ícono `Close`) sobre `SuperficieVariante` que equivale a "Cancelar" |
| Barra de búsqueda | 3 | Campo redondeado (`RoundedCornerShape(50)`) con fondo `SuperficieVariante`, ícono `Search`, `Close` para borrar y texto de ayuda "Buscar álbum por título..." |
| Fila de álbum | 3 | Tarjeta redondeada (16 dp) `Superficie` con borde `Contorno`: portada 56 dp con `AsyncImage` y la ilustración local de respaldo, nombre en `TextoPrincipal`, año en `TextoSecundario`, `RadioButton` a la derecha |
| Fila elegida | 5 | Fondo `primaryContainer` (`NaranjaSuave`), borde y `RadioButton` en `primary` |
| Fila ya asociada | 4 | Fondo `SuperficieVariante`, nombre y año en `TextoSecundario` sin transparencia (para conservar el contraste), chip "Ya asociado" en `Superficie`/`TextoSecundario`, `RadioButton` deshabilitado y sin clic |
| Recuadro de error al asociar | 10 | Tarjeta `ErrorFormularioFondo` con ícono `ic_cat_priority_high` y texto `ErrorFormularioTexto` (mismo estilo que `TarjetaError` de HU07); solo el texto del criterio, sin el título "Error de conexión" del mockup |
| Botón "Asociar" | 6, 7 | Píldora `primary` a lo ancho con ícono `ic_crear_enlace`; deshabilitado sin selección; durante el envío muestra un indicador circular y queda inactivo |
| "Cancelar" | 8 | Botón a lo ancho con fondo `SuperficieVariante` y texto `TextoPrincipal` |
| Vacío / sin resultados | 9 | Ilustración `ic_cat_album` en gris con el texto centrado, como `BloqueVacio` del detalle |
| Cargando / error del listado | DoD 5 | Indicador `primary` con "Cargando álbumes..."; error con "No pudimos cargar los álbumes" y botón "Reintentar" |

Accesibilidad (Sprint 3, en la línea de #113): la portada de cada fila es decorativa (`clearAndSetSemantics`), para que TalkBack no lea el nombre dos veces; cada fila es `selectable` con rol `RadioButton` y una descripción que incluye nombre, año y, si aplica, "Ya asociado"; los botones de ícono tienen `contentDescription`; los objetivos táctiles miden al menos 48 dp.

Tags para pruebas: `TagBotonAgregarAlbum`, `TagVentanaAsociar`, `TagTituloAsociar`, `TagBusquedaAsociar`, `TagBotonAsociar`, `TagCancelarAsociar`, `TagErrorAsociar`, `TagVacioAsociar`, `TagSinResultadosAsociar`, `TagCargandoAsociar`, `TagErrorListadoAsociar` y `tagAlbumAsociable(id)`; el chip usa `tagYaAsociado(id)`.

### D8. Textos
Solo en `values/strings.xml`, con el texto literal de la HU: "Agregar álbum", "Asociar álbum a %1$s", "Buscar álbum por título...", "Ya asociado", "Asociar", "Cancelar" (`asociar_cancelar`, propio de la HU15 para no depender de los textos de crear álbum), "Álbum asociado", "No hay álbumes para asociar", "No se pudo asociar el álbum, intenta de nuevo", más los de D4 y D7 que la HU no define.

## Risks / Trade-offs

- **Carrera en bandas** → Entre el `GET` y el `PUT` otro usuario podría asociar un álbum a la misma banda y el `PUT` lo quitaría. La ventana es de milisegundos y el backend no ofrece una alternativa segura; se acepta y se documenta.
- **`PUT` con lista vacía borra los álbumes de la banda** → Salvaguarda de D1 y prueba unitaria que verifica el cuerpo enviado.
- **"Ya asociado" con un detalle guardado desactualizado** → Si el detalle en pantalla viene de Room y otro usuario asoció un álbum, la fila no saldría como "Ya asociado". Para músicos el `POST` repetido no duplica; para bandas el `PUT` usa la lista fresca. No hay daño de datos.
- **Detalle de álbum en Room desactualizado** → Se corrige solo al abrirlo (D5).
- **PR #113 abierto** → Modifica `Color.kt`, `Theme.kt` y `strings.xml`. Usar `primary` evita depender del orden de integración; los textos nuevos van en un bloque propio al final de `strings.xml` para que el rebase sea simple. Si #113 se integra antes, se agrega la ventana a su `AccesibilidadPantallasTest`.
- **Primer `ModalBottomSheet` de la app** → Las pruebas instrumentadas deben esperar la animación (`waitForIdle`/`waitUntil`) y la rotación se cubre en `RotacionTest`.

## Open Questions

Ninguna. Decisiones confirmadas con el usuario el 2026-11-19: músicos y bandas (D1), carga y error con caché (D3), mensaje propio para búsqueda sin resultados (D4), ViewModel para la rotación (D2) y sin asociaciones pendientes sin conexión (D6).
