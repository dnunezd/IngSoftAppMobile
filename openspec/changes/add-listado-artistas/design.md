# Design

## Context

La HU00 dejó la base técnica de la app: Kotlin con Jetpack Compose, patrón MVVM con `ViewModel` y `UiState` sellado expuesto como `StateFlow`, capa de datos con `Repository` que devuelve `Result`, Retrofit con Gson para el backend, inyección manual con `AppContainer` y un grafo de Navigation Compose. El listado de artistas es hoy el destino provisional `ui/artistas`, al que se llega desde la tarjeta "Listado de Artistas" de la pantalla de inicio.

El backend BackVynils expone los músicos y las bandas en endpoints separados:

- `GET /musicians`: `id`, `name`, `image`, `description`, `birthDate`, `albums`, `performerPrizes`.
- `GET /bands`: `id`, `name`, `image`, `description`, `creationDate`, `albums`, `musicians`, `performerPrizes`.

La arquitectura del Sprint 1 está documentada en la wiki (página Sprint 1, sección Arquitectura) y la rúbrica del curso pide los patrones MVVM, Repository y Service Adapter.

## Goals / Non-Goals

**Goals**

- Un solo listado de músicos y bandas, ordenado alfabéticamente, con búsqueda por nombre y los estados de carga, vacío y error de la HU03.
- Seguir exactamente las capas y convenciones de la HU00, para que el código sea coherente con el diseño arquitectónico de la wiki.
- Lógica (unión, orden, filtro y estados) probada con pruebas unitarias, sin depender de la UI.

**Non-Goals**

- El detalle real del artista (HU04) y los favoritos (HU10).
- Paginación o caché local: el catálogo es pequeño y siempre se consulta al backend.
- Las pruebas E2E contra el backend real (T03-5), que se escriben junto con la estrategia de pruebas.

## Decisions

### D1. Modelo único `Artist` con tipo

`Artist(id: Int, name: String, image: String?, type: ArtistType)`, donde `ArtistType` es `MUSICO` o `BANDA`. La pantalla solo necesita estos campos, y el tipo define la etiqueta "Músico" o "Banda" y la ruta del detalle. Como `id` puede repetirse entre músicos y bandas, la identidad de un artista es el par `(type, id)`, y es la clave de cada fila en la lista.

*Alternativa descartada:* dos listas separadas en la UI. Los criterios piden un solo listado ordenado.

### D2. `ArtistApi` como Service Adapter

`ArtistApi` (Retrofit) declara `GET musicians` y `GET bands` con DTO que solo traen `id`, `name` e `image`; Gson ignora el resto de campos. Es el adaptador de servicio: la única pieza que conoce las rutas y el formato del backend. El `Repository` depende de esta interfaz y no de Retrofit directamente.

### D3. `ArtistRepository` une las dos consultas en paralelo

`obtenerArtistas(): Result<List<Artist>>` lanza las dos consultas en paralelo con `coroutineScope` y `async`, convierte los DTO a `Artist`, descarta los elementos sin `id` o sin nombre y ordena el resultado por nombre. Si **cualquiera** de las dos consultas falla, devuelve un error de carga: un listado parcial (solo músicos o solo bandas) incumpliría el criterio de mostrar ambos y confundiría al usuario. Captura cualquier excepción de red, HTTP o de formato y relanza `CancellationException`, como `CollectorRepository`; a diferencia de este, usa un `catch` general para que ningún error inesperado cierre la app.

**Orden alfabético:** con `java.text.Collator` en español y fuerza primaria, para que "Álvaro" quede junto a "Alberto" y no se distingan mayúsculas.

### D4. `ArtistsViewModel` con `UiState` sellado

`ArtistsUiState`: `Cargando`, `Exito(artistas, consulta)` con la lista `visibles` derivada, `Vacio` y `Error`, igual que `CollectorsUiState`. El `ViewModel` carga al crearse, expone `reintentar()` (vuelve a `Cargando` y cancela la carga anterior) y `buscar(texto)`, que filtra en el dispositivo por nombre sin distinguir mayúsculas ni tildes.

"No se encontraron artistas" cubre dos casos de la HU03: `Vacio` (el backend no tiene artistas) y `Exito` con `visibles` vacío (ninguno coincide con la búsqueda). En el segundo caso la barra de búsqueda sigue visible para corregir el texto.

El `ViewModel` está atado al destino de navegación, así que sobrevive a la rotación sin repetir la consulta; el texto de búsqueda se conserva en el estado.

### D5. Imágenes con Coil

Las imágenes de los artistas son URL externas (Wikimedia y otros). Se usa `AsyncImage` de Coil con un `placeholder` y un `error` que muestran una imagen por defecto (ícono de persona sobre fondo neutro), lo que cumple el criterio de imagen por defecto también cuando `image` es nula o vacía. Coil es la librería de imágenes recomendada para Compose y se integra con las corrutinas que ya usa la app.

*Alternativa descartada:* descargar las imágenes con OkHttp y decodificarlas a mano: más código, sin caché y sin manejo de ciclo de vida.

### D6. Detalle del artista provisional

Se agrega el destino `artista/{tipo}/{id}` con una pantalla provisional que muestra el nombre y el tipo del artista, con la flecha de atrás hacia el listado. La HU04 reemplaza esa pantalla sin cambiar la ruta ni el listado. Seleccionar un artista navega con `launchSingleTop` para evitar destinos repetidos por toques repetidos.

### D7. Inyección e integración

`AppContainer` crea `ArtistRepository` con el mismo `Retrofit` y acepta uno falso para las pruebas instrumentadas, como ya hace con `CollectorRepository`. `VinilosNavHost` recibe el repositorio y crea `ArtistsViewModel` con `viewModelFactory`. La pantalla conserva el `testTag` `artistas` del destino provisional para que las pruebas de navegación existentes sigan llegando a ella.

## Risks / Trade-offs

- **Imágenes externas lentas o caídas:** se mitiga con la imagen por defecto y con Coil, que no bloquea la lista.
- **Conflicto con la HU01:** las dos historias tocan `libs.versions.toml`, `AppContainer`, `VinilosNavHost` y `strings.xml`. Se mitiga con cambios pequeños y localizados, y acordando una sola librería de imágenes.
- **Fallo parcial (una de las dos consultas):** se muestra el error completo con "Reintentar" en lugar de una lista incompleta (D3).
