# Design

## Context

Ver `proposal.md` para la motivación y `specs/catalogo-albumes/spec.md` para el comportamiento.

Estado actual en `develop` (`7dd3337`):

- `AlbumRepository.obtenerAlbumes()` consulta `GET /albums` dentro de `ServiceAdapter.llamar`, descarta con `aResumen()` los álbumes sin id, nombre o género y ordena con `ordenCatalogo` (`Collator` en español de fuerza primaria). Devuelve `Result<List<AlbumResumen>>`.
- `AlbumsViewModel` arranca con `cargar()` (`Cargando`, y `Error` si falla). `actualizar()` conserva la lista y marca `errorAlActualizar` si falla. `albumCreado(id)` (HU07) llama a `actualizar()` o a `cargar()`. La guarda `consultaVigente` descarta respuestas viejas.
- `AlbumsUiState` ya tiene `Exito(albumes, actualizando, errorAlActualizar)` y `Vacio(...)`, y la pantalla ya muestra el aviso "No pudimos actualizar el catálogo" una sola vez.
- `VinilosApplication` declara `var container = AppContainer()` como propiedad y construye el `ImageLoader` de Coil con su propio `OkHttpClient`, sin configurar cachés.
- Las pruebas instrumentadas reemplazan `container` con `AppContainer(baseUrl = ...)` o `AppContainer(albumesRepositorio = ...)` antes de abrir la Activity.
- No hay Robolectric: todo lo que necesite un `Context` real se prueba como instrumentada.
- Kotlin 1.9.0, AGP 8.6.0, Gradle 8.7, `minSdk` 23, `compileSdk` 34. La CI usa JDK 17.

La sección "M4. Room" de `add-crear-album/design.md` validó en una copia desechable que Room 2.6.1 con KSP `1.9.0-1.0.13` compila con esta configuración (usando JDK 21) y midió, con 274 álbumes, de 15 a 26 ms para reemplazar y de 7 a 26 ms para leer, en tres dispositivos incluido API 23.

## Goals / Non-Goals

**Goals:**
- Que el catálogo se pueda ver sin conexión y con el backend dormido, con el menor cambio posible en la pantalla y el ViewModel.
- Que la caché sea opcional en el código: sin DAO, la app y las pruebas actuales se comportan exactamente como hoy.
- Dejar la caché de disco de Coil declarada en el código y no implícita.

**Non-Goals:**
- Convertir el catálogo en una fuente única reactiva (`Flow` desde Room).
- Expirar la caché por tiempo: siempre se consulta al abrir, así que no hace falta.
- Cambiar la caché de memoria de Coil ni `respectCacheHeaders` (ver D8).
- Guardar el detalle, artistas, coleccionistas o favoritos.

## Decisions

### D1. Room 2.6.1 con KSP `1.9.0-1.0.13`

Se agregan `room-runtime`, `room-ktx` y `room-compiler` (por `ksp`) en 2.6.1, y el plugin `com.google.devtools.ksp` en `1.9.0-1.0.13`, al catálogo de versiones y a los dos `build.gradle.kts`. Se exporta el esquema con `ksp { arg("room.schemaLocation", "$projectDir/schemas") }` y `app/schemas/` se versiona.

*Alternativa descartada:* una versión más nueva de Room. Exigiría revisar la compatibilidad con Kotlin 1.9.0 y probablemente subir Kotlin, que arrastra el compilador de Compose. Eso es parte de M1 y no de este cambio.

*Alternativa descartada:* KAPT. Está en modo de mantenimiento y es más lento; KSP ya se validó.

*Alternativa descartada:* caché HTTP de OkHttp. No depende de Room, que es lo que pide el curso. Además, solo funciona si el backend envía cabeceras de caché, y la respuesta se volvería a leer con Gson en cada apertura.

*Alternativa descartada:* guardar el JSON en un archivo o en `SharedPreferences`. Exige escribir la lectura y la escritura a mano y no deja un esquema versionado.

### D2. Una tabla con la forma de `AlbumResumen`, reemplazada completa

`AlbumEntity` (`albumes`) tiene `id` (clave primaria), `nombre`, `portada?`, `genero`, `artista` y `anio?`, los mismos campos de `AlbumResumen`. Se guarda lo que ya pasó por `aResumen()`, así que los álbumes incompletos nunca llegan a la base.

`AlbumDao` es una clase abstracta con `todos()`, `borrarTodos()`, `insertar(lista)` y `reemplazar(lista)`, que hace `borrarTodos` e `insertar` en un `@Transaction`. Una lista vacía deja la tabla vacía.

