# Design

## Context

La app es Android nativa con Kotlin, Jetpack Compose, MVVM y Repository. En la rama actual, el acceso de datos usa `CollectorApi` de Retrofit directamente desde `CollectorRepository`; `AppContainer` crea Retrofit y el catálogo aún es una pantalla provisional. HU01 debe conectar el detalle al álbum seleccionado.

El backend BackVynils expone `GET /albums/:albumId`. Su servicio carga las relaciones `tracks`, `performers` y `comments`, y genera un 404 si no encuentra el álbum. Los comentarios del modelo tienen descripción y calificación; aunque se relacionan con un coleccionista en el backend, HU02 no debe mostrar la identidad de quien escribió. Las fechas de muestra están almacenadas como `YYYY-MM-DD`; el contrato que consume Android se declara como texto ISO.

El proyecto de esta rama declara `minSdk = 26`. El equipo está integrando por separado el objetivo `minSdk = 23`; HU02 debe poder probarse allí cuando ese cambio llegue a la rama. En API menor que 26, `java.time` necesita core library desugaring. Este cambio no altera archivos compartidos de Gradle ni agrega dependencias.

## Goals / Non-Goals

**Goals:**
- Consultar y presentar un álbum por su ID, incluyendo las relaciones que devuelve el backend.
- Distinguir “álbum no disponible” (404) de errores de conexión y servidor.
- Mantener el detalle, su fecha y sus cálculos independientes de Compose y fáciles de probar.
- Reutilizar un único Service Adapter para el acceso de todos los repositorios sin cambiar el comportamiento observable de HU00.
- Probar el flujo desde el catálogo HU01, incluyendo el regreso al catálogo.

**Non-Goals:**
- Modificar BackVynils, el catálogo HU01 o las dependencias/configuración compartidas de Gradle.
- Agregar una librería de carga de imágenes; solo se define el punto de integración y el fallback.
- Crear, editar o eliminar álbumes, canciones, comentarios o calificaciones.
- Implementar el botón o la función "Escuchar ahora", ni convertir controles visibles en la referencia de Stitch (favorito, compartir, calificar o agregar comentario) en nuevas funciones. La imagen guía la organización visual; los criterios de aceptación delimitan las interacciones.
- Cambiar el mínimo de plataforma desde este cambio; se documenta API 23 como objetivo sujeto a la integración transversal en curso.

## Decisions

### D1. El repositorio solicita el álbum usando su ID

`AlbumApi` declara `GET albums/{id}` y DTOs que reflejan los campos usados por HU02: datos principales, canciones (`name`, `duration`), artistas (`name`, `image`) y comentarios (texto y calificación). `AlbumRepository` obtiene la API a través del Service Adapter, valida/mappea la respuesta a modelos de dominio y expone una operación en español, por ejemplo `obtenerAlbum(id)`.

La pantalla y el ViewModel no conocen Retrofit ni los DTOs. Los datos no solicitados por HU02, especialmente la identidad del autor del comentario, no se exponen en la interfaz.

### D2. El Service Adapter centraliza transporte y conserva el status HTTP

Se crea una única instancia de `ServiceAdapter` en el contenedor de la app. Esta instancia construye/configura Retrofit/OkHttp, entrega las interfaces Retrofit y envuelve las llamadas para traducir fallos de transporte y de lectura de respuesta. Todos los repositorios, incluido el `CollectorRepository` actual, la reutilizan.

El resultado de una llamada HTTP no exitosa debe conservar su status code (o un error tipado equivalente). El adaptador no interpreta el significado de 404 por entidad: `AlbumRepository` convierte específicamente el 404 de esta consulta en un error de dominio `AlbumNoDisponible`. Otros errores HTTP, errores de red, timeout y respuesta ilegible se representan como fallo de carga. `CancellationException` se relanza y las excepciones inesperadas no se disfrazan de errores de red.

La migración del `CollectorRepository` debe conservar su comportamiento visible actual. Si la integración independiente de Service Adapter ya aporta esta base, HU02 debe integrarse con ella en vez de crear un segundo adaptador.

### D3. Estados de detalle en el ViewModel

El ViewModel recibe el ID de la ruta y expone un estado observable sellado: `Cargando`, `Exito(album)`, `NoDisponible` y `Error`. Carga el álbum al crearse y permite `reintentar()` desde `Error`. `NoDisponible` no ofrece reintento, porque el criterio pide regresar al catálogo.

Un álbum es un único recurso, por lo que no se agrega un estado de lista vacía. Las canciones y comentarios ausentes son listas vacías dentro del resultado exitoso y activan sus mensajes de criterio. La rotación conserva el estado mientras el ViewModel pertenezca a la entrada de navegación del mismo ID.

### D4. Fecha en una función pura, UTC y preparada para API 23

La conversión queda encapsulada en una función pura de texto a texto (por ejemplo, `formatearFechaLanzamiento(fechaIso: String): String`) en la capa de dominio/presentación, no en el composable. Debe producir un texto español determinista como `1 de agosto de 1984`, interpretando el dato en UTC para que la zona del teléfono no altere el día.

