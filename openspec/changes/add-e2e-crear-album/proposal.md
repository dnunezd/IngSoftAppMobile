# Proposal

## Why

La HU07 (crear un álbum) se integró en `develop` con pruebas unitarias y un recorrido manual por `adb`, pero sin pruebas E2E versionadas: la T07-5 (issue #39) quedó diferida. La definición de terminado y el OBJ03 piden al menos dos escenarios E2E por historia, contra el backend real y con evidencia publicada en el repositorio.

## What Changes

- Nueva clase `CrearAlbumE2ETest` con los escenarios FN07-ESC01 a FN07-ESC07 descritos en la T07-5: el Visitante no ve la opción de crear; el Coleccionista crea un álbum válido y lo ve en el catálogo; cancela un formulario con datos; intenta guardar con campos vacíos; intenta con una URL inválida; gira la pantalla con el formulario lleno; y, con el backend caído, ve el error conservando los datos y reintenta.
- Los escenarios que escriben datos borran con `DELETE /albums/{id}` todo álbum que hayan creado, incluso si la prueba falla a mitad de camino.
- El backend caído se simula con `MockWebServer`, como en las E2E de HU01 y HU03.
- Evidencia por escenario con `EvidenciaE2E` y PDF versionados en `docs/pruebas/sprint-2/HU07/e2e/`, con una sección E2E en el README de la HU07.
- Fila de la clase en la tabla de E2E del README.

No cambia el código de la app. No se incluyen pruebas de regresión (T07-6).

## Capabilities

### New Capabilities

Ninguna.

### Modified Capabilities

Ninguna: el cambio solo agrega pruebas y evidencia (`skip_specs: true`).

## Impact

- `app/src/androidTest/java/com/example/vinilos/CrearAlbumE2ETest.kt` (nuevo).
- `docs/pruebas/sprint-2/HU07/` y `README.md`.
- Las pruebas crean y borran álbumes: se corren contra BackVynils local (`-PbaseUrl=http://10.0.2.2:3000/`) para no dejar rastro en el backend compartido de los tutores.
- Cierra la T07-5 (issue #39).
