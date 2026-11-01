# HU01 · Consultar catálogo de álbumes: pruebas y evidencia

Especificación: [`openspec/specs/catalogo-albumes/spec.md`](../../../../openspec/specs/catalogo-albumes/spec.md) (cambio archivado en `openspec/changes/archive/2026-10-28-add-catalogo-albumes/`). Resultados por entorno en [`resultados.md`](resultados.md), verificación manual en [`manual.md`](manual.md) e informe de la evidencia E2E en [`e2e/informe.md`](e2e/informe.md) (las capturas y los PDF por escenario no se versionan; se conservan en el equipo de ejecución).

Todas las pruebas se escribieron con TDD: ROJO (falla antes de implementar), VERDE (pasa con el código mínimo) y AZUL (refactor con las pruebas otra vez en verde). Las pruebas de integración que se escribieron después del código que ejercitan se validaron con mutación (se rompe el código a propósito y se comprueba que fallan).

## OBJ01 · Cada criterio de aceptación tiene al menos una prueba automatizada

| # | Criterio | Unitarias (JVM) | Integración (Compose Test) | E2E (BackVynils) |
|---|---|---|---|---|
| 1 | Visitante y Coleccionista ven el catálogo | — | `VinilosNavHostTest.elVisitanteVeElCatalogoConSusAlbumesYSuEtiqueta`, `VinilosNavHostTest.verAlbumesLlevaAlCatalogoYLaFlechaVuelveAlInicioConservandoLaSesion`, `PantallaCatalogoTest.laEtiquetaDeRolIndicaVisitante`, `PantallaCatalogoTest.laEtiquetaDeRolIndicaColeccionista` | FN01-ESC01 |
| 2 | Portada, nombre y género | `AlbumRepositoryTest.catalogoConsultaGetAlbumsYConvierteCadaAlbum`, `AlbumRepositoryTest.catalogoDescartaLosAlbumesIncompletosSinAfectarALosDemas` | `PantallaCatalogoTest.cadaAlbumMuestraSuPortadaNombreYGenero` | FN01-ESC01 |
| 3 | Indicador de progreso | `AlbumsViewModelTest.cargandoPasaAExitoConLosAlbumes` | `PantallaCatalogoTest.mientrasCargaMuestraElIndicadorDeProgreso` | — |
| 4 | "No pudimos cargar el catálogo" + "Reintentar", sin cierre | `AlbumRepositoryTest.catalogoConError500EsErrorDeCarga`, `…catalogoConDesconexionEsErrorDeCarga`, `…catalogoConJsonIlegibleEsErrorDeCarga`, `AlbumsViewModelTest.cargandoPasaAErrorCuandoFallaLaConsulta`, `AlbumsViewModelTest.reintentarDesdeErrorPasaPorCargandoYLlegaAExito` | `PantallaCatalogoTest.enErrorMuestraElMensajeYReintentarLlamaAlCallback`, `RecuperacionTest.elCatalogoConElBackendCaidoMuestraElErrorYReintentarLoRecupera` | FN01-ESC03 (MockWebServer) |
| 5 | "Aún no hay álbumes en el catálogo" | `AlbumRepositoryTest.catalogoSinAlbumesEsUnExitoConListaVacia`, `AlbumsViewModelTest.cargandoPasaAVacioSinAlbumes` | `PantallaCatalogoTest.sinAlbumesMuestraElMensajeDeCatalogoVacio` | — |
| 6 | Portada genérica si no carga | — | `PantallaCatalogoTest.unaPortadaVaciaOInvalidaMuestraLaImagenGenericaSinRomperLaLista`, `PortadaAlbumTest` (3) | FN01-ESC01 (en API 23 la portada de Wikimedia cae a la genérica) |
| 7 | Muchos álbumes sin detenerse | — | `CatalogoListaLargaTest.conCincuentaAlbumesSeLlegaAlUltimoDesplazandose`, `CatalogoListaLargaTest.alVolverArribaSeVeOtraVezElPrimero` | FN01-ESC02 |
| 8 | Orden alfabético | `OrdenAlbumesTest` (3), `AlbumRepositoryTest.catalogoOrdenaLosAlbumesPorNombre` | `PantallaCatalogoTest.losAlbumesSeMuestranEnElOrdenRecibido` | FN01-ESC01 |
| 9 | Rotación conserva posición, sin recarga | `AlbumsViewModelTest.laConsultaSeHaceUnaSolaVezAlCrearse` | `RotacionTest.alGirarElCatalogoConservaSuPosicionSinVolverAConsultar` | FN01-ESC04 |
| 10 | Deslizar para actualizar | `AlbumsViewModelTest.actualizarMantieneLaListaVisibleYPublicaLaNueva`, `…actualizarDesdeVacioPuedeMostrarAlbumes`, `…siFallaLaActualizacionConservaLaListaYAvisa`, `…respuestaTardiaNoReemplazaLaConsultaVigente` | `CatalogoActualizarTest` (4) | FN01-ESC02 |

Cobertura: 10/10 criterios con al menos una prueba unitaria o de integración.

