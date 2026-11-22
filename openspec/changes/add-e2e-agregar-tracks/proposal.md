# Proposal

## Why

La HU08 (asociar canciones a un álbum) tiene pruebas unitarias y de Compose UI con repositorios falsos, pero le falta la T08-5 (issue #45): las pruebas E2E del flujo completo contra el backend real, que la definición de terminado pide con evidencia publicada en el repositorio.

## What Changes

- Nueva clase `AgregarCancionE2ETest` con los tres escenarios más importantes, FN08-ESC01 a FN08-ESC03:
  - **Contra BackVynils:** el Visitante no ve "Agregar canción"; el Coleccionista agrega una canción y la ve en el detalle.
  - **Con `MockWebServer`:** el guardado tarda y falla, con los datos conservados, y el reintento agrega la canción.
- Los escenarios que escriben trabajan sobre un álbum propio (`POST /albums`) que se borra al terminar con sus canciones (`AlbumesDePrueba`, compartido con las E2E de HU09 y HU15).
- `EvidenciaE2E` captura la pantalla completa cuando hay una ventana emergente abierta.
- Evidencia por escenario en PDF en `docs/pruebas/sprint-3/HU08/e2e/`, con su README, y fila de la clase en la tabla de E2E del README.

No cambia el código de la app.

## Capabilities

### New Capabilities

Ninguna.

### Modified Capabilities

Ninguna: el cambio solo agrega pruebas y evidencia (`skip_specs: true`).

## Impact

- `app/src/androidTest/java/com/example/vinilos/AgregarCancionE2ETest.kt` (nuevo).
- `app/src/androidTest/java/com/example/vinilos/e2e/AlbumesDePrueba.kt` (nuevo) y `EvidenciaE2E.kt`.
- `docs/pruebas/sprint-3/HU08/` y `README.md`.
- Las pruebas crean y borran álbumes y canciones, así que se corren contra BackVynils local para no dejar rastro en el backend compartido.
- Cierra la T08-5 (issue #45). Depende de la HU08 (#115).
