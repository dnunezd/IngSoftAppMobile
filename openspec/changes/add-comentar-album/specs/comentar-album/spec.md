# Spec Delta

## ADDED Requirements

### Requirement: Acceso solo para el Coleccionista
El detalle del álbum SHALL mostrar el botón "Comentar álbum" solo cuando ingresó un Coleccionista. El Visitante MUST NOT ver el botón ni el formulario para comentar.

#### Scenario: Coleccionista en el detalle
- **WHEN** un Coleccionista abre el detalle de un álbum
- **THEN** ve el botón "Comentar álbum"
- **AND** al pulsarlo se abre la pantalla "Comentar álbum" de ese álbum

#### Scenario: Visitante en el detalle
- **WHEN** un Visitante abre el detalle de un álbum
- **THEN** no ve el botón "Comentar álbum"

#### Scenario: Sesión perdida
- **WHEN** la pantalla "Comentar álbum" se restaura sin una sesión de Coleccionista
- **THEN** la pantalla se cierra sin mostrar el formulario

### Requirement: Álbum que se comenta
La pantalla SHALL mostrar el nombre del álbum que se va a comentar, junto con su portada, sus artistas, su año y el nombre del coleccionista que comenta. Si el álbum no se puede obtener, SHALL mostrar un error con la opción de reintentar.

#### Scenario: Álbum disponible
- **WHEN** se abre la pantalla para el álbum "Buscando América" como "Jaime Monsalve"
- **THEN** se ve "Buscando América"
- **AND** se ve "Jaime Monsalve"

#### Scenario: Álbum guardado en el dispositivo
- **WHEN** el álbum ya está guardado en el dispositivo
- **THEN** la pantalla lo muestra sin consultar el backend

#### Scenario: Álbum que no se puede obtener
- **WHEN** el álbum no está guardado y la consulta falla
- **THEN** se muestra un error con "Reintentar"
- **AND** al pulsar "Reintentar" se vuelve a consultar

### Requirement: Calificación
La pantalla SHALL mostrar 5 estrellas para calificar, de las que se elige de 1 a 5, y el texto de la calificación elegida: "Muy malo (1/5)", "Malo (2/5)", "Regular (3/5)", "Bueno (4/5)" o "Excelente (5/5)". Al elegir una estrella MUST quedar marcadas esa y las anteriores.

#### Scenario: Elegir la calificación
- **WHEN** el coleccionista pulsa la cuarta estrella
- **THEN** quedan marcadas las cuatro primeras
- **AND** se ve "Bueno (4/5)"

#### Scenario: Sin calificación
- **WHEN** el coleccionista no ha elegido ninguna estrella
- **THEN** ninguna estrella está marcada
- **AND** no se muestra texto de calificación

### Requirement: Opinión
La pantalla SHALL mostrar un campo de texto con el texto de ayuda "Escribe tu opinión". La opinión MUST admitir como máximo 400 caracteres y mostrar cuántos lleva.

#### Scenario: Contador
- **WHEN** el coleccionista escribe una opinión de 57 caracteres
- **THEN** se ve "57 / 400"

#### Scenario: Límite de la opinión
- **WHEN** el coleccionista escribe más de 400 caracteres
- **THEN** la opinión conserva solo los primeros 400
- **AND** se ve "400 / 400"

### Requirement: Botón de publicar
El botón "Publicar comentario" SHALL permanecer deshabilitado mientras no se haya elegido una calificación y escrito una opinión. Una opinión con solo espacios MUST considerarse vacía.

#### Scenario: Falta la calificación
- **WHEN** hay opinión pero no calificación
- **THEN** "Publicar comentario" está deshabilitado

#### Scenario: Falta la opinión
- **WHEN** hay calificación pero la opinión está vacía o tiene solo espacios
- **THEN** "Publicar comentario" está deshabilitado

#### Scenario: Campos completos
- **WHEN** hay calificación y opinión
- **THEN** "Publicar comentario" está habilitado

