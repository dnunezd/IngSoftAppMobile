# Design

## Context

HU02 ya consulta el detalle de álbum y presenta canciones con nombre y duración. HU07 estableció los patrones de formulario del proyecto: pantalla Compose sin estado, estado en ViewModel, validación separada, repositorio para las operaciones remotas, mensajes en `strings.xml`, protección contra envíos repetidos y conservación al girar.

La sesión ya indica si `collectorActionsEnabled`; la acción de agregar una canción debe seguir la misma regla de visibilidad de las demás acciones exclusivas del Coleccionista. El detalle se carga con `AlbumRepository`, que usa el `ServiceAdapter` compartido y distingue un recurso no disponible de otros errores de red o servicio.

La auditoría del controlador y servicio de referencia de BackVynils confirmó `POST /albums/{albumId}/tracks`, con cuerpo `name` y `duration`, respuesta HTTP 200 con el track creado y 404 cuando el álbum no existe. El DTO de respuesta incluye `id`, `name` y `duration`; la solicitud no debe enviar `id`. Durante la verificación local, `GET /albums` respondió correctamente y el `POST` a un ID de álbum inexistente respondió 404. No se probó una creación exitosa contra un álbum real para evitar dejar datos de prueba sin una estrategia de limpieza confirmada.

## Goals / Non-Goals

**Goals:**
- Asociar una canción al ID del álbum cuyo detalle está abierto.
- Impedir que el Visitante vea o ejecute la acción.
- Validar nombre y duración antes de enviar.
- Distinguir guardado exitoso, error reintentable y álbum inexistente.
- Evitar peticiones repetidas y mantener el estado durante la rotación.
- Aplicar TDD a toda lógica nueva.

**Non-Goals:**
- Cambiar BackVynils, agregar autenticación o modificar el modelo de álbum fuera de lo necesario para presentar la canción creada.
- Implementar E2E en este cambio. Las E2E quedan planteadas para un cambio posterior.
- Agregar una biblioteca o dependencia nueva sin necesidad comprobada.

## Decisions

### D1. Confirmar el contrato antes de implementar el cliente

La implementación debe consultar el código/documentación de BackVynils y confirmar el endpoint de asociación, el método HTTP, el formato del cuerpo y el resultado de éxito. La ruta probable `/albums/{id}/tracks` no se toma como contrato hasta verificarla. Se agregan pruebas de `MockWebServer` que fijen el contrato confirmado: ID del álbum, método, ruta, campos enviados y casos de éxito/404/500/error de transporte.

Si el backend devuelve una canción creada, el repositorio la transforma al modelo que ya usa el detalle. Si no devuelve un cuerpo, el resultado mínimo del repositorio debe indicar éxito para que la pantalla pueda regresar y volver a consultar el álbum. No se debe fingir una respuesta de éxito ante errores HTTP o de transporte.

### D2. Validación con reglas explícitas

- El nombre vacío o compuesto solo por espacios es obligatorio; se envía recortado, siguiendo el patrón de HU07.
- El formulario inicia vacío. Por decisión confirmada, ambos errores de obligatoriedad son visibles desde que se abre; al editar un campo, se actualiza su propio error sin ocultar el error del otro.
- La duración sigue el formato `m:ss`: uno o más dígitos para minutos y exactamente dos para segundos, entre `00` y `59`. Así se permiten duraciones de más de nueve minutos, como `12:34`.
- Una duración vacía se marca como "Campo obligatorio", no como error de formato. Para una duración no vacía pero inválida se muestra exactamente "Usa el formato m:ss, por ejemplo 5:05".
- "Guardar" solo está habilitado cuando nombre y duración son válidos y no hay un envío en curso.

La regla de duración se encapsula en una función pura para probarla sin Compose ni red. No se añade una restricción de duración mínima que no esté indicada en los criterios.

### D3. Operación de dominio en el repositorio de álbumes

La asociación pertenece al álbum seleccionado y reutiliza `AlbumRepository`/`AlbumRepositoryImpl`, al igual que consultar y crear álbumes. El método recibe el ID del álbum y los datos de la canción, y devuelve un resultado tipado de éxito o fallo. Un 404 se distingue como álbum no disponible; errores de conexión, timeout y errores HTTP distintos de 404 permiten reintentar.

