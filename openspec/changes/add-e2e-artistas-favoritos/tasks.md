# Tasks

## 1. Pruebas E2E de artistas favoritos (T10-5, issue #33)

- [x] 1.1 Crear `ArtistasFavoritosE2ETest` con el repositorio que cuenta las llamadas (D1), el coleccionista propio que se borra en `@After` (D2), la elección de artistas a partir de los datos reales (D3) y los ayudantes para ingresar como Coleccionista y abrir el detalle de un artista; verificar que `:app:assembleDebugAndroidTest` compila.
- [x] 1.2 Implementar FN10-ESC01 (el Visitante no ve el botón en el detalle de una banda ni de un músico), FN10-ESC02 (agregar una banda: "Agregar Favorito" pasa a "Favorito" desactivado, aparece y desaparece la confirmación, y el backend la devuelve en `GET /collectors/{id}/performers`) y FN10-ESC03 (lo mismo con un músico); verificar con `am instrument` de la clase en el Pixel 8 contra BackVynils local y que el backend queda sin favoritos de prueba. Resultado (2026-11-10): la primera corrida pasó 8/8 pero dejó los favoritos, porque el `DELETE` de favoritos de BackVynils no borra; se cambió a un coleccionista propio por escenario (D2). Con ese cambio, 3 corridas seguidas `OK (8 tests)`, sin omitidos, y el backend quedó con sus 2 coleccionistas y sus 2 favoritos de ejemplo.
- [x] 1.3 Implementar FN10-ESC04 (agregar, cerrar y volver a abrir la app, entrar con el mismo coleccionista y ver "Favorito" desactivado, con una nueva consulta al backend) y FN10-ESC05 (un artista que ya es favorito aparece como "Favorito" y desactivado sin enviar nada); verificar con la misma ejecución.
- [x] 1.4 Implementar con `MockWebServer` (D5, D6) FN10-ESC06 (no se puede comprobar: botón desactivado con "Reintentar", que con el servidor arriba deja "Agregar Favorito"), FN10-ESC07 (al guardar se ve "Guardando..." desactivado y un segundo toque no envía nada; el guardado falla con su mensaje y sin confirmación; el reintento lo guarda) y FN10-ESC08 (el artista ya no existe: mensaje, volver al listado y el listado sin el artista); verificar con la misma ejecución.

## 2. Verificación

- [x] 2.1 Ejecutar `testDebugUnitTest`, `lintDebug` y la suite instrumentada completa en el Pixel 8; verificar que no hay fallos ni errores de lint. Resultado (2026-11-10, contra BackVynils local): 238 unitarias sin fallos, 0 errores de lint y `OK (152 tests)`.

## 3. Evidencia y documentación

- [x] 3.1 Traer la evidencia con `adb pull` y versionar los PDF de FN10_ESC01 a FN10_ESC08 en `docs/pruebas/sprint-2/HU10/e2e/`; verificar que cada PDF abre y está en "Ok".
- [x] 3.2 Agregar la sección E2E al README de la HU10 con la relación entre escenarios y criterios, y la clase a la tabla de E2E del README; verificar revisando el diff.
- [x] 3.3 Preparar la tabla de escenarios (descripción y resultado esperado) para el inventario de pruebas en Excel; verificar que cubre los 8 escenarios.

## Workflow follow-up

- Abrir el PR hacia `develop` con `Closes #33`.
- Archivar este cambio después de `add-artistas-favoritos`.
