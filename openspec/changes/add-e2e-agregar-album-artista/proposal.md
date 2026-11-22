# Proposal

## Why

La HU15 (agregar un álbum a un artista) se integró en `develop` (#114) con pruebas unitarias y de Compose UI. Falta la T15-5 (issue #56): las pruebas E2E del flujo completo contra el backend real, con evidencia publicada en el repositorio.

## What Changes

- Nueva clase `AgregarAlbumArtistaE2ETest` con los tres escenarios más importantes, FN15-ESC01 a FN15-ESC03:
  - **Contra BackVynils:** una búsqueda sin resultados quita la selección y "Cancelar" cierra sin asociar; el Coleccionista asocia un álbum a una banda, que conserva los álbumes que ya tenía.
  - **Con `MockWebServer`:** la asociación tarda y falla, con la ventana y la selección conservadas, y el reintento la guarda.
- Lo que se asocia es siempre un álbum propio (`POST /albums`) que se borra al terminar, y con él su relación con el artista (`AlbumesDePrueba`, de `add-e2e-agregar-tracks`).
- Evidencia por escenario en PDF en `docs/pruebas/sprint-3/HU15/e2e/`, sección E2E en sus resultados y fila de la clase en la tabla de E2E del README.

No cambia el código de la app.

## Capabilities

### New Capabilities

Ninguna.

### Modified Capabilities

Ninguna: el cambio solo agrega pruebas y evidencia (`skip_specs: true`).

## Impact

- `app/src/androidTest/java/com/example/vinilos/AgregarAlbumArtistaE2ETest.kt` (nuevo).
- `docs/pruebas/sprint-3/HU15/` y `README.md`.
- Las pruebas asocian álbumes, así que se corren contra BackVynils local.
- Cierra la T15-5 (issue #56).
