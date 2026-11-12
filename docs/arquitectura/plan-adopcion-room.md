# Plan de adopción de Room: catálogo de álbumes

**Proyecto:** Vinilos · equipo 9 · **Base:** `develop` (`7dd3337`) · **Fecha:** 9 de noviembre de 2026
**Cambio OpenSpec:** `add-cache-catalogo` (nombre reservado en la sección M4 de `add-crear-album/design.md`)
**Rama sugerida:** `feature/room-cache-catalogo`, creada desde `develop`

## Resumen

Room se adopta **solo en el catálogo de álbumes** (HU01, con el refresco de HU07), como **caché local de solo lectura** de `GET /albums`. La app guarda en el dispositivo la última respuesta completa del catálogo y la muestra al abrir la pantalla mientras consulta el backend. Si la consulta falla, el usuario sigue viendo los álbumes guardados con el aviso que ya existe ("No pudimos actualizar el catálogo"), en lugar de una pantalla de error.

El backend sigue siendo la fuente de verdad: Room nunca recibe escrituras propias de la app, y crear un álbum (`POST /albums`) sigue necesitando conexión.

Las portadas no se guardan en Room: la tabla guarda su URL y la imagen queda a cargo de la caché de Coil que la app ya tiene. Sin conexión se ven las portadas ya vistas, y las demás muestran la ilustración por defecto.

## Por qué el catálogo de álbumes

| Motivo | Evidencia en el proyecto |
|---|---|
| **Resuelve un problema real de uso.** El backend de los tutores está en un plan gratuito de Render que se duerme, y `ServiceAdapter` corta cada consulta a los 10 s. La primera apertura del catálogo con el servidor dormido termina en "No pudimos cargar el catálogo". | `ServiceAdapter.kt` (`TIMEOUT_SEGUNDOS = 10`); README, "Opción A: backend desplegado" |
| **Es la pantalla principal.** El catálogo es el destino de "Ver álbumes" para el Visitante y el Coleccionista, y de él salen el detalle (HU02) y el formulario de creación (HU07). | `VinilosNavHost.kt`; spec `catalogo-albumes` |
| **Los datos tienen forma plana.** Cada fila del catálogo (`AlbumResumen`) tiene seis campos simples y una clave natural (`id`). Basta con una tabla: no hacen falta relaciones, `TypeConverter` ni migraciones complejas. | `data/model/AlbumResumen.kt` |
| **Los estados de la pantalla ya contemplan datos viejos.** `AlbumsUiState.Exito` tiene `actualizando` y `errorAlActualizar`: la pantalla ya sabe mostrar una lista mientras se actualiza y avisar una sola vez si la actualización falla. La caché reutiliza ese comportamiento y no hace falta un estado nuevo. | `viewmodel/AlbumsUiState.kt`; requisito "Actualizar deslizando hacia abajo" |
| **No interfiere con HU07.** Tras crear un álbum, `AlbumsViewModel.albumCreado` vuelve a consultar el catálogo, y esa consulta es la que actualiza la caché. El formulario y la insignia "NUEVO" (en memoria) no cambian. | `AlbumsViewModel.albumCreado`; D11/M4 en `add-crear-album/design.md` |
| **La integración ya se validó.** Room 2.6.1 con KSP `1.9.0-1.0.13` compila con Kotlin 1.9.0, AGP 8.6.0 y Gradle 8.7. Con 274 álbumes, reemplazar la caché tardó de 15 a 26 ms y leerla de 7 a 26 ms en tres dispositivos (incluido API 23). El APK debug pasó de 10,24 a 10,39 MB. | Sección "M4. Room" de `add-crear-album/design.md` |
| **El riesgo es bajo.** El repositorio recibe el DAO como parámetro opcional. Sin DAO se comporta como hoy, así que los repositorios falsos, `AppContainerTest`, `RecuperacionTest` y `CatalogoE2ETest` (que crean su propio `AppContainer`) siguen funcionando sin cambios. | `AppContainer.kt`; `RecuperacionTest.kt` y `CatalogoE2ETest.kt` |

**Por qué solo una entidad.** El Sprint 2 cierra el 15 de noviembre y comparte tiempo con HU04, HU07 y HU10. Adoptar Room en una sola pantalla deja un alcance que se puede terminar, probar y medir de punta a punta, con un antes y un después claros para la entrega. El patrón queda escrito y se puede repetir después en artistas o coleccionistas si el curso lo pide.

## Dónde no se adopta (y por qué)

