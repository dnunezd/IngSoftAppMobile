# Tasks

## 1. Base (dependencias y recursos)

- [x] 1.1 Agregar `io.coil-kt:coil-compose` al catálogo de versiones y a `app/build.gradle.kts`, con una versión compatible con Kotlin 1.9 y el BOM de Compose del proyecto. Verificar que `./gradlew assembleDebug` compila.
- [x] 1.2 Agregar a `strings.xml` los textos de la HU03 y del mockup: "Artistas", "LEYENDAS & BANDAS", "Explora artistas", "Músicos y bandas del catálogo, en orden alfabético.", "Buscar artista por nombre...", el contador de artistas (plural), "Músico", "Banda", "No se encontraron artistas", "No se pudo cargar el listado de artistas" y "Reintentar"; y la imagen por defecto en `res/drawable`.

## 2. Consumo de músicos y bandas (T03-2)

- [x] 2.1 Crear el modelo `Artist` y `ArtistType` (`MUSICO`, `BANDA`), los DTO `MusicianDto` y `BandDto` con solo `id`, `name` e `image`, y `ArtistApi` con `GET musicians` y `GET bands`. Verificar con una prueba que un JSON con campos extra (`albums`, `performerPrizes`, `musicians`) se deserializa ignorándolos.
- [x] 2.2 Escribir primero las pruebas (TDD) e implementar `ArtistRepository.obtenerArtistas()` devolviendo `Result<List<Artist>>`: consulta en paralelo, une y ordena por nombre con `Collator` en español, descarta elementos sin `id` o sin nombre, devuelve error si cualquiera de las dos consultas falla y relanza `CancellationException`. Verificar con pruebas unitarias con `MockWebServer`: ambas con datos, ambas vacías, solo músicos, `id` repetido entre músico y banda, orden con tildes y mayúsculas, error 500 en una de las dos, desconexión y JSON inválido.

## 3. Pantalla y su lógica (T03-3)

- [x] 3.1 Escribir primero las pruebas (TDD) e implementar `ArtistsViewModel` con `ArtistsUiState` (`Cargando`, `Exito`, `Vacio`, `Error`): carga al crearse y `reintentar()` vuelve a `Cargando` y cancela la carga anterior. Verificar con pruebas unitarias y repositorio falso: `Cargando → Exito`, `Cargando → Vacio`, `Cargando → Error`, `Error → Cargando → Exito` y `Error → Cargando → Error`.
- [x] 3.2 Escribir primero las pruebas (TDD) y agregar `buscar(texto)` con la lista `visibles`: filtro por nombre en cualquier parte, sin distinguir mayúsculas ni tildes; texto vacío o de espacios devuelve todos. Verificar con pruebas unitarias: "que" → "Queen", "RUBEN" → "Rubén…", texto vacío, sin coincidencias, y que `buscar` no tiene efecto mientras carga o en error.
- [x] 3.3 Implementar `PantallaArtistas` en `ui.artistas` según el mockup: encabezado con flecha de atrás, etiqueta, titular, subtítulo, barra de búsqueda, encabezado con contador y filas con imagen (Coil, con imagen por defecto), nombre, etiqueta "Músico"/"Banda" y flecha; indicador de carga, mensaje de vacío o sin coincidencias, y error con "Reintentar". Conservar el `testTag` `artistas` en la raíz. Verificar con pruebas de UI de Compose para cada estado y con vistas previas comparadas con el mockup.

## 4. Integración y navegación (T03-4)

- [x] 4.1 Agregar `ArtistRepository` a `AppContainer` (reemplazable en pruebas) y pasarlo a `VinilosNavHost` desde `MainActivity`; crear `ArtistsViewModel` con `viewModelFactory` en el destino `artistas` y eliminar `PantallaArtistasProvisional`.
- [x] 4.2 Agregar el destino provisional `artista/{tipo}/{id}` con el nombre y el tipo del artista y la flecha de atrás; navegar a él al tocar una fila, con `launchSingleTop`. Verificar con pruebas de navegación (`TestNavHostController`): inicio → artistas, artistas → detalle, "Atrás" desde el detalle vuelve al listado sin repetir la consulta y "Atrás" desde el listado vuelve al inicio sin cambiar la sesión. Ajustar `VinilosNavHostTest` e `IngresoE2ETest` para que usen un repositorio de artistas falso donde corresponda.
- [x] 4.3 Verificar en el emulador contra BackVynils: el listado muestra a "Queen" y "Rubén Blades Bellido de Luna" en orden, la búsqueda filtra, una imagen inválida muestra la imagen por defecto, con el backend detenido aparece el error y "Reintentar" recupera el listado, y al girar no se repite la consulta. Ejecutar `./gradlew lint testDebugUnitTest` sin errores.

## 5. Pendiente para la siguiente semana del sprint

- [ ] 5.1 (T03-5) Pruebas E2E con Espresso contra el backend real (abrir, buscar y seleccionar un artista), según la estrategia de pruebas que defina el equipo.
- [ ] 5.2 (T03-6) Actualizar la wiki de la HU03 y los mockups.
