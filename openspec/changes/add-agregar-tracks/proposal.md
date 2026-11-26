# Proposal

## Why

El detalle de álbum ya presenta sus canciones, pero el Coleccionista no puede asociar una nueva. HU08 permitirá completar y actualizar el catálogo desde el propio detalle, sin ofrecer esta acción al Visitante.

## What Changes

- Agrega, solo para el Coleccionista, la acción para abrir desde el detalle de un álbum el formulario "Agregar canción".
- Pide el nombre y la duración. Al abrir el formulario vacío, ambos campos muestran "Campo obligatorio" y "Guardar" permanece deshabilitado. Los errores se actualizan al editar cada campo.
- Valida la duración con el formato `m:ss` (por ejemplo, `5:05`). Si no cumple, muestra "Usa el formato m:ss, por ejemplo 5:05" debajo del campo y mantiene deshabilitado "Guardar".
- Al guardar, asocia la canción con el álbum seleccionado. Mientras espera la respuesta, deshabilita el guardado y muestra "Guardando...".
- Al confirmar el guardado, muestra "Canción agregada", vuelve al detalle y actualiza las canciones para incluir la nueva.
- Permite cancelar antes de iniciar el envío, sin enviar una petición. Mientras guarda, "Cancelar" y las acciones para salir del formulario quedan deshabilitadas para no abandonar una petición cuyo resultado todavía no se conoce.
- Si falla la conexión o el servicio, muestra "No se pudo agregar la canción, intenta de nuevo", conserva los valores y permite reintentar. Si el álbum responde 404, muestra "El álbum ya no está disponible" y permite regresar a la pantalla anterior.
- Conserva los valores, errores y estado de envío al girar la pantalla.

La ruta y el cuerpo exactos del endpoint para asociar tracks se deben confirmar contra el contrato de BackVynils antes de implementar el cliente. Este cambio no modifica el backend.

**Fuera de alcance:** implementar o ejecutar pruebas E2E. Se dejan sus escenarios planteados en `tasks.md` para un cambio separado, a cargo de quien tenga esa responsabilidad.

## Capabilities

### New Capabilities
- `agregar-tracks`: acceso desde el detalle, formulario, validación, asociación, cancelación y estados de respuesta. Incluye la actualización de la lista de canciones del detalle al regresar de un guardado exitoso.

### Modified Capabilities
Ninguna. La especificación base de detalle de álbum aún no está archivada en `openspec/specs`; el acceso y refresco del detalle quedan definidos en la nueva capacidad `agregar-tracks`. La sesión no cambia: ya expone si las acciones del Coleccionista están habilitadas.

## Impact

- **Datos** (`data.remote`, `data.model`, `data.repository`): método Retrofit para asociar el track al álbum, cuerpo de petición, contrato del repositorio y traducción de respuestas HTTP.
- **Estado** (`viewmodel`): validación de duración, estado del formulario y envío protegido frente a toques repetidos.
- **Interfaz** (`ui`): acción exclusiva del Coleccionista, formulario y mensajes especificados por HU08.
- **Navegación** (`navigation`): abrir y cerrar el formulario, regresar al detalle y actualizar el detalle tras una creación exitosa.
- **Pruebas de esta implementación:** unitarias, repositorio con MockWebServer, ViewModel y las pruebas de interfaz apropiadas. Cada comportamiento se desarrolla con TDD.
- **E2E:** FN08-ESC01 a FN08-ESC09 quedan propuestos en `tasks.md`; se implementarán y evidenciarán por separado.
- **Dependencias y backend:** no se anticipan dependencias nuevas ni cambios en BackVynils; confirmar el contrato del endpoint antes de cerrar la implementación.