| Parte de la app | Decisión | Razón |
|---|---|---|
| Detalle de álbum (HU02) | No | Trae listas anidadas (canciones, artistas, comentarios) que exigen relaciones o `TypeConverter`, y los comentarios van a cambiar con la historia de comentar. Más costo y más riesgo de mostrar datos viejos. |
| Listado y detalle de artistas (HU03, HU04) | No por ahora | Encajarían con el mismo patrón, pero quedan fuera para mantener el alcance. Es el siguiente candidato natural (clave `(tipo, id)`). |
| Favoritos (HU10) | No | El spec `favoritos-artista` exige comprobar en el backend cada vez y desactivar el botón si la comprobación falla, para no agregar dos veces. Una caché contradiría ese requisito. |
| Coleccionistas (HU00) | No | Lista pequeña; el beneficio no justifica ampliar el alcance en este sprint. |
| Sesión | No | Que no se guarde entre cierres es una decisión explícita de HU00. Si algún día se guarda, lo indicado es DataStore, no una base de datos. |
| Imágenes de las portadas | No en Room | Room guarda solo la URL. Las imágenes ya las guarda Coil en su caché de memoria y de disco; ver "Portadas e imágenes". |
| Crear álbum (HU07) | No | No se crea una cola de envíos sin conexión ni borradores: el `POST` sigue requiriendo red. |

## Comportamiento esperado

| Situación al abrir el catálogo | Hoy | Con Room |
|---|---|---|
| Sin catálogo guardado, el backend responde | Carga y luego la lista | Igual, y la respuesta queda guardada |
| Sin catálogo guardado, el backend falla | "No pudimos cargar el catálogo" + "Reintentar" | Igual |
| Con catálogo guardado, el backend responde | Carga y luego la lista | Lista guardada al instante con el indicador de actualización; luego la lista nueva |
| Con catálogo guardado, el backend falla o está dormido | "No pudimos cargar el catálogo" | Lista guardada y el aviso "No pudimos actualizar el catálogo" |
| El backend responde una lista vacía | "Aún no hay álbumes en el catálogo" | Igual, y la caché queda vacía |
| Deslizar para actualizar | Consulta y reemplaza la lista | Igual, y además reemplaza la caché |
| Tras crear un álbum (HU07) | Consulta y marca "NUEVO" | Igual; la caché se actualiza con esa consulta |

Reglas de la caché:

- Cada consulta exitosa **reemplaza** la caché completa en una transacción. Así, un álbum borrado en el backend desaparece en la siguiente actualización.
- Solo se guardan álbumes válidos: la conversión tolerante actual (`aResumen`) descarta antes de guardar los que no tienen id, nombre o género.
- El orden se sigue calculando en Kotlin con `ordenCatalogo`, también al leer la caché: el `ORDER BY` de SQLite no ignora tildes ni mayúsculas como el `Collator` en español.
- Si leer o escribir la caché falla, la app sigue como hoy (solo red). La caché es una mejora, nunca una condición para mostrar el catálogo.

## Portadas e imágenes

Con Room el catálogo se puede ver sin conexión, así que hay que definir qué pasa con las portadas. La decisión es **no guardar imágenes en Room** y apoyarse en la caché de Coil, que la app ya tiene.

**Qué guarda cada pieza:**

| Pieza | Qué guarda | Dónde |
|---|---|---|
| Room (`AlbumEntity.portada`) | Solo la URL de la portada, como texto | Base `vinilos.db` |
| Coil 2.6 (`VinilosApplication.newImageLoader`) | Las imágenes ya descargadas: en memoria mientras la app está abierta y en disco entre aperturas | Caché de memoria y carpeta de caché de la app (`image_cache`) |
| `PortadaAlbum` | La ilustración local `inicio_albumes` como `placeholder`, `error` y `fallback` | Recursos del APK |

`VinilosApplication` no configura la caché de disco de Coil, pero Coil 2 la activa por defecto. La app ya la usa hoy sin haberlo decidido de forma explícita.

**Por qué no guardar las imágenes en Room:**

- Una portada pesa de decenas a cientos de KB. Guardarla como `BLOB` hace crecer la base y obliga a leer bytes en cada consulta del catálogo. Además, Android limita a 2 MB lo que una fila puede pasar en una lectura (`CursorWindow`).
- Duplicaría lo que ya hace Coil, que además maneja el tamaño de la caché, la decodificación y el ciclo de vida de cada imagen.
- Las portadas vienen de servidores externos al backend: no son datos del catálogo, son recursos que el catálogo referencia.

**Cómo se verán las portadas sin conexión:**

