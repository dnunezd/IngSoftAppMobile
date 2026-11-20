# Spec Delta

## Purpose
Definir los criterios mínimos de accesibilidad visual y de lectores de pantalla que cumplen todas las pantallas de la app, y la forma de verificarlos.

## ADDED Requirements

### Requirement: Contraste de texto
Todo texto de la app SHALL tener una razón de contraste de al menos 4,5:1 con su fondo, o de 3:1 si es texto grande (18 sp o más, o 14 sp o más en negrita). Esto incluye el texto sobre botones, chips, tarjetas y avatares. El Accessibility Scanner MUST NOT reportar "Text contrast" en ninguna pantalla.

#### Scenario: Botón con texto sobre color de marca
- **WHEN** se muestra un botón o chip con el color de marca y texto claro
- **THEN** la razón de contraste entre el texto y el relleno es de al menos 4,5:1

#### Scenario: Texto de color de marca sobre fondo claro
- **WHEN** se muestra un encabezado, etiqueta o calificación con el color de marca sobre el fondo de la pantalla
- **THEN** la razón de contraste es de al menos 4,5:1, o de 3:1 si es texto grande

#### Scenario: Escaneo de todas las pantallas
- **WHEN** se ejecuta el Accessibility Scanner sobre Ingreso, Coleccionistas, Inicio, Catálogo, Detalle de álbum, Artistas, Detalle de artista y Crear álbum
- **THEN** no reporta sugerencias de contraste de texto

### Requirement: Elementos decorativos e informativos
Los iconos e imágenes puramente decorativos SHALL ocultarse a los lectores de pantalla, y los que informan o accionan MUST tener una descripción en español. Un botón con texto y un icono acompañante MUST leerse como un solo elemento, sin repetir el icono.

#### Scenario: Icono decorativo
- **WHEN** un icono solo adorna un texto o botón que ya lo describe
- **THEN** TalkBack no lo anuncia

#### Scenario: Botón con texto e icono
- **WHEN** TalkBack enfoca el botón "Entrar"
- **THEN** lo anuncia una sola vez, con su texto
- **AND** el Scanner no reporta "Item descriptions" sobre su icono

#### Scenario: Encabezado de pantalla
- **WHEN** TalkBack recorre una pantalla
- **THEN** cada título de pantalla y de sección se anuncia como encabezado, con su texto visible
- **AND** un titular partido en varios textos ("Nuevo" y "álbum") se anuncia como un solo encabezado

### Requirement: Adornos de texto omitidos por TalkBack
Los textos de ambientación SHALL ocultarse a los lectores de pantalla sin cambiar su aspecto: el titular "Make Endless Beats, Sound Amazing", la etiqueta "Beats & Sound", "AUDIO WAVE" con su "94 BPM", "CURATED VINYLS", "Explore Sounds" y "AUDIO FEED".

#### Scenario: Recorrido con TalkBack
- **WHEN** TalkBack recorre Ingreso o Coleccionistas
- **THEN** no anuncia ninguno de esos textos
- **AND** siguen visibles en pantalla

### Requirement: Texto ampliado
Con la fuente del sistema al 200 %, ningún texto SHALL solaparse con otro ni quedar recortado por una altura fija. Las barras, campos y botones MUST crecer con su contenido, y el titular de Ingreso y la cabecera de Coleccionistas MUST pasar a un flujo vertical cuando la escala de fuente supere 1,15.

#### Scenario: Barra superior con fuente ampliada
- **WHEN** la fuente del sistema está al 200 % y se abre Catálogo o Crear álbum
- **THEN** la etiqueta del rol y el título se ven completos

#### Scenario: Campo con texto de ejemplo largo
- **WHEN** la fuente está al 200 % y se muestra "Selecciona un género"
- **THEN** el texto pasa a dos líneas y el campo crece, sin recortarse

### Requirement: Modo oscuro del sistema
La app SHALL mantener su tema claro de marca cuando el sistema está en modo oscuro, sin perder legibilidad ni contraste.

#### Scenario: Sistema en modo oscuro
- **WHEN** el sistema está en modo oscuro y se abre cualquier pantalla
- **THEN** la app se muestra con la paleta clara y cumple los mismos contrastes

### Requirement: Campos y valores anunciados
Cada campo del formulario "Crear álbum" MUST anunciar su etiqueta ("Nombre del álbum", "Portada (URL)", "Descripción"), y los campos de fecha y de lista MUST anunciar la etiqueta junto con el valor actual. Una calificación mostrada con estrellas MUST anunciarse con su valor numérico ("4,5 de 5 estrellas") y no como símbolos.

#### Scenario: Campo vacío del formulario
- **WHEN** TalkBack enfoca el campo "Nombre del álbum" vacío
- **THEN** anuncia "Nombre del álbum" y no solo el texto de ejemplo

#### Scenario: Campo de lista con valor
- **WHEN** TalkBack enfoca "Género" con "Rock" elegido
- **THEN** se detiene en un solo elemento con rol de lista y anuncia "Género, Rock", sin un segundo elemento con el texto de adentro

### Requirement: Verificación automática de accesibilidad
El proyecto SHALL incluir una prueba unitaria del contraste de la paleta y una prueba instrumentada que active las comprobaciones de accesibilidad de Compose y recorra las 8 pantallas con la fuente normal y al 200 %. El contraste MUST verificarse con `ContrasteDePaletaTest`, sobre los pares texto/fondo de la paleta, y con el Accessibility Scanner a mano, porque las comprobaciones de Compose no evalúan contraste. La prueba instrumentada MUST fallar si una pantalla vuelve a tener un elemento sin etiqueta, un destino táctil menor al mínimo, elementos duplicados, o un título que deje de ser encabezado.

#### Scenario: Regresión de contraste
- **WHEN** alguien cambia un color de la paleta y deja un par texto/fondo por debajo del contraste exigido
- **THEN** `ContrasteDePaletaTest` falla e indica el par

#### Scenario: Regresión de semántica
- **WHEN** un título de pantalla o de sección deja de marcarse como encabezado, o un campo de lista vuelve a anunciar solo su valor
- **THEN** `AccesibilidadPantallasTest` falla e indica la pantalla
