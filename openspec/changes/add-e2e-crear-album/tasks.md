# Tasks

## 1. Pruebas E2E de crear álbum (T07-5, issue #39)

- [x] 1.1 Crear `CrearAlbumE2ETest` con el repositorio que registra lo creado (D1), la limpieza en `@After` (D2), el ingreso como Coleccionista a partir de los datos reales (D4) y los ayudantes para llenar el formulario y elegir la fecha (D3); verificar que `:app:assembleDebugAndroidTest` compila.
- [x] 1.2 Implementar FN07-ESC01 (el Visitante no ve "Crear álbum") y FN07-ESC02 (crear un álbum válido, ver "Álbum creado", el álbum con NUEVO en el catálogo y la fecha guardada sin desfase); verificar con `am instrument` de la clase en el Pixel 8 contra BackVynils local y que el backend quede sin álbumes de prueba.
- [x] 1.3 Implementar FN07-ESC03 (cancelar con datos), FN07-ESC04 (campos vacíos) y FN07-ESC05 (URL inválida), comprobando que no se envía nada; verificar con la misma ejecución.
- [x] 1.4 Implementar FN07-ESC06 (girar con el formulario lleno conserva los datos) con un giro real, y FN07-ESC07 (backend caído con `MockWebServer`, D5); verificar con la misma ejecución y revisando que la captura de ESC06 quede en horizontal.

## 2. Verificación

- [x] 2.1 Ejecutar `testDebugUnitTest`, `lintDebug` y la suite instrumentada completa en el Pixel 8; verificar que no hay fallos ni errores de lint.

## 3. Evidencia y documentación

- [x] 3.1 Traer la evidencia con `adb pull` y versionar los PDF de FN07_ESC01 a FN07_ESC07 en `docs/pruebas/sprint-2/HU07/e2e/`; verificar que cada PDF abre y está en "Ok".
- [x] 3.2 Agregar la sección E2E al README de la HU07 con la relación entre escenarios y criterios, y la clase a la tabla de E2E del README; verificar revisando el diff.
- [x] 3.3 Preparar la tabla de escenarios (descripción y resultado esperado) para el inventario de pruebas en Excel; verificar que cubre los 7 escenarios.

## Workflow follow-up

- Abrir el PR hacia `develop` con `Closes #39`.
- Archivar este cambio después de `add-crear-album`.
