# Informe de inspecciones de código (Inspect Code)

## Origen

Export completo de **"Inspect Code"** de IntelliJ/Android Studio sobre todo el
proyecto (perfil `Project Default`), no solo Android Lint: incluye inspecciones
de Kotlin, Java, Gradle, Markdown y ortografía para todos los módulos y variantes.
El archivo exportado (`index.html`) ocupa **1.3 GB** y reporta **33,263 ítems** en
total.

## Resumen ejecutivo

| Categoría | Ítems | Tratamiento |
|---|---|---|
| **Spelling** (ortografía) | 31,643 | Ruido: el diccionario en inglés marca como error casi todo el texto en español (nombres de variables, strings, comentarios). No se desglosa. |
| **Composable function call without Compose plugin** | 1,349 | Falso positivo: el perfil `Project Default` de IntelliJ no reconoce el plugin de Compose configurado en el proyecto para este tipo de análisis general. No representa 1,349 errores reales de Compose. Recomendación: volver a correr el análisis con el perfil "Android Lint" (o desde Android Studio con el contexto del proyecto cargado) para confirmar. |
| **Resto (ítems accionables)** | **271** | Objeto de este informe. |

Este documento se enfoca únicamente en los 271 ítems accionables.

## Tabla de inspecciones accionables

Ordenadas por cantidad de ocurrencias.

| Inspección | Ítems | Tipo | Ejemplos de ubicación |
|---|---|---|---|
| Incorrect table formatting | 86 | weak | Tablas Markdown en `docs/` |
| Unused declaration | 40 | warning | Mayormente clases/métodos de prueba (`*Test.kt`, `*E2ETest.kt`) |
| Grammar | 32 | grammar | Comentarios y documentación en Markdown/KDoc |
| Style | 16 | style | Varios (Kotlin/Markdown) |
| Obsolete Gradle Dependency | 12 | warning | `libs.versions.toml` / `build.gradle.kts` |
| Method parameter always has the same value | 10 | warning | `FavoriteRepositoryImpl`, clases de prueba E2E |
| Newer Library Versions Available | 8 | warning | `libs.versions.toml` |
| Const property naming convention | 8 | weak | `Rutas` (navegación) |
| Attribute unused on older versions | 7 | warning | `ic_launcher_foreground.xml` (atributos API 24+ con `minSdk 23`) |
| Unused resources | 7 | warning | `colors.xml` |
| Redundant nullable return type | 6 | warning | Repositorios/ViewModels falsos de prueba (`*Falso`, `*Test`) |
| Unresolved file references | 5 | warning | Enlaces rotos en varios `README.md` |
| Unresolved link in KDoc | 5 | warning | `AppContainer.kt`, `PantallaDetalleArtista.kt` |
| Unused import directive | 5 | warning | `AppContainerTest.kt`, `ArtistasFavoritosE2ETest.kt`, `CrearAlbumE2ETest.kt`, `FavoritoEntity.kt`, `PantallaColeccionistas.kt` |
| Unstable API Usage | 4 | warning | `settings.gradle.kts` |
| Annotator | 4 | error | `plan-adopcion-room.md` (resaltado de error en Markdown, no en código Kotlin/Java) |
| Unnecessary type argument | 2 | weak | Kotlin, varios |
| Obsolete Android Gradle Plugin Version | 2 | warning | `build.gradle.kts` (raíz) |
| Redundant interpolation prefix | 2 | weak | Kotlin, varios |
| Target SDK attribute is not targeting latest version | 1 | warning | `AndroidManifest.xml` |
| Java methods should be replaced with Kotlin analog | 1 | weak | — |
| Redundant label on activity | 1 | warning | `AndroidManifest.xml` |
| 'Enum.values()' is recommended to be replaced by 'Enum.entries' since 1.9 | 1 | warning | Kotlin |
| Redundant 'suspend' modifier | 1 | warning | Kotlin |
| Long overload to Duration conversion | 1 | warning | Kotlin |
| Latest Gradle minor version | 1 | warning | `gradle-wrapper.properties` |
| Assignment can be replaced with operator assignment | 1 | weak | Kotlin |
| Unused symbol | 1 | warning | Kotlin |
| Unnecessary '@OptIn' annotation | 1 | weak | Kotlin |

## Clasificación por prioridad

