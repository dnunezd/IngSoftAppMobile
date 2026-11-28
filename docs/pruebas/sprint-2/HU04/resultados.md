# Resultados: HU04 Consultar el detalle de un artista (unitarias e interfaz)

Ejecución del 2026-11-28 sobre `develop` (`278dedc`). Las pruebas de interfaz usan repositorios falsos, así que no necesitan backend. Las pruebas E2E de esta historia y su evidencia están en el [README](README.md).

## Suites

| Suite | Resultado | Entorno |
|---|---|---|
| Unitarias (`testDebugUnitTest`) | 540 sin fallos (todas las de la app) | JVM local (JDK 21) y [CI en `develop`](https://github.com/dnunezd/IngSoftAppMobile/actions/runs/46088992565) (JDK 17) |
| Interfaz (ver las clases abajo) | `OK (144 tests)` en cada dispositivo, en una sola corrida de las clases de interfaz de HU00, HU03, HU04 y HU09; `AlbumCoverTest`: 4/4 (2/2 en API 23, porque dos pruebas necesitan API 26) | Nexus 5X (API 23), Pixel 8 (API 34) y Motorola Edge 30 (Android 14) |

## Clases de prueba

| Tipo | Clase | Pruebas | Qué cubre |
|---|---|:---:|---|
| Unitaria | `ArtistRepositoryTest` | 21 de 31 | El detalle: ruta de músico y de banda, premios cruzados con sus premiaciones, fechas, integrantes, álbumes y premios ordenados, datos incompletos, 404, error del servidor, sin conexión y respuesta ilegible (las demás son del listado, HU03) |
| Unitaria | `ArtistDetailViewModelTest` | 14 | Éxito, error y reintento, detalle guardado con su aviso, 404 y el álbum asociado de HU15 |
| Unitaria | `ArtistRepositoryCacheTest` | 20 | El detalle guardado en Room por tipo de artista y lo que pasa con un 404 (comparte la clase con HU03) |
| Unitaria | `FormatoFechaTest` | 6 | Fechas de nacimiento, creación y premiación en el idioma de la app, sin cambiar el día por la zona horaria |
| Interfaz | `ArtistDetailScreenGuardadoTest` | 3 | El detalle guardado mientras se actualiza, el aviso si no se pudo actualizar y la fecha en el idioma de la app |
| Interfaz | `VinilosNavHostTest` | 5 de 29 | Abrir el detalle desde el listado, abrir un álbum, consultar por tipo e id, "Atrás" y el artista que ya no existe |
| Interfaz | `AlbumCoverTest` | 4 | La portada por defecto cuando la URL falla o está vacía |
| E2E | `ArtistDetailE2ETest` | 7 | Músico, banda, abrir un álbum, sin álbumes ni premios, rotación, error con reintento e integrantes, contra BackVynils |

## Criterios de aceptación

| # | Criterio | Unitaria | Interfaz (Compose Test) | E2E |
|---|---|---|---|---|
| 1 | Se abre el detalle al seleccionar un artista del listado (HU03) | — | `selectUnArtistaLlevaASuDetalleYAtrasVuelveAlListadoSinRepetirLaConsulta`, `elMusicoSeConsultaPorSuTipoEId` | `elDetalleDeUnMusicoMuestraSusDatosAlbumesYPrizes` |
| 2 | Se ven imagen, nombre, descripción y la etiqueta "Músico" o "Banda" | `detalleDeMusicoConsultaSuRutaYLoMapea`, `detalleDeBandaConsultaSuRutaYLoMapea`, `alCrearseConsultaElArtistaYPasaASuccess` | — | `elDetalleDeUnMusicoMuestraSusDatosAlbumesYPrizes`, `elDetalleDeUnaBandaMuestraSuFechaDeCreacionIntegrantesAlbumesYPrizes` |
| 3 | Un músico muestra su fecha de nacimiento | `detalleDeMusicoConsultaSuRutaYLoMapea`, `fechaYDescripcionAusentesOMalformadasSeOmiten`, `laFechaLargaSigueElAppLanguage` | `laFechaGuardadaSeMuestraEnElAppLanguage` | `elDetalleDeUnMusicoMuestraSusDatosAlbumesYPrizes` |
| 4 | Una banda muestra su fecha de creación y sus integrantes con imagen y nombre | `laBandaUsaSuFechaDeCreacionYNoLaDeNacimiento`, `losIntegrantesConservanElOrdenDelBackend`, `omiteLosAlbumesEIntegrantesIncompletosSinAfectarALosDemas` | — | `elDetalleDeUnaBandaMuestraSuFechaDeCreacionIntegrantesAlbumesYPrizes`, `unaBandaConIntegrantesLosMuestraConImagenYNombre` |
| 5 | Se ven los álbumes del artista con portada, nombre y año | `ordenaLosAlbumesDelMasRecienteAlMasAntiguoConLosSinFechaAlFinal`, `omiteLosAlbumesEIntegrantesIncompletosSinAfectarALosDemas` | — | `elDetalleDeUnMusicoMuestraSusDatosAlbumesYPrizes` |
| 6 | Al seleccionar un álbum se abre su detalle (HU02) | — | `selectUnAlbumDelArtistaAbreSuDetalleYVolverNoRepiteLaConsultaDelArtista` | `tocarUnAlbumAbreSuDetalleYAtrasVuelveAlArtistaSinRepetirLaConsulta` |
| 7 | Se ven los premios con nombre, organización y fecha | `cruzaLosPrizesPorElIdDeLaPremiacionYNoPorElDelPrize`, `ordenaLosPrizesDelMasRecienteAlMasAntiguoConLosSinFechaAlFinal`, `omiteLosPrizesSinDatosDisponiblesSinAfectarALosDemas`, `falloSoloDeLasPremiacionesEsLoadError` | — | `elDetalleDeUnMusicoMuestraSusDatosAlbumesYPrizes` |
| 8 | Sin álbumes o sin premios: "Sin álbumes" o "Sin premios" | `sinPrizesNoConsultaLasPremiaciones` | — | `unArtistaSinAlbumesNiPrizesMuestraLosMensajesDeEmpty` (se omite si el backend no tiene un artista así) |
| 9 | Si la imagen o una portada no carga, se ve una por defecto | — | `conUnaUrlNoRompeLaPantallaMientrasCargaOFalla`, `unaUrlEnBlancoSeTrataComoAusenteYMuestraElIconoDeRespaldo` | `elDetalleDeUnMusicoMuestraSusDatosAlbumesYPrizes` |
| 10 | Mientras carga se ve un indicador | `retryPasaDeErrorALoadingYSuccessConElMismoArtista` | `conElDetalleGuardadoActualizandoseMuestraElIndicadorPequeno` | — |
| 11 | Si el artista ya no existe o falla la carga: "No se pudo cargar el artista" y "Reintentar" | `pasaAErrorCuandoFallaLaConsulta`, `retryPasaDeErrorALoadingYSuccessConElMismoArtista`, `un404TerminaEnErrorAunqueHayaDetalleGuardado`, `artistaInexistenteEsLoadErrorEnAmbosTipos`, `errorDelServidorEsLoadErrorEnAmbosTipos`, `desconexionEsLoadErrorEnAmbosTipos` | `alVolverDesdeArtistNotAvailableElListadoSeActualizaSinElArtistaEliminado` | `siFallaLaCargaSeVeElErrorYReintentarRecuperaElDetalle` |

Lo que no tiene prueba de interfaz propia: la pantalla de detalle de un artista no tiene una clase de Compose Test para sus estados de datos, cargando y error. Esos estados se prueban en el `ViewModel` (unitarias) y de punta a punta en las E2E. Las clases `ArtistDetailScreenFavoritoTest` y `ArtistDetailScreenAsociarTest` prueban la misma pantalla, pero para HU10 y HU15.

## Cómo repetirlas

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.example.vinilos.viewmodel.ArtistDetailViewModelTest" --tests "com.example.vinilos.data.repository.ArtistRepository*" --tests "com.example.vinilos.data.model.FormatoFechaTest"
```

```powershell
.\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.example.vinilos.ui.artistas.ArtistDetailScreenGuardadoTest,com.example.vinilos.navigation.VinilosNavHostTest,com.example.vinilos.ui.detail.AlbumCoverTest"
```
