# Tasks

## 1. Pruebas E2E del detalle del artista (T04-5, issue #28)

- [x] 1.1 Crear `ArtistaDetalleE2ETest` con el repositorio real que cuenta las consultas del detalle (D2), la selección de artistas a partir de los datos reales (D1) y la navegación desde el listado (D4); verificar que `:app:assembleDebugAndroidTest` compila.
- [x] 1.2 Implementar FN04-ESC01 (músico), FN04-ESC02 (banda), FN04-ESC03 (abrir un álbum y volver sin repetir la consulta) y FN04-ESC04 (sin álbumes ni premios) contra el backend real; verificar con `am instrument` de la clase en el Pixel 8 contra el backend local.
- [x] 1.3 Implementar FN04-ESC05 (error y "Reintentar") y FN04-ESC06 (banda con integrantes) con `MockWebServer` (D3); verificar con la misma ejecución.
- [x] 1.4 Implementar FN04-ESC07 (rotación sin repetir la consulta) con un giro real de la pantalla; verificar con la misma ejecución y revisando que la captura quede en horizontal.

## 2. Verificación

- [x] 2.1 Ejecutar la clase contra el backend desplegado y contra el local, y la clase en el emulador API 23; verificar `OK (7 tests)` sin escenarios omitidos. Hecho en el Pixel 8: desplegado `OK (7 tests)` sin omitidos y local `OK (7 tests)` con ESC04 omitido por falta de datos. La corrida en API 23 la hizo la revisión del PR en un emulador Nexus 5X: 3/3 `OK (7 tests)` contra el desplegado y 3/3 con ESC04 omitido contra el local.
- [x] 2.2 Ejecutar `testDebugUnitTest`, `lintDebug` y la suite instrumentada completa en el Pixel 8; verificar que no hay fallos ni errores de lint.

## 3. Evidencia y documentación

- [x] 3.1 Traer la evidencia con `adb pull` y versionar los PDF de FN04_ESC01 a FN04_ESC07 en `docs/pruebas/sprint-2/HU04/e2e/` (D5); verificar que cada PDF abre y está en "Ok".
- [x] 3.2 Escribir `docs/pruebas/sprint-2/HU04/README.md` con la relación entre escenarios y criterios, y agregar la HU04 a la tabla de E2E del README; verificar revisando el diff.
- [x] 3.3 Preparar la tabla de escenarios (descripción y resultado esperado) para el inventario de pruebas en Excel; verificar que cubre los 7 escenarios.

## Workflow follow-up

- Abrir el PR hacia `develop` con `Closes #28`.
- Archivar este cambio después de `add-detalle-artista`.
