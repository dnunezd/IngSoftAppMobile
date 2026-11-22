# Design

## Context

Ver proposal.md (Why). El detalle del álbum expone `TagListaDetalleAlbum` y `TagAgregarCancionDetalle`, y el formulario `TagFormularioAgregarCancion`, `TagNombreCancion`, `TagDuracionCancion`, `TagGuardarCancion` y `TagCancelarAgregarCancion`. `AlbumRepository` ya está en `AppContainer` y se puede reemplazar.

Criterios de la HU08 (spec `agregar-tracks` de `add-agregar-tracks`) y escenario que cubre cada uno:

| #   | Criterio                                                                                                  | Escenario    |
|-----|-----------------------------------------------------------------------------------------------------------|--------------|
| 1   | Solo el Coleccionista ve la acción para agregar una canción                                               | ESC01, ESC02 |
| 2   | El formulario abre vacío con "Campo obligatorio" en ambos campos y "Guardar" deshabilitado                | ESC02, ESC04 |
| 3   | La duración cumple `m:ss`; si no, "Usa el formato m:ss, por ejemplo 5:05" y "Guardar" deshabilitado       | ESC03        |
| 4   | Al guardar, "Canción agregada", vuelve al detalle y la lista muestra la canción con su duración           | ESC02, ESC05 |
| 5   | Mientras guarda, "Guardando..." deshabilitado y una sola petición                                         | ESC05        |
| 6   | "Cancelar" cierra sin guardar y el formulario vuelve vacío                                                | ESC04        |
| 7   | Si falla, "No se pudo agregar la canción, intenta de nuevo", conserva los valores y permite reintentar    | ESC05        |
| 8   | Si el álbum no existe, "El álbum ya no está disponible" y permite regresar                                | ESC06        |

## Goals / Non-Goals

**Goals:**
- Recorrer la app como el usuario, desde el ingreso, contra el backend real.
- Cubrir con `MockWebServer` los errores que el backend real no provoca a voluntad (criterios 5, 7 y 8).
- No dejar canciones ni álbumes de prueba en el backend.

**Non-Goals:**
- Pruebas de regresión (T08-6) y rotación (ya cubierta por las pruebas de Compose UI de la HU08).

## Decisions

**D1. Repositorio real que cuenta los envíos.** Un `AlbumRepository` que delega en el real y cuenta las llamadas a `agregarCancion`, para comprobar que validar o cancelar no envía nada.

**D2. Un álbum propio por escenario.** Los escenarios del Coleccionista crean su álbum con `POST /albums` y le agregan la canción desde la app; así parten de un álbum sin canciones y las corridas se pueden repetir. `AlbumesDePrueba` lo borra en `@After`, aunque la prueba falle. BackVynils no borra un álbum con canciones (la clave foránea lo impide): primero se llama a `DELETE /albums/{id}/tracks/{id}`, que desvincula la canción del álbum (queda en la tabla sin álbum, invisible para la API), y luego a `DELETE /albums/{id}`.

**D3. Datos reales para lo que solo se lee.** El Visitante usa el primer álbum del backend (si no hay, se omite con `Assume`) y el Coleccionista, el primer coleccionista.

**D4. Capturas con ventana emergente.** `captureToImage()` solo funciona con una raíz Compose. Con un diálogo o una hoja inferior abiertos hay dos, así que `EvidenciaE2E` captura entonces la pantalla completa con `UiAutomation`, como ya hacía en API 23-25. Lo necesitan las E2E de HU09 (diálogo de descartar) y HU15 (ventana de asociar).

**D5. Backend local.** Las pruebas escriben datos, así que la evidencia se genera contra BackVynils local.

## Risks / Trade-offs

- [La sección de canciones de un álbum real queda fuera de pantalla y aún sin componer] → Se espera a que la lista se pueda desplazar hasta ella.
- [Un álbum de prueba queda en el backend si la instrumentación se interrumpe a la fuerza] → `@After` no corre en ese caso; el álbum se reconoce por el nombre `E2E FN08 <marca de tiempo>`.
