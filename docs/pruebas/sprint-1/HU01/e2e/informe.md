# HU01 · Evidencia E2E (`CatalogoE2ETest`)

Ejecución del 2026-10-31 con `am instrument` contra BackVynils (datos de `sql/data.sql`), con `adb reverse tcp:3000 tcp:3000` y la app instalada con `-PbaseUrl=http://localhost:3000/`. `EvidenciaE2E` guarda cada escenario en su propia carpeta `e2e-evidencia/FN01_ESCxx/` del dispositivo, con un pantallazo por paso, un `informe.md` y un `FN01_ESCxx.pdf` (portada con el dispositivo y la tabla de pasos, y una página por captura). Por acuerdo del equipo, las capturas y los PDF no se versionan: se conservan en el equipo donde se ejecutaron y se pueden regenerar con los comandos de [`resultados.md`](../resultados.md). "Ok" significa que la captura se guardó; el resultado de las pruebas es el de `am instrument`.

## Resumen por dispositivo

| Dispositivo | Tipo | Android | `am instrument` | Carpetas | Capturas | PDF | Cierres |
|---|---|---|---|---|---|---|---|
| Motorola Edge 30 | Físico | 14 (API 34) | OK (5 tests) | 5 | 16 | 5 | 0 |
| Nexus 5X API 23 | Emulador | 6.0 (API 23) | OK (5 tests) | 5 | 16 | 5 | 0 |
| Pixel 8 API 34 | Emulador | 14 (API 34) | OK (5 tests) | 5 | 16 | 5 | 0 |

## Escenarios

| Id | Escenario | Backend | Prueba | Pasos con captura |
|---|---|---|---|---|
| FN01-ESC01 | Visitante y Coleccionista ven los 4 álbumes en orden con portada, nombre y género | Real | `visitanteYColeccionistaVenLosCuatroAlbumesEnOrden` | `01_inicio_visitante`, `02_catalogo_visitante`, `03_inicio_coleccionista`, `04_catalogo_coleccionista` |
| FN01-ESC02 | Recorrer hasta el final y deslizar para actualizar | Real | `recorrerHastaElFinalYDeslizarParaActualizar` | `01_inicio_visitante`, `02_final_del_catalogo`, `03_tras_actualizar` |
| FN01-ESC03 | Backend caído → error → "Reintentar" → lista | MockWebServer | `conElBackendCaidoSeVeElErrorYReintentarRecuperaElCatalogo` | `01_inicio_visitante`, `02_backend_caido`, `03_recuperado` |
| FN01-ESC04 | Al girar conserva la posición y no vuelve a consultar | Real | `alGirarConservaLaPosicionSinVolverAConsultar` | `01_inicio_visitante`, `02_antes_de_girar`, `03_despues_de_girar` |
| FN01-ESC05 | Tocar un álbum abre su detalle real y "Atrás" vuelve sin recargar | Real | `tocarUnAlbumAbreSuDetalleYAtrasVuelveAlCatalogo` | `01_inicio_visitante`, `02_detalle`, `03_regreso_al_catalogo` |

## Detalle de los pasos en el Motorola Edge 30 (dispositivo físico)

### FN01-ESC01 · `FN01_ESC01/FN01_ESC01.pdf`

| Paso | Estado | Captura | Hora |
|---|---|---|---|
| 01_inicio_visitante | Ok | 01_inicio_visitante.png | 2026-10-31 16:02:16 |
| 02_catalogo_visitante | Ok | 02_catalogo_visitante.png | 2026-10-31 16:02:19 |
| 03_inicio_coleccionista | Ok | 03_inicio_coleccionista.png | 2026-10-31 16:02:22 |
| 04_catalogo_coleccionista | Ok | 04_catalogo_coleccionista.png | 2026-10-31 16:02:25 |

### FN01-ESC02 · `FN01_ESC02/FN01_ESC02.pdf`

| Paso | Estado | Captura | Hora |
|---|---|---|---|
| 01_inicio_visitante | Ok | 01_inicio_visitante.png | 2026-10-31 16:02:27 |
| 02_final_del_catalogo | Ok | 02_final_del_catalogo.png | 2026-10-31 16:02:30 |
| 03_tras_actualizar | Ok | 03_tras_actualizar.png | 2026-10-31 16:02:33 |

### FN01-ESC03 · `FN01_ESC03/FN01_ESC03.pdf`

| Paso | Estado | Captura | Hora |
|---|---|---|---|
| 01_inicio_visitante | Ok | 01_inicio_visitante.png | 2026-10-31 16:02:09 |
| 02_backend_caido | Ok | 02_backend_caido.png | 2026-10-31 16:02:12 |
| 03_recuperado | Ok | 03_recuperado.png | 2026-10-31 16:02:14 |

### FN01-ESC04 · `FN01_ESC04/FN01_ESC04.pdf`

| Paso | Estado | Captura | Hora |
|---|---|---|---|
| 01_inicio_visitante | Ok | 01_inicio_visitante.png | 2026-10-31 16:02:42 |
| 02_antes_de_girar | Ok | 02_antes_de_girar.png | 2026-10-31 16:02:45 |
| 03_despues_de_girar | Ok | 03_despues_de_girar.png | 2026-10-31 16:02:48 |

### FN01-ESC05 · `FN01_ESC05/FN01_ESC05.pdf`

| Paso | Estado | Captura | Hora |
|---|---|---|---|
| 01_inicio_visitante | Ok | 01_inicio_visitante.png | 2026-10-31 16:02:34 |
| 02_detalle | Ok | 02_detalle.png | 2026-10-31 16:02:37 |
| 03_regreso_al_catalogo | Ok | 03_regreso_al_catalogo.png | 2026-10-31 16:02:41 |
