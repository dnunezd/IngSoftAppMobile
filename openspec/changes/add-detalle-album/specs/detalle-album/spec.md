# Spec Delta

## Purpose

Definir la consulta y presentación del detalle de un álbum seleccionado desde el catálogo, incluidos sus datos relacionados, la fecha de lanzamiento y las respuestas ante estados de carga y errores.

## ADDED Requirements

### Requirement: Consultar el detalle del álbum seleccionado
Cuando se selecciona un álbum en el catálogo, la app SHALL consultar `GET /albums/{id}` y mostrar el nombre, la descripción, el género, la disquera y la fecha de lanzamiento del álbum seleccionado.

#### Scenario: Detalle disponible
- **WHEN** el usuario selecciona un álbum existente en el catálogo
- **THEN** se muestra el detalle correspondiente a su identificador
- **AND** se muestran su nombre, descripción, género, disquera y fecha de lanzamiento

#### Scenario: El identificador corresponde al álbum seleccionado
- **WHEN** se selecciona un álbum con un identificador determinado
- **THEN** la consulta de detalle usa ese identificador
- **AND** no muestra información de otro álbum

### Requirement: Mostrar portada del álbum
La pantalla de detalle SHALL mostrar la portada en la parte superior. Si la portada no está disponible o no puede cargarse, la app SHALL mostrar una imagen genérica y MUST NOT cerrarse por ese fallo.

#### Scenario: Portada disponible
- **WHEN** se carga correctamente la imagen de portada del álbum
- **THEN** la portada aparece en la parte superior de la pantalla de detalle

#### Scenario: Portada no disponible
- **WHEN** la portada está ausente o no puede cargarse
- **THEN** aparece una imagen genérica en su lugar
- **AND** el resto del detalle sigue disponible
- **AND** la app no se cierra

### Requirement: Mostrar canciones del álbum
La pantalla de detalle SHALL mostrar las canciones del álbum con el nombre y la duración de cada una. Si el álbum no tiene canciones, SHALL mostrar "Este álbum aún no tiene canciones registradas".

#### Scenario: Álbum con canciones
- **WHEN** el detalle contiene una o más canciones
- **THEN** cada canción se muestra con su nombre y duración

#### Scenario: Álbum sin canciones
- **WHEN** el álbum no tiene canciones
- **THEN** se muestra "Este álbum aún no tiene canciones registradas"

### Requirement: Mostrar artistas del álbum
La pantalla de detalle SHALL mostrar los artistas relacionados con el álbum, incluyendo el nombre y la imagen de cada artista.

#### Scenario: Álbum con artistas
- **WHEN** el álbum tiene artistas relacionados
- **THEN** cada artista se muestra con su nombre y su imagen

### Requirement: Mostrar comentarios y calificaciones
La pantalla SHALL mostrar los comentarios del álbum con su calificación de 0 a 5, y MUST NOT mostrar quién escribió cada comentario. Si no hay comentarios, SHALL mostrar "Aún no hay comentarios para este álbum". La pantalla SHALL mostrar el promedio aritmético de las calificaciones y, si no hay calificaciones, SHALL mostrar "Sin calificaciones".

#### Scenario: Álbum con comentarios y calificaciones
- **WHEN** el álbum tiene comentarios con calificaciones
- **THEN** se muestran los comentarios y sus calificaciones
- **AND** no se muestra la identidad de sus autores
- **AND** se muestra el promedio aritmético de las calificaciones

#### Scenario: Álbum sin comentarios
- **WHEN** el álbum no tiene comentarios
- **THEN** se muestra "Aún no hay comentarios para este álbum"
- **AND** se muestra "Sin calificaciones"

#### Scenario: No se identifica al autor
- **WHEN** se muestra un comentario
- **THEN** no se muestra el nombre, identificador ni otro dato que identifique a quien lo escribió

### Requirement: Formatear la fecha de lanzamiento en UTC
La app SHALL presentar la fecha de lanzamiento en español con el formato de día, mes escrito y año (por ejemplo, "1 de agosto de 1984"). La interpretación y el formato SHALL realizarse en UTC, de modo que el día mostrado no cambie con la zona horaria del teléfono. La conversión SHALL estar aislada en una función pura que recibe texto y devuelve texto.

#### Scenario: Fecha ISO representada en UTC
- **WHEN** el backend devuelve la fecha ISO `1984-08-01`
- **THEN** se muestra "1 de agosto de 1984"

#### Scenario: Fecha independiente de la zona horaria del dispositivo
- **WHEN** se muestra la misma fecha ISO en dispositivos con distintas zonas horarias
- **THEN** todos muestran el mismo día, mes y año

### Requirement: Indicador de carga
Mientras la app consulta el detalle del álbum, SHALL mostrar un indicador de progreso y MUST NOT mostrar una pantalla en blanco.

#### Scenario: Carga en curso
- **WHEN** se solicita el detalle y todavía no llega la respuesta
- **THEN** se muestra un indicador de progreso

### Requirement: Álbum no disponible
Si el backend responde HTTP 404 para el álbum solicitado, la app SHALL mostrar "El álbum ya no está disponible" y una opción para regresar al catálogo. El 404 MUST NOT tratarse como un fallo de conexión.

#### Scenario: Álbum eliminado o inexistente
- **WHEN** `GET /albums/{id}` responde HTTP 404
- **THEN** se muestra "El álbum ya no está disponible"
- **AND** se ofrece una acción para regresar al catálogo

#### Scenario: Volver al catálogo desde álbum no disponible
- **WHEN** el usuario activa la opción de regresar en el estado de álbum no disponible
- **THEN** vuelve al catálogo

### Requirement: Error de conexión o servidor
Si falla la consulta del detalle por conexión o por un error del servidor, la app SHALL mostrar "No pudimos cargar el álbum" con un botón "Reintentar", y MUST NOT cerrarse. Estos errores SHALL distinguirse de la respuesta 404.

#### Scenario: Error de conexión o servidor
- **WHEN** la consulta falla porque no hay conexión, vence el tiempo de espera o el servidor responde con un error distinto de 404
- **THEN** se muestra "No pudimos cargar el álbum" y el botón "Reintentar"
- **AND** la app no se cierra

#### Scenario: Reintentar la consulta
- **WHEN** el usuario pulsa "Reintentar"
- **THEN** la app vuelve a mostrar el indicador de progreso
- **AND** vuelve a consultar el mismo álbum

#### Scenario: El reintento tiene éxito
- **WHEN** una consulta reintentada recibe correctamente el detalle del álbum
- **THEN** se muestra el detalle

### Requirement: Mostrar el promedio de calificaciones
La pantalla SHALL calcular el promedio aritmético de las calificaciones disponibles. Si no hay calificaciones para promediar, SHALL mostrar "Sin calificaciones".

#### Scenario: Calificaciones disponibles
- **WHEN** hay una o más calificaciones válidas
- **THEN** el promedio mostrado corresponde a la suma de las calificaciones dividida entre su cantidad

#### Scenario: Ninguna calificación disponible
- **WHEN** no hay calificaciones
- **THEN** se muestra "Sin calificaciones"
