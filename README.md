# IngSoftAppMobile · Vinilos

[![CI](https://github.com/dnunezd/IngSoftAppMobile/actions/workflows/ci.yml/badge.svg)](https://github.com/dnunezd/IngSoftAppMobile/actions/workflows/ci.yml)

Proyecto educativo del **equipo 9** del curso de Ingeniería de software para aplicaciones móviles.

**Vinilos** es una app Android para consultar y administrar un catálogo de álbumes musicales: el visitante explora álbumes y artistas, y el coleccionista aporta información al catálogo. La app consume **BackVynils**, una API REST ya existente (NestJS + PostgreSQL).

- Documentación del proyecto (historias de usuario, sprints, actas, mockups): [wiki](https://github.com/dnunezd/IngSoftAppMobile/wiki)
- Diseño: prototipo y diseño UX en Figma, enlazados desde la wiki.

## Equipo

Daniel Alejandro Nuñez Donado · Jeffrey Alexander Díaz Pérez · Michael Andrés Mora Poveda · Ivette Lorena Nossa Ruiz

## Estado

| Historia | Estado |
|---|---|
| HU00 · Ingreso a la plataforma | Implementada: ingreso (Visitante o Coleccionista), selector de coleccionista, sesión por rol y pantalla de inicio |
| HU01 a HU03 | Pendientes. El catálogo de álbumes y el listado de artistas son **destinos provisionales** a los que se llega desde las tarjetas de la pantalla de inicio |

Flujo actual: `ingreso → (selector de coleccionistas) → inicio → catálogo | artistas`.

## Versión y APK

| Dato | Valor |
|---|---|
| Versión | `1.0.0` (`versionCode` 1), publicada como el release [`v1.0.0`](https://github.com/dnunezd/IngSoftAppMobile/releases/tag/v1.0.0) sobre `main` |
| Identificador de la app | `com.misw.vinilos` |
| Android soportado | 5.0 Lollipop (API 21) o superior |

Para instalar la app sin compilarla, descarga el APK adjunto al [release](https://github.com/dnunezd/IngSoftAppMobile/releases) en el teléfono y ábrelo (Android pide permitir la instalación desde esa fuente). El APK del release está compilado para un dispositivo físico (`-PbaseUrl=http://localhost:3000/`): necesita BackVynils corriendo en el computador ([paso 2](#2-levantar-el-backend-backvynils)) y el teléfono conectado por USB con `adb reverse tcp:3000 tcp:3000` ([paso 3](#3-conectar-la-app-con-el-backend)). Si tenías instalada una versión anterior con el identificador `com.example.vinilos`, desinstálala: Android la trata como otra app.

En cada release se sube `versionCode` en 1 y `versionName` sigue el tag (`1.0.0` para `v1.0.0`), ambos en `app/build.gradle.kts`. El APK que se adjunta se genera con:

```bash
./gradlew assembleDebug -PbaseUrl=http://localhost:3000/   # app/build/outputs/apk/debug/app-debug.apk
```

y se renombra como `vinilos-v<versión>.apk` (por ejemplo `vinilos-v1.0.0.apk`). Se usa la variante de depuración porque la de release no está firmada y Android no la instala.

## Requisitos

| Herramienta | Versión |
|---|---|
| Sistema operativo | Windows 10 u 11, macOS o Linux |
| JDK | 17. Para compilar por terminal define `JAVA_HOME` apuntando a un JDK 17: el que trae Android Studio puede ser más nuevo y Gradle 8.7 no lo soporta |
| Android Studio | Una versión compatible con AGP 8.6.0 (Koala Feature Drop, 2024.1.2, o superior) |
| Android SDK | Plataforma 34 (`minSdk` 21, `compileSdk` y `targetSdk` 34). Trae `adb` en `platform-tools` |
| Gradle | 8.7, ya incluido con el wrapper (`gradlew`) |
| Docker | Docker Desktop (Windows y macOS) o Docker Engine con el plugin Compose (Linux), para levantar BackVynils |
| Dispositivo Android | Físico o emulador con Android 5.0 Lollipop (API 21) o superior. En Mac con chip Apple el emulador necesita una imagen ARM64 |

Stack: Kotlin 1.9.0, Jetpack Compose (BOM 2024.04.01) con Material 3, MVVM + Repository, Retrofit 2.9.0 + OkHttp 4.12.0, Navigation Compose 2.7.7 y corrutinas 1.8.0.

## Puesta en marcha

Los comandos se ejecutan desde la raíz del repositorio y los ejemplos están escritos para **macOS, Linux o Git Bash**. En **Windows** cambian estas cosas:

| Shell | Gradle | Variable de entorno | `curl` |
|---|---|---|---|
| macOS, Linux, Git Bash | `./gradlew` | `VAR=valor comando` | `curl` |
| PowerShell | `.\gradlew.bat` | `$env:VAR="valor"` | `curl.exe` (en Windows PowerShell 5.1 `curl` es otro comando) |
| CMD | `gradlew.bat` | `set VAR=valor` | `curl` |

Pon entre comillas los argumentos `-P` cuyo nombre lleva puntos (como `"-Pandroid.testInstrumentationRunnerArguments.class=..."`): en PowerShell pueden partirse si no.

### 1. Abrir el proyecto

Abre la **raíz del repositorio** en Android Studio y deja que sincronice Gradle. Android Studio crea `local.properties` con la ruta del SDK. Si compilas solo por terminal y Gradle dice `SDK location not found`, crea ese archivo (no se versiona) con la ruta de tu sistema:

```properties
# Windows
sdk.dir=C\:\\Users\\TU_USUARIO\\AppData\\Local\\Android\\Sdk
# macOS
sdk.dir=/Users/TU_USUARIO/Library/Android/sdk
# Linux
sdk.dir=/home/TU_USUARIO/Android/Sdk
```

o define la variable de entorno `ANDROID_HOME` con esa misma ruta.

### 2. Levantar el backend (BackVynils)

En la carpeta del repositorio de BackVynils:

```bash
docker compose up -d --build
curl http://localhost:3000/collectors      # PowerShell: curl.exe http://localhost:3000/collectors
```

La respuesta debe incluir a **Manolo Bellon** y **Jaime Monsalve** (ids 100 y 101). Las pruebas E2E dependen de esos datos.

Dos ajustes que pueden hacer falta:

- **Si PostgreSQL no arranca** (el `docker-compose.yml` usa la imagen `postgres` sin versión y Postgres 18 cambió la ruta de datos), crea `docker-compose.override.yml` junto al compose:

  ```yaml
  services:
    db:
      image: postgres:13
  ```

  y vuelve a ejecutar `docker compose up -d --build`.
- **Si `/collectors` responde `[]`**, carga los datos de ejemplo. El archivo trae una marca BOM al inicio que `psql` no tolera, así que se copia al contenedor y se limpia allí; como el `sed` corre dentro del contenedor (Linux), los dos comandos son los mismos en Windows, macOS y Linux:

  ```bash
  docker compose cp sql/data.sql db:/tmp/data.sql
  docker compose exec db sh -c "sed -i '1s/^\xEF\xBB\xBF//' /tmp/data.sql && psql -U postgres -d vinyls -f /tmp/data.sql"
  ```

### 3. Conectar la app con el backend

La URL base sale de la propiedad de Gradle `baseUrl` y vale `http://10.0.2.2:3000/` por defecto.

| Dónde corres la app | Qué hacer |
|---|---|
| **Emulador** | Nada: `10.0.2.2` es el `localhost` de tu computador visto desde el emulador. |
| **Dispositivo físico** | Ejecuta `adb reverse tcp:3000 tcp:3000` y compila con `-PbaseUrl=http://localhost:3000/`. |

Para no repetir `-PbaseUrl` en cada comando, guárdala una vez en el `gradle.properties` de tu usuario, fuera del repositorio (macOS y Linux: `~/.gradle/gradle.properties`; Windows: `%USERPROFILE%\.gradle\gradle.properties`):

```properties
baseUrl=http://localhost:3000/
```

`adb` viene en la carpeta `platform-tools` del SDK. Si tu terminal no lo encuentra, agrégala al `PATH`: Windows `%LOCALAPPDATA%\Android\Sdk\platform-tools`, macOS `~/Library/Android/sdk/platform-tools`, Linux `~/Android/Sdk/platform-tools`.

El tráfico HTTP sin cifrar solo está permitido hacia `localhost` y `10.0.2.2` (`app/src/main/res/xml/network_security_config.xml`). Si usas la IP de tu computador en la red local, agrégala allí o la app no podrá conectarse (`Cleartext HTTP traffic not permitted`).

### 4. Compilar e instalar

```bash
./gradlew assembleDebug                                     # APK en app/build/outputs/apk/debug/
./gradlew installDebug -PbaseUrl=http://localhost:3000/     # instala en el dispositivo conectado
```

Si hay más de un dispositivo conectado (`adb devices`), elige uno con la variable `ANDROID_SERIAL`:

```bash
ANDROID_SERIAL=<serial> ./gradlew installDebug              # macOS, Linux, Git Bash
```

```powershell
$env:ANDROID_SERIAL="<serial>"; .\gradlew.bat installDebug  # PowerShell
```

```bat
set ANDROID_SERIAL=<serial>
gradlew.bat installDebug
```

### Resumen de ajustes

| Qué | Dónde | Cuándo |
|---|---|---|
| Ruta del SDK | `local.properties` o `ANDROID_HOME` | Siempre, la primera vez |
| URL del backend | `-PbaseUrl=...` o `~/.gradle/gradle.properties` | Dispositivo físico (o backend en otra dirección) |
| Túnel al backend | `adb reverse tcp:3000 tcp:3000` | Dispositivo físico; hay que repetirlo si se reconecta |
| Imagen de Postgres | `docker-compose.override.yml` de BackVynils | Si la base no arranca |
| Hosts HTTP permitidos | `network_security_config.xml` | Si el backend no está en `localhost` ni `10.0.2.2` |

## Pruebas

| Tipo | Comando | Cantidad | Necesita |
|---|---|---|---|
| Unitarias | `./gradlew testDebugUnitTest` | 49 | Nada (corren en el computador) |
| Lint | `./gradlew lint` | | Nada |
| Instrumentadas sin E2E | `./gradlew connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.notClass=com.example.vinilos.IngresoE2ETest"` | 49 | Dispositivo desbloqueado (ni backend ni Docker) |
| Todas las instrumentadas (con E2E) | `./gradlew connectedDebugAndroidTest -PbaseUrl=http://localhost:3000/` | 53 | Dispositivo desbloqueado, backend con datos y `adb reverse` |
| Todo | `./gradlew lint testDebugUnitTest connectedDebugAndroidTest -PbaseUrl=http://localhost:3000/` | | Lo de las dos filas anteriores |

En Windows, los comandos de `gradlew` cambian como se explicó arriba (`.\gradlew.bat` en PowerShell). Las unitarias corren en pocos segundos con la compilación ya hecha (la primera vez, con Gradle descargando dependencias, tardan más). Las instrumentadas instalan la app y las pruebas en el dispositivo y tardan alrededor de un minuto.

### Qué cubre cada prueba

| Ubicación | Qué verifica |
|---|---|
| `src/test` · `data/` | `ServiceAdapter` con `MockWebServer`: éxito, lista vacía, 404, 500, sin conexión, tiempo de espera, JSON inválido, cancelación y reintento tras un fallo. `CollectorRepository`: mapeo de datos, campos desconocidos y elementos incompletos descartados, y propagación del error del adaptador; el DTO ignora los campos que no usa |
| `src/test` · `viewmodel/` | `CollectorsViewModel` (estados cargando, éxito, vacío y error, reintento, búsqueda sin distinguir mayúsculas ni tildes) y `SessionViewModel` (rol, coleccionista activo, acciones de coleccionista) |
| `src/test` · `ui/ingreso/` | Iniciales del avatar |
| `src/androidTest` · `ui/` | Pantallas de Compose en cada estado: ingreso, selector de coleccionista y pantalla de inicio (textos literales de la HU00, callbacks, saludo según el perfil) |
| `src/androidTest` · `navigation/` | El grafo completo con un repositorio falso: cada transición, "Atrás", cambio de perfil y sesión |
| `src/androidTest` · `RotacionTest` | Recrea la `Activity` (girar la pantalla): se conservan lista, búsqueda y sesión sin repetir la consulta |
| `src/androidTest` · `RecuperacionTest` | **Recuperación ante fallos del backend, automática y sin backend**: con el servidor caído o con error 500 se ve "No pudimos cargar los coleccionistas" con "Reintentar" y la opción de Visitante; al volver el servidor, "Reintentar" muestra la lista; varios reintentos fallidos seguidos de uno exitoso. Usa `MockWebServer` en el dispositivo |
| `src/androidTest` · `IngresoE2ETest` | **E2E contra BackVynils real**: Visitante, Coleccionista con la lista real, buscador, "Atrás" y cambio de perfil |

### Ejecutar solo una parte

```bash
# Una clase
./gradlew connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.example.vinilos.ui.inicio.PantallaInicioTest"

# Un método
./gradlew connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.example.vinilos.ui.inicio.PantallaInicioTest#sinSesionElSaludoEsElDelVisitante"

# Unitarias de una clase
./gradlew testDebugUnitTest --tests "com.example.vinilos.viewmodel.CollectorsViewModelTest"
```

### Recuperación ante fallos del backend

No requiere pasos manuales: `RecuperacionTest` levanta un servidor local dentro de la propia prueba (`MockWebServer`), lo hace caer o responder con error y luego "volver", y comprueba qué ve la persona en cada momento. La app usa su pila de red real (Retrofit y OkHttp); solo el servidor es de mentira, lo que hace la prueba determinista y deja las E2E contra el backend real para los flujos normales. Corre sola con el dispositivo, sin Docker ni `adb reverse`:

```bash
./gradlew connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.example.vinilos.RecuperacionTest"
```

### Antes de correr las instrumentadas

- El dispositivo debe estar **desbloqueado y con la pantalla encendida** durante toda la corrida. Si se bloquea, las pruebas fallan con `No compose hierarchies found in the app`. Para una corrida larga, sube el tiempo de pantalla: `adb shell settings put system screen_off_timeout 600000`.
- Para las E2E: backend arriba con los datos de ejemplo y, en dispositivo físico, `adb reverse tcp:3000 tcp:3000`. Pasa `-PbaseUrl` también a `connectedDebugAndroidTest`: la URL queda fija en la APK que se instala para la prueba.

### Reportes

| Reporte | Ruta |
|---|---|
| Unitarias | `app/build/reports/tests/testDebugUnitTest/index.html` |
| Instrumentadas | `app/build/reports/androidTests/connected/debug/index.html` |
| Lint | `app/build/reports/lint-results-debug.html` |

Referencia actual del lint: 0 errores y 48 avisos, todos de la plantilla de Android Studio o esperados (por ejemplo `targetSdk`, versiones de dependencias y atributos del degradado del ícono que solo se usan desde API 24).

### Integración continua

El flujo [`.github/workflows/ci.yml`](.github/workflows/ci.yml) corre en GitHub Actions en cada Pull Request y en cada push a `develop` y `main` (también a mano desde la pestaña *Actions*). Ejecuta, con JDK 17 en Ubuntu, el mismo comando que puedes correr en tu computador:

```bash
./gradlew lint testDebugUnitTest --continue
```

| Paso | Falla la ejecución si... |
|---|---|
| Validación del wrapper de Gradle | El `gradle-wrapper.jar` no es el oficial |
| `lint` | Aparece un error de lint (los avisos de la plantilla no fallan) |
| `testDebugUnitTest` | Falla alguna prueba unitaria |

Cada ejecución deja un resumen (pruebas y lint) en su página y publica los informes HTML como artefacto `informes-calidad` (14 días).

Las pruebas instrumentadas **no corren en CI** porque necesitan un dispositivo y, las E2E, el backend. Se corren a mano antes de integrar, como se describe arriba.

### Problemas frecuentes

| Síntoma | Causa y solución |
|---|---|
| `No compose hierarchies found in the app` | El dispositivo está bloqueado o con la pantalla apagada. Desbloquéalo y repite. |
| La app muestra "No pudimos cargar los coleccionistas" o las E2E fallan | Backend caído, falta `adb reverse`, o la app se instaló con otra `baseUrl`. Revisa `curl http://localhost:3000/collectors` y vuelve a instalar con `-PbaseUrl`. |
| `more than one device/emulator` | Hay varios dispositivos conectados: elige uno con `ANDROID_SERIAL` (ver el paso 4, según tu shell). |
| `adb` no se reconoce o `command not found` | Agrega `platform-tools` del SDK al `PATH` (rutas en el paso 3). |
| PowerShell: `gradlew.bat is not recognized` | En PowerShell hay que anteponer `.\`: `.\gradlew.bat`. |
| `SDK location not found` | Falta `local.properties` o `ANDROID_HOME`. |
| `Permission denied` al ejecutar `./gradlew` (macOS y Linux) | `chmod +x gradlew` (en el repositorio ya va con permiso de ejecución). |
| `Cleartext HTTP traffic not permitted` | El host del backend no está en `network_security_config.xml`. |
| `Your project path contains non-ASCII characters` (Windows) | La ruta del repositorio tiene tildes u otros caracteres no ASCII. Clónalo en una ruta sin ellos (por ejemplo `C:\dev\IngSoftAppMobile`); forzar la compilación con `android.overridePathCheck=true` no basta, porque las pruebas unitarias fallan con `ClassNotFoundException`. |
| Gradle falla al arrancar y solo muestra una versión de Java (por ejemplo `25.0.3`) | Gradle 8.7 no soporta ese JDK. Define `JAVA_HOME` apuntando a un JDK 17 (las versiones recientes de Android Studio traen un JDK más nuevo). |

## Estructura

```
app/src/main/java/com/example/vinilos/
  data/        model (Collector, Session), remote (ServiceAdapter, ErrorDeCarga, CollectorApi, DTO), repository
  viewmodel/   SessionViewModel, CollectorsViewModel y sus estados
  ui/          ingreso, inicio, catalogo (provisional), artistas (provisional), theme
  navigation/  VinilosNavHost: grafo de navegación
app/src/main/res/  textos (strings.xml), vectores del Figma (drawable/) e imágenes (drawable-nodpi/)
app/src/test/ y app/src/androidTest/   pruebas unitarias e instrumentadas
openspec/      especificaciones (specs/) y cambios archivados (changes/archive/)
```

Arquitectura: **MVVM** con capa de datos por **Repository**. La vista dibuja el estado que expone el `ViewModel` y le envía los eventos; los repositorios hablan con el backend solo a través del `ServiceAdapter` (`data/remote`), que construye el cliente de red una sola vez (Retrofit y OkHttp), entrega la interfaz de cada entidad y traduce los errores de red y del servidor a `ErrorDeCarga`; el repositorio se queda con convertir los DTO al modelo.

## Flujo de trabajo

- Gitflow: ramas `main`, `develop` y `feature/*`. Cada historia se trabaja en una rama `feature/` y se integra a `develop` con un Pull Request (*squash and merge*, revisado por al menos dos integrantes, con `Closes #n` en la descripción).
- El comportamiento del sistema se documenta con [OpenSpec](https://github.com/Fission-AI/OpenSpec) en `openspec/`: cada capacidad tiene su spec con requisitos y escenarios verificables.
- La definición de terminado de cada sprint está en la [wiki](https://github.com/dnunezd/IngSoftAppMobile/wiki).
