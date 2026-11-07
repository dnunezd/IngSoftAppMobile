# Proposal

## Why

Al seleccionar un artista en el listado (HU03), el usuario llega hoy a un destino provisional que solo repite el nombre y el tipo, porque la HU03 lo dejó así a la espera de esta historia. La HU04 (Sprint 2, 3 puntos) completa ese flujo: el usuario, sea Visitante o Coleccionista, necesita conocer la información del artista, sus álbumes y sus premios, y poder saltar de ahí al detalle de un álbum. Además, la HU10 (favoritos, también del Sprint 2) pone su botón sobre esta pantalla, así que debe existir antes.

## What Changes

- Reemplaza `PantallaArtistaProvisional` por la pantalla real del detalle, a la que se llega desde el listado por la misma ruta `artista/{tipo}/{id}`. El parámetro opcional `?nombre=`, que solo existía para el destino provisional, se elimina: el nombre ahora viene de la consulta.
- Muestra imagen, nombre, descripción y etiqueta "Músico" o "Banda" del artista, con la fecha de nacimiento (músico) o la fecha de creación y los integrantes con imagen y nombre (banda).
- Muestra los álbumes del artista con portada, nombre y año; al seleccionar uno se navega a su detalle (HU02).
- Muestra los premios con nombre, organización y fecha de premiación. Como el recurso del artista solo trae el identificador y la fecha de cada premiación, el nombre y la organización se obtienen de `GET /performerprizes`.
- Muestra "Sin álbumes" y "Sin premios" cuando la sección no tiene elementos, e imagen por defecto cuando la imagen del artista o la portada no cargan.
- Maneja el estado de carga y un único estado de error: si el artista ya no existe (404) o falla la consulta, muestra "No se pudo cargar el artista" y "Reintentar". A diferencia de la HU02, el 404 no tiene pantalla propia, porque así lo pide el criterio de aceptación.
- Conserva los datos al girar el dispositivo y al volver del detalle de un álbum, sin repetir la consulta.
- Actualiza las pruebas existentes de HU03 que comprobaban el destino provisional.

Decisiones sobre lo que los criterios no definen, confirmadas por Daniel Nuñez el 2026-11-04 (ver `design.md`):

- Una banda sin integrantes muestra "Sin integrantes", con el mismo patrón de las otras secciones. El caso es real: Queen, la única banda del backend desplegado, no tiene integrantes.
- Los álbumes y los premios se ordenan del más reciente al más antiguo, con los que no tienen fecha al final. Ni los criterios ni Figma fijan un orden, y el del backend no está definido.
- Si falla solo la consulta de premios, se muestra el error completo en lugar de un detalle parcial, igual que el listado de la HU03.
- Solo es error un artista sin `id` o sin nombre. Los elementos incompletos y la fecha o descripción ausente o malformada se omiten.
- El avatar del encabezado de Figma se omite, como en el listado, y se anota como diferencia con el wireframe.
- Los integrantes no son seleccionables, como muestra Figma.

## Capabilities

### New Capabilities
- `detalle-artista`: consulta y presentación de la información detallada de un músico o una banda, con sus integrantes, álbumes y premios, sus estados de carga y error y la navegación hacia el detalle del álbum.

### Modified Capabilities

Ninguna. El requisito "Navegación al detalle del artista" de `listado-artistas` menciona el destino provisional, pero esa capacidad sigue en el cambio sin archivar `add-listado-artistas` y aún no existe en `openspec/specs/`, por lo que no se puede declarar aquí un delta sobre ella. Al archivar ambos cambios, la frase del destino provisional debe quedar retirada de esa spec. El comportamiento que la reemplaza se define en `detalle-artista`.

## Impact

- **Datos** (`data.remote`, `data.model`, `data.repository`): `ArtistApi` agrega `GET /musicians/{id}`, `GET /bands/{id}` y `GET /performerprizes`; nuevos DTOs y modelo de detalle; `ArtistRepository` agrega la consulta del detalle y el cruce con los premios.
- **Estado** (`viewmodel`): nuevo ViewModel con estados de carga, éxito y error, y reintento.
- **Interfaz** (`ui.artistas`): nueva pantalla del detalle; se elimina `PantallaArtistaProvisional` y se comparten con el listado el encabezado, la etiqueta de tipo y el estado de error. Sin dependencias nuevas: las imágenes siguen con Coil.
- **Navegación** (`navigation`): cambia el destino de `Rutas.DetalleArtista` y se le agrega el salto a `Rutas.detalleAlbum`.
- **Pruebas**: unitarias de repositorio y ViewModel, y de navegación. Hay que adaptar `VinilosNavHostTest` y `ArtistasE2ETest` (FN03), que hoy esperan el destino provisional. Las E2E de la historia (T04-5) son responsabilidad de Michael Mora.
- **Documentación**: fila de la HU04 en el README y actualización de la wiki y los mockups (T04-6).
- **Otras historias**: la HU10 (Lorena Nossa) se apoya en esta pantalla; conviene integrar este cambio primero.
- **Fuera de alcance**: el botón de favoritos (HU10), hacer seleccionables a los integrantes de una banda, editar artistas y cambios al backend.
