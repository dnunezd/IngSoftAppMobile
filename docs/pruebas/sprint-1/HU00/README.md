# HU00 · Ingreso a la plataforma: pruebas por criterio

Ejecución del 2026-11-28 sobre `develop` (`278dedc`). Las pruebas de interfaz usan repositorios falsos, así que no necesitan backend. Los escenarios E2E (`FN00-ESC01` a `ESC03`) y su evidencia del Sprint 1 están en el [inventario del Sprint 1](https://github.com/dnunezd/IngSoftAppMobile/wiki/Sprint-1#inventario-pruebas-e2e---sprint-1).

## Suites

| Suite | Resultado | Entorno |
|---|---|---|
| Unitarias (`testDebugUnitTest`) | 540 sin fallos (todas las de la app) | JVM local (JDK 21) y [CI en `develop`](https://github.com/dnunezd/IngSoftAppMobile/actions/runs/46088992565) (JDK 17) |
| Interfaz (ver las clases abajo) | `OK (144 tests)` en cada dispositivo, en una sola corrida de las clases de interfaz de HU00, HU03, HU04 y HU09 | Nexus 5X (API 23), Pixel 8 (API 34) y Motorola Edge 30 (Android 14) |

## Clases de prueba

| Tipo | Clase | Pruebas | Qué cubre |
|---|---|:---:|---|
| Unitaria | `SessionViewModelTest` | 8 | Sesión de Visitante o de Coleccionista, acciones del Coleccionista habilitadas solo con un coleccionista elegido, cambio de perfil y cierre de sesión |
| Unitaria | `CollectorsViewModelTest` | 23 | Cargando, éxito, vacío y error, reintento, búsqueda por nombre y correo sin tildes ni mayúsculas, y listado guardado con su aviso |
| Unitaria | `CollectorRepositoryTest` | 9 | Respuesta con datos o vacía, campos desconocidos, elementos incompletos, error del servidor, sin conexión, tiempo agotado y JSON malformado |
| Unitaria | `CollectorRepositoryCacheTest` | 14 | El listado guardado en Room: qué se guarda, en qué orden y cuándo no se escribe |
| Unitaria | `CollectorDtoTest`, `InitialsTest` | 2 y 5 | Lectura del coleccionista y sus iniciales |
| Interfaz | `EntryScreenTest` | 4 | Las dos opciones de ingreso y a qué rol lleva cada una |
| Interfaz | `CollectorsScreenTest` | 16 | Cargando, datos, búsqueda, vacío, error con "Reintentar" y "Visitante", y la flecha de atrás |
| Interfaz | `HomeScreenTest` | 8 | El saludo según el rol y el acceso al catálogo y a los artistas |
| Interfaz | `VinilosNavHostTest` | 11 de 29 | Navegación entre el ingreso, el selector y el inicio, con y sin sesión |
| E2E | `EntryE2ETest` | 4 | Ingreso como Visitante y como Coleccionista contra BackVynils, búsqueda y "Atrás" |
| E2E | `ColeccionistasSinConexionE2ETest` | 2 | El listado guardado sin conexión |

## Criterios de aceptación

| # | Criterio | Unitaria | Interfaz (Compose Test) | E2E |
|---|---|---|---|---|
| 1 | Al abrir la app se ven las opciones "Visitante" y "Coleccionista" | — | `muestraLasOpcionesVisitanteYColeccionistaConSuBotonEntrar`, `laAppAbreEnElIngresoSinSesionYSinPantallaAnterior` | `ingresarComoVisitanteLlevaAlInicioYDeAhiAlCatalogo` |
| 2 | El Visitante entra sin dar datos y no ve acciones del Coleccionista | `ingresarComoVisitanteNoTieneColeccionistaNiAcciones` | `entrarComoVisitanteAvisaSoloAlVisitante`, `visitanteEntraAlInicioConSesionDeVisitante`, `sinSesionElSaludoEsElDelVisitante` | `ingresarComoVisitanteLlevaAlInicioYDeAhiAlCatalogo` |
| 3 | El Coleccionista ve el listado con nombre y correo y, al elegir uno, entra con sus acciones habilitadas | `cargandoPasaASuccessConLosColeccionistas`, `respuestaConDatosDevuelveLosColeccionistas`, `ingresarComoColeccionistaGuardaSusDatosYHabilitaLasAcciones` | `conDatosMuestraAvatarNombreYCorreoDeCadaColeccionista`, `selectUnColeccionistaAvisaConEseColeccionista`, `elegirUnColeccionistaEntraAlInicioConEseColeccionista` | `ingresarComoColeccionistaMuestraLaListaRealYSeEligeUno` |
| 4 | Mientras carga el listado se ve un indicador de progreso | `alCrearseEmpiezaLoadingYPideLosColeccionistasUnaVez` | `cargandoMuestraElIndicadorDeProgresoYNoElBuscador` | — |
| 5 | Si el listado no carga: "No pudimos cargar los coleccionistas", "Reintentar" y la opción "Visitante" | `cargandoPasaAErrorSiFallaLaConsulta`, `retryTrasUnErrorVuelveALoadingYLuegoASuccess`, `sinConexionSeTraduceALoadError`, `tiempoDeEsperaAgotadoSeTraduceALoadError` | `errorMuestraElMensajeReintentarYLaOpcionVisitante`, `retryYVisitanteDelErrorAvisan`, `visitanteDesdeElErrorEntraAlInicioYAtrasVuelveAlIngreso` | — |
| 6 | Sin coleccionistas: "Aún no hay coleccionistas registrados" y la opción de volver | `cargandoPasaAEmptySiNoHayColeccionistas`, `respuestaVaciaDevuelveListaVacia` | `vacioMuestraElMensajeYLaOpcionDeVolver` | — |
| 7 | "Atrás" desde el listado vuelve al ingreso sin coleccionista elegido | `clearDejaLaSesionSinPerfil`, `cambiarDeColeccionistaAVisitanteNoDejaRestosDelAnterior` | `laFlechaDeAtrasAvisa`, `atrasDesdeElSelectorVuelveAlIngresoSinSesion`, `atrasDesdeElSelectorEnErrorVuelveAlIngresoSinCerrar` | `navegaEntreIngresoInicioCatalogoYArtistasConAtras` |

Nota: la app lleva al Visitante y al Coleccionista primero a la pantalla de inicio, desde donde abren el catálogo; las pruebas verifican ese recorrido.

## Cómo repetirlas

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.example.vinilos.viewmodel.SessionViewModelTest" --tests "com.example.vinilos.viewmodel.CollectorsViewModelTest" --tests "com.example.vinilos.data.repository.CollectorRepository*"
```

```powershell
.\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.example.vinilos.ui.entry.EntryScreenTest,com.example.vinilos.ui.entry.CollectorsScreenTest,com.example.vinilos.ui.home.HomeScreenTest,com.example.vinilos.navigation.VinilosNavHostTest"
```
