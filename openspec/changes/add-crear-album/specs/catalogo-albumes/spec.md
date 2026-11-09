# Spec Delta

## ADDED Requirements

### Requirement: Acceso a crear un álbum
El catálogo SHALL mostrar un botón con el texto "Crear álbum", fijo abajo a la derecha, solo cuando la sesión tiene habilitadas las acciones de coleccionista y en todos los estados del catálogo. Al Visitante MUST NOT mostrárselo.

#### Scenario: Coleccionista
- **WHEN** el usuario entra al catálogo como Coleccionista
- **THEN** ve el botón "Crear álbum"
- **AND** al pulsarlo se abre el formulario

#### Scenario: Visitante
- **WHEN** el usuario entra al catálogo como Visitante
- **THEN** no ve el botón "Crear álbum"

#### Scenario: Catálogo vacío o con error
- **WHEN** el Coleccionista ve "Aún no hay álbumes en el catálogo" o "No pudimos cargar el catálogo"
- **THEN** sigue viendo el botón "Crear álbum"

#### Scenario: Visible al desplazarse
- **WHEN** el Coleccionista se desplaza por el catálogo
- **THEN** el botón "Crear álbum" sigue visible

#### Scenario: El botón no tapa el contenido
- **WHEN** el Coleccionista se desplaza hasta el final del catálogo
- **THEN** la última tarjeta queda completamente visible por encima del botón

### Requirement: Mostrar el álbum recién creado
Al volver del formulario tras crear un álbum, el catálogo SHALL mostrar "Álbum creado", volver a consultar los álbumes y desplazarse hasta el nuevo en su posición alfabética. El aviso y el desplazamiento MUST ocurrir una sola vez.

#### Scenario: Volver con el catálogo cargado
- **WHEN** el usuario crea "Abbey Road" y regresa al catálogo
- **THEN** se muestra "Álbum creado"
- **AND** "Abbey Road" aparece en su posición alfabética y queda visible

#### Scenario: Catálogo con error al crear
- **WHEN** el catálogo mostraba "No pudimos cargar el catálogo" y el usuario crea un álbum
- **THEN** al regresar el catálogo se vuelve a cargar

#### Scenario: Falla la consulta posterior
- **WHEN** el álbum se creó pero la consulta del catálogo falla
- **THEN** se muestra "Álbum creado"
- **AND** se informa "No pudimos actualizar el catálogo"
- **AND** los álbumes que ya se veían siguen en pantalla

#### Scenario: Girar después de crear
- **WHEN** el usuario regresa al catálogo tras crear un álbum y gira la pantalla
- **THEN** no se repite el aviso, la consulta ni el desplazamiento

#### Scenario: Cancelar no consulta
- **WHEN** el usuario cancela el formulario y regresa al catálogo
- **THEN** no se muestra "Álbum creado"
- **AND** no se repite la consulta del catálogo

### Requirement: Insignia del álbum nuevo
La tarjeta del álbum recién creado SHALL mostrar la insignia "NUEVO" mientras el usuario permanezca en el catálogo. Al salir del catálogo hacia la pantalla de inicio, la insignia MUST desaparecer.

#### Scenario: Insignia en la tarjeta
- **WHEN** el usuario regresa al catálogo tras crear un álbum
- **THEN** solo la tarjeta de ese álbum muestra "NUEVO"

#### Scenario: Se conserva dentro del catálogo
- **WHEN** el usuario gira la pantalla, abre un álbum y vuelve, o desliza para actualizar
- **THEN** la tarjeta sigue mostrando "NUEVO"

#### Scenario: Crear otro álbum
- **WHEN** el usuario crea un segundo álbum
- **THEN** "NUEVO" se muestra solo en el segundo

#### Scenario: Salir del catálogo
- **WHEN** el usuario vuelve al inicio y entra otra vez al catálogo
- **THEN** ninguna tarjeta muestra "NUEVO"
