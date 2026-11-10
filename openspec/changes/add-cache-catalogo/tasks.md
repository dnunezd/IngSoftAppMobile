# Tasks

Cada comportamiento se prueba primero y se comprueba que la prueba falla antes de implementarlo. La verificación en dispositivo se hace en el Samsung SM-A356E por adb inalámbrico. Para compilar por terminal se usa el JDK 17 de la CI (`JAVA_HOME` del README). Rama: `feature/room-cache-catalogo`, creada desde `develop` (`7dd3337`).

## 1. Dependencias (D1)

- [ ] 1.1 Agregar a `gradle/libs.versions.toml` las versiones `room = "2.6.1"` y `ksp = "1.9.0-1.0.13"`, las librerías `room-runtime`, `room-ktx` y `room-compiler` y el plugin `com.google.devtools.ksp`; aplicar el plugin en `build.gradle.kts` (raíz, `apply false`) y en `app/build.gradle.kts`, con `ksp(libs.androidx.room.compiler)` y `ksp { arg("room.schemaLocation", "$projectDir/schemas") }`. Verificar que `.\gradlew.bat assembleDebug testDebugUnitTest` termina en `BUILD SUCCESSFUL` con JDK 17 y que las unitarias actuales siguen en verde.

## 2. Capa local (D2, D3)

- [ ] 2.1 Escribir `AlbumDaoTest` en `androidTest` con `Room.inMemoryDatabaseBuilder`: leer una base vacía, `reemplazar` guarda la lista, un segundo `reemplazar` borra los álbumes que ya no vienen, `reemplazar` con lista vacía deja la tabla vacía, y 274 álbumes se guardan y se leen completos. Agregar un caso con base en archivo que se cierra y se vuelve a abrir y conserva lo guardado. Verificar que no compila por no existir `data.local`.
- [ ] 2.2 Crear `data/local` con `AlbumEntity` (tabla `albumes`), `AlbumDao` (clase abstracta con `todos`, `borrarTodos`, `insertar` y `reemplazar` en `@Transaction`) y `VinilosDb` (versión 1, `exportSchema = true`, `fallbackToDestructiveMigration`). Verificar que `AlbumDaoTest` pasa en el Samsung, que existe `app/schemas/com.example.vinilos.data.local.VinilosDb/1.json` y que queda agregado al repositorio.

## 3. Repositorio (D4, D7)

- [ ] 3.1 Escribir en `AlbumRepositoryTest`, con `MockWebServer` y un `AlbumDao` falso en memoria: una consulta exitosa reemplaza lo guardado con los álbumes válidos; una consulta fallida no toca lo guardado; `obtenerAlbumesGuardados` devuelve lo guardado en el orden de `ordenCatalogo` (incluido un nombre con tilde); sin DAO devuelve una lista vacía y `obtenerAlbumes` se comporta como hoy. Verificar que fallan por no existir el parámetro ni la función.
- [ ] 3.2 Agregar `albumDao: AlbumDao? = null` a `AlbumRepository`, la función `obtenerAlbumesGuardados()` y la escritura tras una consulta exitosa, con la conversión entre `AlbumEntity` y `AlbumResumen`. Verificar con `testDebugUnitTest` que pasan las pruebas de 3.1 y todas las existentes de `AlbumRepositoryTest`.
- [ ] 3.3 Escribir y hacer pasar las pruebas de fallo del almacenamiento: si el DAO falso lanza `SQLiteException` al leer, `obtenerAlbumesGuardados` devuelve una lista vacía; si la lanza al escribir, `obtenerAlbumes` devuelve igual la respuesta de red; una `CancellationException` se relanza. Verificar con `testDebugUnitTest`.

## 4. ViewModel (D5)

- [ ] 4.1 Escribir en `AlbumsViewModelTest`, con un repositorio falso que sobrescribe las dos funciones: sin guardados y red bien pasa de `Cargando` a `Exito`; sin guardados y red mal termina en `Error`; con guardados se publica de inmediato `Exito(guardados, actualizando = true)` y luego la respuesta; con guardados y red mal termina en `Exito(guardados, errorAlActualizar = true)`; con guardados y respuesta vacía termina en `Vacio`; un `actualizar()` o un `albumCreado()` que llega durante la lectura de lo guardado gana. Verificar que las pruebas nuevas fallan.
- [ ] 4.2 Extraer la consulta de `consultar()` a una función `suspend` y cambiar `cargar()` para leer lo guardado primero, sin tocar `AlbumsUiState`, `reintentar`, `actualizar`, `avisoMostrado` ni `albumCreado`. Verificar con `testDebugUnitTest` que pasan las pruebas nuevas y todas las existentes de `AlbumsViewModelTest` (incluidas las de HU07).

