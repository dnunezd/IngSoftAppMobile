# Proposal

## Why

La HU04 (consultar el detalle de un artista) se integró en `develop` con pruebas unitarias y de navegación, pero sin pruebas E2E: la T04-5 (issue #28) quedó pendiente. La definición de terminado del sprint y el OBJ03 de la estrategia de pruebas piden que cada historia tenga al menos dos escenarios E2E contra el backend real, con evidencia publicada en el repositorio.

## What Changes

- Nueva clase `ArtistaDetalleE2ETest` con los escenarios FN04-ESC01 a FN04-ESC07 como Visitante: detalle de un músico, detalle de una banda, abrir un álbum desde el detalle y volver, artista sin álbumes ni premios, error de carga con "Reintentar", banda con integrantes y rotación sin repetir la consulta.
- Los escenarios contra el backend real no asumen un conjunto de datos fijo: eligen el artista a partir de lo que devuelve el backend conectado y se omiten (sin fallar) si no hay un artista que cumpla la condición, igual que las E2E de HU00 a HU03 desde #79.
- Los casos que no se pueden provocar contra BackVynils (error de carga y banda con integrantes, que no existe en el backend desplegado ni en los datos de ejemplo) usan `MockWebServer`, como `RecuperacionTest` y FN03-ESC05.
- Evidencia por escenario con `EvidenciaE2E` (capturas, `informe.md` y PDF), y los PDF versionados en `docs/pruebas/sprint-2/HU04/e2e/`, junto con un `README.md` que relaciona los escenarios con los criterios de la HU04.
- Fila de la HU04 en la tabla de E2E del README.

No cambia el código de la app ni el comportamiento de ninguna pantalla.

## Capabilities

### New Capabilities

Ninguna.

### Modified Capabilities

Ninguna: el cambio solo agrega pruebas y evidencia (`skip_specs: true`).

## Impact

- `app/src/androidTest/java/com/example/vinilos/ArtistaDetalleE2ETest.kt` (nuevo).
- `docs/pruebas/sprint-2/HU04/` (nuevo) y `README.md`.
- Las E2E pasan de 19 a 26 escenarios. Siguen requiriendo dispositivo y backend, por lo que no corren en la CI.
- Cierra la T04-5 (issue #28).