| Caso | Resultado |
|---|---|
| Portada que el usuario ya vio en una apertura anterior | Coil la toma de su caché de disco y se muestra |
| Portada que nunca se mostró (la cuadrícula solo descarga las tarjetas visibles) | Se muestra la ilustración `inicio_albumes` |
| Álbum sin URL de portada | La ilustración, igual que hoy |
| Portada `http://` externa o con un certificado que Android 6 no acepta | La ilustración, igual que hoy y con o sin conexión: lo bloquean `network_security_config.xml` o el sistema, no la caché |

En todos los casos se cumple el requisito "Portada por defecto": una imagen que no carga nunca rompe la lista.

**Lo que hay que tener en cuenta:**

- **Cabeceras de caché.** Coil respeta por defecto las cabeceras HTTP (`Cache-Control`) de cada servidor de portadas. Si un servidor marca la imagen como no guardable o ya vencida, sin conexión Coil intentará descargarla, fallará y mostrará la ilustración. Este plan no cambia ese comportamiento. Si la evidencia muestra muchas portadas perdidas sin conexión, la opción es `respectCacheHeaders(false)` en el `ImageLoader`. Como el `ImageLoader` es compartido, el cambio afectaría también las fotos de artistas y se decide en equipo.
- **Caché explícita.** Se recomienda declarar la caché de disco en `newImageLoader` (`diskCache { directory(cacheDir.resolve("image_cache")); maxSizePercent(0.02) }`). Son los mismos valores que usa Coil por defecto, pero así la decisión queda escrita en el código y no depende de un valor implícito de la librería.
- **Memoria (M3).** La caché de memoria de Coil es la mayor candidata del consumo de "Gráficos" medido (79,5 MB de 173,2 MB en el Motorola). Ajustarla (`memoryCache { maxSizePercent(...) }`) pertenece al cambio `mejorar-desempeno` y no se mezcla con este, para poder medir cada mejora por separado.
- **Sin descarga anticipada.** No se descargan todas las portadas para tenerlas sin conexión: serían 274 imágenes desde servidores externos, con un gasto de datos que el usuario no pidió.

## Diseño técnico

### Dependencias

Se fija **Room 2.6.1** con **KSP `1.9.0-1.0.13`**, compatibles con Kotlin 1.9.0. No se debe subir Room sin revisar antes su compatibilidad con la versión de Kotlin del proyecto.

```toml
# gradle/libs.versions.toml
[versions]
room = "2.6.1"
ksp = "1.9.0-1.0.13"

[libraries]
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
androidx-room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }

[plugins]
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

```kotlin
// build.gradle.kts (raíz): alias(libs.plugins.ksp) apply false
// app/build.gradle.kts
plugins { alias(libs.plugins.ksp) }

ksp { arg("room.schemaLocation", "$projectDir/schemas") }

dependencies {
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
}
```

El esquema exportado (`app/schemas/`) se versiona en el repositorio.

### Capa local: paquete `data/local`

```kotlin
@Entity(tableName = "albumes")
data class AlbumEntity(
    @PrimaryKey val id: Int,
    val nombre: String,
    val portada: String?,
    val genero: String,
    val artista: String,
    val anio: String?
)

@Dao
abstract class AlbumDao {
    @Query("SELECT * FROM albumes")
    abstract suspend fun todos(): List<AlbumEntity>

    @Query("DELETE FROM albumes")
    abstract suspend fun borrarTodos()

    @Insert
    abstract suspend fun insertar(albumes: List<AlbumEntity>)

    /** La caché es siempre la última respuesta completa del backend. */
    @Transaction
    open suspend fun reemplazar(albumes: List<AlbumEntity>) {
        borrarTodos()
        insertar(albumes)
    }
}

@Database(entities = [AlbumEntity::class], version = 1, exportSchema = true)
abstract class VinilosDb : RoomDatabase() {
    abstract fun albumDao(): AlbumDao

