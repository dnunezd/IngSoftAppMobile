# Spec Delta

## Purpose

Define la pantalla de inicio a la que llega el usuario después de ingresar, desde la cual elige explorar el catálogo de álbumes o el listado de artistas.

## ADDED Requirements

### Requirement: Pantalla de inicio tras el ingreso
Después de ingresar, como Visitante o como coleccionista, la app SHALL mostrar la pantalla de inicio con un saludo, el titular "Descubre música en su formato más puro", un subtítulo, y dos tarjetas: "Catálogo de Álbumes" y "Listado de Artistas". El saludo MUST ser "¡Hola, melómano!" para el Visitante y "¡Hola, <nombre>!" para el coleccionista, con el nombre del coleccionista elegido. El resto de la pantalla MUST ser igual para ambos roles, y la pantalla MUST NOT consultar al backend.

#### Scenario: Inicio como visitante
- **WHEN** el usuario ingresa como Visitante
- **THEN** se muestra la pantalla de inicio con el saludo "¡Hola, melómano!", el titular y las tarjetas "Catálogo de Álbumes" y "Listado de Artistas"

#### Scenario: Inicio como coleccionista
- **WHEN** el usuario ingresa eligiendo al coleccionista "Manolo Bellon"
- **THEN** se muestra la pantalla de inicio con el saludo "¡Hola, Manolo Bellon!"
- **AND** el titular y las dos tarjetas son los mismos que para el Visitante
- **AND** no se muestra "¡Hola, melómano!"

#### Scenario: Cambio de perfil actualiza el saludo
- **WHEN** el usuario vuelve al ingreso desde la pantalla de inicio y entra con otro perfil
- **THEN** el saludo corresponde al nuevo perfil

#### Scenario: Contenido de las tarjetas
- **WHEN** se muestra la pantalla de inicio
- **THEN** la tarjeta "Catálogo de Álbumes" tiene su etiqueta "CATÁLOGO COMPLETO · LPS", una descripción, una ilustración y el botón "Ver álbumes"
- **AND** la tarjeta "Listado de Artistas" tiene su etiqueta "LEYENDAS & BANDAS", una descripción, una ilustración y el botón "Ver artistas"

### Requirement: Ir al catálogo de álbumes
Al pulsar "Ver álbumes", la app SHALL llevar al usuario al catálogo de álbumes.

#### Scenario: Ver álbumes
- **WHEN** el usuario pulsa "Ver álbumes" en la pantalla de inicio
- **THEN** la app navega al catálogo de álbumes

### Requirement: Ir al listado de artistas
Al pulsar "Ver artistas", la app SHALL llevar al usuario al listado de artistas.

#### Scenario: Ver artistas
- **WHEN** el usuario pulsa "Ver artistas" en la pantalla de inicio
- **THEN** la app navega al listado de artistas

### Requirement: Volver a la pantalla de inicio
Desde el catálogo de álbumes y desde el listado de artistas, la flecha de atrás del encabezado y el botón "Atrás" del sistema SHALL llevar al usuario de vuelta a la pantalla de inicio, sin cambiar la sesión.

#### Scenario: Atrás desde el catálogo
- **WHEN** el usuario está en el catálogo de álbumes y presiona "Atrás"
- **THEN** la app muestra la pantalla de inicio
- **AND** el rol y el coleccionista activos no cambian

#### Scenario: Atrás desde los artistas
- **WHEN** el usuario está en el listado de artistas y presiona "Atrás"
- **THEN** la app muestra la pantalla de inicio
- **AND** el rol y el coleccionista activos no cambian

### Requirement: Conservación del estado de la pantalla de inicio al rotar
La pantalla de inicio SHALL seguir visible con su contenido al girar el dispositivo.

#### Scenario: Rotación en la pantalla de inicio
- **WHEN** el usuario está en la pantalla de inicio y gira el dispositivo
- **THEN** la pantalla de inicio sigue visible y la sesión se mantiene
