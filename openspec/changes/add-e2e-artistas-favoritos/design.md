# Design

## Context

Ver proposal.md (Why). El detalle del artista (`PantallaDetalleArtista`) ya expone las etiquetas de prueba de la sección de favorito: `TagBotonFavorito`, `TagReintentarFavorito`, `TagErrorFavorito`, `TagConfirmacionFavorito` y `TagNoDisponibleFavorito`. El listado expone `TagListaArtistas`, `TagContadorArtistas` y `tagArtista(artista)`, y el ingreso `TagEntrarColeccionista` y `tagColeccionista(id)`. `FavoriteRepository` ya está en `AppContainer` y se puede reemplazar.

Criterios de la [HU10](https://github.com/dnunezd/IngSoftAppMobile/wiki/HU10) y escenario que cubre cada uno:

| # | Criterio | Escenario |
|---|---|---|
| 1 | Marcar como favorito una banda o un músico; el botón pasa de "Agregar Favorito" a "Favorito" | ESC02, ESC03 |
| 2 | Si ya es favorito, el botón aparece como "Favorito" y desactivado | ESC04, ESC05 |
| 3 | El botón refleja si es favorito cada vez que se abre el detalle, aunque se cierre y vuelva a abrir la app | ESC04 |
| 4 | Si no se puede comprobar, el botón aparece desactivado con opción de reintentar | ESC06 |
| 5 | El Visitante no ve el botón | ESC01 |
| 6 | Si el artista ya no existe, "El artista ya no está disponible" y botón para volver al listado, que se actualiza sin ese artista | ESC08 |
| 7 | Si no se puede guardar, "No se pudo guardar el favorito, intenta de nuevo" y el botón queda disponible para reintentar | ESC07 |
| 8 | Mientras se guarda, el botón se desactiva y muestra "Guardando..." | ESC07 |
| 9 | Al guardar, mensaje breve "<artista> se agregó a tus favoritos" que desaparece solo; si falla, no aparece | ESC02, ESC07 |

## Goals / Non-Goals

**Goals:**
- Cubrir los escenarios de la T10-5 recorriendo la app como el usuario, desde el ingreso: banda y músico, cerrar y volver a abrir, y Visitante.
- Cubrir con `MockWebServer` los criterios que el backend real no puede provocar a voluntad (4, 6, 7 y 8).
- No dejar favoritos de prueba en el backend.

**Non-Goals:**
- Pruebas de regresión (T10-6), que no se harán.
- "Quitar de favoritos" desde la app: no es parte de la HU10.

## Decisions

**D1. Repositorio real que cuenta las llamadas.** Un `FavoriteRepository` que llama al real y cuenta las llamadas a `esFavorito` y `agregarFavorito`. Sirve para comprobar que cada escenario envía un solo favorito, que al volver a abrir el detalle se consulta de nuevo el backend (ESC04) y que un artista que ya es favorito no se envía otra vez (ESC05).

**D2. Un coleccionista propio por escenario, borrado en `@After`.** Cada escenario que agrega favoritos crea un coleccionista sin favoritos con `POST /collectors` y lo borra con `DELETE /collectors/{id}`, que también borra la relación con sus favoritos. Corre aunque la prueba falle.

*Alternativa descartada:* borrar cada favorito con `DELETE /collectors/{id}/bands/{id}` o `.../musicians/{id}`. En BackVynils (`collectorperformer.service.ts`, `deleteCollectorPerformer`) el resultado de `favoritePerformers.filter(...)` no se asigna, así que responde 204 y el favorito sigue. En la primera corrida los favoritos de prueba quedaron en el backend local y hubo que quitarlos a mano de la tabla `performer_collectors_collector`.

**D3. Artistas a partir de los datos reales.** Cada escenario usa la primera banda o el primer músico que devuelve `GET /bands` o `GET /musicians`, sin fijar ids; si no hay, se omite con `Assume`. Como el coleccionista es nuevo (D2), no tiene favoritos: ESC02 a ESC04 parten de "Agregar Favorito", y ESC05 agrega el favorito directo en el backend antes de abrir la app. Así las corridas se pueden repetir sin depender de los favoritos de ejemplo.

**D4. Cerrar y volver a abrir la app (ESC04).** Se cierra el `ActivityScenario` y se lanza uno nuevo, que vuelve a pedir el ingreso; se entra con el mismo coleccionista. El proceso de prueba no se puede matar sin terminar la instrumentación, pero la HU10 no guarda nada en el dispositivo: el estado del botón sale siempre de `GET /collectors/{id}/performers`, y D1 comprueba que esa consulta se repite.

**D5. `MockWebServer` para los errores.** Igual que en las E2E de HU01, HU03 y HU07. Un `Dispatcher` responde coleccionistas, artistas y detalle, y según el escenario:
- corta `GET /collectors/{id}/performers` hasta que la prueba "levanta" el servidor (ESC06);
- corta `POST` y luego lo deja pasar (ESC07);
- responde 404 al `POST` y quita al artista de `GET /musicians` y `GET /bands` (ESC08).

**D6. "Guardando..." visible (ESC07).** El primer `POST` se responde con un retraso (`setHeadersDelay`), para comprobar en ese intervalo que el botón muestra "Guardando..." y está desactivado, y que un segundo toque no envía otra petición.

**D7. Backend local.** Las pruebas escriben datos, así que la evidencia se genera contra BackVynils local. Además, el backend desplegado responde 500 desde el 9 de noviembre.

## Risks / Trade-offs

- [El mensaje de confirmación desaparece antes de verificarlo] → Se espera la etiqueta `TagConfirmacionFavorito` justo después del toque, dentro de sus 2,5 s, y después se espera a que desaparezca.
- [El coleccionista nuevo queda al final del listado de coleccionistas] → La prueba espera a que el listado cargue y se desplaza hasta él antes de tocarlo.
- [Un coleccionista de prueba queda en el backend si la instrumentación se interrumpe a la fuerza] → `@After` no corre en ese caso; el coleccionista se reconoce por el nombre `E2E FN10 <marca de tiempo>` y se borra con `DELETE /collectors/{id}`.
- [Los datos de ejemplo cambian] → D3 elige los artistas en cada corrida y omite el escenario con `Assume` en lugar de fallar.
