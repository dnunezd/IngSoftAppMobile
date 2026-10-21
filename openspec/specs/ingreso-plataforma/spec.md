# ingreso-plataforma Specification

## Purpose

Define cómo el usuario ingresa a Vinilos eligiendo su perfil (Visitante o Coleccionista), de modo que la app sepa con qué rol se usará y habilite las funcionalidades que le corresponden.

## Requirements

### Requirement: Pantalla de ingreso al abrir la app
Al abrirse, la app SHALL mostrar la pantalla de ingreso con dos opciones claramente identificadas: "Visitante" y "Coleccionista". Cada opción MUST presentarse como una tarjeta con su nombre y un botón "Entrar". La pantalla de ingreso MUST ser la primera que ve el usuario y MUST NOT requerir datos adicionales para mostrarse.

#### Scenario: Apertura de la app
- **WHEN** el usuario abre la app Vinilos
- **THEN** se muestra la pantalla de ingreso con las opciones "Visitante" y "Coleccionista"
- **AND** cada opción tiene su botón "Entrar"

#### Scenario: Franja decorativa
- **WHEN** se muestra la pantalla de ingreso
- **THEN** bajo el subtítulo aparece una franja decorativa con el texto "AUDIO WAVE", una onda de audio y el texto "94 BPM"
- **AND** la franja no responde a toques ni cambia con los datos
- **AND** no se muestra ninguna insignia de versión

### Requirement: Ingreso como visitante
Al elegir "Visitante" (con su botón "Entrar"), la app SHALL llevar al usuario directamente a la pantalla de inicio, sin pedirle datos adicionales y sin consultar al backend.

#### Scenario: Elección de Visitante
- **WHEN** el usuario pulsa "Entrar" en la opción "Visitante"
- **THEN** la app navega a la pantalla de inicio
- **AND** no se solicita ningún dato adicional

#### Scenario: Visitante sin conexión
- **WHEN** el dispositivo no tiene conexión y el usuario elige "Visitante"
- **THEN** la app navega igualmente a la pantalla de inicio, sin mostrar errores de conexión en el ingreso

### Requirement: Listado de coleccionistas para ingresar
Al elegir "Coleccionista" (con su botón "Entrar"), la app SHALL mostrar el listado de coleccionistas registrados, presentando para cada uno un avatar con sus iniciales, su nombre y su correo. Los datos MUST obtenerse del backend en el momento de abrir el listado.

#### Scenario: Listado con coleccionistas
- **WHEN** el usuario pulsa "Entrar" en la opción "Coleccionista" y el backend devuelve coleccionistas
- **THEN** la app muestra una lista donde cada elemento incluye un avatar con las iniciales, el nombre y el correo del coleccionista

#### Scenario: Iniciales del avatar
- **WHEN** se muestra un coleccionista llamado "Manolo Bellon"
- **THEN** su avatar muestra las iniciales "MB"

### Requirement: Franja decorativa del selector
El selector de coleccionistas SHALL mostrar, bajo el titular "¿Quién eres?" y sobre el subtítulo, una franja decorativa con el texto "AUDIO FEED" entre dos ondas de barras, en cualquiera de sus estados (cargando, con datos, error o vacío).

#### Scenario: Franja visible
- **WHEN** se muestra el selector de coleccionistas
- **THEN** aparece la franja con el texto "AUDIO FEED" y una onda de barras a cada lado
- **AND** la franja no responde a toques ni cambia con los datos

### Requirement: Búsqueda de coleccionistas
Cuando el listado de coleccionistas está cargado, la app SHALL mostrar un campo de búsqueda con el texto de ayuda "Buscar coleccionista..." que filtre los coleccionistas en el dispositivo, sin consultar al backend. El filtro MUST comparar el texto escrito con el nombre y con el correo, sin distinguir mayúsculas ni tildes.

#### Scenario: Filtrar por nombre
- **WHEN** el usuario escribe "bel" y hay un coleccionista llamado "Manolo Bellon"
- **THEN** la lista muestra únicamente los coleccionistas cuyo nombre o correo contiene "bel"

#### Scenario: Filtrar por correo
- **WHEN** el usuario escribe "rtvc" y un coleccionista tiene el correo "jmonsalve@rtvc.com.co"
- **THEN** la lista muestra a ese coleccionista

#### Scenario: Sin distinguir mayúsculas ni tildes
- **WHEN** el usuario escribe "JAIME" y hay un coleccionista llamado "Jaime Monsalve"
- **THEN** la lista muestra a ese coleccionista

#### Scenario: Búsqueda vacía
- **WHEN** el campo de búsqueda está vacío o solo contiene espacios
- **THEN** la lista muestra todos los coleccionistas

#### Scenario: Búsqueda sin coincidencias
- **WHEN** el usuario escribe un texto que no coincide con ningún coleccionista
- **THEN** la app muestra el mensaje "No encontramos coleccionistas con esa búsqueda"
- **AND** el campo de búsqueda sigue disponible para corregir el texto

#### Scenario: Seleccionar un resultado filtrado
- **WHEN** el usuario filtra la lista y selecciona uno de los resultados
- **THEN** la app ingresa con ese coleccionista, igual que sin filtro

#### Scenario: Sin buscador fuera de la lista cargada
- **WHEN** el listado está cargando, en error o sin coleccionistas registrados
- **THEN** no se muestra el campo de búsqueda

### Requirement: Ingreso como coleccionista
Al seleccionar un coleccionista del listado, la app SHALL llevar al usuario a la pantalla de inicio con las acciones de coleccionista habilitadas y con ese coleccionista como identidad activa.

