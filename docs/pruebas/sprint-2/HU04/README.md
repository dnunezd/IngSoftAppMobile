# HU04: Consultar el detalle de un artista · pruebas E2E

Evidencia de la tarea T04-5 (issue #28). Las pruebas están en [`ArtistaDetalleE2ETest`](../../../../app/src/androidTest/java/com/example/vinilos/ArtistaDetalleE2ETest.kt) y la especificación del cambio en `openspec/changes/add-e2e-detalle-artista/`. Cada escenario genera un PDF con una captura por paso, guardado en [`e2e/`](e2e/).

Todos los escenarios recorren la app como **Visitante**, desde el inicio y el listado de artistas (HU03). Los que van contra el backend real no asumen datos fijos: antes de abrir la app consultan el listado y los detalles reales y eligen el artista que cumple la condición del escenario; si no existe, el escenario se omite en lugar de fallar.

## Escenarios

| ID | Descripción del escenario | Resultado esperado | Backend | Evidencia |
|---|---|---|---|---|
| FN04-ESC01 | **Ver el detalle de un músico.** Desde el listado, el Visitante toca el músico con premios y más álbumes (Rubén Blades en el backend desplegado). | Se ven la imagen (o la de por defecto), el nombre, la etiqueta "Músico", "FECHA DE NACIMIENTO" con la fecha en español, la descripción, los álbumes con portada, nombre y año, y los premios con nombre, organización y fecha. No aparece la sección de integrantes. | Real | [PDF](e2e/FN04_ESC01.pdf) |
| FN04-ESC02 | **Ver el detalle de una banda.** Toca la banda con premios y más álbumes (Queen). | Se ven la etiqueta "Banda", "FECHA DE CREACIÓN" con su fecha, la sección de integrantes ("Sin integrantes" si no tiene), los álbumes y los premios. | Real | [PDF](e2e/FN04_ESC02.pdf) |
| FN04-ESC03 | **Abrir un álbum desde el detalle y volver.** En un artista con álbumes, toca el primero y luego pulsa "Atrás". | Se abre el detalle del álbum (HU02). Al volver se ve otra vez el artista, sin repetir la consulta al backend. | Real | [PDF](e2e/FN04_ESC03.pdf) |
| FN04-ESC04 | **Artista sin álbumes ni premios.** Abre un artista que no tiene ninguno de los dos (Adele en el backend desplegado). | Se ven "Sin álbumes" y "Sin premios", sin contadores. | Real | [PDF](e2e/FN04_ESC04.pdf) |
| FN04-ESC05 | **Error de carga y reintento.** Abre un músico con el backend caído; el backend vuelve y pulsa "Reintentar". | Se ven "No se pudo cargar el artista" y "Reintentar", sin cierres. Al reintentar se ve el detalle con su fecha de nacimiento. | MockWebServer | [PDF](e2e/FN04_ESC05.pdf) |
| FN04-ESC06 | **Banda con integrantes.** Abre una banda con dos integrantes, uno sin imagen y otro con una URL que no carga. | Se ven "2 integrantes" y cada integrante con su nombre y la imagen por defecto. | MockWebServer | [PDF](e2e/FN04_ESC06.pdf) |
| FN04-ESC07 | **Girar la pantalla con el detalle abierto.** Abre un músico y gira el teléfono a horizontal. | El detalle se conserva y el backend no se vuelve a consultar (1 consulta). | Real | [PDF](e2e/FN04_ESC07.pdf) |

ESC05 y ESC06 usan `MockWebServer` porque un error de carga no se puede provocar contra BackVynils sin detenerlo, y ni el backend desplegado ni los datos de ejemplo locales tienen bandas con integrantes.

## Cobertura de los criterios de aceptación

| Criterio | Escenarios |
|---|---|
| Acceso al detalle al seleccionar un artista en el listado (HU03) | Todos |
| Imagen, nombre, descripción y etiqueta "Músico" o "Banda" | ESC01, ESC02 |
| Músico: fecha de nacimiento | ESC01, ESC05, ESC07 |
| Banda: fecha de creación e integrantes con imagen y nombre | ESC02, ESC06 |
| Álbumes con portada, nombre y año | ESC01, ESC02 |
| Al seleccionar un álbum se navega a su detalle (HU02) | ESC03 |
| Premios con nombre, organización y fecha | ESC01, ESC02 |
| "Sin álbumes" y "Sin premios" | ESC04 |
| Imagen por defecto si la imagen o la portada no carga | ESC01, ESC06 |
| Indicador de carga | No se cubre con E2E: contra el backend real la carga es demasiado rápida para observarla. Lo cubre `ArtistaDetalleViewModelTest`. |
| "No se pudo cargar el artista" y "Reintentar" | ESC05 |

## Resultados

Emulador Pixel 8 (API 34), 2026-11-07.

| Ejecución | Resultado |
|---|---|
| `ArtistaDetalleE2ETest` contra el backend desplegado (evidencia de esta carpeta) | `OK (7 tests)`, ninguno omitido; 30 capturas en "Ok" |
| `ArtistaDetalleE2ETest` contra BackVynils local (`-PbaseUrl=http://10.0.2.2:3000/`) | `OK (7 tests)`; ESC04 omitido, porque los datos de ejemplo no tienen un artista sin álbumes ni premios |
| Suite instrumentada completa | `OK (138 tests)` |
| `testDebugUnitTest` y `lintDebug` | 220 sin fallos; 0 errores de lint |

En la revisión del PR se corrió la clase 3 veces por dispositivo, en paralelo, en un Motorola Edge 30 (API 34), el emulador Pixel 8 (API 34) y un emulador Nexus 5X (API 23). Contra el backend desplegado pasó 3/3 `OK (7 tests)` en cada uno, sin omitidos y con 30 capturas por dispositivo; contra BackVynils local, 3/3 con ESC04 omitido. No hubo fallos intermitentes: estas pruebas no escriben en ningún campo, así que no les afecta el teclado en API 23.

## Cómo correrlas

```bash
./gradlew installDebug installDebugAndroidTest        # backend desplegado (por defecto)
adb shell rm -rf /sdcard/Android/data/com.example.vinilos/files/e2e-evidencia
adb shell am instrument -w -e class com.example.vinilos.ArtistaDetalleE2ETest com.example.vinilos.test/androidx.test.runner.AndroidJUnitRunner
adb pull /sdcard/Android/data/com.example.vinilos/files/e2e-evidencia ./e2e-evidencia
```
