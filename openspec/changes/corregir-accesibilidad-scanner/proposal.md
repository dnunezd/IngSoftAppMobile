# Proposal

## Why

Android Lint no reportó nada de accesibilidad (0 errores, 2 avisos de dependencias), pero Lint casi no revisa Compose. Se ejecutó el Accessibility Scanner de Google en un emulador API 35 con el backend local, sobre las 8 pantallas, y encontró problemas de **contraste de texto** en 7 de ellas y de **descripciones de elementos** en 3. Solo "Detalle de artista" salió limpia. Hay que corregirlos y dejar una prueba que impida que vuelvan.

## What Changes

- Subir el contraste de texto a 4,5:1 (3:1 para texto grande) en:
  - Texto blanco sobre el naranja `#FF4D26` (3,3:1): botones "Entrar", "Ver álbumes", "Ver artistas", "Crear álbum" y chips "VINILO CATALOG", "álbumes", "artistas", "álbum", "LEYENDAS & BANDAS".
  - Texto naranja sobre fondo claro: "Sound", "Beats & Sound", "VINILO CLUB" y los encabezados de Detalle de álbum ("Rock", "Salsa", "Artistas", "Canciones", "Comentarios"), junto con la calificación "5,0" y sus estrellas.
  - Texto gris: el titular "Beats," y el chip "Acceso Libre" de Ingreso.
  - Iniciales blancas sobre los avatares de Coleccionistas, el número de pista y los chips de Coleccionistas ("CURATED VINYLS", "Explore Sounds").
  - Tarjetas del catálogo.
- Corregir los elementos que el Scanner marca por descripción: la flecha del botón "Entrar", los iconos decorativos del encabezado de Coleccionistas y el encabezado "Artistas" del listado. Lo decorativo se oculta a TalkBack y lo que informa recibe su descripción.
- Agregar una prueba instrumentada de Compose con las comprobaciones de accesibilidad activadas (`enableAccessibilityChecks()`) que recorre las 8 pantallas.
- Revisar con la fuente del sistema al 200 % y corregir lo que se solapa o se recorta (alturas fijas en barras, campos y botones; el titular de Ingreso y la cabecera de Coleccionistas pasan a un flujo que crece con el texto).
- Revisar con TalkBack: el botón "Entrar" se anuncia como un solo elemento, los campos de "Crear álbum" anuncian su etiqueta y el promedio del detalle se anuncia como "4,5 de 5 estrellas".
- Ocultar a TalkBack los textos de ambientación de Ingreso y Coleccionistas (titular "Make Endless Beats…", "Beats & Sound", "AUDIO WAVE", "AUDIO FEED", "CURATED VINYLS", "Explore Sounds"), sin cambiar su aspecto.
- Comprobar el modo oscuro del sistema: la app mantiene su tema claro de marca, sin cambios.
- Sin cambios de comportamiento, navegación, datos ni textos. El ajuste de color es mínimo y conserva la identidad de la paleta del Figma.

## Capabilities

### New Capabilities
- `accesibilidad`: criterios mínimos de contraste, semántica de elementos decorativos e informativos y verificación automática, válidos para todas las pantallas.

### Modified Capabilities

Ninguna. Los requisitos funcionales de `ingreso-plataforma`, `pantalla-inicio` y `catalogo-albumes` no cambian; solo cambian colores y semántica.

## Impact

- **Tema**: `ui/theme/Color.kt` (nuevos tokens de contraste) y los usos de `Naranja`, `TitularGris`, `ChipTexto` y `Avatar*` en `ui/ingreso`, `ui/inicio`, `ui/catalogo`, `ui/detalle`, `ui/artistas` y `ui/crear`.
- **Pruebas**: nueva prueba instrumentada de accesibilidad y una unitaria de la razón de contraste de la paleta. Las E2E existentes buscan por texto y `testTag`, así que no deberían cambiar.
- **Dependencias**: ninguna (`ui-test-junit4` ya está).
- **Evidencia**: reporte del Scanner antes y después en la página [Sprint 3 · Accesibilidad](https://github.com/dnunezd/IngSoftAppMobile/wiki/Sprint-3-Accesibilidad) de la wiki.
- **Fuera de alcance**: un tema oscuro propio, un recorrido de TalkBack con voz real en un dispositivo físico, la verbosidad de las tarjetas del catálogo y un rediseño del Figma.