#### Scenario: Selección de un coleccionista
- **WHEN** el usuario selecciona un coleccionista del listado
- **THEN** la app navega a la pantalla de inicio
- **AND** el rol activo es coleccionista y el coleccionista activo es el seleccionado

### Requirement: Indicador de progreso al cargar coleccionistas
Mientras se obtiene el listado de coleccionistas, la app SHALL mostrar un indicador de progreso en lugar de una pantalla en blanco.

#### Scenario: Carga en curso
- **WHEN** el usuario elige "Coleccionista" y la respuesta del backend aún no llega
- **THEN** se muestra un indicador de progreso
- **AND** no se muestra una pantalla en blanco

### Requirement: Error al cargar coleccionistas
Si el listado de coleccionistas no se puede cargar por un problema de conexión o del servidor, la app SHALL mostrar el mensaje "No pudimos cargar los coleccionistas" con un botón "Reintentar", MUST NOT cerrarse y MUST mantener disponible la opción "Visitante".

#### Scenario: Falla de conexión
- **WHEN** el usuario elige "Coleccionista" y no hay conexión con el backend
- **THEN** se muestra "No pudimos cargar los coleccionistas" con el botón "Reintentar"
- **AND** la app no se cierra
- **AND** la opción "Visitante" sigue disponible

#### Scenario: Error del servidor
- **WHEN** el backend responde con un error del servidor al pedir los coleccionistas
- **THEN** se muestra "No pudimos cargar los coleccionistas" con el botón "Reintentar"
- **AND** la app no se cierra

#### Scenario: Recuperación con Reintentar
- **WHEN** el usuario pulsa "Reintentar" tras un error y esta vez el backend responde con coleccionistas
- **THEN** se muestra primero el indicador de progreso y luego el listado de coleccionistas

#### Scenario: Reintento fallido
- **WHEN** el usuario pulsa "Reintentar" y la consulta vuelve a fallar
- **THEN** se vuelve a mostrar el mensaje de error con el botón "Reintentar"

#### Scenario: Visitante tras un error
- **WHEN** se muestra el error de carga de coleccionistas y el usuario elige ingresar como visitante
- **THEN** la app navega a la pantalla de inicio con rol de visitante

### Requirement: Sin coleccionistas registrados
Si no hay coleccionistas registrados, la app SHALL mostrar el mensaje "Aún no hay coleccionistas registrados" y la opción de volver para ingresar como visitante.

#### Scenario: Listado vacío
- **WHEN** el usuario elige "Coleccionista" y el backend devuelve una lista vacía
- **THEN** se muestra "Aún no hay coleccionistas registrados"
- **AND** se ofrece la opción de volver para ingresar como visitante

### Requirement: Regreso desde el listado de coleccionistas
Desde el listado de coleccionistas, al presionar "Atrás", la app SHALL regresar a la pantalla de ingreso sin que quede ningún coleccionista seleccionado.

#### Scenario: Atrás desde el listado
- **WHEN** el usuario está en el listado de coleccionistas y presiona "Atrás"
- **THEN** la app muestra la pantalla de ingreso
- **AND** no hay ningún coleccionista seleccionado

#### Scenario: Atrás desde un estado de error o vacío
- **WHEN** el usuario está en el estado de error o de listado vacío y presiona "Atrás"
- **THEN** la app muestra la pantalla de ingreso sin cerrarse

### Requirement: Volver al ingreso desde la pantalla de inicio
Desde la pantalla de inicio, el botón "Atrás" del sistema SHALL llevar al usuario de vuelta a la pantalla de ingreso, para que pueda cambiar de perfil. Al volver, la sesión MUST quedar sin perfil. Desde la pantalla de ingreso, "Atrás" cierra la app.

#### Scenario: Atrás desde el inicio como visitante
- **WHEN** el usuario ingresó como Visitante y presiona "Atrás" en la pantalla de inicio
- **THEN** la app muestra la pantalla de ingreso
- **AND** no hay rol ni coleccionista activos

#### Scenario: Atrás desde el inicio como coleccionista
- **WHEN** el usuario ingresó como coleccionista y presiona "Atrás" en la pantalla de inicio
- **THEN** la app muestra la pantalla de ingreso, no el listado de coleccionistas
- **AND** no hay rol ni coleccionista activos

#### Scenario: Cambiar de perfil
- **WHEN** el usuario volvió al ingreso desde la pantalla de inicio y elige otro perfil
- **THEN** la app entra a la pantalla de inicio con el nuevo perfil

#### Scenario: Atrás en la pantalla de ingreso
- **WHEN** el usuario está en la pantalla de ingreso y presiona "Atrás" del sistema
- **THEN** la app se cierra

### Requirement: Conservación del estado al rotar la pantalla
La pantalla de ingreso y el listado de coleccionistas SHALL conservar su estado al girar el dispositivo, incluido el texto del campo de búsqueda, sin repetir la consulta al backend innecesariamente.

#### Scenario: Rotación con el listado cargado
- **WHEN** el listado de coleccionistas ya se cargó y el usuario gira el dispositivo
- **THEN** el listado sigue visible con los mismos coleccionistas
- **AND** no se vuelve a consultar al backend

#### Scenario: Rotación con una búsqueda escrita
- **WHEN** el usuario escribió un texto de búsqueda y gira el dispositivo
- **THEN** el campo de búsqueda conserva el texto y la lista sigue filtrada
