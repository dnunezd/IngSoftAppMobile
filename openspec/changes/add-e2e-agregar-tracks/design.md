# Design

## Context

Ver proposal.md (Why). El detalle del álbum expone `TagListaDetalleAlbum` y `TagAgregarCancionDetalle`, y el formulario `TagFormularioAgregarCancion`, `TagNombreCancion`, `TagDuracionCancion`, `TagGuardarCancion` y `TagCancelarAgregarCancion`. `AlbumRepository` ya está en `AppContainer` y se puede reemplazar.

Criterios de la HU08 (spec `agregar-tracks` de `add-agregar-tracks`) y escenario que cubre cada uno:

| #   | Criterio                                                                                                  | Escenario           |
|-----|-----------------------------------------------------------------------------------------------------------|---------------------|
| 1   | Solo el Coleccionista ve la acción para agregar una canción                                               | ESC02, ESC04        |
| 2   | El formulario abre vacío con "Campo obligatorio" en ambos campos y "Guardar" deshabilitado                | ESC02, ESC05        |
| 3   | La duración cumple `m:ss`; si no, "Usa el formato m:ss, por ejemplo 5:05" y "Guardar" deshabilitado       | ESC01, ESC06        |
| 4   | Al guardar, "Canción agregada", vuelve al detalle y la lista muestra la canción con su duración           | ESC02, ESC03, ESC06 |
| 5   | Mientras guarda, "Guardando..." deshabilitado y una sola petición                                         | ESC03               |
| 6   | "Cancelar" cierra sin guardar y el formulario vuelve vacío                                                | ESC05               |
| 7   | Si falla, "No se pudo agregar la canción, intenta de nuevo", conserva los valores y permite reintentar    | ESC03               |
| 8   | Si el álbum no existe, "El álbum ya no está disponible" y permite regresar                                | ESC07               |
| 9   | Al girar, el formulario conserva lo escrito, los errores y envía una sola vez                             | ESC06               |

La [T08-5](https://github.com/dnunezd/IngSoftAppMobile/wiki/HU08) pide, además del flujo completo y del error con el backend caído, que el Visitante no vea la opción (ESC04), cancelar con datos escritos (ESC05), rotar la pantalla (ESC06) y guardar sobre un álbum ya eliminado (ESC07). Estos cuatro se agregaron por la revisión del PR (#116), que señaló que la primera versión no los cubría.

## Goals / Non-Goals

**Goals:**
- Cubrir todos los casos de la T08-5, recorriendo la app desde el ingreso contra el backend real.
- Cubrir con `MockWebServer` el backend caído, que el backend real no provoca a voluntad (criterios 5 y 7).
- No dejar canciones ni álbumes de prueba en el backend.

**Non-Goals:**
- Pruebas de regresión (T08-6).

## Decisions

**D1. Repositorio real que cuenta los envíos.** Un `AlbumRepository` que delega en el real y cuenta las llamadas a `agregarCancion`, para comprobar que se envía una sola canción (también al girar) y que una duración inválida, cancelar o el Visitante no envían nada.

**D2. Un álbum propio por escenario.** Los escenarios del Coleccionista crean su álbum con `POST /albums` y le agregan la canción desde la app; así parten de un álbum sin canciones y las corridas se pueden repetir. `AlbumesDePrueba` lo borra en `@After`, aunque la prueba falle. BackVynils no borra un álbum con canciones (la clave foránea lo impide): primero se llama a `DELETE /albums/{id}/tracks/{id}`, que desvincula la canción del álbum (queda en la tabla sin álbum, invisible para la API), y luego a `DELETE /albums/{id}`.

La secuencia de ids de `album` arranca en 1, pero los datos de ejemplo usan los ids 100 a 103: cuando la secuencia llega a ellos, `POST /albums` responde 500 por clave duplicada (así fallaron 4 de 27 corridas en un Pixel 8 durante la revisión). Como cada intento fallido avanza la secuencia, `AlbumesDePrueba` reintenta hasta 10 veces. Se comprobó llevando la secuencia a 99: la prueba pasó y su álbum quedó con el id 104.

**D3. Datos reales.** Las pruebas entran con el primer coleccionista del backend; el Visitante (ESC04) usa el primer álbum real, y si no hay se omite con `Assume`.

**D6. Álbum eliminado de verdad (ESC07).** Con el formulario lleno, la prueba borra su álbum con `DELETE /albums/{id}` y luego guarda: BackVynils responde 404 al `POST /albums/{id}/tracks`, como cuando otra persona elimina el álbum. No hace falta `MockWebServer`.

**D7. Girar de verdad (ESC06).** Se cambia `requestedOrientation` de la Activity, como en FN03 y FN07, para que la evidencia se vea en horizontal; se gira con un error visible y se vuelve a vertical antes de guardar.

**D4. Capturas con ventana emergente.** `captureToImage()` solo funciona con una raíz Compose. Con un diálogo o una hoja inferior abiertos hay dos, así que `EvidenciaE2E` captura entonces la pantalla completa con `UiAutomation`, como ya hacía en API 23-25. Lo necesitan las E2E de HU09 (diálogo de descartar) y HU15 (ventana de asociar, una hoja inferior).

**D5. Backend local.** Las pruebas escriben datos, así que la evidencia se genera contra BackVynils local.

## Risks / Trade-offs

- [Un álbum de prueba queda en el backend si la instrumentación se interrumpe a la fuerza] → `@After` no corre en ese caso; el álbum se reconoce por el nombre `E2E FN08 <marca de tiempo>`.
