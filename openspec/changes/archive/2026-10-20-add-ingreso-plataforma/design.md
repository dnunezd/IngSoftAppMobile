# Design

## Context

Ver `proposal.md` (Why) para la motivación. Estado inicial del proyecto Android: plantilla (Kotlin 1.9.0, AGP 8.6.0, Compose BOM 2024.04.01, compilador de Compose 1.5.1, `minSdk` 26, `compileSdk`/`targetSdk` 34) con una sola `MainActivity` que muestra "Hello Android!". No hay navegación, red, `ViewModel` ni inyección de dependencias, y el manifest no declara `INTERNET`.

Restricciones que vienen de la wiki del equipo (Sprint 1):
- Arquitectura **MVVM + Repository + Retrofit** sobre `com.example.vinilos`, con paquetes `ui`, `viewmodel`, `data.repository`, `data.remote`, `data.model` y `navigation`. Cada capa tiene responsabilidades acotadas (la vista no llama a la API; el `ViewModel` no conoce Compose; el repositorio traduce errores).
- El `ViewModel` debe sobrevivir a la rotación y exponer un `UiState` con `Cargando`, `Éxito`, `Vacío` y `Error`. Los métodos del repositorio se nombran en español (`obtenerAlbumes()` en el diagrama de la wiki), y las clases del dominio en inglés (`Collector`, `CollectorRepository`).
- La metodología acordada (Home) es Gitflow y **TDD**: las pruebas de cada tarea se escriben antes que la implementación.
- La tarea T00-4 de la wiki asigna la sesión a un "ViewModel compartido y Repository de coleccionistas": el `ViewModel` compartido es el de la sesión (D4) y el repositorio es el de la consulta (D3).
- Las pruebas unitarias se concentran en `Repository` y `ViewModel`; las E2E corren contra el backend real BackVynils (`http://localhost:3000`).
- HU01, HU02 y HU03 las implementan otros integrantes en paralelo sobre la misma base, por lo que la estructura de módulos y rutas que cree este cambio es compartida.
- `GET /collectors` devuelve `CollectorDetail` (incluye `comments`, `favoritePerformers`, `collectorAlbums`); para el ingreso solo hacen falta `id`, `name` y `email`.

## Goals / Non-Goals

**Goals:**
- Dejar la base técnica común (red, navegación, `ViewModel`, estructura de paquetes) para que HU01-HU03 se construyan encima sin rehacerla.
- Implementar el ingreso y la sesión de modo que el cambio de perfil, la rotación y los fallos de red se comporten como piden las specs.
- Que la lógica de ingreso y sesión sea probable con pruebas unitarias sin emulador.

**Non-Goals:**
- Autenticación, persistencia de la sesión entre cierres de la app, o selección de perfil recordada.
- El catálogo real (HU01): aquí solo existe un destino provisional.
- Un framework de inyección de dependencias, caché o paginación de coleccionistas.
- El ícono de perfil del encabezado y la insignia de versión del mockup (ver D6c). La pantalla "Secciones" del Figma sí se implementa como pantalla de inicio (D6d), a pesar de que la HU00 decía que se entraba directamente al catálogo.
- Un indicador visible del rol en el encabezado del catálogo (el mockup muestra "VISITANTE" o "COLECCIONISTA"): lo implementa HU01, que lee el rol desde la sesión (D4). El destino provisional `catalogo` de este cambio lo muestra como texto simple.

## Decisions

### D1. Cinco destinos de navegación: ingreso, coleccionistas, inicio, catálogo y artistas
Se usa Navigation Compose con las rutas `ingreso` (destino de arranque), `coleccionistas`, `inicio`, `catalogo` y `artistas`. El listado de coleccionistas es su propio destino y no un estado interno de la pantalla de ingreso. Flujo: `ingreso` -> (`coleccionistas`) -> `inicio` -> `catalogo` | `artistas`.

