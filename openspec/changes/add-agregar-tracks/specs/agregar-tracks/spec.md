# Spec Delta

## ADDED Requirements

### Requirement: Acción para agregar una canción
Desde el detalle de un álbum, la app SHALL ofrecer al Coleccionista la acción para abrir el formulario "Agregar canción". MUST NOT mostrar ni habilitar esta acción para el Visitante.

#### Scenario: Coleccionista abre el formulario
- **WHEN** un Coleccionista está en el detalle de un álbum y activa la acción para agregar una canción
- **THEN** se abre el formulario asociado al ID de ese álbum

#### Scenario: Visitante consulta el detalle
- **WHEN** un Visitante abre el detalle de un álbum
- **THEN** no ve la acción para agregar una canción

### Requirement: Campos y validación inicial
El formulario SHALL pedir el nombre y la duración. Al abrirse vacío, SHALL mostrar "Campo obligatorio" en ambos campos y MUST mantener "Guardar" deshabilitado. Al editar un campo, SHALL actualizar el error correspondiente sin ocultar el error del otro campo.

#### Scenario: Formulario vacío al abrir
- **WHEN** el Coleccionista abre el formulario
- **THEN** el campo de nombre muestra "Campo obligatorio"
- **AND** el campo de duración muestra "Campo obligatorio"
- **AND** "Guardar" está deshabilitado
- **AND** no se envía ninguna petición

#### Scenario: Nombre obligatorio
- **WHEN** el nombre está vacío o contiene solo espacios
- **THEN** el campo de nombre muestra "Campo obligatorio"
- **AND** "Guardar" está deshabilitado
- **AND** no se envía ninguna petición

#### Scenario: Duración obligatoria
- **WHEN** la duración está vacía
- **THEN** el campo de duración muestra "Campo obligatorio"
- **AND** "Guardar" está deshabilitado
- **AND** no se envía ninguna petición

#### Scenario: Corregir un campo
- **WHEN** el usuario edita un campo que muestra un error
- **THEN** se actualiza el error de ese campo según su valor
- **AND** el error del otro campo se conserva hasta que su valor sea válido

### Requirement: Formato de duración
La duración SHALL cumplir el formato `m:ss`: uno o más dígitos de minutos, dos dígitos de segundos y un valor de segundos entre `00` y `59`. Si el valor no está vacío y no cumple el formato, la app SHALL mostrar "Usa el formato m:ss, por ejemplo 5:05" debajo del campo y MUST mantener "Guardar" deshabilitado.

#### Scenario: Duración válida
- **WHEN** el usuario escribe una duración como "5:05" o "12:34"
- **THEN** la duración no muestra error de formato

#### Scenario: Segundos fuera de rango
- **WHEN** el usuario escribe una duración con segundos mayores que 59
- **THEN** aparece "Usa el formato m:ss, por ejemplo 5:05"
- **AND** "Guardar" está deshabilitado

#### Scenario: Formato incorrecto
- **WHEN** el usuario escribe una duración no vacía que no tiene minutos y dos dígitos de segundos
- **THEN** aparece "Usa el formato m:ss, por ejemplo 5:05"
- **AND** "Guardar" está deshabilitado

### Requirement: Guardar y asociar la canción al álbum
Cuando el nombre y la duración sean válidos, "Guardar" SHALL estar habilitado. Al activarlo, la app SHALL asociar la canción con el álbum cuyo detalle originó el formulario. Ante una confirmación exitosa, SHALL mostrar "Canción agregada", regresar al detalle del mismo álbum y actualizar la lista de canciones.

#### Scenario: Guardado exitoso
- **WHEN** el Coleccionista guarda una canción válida y el servicio confirma la asociación
- **THEN** se muestra "Canción agregada"
- **AND** la app regresa al detalle del mismo álbum
- **AND** el detalle muestra la canción agregada con su duración

#### Scenario: Guardado con campos inválidos
- **WHEN** el nombre o la duración no son válidos
- **THEN** "Guardar" está deshabilitado
- **AND** no se envía ninguna petición

