# HU02 · Resultados de verificación por dispositivo

## Unitarias (`testDebugUnitTest`)

Sin fallos en todas las corridas realizadas durante el desarrollo de T02-2/T02-3 (JVM, no dependen de dispositivo).

## Instrumentadas / UI (`connectedDebugAndroidTest`)

| Dispositivo | API | Resultado | Notas |
|---|---|---|---|
| Nexus 5X (emulador, corrida del compañero) | 23 | Fallos iniciales en `ContenidoAlbumTest` por `performScrollTo()` sobre nodos no compuestos en `LazyColumn` | Corregido con `performScrollToNode`; ver `tasks.md` |
| AVD "Perfil_prueba_23" | 23 | 91/91 | Verificación propia tras la corrección |
| Samsung SM-A356E | 16 | 91/91 | Verificación propia, dispositivo físico por Wi-Fi |

Los conteos (91) corresponden a la suite dedicada de HU02 vigente en el momento de la verificación (antes de integrar HU00/HU01 en esta rama). Tras la integración, `connectedDebugAndroidTest` ejecuta el total combinado de la app (HU00+HU01+HU02); los fallos observados en corridas posteriores sobre API 23 (`CatalogoE2ETest`, `VinilosNavHostTest`, `CatalogoActualizarTest`, `AlbumDetalleE2ETest`) pertenecen a HU00/HU01 y no están cubiertos por esta hoja de resultados.

## E2E con backend real (T02-5)

`AlbumDetalleE2ETest` y `AlbumDetalleBackendRealTest` (3 escenarios: flujo completo exitoso, álbum sin canciones ni comentarios, 404 real) ejecutados contra el backend BackVynils real, en Samsung SM-A356E (Android 16) por USB, con `adb reverse tcp:3000 tcp:3000` y la app instalada con `-PbaseUrl=http://localhost:3000/`:

```
adb shell am instrument -w -e class com.example.vinilos.AlbumDetalleE2ETest,com.example.vinilos.navigation.AlbumDetalleBackendRealTest com.example.vinilos.test/androidx.test.runner.AndroidJUnitRunner
```

Resultado: **OK (3 tests)**. Evidencia generada por `EvidenciaE2E`, una carpeta y un PDF por escenario (`FN02_ESC01`, `FN02_ESC02`, `FN02_ESC03`), extraída del dispositivo con `adb pull` y adjunta en la wiki (T02-6); no se versiona en el repositorio.

T02-5 queda cerrada con esta verificación.

## Pendiente

- Ninguno relevante a T02-5. T02-6 (wiki/mockups) se redacta aparte, sin publicar todavía.
