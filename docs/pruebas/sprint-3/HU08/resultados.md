# Resultados: HU08 Asociar canciones a un álbum (unitarias e interfaz)

Ejecución del 2026-11-28 sobre `develop` (`278dedc`), con el código de HU08 integrado en el PR [#115](https://github.com/dnunezd/IngSoftAppMobile/pull/115) y el cambio de OpenSpec `add-agregar-tracks`. Las pruebas de interfaz usan repositorios falsos, así que no necesitan backend. Las pruebas E2E de esta historia están en el [README](README.md).

## Suites

| Suite | Resultado | Entorno |
|---|---|---|
| Unitarias (`testDebugUnitTest`) | 540 sin fallos; 19 son de HU08 | JVM local (JDK 21) y [CI en `develop`](https://github.com/dnunezd/IngSoftAppMobile/actions/runs/46088992565) (JDK 17) |
| Lint (`lintDebug`) | 0 errores y 8 avisos | [CI en `develop`](https://github.com/dnunezd/IngSoftAppMobile/actions/runs/46088992565) |
| Interfaz con HU08 (`AddTrackScreenTest`, `VinilosNavHostTest` y `AccesibilidadPantallasTest`) | 81/81 en cada dispositivo (`OK (81 tests)`) | Nexus 5X (API 23), Pixel 8 (API 34) y Motorola Edge 30 (Android 14) |

## Pruebas unitarias de HU08

| Clase | Pruebas | Qué cubre |
|---|:---:|---|
| `TrackValidationTest` | 5 | Campos obligatorios (también un nombre con solo espacios), formato `m:ss` con minutos de cualquier longitud y segundos de 00 a 59, y que una duración vacía sea "obligatoria" y no "formato inválido" |
| `AddTrackViewModelTest` | 9 | Estado inicial, errores por campo, no guardar con datos inválidos, un solo envío, error con lo escrito conservado y reintento, álbum que ya no existe, el álbum guardado como contexto sin repetir la consulta, y que un fallo no reconocido no cierre la app |
| `AlbumRepositoryTracksTest` | 5 | `POST /albums/{id}/tracks` solo con nombre y duración, 404 como `AlbumNotAvailable`, 500 y sin conexión como `LoadError` reintentable, y la cancelación |

## Criterios de aceptación

Cada criterio tiene al menos una prueba automatizada (OBJ01). Los que dependen de la pantalla o de la navegación no se pueden probar como unitarias: se cubren con pruebas de interfaz (Compose Test) y con las E2E.

| # | Criterio | Unitaria | Interfaz (Compose Test) | E2E |
|---|---|---|---|---|
| 1 | El Coleccionista abre el formulario desde el detalle del álbum; el Visitante no ve la opción | — | `elColeccionistaAbreElFormularioDesdeElDetalleYCancelaSinEnviar`, `tocarUnAlbumAbreSuDetalleYLaFlechaVuelveAlCatalogoSinRepetirLaConsulta` (Visitante), `detalleDeAlbumConBotonAddTrackParaElColeccionista` | ESC02, ESC04 |
| 2 | El formulario pide el nombre y la duración | `formularioEmptyIniciaConAmbosCamposObligatoriosYGuardarDeshabilitado` | `formularioEmptyMuestraObligatoriosYGuardarDeshabilitado` | ESC02 |
| 3 | Con datos válidos, "Guardar" asocia la canción, muestra "Canción agregada" y vuelve al detalle | `guardaUnaSolaVezYEnviaElAlbumYLosValoresValidos`, `agregaTrackAlAlbumConPostYSoloLosCamposPermitidos` | `saveTrackRegresaAlDetalleActualizadoYConfirma` | ESC02 |
| 4 | Un campo vacío muestra "Campo obligatorio" y "Guardar" sigue deshabilitado | `camposEmptysSonObligatorios`, `nombreConEspaciosEsObligatorio`, `duracionVaciaEsObligatoriaYNoErrorDeFormato`, `noGuardaCuandoLosValoresNoSonValidos` | `formularioEmptyMuestraObligatoriosYGuardarDeshabilitado` | ESC02, ESC05 |
| 5 | "Cancelar" cierra sin guardar ni enviar datos | — | `elColeccionistaAbreElFormularioDesdeElDetalleYCancelaSinEnviar` | ESC05 |
| 6 | La duración usa `m:ss`; si no, "Usa el formato m:ss, por ejemplo 5:05" y "Guardar" deshabilitado | `aceptaMinutosDeCualquierLongitudYSegundosEntreCeroYCincuentaYNueve`, `duracionesQueNoCumplenMssSonInvalidas`, `editarCamposActualizaLosErroresSinOcultarElDelOtroCampo` | `duracionInvalidaMuestraElFormatoEsperado` | ESC01, ESC06 |
| 7 | Mientras guarda, "Guardando..." deshabilitado y un solo envío | `guardaUnaSolaVezYEnviaElAlbumYLosValoresValidos` | `guardandoDeshabilitaGuardarYCancelar` | ESC03 |
| 8 | Error de conexión o del servicio: mensaje, lo escrito se conserva y se puede reintentar | `falloConservaLoEscritoYPermiteReintentar`, `editarDespuesDeUnFalloOcultaElBannerDeErrorAntesDeVolverAGuardar`, `unFalloNoReconocidoNoTumbaLaAppYQuedaComoFallidaReintentable`, `respuesta500AlAddTrackPermiteReintentar`, `desconexionAlAddTrackSeTraduceALoadError` | `errorReintentableConservaElMensajeYHabilitaGuardar` | ESC03 |
| 9 | Si el álbum ya no existe: "El álbum ya no está disponible", no se guarda y se puede regresar | `albumNotAvailableSeDistingueDelErrorReintentable`, `respuesta404AlAddTrackSeTraduceAAlbumNotAvailable` | `albumNotAvailableOfreceRegresar`, `albumNotAvailableConAtrasDelSistemaTambienNotifica` | ESC07 |
| 10 | Al girar la pantalla se conserva lo escrito | — | — | ESC06 |

Las pruebas unitarias de las clases son `TrackValidationTest`, `AddTrackViewModelTest` y `AlbumRepositoryTracksTest`. Las de interfaz son `AddTrackScreenTest`, `VinilosNavHostTest` y `AccesibilidadPantallasTest`.

## Desarrollo con TDD

Las pruebas unitarias llegaron al PR #115 en los mismos commits que el código (`425c1cc` y `be6f99c`, del 21/11), así que el historial del PR no muestra el orden en que se escribieron. Las correcciones de la revisión (`0d9258e`, `9b1317e` y `fac57df`) agregaron sus pruebas junto con cada cambio.

## Cómo repetirlas

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.example.vinilos.viewmodel.AddTrackViewModelTest" --tests "com.example.vinilos.viewmodel.TrackValidationTest" --tests "com.example.vinilos.data.repository.AlbumRepositoryTracksTest"
```

```powershell
.\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.example.vinilos.ui.create.AddTrackScreenTest,com.example.vinilos.navigation.VinilosNavHostTest,com.example.vinilos.ui.AccesibilidadPantallasTest"
```
