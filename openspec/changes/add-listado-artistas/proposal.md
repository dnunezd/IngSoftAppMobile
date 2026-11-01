# Proposal

## Why

Desde la pantalla de inicio, la tarjeta "Listado de Artistas" lleva hoy a un destino provisional que solo muestra el título y el rol activo (dejado por la HU00). La HU03 (Consultar el listado de artistas) es una de las cuatro historias del Sprint 1 y la entrada al detalle del artista (HU04, Sprint 2): el usuario, sea Visitante o Coleccionista, necesita ver en un solo listado los músicos y las bandas del catálogo para escoger los que le interesan.

## What Changes

- Consumo de `GET /musicians` y `GET /bands` del backend BackVynils y unión de ambos resultados en un solo listado de artistas (T03-2).
- Nueva pantalla "Artistas" que reemplaza al destino provisional `ui/artistas` (T03-3), según el mockup de la HU03:
  - Encabezado con flecha de atrás y título "Artistas", etiqueta "LEYENDAS & BANDAS", titular "Explora artistas" y subtítulo "Músicos y bandas del catálogo, en orden alfabético.".
  - Barra de búsqueda "Buscar artista por nombre..." que filtra el listado por nombre a medida que se escribe, sin distinguir mayúsculas ni tildes.
  - Encabezado de la lista "Artistas" con el número de artistas visibles.
  - Una fila por artista con su imagen, su nombre y la etiqueta "Músico" o "Banda", ordenadas alfabéticamente por nombre.
  - Imagen por defecto cuando la imagen de un artista no se puede cargar.
  - Indicador de carga mientras se consultan los artistas.
  - Mensaje "No se encontraron artistas" cuando no hay artistas registrados o ninguno coincide con la búsqueda.
  - Mensaje "No se pudo cargar el listado de artistas" con el botón "Reintentar" cuando falla la consulta.
- Al seleccionar un artista, navegación a su detalle (T03-4). Como la HU04 es del Sprint 2, el detalle es un destino **provisional** que muestra el nombre y el tipo del artista elegido; la HU04 lo reemplaza sin tocar el listado.
- La pantalla conserva el listado y la búsqueda al girar el dispositivo, sin repetir la consulta.
- Nueva dependencia de carga de imágenes (Coil) para mostrar las imágenes de los artistas desde una URL.

Fuera de alcance: el detalle real del artista (HU04), agregar artistas favoritos (HU10) y las pruebas E2E contra el backend real (T03-5), que se abordan en la siguiente semana del sprint junto con la estrategia de pruebas.

## Capabilities

### New Capabilities

- `listado-artistas`: consulta, unión, orden, búsqueda y presentación de los músicos y bandas del catálogo, con sus estados de carga, vacío y error, y la selección de un artista para ver su detalle.

### Modified Capabilities

Ninguna. Los requisitos "Ir al listado de artistas" y "Volver a la pantalla de inicio" de `pantalla-inicio` ya cubren el acceso desde el menú principal y el regreso, y no cambian.

## Impact

- **Código nuevo:** `data.model.Artist`, `data.remote.ArtistApi` y sus DTO, `data.repository.ArtistRepository`, `viewmodel.ArtistsViewModel` y `ArtistsUiState`, `ui.artistas.PantallaArtistas` y el destino provisional del detalle del artista.
- **Código modificado:** `AppContainer` (nuevo repositorio), `VinilosNavHost` (pantalla real de artistas y destino del detalle), `MainActivity`, `strings.xml`; se elimina `PantallaArtistasProvisional`.
- **Pruebas existentes:** `VinilosNavHostTest` e `IngresoE2ETest` llegan a la pantalla de artistas por su `testTag`, que se conserva; los que construyen el grafo de navegación necesitan un repositorio de artistas falso.
- **Dependencias:** `io.coil-kt:coil-compose` en el catálogo de versiones. La HU01 (catálogo de álbumes) también muestra imágenes desde URL, así que conviene acordar con su responsable usar la misma librería para no duplicarla.
- **Backend:** solo lectura de `GET /musicians` y `GET /bands`; no cambia nada en BackVynils.
