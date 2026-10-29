# HU01 · Resultados de ejecución por entorno (OBJ04)

- **Rama:** `feature/HU01-consultar-catalogo-albumes` (desde `feature/HU02-consultar-detalle-album`, 70c48cd), con los cambios de la HU01 sin commit al momento de la ejecución.
- **Backend:** BackVynils en Docker, datos de `sql/data.sql` (álbumes 100 a 103).
- **Cierres inesperados:** `adb logcat -b crash` limpiado antes de cada ejecución y revisado después (`FATAL EXCEPTION`).

## Resumen

| Entorno | Tipo | Android | Unitarias | Instrumentadas | E2E catálogo (`am instrument`) | Cierres | Fecha |
|---|---|---|---|---|---|---|---|
| Nexus 5X API 23 | Emulador | 6.0 (API 23, mínima) | 148/148 | 119/119 | 5/5, 10 capturas | 0 | 2026-10-28 |
| Pixel 8 API 34 | Emulador | 14 (API 34) | — | 119/119 | 5/5, 10 capturas | 0 | 2026-10-28 |
| Motorola Edge 30 | Físico | 14 (API 34) | — | 119/119 | 5/5, 10 capturas ([`e2e/informe.md`](e2e/informe.md)) | 0 | 2026-10-28 |

Lint: 0 errores y 48 avisos, los mismos que antes del cambio.

Verificación manual del flujo (casos M-01) en el Motorola Edge 30 y el Nexus 5X API 23: 8/8 casos aplicables cumplen y 0 cierres en ambos (detalle en [`manual.md`](manual.md)).

OBJ03: el informe [`e2e/informe.md`](e2e/informe.md) corresponde a la ejecución en el dispositivo físico (Motorola Edge 30 con `adb reverse` contra BackVynils). Por acuerdo del equipo, las 10 capturas (8,7 MB) no se suben al repositorio: se conservan en el equipo donde se ejecutaron, junto con las de la verificación manual, y se pueden regenerar con los comandos de abajo. En el informe, "Ok" significa que la captura se guardó; el resultado de las pruebas es `OK (5 tests)` de `am instrument`.

## Comandos

```powershell
$env:JAVA_HOME = "C:\Users\Jeffrey-pc\.jdks\jbr-21.0.11"

# Unitarias y lint (JVM)
.\gradlew.bat lint testDebugUnitTest

# Suite instrumentada completa en un emulador (el backend se ve en 10.0.2.2)
$env:ANDROID_SERIAL = "emulator-5554"     # o emulator-5556
adb logcat -b crash -c
.\gradlew.bat connectedDebugAndroidTest
adb logcat -b crash -d | Select-String "FATAL EXCEPTION"

# E2E del catálogo conservando la evidencia (connectedDebugAndroidTest desinstala la app y la borra)
.\gradlew.bat installDebug installDebugAndroidTest
adb shell rm -rf /sdcard/Android/data/com.example.vinilos/files/e2e-evidencia
adb shell am instrument -w -e class com.example.vinilos.CatalogoE2ETest com.example.vinilos.test/androidx.test.runner.AndroidJUnitRunner
adb pull /sdcard/Android/data/com.example.vinilos/files/e2e-evidencia .
```

En el teléfono, además: `adb reverse tcp:3000 tcp:3000` y agregar `-PbaseUrl=http://localhost:3000/` a `installDebug installDebugAndroidTest`.

## Instrumentadas por clase (igual en los dos emuladores)

| Clase | Pruebas | HU |
|---|---|---|
| `ui.catalogo.PantallaCatalogoTest` | 15 | HU01 |
| `ui.catalogo.CatalogoActualizarTest` | 4 | HU01 |
| `ui.catalogo.CatalogoListaLargaTest` | 2 | HU01 |
| `CatalogoE2ETest` | 5 | HU01 |
| `navigation.VinilosNavHostTest` | 21 | HU00–HU03 |
| `RotacionTest` | 4 | HU00, HU01 |
| `RecuperacionTest` | 5 | HU00, HU01 |
| `IngresoE2ETest` | 4 | HU00 |
| `AlbumDetalleE2ETest` | 1 | HU02 |
| `navigation.AlbumDetalleBackendRealTest` | 2 | HU02 |
| `ui.detalle.ContenidoAlbumTest` | 10 | HU02 |
| `ui.detalle.PantallaDetalleAlbumTest` | 4 | HU02 |
| `ui.detalle.PortadaAlbumTest` | 3 | HU02 |
| `ui.artistas.PantallaArtistasTest` | 12 | HU03 |
| `ui.ingreso.PantallaColeccionistasTest` | 14 | HU00 |
| `ui.ingreso.PantallaIngresoTest` | 4 | HU00 |
| `ui.inicio.PantallaInicioTest` | 8 | HU00 |
| `ExampleInstrumentedTest` | 1 | — |
| **Total** | **119** | |

## Observaciones

- **API 23, portada de Wikimedia:** "A Night at the Opera" muestra la portada genérica en Android 6, mientras que en API 34 carga. Desde el PC la URL responde 200 con el agente de usuario de la app, así que es una limitación de Android 6 al conectarse a ese servidor. La app cumple el criterio 6: muestra la imagen genérica y la lista sigue funcionando.
- **API 23, correcciones de pruebas de la HU02 incluidas en esta rama:** `EvidenciaE2E` captura con `UiAutomation.takeScreenshot()` en API < 26, y `ContenidoAlbumTest` se desplaza con `performScrollToNode`. Sin ellas, 4 pruebas de la HU02 fallaban en API 23 (ver el comentario en el PR #71).
