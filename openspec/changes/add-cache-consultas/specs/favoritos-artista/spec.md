# Spec Delta

## MODIFIED Requirements

### Requirement: Botón desactivado si falla la comprobación de favorito
Si la app no puede comprobar si el artista ya es favorito del coleccionista y no lo tiene guardado como favorito confirmado, el botón SHALL aparecer desactivado y la pantalla SHALL ofrecer la opción de reintentar, para no permitir agregarlo dos veces por error. Si lo tiene guardado como favorito confirmado, el botón MUST mostrarse como "Favorito" y desactivado.

#### Scenario: Falla la comprobación inicial
- **WHEN** el coleccionista abre el detalle de un artista que no tiene guardado como favorito
- **AND** la consulta de favoritos falla por un problema de conexión o del servicio
- **THEN** el botón aparece desactivado
- **AND** la pantalla ofrece la opción de reintentar

#### Scenario: Reintentar la comprobación
- **WHEN** el coleccionista pulsa reintentar tras una falla de comprobación
- **AND** la consulta responde correctamente
- **THEN** el botón refleja el estado real de favorito (activo, o "Favorito" desactivado)

#### Scenario: Falla la comprobación de un favorito guardado
- **WHEN** el coleccionista abre el detalle de un artista que tiene guardado como favorito confirmado
- **AND** la consulta de favoritos falla por un problema de conexión o del servicio
- **THEN** el botón muestra "Favorito" y está desactivado
- **AND** no se muestra el error de comprobación

## ADDED Requirements

### Requirement: Favoritos confirmados guardados en el dispositivo
La app SHALL guardar en el dispositivo, por coleccionista, los artistas que el backend confirmó como favoritos, ya sea al comprobar el estado o al agregar uno con éxito. Cada comprobación exitosa MUST reemplazar por completo los favoritos guardados de ese coleccionista, y lo guardado MUST conservarse al cerrar y volver a abrir la app. La app MUST NOT agregar favoritos sin conexión.

#### Scenario: Favorito agregado y luego sin conexión
- **WHEN** el coleccionista agrega un artista como favorito con conexión
- **AND** más tarde abre el detalle de ese artista sin conexión
- **THEN** el botón muestra "Favorito" y está desactivado

#### Scenario: Favorito quitado en el backend
- **WHEN** un artista guardado como favorito ya no está en la respuesta de favoritos del coleccionista
- **THEN** deja de estar guardado como favorito
- **AND** con conexión el botón muestra "Agregar Favorito" activo

#### Scenario: Favoritos de otro coleccionista
- **WHEN** un coleccionista tiene guardado un artista como favorito
- **AND** otro coleccionista abre sin conexión el detalle de ese artista
- **THEN** el botón aparece desactivado con la opción de reintentar
- **AND** no muestra "Favorito"

#### Scenario: Agregar sin conexión
- **WHEN** la comprobación salió bien y muestra "Agregar Favorito"
- **AND** se pierde la conexión y el coleccionista pulsa "Agregar Favorito"
- **THEN** se muestra el error de guardado que ya existe
- **AND** el artista no queda guardado como favorito

#### Scenario: Falla el almacenamiento del dispositivo
- **WHEN** no se pueden leer ni escribir los favoritos guardados
- **THEN** el botón se comporta como si no hubiera nada guardado
- **AND** la app no se cierra
