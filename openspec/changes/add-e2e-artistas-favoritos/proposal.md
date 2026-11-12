# Proposal

## Why

La HU10 (agregar artistas favoritos) se integró en `develop` (#86) con pruebas unitarias, de Compose UI y una de navegación, todas con repositorios falsos. Falta la T10-5 (issue #33): las pruebas E2E del flujo completo contra el backend real, que la definición de terminado y el OBJ03 piden con al menos dos escenarios por historia y evidencia publicada en el repositorio. En la revisión de #86 también se señaló que ningún escenario agregaba un favorito contra el backend real.

## What Changes

- Nueva clase `ArtistasFavoritosE2ETest` con los escenarios FN10-ESC01 a FN10-ESC08:
  - **Contra BackVynils:** el Visitante no ve el botón; el Coleccionista agrega una banda y un músico como favoritos; el botón sigue en "Favorito" al cerrar y volver a abrir la app; un artista que ya es favorito aparece como "Favorito" y desactivado.
  - **Con `MockWebServer`:** no se puede comprobar si es favorito; falla el guardado; el artista ya no existe; "Guardando..." evita toques repetidos.
- Los escenarios que escriben en el backend crean su propio coleccionista con `POST /collectors` y lo borran al terminar con `DELETE /collectors/{id}`, que también borra sus favoritos, incluso si la prueba falla a mitad de camino. No se usa `DELETE /collectors/{id}/bands/{id}` ni `.../musicians/{id}`: en BackVynils responden 204 sin quitar el favorito.
- Evidencia por escenario con `EvidenciaE2E` y PDF versionados en `docs/pruebas/sprint-2/HU10/e2e/`, con una sección E2E en el README de la HU10.
- Fila de la clase en la tabla de E2E del README.

No cambia el código de la app. No se incluyen pruebas de regresión (T10-6).

## Capabilities

### New Capabilities

Ninguna.

### Modified Capabilities

Ninguna: el cambio solo agrega pruebas y evidencia (`skip_specs: true`).

## Impact

- `app/src/androidTest/java/com/example/vinilos/ArtistasFavoritosE2ETest.kt` (nuevo).
- `docs/pruebas/sprint-2/HU10/` y `README.md`.
- Las pruebas agregan y borran favoritos, así que se corren contra BackVynils local (`-PbaseUrl=http://10.0.2.2:3000/`) para no dejar rastro en el backend compartido de los tutores.
- Cierra la T10-5 (issue #33).
