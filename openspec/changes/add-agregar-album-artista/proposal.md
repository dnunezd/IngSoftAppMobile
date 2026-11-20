# Proposal

## Why

HU15 pide que un coleccionista pueda agregar un álbum del catálogo a un artista (músico o banda) para actualizar su información. Hoy el detalle del artista (HU04) solo muestra sus álbumes: la app no consume ningún endpoint de asociación y no hay forma de elegir un álbum para agregarlo. Los criterios de aceptación de la [HU15 en la wiki](https://github.com/dnunezd/IngSoftAppMobile/wiki/HU15) mandan sobre los mockups cuando no coinciden.

## What Changes

- Agregar a `ArtistApi` el consumo de `POST /musicians/{id}/albums/{albumId}` (músicos) y `PUT /bands/{id}/albums` (bandas), y un método `asociarAlbum(tipo, idArtista, idAlbum)` en `ArtistRepository` que oculte la diferencia. **No** se usa `POST /bands/{id}/albums/{albumId}` porque deja a la banda como única intérprete del álbum, ni `DELETE .../albums/{albumId}`, que borra todos los álbumes del artista (comportamientos verificados el 2026-11-19 contra BackVynils local, que no se modifica).
- Agregar en el detalle del artista, solo para el Coleccionista, el botón "Agregar álbum". Abre una ventana inferior (`ModalBottomSheet`) titulada con el nombre del artista, con barra de búsqueda y el listado de álbumes (portada, nombre y año); los ya asociados aparecen deshabilitados con la etiqueta "Ya asociado".
- Selección de un solo álbum; botón "Asociar" deshabilitado sin selección; opción "Cancelar" que cierra sin cambios.
- Al asociar: se cierra la ventana, se muestra "Álbum asociado" y la discografía del artista se actualiza. Si falla: "No se pudo asociar el álbum, intenta de nuevo", la ventana sigue abierta y se conserva la selección.
- Mensajes de estado: "No hay álbumes para asociar" si no queda ningún álbum disponible, uno distinto si la búsqueda no encuentra nada, y carga y error (con "Reintentar") del listado dentro de la ventana, usando el catálogo guardado en el dispositivo cuando lo hay.
- Nuevo `AsociarAlbumViewModel` que conserva ventana abierta, búsqueda y selección al rotar.
- Pruebas unitarias (repositorio con `MockWebServer`, ViewModels) e instrumentadas (pantalla, navegación, rotación) con los repositorios falsos.

Fuera de alcance:
- Las pruebas E2E contra el backend real (T15-5): las implementa otro integrante.
- Internacionalización: los textos nuevos van solo en `values/strings.xml`.
- Elementos del mockup que no piden los criterios: "Lanzamientos sugeridos", "Orden cronológico", disquera, pantalla de confirmación completa con el vinilo y chip "Recién asociado".
- Quitar un álbum de un artista (no es parte de la HU y el `DELETE` del backend es destructivo).

## Capabilities

### New Capabilities
- `asociar-album-artista`: agregar un álbum existente del catálogo a un músico o una banda desde su detalle (ventana de selección, búsqueda, estados y mensajes).

### Modified Capabilities
(ninguna; el detalle del artista de HU04 todavía vive en un cambio sin archivar, `add-detalle-artista`. Este cambio solo agrega el botón y la actualización de la discografía, sin alterar sus requisitos.)

## Impact

- Nuevo: `AsociarAlbumViewModel` y `AsociarAlbumUiState`; `ui/artistas/VentanaAsociarAlbum.kt`; pruebas unitarias e instrumentadas; textos en `strings.xml`.
- Modificado: `ArtistApi`, `ArtistRepository` y `ArtistRepositoryImpl` (asociación), `ArtistaDetalleViewModel` (agregar el álbum asociado y volver a consultar), `PantallaDetalleArtista` (botón y aviso), `VinilosNavHost` (crear el ViewModel solo para el Coleccionista, sin cambiar su firma) y los falsos que implementan `ArtistRepository`/`ArtistApi` (`RepositorioArtistasFalso`, `ArtistaDetalleViewModelTest`, `ArtistsViewModelTest`, `RepositoriosDispatcherTest`).
- Sin dependencias nuevas ni cambios en `AppContainer`, `MainActivity`, `ServiceAdapter`, Room o Gradle. Íconos de `material-icons-core` y drawables existentes.
- Base: `origin/develop` en `e756226` (con HU09). Convive con el PR #113 (accesibilidad), que cambia la paleta: la ventana usa `MaterialTheme.colorScheme.primary` para quedar alineada con él (design D7).
- Issues: #53 (T15-2), #54 (T15-3), #55 (T15-4), #57 (T15-6). #56 (T15-5, E2E) queda a cargo de otro integrante.
