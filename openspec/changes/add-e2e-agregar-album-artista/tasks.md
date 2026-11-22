# Tasks

## 1. Pruebas E2E de agregar un álbum a un artista (T15-5, issue #56)

- [x] 1.1 Crear `AgregarAlbumArtistaE2ETest` con FN15-ESC01 y ESC02 contra el backend real y FN15-ESC03 con `MockWebServer`, usando `AlbumesDePrueba` (`add-e2e-agregar-tracks`); verificar que `:app:assembleDebugAndroidTest` compila.
- [x] 1.2 Ejecutar la clase con `am instrument` en el Samsung SM-S938B contra BackVynils local. Resultado (2026-11-22): `OK (9 tests)` junto con las E2E de HU08 y HU09; el músico 100 quedó con los álbumes 100 y 101 y la banda 101 con los álbumes 102 y 103, como antes de la corrida. La primera versión tenía cinco escenarios; se dejaron los tres más importantes.

## 2. Evidencia y documentación

- [x] 2.1 Traer la evidencia con `adb pull` y versionar los PDF de FN15_ESC01 a FN15_ESC03 en `docs/pruebas/sprint-3/HU15/e2e/`; verificar que ningún paso quedó sin captura.
- [x] 2.2 Agregar la sección E2E a `docs/pruebas/sprint-3/HU15/resultados.md` y la clase a la tabla de E2E del README.

## Workflow follow-up

- Abrir el PR hacia `develop` con `Closes #56`.
