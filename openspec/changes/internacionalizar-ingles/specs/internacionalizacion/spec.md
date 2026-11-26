# Spec Delta

## ADDED Requirements

### Requirement: Idiomas de la app
La app SHALL estar disponible en español y en inglés, y el español MUST ser el idioma por defecto. Todo texto que la app muestra o anuncia por accesibilidad (etiquetas, mensajes, descripciones y plurales) MUST existir en ambos idiomas, con los mismos parámetros.

#### Scenario: Dispositivo en inglés
- **WHEN** el dispositivo está en inglés y se abre la app
- **THEN** los textos de la app y sus descripciones para TalkBack se muestran en inglés

#### Scenario: Dispositivo en otro idioma
- **WHEN** el dispositivo está en un idioma distinto del español y del inglés
- **THEN** la app se muestra en español

#### Scenario: Traducción incompleta
- **WHEN** se agrega un texto en español y no en inglés, o con parámetros distintos
- **THEN** una prueba automática falla e indica el texto

### Requirement: Elección del idioma
La app SHALL seguir el idioma del dispositivo. En Android 13 o superior, el usuario MUST poder elegir el idioma de la app desde los ajustes del sistema, entre español e inglés.

#### Scenario: Idioma por aplicación
- **WHEN** en Android 13 o superior el usuario elige inglés para Vinilos en los ajustes del sistema
- **THEN** la app se muestra en inglés, aunque el dispositivo esté en español

### Requirement: Formatos según el idioma
Las fechas largas, los promedios y el selector de fecha SHALL usar el idioma en que la app muestra sus textos, no un idioma fijo.

#### Scenario: Fecha y promedio en inglés
- **WHEN** la app está en inglés y se abre el detalle de un álbum con promedio 4,5 y fecha 1 de agosto de 1984
- **THEN** se muestran "4.5" y "August 1, 1984"

#### Scenario: Selector de fecha
- **WHEN** se abre el selector de fecha de "Crear álbum"
- **THEN** sus meses, días y botones están en el idioma de la app

### Requirement: Contenido que no se traduce
Los datos que vienen del backend (nombres, descripciones, géneros, disqueras y comentarios) y los textos de ambientación que el diseño tiene en inglés SHALL mostrarse igual en ambos idiomas. Los valores de género y disquera que se envían al crear un álbum MUST ser los que espera el backend, sin importar el idioma.

#### Scenario: Crear un álbum en inglés
- **WHEN** la app está en inglés y el coleccionista crea un álbum de género "Rock"
- **THEN** el backend recibe el mismo valor de género que en español

### Requirement: Pruebas independientes del idioma del dispositivo
Las pruebas instrumentadas SHALL ejecutarse en español aunque el dispositivo esté en inglés, en Android 13 o superior. Además, una prueba MUST mostrar pantallas en inglés y repetir sobre ellas las comprobaciones de accesibilidad.

#### Scenario: Emulador en inglés
- **WHEN** se corre la suite instrumentada en un emulador API 35 con el sistema en inglés
- **THEN** las pruebas que buscan textos en español pasan
