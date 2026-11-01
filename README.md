# IngSoftAppMobile · Vinilos

[![CI](https://github.com/dnunezd/IngSoftAppMobile/actions/workflows/ci.yml/badge.svg)](https://github.com/dnunezd/IngSoftAppMobile/actions/workflows/ci.yml)

Proyecto educativo del **equipo 9** del curso de Ingeniería de software para aplicaciones móviles: Daniel Alejandro Nuñez Donado · Jeffrey Alexander Díaz Pérez · Michael Andrés Mora Poveda · Ivette Lorena Nossa Ruiz.

**Vinilos** es una app Android para consultar y administrar un catálogo de álbumes musicales: el visitante explora álbumes y artistas, y el coleccionista aporta información al catálogo. Consume **BackVynils**, una API REST ya existente (NestJS + PostgreSQL).

La documentación del proyecto (historias de usuario, sprints con su arquitectura y estrategia de pruebas, actas y diseño en Figma) está en la [wiki](https://github.com/dnunezd/IngSoftAppMobile/wiki).

## Estado

| Historia | Estado |
|---|---|
| HU00 · Ingreso a la plataforma | Implementada: ingreso como Visitante o Coleccionista, selector de coleccionista, sesión por rol y pantalla de inicio |
| HU01 · Catálogo de álbumes | Implementada: cuadrícula de álbumes con portada, nombre y género en orden alfabético, estados de carga, vacío y error con "Reintentar", deslizar para actualizar, posición conservada al girar y paso al detalle. Pruebas y evidencia en [`docs/pruebas/sprint-1/HU01/`](docs/pruebas/sprint-1/HU01/) |
| HU02 · Detalle de álbum | T02-3, T02-4 y T02-5 completas y verificadas en dispositivo físico (Samsung SM-A356E, Android 16) y en emulador API 23 contra el backend real: consulta, presentación, estados de pantalla, portada real con Coil, navegación desde un enlace provisional del catálogo, pruebas E2E y fecha en API 23 (91/91 instrumentadas sin fallos en ambos dispositivos). Pendiente: documentación en wiki/mockups (T02-6, borradores sin publicar) |
| HU03 · Listado de artistas | Integrado desde `develop`: listado de músicos y bandas y cargador compartido de imágenes Coil; verificación de UI de esta integración pendiente |

## Versión y APK

La versión a entregar para el Sprint 1 es `1.1.0` (`versionCode` 2), publicada como el release [`v1.1.0`](https://github.com/dnunezd/IngSoftAppMobile/releases/tag/v1.1.0) sobre `main`. La versión anterior, `1.0.0`, fue la entregada la semana previa para la revisión de los tutores ([`v1.0.0`](https://github.com/dnunezd/IngSoftAppMobile/releases/tag/v1.0.0)). El identificador de la app es `com.example.vinilos` y funciona en Android 6.0 Marshmallow (API 23) o superior.

Para instalarla sin compilar, descarga en el teléfono el APK adjunto al [release](https://github.com/dnunezd/IngSoftAppMobile/releases) y ábrelo. Ese APK está compilado para un dispositivo físico: necesita BackVynils corriendo en el computador ([paso 2](#2-levantar-el-backend)) y el teléfono conectado por USB con `adb reverse tcp:3000 tcp:3000`.

En cada release se sube `versionCode` en 1 y `versionName` sigue el tag (por ejemplo `1.1.0` para `v1.1.0`), ambos en `app/build.gradle.kts`. El APK del release se genera con `./gradlew assembleDebug -PbaseUrl=http://localhost:3000/` y se adjunta renombrado como `vinilos-v<versión>.apk` (la variante de release no está firmada y Android no la instala).

### HU02: portada real con Coil y comprobación del detalle

`ContenidoAlbum` usa una única `PortadaAlbum`, conservando el estilo de HU00. El componente se alineó al patrón ya establecido por HU03: un `AsyncImage` de Coil que reutiliza el cargador compartido de la app (`VinilosApplication`), con `inicio_albumes` como `placeholder`, `error` y `fallback` genérico. Sin URL (o en blanco) muestra directamente esa ilustración local; con URL, Coil intenta la descarga real y cae al mismo recurso si falla. Los artistas ahora también usan Coil con `artista.imagen` (mismo patrón, `inicio_artistas` como `placeholder`/`error`/`fallback`): deja de ser una imagen provisional fija.

Rediseño visual (sobre referencia `album.png` del usuario): fondo crema y tarjetas blancas locales a esta pantalla, promedio mostrado con estrellas (`★`/`☆`) en vez de un número junto a "X avg", fecha de estreno destacada junto al promedio, canciones numeradas y comentarios con estrellas en vez del texto "X / 5". El botón "+ Agregar comentario" (solo coleccionista) que pidió el usuario **no** se implementó: es funcionalidad nueva de escritura fuera del alcance de HU02 (que es solo de consulta); queda pendiente de decidir como tarea o historia aparte.

Comprobación de este cambio: **120 unitarias sin fallos**, compilación de las pruebas instrumentadas correcta y lint sin errores nuevos. Las pruebas de `PortadaAlbum` y `ContenidoAlbum` verifican que el componente se muestra con y sin URL, sin forzar un estado de carga/error específico: Coil los gestiona internamente y no son determinísticos de probar sin mockear red. **Verificado en dispositivo físico**: las 90 pruebas instrumentadas del módulo (incluyendo esta sección) pasaron contra el backend real.

Para repetir las comprobaciones sin dispositivo, desde PowerShell:

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot'
.\gradlew.bat testDebugUnitTest :app:assembleDebugAndroidTest
```

Para ejecutar las pruebas de UI pendientes, conecta y desbloquea un teléfono o usa un emulador ya disponible; comprueba el dispositivo con `adb devices` y, si hay varios, define `$env:ANDROID_SERIAL='<serial>'`:

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot'
.\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.package=com.example.vinilos.ui.detalle"
```

Este filtro ejecuta `PortadaAlbumTest`, `ContenidoAlbumTest` y `PantallaDetalleAlbumTest` con datos controlados, **sin Docker ni backend**. Comprueba portada con y sin URL, posición antes de los metadatos, artistas, canciones, comentarios, promedio, vacíos, regreso, reintento y 404.

**T02-3 está completa y verificada en dispositivo** (consulta, presentación, estados de pantalla y portada real con Coil).

### HU02: navegación desde el catálogo (T02-4)

Al tocar un álbum del catálogo (HU01) se navega a la ruta `album/{id}` con su id; volver desde el detalle (éxito o 404) regresa al catálogo sin repetir su consulta. El enlace provisional que se usó mientras HU01 no existía (`PantallaCatalogoProvisional`, `AlbumIdProvisional = 100`) ya se eliminó.

Comprobación de este cambio: **119 unitarias sin fallos**, `:app:assembleDebugAndroidTest` compila sin errores y lint sin errores. Las pruebas de navegación (`VinilosNavHostTest`) y del enlace provisional (ya eliminado) se escribieron primero (ROJO por compilación) y luego se implementó lo mínimo para que compilen. **Verificado en dispositivo físico por el usuario**: se detectó que el id inicial (`1`) no correspondía a un álbum real (los ids de álbum de BackVynils empiezan en 100, igual que collectors/artistas) y se corrigió a `100`.

Para ejecutar estas pruebas de navegación, conecta y desbloquea un teléfono o usa un emulador ya disponible:

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot'
.\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.package=com.example.vinilos.navigation"
```

E2E (T02-5) ejecutadas y verificadas contra backend real en dispositivo físico: `AlbumDetalleE2ETest` (flujo completo del visitante con el álbum 100) y `AlbumDetalleBackendRealTest` (álbum 101 sin canciones/comentarios y un id inexistente para el 404 real). Suite completa del módulo: 90/90 pruebas instrumentadas sin fallos.

## Requisitos

| Herramienta | Versión |
|---|---|
| JDK | 17. Para compilar por terminal define `JAVA_HOME` con un JDK 17: el que trae Android Studio puede ser más nuevo y Gradle 8.7 no lo soporta |
| Android Studio | Compatible con AGP 8.6.0 (Koala Feature Drop, 2024.1.2, o superior) |
| Android SDK | Plataforma 34 (`minSdk` 23, `compileSdk` y `targetSdk` 34), con `adb` en `platform-tools` |
| Docker | Docker Desktop o Docker Engine con Compose, para levantar BackVynils |
| Dispositivo | Físico o emulador con Android 6.0 (API 23) o superior |

Gradle 8.7 viene con el wrapper. Stack: Kotlin 1.9, Jetpack Compose con Material 3, MVVM + Repository, Retrofit + OkHttp, Navigation Compose y corrutinas.

## Puesta en marcha

Los comandos se ejecutan desde la raíz del repositorio y están escritos para macOS, Linux o Git Bash. En PowerShell usa `.\gradlew.bat` en lugar de `./gradlew`, `curl.exe` en lugar de `curl`, y pon entre comillas los argumentos `-P` cuyo nombre lleva puntos.

### 1. Abrir el proyecto

Abre la raíz del repositorio en Android Studio y deja que sincronice Gradle. Si compilas solo por terminal y Gradle dice `SDK location not found`, define `ANDROID_HOME` con la ruta del SDK o crea `local.properties` (no se versiona) con `sdk.dir=<ruta del SDK>`.

### 2. Levantar el backend

Hay dos formas de tener un backend disponible: el desplegado por los tutores (sin instalar nada) o uno local con Docker.

#### Opción A: backend desplegado por los tutores (preferida)

**Desde esta versión, este es el valor por defecto del proyecto** (`app/build.gradle.kts`): compilar o instalar sin pasar `-PbaseUrl` apunta a `https://backvynils-q6yc.onrender.com/`. No requiere Docker ni `adb reverse`, y funciona igual en emulador y en teléfono.

> 💡 Al estar en un plan gratuito, puede que esté disponible o puede que esté dormido/caído en un momento dado. Antes de compilar, date una vuelta y verifica si está despierto:
>
> ```bash
> curl https://backvynils-q6yc.onrender.com/collectors
> ```
>
> Si responde con datos (no vacío ni error), ya puedes usarlo; si no responde o da error, usa la opción B mientras se restablece.

#### Opción B: backend local con Docker

Abre **Docker Desktop** primero y espera a que el ícono de la ballena esté activo (sin eso, `docker compose` falla con un error de conexión al daemon). Luego, en la carpeta del repositorio de [BackVynils](https://github.com/TheSoftwareDesignLab/BackVynils):

```bash
docker compose up -d --build
curl http://localhost:3000/collectors
```

La respuesta debe incluir a **Manolo Bellon** y **Jaime Monsalve** (ids 100 y 101); las pruebas E2E dependen de esos datos. Dos ajustes que pueden hacer falta:

- **PostgreSQL no arranca** (el compose usa la imagen `postgres` sin versión): crea `docker-compose.override.yml` junto al compose con este contenido y vuelve a levantarlo.

  ```yaml
  services:
    db:
      image: postgres:13
  ```

- **`/collectors` responde `[]`**: carga los datos de ejemplo.

  ```bash
  docker compose cp sql/data.sql db:/tmp/data.sql
  docker compose exec db sh -c "sed -i '1s/^\xEF\xBB\xBF//' /tmp/data.sql && psql -U postgres -d vinyls -f /tmp/data.sql"
  ```

### 3. Conectar la app con el backend

La URL base sale de la propiedad de Gradle `baseUrl`. **Por defecto es `https://backvynils-q6yc.onrender.com/`** (el backend desplegado por los tutores); para usar el backend local con Docker hay que pasar `-PbaseUrl` explícitamente.

| Dónde corres la app | Qué hacer |
|---|---|
| Emulador o dispositivo físico, con el backend desplegado (opción A, preferida) | **Nada**: es el valor por defecto, no necesita `-PbaseUrl` ni `adb reverse` ni Docker |
| Emulador, con backend local (opción B) | Compila con `-PbaseUrl=http://10.0.2.2:3000/` (`10.0.2.2` es el `localhost` del computador visto desde el emulador) |
| Dispositivo físico, con backend local (opción B) | Ejecuta `adb reverse tcp:3000 tcp:3000` (repítelo si se reconecta) y compila con `-PbaseUrl=http://localhost:3000/`, o guárdala como `baseUrl=http://localhost:3000/` en el `gradle.properties` de tu usuario (`~/.gradle/`) |

El HTTP sin cifrar solo está permitido hacia `localhost` y `10.0.2.2` (`app/src/main/res/xml/network_security_config.xml`).

### 4. Compilar e instalar

El APK ya apunta por defecto al backend desplegado por los tutores (ver opción A); mientras esté caído, genera el APK apuntando al local (opción B) con `-PbaseUrl=http://localhost:3000/` (o `http://10.0.2.2:3000/` en emulador).

```bash
./gradlew assembleDebug     # APK en app/build/outputs/apk/debug/, apunta al backend desplegado (default)
./gradlew installDebug      # instala en el dispositivo conectado, apunta al backend desplegado (default)
```

Si el backend desplegado está caído, agrega `-PbaseUrl=http://localhost:3000/` (con `adb reverse tcp:3000 tcp:3000` en dispositivo físico) o `-PbaseUrl=http://10.0.2.2:3000/` en emulador.

Si hay más de un dispositivo conectado, elige uno con la variable de entorno `ANDROID_SERIAL=<serial>`.

## Pruebas

| Tipo | Comando | Cantidad | Necesita |
|---|---|---|---|
| Lint y unitarias (lo que corre la CI) | `./gradlew lint testDebugUnitTest --continue` | 49 unitarias | Nada |
| Instrumentadas sin E2E | `./gradlew connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.notClass=com.example.vinilos.IngresoE2ETest,com.example.vinilos.CatalogoE2ETest,com.example.vinilos.AlbumDetalleE2ETest,com.example.vinilos.navigation.AlbumDetalleBackendRealTest,com.example.vinilos.ArtistasE2ETest"` | 49 | Dispositivo |
| Todas las instrumentadas (con E2E) | `./gradlew connectedDebugAndroidTest -PbaseUrl=http://localhost:3000/` | 68 (49 + 19 E2E) | Dispositivo, backend con datos y `adb reverse` |

- El dispositivo debe estar **desbloqueado y con la pantalla encendida** durante toda la corrida; si no, las pruebas fallan con `No compose hierarchies found in the app`.
- Para las E2E pasa `-PbaseUrl` también a `connectedDebugAndroidTest` apuntando a tu backend local (opción B): la URL queda fija en la APK de prueba y, como el default del proyecto ahora es el backend desplegado (que puede estar caído), **las E2E necesitan este flag explícito para no depender de él**.
- Para una sola clase: `"-Pandroid.testInstrumentationRunnerArguments.class=com.example.vinilos.RecuperacionTest"` (instrumentadas) o `--tests "com.example.vinilos.viewmodel.CollectorsViewModelTest"` (unitarias).
- Los reportes quedan en `app/build/reports/` (`tests/`, `androidTests/` y `lint-results-debug.html`). Referencia del lint: 0 errores y 48 avisos esperados.

### E2E completas y su evidencia (PDF)

Las clases E2E (las que llaman a `EvidenciaE2E.capturar(...)`) corren contra el **backend real** (BackVynils, local o el desplegado por los tutores) y generan, en el dispositivo, una carpeta con capturas y un informe en PDF por cada valor de `carpetaEvidencia`/`evidencia` usado, en `.../files/e2e-evidencia/<carpeta>/`. No se versionan en el repositorio (`.gitignore` las excluye con el sufijo `e2e-evidencia*`); mientras el equipo no defina un estándar único, algunos informes se han versionado también a mano en `docs/pruebas/sprint-1/<HU>/e2e/` (ver por ejemplo `docs/pruebas/sprint-1/HU01/e2e/informe.md`).

| Clase | Historia | Carpeta(s) de evidencia | Escenarios |
|---|---|---|---|
| `IngresoE2ETest` | HU00 | `FN00_ESC01`..`FN00_ESC03` (una por escenario) | 3 |
| `CatalogoE2ETest` | HU01 | `FN01_ESC01`..`FN01_ESC05` (una por escenario) | 5 |
| `AlbumDetalleE2ETest` | HU02 | `FN02_ESC01` | 1 |
| `AlbumDetalleBackendRealTest` | HU02 | `FN02_ESC02`, `FN02_ESC03` (una por escenario) | 2 |
| `ArtistasE2ETest` | HU03 | `FN03_ESC01`..`FN03_ESC07` (una por escenario) | 7 |

Total: **19 pruebas E2E**. Por qué una "limpieza" antes de correrlas: `BuildConfig.BASE_URL` queda fijo dentro del APK de prueba al compilar, así que cambiar la `baseUrl` exige **reinstalar** (`installDebug installDebugAndroidTest`), no alcanza con solo volver a ejecutar; y las carpetas de evidencia se acumulan en el dispositivo entre corridas, así que conviene borrarlas antes de cada corrida para no mezclar capturas de ejecuciones distintas en el mismo PDF.

#### Correr todas las E2E de la app y traer su evidencia

Comandos para bash (macOS, Linux o Git Bash); en PowerShell usa `.\gradlew.bat`, pon entre comillas los `-P` con puntos y `Remove-Item -Recurse -Force` en vez de `rm -rf` para la carpeta local.

```bash
adb devices                                      # confirma el dispositivo/emulador conectado
export ANDROID_SERIAL="<serial>"                 # solo si hay más de uno

# Compila e instala. El default del proyecto es el backend desplegado por los tutores; para
# usar un backend local pasa -PbaseUrl explícito (http://10.0.2.2:3000/ en emulador,
# http://localhost:3000/ en dispositivo físico con "adb reverse" hecho).
./gradlew installDebug installDebugAndroidTest -PbaseUrl=http://localhost:3000/

# Solo en dispositivo físico por USB, si usas el backend local:
adb reverse tcp:3000 tcp:3000

# Limpia evidencia previa en el dispositivo (no es necesario un "gradle clean")
adb shell rm -rf /storage/emulated/0/Android/data/com.example.vinilos/files/e2e-evidencia

# Corre TODAS las clases E2E de la app (las 19 pruebas)
adb shell am instrument -w -e class com.example.vinilos.IngresoE2ETest,com.example.vinilos.CatalogoE2ETest,com.example.vinilos.AlbumDetalleE2ETest,com.example.vinilos.navigation.AlbumDetalleBackendRealTest,com.example.vinilos.ArtistasE2ETest com.example.vinilos.test/androidx.test.runner.AndroidJUnitRunner

# Trae la evidencia (pisa la carpeta local si ya existía)
rm -rf ./e2e-evidencia
adb pull /storage/emulated/0/Android/data/com.example.vinilos/files/e2e-evidencia ./e2e-evidencia
```

Debe terminar en `OK (19 tests)` (3 + 5 + 1 + 2 + 7 escenarios con evidencia, más un test de navegación en `IngresoE2ETest` sin captura propia). El dispositivo debe permanecer desbloqueado y con la pantalla encendida durante toda la corrida (`adb shell svc power stayon true` ayuda si se duerme).

> ⚠️ `./gradlew connectedDebugAndroidTest` instala, corre y **desinstala** la app al terminar, lo que borra su carpeta de evidencia antes de poder hacer `adb pull`. Usa `installDebug installDebugAndroidTest` + `adb shell am instrument` (como arriba) para dejar la app instalada y poder traer la evidencia después.

#### Correr solo una E2E específica

Reemplaza la lista de `-e class` por una sola clase, por ejemplo para HU02:

```bash
adb shell am instrument -w -e class com.example.vinilos.AlbumDetalleE2ETest,com.example.vinilos.navigation.AlbumDetalleBackendRealTest com.example.vinilos.test/androidx.test.runner.AndroidJUnitRunner
adb pull /storage/emulated/0/Android/data/com.example.vinilos/files/e2e-evidencia ./e2e-evidencia
```

La [integración continua](.github/workflows/ci.yml) corre lint y pruebas unitarias con JDK 17 en cada Pull Request y en cada push a `develop` y `main`, y falla si hay un error de lint o una prueba fallida. Las instrumentadas no corren en CI porque necesitan un dispositivo; se corren a mano antes de integrar. Qué cubre cada nivel de prueba está en la estrategia de pruebas de cada sprint en la [wiki](https://github.com/dnunezd/IngSoftAppMobile/wiki).

## Problemas frecuentes

| Síntoma | Causa y solución |
|---|---|
| `No compose hierarchies found in the app` | El dispositivo está bloqueado o con la pantalla apagada. |
| "No pudimos cargar los coleccionistas" o fallan las E2E | Backend caído, falta `adb reverse` o la app se instaló con otra `baseUrl`. Revisa `curl http://localhost:3000/collectors` y reinstala con `-PbaseUrl`. |
| `Cleartext HTTP traffic not permitted` | El host del backend no está en `network_security_config.xml`. |
| `more than one device/emulator` | Elige un dispositivo con `ANDROID_SERIAL`. |
| `Your project path contains non-ASCII characters` (Windows) | La ruta del repositorio tiene tildes. Clónalo en una ruta sin ellas (por ejemplo `C:\dev\IngSoftAppMobile`); `android.overridePathCheck=true` no basta, porque las pruebas unitarias fallan con `ClassNotFoundException`. |
| Gradle falla al arrancar y solo muestra una versión de Java (por ejemplo `25.0.3`) | Gradle 8.7 no soporta ese JDK: define `JAVA_HOME` con un JDK 17. |

## Estructura y arquitectura

```
app/src/main/java/com/example/vinilos/
  data/        model, remote (ServiceAdapter, interfaces Retrofit y DTO), repository
  viewmodel/   SessionViewModel y un ViewModel con su estado por pantalla
  ui/          pantallas Compose por funcionalidad y tema
  navigation/  VinilosNavHost: grafo de navegación
app/src/test/ y app/src/androidTest/   pruebas unitarias e instrumentadas
openspec/      specs vigentes (specs/), cambios en curso (changes/) y archivados (changes/archive/)
```

**MVVM** con capa de datos por **Repository**: la vista dibuja el estado que expone el `ViewModel` y le envía los eventos; los repositorios hablan con el backend solo a través del `ServiceAdapter`, que construye una sola vez el cliente de red y traduce los errores de red y del servidor a `ErrorDeCarga`. Los diagramas están en la página de cada sprint en la wiki.

## Flujo de trabajo

- Gitflow con `main`, `develop` y `feature/*`: cada historia se integra a `develop` por Pull Request (*squash and merge*, dos revisores y `Closes #n`), y cada release se etiqueta sobre `main`.
- El comportamiento se especifica con [OpenSpec](https://github.com/Fission-AI/OpenSpec) en `openspec/`, con requisitos y escenarios verificables por capacidad.
