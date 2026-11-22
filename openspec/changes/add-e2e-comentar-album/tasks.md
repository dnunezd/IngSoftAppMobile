# Tasks

## 1. Pruebas E2E de comentar un álbum (T09-5, issue #51)

- [x] 1.1 Crear `ComentarAlbumE2ETest` con FN09-ESC01 y ESC02 contra el backend real y FN09-ESC03 con `MockWebServer`, usando `AlbumesDePrueba` (`add-e2e-agregar-tracks`); verificar que `:app:assembleDebugAndroidTest` compila.
- [x] 1.2 Ejecutar la clase con `am instrument` en el Samsung SM-S938B contra BackVynils local. Resultado (2026-11-22): `OK (9 tests)` junto con las E2E de HU08 y HU15; el backend quedó sin álbumes ni comentarios de prueba. La primera versión tenía cinco escenarios; se dejaron los tres más importantes, sin el del Visitante.

## 2. Evidencia y documentación

- [x] 2.1 Traer la evidencia con `adb pull` y versionar los PDF de FN09_ESC01 a FN09_ESC03 en `docs/pruebas/sprint-3/HU09/e2e/`; verificar que ningún paso quedó sin captura.
- [x] 2.2 Agregar la sección E2E a `docs/pruebas/sprint-3/HU09/resultados.md` y la clase a la tabla de E2E del README.

## Workflow follow-up

- Abrir el PR hacia `develop` con `Closes #51`.
