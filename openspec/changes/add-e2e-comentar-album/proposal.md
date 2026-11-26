# Proposal

## Why

La HU09 (comentar un álbum) se integró en `develop` (#112) con pruebas unitarias y de Compose UI. Falta la T09-5 (issue #51): las pruebas E2E del flujo completo contra el backend real, con evidencia publicada en el repositorio.

## What Changes

- Nueva clase `ComentarAlbumE2ETest` con los tres escenarios más importantes, FN09-ESC01 a FN09-ESC03:
  - **Contra BackVynils:** salir con algo escrito pregunta "¿Descartar comentario?"; el Coleccionista califica, opina, publica y ve el comentario en el detalle.
  - **Con `MockWebServer`:** la publicación tarda y falla, con lo escrito conservado, y el reintento la publica.
- Los escenarios que escriben comentan un álbum propio (`POST /albums`) que se borra al terminar con sus comentarios (`AlbumesDePrueba`, de `add-e2e-agregar-tracks`).
- Evidencia por escenario en PDF en `docs/pruebas/sprint-3/HU09/e2e/`, sección E2E en sus resultados y fila de la clase en la tabla de E2E del README.

No cambia el código de la app.

## Capabilities

### New Capabilities

Ninguna.

### Modified Capabilities

Ninguna: el cambio solo agrega pruebas y evidencia (`skip_specs: true`).

## Impact

- `app/src/androidTest/java/com/example/vinilos/ComentarAlbumE2ETest.kt` (nuevo).
- `docs/pruebas/sprint-3/HU09/` y `README.md`.
- Las pruebas publican comentarios, así que se corren contra BackVynils local.
- Cierra la T09-5 (issue #51).
