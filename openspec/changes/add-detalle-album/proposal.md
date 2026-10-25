# Proposal

## Why

Después de encontrar un álbum en el catálogo, el usuario necesita consultar información suficiente para conocer su contenido y sus artistas sin depender de datos parciales de la lista. La HU02 amplía el flujo de álbumes con una pantalla de detalle que consume el recurso individual del backend y comunica claramente la carga, la indisponibilidad y los fallos recuperables.

## What Changes

- Agrega una pantalla de detalle a la que se llega al seleccionar un álbum y que muestra su portada, nombre, descripción, género, disquera y fecha de lanzamiento.
- Usa la pantalla adjunta de Stitch como referencia visual para organizar el detalle: portada arriba, información del álbum y secciones de artistas, canciones y comentarios. Se excluye explícitamente el botón "Escuchar ahora"; la imagen no amplía los criterios funcionales.
- Muestra las canciones con nombre y duración; si no existen, muestra "Este álbum aún no tiene canciones registradas".
- Muestra los artistas con nombre e imagen.
- Muestra las calificaciones y comentarios sin identificar a sus autores, y calcula el promedio de calificaciones. Sin comentarios se muestra "Aún no hay comentarios para este álbum"; sin calificaciones se muestra "Sin calificaciones".
- Maneja estados de carga, álbum no disponible (404) y error de conexión o servidor con reintento.
- Formatea la fecha en español usando una función pura de texto a texto y la zona UTC, de modo que el día no cambie según la zona del dispositivo.
- Establece el Service Adapter compartido como punto único de acceso de red para los repositorios, preservando el código HTTP necesario para diferenciar un 404.
- Deja el componente de portada preparado para conectar una librería de imágenes más adelante; no agrega una dependencia de imágenes.
- Documenta como objetivo de compatibilidad `minSdk` 23 y la necesidad de desugaring para usar `java.time` bajo API 26. La configuración compartida de Gradle y su integración de SDK/desugaring se coordinan con el cambio transversal que ya está en curso.

No se agrega la acción "Escuchar ahora" ni acciones de edición, favoritos, compartir, calificar o agregar comentarios. La referencia de Stitch orienta la presentación visual, pero no agrega comportamiento que no esté en los criterios de HU02.

## Capabilities

### New Capabilities
- `detalle-album`: consulta y presentación de la información detallada de un álbum, sus relaciones y sus estados de carga y error.
- `acceso-backend`: acceso común al backend mediante Service Adapter, conservando el código HTTP para que el repositorio pueda diferenciar un 404 de un fallo de red. Si el cambio transversal del adaptador se integra por separado, coordinar esta capacidad para evitar implementaciones duplicadas.

## Impact

- **Datos** (`data.remote`, `data.model`, `data.repository`): nueva API de álbumes, DTOs y modelos para álbum, canciones, artistas y comentarios; repositorio para `GET /albums/{id}`.
- **Acceso de red** (`data.remote` y `AppContainer`): creación y reutilización del Service Adapter compartido por todos los repositorios. El `CollectorRepository` debe migrarse sin alterar el comportamiento visible de HU00.
- **Estado** (`viewmodel`): estados de carga, éxito, no disponible y error, con reintento en los errores recuperables.
- **Interfaz** (`ui`): detalle, datos relacionados, promedio, mensajes de listas vacías y portada genérica cuando la imagen no esté disponible.
- **Navegación** (`navigation`): ruta con identificador del álbum desde el catálogo HU01 y regreso al catálogo desde el detalle y desde el estado 404.
- **Pruebas**: unitarias con Retrofit/MockWebServer y ViewModel; pruebas de UI/navegación; E2E contra BackVynils.
- **Plataforma**: el estado actual de esta rama es `minSdk = 26`. El objetivo acordado para la integración transversal es API 23; las verificaciones de HU02 en API 23 dependen de que se integre primero el cambio compartido de `minSdk` y desugaring. Este cambio no modifica Gradle ni agrega dependencias.
- **Fuera de alcance**: cambios al backend, modificaciones al catálogo HU01, nuevas dependencias, selección de una librería de imágenes, edición del álbum, escritura de comentarios/calificaciones y soporte inferior a API 23.
