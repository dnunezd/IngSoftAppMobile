# Tasks

## 1. API y repositorio de favoritos (T10-2)

- [x] 1.1 Agregar a `CollectorApi` los métodos Retrofit `GET /collectors/{collectorId}/performers`, `POST /collectors/{collectorId}/bands/{bandId}` y `POST /collectors/{collectorId}/musicians/{musicianId}` con su DTO de performer (`id`, y los campos adicionales que ya expone el backend); verificar que el proyecto compila (`.\gradlew.bat :app:compileDebugKotlin`).
- [x] 1.2 Crear `ArtistaNoDisponible` (excepción dedicada, mismo patrón que `AlbumNoDisponible`) y `FavoriteRepository` con `esFavorito(collectorId, artistId)` y `agregarFavorito(collectorId, artistId, tipo)`, con pruebas unitarias (`FavoriteRepositoryTest`, estilo `MockWebServer` como `AlbumRepositoryTest`) que cubran: favorito encontrado por `id`, no encontrado, 404 al guardar mapeado a `ArtistaNoDisponible`, error de red/servidor mapeado a `ErrorDeCarga`, y cancelación propagada sin convertirse en `ErrorDeCarga`. Verificado con `.\gradlew.bat testDebugUnitTest --tests "com.example.vinilos.data.repository.FavoriteRepositoryTest"` (8/8 OK) y con la suite completa (`.\gradlew.bat testDebugUnitTest`).

## 2. Estado y ViewModel del botón de favorito (T10-3, parte 1)

- [x] 2.1 Crear `FavoritoUiState` sellado (`Comprobando`, `ErrorComprobacion`, `NoFavorito`, `Guardando`, `Favorito(mostrarConfirmacion)`, `ErrorGuardado`, `NoDisponible`) y `FavoritoViewModel(collectorId, artistId, tipo, repositorio)` con `agregarFavorito()`, `reintentarComprobacion()` y `confirmacionMostrada()`, protegido con `Job?`/contador de consulta vigente como `AlbumViewModel`.
- [x] 2.2 Pruebas unitarias del ViewModel (`FavoritoViewModelTest`) cubriendo, una por una: comprobación exitosa (favorito y no favorito), falla de comprobación con reintento, guardado exitoso (estado `Guardando` -> `Favorito`), doble toque ignorado mientras `Guardando`, 404 al guardar -> `NoDisponible`, error de red al guardar -> `ErrorGuardado` con reintento, respuesta tardía de una comprobación anterior que no sobrescribe la vigente, y ocultamiento de la confirmación con `confirmacionMostrada()`. Verificado con `.\gradlew.bat testDebugUnitTest --tests "com.example.vinilos.viewmodel.FavoritoViewModelTest"` (11/11 OK) y con la suite completa (`.\gradlew.bat testDebugUnitTest`).

## 3. Integración en el detalle provisional del artista (T10-3, parte 2)

- [ ] 3.1 Agregar el parámetro `id` (ya disponible en la ruta) y el botón de favorito a `PantallaArtistaProvisional`, con los textos "Agregar Favorito"/"Guardando..."/"Favorito" y el estado desactivado correspondiente; marcar en el comentario del archivo que la integración es temporal (HU04 la reemplaza). Pruebas de Compose UI (`PantallaArtistaProvisionalTest` o archivo nuevo) para cada estado visual, una por una.
- [ ] 3.2 Conectar `FavoritoViewModel` en `VinilosNavHost`: crearlo solo cuando `session.collectorActionsEnabled` es `true` (Coleccionista), usando el `collectorId` de `Session.Collector` y el `tipo`/`id` ya presentes en `Rutas.DetalleArtista`; para Visitante, no crear el ViewModel ni mostrar el botón. Prueba de Compose UI que confirme que el botón no aparece para Visitante.

## 4. Mensajes al usuario (T10-4)

- [ ] 4.1 Mostrar "El artista ya no está disponible" con un botón para volver al listado de artistas (reutiliza `popBackStack`, que ya hace que el listado se recargue sin el artista eliminado); prueba de Compose UI que verifique el mensaje y la navegación de regreso.
- [ ] 4.2 Mostrar "No se pudo guardar el favorito, intenta de nuevo" con el botón disponible para reintentar; prueba de Compose UI para ese estado.
- [ ] 4.3 Mostrar la confirmación transitoria ("<nombre> se agregó a tus favoritos") que desaparece sola tras unos segundos, sin aparecer si el guardado falla; prueba de Compose UI que confirme la aparición y, con `advanceTimeBy`/`mainClock`, su desaparición automática.

## 5. Cierre de T02-3 y documentación

- [ ] 5.1 Correr toda la suite unitaria (`.\gradlew.bat testDebugUnitTest`) y registrar en `design.md`/este `tasks.md` cualquier ajuste de contrato descubierto durante la implementación.
- [ ] 5.2 Documentar en el README o en la wiki del equipo (T10-6 si aplica) qué quedó verificado (unitarias) y qué queda pendiente (E2E, a cargo de otro integrante; diseño visual T10-1; detalle real HU04).

## Workflow follow-up

- Las pruebas E2E de HU10 las implementa otro integrante del equipo; no se incluyen en este change.
- Archivar este change cuando el equipo confirme que T10-2, T10-3 y T10-4 están verificadas y el PR fue revisado.