- *Por qué*: así "Atrás" (botón del sistema y flecha de la barra) regresa a `ingreso` por el comportamiento estándar de la pila, sin `BackHandler` manual, y el `ViewModel` del listado queda atado a ese destino: sobrevive a la rotación y se descarta al salir (al volver a entrar se consulta de nuevo).
- *Alternativa*: un solo composable con un estado "mostrando coleccionistas" + `BackHandler`. Descartada: mezcla dos pantallas en un `ViewModel` y complica "Atrás" y la rotación.
- Al navegar a `inicio` (desde el ingreso o desde el selector) se hace `popUpTo(ingreso)` con `inclusive = false`: la pantalla de ingreso queda en la pila y `coleccionistas` sale de ella. Así "Atrás" desde el inicio regresa al ingreso, no al selector, y el usuario puede cambiar de perfil. La pantalla de ingreso limpia la sesión al mostrarse, por lo que el cambio de perfil queda cubierto. Desde el ingreso, "Atrás" cierra la app.
- Desde `inicio`, "Ver álbumes" y "Ver artistas" hacen un `navigate` normal a `catalogo` y `artistas`; "Atrás" (flecha del encabezado o sistema) en esos destinos regresa a `inicio` sin tocar la sesión. El encabezado del inicio no tiene flecha (el mockup no la trae): se sale con "Atrás" del sistema.
- *Alternativa*: sacar `ingreso` de la pila al entrar al catálogo (`inclusive = true`). Descartada: no hay forma de cambiar de perfil sin reiniciar la app.
- `catalogo` y `artistas` son composables provisionales (`ui.catalogo` y `ui.artistas`) que muestran el título y el rol activo; HU01 (T01-4) y HU03 (T03-4) los reemplazan sin tocar el grafo.

### D2. Estado de la pantalla de coleccionistas como `UiState` sellado
`CollectorsViewModel` expone `StateFlow<CollectorsUiState>` con `Cargando`, `Exito(lista)`, `Vacio` y `Error` (los cuatro estados de la arquitectura de la wiki). Carga al crearse (en `init`) y recarga en `reintentar()`.

- *Por qué*: coincide con la arquitectura de la wiki, es trivial de probar y la vista solo hace `when` sobre el estado.
- `Vacio` se distingue de `Exito` en el `ViewModel` (lista vacía → `Vacio`), no en la vista.
- Un reintento vuelve a emitir `Cargando` antes del resultado y descarta una carga anterior en curso, evitando que una respuesta tardía pise a una nueva.

### D3. Repositorio devuelve `Result` con errores ya traducidos
`CollectorRepository.obtenerColeccionistas()` devuelve `Result<List<Collector>>`. Convierte `CollectorDto(id, name, email)` en el modelo `Collector` y traduce `IOException` (sin conexión, tiempo de espera) y `HttpException` (4xx/5xx) en un único error de carga que el `ViewModel` entiende; `CancellationException` se relanza para no romper la cancelación estructurada.

- *Por qué*: la vista muestra el mismo mensaje para red y servidor (criterio de la HU), por lo que el `ViewModel` no necesita distinguir causas; mantener la distinción en el tipo de error deja espacio para mensajes más específicos sin cambiar la capa de datos.
- *Alternativa*: lanzar excepciones hasta el `ViewModel`. Descartada: la wiki asigna al repositorio la traducción de errores.
- El DTO declara solo `id`, `name` y `email`; el resto de `CollectorDetail` se ignora al deserializar. Con Gson, un campo faltante llega como `null`, por lo que el mapeo descarta o rellena de forma segura los elementos sin `id`.

### D4. Sesión: un `SessionViewModel` compartido con `Session` sellado
`Session` es un tipo sellado: `None` (aún sin perfil), `Visitor` y `Collector(collector)`. `SessionViewModel` está atado a la `Activity` (no a un destino), expone `StateFlow<Session>` y funciones `enterAsVisitor()`, `enterAsCollector(collector)` y `clear()`. La capacidad "acciones de coleccionista habilitadas" se deriva (`session is Session.Collector`) en un solo lugar para que todas las historias usen la misma regla.

- *Por qué*: la wiki pide "ViewModel compartido"; atarlo a la `Activity` lo hace visible desde cualquier destino y sobrevive a la rotación.
- *Alternativas*: un singleton en el contenedor de la app (sobrevive a más cosas pero se vuelve estado global difícil de probar) o pasar el rol como argumento de navegación (obliga a cada historia a propagarlo y no sirve para el coleccionista completo).
- La sesión se asigna solo al **pulsar** una opción final (Visitante o un coleccionista), nunca al abrir el listado: así "Atrás" deja sin sesión por construcción. La pantalla de ingreso llama a `clear()` al mostrarse, de modo que regresar a ella restablece el estado.
- No se persiste: al cerrar la app el usuario vuelve a elegir perfil (coherente con el alcance y con que no hay autenticación).

