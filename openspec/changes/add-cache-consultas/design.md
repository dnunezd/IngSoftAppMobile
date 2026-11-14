# Design

## Context

Ver `proposal.md` para la motivación y los specs de este cambio para el comportamiento.

Estado actual en `develop` (`b3a0aaf`, después de la PR #92):

- **Caché existente (`add-cache-catalogo`):** `VinilosDb` versión 1 con una sola tabla, `albumes` (`AlbumEntity`), y `AlbumDao`, una clase abstracta con `todos`, `borrarTodos`, `insertar` y `reemplazar` en `@Transaction`. `AlbumRepositoryImpl` recibe `albumDao: AlbumDao? = null`, guarda tras cada consulta exitosa del catálogo, expone `obtenerAlbumesGuardados()` y atrapa `SQLiteException` al leer y al escribir. `AlbumsViewModel.cargar()` lee lo guardado, publica `Exito(guardados, actualizando = true)` y consulta; si falla, publica `Exito(guardados, errorAlActualizar = true)`. `PantallaCatalogo` muestra el indicador junto al encabezado y el aviso con un `Snackbar`, y llama a `avisoMostrado()` en un `finally`.
- **Arquitectura adoptada (PR #92):** repositorios como interfaces con su `*Impl`; `ServiceAdapter` como único acceso al backend, que reporta el 404 como `RecursoNoEncontrado` (subclase de `ErrorDeCarga`); cada `*Impl` envuelve sus métodos en `withContext(dispatcher)` con `Dispatchers.Default` inyectable; `AppContainer` manual; los ViewModel exponen `StateFlow` y usan `viewModelScope`.
- **Pantallas sin caché:** `CollectorsViewModel`, `AlbumViewModel`, `ArtistsViewModel` y `ArtistaDetalleViewModel` pasan de `Cargando` a `Exito`, `Vacio` o `Error` con una sola consulta. `FavoritoViewModel` pasa de `Comprobando` a `NoFavorito`, `Favorito` o `ErrorComprobacion`.
- **Datos que ya llegan:** `GET /albums` devuelve cada álbum con `tracks`, `performers` y `comments` (`AlbumDto`), pero `AlbumRepositoryImpl.aResumen()` los descarta. `GET /collectors/{id}/performers` devuelve todos los favoritos del coleccionista, y `esFavorito` solo busca un id en esa lista.
- **Pruebas:** no hay Robolectric. Los DAO se prueban como instrumentadas con `Room.inMemoryDatabaseBuilder` (`AlbumDaoTest`) y el repositorio con un DAO falso en memoria (`AlbumRepositoryCacheTest`). Hay unos 30 repositorios falsos que implementan las interfaces explícitamente, sin heredar de la implementación.
- `isMinifyEnabled = false` en todas las variantes. Gson ya es dependencia por `converter-gson`. Kotlin 1.9.0, Room 2.6.1 con KSP, `minSdk` 23.

## Goals / Non-Goals

**Goals:**
- Repetir en cinco pantallas el patrón del catálogo, sin una forma nueva de cargar datos: leer lo guardado, mostrarlo con indicador, consultar y reemplazar.
- Que cada repositorio siga siendo la única pieza que decide qué se guarda y cuándo; los ViewModel solo eligen qué mostrar.
- Que todo siga siendo opcional en el código: sin DAO, cada repositorio se comporta exactamente como hoy.

**Non-Goals:**
- Cambiar el catálogo (HU01), salvo que su consulta ahora también guarda los detalles.
- Una capa genérica de caché (`NetworkBoundResource` o similar) o `Flow` desde Room.
- Tablas normalizadas para las listas anidadas.
- Mostrar cuándo se guardaron los datos.

## Decisions

### D1. `VinilosDb` versión 2 con cinco tablas nuevas y migración destructiva

| Tabla | Clave | Columnas | DAO |
|---|---|---|---|
| `albumes` (existe) | `id` | igual que hoy | `AlbumDao` |
| `album_detalle` | `id` | campos de `Album`; `canciones`, `artistas` y `comentarios` como JSON | `AlbumDao` |
| `coleccionistas` | `id` | `nombre`, `correo`, `posicion` | `ColeccionistaDao` |
| `artistas` | (`tipo`, `id`) | `nombre`, `imagen` | `ArtistaDao` |
| `artista_detalle` | (`tipo`, `id`) | campos de `ArtistaDetalle`; `integrantes`, `albumes` y `premios` como JSON | `ArtistaDao` |
| `favoritos` | (`coleccionistaId`, `artistaId`) | sin más columnas | `FavoritoDao` |

Los DAO son clases abstractas, como `AlbumDao`, para que las transacciones tengan cuerpo sin `-Xjvm-default`. Se exporta `app/schemas/com.example.vinilos.data.local.VinilosDb/2.json` y se versiona. La versión sube a 2 y se mantiene `fallbackToDestructiveMigration()`: al actualizar la app se borra el catálogo guardado una vez y la siguiente consulta lo vuelve a llenar.

- `tipo` se guarda como el nombre del `enum` `ArtistType`, que Room 2.6 convierte solo. La clave compuesta evita que un músico y una banda con el mismo id se pisen.
- `coleccionistas.posicion` guarda el orden en que llegaron del backend, porque hoy el selector no reordena y SQLite no garantiza el orden sin `ORDER BY`.
- Artistas y álbumes se leen sin `ORDER BY` y se ordenan en Kotlin con `ordenAlfabetico()` / `ordenCatalogo()`, porque SQLite no ignora tildes (D3 de `add-cache-catalogo`).

*Alternativa descartada:* una migración manual de la versión 1 a la 2. Conservaría el catálogo guardado al actualizar, pero agrega código y pruebas de migración para datos que son caché y se recuperan con una consulta.

*Alternativa descartada:* una base aparte por pantalla. Multiplica las instancias de Room sin ningún beneficio.

### D2. Listas anidadas como JSON con un conversor de Gson

`data/local/Conversores.kt` declara un `@TypeConverter` de ida y vuelta por cada tipo de lista (`List<Cancion>`, `List<Artista>`, `List<Comentario>`, `List<IntegranteArtista>`, `List<AlbumArtista>`, `List<PremioArtista>`), con `TypeToken` de Gson. `VinilosDb` los registra con `@TypeConverters`. Se guardan las clases de `data.model` tal cual: el detalle siempre se lee y se reemplaza entero.

*Alternativa descartada (decidida con el usuario):* tablas hijas con `@Relation`. Serían seis entidades y DAO más, con claves foráneas y borrado en cascada, para datos que nunca se consultan por separado.

*Alternativa descartada:* clases intermedias solo para el JSON. Aíslan el formato de la base de los cambios en el modelo, pero duplican seis clases. Se mitiga con la regla del riesgo correspondiente.

### D3. Cada interfaz suma solo lecturas de lo guardado

| Interfaz | Función nueva |
|---|---|
| `CollectorRepository` | `obtenerColeccionistasGuardados(): List<Collector>` |
| `AlbumRepository` | `obtenerAlbumGuardado(id: Int): Album?` |
| `ArtistRepository` | `obtenerArtistasGuardados(): List<Artist>` y `obtenerDetalleGuardado(tipo, id): ArtistaDetalle?` |
| `FavoriteRepository` | `esFavoritoGuardado(collectorId, artistId): Boolean` |

Las funciones que consultan el backend no cambian de firma ni de resultado: además escriben en el DAO cuando salen bien. Es el mismo contrato que `obtenerAlbumesGuardados` y `obtenerAlbumes` del catálogo (D4 de `add-cache-catalogo`). Sin DAO, las lecturas devuelven lista vacía, `null` o `false`.

*Alternativa descartada:* implementaciones por defecto en las interfaces, como propone el plan del equipo. Evitarían tocar los repositorios falsos, pero las interfaces de la PR #92 son contratos sin comportamiento, y un falso que olvide la función mostraría "sin caché" sin que nadie lo note. Los falsos implementan las funciones nuevas explícitamente.

*Alternativa descartada:* que la consulta devuelva lo guardado cuando la red falla. El ViewModel no podría saber si debe avisar, y se mezclaría la decisión de qué mostrar con la de qué guardar.

### D4. Qué escribe cada repositorio

| Repositorio | Consulta exitosa | 404 (`RecursoNoEncontrado`) |
|---|---|---|
| `CollectorRepositoryImpl.obtenerColeccionistas` | reemplaza `coleccionistas` | — |
| `AlbumRepositoryImpl.obtenerAlbumes` | reemplaza `albumes` y `album_detalle` en una sola transacción | — |
| `AlbumRepositoryImpl.obtenerAlbum` | inserta o reemplaza la fila de `album_detalle` | borra la fila de `album_detalle` y la de `albumes` |
| `ArtistRepositoryImpl.obtenerArtistas` | reemplaza `artistas` | — |
| `ArtistRepositoryImpl.obtenerDetalle` | inserta o reemplaza la fila de `artista_detalle` | borra esa fila |
| `FavoriteRepositoryImpl.esFavorito` | reemplaza los favoritos de ese coleccionista | — |
| `FavoriteRepositoryImpl.agregarFavorito` | agrega la fila | — |

- **Detalle de álbum desde el catálogo:** `obtenerAlbumes` convierte cada `AlbumDto` dos veces: con `aResumen()`, tolerante, para el catálogo, y con `aModelo()`, estricto, para el detalle. Un álbum que no pasa `aModelo()` (le falta descripción, disquera o una fecha válida) no se guarda como detalle, pero sigue en el catálogo, como hoy. Para eso, la conversión de cada álbum atrapa solo `JsonParseException`, la misma que ya lanza `aModelo()`.
- **El 404 del artista** se sigue devolviendo como hoy (`ErrorDeCarga`, porque `RecursoNoEncontrado` es su subclase), así que el comportamiento sin caché no cambia.
- **Fallos del almacenamiento:** como D7 de `add-cache-catalogo`, toda lectura atrapa `SQLiteException` y devuelve "nada guardado", y toda escritura la atrapa y la ignora. `CancellationException` no se atrapa.
- **Hilos:** todo ocurre dentro del `withContext(dispatcher)` que ya tiene cada método. Las funciones `suspend` de Room hacen su I/O en sus propios hilos, así que `Dispatchers.Default` sigue siendo correcto.

*Alternativa descartada:* consultar `GET /albums/{id}` para cada álbum del catálogo. Serían cientos de peticiones para datos que `GET /albums` ya entrega.

*Alternativa descartada (decidida con el usuario):* precargar el detalle de todos los artistas al abrir el listado. Serían N peticiones más al backend compartido en cada apertura.

### D5. Los ViewModel siguen el patrón de `AlbumsViewModel`

`CollectorsUiState.Exito`, `AlbumUiState.Exito`, `ArtistsUiState.Exito` y `ArtistaDetalleUiState.Exito` suman `actualizando: Boolean = false` y `errorAlActualizar: Boolean = false`, con valores por defecto para que los usos actuales no cambien. Cada ViewModel suma `avisoMostrado()` y su carga queda así:

1. Lee lo guardado. Si no hay nada, publica `Cargando`; si hay, publica `Exito(guardado, actualizando = true)`.
2. Consulta. Si sale bien, publica `Exito` o `Vacio` como hoy.
3. Si falla con `ErrorDeCarga`: con algo guardado, publica `Exito(guardado, errorAlActualizar = true)`; sin nada, `Error`.
4. Excepciones a 3: `AlbumNoDisponible` sigue llevando a `NoDisponible` y `RecursoNoEncontrado` en el detalle de artista lleva a `Error`, haya o no algo guardado.

Una guarda `consultaVigente`, como la de `AlbumViewModel` y `AlbumsViewModel`, descarta resultados de una carga reemplazada por otra (por ejemplo, `cargarAlbum(id)` con otro id durante la lectura). `buscar(texto)` en coleccionistas y artistas copia el `Exito` conservando los indicadores. `reintentar()` vuelve a ejecutar la carga completa, que vuelve a leer lo guardado.

`FavoritoViewModel` no muestra nada guardado mientras comprueba, porque el botón ya está desactivado en `Comprobando`. Solo si la comprobación falla con `ErrorDeCarga`, pregunta `esFavoritoGuardado`: si es `true`, publica `Favorito(mostrarConfirmacion = false)`; si no, `ErrorComprobacion`, como hoy.

*Alternativa descartada:* un estado nuevo `DesdeCache` en cada pantalla. Duplicaría en Compose lo que el catálogo ya resuelve con los dos indicadores.

### D6. Pantallas: indicador y aviso como en el catálogo

Las cuatro pantallas reciben `onAvisoMostrado` y, con `Exito`:

- muestran un indicador pequeño de progreso cuando `actualizando` es `true`, con una descripción de accesibilidad y un `testTag` para las pruebas;
- muestran el aviso con un `Snackbar` cuando `errorAlActualizar` es `true` y llaman a `onAvisoMostrado()` en un `finally`, igual que `PantallaCatalogo`.

Textos nuevos en `strings.xml`, en el estilo de cada error ya existente:

| Pantalla | Aviso |
|---|---|
| Coleccionistas | "No pudimos actualizar los coleccionistas" |
| Detalle de álbum | "No pudimos actualizar el álbum" |
| Listado de artistas | "No se pudo actualizar el listado de artistas" |
| Detalle de artista | "No se pudo actualizar el artista" |

Más cuatro descripciones para el indicador de actualización. El botón de favorito no cambia.

### D7. `AppContainer` recibe los DAO y `VinilosApplication` los crea

`AppContainer` suma `coleccionistaDao`, `artistaDao` y `favoritoDao` (nulos por defecto) junto a `albumDao`, y pasa cada uno a su `*Impl` por nombre solo cuando no recibe un repositorio de reemplazo. `VinilosApplication.onCreate()` los obtiene de la misma instancia de `VinilosDb`. Las pruebas instrumentadas que crean su propio `AppContainer` sin DAO siguen sin caché, como hoy.

*Alternativa descartada:* pasar `VinilosDb` a `AppContainer`. Obligaría a las unitarias de `AppContainerTest` a crear una base de Room, que necesita `Context`.

### D8. Pruebas y orden de los commits

Un commit por pieza, en este orden, con la suite en verde en cada uno:

1. **Pieza común:** `VinilosDb` v2, entidades, DAO, conversores, esquema y pruebas de DAO.
2. **HU02:** repositorio, ViewModel y pantalla del detalle de álbum.
3. **HU00:** selector de coleccionistas.
4. **HU03 y HU04:** listado y detalle de artistas.
5. **HU10:** favoritos.
6. **Evidencia y documentación.**

Cada pieza agrega, con el modelo de las pruebas del catálogo:

- **DAO (instrumentada):** reemplazo, lectura de tabla vacía, lectura y borrado por clave, y ida y vuelta de los conversores. Modelo: `AlbumDaoTest`.
- **Repositorio (unitaria, DAO falso y `MockWebServer`):** guarda tras el éxito, no toca lo guardado si falla, lee en orden, sin DAO se comporta como hoy, tolera `SQLiteException` y borra ante un 404. Modelo: `AlbumRepositoryCacheTest`.
- **ViewModel (unitaria):** con y sin datos guardados, con la red bien y mal, y una carga vigente que gana sobre una lectura atrasada. Modelo: `AlbumsViewModelCacheTest`.
- **E2E sin conexión:** primera apertura con `MockWebServer` sano y segunda con el servidor apagado y un `AppContainer` con base en memoria. Modelo: `CatalogoSinConexionE2ETest`.

`RepositoriosDispatcherTest` se amplía con las lecturas nuevas.

## Risks / Trade-offs

- [Gson ignora la nulabilidad de Kotlin: si cambia un campo de `Cancion`, `PremioArtista` u otra clase guardada como JSON, una fila vieja se leería con `null` en un campo no nulo] → Regla escrita en `Conversores.kt`: cualquier cambio en esas clases sube la versión de `VinilosDb`, lo que borra lo guardado. Las pruebas de ida y vuelta de los conversores cubren cada tipo.
- [Si se activa la minificación, R8 puede renombrar los campos de esas clases y romper el JSON] → Hoy `isMinifyEnabled = false`. Se deja escrito en el design y en `proguard-rules.pro` que, al activarla, hace falta un `-keep` para las clases de `data.model` que se guardan como JSON.
- [La migración destructiva borra el catálogo guardado al actualizar] → Ocurre una sola vez; la siguiente consulta lo llena.
- [Datos viejos en pantalla mientras cambian en el backend compartido] → Cada apertura consulta, y el aviso dice que no se pudo actualizar. El 404 borra lo guardado del álbum o artista.
- [Guardar 274 álbumes con su detalle en una transacción hace más lenta la consulta del catálogo] → Se mide en la prueba del DAO con 274 álbumes y su detalle. La escritura ocurre después de recibir la respuesta y no bloquea el hilo principal.
- [Una fila de detalle con muchas canciones o comentarios supera el límite de 2 MB de `CursorWindow`] → Se lee una fila por id, no la tabla completa. Los álbumes del backend tienen decenas de canciones como mucho.
- [Unos 30 repositorios falsos deben implementar las funciones nuevas] → Es un cambio mecánico, por pieza, que compila o no compila. Los E2E que envuelven el repositorio real delegan con `by real` y no cambian.
- [El backend desplegado respondió 500 el 13 de noviembre] → Las unitarias y las E2E sin conexión usan `MockWebServer`. La verificación con datos reales usa BackVynils local.

## Migration Plan

1. Al instalar la nueva versión, Room detecta el cambio de versión y recrea la base vacía. No hay que hacer nada en el dispositivo.
2. Rollback: revertir los commits. Si un dispositivo vuelve a la app anterior con la base ya en la versión 2, Room puede rechazarla por ser una versión menor que la suya; en ese caso basta con borrar los datos de la app o reinstalarla, porque todo lo guardado es caché.

## Open Questions

- Textos definitivos de los avisos: los de D6 siguen el estilo de cada error actual y se pueden ajustar con el equipo sin cambiar el enfoque ni las tareas.