*Alternativa descartada:* `@Upsert` sin borrar. Los álbumes eliminados en el backend quedarían guardados para siempre, y el spec pide que desaparezcan.

*Alternativa descartada:* guardar `AlbumDto` completo (canciones, comentarios, artistas). Exige relaciones o `TypeConverter` para datos que el catálogo no muestra.

*Alternativa descartada:* un DAO `interface` con un método por defecto para la transacción. Con Kotlin 1.9 y `jvmTarget` 1.8 requiere `-Xjvm-default`; la clase abstracta evita ese ajuste.

### D3. El orden se sigue calculando en Kotlin

`todos()` no usa `ORDER BY`. El repositorio ordena lo leído con `ordenCatalogo`, igual que la respuesta de red.

*Alternativa descartada:* `ORDER BY nombre COLLATE NOCASE`. SQLite no ignora tildes, así que "Álbum" quedaría después de "Zeta" y no se cumpliría el requisito "Orden alfabético".

### D4. `AlbumRepository` con DAO opcional y dos funciones

```kotlin
open class AlbumRepository(
    private val adaptador: ServiceAdapter,
    private val api: AlbumApi = adaptador.api(),
    private val albumDao: AlbumDao? = null
)
```

- `obtenerAlbumesGuardados(): List<AlbumResumen>`: lee la tabla, convierte y ordena. Sin DAO devuelve una lista vacía.
- `obtenerAlbumes(): Result<List<AlbumResumen>>`: igual que hoy; si la consulta sale bien, además llama a `reemplazar` antes de devolver.

`obtenerAlbum` y `crearAlbum` no cambian. Los repositorios falsos de `androidTest` (`RepositorioAlbumesFalso` y los de `CatalogoE2ETest`) sobrescriben `obtenerAlbumes` y heredan `obtenerAlbumesGuardados` vacío, así que siguen funcionando.

*Alternativa descartada:* que `obtenerAlbumes` devuelva lo guardado cuando la red falla. El ViewModel no podría saber si debe mostrar el aviso, y cambiaría el contrato que usan `actualizar()` y HU07.

*Alternativa descartada:* un repositorio decorador aparte (`AlbumRepositoryConCache`). Agrega una clase y otra forma de crear el repositorio para el mismo resultado.

### D5. Solo cambia la carga inicial del ViewModel

`cargar()` lanza una sola corrutina con su número de consulta vigente:

1. Lee `obtenerAlbumesGuardados()`.
2. Si está vacío, deja `Cargando` y consulta como hoy, con `Error` si falla.
3. Si hay álbumes, publica `Exito(guardados, actualizando = true)` y consulta. Si sale bien, publica el resultado como hoy (`Exito` o `Vacio`). Si falla, publica `Exito(guardados, errorAlActualizar = true)`.

Para que la lectura y la consulta queden en la misma corrutina, la consulta de `consultar()` se extrae a una función `suspend` que reciben tanto `cargar()` como `actualizar()`. `consultaVigente` se incrementa una sola vez por carga y se comprueba después de cada suspensión, así un `actualizar()` o un `albumCreado()` que llegue durante la lectura gana.

`reintentar()` (solo desde `Error`), `actualizar()`, `avisoMostrado()`, `albumCreado()` y `AlbumsUiState` no cambian. Girar la pantalla no vuelve a cargar, porque el ViewModel sobrevive.

*Alternativa descartada:* un estado nuevo (`DesdeCache`). La pantalla ya sabe mostrar una lista con `actualizando` y avisar con `errorAlActualizar`; un estado nuevo duplicaría esa lógica en Compose.

### D6. La base de datos se crea en `Application.onCreate`

`VinilosDb` (versión 1, `exportSchema = true`) se construye con `Room.databaseBuilder(context, VinilosDb::class.java, "vinilos.db").fallbackToDestructiveMigration()`. `VinilosApplication.container` pasa a `lateinit var` y se asigna en `onCreate()` con `AppContainer(albumDao = db.albumDao())`. `AppContainer` agrega el parámetro `albumDao: AlbumDao? = null` y se lo pasa a `AlbumRepository` solo cuando no recibe `albumesRepositorio`.

Se crea en `onCreate()` porque los inicializadores de propiedades del `Application` se ejecutan antes de `attachBaseContext`, cuando el `Context` todavía no se puede usar. Las pruebas instrumentadas reemplazan `container` después de `onCreate()`, como hoy.

`fallbackToDestructiveMigration` es aceptable porque la tabla es una caché: si cambia el esquema, se borra y la siguiente consulta la vuelve a llenar.