### D5. Inyección de dependencias manual
Un contenedor simple (`AppContainer`) creado en una `Application` expone el cliente Retrofit y los repositorios; los `ViewModel` se crean con `viewModelFactory`/`initializer` que lo consultan.

- *Por qué*: con tres clases de datos en este cambio, Hilt (kapt/KSP, plugin y anotaciones) agrega más configuración y riesgo de incompatibilidad con Kotlin 1.9.0 que valor. Los repositorios reciben su API por constructor, así que migrar a Hilt más adelante es mecánico.
- *Alternativa*: Hilt/Koin. Descartada por ahora por costo de configuración compartido por todo el equipo.

### D6. Cliente de red
Retrofit con conversor Gson sobre OkHttp, con tiempos de espera de conexión y lectura de 10 s, para que un backend inalcanzable muestre el error en un tiempo razonable en lugar de dejar el indicador de progreso indefinido. La URL base sale de un `buildConfigField` (`http://10.0.2.2:3000/`, la forma en que el emulador ve el `localhost` del anfitrión). El manifest agrega `INTERNET` y una configuración de seguridad de red que permite HTTP sin cifrar solo hacia `10.0.2.2` y `localhost`.

- *Por qué*: el backend de desarrollo es HTTP en el puerto 3000; limitar el tráfico sin cifrar a esos hosts evita un `usesCleartextTraffic="true"` global.
- *Alternativas*: Moshi/kotlinx.serialization (más estrictos con nulos, pero requieren plugin o codegen adicional); se prefiere Gson por simplicidad en esta etapa.
- Versiones propuestas, compatibles con el toolchain actual (Kotlin 1.9.0, `compileSdk` 34): Retrofit 2.9.0, OkHttp 4.12.0, Navigation Compose 2.7.7, `lifecycle-viewmodel-compose` 2.7.0 (igual que `lifecycle-runtime-ktx`), corrutinas 1.8.0; para pruebas, `kotlinx-coroutines-test` y `MockWebServer` 4.12.0.

### D6b. Búsqueda de coleccionistas en el `ViewModel`
`CollectorsViewModel` guarda el texto de búsqueda en un `StateFlow` y combina la lista completa con ese texto para exponer la lista visible. El filtro normaliza ambos textos (minúsculas y sin tildes, con `Normalizer` en forma NFD) y busca el texto como subcadena en el nombre y en el correo; un texto vacío o solo de espacios devuelve todos. La búsqueda no consulta al backend. El campo solo se muestra en el estado `Exito`. El texto vive en el `ViewModel`, por lo que sobrevive a la rotación.

- *Por qué en el `ViewModel`*: es lógica probable sin emulador y conserva el texto al rotar; la vista solo pinta.
- *Alternativa*: filtrar en el composable con `remember`. Descartada: pierde el texto si se recrea la composición y no se puede probar con pruebas unitarias.
- Cuando el filtro no devuelve coincidencias, la vista muestra "No encontramos coleccionistas con esa búsqueda" y mantiene el campo visible. Ese texto no viene de la wiki ni del Figma, lo propuse yo.