### Requirement: Evitar envíos repetidos
Mientras se guarda la canción, la app SHALL deshabilitar el botón y mostrar "Guardando...". La app MUST realizar como máximo una petición de guardado por envío, aunque se reciban toques repetidos o la pantalla se recree por una rotación.

#### Scenario: Guardado en curso
- **WHEN** la petición de guardado está pendiente
- **THEN** "Guardar" está deshabilitado y muestra "Guardando..."

#### Scenario: Toques repetidos
- **WHEN** el usuario activa el botón varias veces durante el mismo envío
- **THEN** se realiza una sola petición

#### Scenario: Rotar durante el guardado
- **WHEN** el dispositivo gira mientras la petición de guardado está pendiente
- **THEN** el estado sigue mostrando "Guardando..."
- **AND** no se inicia una petición adicional

### Requirement: Cancelar sin guardar
Antes de iniciar un envío, el usuario SHALL poder cancelar el formulario. Cancelar SHALL cerrar el formulario sin guardar datos ni enviar una petición. Al abrirlo de nuevo, SHALL aparecer vacío. Mientras una petición esté pendiente, "Cancelar", la flecha y el botón atrás del sistema SHALL estar deshabilitados para no abandonar un resultado incierto.

#### Scenario: Cancelar con datos escritos
- **WHEN** el usuario llena uno o más campos y activa "Cancelar" antes de guardar
- **THEN** se cierra el formulario
- **AND** no se envía ninguna petición

#### Scenario: Abrir de nuevo después de cancelar
- **WHEN** el usuario cancela y abre de nuevo el formulario
- **THEN** ambos campos aparecen vacíos

#### Scenario: Salida durante el envío
- **WHEN** la petición de guardado está pendiente
- **THEN** "Cancelar", la flecha y el botón atrás del sistema no cierran el formulario

### Requirement: Error recuperable al guardar
Si el guardado falla por conexión o por un error del servicio distinto de que el álbum no exista, la app SHALL mostrar "No se pudo agregar la canción, intenta de nuevo", SHALL conservar los valores y SHALL permitir reintentar. MUST NOT mostrar confirmación de éxito ni cerrar el formulario.

#### Scenario: Fallo de conexión o servicio
- **WHEN** no se puede guardar por falta de conexión, timeout o error del servicio
- **THEN** se muestra "No se pudo agregar la canción, intenta de nuevo"
- **AND** el nombre y la duración conservan lo escrito
- **AND** "Guardar" vuelve a estar disponible si los valores siguen válidos

#### Scenario: Reintento exitoso
- **WHEN** el usuario reintenta y el servicio confirma la asociación
- **THEN** se muestra "Canción agregada"
- **AND** se regresa al detalle actualizado del álbum

### Requirement: Álbum no disponible al guardar
Si el servicio responde que el álbum ya no existe, la app SHALL mostrar "El álbum ya no está disponible", MUST NOT asociar la canción y SHALL permitir regresar a la pantalla anterior.

#### Scenario: Álbum eliminado antes del guardado
- **WHEN** la petición de asociación informa que el álbum no existe
- **THEN** se muestra "El álbum ya no está disponible"
- **AND** la canción no se agrega
- **AND** se ofrece regresar a la pantalla anterior

#### Scenario: Regresar desde álbum no disponible
- **WHEN** el usuario activa la acción para regresar desde el mensaje de álbum no disponible
- **THEN** vuelve a la pantalla anterior

### Requirement: Conservar el formulario al girar
Al girar la pantalla, la app SHALL conservar el nombre, la duración, los errores visibles y el estado del formulario. Si había un envío en curso, MUST conservarlo sin enviar la canción una segunda vez.

#### Scenario: Girar con datos escritos
- **WHEN** el usuario escribe el nombre y la duración y gira el dispositivo
- **THEN** ambos campos conservan los valores
- **AND** los errores corresponden a los valores conservados

#### Scenario: Girar durante el envío
- **WHEN** el usuario gira el dispositivo mientras guarda
- **THEN** el envío continúa una sola vez
- **AND** "Guardar" permanece en estado deshabilitado "Guardando..."