### D7. Un fallo del almacenamiento nunca tumba el catálogo

En `AlbumRepository`, la lectura atrapa `SQLiteException` y devuelve una lista vacía, y la escritura la atrapa y la ignora, devolviendo igual la respuesta de red. `CancellationException` no se atrapa. Así se cumple el escenario "Falla el almacenamiento del dispositivo".

*Alternativa descartada:* convertir el fallo en `ErrorDeCarga`. Mostraría "No pudimos cargar el catálogo" aunque la red haya respondido bien.

### D8. Portadas: caché de disco de Coil explícita, sin BLOB en Room

Room guarda solo la URL. En `newImageLoader` se declara `diskCache { directory(cacheDir.resolve("image_cache")); maxSizePercent(0.02) }`, que son los valores por defecto de Coil 2, para que la decisión quede escrita. Además, `PortadaAlbum` ya usa la imagen genérica como `placeholder`, `error` y `fallback`, lo que cubre el escenario "Portada nunca vista".

Coil sigue respetando las cabeceras de caché de cada servidor de portadas (`respectCacheHeaders` en su valor por defecto). Por eso el spec dice "si el dispositivo aún la conserva".

*Alternativa descartada:* guardar la imagen como `BLOB`. Hace crecer la base, choca con el límite de 2 MB de `CursorWindow` y duplica la caché de Coil.

*Alternativa descartada:* `respectCacheHeaders(false)`. Mostraría más portadas sin conexión, pero cambia el `ImageLoader` compartido con las fotos de artistas. Se decide después con la evidencia (ver Open Questions).

### D9. Estrategia de pruebas

- **Unitarias (JVM):** `AlbumRepositoryTest` usa un `AlbumDao` falso que guarda en una lista (subclase de la clase abstracta) junto con `MockWebServer`. `AlbumsViewModelTest` usa un `AlbumRepository` falso que sobrescribe las dos funciones.
- **Instrumentada:** `AlbumDaoTest` usa `Room.inMemoryDatabaseBuilder` para el reemplazo y la lectura, y una base en archivo que se cierra y se vuelve a abrir para comprobar que lo guardado persiste.
- **E2E:** un caso nuevo con `MockWebServer` y un `AppContainer` con DAO de una base en memoria. Primero se entra al catálogo con red, luego se vuelve al inicio, se apaga el servidor y se vuelve a entrar. Salir del catálogo descarta su ViewModel, así que la segunda entrada vuelve a ejecutar `cargar()` sin reiniciar el proceso.
- **Regresión:** el resto de la suite no se toca. Que siga en verde demuestra que la caché es opcional.

## Risks / Trade-offs

- [Se muestra un álbum ya borrado en el backend hasta la siguiente consulta exitosa] → El reemplazo es completo. Si el usuario lo abre antes, el detalle ya muestra "El álbum ya no está disponible" (`AlbumNoDisponible`, 404).
- [El detalle no funciona sin conexión] → Fuera de alcance. El detalle muestra su propio error con "Reintentar".
- [Room 2.6.1 se quedó atrás] → Queda fijado mientras el proyecto siga en Kotlin 1.9.0. Subir Kotlin y Room va junto en M1.
- [La validación previa usó JDK 21 y la CI usa JDK 17] → Una tarea explícita compila con JDK 17 antes de abrir el PR.
- [Pruebas contaminadas por una base de corridas anteriores] → Las pruebas nuevas usan una base en memoria o llaman a `clearAllTables()` al empezar. Las existentes no usan DAO.
- [Cambiar `container` a `lateinit` rompe algo que lo lea antes de `onCreate`] → Nada lo lee antes: `MainActivity` lo usa en su `onCreate`. Se cubre con la suite instrumentada completa.
- [Sin conexión, algunas portadas muestran la imagen genérica] → Es el comportamiento esperado del spec. Se mide en la evidencia.
- [Otros equipos cambian los datos del backend compartido] → No afecta a la caché: solo guarda lo que el backend devuelve y se reemplaza en cada consulta exitosa.

## Migration Plan

1. Instalar sobre la versión anterior no requiere migración: la base se crea vacía en la primera apertura y se llena con la primera consulta exitosa.
2. Rollback: revertir el cambio. El archivo `vinilos.db` que quede en los dispositivos no se lee y desaparece al desinstalar o al borrar los datos de la app.

## Open Questions

- ¿Conviene `respectCacheHeaders(false)` en el `ImageLoader`? Se responde con la evidencia de portadas sin conexión (tarea 8.2), sin cambiar el spec ni el enfoque.
