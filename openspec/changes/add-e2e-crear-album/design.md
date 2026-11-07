# Design

## Context

Ver proposal.md (Why). El formulario (`PantallaCrearAlbum`) y el catálogo ya exponen etiquetas de prueba: `TagCrearAlbumCatalogo`, `TagNombreAlbum`, `TagPortadaAlbum`, `TagFechaAlbum`, `TagGeneroAlbum`, `TagDisqueraAlbum`, `TagDescripcionAlbum`, `tagErrorCampo(campo)`, `TagCrearAlbum`, `TagCancelarCrear`, `TagErrorEnvio` y `tagAlbumNuevo(id)`. La fecha se elige en un `DatePicker` dentro de un diálogo propio, y el género y la disquera en menús con los valores que acepta el backend.

## Goals / Non-Goals

**Goals:**
- Cubrir los escenarios de la T07-5 recorriendo la app como el usuario, desde el ingreso.
- No dejar álbumes de prueba en el backend.

**Non-Goals:**
- Pruebas de regresión (T07-6), que no se harán.
- Verificar "Creando..." de forma visual: contra el backend local la respuesta llega antes de poder capturarlo; el bloqueo de envíos repetidos lo cubren `CrearAlbumViewModelTest` y el criterio se comprueba en ESC07, donde el reintento crea un solo álbum.

## Decisions

**D1. Repositorio real que registra lo creado.** Un `AlbumRepository` que llama al real, cuenta las llamadas a `crearAlbum` y guarda los ids devueltos. Sirve para tres cosas: comprobar que cancelar, campos vacíos y URL inválida no envían nada (0 llamadas), saber qué álbum debe marcarse como NUEVO, y borrarlo en `@After`. *Alternativa:* comparar el catálogo antes y después; se usa además como comprobación contra el backend, pero no basta para borrar si la prueba falla a mitad.

**D2. Limpieza con `DELETE /albums/{id}` en `@After`.** Se hace con OkHttp directo contra la misma URL base, porque la app no tiene la operación de borrar. Corre aunque la prueba falle.

**D3. Elegir la fecha en el selector como el usuario.** Se toca el día 1 del mes actual (siempre seleccionable, porque no es futuro) y "Aceptar". La prueba calcula la fecha esperada en UTC y la compara con lo que muestra el formulario (`dd/MM/yyyy`) y con lo que guardó el backend (`yyyy-MM-ddT00:00:00.000Z`), lo que cubre el criterio de no correrse un día.

**D4. Coleccionista a partir de los datos reales.** Se entra con el primer coleccionista que devuelve el backend, sin fijar un id.

**D5. `MockWebServer` para el backend caído.** ESC07 responde coleccionistas y catálogo, y corta la conexión de `POST /albums` hasta que la prueba "levanta" el servidor. Así se comprueba que se conservan los datos y que el reintento crea el álbum, sin apagar Docker en medio de la prueba.

**D6. Backend local.** Las pruebas crean datos, así que la evidencia se genera contra BackVynils local. Si alguien las corre contra el desplegado, D2 borra lo creado, pero los ids del backend compartido avanzan.

## Risks / Trade-offs

- [La prueba falla después de crear y antes de registrar el id] → El id se registra apenas el repositorio recibe la respuesta, antes de que la UI navegue.
- [El aviso "Álbum creado" desaparece antes de verificarlo] → Se espera el aviso justo al volver al catálogo, dentro de su duración corta (unos 4 s).
- [Diferencias de teclado entre dispositivos] → Los textos se escriben con `performTextInput` sobre el campo, sin depender del teclado en pantalla.
