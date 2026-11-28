# HU03 · Consultar el listado de artistas: pruebas por criterio

Ejecución del 2026-11-28 sobre `develop` (`278dedc`). Las pruebas de interfaz usan repositorios falsos, así que no necesitan backend. Los escenarios E2E (`FN03-ESC01` a `ESC07`) y su evidencia del Sprint 1 están en el [inventario del Sprint 1](https://github.com/dnunezd/IngSoftAppMobile/wiki/Sprint-1#inventario-pruebas-e2e---sprint-1).

## Suites

| Suite | Resultado | Entorno |
|---|---|---|
| Unitarias (`testDebugUnitTest`) | 540 sin fallos (todas las de la app) | JVM local (JDK 21) y [CI en `develop`](https://github.com/dnunezd/IngSoftAppMobile/actions/runs/46088992565) (JDK 17) |
| Interfaz (ver las clases abajo) | `OK (144 tests)` en cada dispositivo, en una sola corrida de las clases de interfaz de HU00, HU03, HU04 y HU09 | Nexus 5X (API 23), Pixel 8 (API 34) y Motorola Edge 30 (Android 14) |

## Clases de prueba

| Tipo | Clase | Pruebas | Qué cubre |
|---|---|:---:|---|
| Unitaria | `ArtistsViewModelTest` | 16 | Cargando, éxito, vacío y error, reintento, búsqueda sin tildes ni mayúsculas, y listado guardado con su aviso |
| Unitaria | `ArtistRepositoryTest` | 10 de 31 | El listado: músicos y bandas en una sola lista, orden alfabético, elementos incompletos, error del servidor, sin conexión, tiempo agotado y JSON inválido (las demás son del detalle, HU04) |
| Unitaria | `ArtistRepositoryCacheTest` | 20 | El listado y el detalle guardados en Room, con el orden y el tipo de cada artista |
| Unitaria | `ArtistDtoTest` | 3 | Lectura de músicos y bandas, y una imagen ausente |
| Interfaz | `ArtistsScreenTest` | 14 | Filas con nombre y etiqueta, orden, contador, búsqueda, vacío, cargando, error, imagen que no carga y selección de un artista |
| Interfaz | `VinilosNavHostTest` | 3 de 29 | Acceso desde el inicio, abrir el detalle y volver con "Atrás" |
| E2E | `ArtistsE2ETest` | 7 | Listado, búsqueda, sin coincidencias, detalle, error con reintento, sin artistas y rotación, contra BackVynils |
| E2E | `ArtistasSinConexionE2ETest` | 2 | El listado guardado sin conexión |

## Criterios de aceptación

| # | Criterio | Unitaria | Interfaz (Compose Test) | E2E |
|---|---|---|---|---|
| 1 | Se accede al listado desde el menú principal | — | `verArtistasAvisaSoloAlListadoDeArtistas`, `verArtistasLlevaAlListadoYLaFlechaVuelveAlInicioConservandoLaSesion` | `navegaEntreIngresoInicioCatalogoYArtistasConAtras` |
| 2 | El listado incluye músicos y bandas | `uneMusicosYBandasEnUnSoloListado`, `soloMusicosDevuelveLosMusicos`, `mismoIdEnMusicoYBandaSonArtistasDistintos` | `cadaFilaMuestraNombreYEtiquetaSegunElTipo` | `elVisitanteVeMusicosYBandasEnOrdenConImagenNombreYEtiqueta` |
| 3 | Cada artista muestra imagen, nombre y la etiqueta "Músico" o "Banda" | `musicoDeserializaSoloIdNombreEImagenIgnorandoElDetalle`, `bandaDeserializaSoloIdNombreEImagenIgnorandoElDetalle`, `loGuardadoConservaElTipoDeCadaArtista` | `cadaFilaMuestraNombreYEtiquetaSegunElTipo` | `elVisitanteVeMusicosYBandasEnOrdenConImagenNombreYEtiqueta` |
| 4 | Orden alfabético por nombre | `ordenaPorNombreSinDistinguirMayusculasNiTildes`, `loGuardadoSeDevuelveEnOrdenAlfabeticoSinDistinguirTildes` | `lasFilasRespetanElOrdenDelEstado` | `elVisitanteVeMusicosYBandasEnOrdenConImagenNombreYEtiqueta` |
| 5 | La barra de búsqueda filtra por nombre mientras se escribe | `searchFiltraPorNombreEnCualquierParte`, `searchNoDistingueMayusculasNiTildes`, `textoEmptyODeEspaciosMuestraTodos` | `escribirEnElBuscadorAvisaYFiltra` | `elBuscadorFiltraPorNombreMientrasSeEscribe` |
| 6 | Si la imagen no carga, se ve una imagen por defecto | `unaImagenAusenteLlegaComoNula` | `imagenQueNoCargaNoRompeLaFila` | — |
| 7 | Mientras carga se ve un indicador | `alCrearseEmpiezaLoadingYPideLosArtistasUnaVez` | `mientrasCargaMuestraElIndicadorYElBuscadorDeshabilitado` | — |
| 8 | Al seleccionar un artista se abre su detalle (HU04) | — | `tocarUnaFilaSeleccionaEseArtista`, `selectUnArtistaLlevaASuDetalleYAtrasVuelveAlListadoSinRepetirLaConsulta` | `selectUnArtistaLlevaASuDetalleYAtrasVuelveAlListado` |
| 9 | Sin artistas o sin coincidencias: "No se encontraron artistas" | `cargandoPasaAEmptySiNoHayArtistas`, `ambasVaciasDevuelveListaVacia`, `busquedaSinCoincidenciasDejaLaListaVisibleVacia` | `catalogoSinArtistasMuestraElMensaje`, `busquedaSinCoincidenciasMuestraElMensajeYConservaElBuscador` | `sinArtistasRegistradosSeMuestraElMensaje`, `unaBusquedaSinCoincidenciasMuestraElMensaje` |
| 10 | Si falla la carga: "No se pudo cargar el listado de artistas" y "Reintentar" | `cargandoPasaAErrorSiLaConsultaFalla`, `retryTrasErrorVuelveALoadingYLuegoASuccess`, `errorDelServidorEnUnaDeLasDosDevuelveError`, `desconexionDevuelveError`, `tiempoAgotadoDevuelveError`, `jsonInvalidoDevuelveError` | `errorMuestraElMensajeYReintentarAvisa` | `conElBackendCaidoSeVeElErrorYReintentarRecuperaElListado` |

## Cómo repetirlas

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.example.vinilos.viewmodel.ArtistsViewModelTest" --tests "com.example.vinilos.data.repository.ArtistRepository*" --tests "com.example.vinilos.data.remote.ArtistDtoTest"
```

```powershell
.\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.example.vinilos.ui.artistas.ArtistsScreenTest,com.example.vinilos.navigation.VinilosNavHostTest"
```
