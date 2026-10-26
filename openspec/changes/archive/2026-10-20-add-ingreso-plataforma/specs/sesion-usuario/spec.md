# Spec Delta

## Purpose

Mantener en la app quién la está usando (visitante o coleccionista concreto) y exponerlo a todas las pantallas, para que las acciones exclusivas del coleccionista se muestren u oculten según el rol.

## ADDED Requirements

### Requirement: Rol y coleccionista activos
La app SHALL mantener una sesión con el rol activo (visitante o coleccionista) y, cuando el rol es coleccionista, el coleccionista seleccionado (identificador, nombre y correo). La sesión MUST estar disponible para todas las pantallas de la app mientras la app esté en uso.

#### Scenario: Sesión de visitante
- **WHEN** el usuario ingresa como Visitante
- **THEN** el rol activo es visitante
- **AND** no hay coleccionista activo

#### Scenario: Sesión de coleccionista
- **WHEN** el usuario ingresa eligiendo un coleccionista del listado
- **THEN** el rol activo es coleccionista
- **AND** el coleccionista activo es el seleccionado, con su identificador, nombre y correo

#### Scenario: Cambio de perfil
- **WHEN** el usuario vuelve a la pantalla de ingreso y elige otro perfil
- **THEN** la sesión refleja únicamente el nuevo perfil y no conserva datos del anterior

### Requirement: Sesión inicial sin perfil
Mientras el usuario no haya elegido un perfil, la app SHALL considerar que no hay sesión activa y MUST NOT habilitar acciones de coleccionista.

#### Scenario: Antes de elegir perfil
- **WHEN** la app acaba de abrirse y el usuario está en la pantalla de ingreso
- **THEN** no hay rol ni coleccionista activos

#### Scenario: Atrás desde el listado sin elegir
- **WHEN** el usuario abre el listado de coleccionistas, no selecciona ninguno y presiona "Atrás"
- **THEN** no hay rol ni coleccionista activos

#### Scenario: Regreso al ingreso desde la pantalla de inicio
- **WHEN** hay un rol activo y el usuario vuelve a la pantalla de ingreso desde la pantalla de inicio
- **THEN** no hay rol ni coleccionista activos, y las acciones de coleccionista dejan de estar habilitadas

#### Scenario: Navegar entre inicio, catálogo y artistas
- **WHEN** hay un rol activo y el usuario va de la pantalla de inicio al catálogo o al listado de artistas y vuelve
- **THEN** el rol activo y el coleccionista activo se mantienen sin cambios

### Requirement: Acciones exclusivas del coleccionista según el rol
La app SHALL ofrecer a las pantallas una forma de saber si las acciones exclusivas del coleccionista (crear álbum, agregar tracks, comentar y agregar favoritos) están habilitadas. Estas acciones MUST estar habilitadas solo cuando el rol activo es coleccionista y MUST NOT mostrarse cuando el rol es visitante.

#### Scenario: Visitante sin acciones de coleccionista
- **WHEN** el rol activo es visitante
- **THEN** las acciones de crear álbum, agregar tracks, comentar y agregar favoritos no están habilitadas ni se muestran

#### Scenario: Coleccionista con acciones habilitadas
- **WHEN** el rol activo es coleccionista
- **THEN** las acciones de crear álbum, agregar tracks, comentar y agregar favoritos están habilitadas

### Requirement: Sesión ante rotación de pantalla
La sesión SHALL conservarse al girar el dispositivo y ante otros cambios de configuración de la pantalla.

#### Scenario: Rotación con sesión activa
- **WHEN** hay un rol activo y el usuario gira el dispositivo
- **THEN** el rol activo y el coleccionista activo se mantienen sin cambios