    companion object {
        fun crear(context: Context): VinilosDb =
            Room.databaseBuilder(context, VinilosDb::class.java, "vinilos.db")
                // Es una caché que se reconstruye con la siguiente consulta: no hace falta migrar datos.
                .fallbackToDestructiveMigration()
                .build()
    }
}
```

Los métodos `suspend` del DAO corren en los hilos de Room, no en el hilo principal.

### Repositorio

`AlbumRepository` recibe `albumDao: AlbumDao? = null` y sigue siendo el único que sabe de dónde salen los datos:

```kotlin
open class AlbumRepository(
    private val adaptador: ServiceAdapter,
    private val api: AlbumApi = adaptador.api(),
    private val albumDao: AlbumDao? = null
) {
    /** Catálogo guardado en el dispositivo, ya ordenado; vacío si no hay caché o no se pudo leer. */
    open suspend fun obtenerAlbumesGuardados(): List<AlbumResumen> = ...

    /** Igual que hoy; si la consulta sale bien, además reemplaza la caché. */
    open suspend fun obtenerAlbumes(): Result<List<AlbumResumen>> = ...
}
```

`obtenerAlbum`, `crearAlbum` y el resto del repositorio no cambian.

### ViewModel

Solo cambia la carga inicial de `AlbumsViewModel` (`cargar`). La actualización, el reintento y el aviso de álbum creado se mantienen:

1. Leer `obtenerAlbumesGuardados()`.
2. Si está vacío: `Cargando` y consultar como hoy, con `Error` si falla.
3. Si hay álbumes: mostrar `Exito(guardados, actualizando = true)` y consultar; si falla, `Exito(guardados, errorAlActualizar = true)`.

La guarda `consultaVigente` sigue descartando respuestas viejas.

### Contenedor y aplicación

- `AppContainer` recibe `albumDao: AlbumDao? = null` y se lo pasa a `AlbumRepository`.
- `VinilosApplication` crea la base de datos en `onCreate()`, no al declarar la propiedad: durante la construcción del `Application` todavía no existe un `Context` utilizable. `container` pasa a `lateinit var`, y las pruebas lo siguen reemplazando antes de abrir la Activity.

## Cambios en el spec `catalogo-albumes`

```markdown
## MODIFIED Requirements

### Requirement: Error de carga recuperable
Si el catálogo no se puede cargar por un problema de conexión o del servidor y no hay un catálogo
guardado en el dispositivo, la app SHALL mostrar el mensaje "No pudimos cargar el catálogo" con un
botón "Reintentar", y MUST NOT cerrarse.

## ADDED Requirements

### Requirement: Catálogo guardado en el dispositivo
La app SHALL guardar en el dispositivo la última respuesta completa del catálogo y mostrarla al
abrir el catálogo mientras lo consulta. La consulta exitosa MUST reemplazar lo guardado.

#### Scenario: Abrir sin conexión con catálogo guardado
- **WHEN** el usuario ya vio el catálogo antes y lo abre sin conexión o con el servidor caído
- **THEN** se muestran los álbumes guardados, en orden alfabético
- **AND** se informa "No pudimos actualizar el catálogo"

#### Scenario: Abrir con conexión y catálogo guardado
- **WHEN** hay un catálogo guardado y el backend responde con cambios
- **THEN** se muestran primero los álbumes guardados y luego los actualizados