La cancelación de coroutines se relanza y los errores inesperados no se convierten silenciosamente en un error de red, de acuerdo con `ServiceAdapter`.

### D4. Estado del formulario y envío

El formulario recibe el ID del álbum y utiliza un ViewModel para el nombre, la duración, sus errores y el estado del envío. Los estados distinguen edición, envío, éxito, error reintentable y álbum no disponible.

Al enviar, el ViewModel pasa al estado de guardado antes de iniciar la petición. El botón queda deshabilitado y muestra "Guardando..."; una segunda llamada no inicia otra petición. Al fallar de forma reintentable, conserva lo escrito y vuelve a habilitar el guardado. Un 404 muestra el estado de álbum no disponible y no permite enviar de nuevo.

### D5. Cancelación y navegación durante el envío

Cancelar cierra el formulario solo antes de enviar y no llama al repositorio; al abrirlo de nuevo empieza vacío. Como se confirmó que no se debe abandonar una petición ya iniciada, "Cancelar", la flecha y el botón atrás del sistema quedan bloqueados mientras el estado es "Guardando...". El 404 ofrece explícitamente el regreso a la pantalla anterior.

### D6. Éxito y actualización del detalle

Ante éxito se muestra "Canción agregada" y se regresa al mismo detalle. El detalle debe volver a consultar el álbum o recibir el resultado creado de forma controlada, de modo que la lista visible incluya la canción nueva; no basta con mostrar una confirmación si se conserva un detalle obsoleto. La navegación mantiene el ID del álbum original.

### D7. Rotación

El ViewModel conserva nombre y duración, errores y estado durante cambios de configuración. El envío pertenece al ViewModel y no debe repetirse porque se recree la pantalla. Las pruebas deben verificar tanto la edición como la rotación durante el envío, sin producir dos peticiones.

### D8. TDD y estrategia de pruebas

Cada grupo de implementación comienza con pruebas que expresen el comportamiento esperado. Se ejecutan primero para observar el fallo (ROJO), luego se implementa el cambio mínimo y se vuelven a ejecutar para comprobar el paso (VERDE); al final se corre la regresión pertinente. No se marca una prueba como ROJO si solo falla por una referencia temporal o un problema de compilación que no pruebe el comportamiento requerido.

La cobertura de la implementación incluye:
- Función pura de validación de nombre/duración, límites de segundos y formatos inválidos.
- Repositorio con MockWebServer: ruta y cuerpo confirmados, éxito, 404, 500, desconexión, respuesta ilegible si aplica y cancelación.
- ViewModel: errores iniciales, edición de campos, guardado válido, doble llamada, error y reintento, 404 y conservación ante rotación.
- Interfaz/navegación: visibilidad por rol, textos y estado de botones, cancelar sin petición, éxito con regreso/refresco, 404 y restauración del formulario.

Las pruebas E2E están fuera de este cambio y se plantean en `tasks.md`.

## Risks / Trade-offs

- **Contrato de BackVynils no confirmado** → No codificar la ruta supuesta; verificar primero el endpoint real y fijarlo con pruebas del repositorio.
- **El detalle conserva un ViewModel en la pila de navegación** → Al regresar tras guardar, invalidar/recargar el detalle explícitamente para que la nueva canción no quede oculta por estado anterior.
- **Cancelar durante una petición es ambiguo en los criterios** → Decisión confirmada: Cancelar y las acciones de salida se deshabilitan mientras se guarda.
- **Errores iniciales y botón deshabilitado** → Decisión confirmada: los dos campos se marcan obligatorios desde la apertura; Guardar permanece deshabilitado hasta que ambos sean válidos.
- **Formato de duración** → Se acepta uno o más dígitos de minutos y exactamente dos segundos entre `00` y `59`; no se agrega regla de duración mínima.
- **E2E no implementadas aquí** → Se documentan como trabajo separado, sin afirmar que están hechas ni que la HU está validada E2E.

## Open Questions

- Confirmar contra BackVynils el endpoint, el cuerpo exacto de creación y la respuesta exitosa antes de iniciar la implementación.
