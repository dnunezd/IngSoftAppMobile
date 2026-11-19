# Tasks

Cada paso de código empieza con su prueba unitaria en rojo (TDD).

## 1. Datos (T09-2, #48)

- [x] 1.1 Prueba `ComentarioRepositoryTest` con `MockWebServer`: el cuerpo y la ruta del `POST`, el éxito, el 404, el 400, el 500 y la falta de conexión. Debe fallar.
- [x] 1.2 Agregar `ComentarioNuevo`, los DTO del envío, `ComentarioApi`, `ComentarioRepository` y `ComentarioRepositoryImpl` (D1) hasta que la prueba pase.
- [x] 1.3 Registrar `comentarioRepository` en `AppContainer`, reemplazable en pruebas, con su prueba en `AppContainerTest`.

## 2. Estado y ViewModel (T09-3, #49)

- [ ] 2.1 Pruebas del estado (D2): `puedePublicar`, `tieneBorrador`, el texto de la calificación y el límite de 400 caracteres.
- [ ] 2.2 Pruebas de `ComentarAlbumViewModel`:
  - carga del álbum guardado, por red, con error y al reintentar (D3);
  - edición, el envío, la guarda contra dos toques, éxito y error, y editar después del error (D4);
  - salida con y sin borrador, el diálogo y la salida después de publicar (D5).
- [ ] 2.3 Implementar `ComentarAlbumUiState` y `ComentarAlbumViewModel` hasta que las pruebas pasen.
- [ ] 2.4 Prueba de `AlbumViewModel.actualizar()` (D7): mantiene el álbum a la vista y lo reemplaza con la respuesta; si falla, conserva el álbum con el aviso. Luego implementarlo.

## 3. Pantalla (T09-3, #49)

- [ ] 3.1 Agregar los textos a `strings.xml` (D9).
- [ ] 3.2 Crear `PantallaComentarAlbum`:
  - encabezado con flecha;
  - álbum y coleccionista;
  - estrellas con su texto;
  - opinión con contador;
  - botón de publicar con su estado de envío;
  - aviso de error;
  - diálogo de descarte;
  - contenido de éxito con "Aceptar";
  - estados de carga y de error del álbum.
- [ ] 3.3 Agregar el botón "Comentar álbum" a `PantallaDetalleAlbum`, con `onComentar` opcional (D8).

## 4. Navegación (T09-4, #50)

- [ ] 4.1 Agregar `Rutas.COMENTAR_ALBUM` (`album/{id}/comentar`) y su `viewModelFactory` con el coleccionista de la sesión.
- [ ] 4.2 En el detalle, pasar `onComentar` solo para el Coleccionista. Observar `comentario_publicado` y llamar a `actualizar()` (D7).
- [ ] 4.3 En la ruta de comentar:
  - `BackHandler` hacia `pedirSalir()`;
  - cerrar al ver `salir`, dejando el resultado si se publicó;
  - cerrar si no hay sesión de Coleccionista (D8).

## 5. Verificación

- [ ] 5.1 `./gradlew lint testDebugUnitTest --continue`: 0 errores de lint y todas las unitarias pasan.
- [ ] 5.2 `./gradlew compileDebugAndroidTestKotlin` y la suite instrumentada, para confirmar que lo existente sigue pasando.
- [ ] 5.3 Verificar a mano en el emulador, contra BackVynils local:
  - los 7 criterios;
  - el diálogo de descarte;
  - el giro;
  - el detalle actualizado.

  Borrar después los comentarios creados.
- [ ] 5.4 `openspec validate add-comentar-album --strict`.

## 6. Pendiente, fuera de este cambio

- [ ] 6.1 T09-5 (#51): pruebas E2E, a cargo de Daniel Nuñez.
- [ ] 6.2 T09-6 (#52): actualizar la wiki de la HU09 con las decisiones y ajustar T09-4 (#50) al acceso desde el detalle.