## OBJ02 · Estados visibles y rutas de navegación (Compose Test)

### Estados del catálogo (9/9)

| Estado visible | Prueba |
|---|---|
| Cargando (indicador y texto) | `PantallaCatalogoTest.mientrasCargaMuestraElIndicadorDeProgreso` |
| Éxito con artista y año | `PantallaCatalogoTest.conArtistaYAnioLosMuestra` |
| Éxito sin artista ni año | `PantallaCatalogoTest.sinArtistaNiAnioNoMuestraHuecosNiNull` |
| Éxito con portada por defecto | `PantallaCatalogoTest.unaPortadaVaciaOInvalidaMuestraLaImagenGenericaSinRomperLaLista` |
| Vacío | `PantallaCatalogoTest.sinAlbumesMuestraElMensajeDeCatalogoVacio` |
| Error con "Reintentar" | `PantallaCatalogoTest.enErrorMuestraElMensajeYReintentarLlamaAlCallback` |
| Actualizando | `PantallaCatalogoTest.mientrasActualizaMuestraElIndicadorSinQuitarLaLista` |
| Aviso al fallar la actualización ("No pudimos actualizar el catálogo", una sola vez) | `PantallaCatalogoTest.siFallaLaActualizacionAvisaYConservaLaLista`, `…siLaPantallaSaleMientrasSeVeElAvisoQuedaComoMostrado` |
| Etiqueta VISITANTE / COLECCIONISTA | `PantallaCatalogoTest.laEtiquetaDeRolIndicaVisitante`, `…laEtiquetaDeRolIndicaColeccionista` |

### Rutas que entran o salen del catálogo (8/8)

| Ruta | Prueba (`VinilosNavHostTest`) |
|---|---|
| Ingreso → Inicio → Catálogo (Visitante) | `elVisitanteVeElCatalogoConSusAlbumesYSuEtiqueta` |
| Ingreso → Coleccionistas → Inicio → Catálogo | `verAlbumesLlevaAlCatalogoYLaFlechaVuelveAlInicioConservandoLaSesion` |
| Catálogo → Inicio (flecha) | `verAlbumesLlevaAlCatalogoYLaFlechaVuelveAlInicioConservandoLaSesion` |
| Catálogo → Inicio ("Atrás" del sistema) | `atrasDelSistemaDesdeElCatalogoYLosArtistasVuelveAlInicio` |
| Catálogo → Detalle (id tocado) | `tocarUnAlbumAbreSuDetalleYLaFlechaVuelveAlCatalogoSinRepetirLaConsulta` |
| Detalle → Catálogo (flecha, sin nueva consulta) | `tocarUnAlbumAbreSuDetalleYLaFlechaVuelveAlCatalogoSinRepetirLaConsulta` |
| Detalle → Catálogo ("Atrás", sin nueva consulta) | `atrasDelSistemaDesdeElDetalleVuelveAlCatalogoSinRepetirLaConsulta` |
| Detalle 404 → Catálogo ("Volver al catálogo") | `elAlbumNoDisponibleMuestraElMensaje404YVolverLlevaAlCatalogo` |

## OBJ03 · E2E contra BackVynils (`CatalogoE2ETest`)

Los escenarios usan los IDs `FN01-ESCxx`, igual que las E2E de la HU00, la HU02 y la HU03 (equivalen a `E2E-01-xx` del inventario de la wiki). Cada uno guarda su evidencia con `EvidenciaE2E` en su propia carpeta `e2e-evidencia/FN01_ESCxx/` del dispositivo: un pantallazo por paso, empezando en el inicio, un `informe.md` y un `FN01_ESCxx.pdf`.

| Id | Escenario | Backend | Prueba |
|---|---|---|---|
| FN01-ESC01 | Visitante y Coleccionista ven los 4 álbumes en orden con portada, nombre y género | Real | `visitanteYColeccionistaVenLosCuatroAlbumesEnOrden` |
| FN01-ESC02 | Recorrer hasta el final y deslizar para actualizar | Real | `recorrerHastaElFinalYDeslizarParaActualizar` |
| FN01-ESC03 | Backend caído → error → "Reintentar" → lista | MockWebServer (no cuenta para OBJ03) | `conElBackendCaidoSeVeElErrorYReintentarRecuperaElCatalogo` |
| FN01-ESC04 | Al girar conserva la posición y no vuelve a consultar | Real | `alGirarConservaLaPosicionSinVolverAConsultar` |
| FN01-ESC05 | Tocar un álbum abre su detalle real y "Atrás" vuelve sin recargar | Real | `tocarUnAlbumAbreSuDetalleYAtrasVuelveAlCatalogo` |

4 escenarios contra el backend real (el objetivo pide al menos 2). La ejecución en dispositivo físico está en [`resultados.md`](resultados.md) y su informe en [`e2e/informe.md`](e2e/informe.md).

## Estética

Capturas del catálogo en [`capturas-estetica/`](capturas-estetica/): usa solo los colores, la tipografía y los componentes del tema de la app, y las transiciones del `NavHost` actual.
