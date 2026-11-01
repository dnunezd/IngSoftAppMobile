# HU02 · Consultar detalle de álbum: pruebas y evidencia

Especificación: [`openspec/changes/add-detalle-album/specs/detalle-album/spec.md`](../../../../openspec/changes/add-detalle-album/specs/detalle-album/spec.md) y [`.../specs/acceso-backend/spec.md`](../../../../openspec/changes/add-detalle-album/specs/acceso-backend/spec.md) (ServiceAdapter compartido, T02-2). Tareas y su estado en [`openspec/changes/add-detalle-album/tasks.md`](../../../../openspec/changes/add-detalle-album/tasks.md). Resultados por dispositivo en [`resultados.md`](resultados.md).

La evidencia de las pruebas E2E (capturas por paso e informe en PDF, generados por `EvidenciaE2E` en `e2e-evidencia/` dentro del dispositivo, excluida en `.gitignore`) se adjunta en la wiki como parte de T02-6, no se duplica en este repositorio.

Todas las pruebas se escribieron con TDD: ROJO (falla antes de implementar) y VERDE (pasa con el código mínimo).

## Cada criterio de aceptación tiene al menos una prueba automatizada

| # | Criterio | Unitarias (JVM) | Integración (Compose Test) | E2E / backend real |
|---|---|---|---|---|
| 1 | Consultar el detalle del álbum seleccionado por id | `AlbumRepositoryTest.albumCompletoConsultaElIdYMapeaSusRelacionesSinAutor`, `AlbumViewModelTest.cargandoPasaAExitoConElAlbumDelIdSolicitado` | `ContenidoAlbumTest.muestraLosMetadatosDelAlbumConsultado`, `PantallaDetalleAlbumTest.exitoMuestraElDetalleSinProgresoNiError`, `VinilosNavHostTest.tocarUnAlbumAbreSuDetalleYLaFlechaVuelveAlCatalogoSinRepetirLaConsulta` | `AlbumDetalleE2ETest.elVisitanteVeElDetalleCompletoDelAlbumConCancionesArtistaYComentario` |
| 2 | Portada arriba; imagen genérica si falta/falla, sin cerrar la app | — | `PortadaAlbumTest` (3), `ContenidoAlbumTest.muestraUnaIlustracionLocalProvisionalAntesDeLosMetadatos`, `…conUrlMuestraUnaUnicaPortadaSinRomperLaPantalla` | `AlbumDetalleE2ETest` (portada visible en el flujo completo) |
| 3 | Canciones con nombre y duración; "Este álbum aún no tiene canciones registradas" si no hay | `AlbumPresentacionTest.sinCancionesSeleccionaElMensajeDeVacio`, `AlbumRepositoryTest.albumSinRelacionesDevuelveListasVacias` | `ContenidoAlbumTest.cancionesMuestraCadaNombreYDuracionSinMensajeDeVacio`, `…sinCancionesMuestraElMensajeExacto` | `AlbumDetalleBackendRealTest.unAlbumSinCancionesNiComentariosMuestraLosMensajesDeVacioConDatosReales` |
| 4 | Artistas relacionados con nombre e imagen | — | `ContenidoAlbumTest.muestraLosArtistasConUnaImagenProvisionalParaCadaUno` | `AlbumDetalleE2ETest` |
| 5 | Comentarios con calificación 0-5, sin autor; "Aún no hay comentarios para este álbum" y "Sin calificaciones" si no hay | `AlbumPresentacionTest.sinComentariosSeleccionaElMensajeDeVacio`, `…losComentariosPresentadosNoExponenIdentidadDeAutor` | `ContenidoAlbumTest.sinComentariosMuestraElMensajeYLaAusenciaDeCalificaciones`, `…comentariosMuestraTextoNotaYPromedioSinDatosDeAutor`, `…calificacionFueraDeRangoNoCierraLaAppYMuestraLasEstrellasLimitadas` | `AlbumDetalleBackendRealTest.unAlbumSinCancionesNiComentariosMuestraLosMensajesDeVacioConDatosReales` |
| 6 | Fecha de lanzamiento en UTC, en español, independiente de la zona del teléfono | `FormatoFechaTest` (4), `AlbumPresentacionTest.entregaLaFechaFormateadaFueraDeCompose`, `AlbumRepositoryTest.fechaDeLanzamientoConFormatoInvalidoSeTraduceAErrorDeCarga` | `ContenidoAlbumTest.conCalificacionesMuestraLaFechaDeEstrenoYLasEstrellasDelPromedio` | `AlbumDetalleE2ETest` |
| 7 | Indicador de progreso durante la consulta, sin pantalla en blanco | `AlbumViewModelTest.cargandoPasaAExitoConElAlbumDelIdSolicitado` (estado inicial `Cargando`) | `PantallaDetalleAlbumTest.duranteLaConsultaMuestraProgresoSinErrorNiReintento` | — |
| 8 | 404 → "El álbum ya no está disponible" + volver al catálogo, distinto de error de red | `AlbumRepositoryTest.respuesta404SeTraduceAAlbumNoDisponible`, `…cuerpoNuloSeTraduceAErrorDeCargaSinConfundirloCon404`, `AlbumViewModelTest.cargandoPasaANoDisponibleCuandoElAlbumNoExiste`, `…noDisponibleNoPermiteReintentar` | `PantallaDetalleAlbumTest.albumNoDisponiblePermiteVolverSinOfrecerReintento`, `VinilosNavHostTest.elAlbumNoDisponibleMuestraElMensaje404YVolverLlevaAlCatalogo` | `AlbumDetalleBackendRealTest.unAlbumInexistenteMuestraElMensaje404RealDelBackend` |
| 9 | Error de conexión/servidor → "No pudimos cargar el álbum" + "Reintentar", sin cerrar la app | `AlbumRepositoryTest.respuesta500SeTraduceAErrorDeCargaConSuCausa`, `…desconexionSeTraduceAErrorDeCargaConSuCausa`, `…timeoutSeTraduceAErrorDeCargaConSuCausa`, `…jsonIlegibleSeTraduceAErrorDeCargaConSuCausa`, `…errorInesperadoSePropagaSinDisfrazarloDeErrorDeCarga`, `…cancelarLaConsultaPropagaLaCancelacionSinErrorDeCarga`, `…respuestaSinCamposObligatoriosSeTraduceAErrorDeCarga`, `AlbumViewModelTest.cargandoPasaAErrorCuandoFallaLaConsulta`, `…reintentarPasaDeErrorACargandoYExitoConElMismoId`, `…respuestaTardiaNoReemplazaLaConsultaVigente` | `PantallaDetalleAlbumTest.errorDeCargaMuestraElMensajeYNotificaElReintento` | — |
| 10 | Promedio aritmético de calificaciones; "Sin calificaciones" si no hay | `PromedioCalificacionesTest` (5), `AlbumPresentacionTest.entregaElPromedioDeLasCalificacionesDisponibles` | `ContenidoAlbumTest.conCalificacionesMuestraLaFechaDeEstrenoYLasEstrellasDelPromedio`, `…sinComentariosMuestraElMensajeYLaAusenciaDeCalificaciones` | — |

Cobertura: 10/10 criterios con al menos una prueba automatizada.

## Infraestructura compartida (T02-2 · `acceso-backend`)

| Requisito | Prueba |
|---|---|
| Un único `ServiceAdapter` para todos los repositorios | `AppContainerTest.albumRepositorioPuedeReemplazarseComoLosDemas` |
| Distinguir 404 de fallo de transporte | `AlbumRepositoryTest.respuesta404SeTraduceAAlbumNoDisponible` vs `…desconexionSeTraduceAErrorDeCargaConSuCausa` |
| Cancelación no se reporta como error de carga | `AlbumRepositoryTest.cancelarLaConsultaPropagaLaCancelacionSinErrorDeCarga` |

## Pendiente (no bloquea)

- T02-6 (wiki/mockups) redactada sin publicar.
- Las imágenes de portada/artistas son locales provisionales (de HU00), a la espera de Coil en HU03; no se marcan como criterio de "imagen real" verificado.
