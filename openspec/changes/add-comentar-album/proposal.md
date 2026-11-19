# Proposal

## Why

El coleccionista puede ver los comentarios de un álbum, pero no puede escribir uno. La HU09 (Sprint 3) agrega esa acción. BackVynils ya ofrece `POST /albums/{id}/comments`, y `Session.collectorActionsEnabled` ya prevé "comentar" entre las acciones del coleccionista.

## What Changes

- Agrega el botón "Comentar álbum" en el detalle del álbum, solo para el Coleccionista. El Visitante no lo ve ni puede llegar al formulario.
- Agrega la pantalla "Comentar álbum" con:
  - el nombre del álbum, su portada, sus artistas y su año, y el coleccionista que comenta;
  - una calificación de 5 estrellas en la que se elige de 1 a 5, con su texto, por ejemplo "Excelente (5/5)";
  - un campo de opinión con el texto de ayuda "Escribe tu opinión", con un máximo de 400 caracteres y su contador;
  - el botón "Publicar comentario", deshabilitado mientras no haya calificación y opinión.
- Envía `POST /albums/{id}/comments` con la opinión, la calificación y el id del coleccionista que ingresó. Mientras envía, el botón muestra "Publicando..." y no se puede editar ni salir.
- Si falla, muestra "No se pudo publicar, intenta de nuevo" y conserva lo escrito.
- Si se publica, muestra la pantalla de éxito "Comentario publicado" con el resumen del comentario. "Aceptar" vuelve al detalle, que se actualiza y muestra el comentario nuevo.
- Al salir con algo escrito, pregunta "¿Descartar comentario?" con "Continuar escribiendo" y "Descartar y salir".
- Conserva lo escrito al girar la pantalla.

Decisiones del responsable de la HU sobre los mockups y la tarea T09-4 (#50):
- Se entra desde el detalle del álbum y no desde el menú principal, así que no hace falta una pantalla para elegir el álbum. Hay que ajustar el texto de T09-4.
- Se adoptan de los mockups la pantalla de éxito, el diálogo de descarte, el límite de 400 caracteres y el texto de la calificación.
- No se adopta la lista "Comentarios del Álbum" debajo del formulario: ya se ve en el detalle.

**Fuera de alcance:**
- Pruebas E2E: T09-5 (#51), a cargo de Daniel Nuñez.
- Editar o borrar comentarios.
- La insignia "Verificado" del mockup, que el backend no ofrece.
- Cualquier cambio en BackVynils.

**Rama:** `feature/HU09-comentar-album`, basada en `develop` (`8ffd3d0`).

## Objetivos de calidad

- TDD: las pruebas unitarias de toda la lógica nueva se escriben antes que el código (repositorio, estado y ViewModel).
- Sin avisos de lint nuevos.
- Verificación manual en el emulador contra BackVynils local, nunca contra el backend de los tutores, borrando después los comentarios creados.

## Capabilities

### New Capabilities
- `comentar-album`: acceso desde el detalle, formulario, calificación, validación, envío, éxito, error, descarte y conservación al girar.

### Modified Capabilities
Ninguna en `openspec/specs/`. El detalle del álbum, que todavía no tiene spec archivada (`add-detalle-album`), gana el botón de acceso, que queda descrito en `comentar-album`.

## Impact

- **Datos:** `ComentarioApi` (nuevo `POST`), DTO del comentario nuevo, `ComentarioRepository` y su implementación, y `AppContainer`.
- **ViewModel:** `ComentarAlbumViewModel` y su estado nuevos; `AlbumViewModel` gana la actualización después de comentar.
- **UI:** `PantallaComentarAlbum` nueva y el botón en `PantallaDetalleAlbum`.
- **Navegación:** ruta `album/{id}/comentar` en `VinilosNavHost` y el resultado hacia el detalle.
- **Recursos:** textos nuevos en `strings.xml`.
- Cierra T09-2 (#48), T09-3 (#49) y T09-4 (#50). T09-6 (#52) se actualiza en la wiki al terminar.