### D6c. Tema y recursos tomados del Figma
Los colores y la tipografía se toman del marco `Coleccionistas Disponibles` del archivo de Figma "Vinilos App" y se aplican en `ui.theme` (hoy es la plantilla de Android Studio). Se leen valores exactos con `get_design_context`, no se estiman de las capturas: naranja `#FF4D26`, fondo `#FCF9F8`, texto `#1C1B1B` y `#5C5B5E`, naranja oscuro `#AF2900`, tipografía Inter. El tema es claro y de marca: se retira el color dinámico y el esquema oscuro de la plantilla porque el mockup solo existe en claro. Inter no se empaqueta todavía (ver Open Questions): se usa la fuente sans-serif del sistema con los mismos pesos, tamaños e interlineados. El logo se exporta de Figma como recurso de la app. La pantalla de ingreso es el componente `Html → Body → Main` del Figma; sus dos ilustraciones las entregó el equipo como archivos. La pantalla de inicio es el marco `Html → Body`. Se replican el titular, las etiquetas "Acceso Libre" y "PRO", las tarjetas con "Entrar" y el avatar de iniciales; no se replican la insignia "v1.0.4" ni el ícono de perfil del encabezado (ver Non-Goals). La franja "AUDIO WAVE · 94 BPM" sí se replica, por decisión del equipo, como elemento decorativo estático: un composable propio con los textos en `strings.xml` y la onda del SVG del Figma convertido a `VectorDrawable` (picos de trazo `#FCA58F` recortados a 200×24), dentro de un contenedor de esquinas de 16dp con fondo blanco translúcido y borde tenue. No tiene estado, ni datos reales, ni interacción; "94 BPM" es texto fijo del mockup.

### D6d. Pantalla de inicio (secciones)
`PantallaInicio` (`ui.inicio`) es un composable sin estado ni `ViewModel`: recibe `onVerAlbumes` y `onVerArtistas`. Replica el marco `Html → Body` del Figma: encabezado con el logo y "Vinilo", chip "VINILO CLUB", saludo, titular, subtítulo y dos tarjetas con etiqueta, ícono circular, título, descripción, ilustración y botón. Las ilustraciones y los íconos circulares se exportaron del Figma como PNG (`inicio_albumes.png`, `inicio_artistas.png`, `inicio_icono_albumes.png`, `inicio_icono_artistas.png`); las ilustraciones traen sus textos incrustados ("LP EDITION", "EN VIVO", "Estudio"). Como en el resto del cambio, no se replican el ícono de búsqueda ni el de perfil del encabezado, cuya función no está definida.

- *Por qué sin estado*: no consulta datos ni depende del rol (es igual para ambos), así que no necesita `ViewModel` y se prueba con pruebas de UI directas.
- *Alternativa*: dibujar las ilustraciones con Compose (formas y degradados). Descartada: los micrófonos y discos del Figma se componen de muchos trazos y el PNG exportado es fiel al diseño con 2 archivos pequeños.

### D6e. Saludo, franja AUDIO FEED y decoraciones vectoriales
- **Saludo:** `PantallaInicio` recibe la `Session` y elige el texto: `Session.Collector` -> "¡Hola, <nombre>!" con `collector.name` completo (el campo `name` del backend, por ejemplo "Manolo Bellon"); `Visitor` y `None` -> "¡Hola, melómano!". Sigue sin `ViewModel`: el grafo le pasa la sesión.
- **AUDIO FEED:** `FranjaAudioFeed` (en `ui.ingreso`) replica la franja del marco `Coleccionistas Disponibles`: dos grupos de barras redondeadas de 4dp (14 a la izquierda, 10 a la derecha alineadas al final, con las alturas y opacidades del Figma) y el texto en medio. Es estática, igual que la franja AUDIO WAVE.
- **Corcheas y doodles:** en el Figma las corcheas son SVG (íconos `music_note` de Material, el ícono de estrella en círculo, la onda de barras del pill "Explore Sounds", la estrella del rombo naranja), mientras que las notas ♪ ♫ 𝄞 de los racimos son texto. Los SVG se convierten a `VectorDrawable` en `res/drawable` (las rutas SVG se copian tal cual a `pathData`) y conservan el color del diseño. Los glifos de texto del Figma se mantienen como texto. El ingreso es el componente `Html → Body → Main`: de ahí salen la chispa sobre "Make" (`SVG - Doodle curved accent burst lines above title`), la corchea junto a "Beats", las corcheas unidas, la corchea tenue, la estrella "sticker", los íconos de las etiquetas y la onda del AUDIO WAVE. La conversión la hace un script que copia las rutas SVG a `pathData`.

- *Alternativa*: seguir con glifos de texto (`♪`, `♫`). Descartada: dependen de la fuente del dispositivo y no coinciden con las formas del diseño.

