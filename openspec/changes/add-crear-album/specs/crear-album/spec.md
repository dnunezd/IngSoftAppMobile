# Spec Delta

## ADDED Requirements

### Requirement: Campos del formulario
El formulario SHALL pedir nombre, portada (URL), fecha de lanzamiento, género, disquera y descripción, todos obligatorios. Género y disquera MUST elegirse de listas con los valores que acepta el backend. La descripción MUST admitir como máximo 500 caracteres y mostrar cuántos lleva.

#### Scenario: Opciones de género
- **WHEN** el usuario abre la lista de género
- **THEN** ve exactamente "Classical", "Salsa", "Rock" y "Folk"

#### Scenario: Opciones de disquera
- **WHEN** el usuario abre la lista de disquera
- **THEN** ve exactamente "Sony Music", "EMI", "Discos Fuentes", "Elektra" y "Fania Records"

#### Scenario: Límite de la descripción
- **WHEN** el usuario escribe más de 500 caracteres en la descripción
- **THEN** la descripción conserva solo los primeros 500
- **AND** el contador muestra "500 / 500"

### Requirement: Campos obligatorios
Al pulsar "Crear álbum", la app SHALL marcar cada campo faltante con "Campo obligatorio" y MUST NOT enviar nada al servidor. Un campo con solo espacios MUST considerarse faltante. El botón "Crear álbum" MUST seguir activo aunque falten campos, para que el usuario pueda ver qué falta.

#### Scenario: Formulario vacío
- **WHEN** el usuario pulsa "Crear álbum" sin llenar ningún campo
- **THEN** los seis campos muestran "Campo obligatorio"
- **AND** se muestra "Atención requerida" al inicio del formulario
- **AND** no se envía nada al servidor

#### Scenario: Nombre con solo espacios
- **WHEN** el nombre contiene solo espacios y el usuario pulsa "Crear álbum"
- **THEN** el nombre muestra "Campo obligatorio"
- **AND** no se envía nada al servidor

#### Scenario: Corregir un campo
- **WHEN** un campo muestra un error y el usuario lo edita
- **THEN** ese campo deja de mostrar el error
- **AND** los demás campos siguen marcados

### Requirement: URL de la portada
Si la portada no empieza con "http://" o "https://", la app SHALL mostrar "Ingresa una URL válida" al pulsar "Crear álbum" y MUST NOT enviar nada al servidor. El prefijo MUST compararse sin distinguir mayúsculas.

#### Scenario: Protocolo no permitido
- **WHEN** la portada es "ftp://servidor/portada.png" y el usuario pulsa "Crear álbum"
- **THEN** la portada muestra "Ingresa una URL válida"
- **AND** no se envía nada al servidor

#### Scenario: Prefijos válidos
- **WHEN** la portada empieza con "http://", "https://" o "HTTPS://"
- **THEN** la portada no muestra error

### Requirement: Selector de fecha
La fecha de lanzamiento SHALL elegirse con un selector, sin poder escribirla a mano, y MUST NOT permitir días posteriores a la fecha actual del teléfono. El día elegido MUST enviarse y mostrarse sin cambios por la zona horaria.

#### Scenario: Elegir una fecha
- **WHEN** el usuario toca el campo de fecha, elige el 26 de septiembre de 1969 y pulsa "Aceptar"
- **THEN** el campo muestra "26/09/1969"

#### Scenario: Selector en español
- **WHEN** el dispositivo está en otro idioma y el usuario abre el selector
- **THEN** el mes, los días y los textos del selector se muestran en español

#### Scenario: Sin escritura manual
- **WHEN** el selector está abierto
- **THEN** no se ofrece ninguna opción para escribir la fecha

#### Scenario: Cerrar el selector sin elegir
- **WHEN** el usuario abre el selector y pulsa "Cancelar"
- **THEN** el campo conserva el valor que tenía

#### Scenario: Fecha actual permitida y futura no
- **WHEN** el usuario abre el selector
- **THEN** puede elegir el día de hoy
- **AND** no puede elegir el día de mañana ni los siguientes