### Requirement: Publicar el comentario
Al pulsar "Publicar comentario", la app SHALL enviar `POST /albums/{id}/comments` con la opinión sin espacios al inicio ni al final, la calificación y el id del coleccionista que ingresó. Mientras envía, el botón MUST mostrar "Publicando..." y la app MUST NOT permitir editar, salir ni enviar de nuevo.

#### Scenario: Envío
- **WHEN** "Jaime Monsalve" (id 101) califica con 5 y escribe " Gran prensado " sobre el álbum 100, y pulsa "Publicar comentario"
- **THEN** se envía a `/albums/100/comments` el cuerpo `{"description": "Gran prensado", "rating": 5, "collector": {"id": 101}}`

#### Scenario: Dos toques seguidos
- **WHEN** el coleccionista pulsa "Publicar comentario" dos veces seguidas
- **THEN** se envía un solo comentario

#### Scenario: Durante el envío
- **WHEN** el comentario se está enviando
- **THEN** el botón muestra "Publicando..." y está deshabilitado
- **AND** las estrellas y la opinión no se pueden editar
- **AND** la flecha y el botón atrás del sistema no cierran la pantalla

### Requirement: Comentario publicado
Si el backend guarda el comentario, la app SHALL mostrar la pantalla de éxito con el mensaje "Comentario publicado", el álbum, el coleccionista, la calificación y la opinión. Al pulsar "Aceptar", la flecha o el botón atrás del sistema, MUST volver al detalle del álbum, que se actualiza y muestra el comentario nuevo.

#### Scenario: Éxito
- **WHEN** el backend responde que guardó el comentario
- **THEN** se ve "Comentario publicado"
- **AND** se ve la opinión publicada y su calificación

#### Scenario: Volver al detalle
- **WHEN** el coleccionista pulsa "Aceptar"
- **THEN** vuelve al detalle del álbum
- **AND** el detalle se vuelve a consultar y muestra el comentario nuevo

### Requirement: Error al publicar
Si el envío falla por cualquier motivo (sin conexión, error del servidor, álbum o coleccionista inexistente, o datos inválidos), la app SHALL mostrar "No se pudo publicar, intenta de nuevo" y MUST conservar la calificación y la opinión, con el botón habilitado para reintentar.

#### Scenario: Falla el envío
- **WHEN** el backend responde con error o no hay conexión
- **THEN** se ve "No se pudo publicar, intenta de nuevo"
- **AND** la calificación y la opinión siguen como estaban
- **AND** "Publicar comentario" está habilitado

#### Scenario: Editar después del error
- **WHEN** se muestra el error y el coleccionista edita la opinión
- **THEN** el mensaje de error deja de mostrarse

### Requirement: Descartar el comentario
Si el coleccionista intenta salir con una calificación o una opinión sin publicar, la app SHALL preguntar "¿Descartar comentario?" con las opciones "Continuar escribiendo" y "Descartar y salir". Sin nada escrito MUST salir sin preguntar.

#### Scenario: Salir con algo escrito
- **WHEN** hay calificación u opinión y el coleccionista pulsa la flecha o el botón atrás del sistema
- **THEN** se ve "¿Descartar comentario?"

#### Scenario: Continuar escribiendo
- **WHEN** el diálogo está abierto y el coleccionista pulsa "Continuar escribiendo"
- **THEN** el diálogo se cierra y lo escrito se conserva

#### Scenario: Descartar y salir
- **WHEN** el diálogo está abierto y el coleccionista pulsa "Descartar y salir"
- **THEN** vuelve al detalle del álbum sin publicar nada

#### Scenario: Salir sin nada escrito
- **WHEN** no hay calificación ni opinión y el coleccionista pulsa la flecha
- **THEN** vuelve al detalle del álbum sin preguntar

### Requirement: Conservación al girar
La pantalla SHALL conservar la calificación, la opinión, el estado del envío y el diálogo abierto al girar el teléfono.

#### Scenario: Girar con el formulario lleno
- **WHEN** hay calificación y opinión y el coleccionista gira el teléfono
- **THEN** la calificación y la opinión siguen como estaban
