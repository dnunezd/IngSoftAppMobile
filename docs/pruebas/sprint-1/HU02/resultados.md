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

## Pendiente

- T02-5 (verificación con backend real) requiere backend levantado y dispositivo disponible; no ejecutada en esta sesión.