#### Scenario: Álbum eliminado en el backend
- **WHEN** un álbum guardado ya no está en la respuesta del backend
- **THEN** deja de mostrarse después de la actualización
```

Los demás requisitos (orden, vacío, portada por defecto, giro, detalle y los de HU07) no cambian.

## Pruebas

Se escriben primero, como en el resto del proyecto.

| Nivel | Prueba | Qué cubre |
|---|---|---|
| Unitaria | `AlbumRepositoryTest` con un `AlbumDao` falso en memoria | Guarda tras una consulta exitosa; no toca la caché si falla; lo guardado se devuelve ordenado; sin DAO devuelve una lista vacía; un fallo de la caché no rompe la consulta |
| Unitaria | `AlbumsViewModelTest` | Sin caché y red bien → `Exito`; sin caché y red mal → `Error`; con caché y red bien → `Exito` guardado y luego el nuevo; con caché y red mal → `Exito` con `errorAlActualizar` |
| Instrumentada | `AlbumDaoTest` con `Room.inMemoryDatabaseBuilder` | `reemplazar` borra los álbumes que ya no vienen; leer una base vacía; 274 álbumes |
| E2E | Caso nuevo con `MockWebServer`: primera apertura con red y segunda con el servidor caído | La segunda muestra los álbumes guardados y el aviso |
| Regresión | `RecuperacionTest`, `CatalogoE2ETest` y el resto de la suite | Siguen pasando sin cambios, porque crean su `AppContainer` sin DAO |

El DAO se prueba con pruebas instrumentadas porque el proyecto no tiene Robolectric. Se verifica en el Samsung SM-A356E (adb inalámbrico) y en el emulador API 23. Además hay que comprobar que la CI compila con su JDK 17, porque la validación previa usó JDK 21.

## Tareas

1. [ ] Crear la rama `feature/room-cache-catalogo` desde `develop` y el cambio OpenSpec `add-cache-catalogo` (proposal, design, delta de `catalogo-albumes` y tasks).
2. [ ] Agregar KSP y Room 2.6.1 al catálogo de versiones y a los `build.gradle.kts`; exportar el esquema a `app/schemas/`.
3. [ ] Escribir en rojo `AlbumDaoTest`, los casos nuevos de `AlbumRepositoryTest` y los de `AlbumsViewModelTest`.
4. [ ] Crear `data/local` con `AlbumEntity`, `AlbumDao` y `VinilosDb`.
5. [ ] Agregar el DAO opcional a `AlbumRepository` (`obtenerAlbumesGuardados` y escritura tras éxito).
6. [ ] Ajustar `AlbumsViewModel.cargar` para mostrar lo guardado.
7. [ ] Crear la base en `VinilosApplication.onCreate` y pasar el DAO por `AppContainer`.
8. [ ] Agregar el E2E sin conexión con catálogo guardado.
9. [ ] Declarar la caché de disco de Coil en `VinilosApplication.newImageLoader`, con los valores por defecto, y comprobar a mano cómo se ven las portadas sin conexión.
10. [ ] Verificar: unitarias, lint sin avisos nuevos, compilación con JDK 17 e instrumentadas en el Samsung y en API 23.
11. [ ] Medir y guardar la evidencia en `docs/pruebas/sprint-2/room/` (ver abajo).
12. [ ] Actualizar el README, la wiki y la sección M4 de `add-crear-album/design.md`, que dice que Room no está en el enunciado de la Iteración 2.

## Evidencia para la entrega

| Medida | Cómo se obtiene |
|---|---|
| Catálogo sin conexión | Capturas antes y después: abrir el catálogo, activar el modo avión y volver a entrar. Antes se ve el error; después, la lista guardada con el aviso. |
| Portadas sin conexión | En la misma prueba en modo avión: cuántas portadas visibles salen de la caché de Coil y cuántas muestran la ilustración, comparando las ya vistas con las nunca vistas. |
| Backend dormido | Abrir el catálogo con Render dormido: antes, error a los 10 s; después, la lista guardada al instante. |
| Tiempo hasta ver álbumes | Tiempo desde "Ver álbumes" hasta la primera tarjeta, con y sin caché, mediana de 3 corridas en el Samsung. |
| Costo | Tamaño del APK debug antes y después, y tiempos de lectura y escritura de `AlbumDaoTest` con 274 álbumes. |

Room no reduce la memoria: no se presenta como evidencia de la mejora M3 (memoria).

## Riesgos y mitigaciones

| Riesgo | Mitigación |
|---|---|
| Se muestra un álbum que ya se borró en el backend | La caché se reemplaza completa en cada consulta exitosa. Si el usuario lo abre antes, el detalle ya muestra "El álbum ya no está disponible" (`AlbumNoDisponible`, 404). |
| El detalle no funciona sin conexión | Está fuera de alcance: el detalle muestra su propio error con "Reintentar". |
| Cambios futuros en el esquema | `fallbackToDestructiveMigration`: es una caché y se reconstruye con la siguiente consulta. El esquema exportado deja ver cada cambio. |
| Pruebas contaminadas por una caché de corridas anteriores | Las pruebas existentes no usan DAO. Las nuevas usan una base en memoria o limpian la base con `clearAllTables()` al empezar. |
| Incompatibilidad de versiones | Room 2.6.1 y KSP `1.9.0-1.0.13` quedan fijos mientras el proyecto siga en Kotlin 1.9.0. |
| Sin conexión, algunas portadas muestran la ilustración | Es el comportamiento esperado para las portadas nunca vistas o que su servidor marca como no guardables. Se mide en la evidencia y, si hace falta, se evalúa `respectCacheHeaders(false)`. |
| Un error de disco tumba el catálogo | Los fallos al leer o escribir la caché se ignoran y la app sigue solo con la red. |

## Fuera de alcance

- Caché del detalle de álbum, de artistas, de coleccionistas o de favoritos.
- Crear álbumes sin conexión, colas de envío o borradores.
- Guardar imágenes en Room, descargar portadas por adelantado o ajustar la caché de memoria de Coil (eso último es parte de M3).
- Expiración de la caché por tiempo: siempre se consulta al abrir, así que no hace falta.
- Observar la base con `Flow` (fuente única reactiva): se puede hacer después; este plan mantiene el modelo actual de consultas con `Result`.
