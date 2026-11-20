# Tasks

Implementado con TDD (rojo, verde y refactor por ciclo). Comandos desde PowerShell con `JAVA_HOME` en un JDK 17 o 21. Backend local: `docker compose up -d` en BackVynils (puerto 3000); para escrituras manuales usar un contenedor aislado y recargar `sql/data.sql` al terminar.

## 0. Rama

- [x] 0.1 Crear `feature/hu15-agregar-album-artista` desde `origin/develop` (`e756226`, con HU09). Antes del PR, rebasar sobre `origin/develop` y revisar si #113 ya se integró (D7).

## 1. API y repositorio (T15-2, #53)

- [x] 1.1 Agregar a `ArtistApi` `POST musicians/{id}/albums/{albumId}` (`asociarAlbumAMusico`) y `PUT bands/{id}/albums` (`reemplazarAlbumesDeBanda`, cuerpo `List<IdAlbumDto>`), con `IdAlbumDto(id)` en `data/remote` (D1). Compila con `.\gradlew.bat :app:compileDebugKotlin`.
- [x] 1.2 Agregar `asociarAlbum(tipo, idArtista, idAlbum): Result<Unit>` a `ArtistRepository` e implementarlo en `ArtistRepositoryImpl` con `ServiceAdapter.llamar`: músico con `POST`; banda con `GET bands/{id}` fresco más `PUT` con los ids actuales y el nuevo, sin enviar nunca una lista vacía ni un `PUT` si el `GET` falla (D1). Implementar el método nuevo en `RepositorioArtistasFalso` (androidTest, con resultado configurable y registro de llamadas) y en los falsos de `ArtistaDetalleViewModelTest` y `ArtistsViewModelTest`; agregar los dos métodos Retrofit a la `ArtistApi` falsa de `RepositoriosDispatcherTest`, con un caso de dispatcher para `asociarAlbum`.
- [x] 1.3 Pruebas unitarias en `ArtistRepositoryAsociarTest` (clase propia, para no mezclar su servidor por ruta y método con el de `ArtistRepositoryTest`) con `MockWebServer`: ruta y método del músico; `GET` + cuerpo del `PUT` de la banda con los álbumes actuales más el nuevo (sin duplicar si ya estaba); `GET` fallido sin `PUT`; 404, 500 y desconexión como `ErrorDeCarga`; cancelación propagada; además, banda que llega sin lista de álbumes o con un álbum sin id, sin `PUT`. 13 pruebas.

## 2. Estado y lógica de la ventana (T15-3, #54)

- [x] 2.1 Crear `AsociarAlbumUiState` y `AsociarAlbumViewModel(tipo, idArtista, albumRepository, artistRepository)` (sin dispatcher propio: usa `viewModelScope`, como los demás ViewModels) con `abrir`, `buscar`, `seleccionar`, `cancelar`, `reintentarCarga`, `asociar`, `asociadoNotificado`, el listado filtrado con `normalizar()` y la distinción entre "sin álbumes disponibles" y "sin resultados" (D2, D3, D4).
- [x] 2.2 Pruebas unitarias `AsociarAlbumViewModelTest` (con `MainDispatcherRule`): carga con y sin catálogo guardado y su error con reintento; filtro sin tildes ni mayúsculas; selección única; ya asociado no seleccionable; "Asociar" deshabilitado sin selección; selección conservada al filtrar; vacío vs. sin resultados; éxito que cierra y emite el álbum; error que conserva ventana y selección; reintento; doble toque ignorado; cancelar y reabrir limpio; respuesta tardía de una apertura anterior. 31 pruebas.
- [x] 2.3 En `ArtistaDetalleViewModel`, agregar `albumAsociado(album)` y `albumAsociadoMostrado()`: agrega el álbum a la discografía, activa el aviso y vuelve a consultar el detalle sin mostrar error si esa consulta falla (D5). Pruebas en `ArtistaDetalleViewModelTest` (5 nuevas). El álbum se ubica por año, de más reciente a más antiguo, como la discografía.

## 3. Interfaz (T15-3, #54)

