# catalogo-albumes Specification

## Purpose
Definir el catálogo de álbumes al que llega el usuario desde la pantalla de inicio: qué muestra de cada álbum, en qué orden, cómo responde mientras carga, ante un catálogo vacío o un fallo, cómo se actualiza y cómo lleva al detalle de un álbum.

## Requirements

### Requirement: Mostrar el catálogo de álbumes
Al llegar al catálogo, la app SHALL consultar `GET /albums` y mostrar todos los álbumes recibidos. Cada álbum MUST mostrar su portada, su nombre y su género, y MAY mostrar además su artista y su año de lanzamiento cuando el backend los entregue. El catálogo MUST ser el mismo para el Visitante y para el Coleccionista; solo cambia la etiqueta de rol del encabezado ("VISITANTE" o "COLECCIONISTA").

#### Scenario: Catálogo como Visitante
- **WHEN** el usuario entra como Visitante y pulsa "Ver álbumes"
- **THEN** se muestra el catálogo con la etiqueta "VISITANTE"
- **AND** cada álbum muestra su portada, su nombre y su género

#### Scenario: Catálogo como Coleccionista
- **WHEN** el usuario entra como Coleccionista, elige un coleccionista y pulsa "Ver álbumes"
- **THEN** se muestra el catálogo con la etiqueta "COLECCIONISTA"
- **AND** los álbumes y sus datos son los mismos que ve el Visitante

#### Scenario: Datos complementarios
- **WHEN** un álbum tiene artistas y fecha de lanzamiento
- **THEN** su tarjeta muestra el nombre del artista y el año de lanzamiento
- **AND** si no los tiene, la tarjeta se muestra igual sin esos datos

#### Scenario: Artista sin nombre
- **WHEN** un álbum no tiene artistas, o sus artistas llegan sin nombre
- **THEN** la tarjeta no muestra artista ni el texto "null"
- **AND** si alguno de sus artistas sí tiene nombre, se muestra ese nombre

#### Scenario: Cantidad de álbumes
- **WHEN** el catálogo tiene álbumes
- **THEN** se indica cuántos álbumes contiene

#### Scenario: Álbum incompleto
- **WHEN** el backend entrega un álbum sin identificador, sin nombre o sin género
- **THEN** ese álbum no se muestra
- **AND** los demás álbumes se muestran normalmente

### Requirement: Orden alfabético
Los álbumes SHALL mostrarse ordenados alfabéticamente por nombre, sin distinguir mayúsculas, minúsculas ni tildes.

#### Scenario: Álbumes de ejemplo
- **WHEN** el backend entrega "Buscando América", "Poeta del pueblo", "A Night at the Opera" y "A Day at the Races"
- **THEN** se muestran en el orden "A Day at the Races", "A Night at the Opera", "Buscando América", "Poeta del pueblo"

#### Scenario: Mayúsculas y tildes
- **WHEN** hay álbumes cuyos nombres solo difieren en mayúsculas o tildes de otros
- **THEN** quedan ordenados como si esas diferencias no existieran

### Requirement: Indicador de carga
Mientras se obtienen los álbumes, la app SHALL mostrar un indicador de progreso en lugar de una pantalla vacía.

#### Scenario: Consulta en curso
- **WHEN** el catálogo se está consultando por primera vez
- **THEN** se muestra un indicador de progreso
- **AND** no se muestra una pantalla en blanco

### Requirement: Error de carga recuperable
Si el catálogo no se puede cargar por un problema de conexión o del servidor, la app SHALL mostrar el mensaje "No pudimos cargar el catálogo" con un botón "Reintentar", y MUST NOT cerrarse.

#### Scenario: Backend no disponible
- **WHEN** la consulta falla por falta de conexión, por un error del servidor o por una respuesta ilegible
- **THEN** se muestra "No pudimos cargar el catálogo" y el botón "Reintentar"
- **AND** la app no se cierra

#### Scenario: Reintentar con éxito
- **WHEN** el usuario pulsa "Reintentar" y el backend ya responde
- **THEN** se muestra el indicador de progreso y luego el catálogo

### Requirement: Catálogo vacío
Si el backend responde sin álbumes, la app SHALL mostrar el mensaje "Aún no hay álbumes en el catálogo".

#### Scenario: Sin álbumes
- **WHEN** la consulta es exitosa y no devuelve álbumes
- **THEN** se muestra "Aún no hay álbumes en el catálogo"

### Requirement: Portada por defecto
Si la portada de un álbum falta o no se puede cargar, la app SHALL mostrar una imagen genérica en su lugar, y la lista MUST seguir funcionando.

#### Scenario: Portada inválida
- **WHEN** un álbum tiene una URL de portada vacía, inválida o que no se puede descargar
- **THEN** su tarjeta muestra la imagen genérica
- **AND** los demás álbumes y el desplazamiento siguen funcionando

### Requirement: Catálogos largos
Cuando el catálogo tiene muchos álbumes, el usuario SHALL poder recorrer toda la lista desplazándose, sin que la app se detenga ni se cierre.

#### Scenario: Recorrer hasta el final
- **WHEN** el catálogo tiene 50 álbumes y el usuario se desplaza hasta el final
- **THEN** el último álbum queda visible
- **AND** la app no se detiene ni se cierra

### Requirement: Conservación al girar la pantalla
Al girar la pantalla, el catálogo SHALL conservar su posición y MUST NOT volver a consultar el backend sin necesidad.

#### Scenario: Rotación con el catálogo cargado
- **WHEN** el usuario se desplaza a la mitad del catálogo y gira la pantalla
- **THEN** se siguen viendo los mismos álbumes en la misma posición
- **AND** no se muestra de nuevo el indicador de carga ni se repite la consulta

### Requirement: Actualizar deslizando hacia abajo
El usuario SHALL poder actualizar el catálogo deslizando la lista hacia abajo, de modo que los álbumes nuevos aparezcan sin reiniciar la app. Mientras se actualiza, MUST seguir viendo los álbumes que ya tenía.

#### Scenario: Aparece un álbum nuevo
- **WHEN** el backend tiene un álbum nuevo y el usuario desliza la lista hacia abajo
- **THEN** se muestra un indicador de actualización sobre la lista
- **AND** el álbum nuevo aparece en su posición alfabética sin reiniciar la app

#### Scenario: Actualizar un catálogo vacío
- **WHEN** se muestra "Aún no hay álbumes en el catálogo" y el usuario desliza hacia abajo
- **THEN** se vuelve a consultar el catálogo y, si ya hay álbumes, se muestran

#### Scenario: Falla la actualización
- **WHEN** la actualización falla mientras se ven los álbumes
- **THEN** los álbumes que ya se veían siguen en pantalla
- **AND** se informa brevemente "No pudimos cargar el catálogo"
- **AND** la app no se cierra

### Requirement: Abrir el detalle de un álbum
Al tocar un álbum del catálogo, la app SHALL abrir el detalle de ese álbum. Al volver, MUST regresar al catálogo sin volver a consultarlo.

#### Scenario: Tocar un álbum
- **WHEN** el usuario toca "Buscando América" en el catálogo
- **THEN** se abre el detalle del álbum con ese identificador

#### Scenario: Volver al catálogo
- **WHEN** el usuario vuelve desde el detalle
- **THEN** se muestra el catálogo en la misma posición
- **AND** no se repite la consulta del catálogo