#### Scenario: Hoy se calcula con el día local
- **WHEN** en Bogotá son las 8:00 p. m. y en UTC ya es el día siguiente
- **THEN** el último día que se puede elegir es el día local de Bogotá

#### Scenario: Sin desfase de un día
- **WHEN** el usuario elige el 26 de septiembre de 1969 con el teléfono en cualquier zona horaria
- **THEN** se envía `1969-09-26T00:00:00.000Z`

### Requirement: Crear el álbum
Con todos los campos válidos, al pulsar "Crear álbum" la app SHALL enviar `POST /albums` solo con nombre, portada, fecha, descripción, género y disquera, sin espacios sobrantes en los textos. Al recibir la confirmación MUST regresar a la pantalla anterior.

#### Scenario: Creación exitosa
- **WHEN** todos los campos son válidos, el usuario pulsa "Crear álbum" y el backend confirma
- **THEN** la app regresa al catálogo
- **AND** se muestra el mensaje "Álbum creado"

#### Scenario: Cuerpo de la petición
- **WHEN** se envía el álbum
- **THEN** el cuerpo contiene exactamente `name`, `cover`, `releaseDate`, `description`, `genre` y `recordLabel`

### Requirement: Envío sin repeticiones
Mientras se envía el formulario, el botón SHALL desactivarse y mostrar "Creando...". La app MUST enviar una sola petición aunque el usuario toque varias veces, y MUST bloquear los campos, "Cancelar", la flecha y el botón atrás del sistema.

#### Scenario: Envío en curso
- **WHEN** el formulario se está enviando
- **THEN** el botón está desactivado y muestra "Creando..."
- **AND** los campos y "Cancelar" están desactivados

#### Scenario: Toques repetidos
- **WHEN** el usuario pulsa "Crear álbum" dos veces seguidas
- **THEN** se envía una sola petición

#### Scenario: Atrás durante el envío
- **WHEN** el formulario se está enviando y el usuario pulsa atrás
- **THEN** el formulario sigue abierto y el envío continúa

### Requirement: Error al crear
Si el álbum no se puede crear por un problema de conexión o del servicio, la app SHALL mostrar "No se pudo crear el álbum, intenta de nuevo", MUST conservar lo que el usuario escribió y MUST dejar el botón disponible para reintentar. La app MUST NOT cerrarse.

#### Scenario: Backend no disponible
- **WHEN** el envío falla por falta de conexión, tiempo agotado o un error del servidor
- **THEN** se muestra "No se pudo crear el álbum, intenta de nuevo"
- **AND** todos los campos conservan su valor y se pueden editar
- **AND** el botón está activo y muestra "Reintentar creación"

#### Scenario: Reintentar con éxito
- **WHEN** el usuario pulsa "Reintentar creación" y el backend ya responde
- **THEN** el álbum se crea y la app regresa al catálogo

### Requirement: Cancelar la creación
Mientras no haya un envío en curso, el usuario SHALL poder cerrar el formulario con "Cancelar", con la flecha o con el botón atrás del sistema, sin guardar nada y sin enviar datos al servidor.

#### Scenario: Cancelar con datos escritos
- **WHEN** el usuario llena campos y pulsa "Cancelar"
- **THEN** la app regresa a la pantalla anterior
- **AND** no se envía nada al servidor

#### Scenario: Volver a abrir tras cancelar
- **WHEN** el usuario cancela y abre de nuevo el formulario
- **THEN** el formulario aparece vacío

### Requirement: Conservación al girar la pantalla
Al girar la pantalla, el formulario SHALL conservar todo lo escrito o elegido, sus errores y su estado de envío.

#### Scenario: Girar con el formulario lleno
- **WHEN** el usuario llena los campos y gira la pantalla
- **THEN** todos los campos conservan su valor

#### Scenario: Girar con el selector abierto
- **WHEN** el selector de fecha está abierto y el usuario gira la pantalla
- **THEN** el selector sigue abierto
- **AND** "Aceptar" y "Cancelar" siguen visibles y se pueden pulsar

#### Scenario: Girar durante el envío
- **WHEN** el formulario se está enviando y el usuario gira la pantalla
- **THEN** el botón sigue mostrando "Creando..."
- **AND** no se envía una segunda petición
