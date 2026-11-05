# Spec Delta

## Purpose

Permite a un coleccionista marcar artistas (músicos o bandas) como favoritos desde su detalle, y ver reflejado ese estado cada vez que vuelve a abrir el detalle, incluso tras cerrar y reabrir la app.

## ADDED Requirements

### Requirement: Marcar un artista como favorito
Cuando el coleccionista está en el detalle de un artista que no es su favorito, el detalle SHALL mostrar el botón "Agregar Favorito". Al pulsarlo y guardarse correctamente, el botón SHALL pasar a mostrar "Favorito".

#### Scenario: Agregar favorito exitosamente
- **WHEN** el coleccionista pulsa "Agregar Favorito" en el detalle de un artista que no es su favorito
- **AND** el backend confirma el guardado
- **THEN** el botón muestra "Favorito"

### Requirement: Botón desactivado cuando ya es favorito
Si el artista ya es favorito del coleccionista, el detalle SHALL mostrar el botón como "Favorito" y desactivado, de modo que no se pueda agregar otra vez.

#### Scenario: Artista ya favorito
- **WHEN** el coleccionista abre el detalle de un artista que ya es su favorito
- **THEN** el botón muestra "Favorito" y está desactivado

### Requirement: Persistencia del estado de favorito entre aperturas
Cada vez que el coleccionista abre el detalle de un artista, la app SHALL comprobar si ya es su favorito y reflejar ese estado en el botón, incluso si la app fue cerrada y vuelta a abrir.

#### Scenario: Reabrir la app
- **WHEN** el coleccionista marcó un artista como favorito, cierra la app y la vuelve a abrir
- **AND** abre de nuevo el detalle de ese artista
- **THEN** el botón muestra "Favorito" y está desactivado

### Requirement: Botón desactivado si falla la comprobación de favorito
Si la app no puede comprobar si el artista ya es favorito del coleccionista, el botón SHALL aparecer desactivado y la pantalla SHALL ofrecer la opción de reintentar, para no permitir agregarlo dos veces por error.

#### Scenario: Falla la comprobación inicial
- **WHEN** el coleccionista abre el detalle de un artista
- **AND** la consulta de favoritos falla por un problema de conexión o del servicio
- **THEN** el botón aparece desactivado
- **AND** la pantalla ofrece la opción de reintentar

#### Scenario: Reintentar la comprobación
- **WHEN** el coleccionista pulsa reintentar tras una falla de comprobación
- **AND** la consulta responde correctamente
- **THEN** el botón refleja el estado real de favorito (activo, o "Favorito" desactivado)

### Requirement: Botón de favorito oculto para el Visitante
Si el usuario ingresó como Visitante, el detalle del artista SHALL NOT mostrar el botón de favorito.

#### Scenario: Visitante en el detalle de un artista
- **WHEN** un usuario que ingresó como Visitante abre el detalle de un artista
- **THEN** el detalle no muestra ningún botón de favorito

### Requirement: Artista no disponible al guardar el favorito
Si al guardar el favorito el artista ya no existe en el backend, la app SHALL mostrar el mensaje "El artista ya no está disponible" junto con un botón para regresar al listado de artistas, y ese listado SHALL actualizarse sin ese artista.

#### Scenario: El artista fue eliminado antes de guardar
- **WHEN** el coleccionista pulsa "Agregar Favorito"
- **AND** el backend responde que el artista ya no existe
- **THEN** se muestra "El artista ya no está disponible" con un botón para volver al listado
- **AND** al volver, el listado de artistas ya no incluye ese artista

### Requirement: Error de conexión o del servicio al guardar el favorito
Si el favorito no se puede guardar por un problema de conexión o del servicio, la app SHALL mostrar el mensaje "No se pudo guardar el favorito, intenta de nuevo" y el botón SHALL quedar disponible para reintentar.

#### Scenario: Falla de red al guardar
- **WHEN** el coleccionista pulsa "Agregar Favorito"
- **AND** la petición de guardado falla por falta de conexión o un error del servidor
- **THEN** se muestra "No se pudo guardar el favorito, intenta de nuevo"
- **AND** el botón vuelve a estar disponible para reintentar

### Requirement: Estado "Guardando" mientras se registra el favorito
Mientras se guarda el favorito, el botón SHALL desactivarse y mostrar "Guardando...", para evitar toques repetidos.

#### Scenario: Guardado en curso
- **WHEN** el coleccionista pulsa "Agregar Favorito"
- **AND** la respuesta del backend aún no llega
- **THEN** el botón muestra "Guardando..." y está desactivado

### Requirement: Confirmación al guardar el favorito
Cuando el favorito se guarda correctamente, la app SHALL mostrar un mensaje breve de confirmación con el nombre del artista (por ejemplo "Rubén Blades se agregó a tus favoritos"), que SHALL desaparecer solo después de unos segundos. Si el guardado falla, ese mensaje SHALL NOT aparecer.

#### Scenario: Confirmación tras guardar
- **WHEN** el backend confirma que el favorito se guardó
- **THEN** se muestra "Rubén Blades se agregó a tus favoritos"
- **AND** el mensaje desaparece por sí solo después de unos segundos

#### Scenario: Sin confirmación si falla el guardado
- **WHEN** el guardado del favorito falla
- **THEN** no se muestra ningún mensaje de confirmación