- [x] 3.1 Agregar los textos de D8 en un bloque propio al final de `values/strings.xml` (`<!-- Agregar un álbum a un artista (HU15) -->`).
- [x] 3.2 Crear `ui/artistas/VentanaAsociarAlbum.kt` sin estado, con `ModalBottomSheet`, los estilos, la accesibilidad y los `Tag*` de D7. Previsualizaciones (`@Preview`) para lista con seleccionado y ya asociado, error al asociar, vacío, sin resultados, cargando y error del listado.
- [x] 3.3 En `PantallaDetalleArtista`, agregar parámetros opcionales para el botón "Agregar álbum" (solo si se pasa el estado de la ventana) junto al título de la sección de álbumes, mostrar la ventana y el aviso "Álbum asociado" en el `SnackbarHost` existente.
- [x] 3.4 Pruebas instrumentadas `VentanaAsociarAlbumTest` y `PantallaDetalleArtistaAsociarTest` (en `ui/artistas`): un caso por criterio de aceptación (nombre del artista, botón visible solo con estado, portada/nombre/año, "Ya asociado" deshabilitado, selección única, "Asociar" deshabilitado, "Álbum asociado", "Cancelar", "No hay álbumes para asociar", error con selección conservada, Visitante sin botón) más sin resultados, cargando y error del listado: 19 + 6 pruebas.
  - Pendiente: `enableAccessibilityChecks()` necesita la dependencia `ui-test-junit4-accessibility` que trae #113; al integrarse #113, agregar la ventana a `AccesibilidadPantallasTest` (escalas 1 y 2).
  - Hallazgo en el Nexus 5X (API 23): la ventana enfocaba sola la búsqueda y abría el teclado, que dejaba la lista sin espacio. Se corrigió llevando el foco inicial al título (también lo anuncia TalkBack primero) y las pruebas se desplazan hasta la fila que revisan (`TagListaAsociar`).

## 4. Integración (T15-4, #55)

- [x] 4.1 En `VinilosNavHost`, crear `AsociarAlbumViewModel` solo para `Session.Collector` en el destino `artista/{tipo}/{id}`, pasar su estado y callbacks a la pantalla, y conectar `albumAsociado` con `ArtistaDetalleViewModel.albumAsociado` mediante `LaunchedEffect` (D5).
- [x] 4.2 Pruebas de navegación en `navigation/VinilosNavHostTest` (con `RepositorioArtistasFalso` y `RepositorioAlbumesFalso`): Coleccionista → artistas → detalle → "Agregar álbum" → elegir → "Asociar" → aviso y álbum en la discografía; y Visitante sin botón.
- [x] 4.3 Agregar a `RotacionTest` el caso de rotar con la ventana abierta, búsqueda escrita y álbum elegido. `RecuperacionTest` no se amplió: el error al asociar seguido de un reintento exitoso ya lo cubren `AsociarAlbumViewModelTest.reintentarDespuesDelErrorAsociaYCierra` y `VentanaAsociarAlbumTest.siFallaAlAsociarMuestraElErrorYConservaLaSeleccion`.

## 5. Verificación

- [x] 5.1 `.\gradlew.bat lint testDebugUnitTest --continue`: 0 errores de lint y todas las unitarias en verde (se quitaron 2 textos sin uso que había agregado la HU).
- [x] 5.2 Las 29 instrumentadas de la HU15 en paralelo en los 3 dispositivos, todas en verde: Pixel 8 (API 34), Nexus 5X (API 23) y Motorola Edge 30 (con `applicationId` `com.example.vinilos.revision` para no reemplazar la app instalada). Suite instrumentada completa sin E2E en el Pixel 8 como regresión.
- [x] 5.3 Prueba manual contra un backend local aislado (puerto 3001, Pixel 8): se asoció "Buscando América" a Queen y "A Night at the Opera" a Rubén Blades desde la app; `GET /albums` mostró los dos álbumes con ambos intérpretes, Queen con [100, 102, 103] y Rubén con [100, 101, 102], y el aviso "Álbum asociado". Se restauró con `sql/data.sql`. Capturas de la ventana en el Pixel 8 y en el Nexus 5X. Quedan pendientes las capturas rotadas y con fuente al 200 % para la wiki (6.1); la app solo tiene tema claro.

## 6. Documentación (T15-6, #57)

- [ ] 6.1 Actualizar la wiki de HU15 con los mockups de la implementación y la nota de las decisiones del backend (D1).
- [x] 6.2 Dejar en el PR o en `docs/pruebas` la guía para las E2E de T15-5 (#56) (hecho en `docs/pruebas/sprint-3/HU15/resultados.md`, junto con la evidencia): usar un álbum creado por la prueba con `POST /albums`, restaurar con `PUT /musicians/{id}/albums` o `PUT /bands/{id}/albums` con la lista original y nunca con `DELETE`; el error al asociar se cubre con `MockWebServer`.
- [ ] 6.3 Pull Request a `develop` (`feat(HU15): agregar un álbum a un artista`) con `Closes #53`, `Closes #54`, `Closes #55` y `Closes #57`.
