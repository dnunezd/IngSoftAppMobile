# Design

## Context

Ver proposal.md (Why). El detalle del artista expone `TagListaDetalleArtista`, `TagBotonAgregarAlbum` (solo para el Coleccionista) y `tagAlbumArtista(id)`, y la ventana expone `TagVentanaAsociar`, `TagTituloAsociar`, `TagBusquedaAsociar`, `TagListaAsociar`, `tagAlbumAsociable(id)`, `tagYaAsociado(id)`, `TagBotonAsociar`, `TagAsociandoAsociar`, `TagCancelarAsociar`, `TagErrorAsociar` y `TagSinResultadosAsociar`. `ArtistRepository` ya está en `AppContainer` y se puede reemplazar.

Criterios de la HU15 (spec `asociar-album-artista` de `add-agregar-album-artista`) y escenario que cubre cada uno:

| #   | Criterio                                                                                                  | Escenario           |
|-----|-----------------------------------------------------------------------------------------------------------|---------------------|
| 1   | Solo el Coleccionista ve "Agregar álbum"                                                                  | ESC01, ESC02 |
| 2   | La ventana muestra "Asociar álbum a <artista>", la búsqueda y el catálogo                                 | ESC02        |
| 3   | La búsqueda filtra por nombre                                                                             | ESC02        |
| 4   | Los álbumes ya asociados aparecen deshabilitados con "Ya asociado"                                        | ESC02        |
| 5   | "Asociar" deshabilitado sin selección y mientras se envía                                                 | ESC02, ESC03 |
| 6   | Al asociar, la ventana se cierra, "Álbum asociado" y el álbum aparece en el detalle                       | ESC02, ESC03 |
| 7   | La banda conserva sus otros álbumes                                                                       | ESC02        |
| 8   | Si falla, "No se pudo asociar el álbum, intenta de nuevo" con la ventana abierta y la selección conservada | ESC03        |

## Goals / Non-Goals

**Goals:**
- Tres escenarios por historia, los más importantes y completos: el control por rol, el flujo completo desde el ingreso contra el backend real y el error con reintento. El flujo completo usa una banda porque, además, comprueba que conserva sus otros álbumes (la app reemplaza su lista con `PUT`).
- Cubrir con `MockWebServer` el envío en curso y el error al asociar (criterios 5 y 8).
- No dejar asociaciones de prueba ni tocar los álbumes reales de los artistas.

**Non-Goals:**
- La búsqueda sin resultados, la selección que se quita al filtrar y "Cancelar": ya los cubren `VentanaAsociarAlbumTest` y `AsociarAlbumViewModelTest`.
- Rotación, catálogo vacío y error al cargar el catálogo: ya cubiertos por las pruebas de Compose UI de la HU15.

## Decisions

**D1. Repositorio real que cuenta las asociaciones.** Un `ArtistRepository` que delega en el real y cuenta las llamadas a `asociarAlbum`, para comprobar que se envía una sola asociación y que el Visitante no envía nada.

**D2. Siempre se asocia un álbum propio.** Cada escenario que asocia crea su álbum con `POST /albums` y lo elige en la ventana buscándolo por su nombre. Al borrarlo con `DELETE /albums/{id}`, la base de datos quita también su relación con el artista, que conserva sus álbumes reales. Así no hace falta restaurar la lista del artista con `PUT`, como sugería la guía de `docs/pruebas/sprint-3/HU15/resultados.md`, y tampoco se usa `DELETE /musicians|bands/{id}/albums/{id}`, que en BackVynils le quita al artista todos sus álbumes. Se comprobó con curl y en cada corrida: los artistas de ejemplo quedan con los mismos álbumes.

**D3. Artistas a partir de los datos reales.** La primera banda y el primer músico del backend; si no hay, el escenario se omite con `Assume`. Si el artista ya tiene álbumes, la prueba comprueba que el primero aparece como "Ya asociado".

**D4. La ventana se captura con la pantalla completa** (`add-e2e-agregar-tracks`, D4): es una hoja inferior, con su propia raíz Compose.

## Risks / Trade-offs

- [El álbum de prueba queda al final del catálogo de la ventana] → Se busca por su nombre antes de elegirlo.
- [Un álbum de prueba queda en el backend si la instrumentación se interrumpe a la fuerza] → Se reconoce por el nombre `E2E FN15 <marca de tiempo>`; borrarlo con `DELETE /albums/{id}` quita también la asociación.
