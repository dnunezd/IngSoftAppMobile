# Tasks

## 1. Pruebas E2E de agregar canción (T08-5, issue #45)

- [x] 1.1 Crear `AlbumesDePrueba` (álbum propio por escenario y borrado con sus canciones y comentarios, D2) y ajustar `EvidenciaE2E` para capturar con una ventana emergente abierta (D4); verificar con curl contra BackVynils local que el álbum se borra y que los artistas conservan sus álbumes.
- [x] 1.2 Crear `AgregarCancionE2ETest` con FN08-ESC01 y ESC02 contra el backend real y FN08-ESC03 con `MockWebServer`; verificar que `:app:assembleDebugAndroidTest` compila.
- [x] 1.3 Ejecutar la clase con `am instrument` en el Samsung SM-S938B contra BackVynils local. Resultado (2026-11-22): `OK (9 tests)` junto con las E2E de HU09 y HU15; el backend quedó con sus 4 álbumes de ejemplo y ningún álbum de prueba. La primera versión tenía seis escenarios; se dejaron los tres más importantes.

## 2. Evidencia y documentación

- [x] 2.1 Traer la evidencia con `adb pull` y versionar los PDF de FN08_ESC01 a FN08_ESC03 en `docs/pruebas/sprint-3/HU08/e2e/`; verificar que ningún paso quedó sin captura.
- [x] 2.2 Agregar el README de la HU08 con los escenarios y criterios, y la clase a la tabla de E2E del README.

## Workflow follow-up

- Abrir el PR hacia `develop` con `Closes #45`, después de integrar la HU08 (#115).