### D7. Textos y accesibilidad de la pantalla
Los textos ("Visitante", "Coleccionista", "No pudimos cargar los coleccionistas", "Reintentar", "Aún no hay coleccionistas registrados") viven en `strings.xml` y se usan idénticos a los de los criterios de aceptación; los elementos interactivos reciben un `testTag` estable para las pruebas E2E con Compose. El orden de los coleccionistas es el que entrega el backend (la HU no pide uno).

### D8. Estrategia de pruebas
- `CollectorRepository`: pruebas unitarias con `MockWebServer` (200 con datos, 200 vacío, 500, desconexión/tiempo de espera, campos extra ignorados).
- `CollectorsViewModel` y `SessionViewModel`: pruebas unitarias con un repositorio falso y `kotlinx-coroutines-test` (transiciones `Cargando → Exito/Vacio/Error`, `reintentar`, sesión según perfil, `clear`). Siguiendo TDD, cada prueba se escribe primero y falla antes de implementar.
- E2E (T00-5): pruebas instrumentadas con Compose UI Test contra el backend real levantado en la máquina anfitriona: ingreso como Visitante, como Coleccionista, error con backend caído y recuperación con "Reintentar", y "Atrás".

## Risks / Trade-offs

- **[HU01-HU03 se integran sobre una base que este cambio crea]** → Fijar pronto las rutas, el paquete `navigation` y las dependencias y comunicarlo al equipo; el destino `catalogo` es provisional y su reemplazo no toca el grafo.
- **[Pérdida de sesión por muerte del proceso]**: Android puede restaurar el destino activo sin el `SessionViewModel` → La sesión inicial es `None`, y las acciones de coleccionista requieren explícitamente `Collector`, por lo que el fallo es seguro (se ocultan). Queda como mejora futura volver a `ingreso` si falta sesión.
- **[Tráfico HTTP sin cifrar]** → Acotado a `10.0.2.2` y `localhost` en el manifest; un backend desplegado con HTTPS requeriría cambiar la URL base y quitar la excepción.
- **[Un dispositivo físico no alcanza `10.0.2.2`]** → La URL base se define en `buildConfigField`, así que se puede cambiar por la IP de la máquina del backend sin tocar el código.
- **[Gson y valores nulos]**: un campo ausente llega como `null` aunque el tipo Kotlin no lo permita → El mapeo en el repositorio valida `id`/`name`/`email` y descarta elementos inválidos en lugar de fallar al mostrarlos.
- **[Sin mockups para carga, error y vacío de la HU00]**: el Figma solo tiene los marcos del ingreso y del selector con datos → Se usan los textos de los criterios y el estilo del resto de pantallas; esos estados se documentan en los mockups al hacer T00-6.
- **[El buscador y su mensaje de sin resultados no están en los criterios de la HU00]** → Quedan especificados en `ingreso-plataforma` para poder probarlos; si el equipo los quita, se eliminan ese requisito y las tareas 4.2 y 4.4 sin afectar el resto.
- **[Respuestas tardías]** → Reintentos cancelan la carga anterior (D2) y los tiempos de espera acotan la espera máxima.

## Open Questions

- ¿Se empaqueta la tipografía Inter del mockup? Requiere agregar los TTF a `res/font` (descargados de Google Fonts, ~1 MB) o usar fuentes descargables de Google con sus certificados. Mientras tanto se usa la fuente del sistema; no cambia el comportamiento ni las specs.
- ~~Las dos ilustraciones de las tarjetas del ingreso no estaban disponibles.~~ Resuelto: el equipo las entregó (512×512, ~90 KB cada una) y viven en `res/drawable-nodpi` como `ingreso_visitante.jpg` e `ingreso_coleccionista.jpg`. Se recortan para llenar la tarjeta: la del visitante con un sesgo hacia arriba y la del coleccionista desde arriba, para que se vean las caras.

- ~~¿El equipo probará en dispositivos físicos además del emulador?~~ Resuelto al implementar: las pruebas se hacen en un Samsung físico. La URL base por defecto sigue siendo `http://10.0.2.2:3000/` (emulador) y se sobrescribe con la propiedad de Gradle `-PbaseUrl=http://localhost:3000/`; el dispositivo alcanza el backend del PC con `adb reverse tcp:3000 tcp:3000`, y `localhost` ya está permitido en la configuración de seguridad de red.