### 1. Corregir ahora (bajo riesgo, alto valor)
- **Unused import directive** (5): limpieza directa, sin riesgo de romper nada.
- **Redundant nullable return type** (6): simplifica firmas en dobles/falsos de
  prueba sin cambiar comportamiento.
- **Unresolved link in KDoc** (5) y **Unresolved file references** (5): enlaces
  rotos en comentarios y en varios `README.md`; fáciles de corregir y mejoran la
  documentación.
- **Const property naming convention** (8) en `Rutas`: renombrar constantes según
  convención de Kotlin (`UPPER_SNAKE_CASE`), cambio mecánico.
- **Redundant 'suspend' modifier**, **Redundant interpolation prefix**,
  **Unnecessary type argument**, **Unnecessary '@OptIn' annotation**,
  **Assignment can be replaced with operator assignment**, **Unused symbol**,
  **'Enum.values()' → 'Enum.entries'** (1–2 cada una): micro-limpiezas de estilo
  Kotlin, de bajo riesgo.

### 2. Revisar con criterio (requieren juicio, no mecánico)
- **Unused declaration** (40): en su mayoría son métodos dentro de clases de
  prueba (`AgenteDeUsuarioInterceptorTest`, `AlbumDaoTest`, etc.). Muchos son
  probablemente falsos positivos, porque JUnit invoca los métodos `@Test` por
  reflexión/anotación y no por una llamada directa que IntelliJ pueda rastrear.
  **No eliminar en bloque**: revisar caso por caso antes de borrar nada, porque
  algunos podrían ser helpers realmente sin uso.
- **Method parameter always has the same value** (10): incluye código de
  producción (`FavoriteRepositoryImpl`) junto con clases de prueba E2E. Vale la
  pena revisar si el parámetro en producción debería simplificarse o si el valor
  fijo es intencional (p. ej. para futuras variantes).
- **Attribute unused on older versions** (7) en `ic_launcher_foreground.xml`:
  atributos de gradiente que requieren API 24+, pero `minSdk = 23`. No rompe nada
  en tiempo de ejecución (Android ignora el atributo en versiones viejas), pero
  conviene decidir si se acepta la degradación visual en API 23 o se ajusta el
  drawable.
- **Unused resources** (7) en `colors.xml`: confirmar que de verdad no se usan
  antes de borrar (podrían estar referenciados dinámicamente).
- **Incorrect table formatting** (86), **Grammar** (32) y **Style** (16): todas
  en archivos Markdown/KDoc de `docs/`. Son de forma, no de código; se pueden
  corregir de forma incremental sin urgencia.
- **Annotator** (4) en `plan-adopcion-room.md`: es resaltado de error del editor
  sobre un archivo Markdown, no un error de compilación; revisar el contenido
  puntual de ese documento para confirmar si hay algo real que corregir.
- **Unstable API Usage** (4) en `settings.gradle.kts`: señala uso de una API de
  Gradle marcada como inestable; evaluar si hay alternativa estable, pero es un
  archivo de configuración protegido (ver siguiente sección).

### 3. Fuera de alcance / archivos protegidos
Por las reglas de esta sesión (no modificar `app/build.gradle.kts`,
`gradle/libs.versions.toml` ni agregar dependencias), estas inspecciones **no se
resuelven en esta rama** sin autorización explícita adicional:
- **Obsolete Gradle Dependency** (12)
- **Newer Library Versions Available** (8)
- **Obsolete Android Gradle Plugin Version** (2)
- **Latest Gradle minor version** (1)
- **Unstable API Usage** (4) en `settings.gradle.kts`

## Nota metodológica

El archivo `index.html` exportado pesa 1.3 GB y no es viable cargarlo completo en
memoria. Se procesó por streaming (lectura línea por línea) para extraer solo los
nodos de árbol relevantes (grupo/inspección/archivo/clase/método), reconstruyendo
el conteo real de cada inspección — incluyendo casos donde el exportador usa el
punto como separador de miles (p. ej. `31.643` = 31,643, no 31.643 en formato
decimal). Los ejemplos de archivo/clase por inspección se obtuvieron ubicando las
entradas hijas más cercanas a cada encabezado de inspección en el orden del
documento (recorrido en profundidad), por lo que la lista de ejemplos puede no
ser exhaustiva para inspecciones con muchas ocurrencias.