## 5. Aplicación, contenedor y portadas (D6, D8)

- [ ] 5.1 Agregar `albumDao: AlbumDao? = null` a `AppContainer` y pasarlo a `AlbumRepository` cuando no se recibe `albumesRepositorio`, con un caso nuevo en `AppContainerTest` que lo compruebe. Verificar con `testDebugUnitTest`.
- [ ] 5.2 Cambiar `VinilosApplication.container` a `lateinit var` y asignarlo en `onCreate()` con la base creada por `VinilosDb`. Verificar que la app abre el catálogo en el Samsung, que la segunda apertura con el modo avión activado muestra los álbumes guardados y el aviso, y que `:app:assembleDebugAndroidTest` compila.
- [ ] 5.3 Declarar en `newImageLoader` la caché de disco de Coil con `directory(cacheDir.resolve("image_cache"))` y `maxSizePercent(0.02)`, y documentar en el KDoc por qué es explícita. Verificar en el Samsung que las portadas y las fotos de artistas siguen cargando con conexión, y que sin conexión las portadas ya vistas se muestran y las demás muestran la imagen genérica.

## 6. E2E sin conexión

- [ ] 6.1 Escribir una E2E con `MockWebServer` y un `AppContainer` con el DAO de una base en memoria: entrar como Visitante y abrir el catálogo con el servidor sano; volver al inicio, apagar el servidor y volver a entrar; comprobar que se ven los álbumes guardados en orden, que se muestra "No pudimos actualizar el catálogo" y que no aparece "No pudimos cargar el catálogo". Agregar un segundo caso en el que el servidor responde un álbum menos y comprobar que ya no aparece. Verificar en el Samsung con `connectedDebugAndroidTest` filtrado por la clase.

## 7. Verificación e integración

- [ ] 7.1 Ejecutar `testDebugUnitTest`, `lint` y la suite instrumentada completa en el Samsung. Verificar `BUILD SUCCESSFUL`, sin fallos y sin avisos de lint nuevos frente a `develop`, lo que confirma que la caché es opcional y que HU00 a HU10 no se afectaron.
- [ ] 7.2 Comprobar en el emulador API 23 la suite instrumentada del catálogo y la apertura sin conexión. Como aquí no se arranca el emulador, pedir esta corrida al usuario o a un compañero y dejar constancia del resultado.

## 8. Evidencia y documentación

- [ ] 8.1 Medir en el Samsung, con mediana de 3 corridas, el tiempo desde "Ver álbumes" hasta la primera tarjeta con y sin catálogo guardado, el tamaño del APK debug antes y después y los tiempos de `AlbumDaoTest` con 274 álbumes; guardar las cifras y las capturas antes y después (modo avión y backend dormido) en `docs/pruebas/sprint-2/room/README.md`. Verificar que el archivo tiene cada medida con su dispositivo y su fecha.
- [ ] 8.2 Contar en la misma prueba en modo avión cuántas portadas visibles se muestran y cuántas muestran la imagen genérica, separando las ya vistas de las nunca vistas, y registrar en `docs/pruebas/sprint-2/room/README.md` si hace falta `respectCacheHeaders(false)` (Open Questions de `design.md`). Verificar que la decisión queda escrita con su motivo.
- [ ] 8.3 Actualizar el README (fila del cambio en el Sprint 2 y stack con Room) y, en `add-crear-album/design.md`, la sección M4 y el "Orden sugerido", que dicen que Room no está en el enunciado de la Iteración 2. Verificar revisando el diff.
- [ ] 8.4 Redactar para el equipo el texto de la wiki sobre la adopción de Room, a partir de `docs/arquitectura/plan-adopcion-room.md` y la evidencia de 8.1 y 8.2. Publicarlo requiere la confirmación del usuario y queda verificado cuando la página lo refleje.

## Workflow follow-up

- Abrir el PR hacia `develop` con al menos dos revisores, según la definición de terminado.
- Archivar este cambio después de `add-crear-album`, porque ambos tienen deltas sobre `catalogo-albumes`.