Para fechas ISO de solo día (`YYYY-MM-DD`), se interpreta la fecha calendario como ese día en UTC, no como medianoche local. Para un timestamp ISO con zona, se interpreta su instante y se toma su fecha UTC. La implementación puede usar `java.time` aislada detrás de esa función. La ejecución en API 23 requiere core library desugaring, que depende de la integración compartida pendiente; este cambio no edita Gradle ni incorpora esa dependencia por su cuenta.

El idioma/locale de salida debe ser español estable y no depender del idioma configurado en el teléfono. El comportamiento para una fecha ISO inválida queda señalado como punto para confirmar antes de cerrar la especificación.

### D5. Portada con fallback y sin fijar librería

La pantalla coloca un componente de portada arriba y siempre puede dibujar el recurso local genérico cuando la URL falte o el cargador reporte error. El contrato del componente separa URL, estado de carga y fallback para que después se conecte una librería de imágenes sin cambiar la pantalla ni el ViewModel.

No se agrega dependencia para descargar imágenes en este cambio. Hasta integrar la librería elegida, el detalle debe seguir siendo funcional y mostrar el fallback; el equipo debe decidir cómo verificará una carga remota real sin añadir la dependencia ahora.

### D6. Navegación enlazada con HU01

La ruta de detalle recibe el ID del álbum que seleccionó el usuario. La acción de volver desde el detalle o desde `NoDisponible` regresa a la ruta/lista del catálogo, sin crear una pantalla nueva de catálogo dentro de HU02. La integración debe coordinarse con HU01, cuyo catálogo sustituirá el destino provisional actual.

### D7. Pruebas TDD

- `ServiceAdapter`: éxito, 404 con status preservado, 500, desconexión, timeout, JSON ilegible y cancelación.
- `AlbumRepository`: ruta/ID correctos, mapeo de relaciones, 404 a `AlbumNoDisponible`, y fallos de carga para errores de red/servidor.
- Fecha: función pura con fecha de ejemplo, timestamp UTC y misma salida independientemente de la zona del proceso/dispositivo.
- ViewModel/presentación: transiciones de estado, reintento, canciones/comentarios vacíos, promedio, no exposición de autor y fallback de portada.
- Navegación/UI: ID recibido, botón de regreso, mensaje 404 y pantallas de progreso, éxito y error.
- E2E: backend real con un álbum con canciones/comentarios y otro sin ellos.

Cada prueba de una tarea se escribe y se comprueba fallida antes de implementar el comportamiento correspondiente.

### D8. Stitch como referencia visual, no como ampliación de alcance

La imagen adjunta de Stitch orienta la jerarquía y el orden visual: portada en la parte superior, nombre y metadatos, descripción, artistas destacados, canciones, comentarios y calificación promedio. La composición puede adaptar esos bloques al tamaño de pantalla y a los datos reales que devuelve BackVynils.

El botón "Escuchar ahora" que aparece en la imagen se excluye expresamente. Los iconos de favorito y compartir, la acción "Agregar comentario" y cualquier otra interacción dibujada que no esté descrita en los criterios también son solo elementos de referencia: no se implementan como funciones. Se mantienen los criterios aprobados como límite funcional, incluyendo mostrar datos de artistas, canciones y comentarios, así como los mensajes de estados vacíos.

## Risks / Trade-offs

- **El adaptador compartido podría homogeneizar el 404 con otros errores** → Preservar el status HTTP en un tipo de error que el repositorio pueda interpretar y probarlo con MockWebServer.
- **El Service Adapter puede integrarse en paralelo** → Acordar su firma/errores con el equipo y reutilizarlo; no crear otra instancia o abstracción competidora.
- **La fecha ISO puede ser fecha calendario o timestamp** → Cubrir ambas formas si las entrega el backend y confirmar el contrato real; la salida siempre se calcula en UTC.
- **`java.time` no está disponible directamente en API 23** → Depender del cambio compartido de minSdk y desugaring antes de afirmar soporte; verificar HU02 en emulador API 23 tras ese merge.
- **Sin librería no se descarga la portada** → Diseñar el fallback y el punto de integración, y dejar la carga real conectable cuando el equipo elija la librería.
- **HU01 puede no estar integrado cuando HU02 empiece** → Mantener la navegación parametrizada y coordinar el callback/ruta con la implementación del catálogo; las pruebas pueden usar un destino de catálogo de prueba.
- **El promedio no define redondeo ni precisión** → Calcular la media aritmética y solicitar decisión del equipo sobre formato/precisión antes de fijar pruebas de presentación para valores periódicos.
- **La imagen de Stitch contiene controles que exceden los criterios** → Usarla como referencia de composición y documentar explícitamente que no se agregan esas interacciones; en particular, excluir "Escuchar ahora".

## Open Questions

- ¿El promedio se presenta como entero, con un decimal o con más precisión? ¿Qué regla de redondeo se usa?
- ¿Qué se muestra si una fecha ISO falta o está malformada?
- ¿Qué fallback se espera para una imagen de artista que no carga? El criterio solo define fallback para la portada.
- Sin agregar una librería de imágenes, ¿se acepta que la portada permanezca en fallback hasta la integración posterior, o el equipo tiene un mecanismo existente para cargarla?
- ¿Puede confirmarse API 23 como mínimo de esta integración transversal? La descripción de la rúbrica menciona Android L/API 21–22, pero el equipo indicó API 23 como mínimo y la integración de ese cambio está en curso.
