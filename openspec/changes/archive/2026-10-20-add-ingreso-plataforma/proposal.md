# Proposal

## Why

La app Vinilos hoy es solo el esqueleto de Android Studio ("Hello Android!"): no tiene pantallas, navegación, capa de red ni noción de quién la usa. La HU00 (Ingreso a la plataforma) es la puerta de entrada de la que dependen todas las demás historias: el catálogo (HU01) debe mostrarse tanto al Visitante como al Coleccionista, y las acciones del coleccionista (HU07-HU10, HU15) solo pueden habilitarse si la app sabe quién ingresó. Es la historia de mayor prioridad del Sprint 1 y desbloquea el trabajo en paralelo del resto del equipo.

## What Changes

- Nueva pantalla de ingreso, mostrada al abrir la app, con dos opciones claramente identificadas: "Visitante" y "Coleccionista", presentadas como tarjetas con su botón "Entrar", según el mockup, y con la franja decorativa "AUDIO WAVE · 94 BPM" bajo el subtítulo.
- "Visitante" entra sin pedir datos adicionales, directamente a la pantalla de inicio.
- "Coleccionista" abre un selector ("¿Quién eres?") que consulta `GET /collectors` al backend BackVynils y lista los coleccionistas con avatar de iniciales, nombre y correo; al elegir uno se entra a la pantalla de inicio con el rol de coleccionista.
- Campo de búsqueda "Buscar coleccionista..." que filtra la lista en el dispositivo por nombre o correo (está en el mockup aunque los criterios de la HU00 no lo mencionan; el equipo decidió incluirlo).
- El selector maneja los estados de carga (indicador de progreso), error ("No pudimos cargar los coleccionistas" con "Reintentar", la opción "Visitante" sigue disponible) y vacío ("Aún no hay coleccionistas registrados" con la opción de volver), y "Atrás" regresa al ingreso sin dejar ningún coleccionista seleccionado.
- Nueva pantalla de inicio (la de "¡Hola, melómano!" del Figma, marco `Html → Body`) a la que llegan ambos roles tras ingresar, con dos tarjetas: "Catálogo de Álbumes" (botón "Ver álbumes") y "Listado de Artistas" (botón "Ver artistas"). Los destinos son provisionales hasta que HU01 y HU03 los implementen. Se agregó por decisión del equipo y **cambia el criterio de la HU00** "entra directamente al catálogo": ahora entra a esta pantalla primero.
- El saludo de la pantalla de inicio depende del perfil: "¡Hola, melómano!" para el Visitante y "¡Hola, <nombre>!" para el coleccionista que ingresó.
- El selector de coleccionistas lleva, bajo el titular, la franja decorativa "AUDIO FEED" con una onda de barras a cada lado (del mockup).
- Las corcheas y demás decoraciones del mockup (iconos de música, chispas junto a "Make", estrella del chip) pasan de glifos de texto a gráficos vectoriales: todos se convierten de los SVG del Figma (ingreso, selector e inicio). La pantalla de ingreso también se ajusta al componente del Figma (chips, franja AUDIO WAVE, estrella "sticker").
- "Atrás" del sistema desde la pantalla de inicio regresa al ingreso para poder cambiar de perfil; desde el catálogo y los artistas (flecha del encabezado o sistema) regresa a la pantalla de inicio.
- Nueva sesión compartida que conserva el rol (visitante o coleccionista) y el coleccionista seleccionado, y permite a las demás pantallas mostrar u ocultar las acciones exclusivas del coleccionista (crear álbum, agregar tracks, comentar, agregar favoritos).
- Base técnica mínima para poder construir lo anterior, según la arquitectura definida en la wiki (MVVM + Repository + Retrofit): dependencias de red, navegación y `ViewModel` para Compose, permiso de Internet y estructura de paquetes bajo `com.example.vinilos`.

No hay cambios incompatibles (**BREAKING**): el proyecto no tiene código de producto previo.

## Capabilities

### New Capabilities
- `ingreso-plataforma`: pantalla de ingreso y flujo de elección de perfil (Visitante o Coleccionista), incluida la consulta y presentación del listado de coleccionistas con sus estados de carga, vacío y error, y la navegación "Atrás".
- `sesion-usuario`: estado compartido de sesión con el rol activo y el coleccionista seleccionado, y la regla de visibilidad de las acciones exclusivas del coleccionista según el rol.
- `pantalla-inicio`: pantalla a la que llega el usuario tras ingresar, con las tarjetas que llevan al catálogo de álbumes y al listado de artistas, y la navegación de vuelta.

### Modified Capabilities
<!-- Ninguna: openspec/specs/ está vacío. -->

## Impact

- **Diseño visual**: el selector de coleccionistas está en el Figma "Vinilos App" (marco `Coleccionistas Disponibles`, de donde salen colores, tipografía y logo para el tema). La pantalla de ingreso (la de "Make Endless Beats") está en ese mismo archivo como el componente `Html → Body → Main` (`125:460`), con sus SVG decorativos; sus dos ilustraciones las entregó además el equipo como archivos. El marco `Html → Body` es la pantalla de inicio ("Secciones"), que sí es parte de este cambio y de la que se exportaron las ilustraciones de las tarjetas. No hay marcos de Figma para los estados de carga, error y vacío de la HU00; se siguen los textos de los criterios de aceptación.
- **Código** (`Vinilos/app`): nuevos paquetes `ui.ingreso`, `viewmodel`, `data.repository`, `data.remote`, `data.model`, `navigation`; `MainActivity` pasa de la plantilla a alojar el grafo de navegación.
- **Dependencias** (`gradle/libs.versions.toml`, `app/build.gradle.kts`): Retrofit con conversor JSON, OkHttp, Navigation Compose, `lifecycle-viewmodel-compose`, corrutinas, y librerías de prueba para unitarias (corrutinas de prueba, mock de red).
- **Manifest**: permiso `INTERNET` y acceso HTTP sin cifrar hacia el backend local de desarrollo (`http://localhost:3000`, que el emulador alcanza como `10.0.2.2`).
- **API consumida**: `GET /collectors` del backend BackVynils (solo `id`, `name` y `email`; el resto de `CollectorDetail` se ignora).
- **Dependencias entre historias**: HU01 (T01-4) y HU03 (T03-4) integran el catálogo y el listado de artistas como destinos de las tarjetas de la pantalla de inicio (ya no del ingreso directamente); hasta entonces los destinos son marcadores provisionales. La wiki de HU00 (criterios de aceptación) y las tareas T01-4 y T03-4 deben reflejar este flujo. HU07-HU10 y HU15 consumen la sesión para habilitar sus acciones.
- **Fuera de alcance**: autenticación o contraseñas (el ingreso es solo elegir perfil), persistencia de la sesión entre cierres de la app, creación de coleccionistas, el catálogo en sí (HU01) y las acciones del coleccionista. Del mockup no se replican la insignia de versión "v1.0.4" (no coincide con la versión real de la app) ni el ícono de perfil del encabezado, cuya función no está definida. La franja "AUDIO WAVE · 94 BPM" sí se incluye como elemento decorativo estático, por decisión del equipo. La actualización de la wiki y los mockups (T00-6) vive en el repositorio de la wiki y se gestiona allí.
