# Design

## Context

Ver proposal.md (Why). El detalle del álbum expone `TagComentarAlbum` (solo para el Coleccionista) y la pantalla de comentar expone `TagAlbumComentado`, `tagEstrella(n)`, `TagNivelCalificacion`, `TagOpinion`, `TagContadorOpinion`, `TagPublicarComentario`, `TagErrorPublicar`, `TagDialogoDescartar`, `TagContinuarEscribiendo`, `TagDescartarComentario`, `TagComentarioPublicado` y `TagAceptarPublicado`. `ComentarioRepository` ya está en `AppContainer` y se puede reemplazar.

Criterios de la HU09 (spec `comentar-album` de `add-comentar-album`) y escenario que cubre cada uno:

| #   | Criterio                                                                                                     | Escenario    |
|-----|--------------------------------------------------------------------------------------------------------------|--------------|
| 1   | Solo el Coleccionista ve "Comentar álbum"                                                                    | ESC01, ESC02 |
| 2   | La pantalla muestra el álbum y el coleccionista que comenta                                                  | ESC02        |
| 3   | Calificación de 1 a 5 con su texto ("Bueno (4/5)") y opinión con contador                                    | ESC02, ESC03 |
| 4   | "Publicar comentario" deshabilitado sin calificación ni opinión                                              | ESC02        |
| 5   | Al publicar, "Comentario publicado" y, al aceptar, el detalle muestra el comentario                          | ESC02, ESC03 |
| 6   | Mientras publica, "Publicando..." deshabilitado y un solo envío                                              | ESC03        |
| 7   | Si falla, "No se pudo publicar, intenta de nuevo" y se conserva lo escrito                                    | ESC03        |

## Goals / Non-Goals

**Goals:**
- Tres escenarios por historia, los más importantes y completos: el control por rol, el flujo completo desde el ingreso contra el backend real y el error con reintento.
- Cubrir con `MockWebServer` el envío en curso y el error al publicar (criterios 6 y 7).
- No dejar comentarios ni álbumes de prueba en el backend.

**Non-Goals:**
- La opinión en blanco, el límite de 400 caracteres y el diálogo "¿Descartar comentario?": ya los cubren `ComentarAlbumViewModelTest` y `ComentarAlbumUiStateTest`.
- Rotación y sesión perdida: ya cubiertas por las pruebas de la HU09.

## Decisions

**D1. Repositorio real que cuenta los envíos.** Un `ComentarioRepository` que delega en el real y cuenta las llamadas a `publicarComentario`, para comprobar que se envía un solo comentario y que el Visitante no envía nada.

**D2. Un álbum propio por escenario.** Los escenarios del Coleccionista comentan un álbum creado con `POST /albums`, que parte sin comentarios. `AlbumesDePrueba` lo borra en `@After`: primero sus comentarios (`DELETE /albums/{id}/comments/{id}`), porque BackVynils no borra un álbum con comentarios, y luego el álbum. El backend guarda el comentario con la calificación y la opinión que envió la app, y la prueba lo comprueba con `GET /albums/{id}`.

**D3. Datos reales para lo que solo se lee.** El Visitante usa el primer álbum del backend y el Coleccionista, el primer coleccionista.

## Risks / Trade-offs

- [La etiqueta del coleccionista se dibuja en mayúsculas] → Se busca sin distinguir mayúsculas.
- [Un álbum de prueba queda en el backend si la instrumentación se interrumpe a la fuerza] → Se reconoce por el nombre `E2E FN09 <marca de tiempo>`.
