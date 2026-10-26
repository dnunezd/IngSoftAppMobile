# IngSoftAppMobile · Vinilos

[![CI](https://github.com/dnunezd/IngSoftAppMobile/actions/workflows/ci.yml/badge.svg)](https://github.com/dnunezd/IngSoftAppMobile/actions/workflows/ci.yml)

Proyecto educativo del **equipo 9** del curso de Ingeniería de software para aplicaciones móviles: Daniel Alejandro Nuñez Donado · Jeffrey Alexander Díaz Pérez · Michael Andrés Mora Poveda · Ivette Lorena Nossa Ruiz.

**Vinilos** es una app Android para consultar y administrar un catálogo de álbumes musicales: el visitante explora álbumes y artistas, y el coleccionista aporta información al catálogo. Consume **BackVynils**, una API REST ya existente (NestJS + PostgreSQL).

La documentación del proyecto (historias de usuario, sprints con su arquitectura y estrategia de pruebas, actas y diseño en Figma) está en la [wiki](https://github.com/dnunezd/IngSoftAppMobile/wiki).

## Estado

| Historia | Estado |
|---|---|
| HU00 · Ingreso a la plataforma | Implementada: ingreso como Visitante o Coleccionista, selector de coleccionista, sesión por rol y pantalla de inicio |
| HU01 a HU03 | Pendientes. El catálogo de álbumes y el listado de artistas son destinos provisionales a los que se llega desde la pantalla de inicio |

## Versión y APK

La versión actual es `1.0.0` (`versionCode` 1), publicada como el release [`v1.0.0`](https://github.com/dnunezd/IngSoftAppMobile/releases/tag/v1.0.0) sobre `main`. El identificador de la app es `com.example.vinilos` y funciona en Android 6.0 Marshmallow (API 23) o superior.

Para instalarla sin compilar, descarga en el teléfono el APK adjunto al [release](https://github.com/dnunezd/IngSoftAppMobile/releases) y ábrelo. Ese APK está compilado para un dispositivo físico: necesita BackVynils corriendo en el computador ([paso 2](#2-levantar-el-backend)) y el teléfono conectado por USB con `adb reverse tcp:3000 tcp:3000`.

En cada release se sube `versionCode` en 1 y `versionName` sigue el tag (`1.0.0` para `v1.0.0`), ambos en `app/build.gradle.kts`. El APK del release se genera con `./gradlew assembleDebug -PbaseUrl=http://localhost:3000/` y se adjunta renombrado como `vinilos-v<versión>.apk` (la variante de release no está firmada y Android no la instala).

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

En la carpeta del repositorio de [BackVynils](https://github.com/TheSoftwareDesignLab/BackVynils):

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

La URL base sale de la propiedad de Gradle `baseUrl` (por defecto `http://10.0.2.2:3000/`).

| Dónde corres la app | Qué hacer |
|---|---|
| Emulador | Nada: `10.0.2.2` es el `localhost` del computador visto desde el emulador |
| Dispositivo físico | Ejecuta `adb reverse tcp:3000 tcp:3000` (repítelo si se reconecta) y compila con `-PbaseUrl=http://localhost:3000/`, o guárdala como `baseUrl=http://localhost:3000/` en el `gradle.properties` de tu usuario (`~/.gradle/`) |

El HTTP sin cifrar solo está permitido hacia `localhost` y `10.0.2.2` (`app/src/main/res/xml/network_security_config.xml`).

### 4. Compilar e instalar

```bash
./gradlew assembleDebug                                     # APK en app/build/outputs/apk/debug/
./gradlew installDebug -PbaseUrl=http://localhost:3000/     # instala en el dispositivo conectado
```

Si hay más de un dispositivo conectado, elige uno con la variable de entorno `ANDROID_SERIAL=<serial>`.

## Pruebas

| Tipo | Comando | Cantidad | Necesita |
|---|---|---|---|
| Lint y unitarias (lo que corre la CI) | `./gradlew lint testDebugUnitTest --continue` | 49 unitarias | Nada |
| Instrumentadas sin E2E | `./gradlew connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.notClass=com.example.vinilos.IngresoE2ETest"` | 49 | Dispositivo |
| Todas las instrumentadas (con E2E) | `./gradlew connectedDebugAndroidTest -PbaseUrl=http://localhost:3000/` | 53 | Dispositivo, backend con datos y `adb reverse` |

- El dispositivo debe estar **desbloqueado y con la pantalla encendida** durante toda la corrida; si no, las pruebas fallan con `No compose hierarchies found in the app`.
- Para las E2E pasa `-PbaseUrl` también a `connectedDebugAndroidTest`: la URL queda fija en la APK de prueba.
- Para una sola clase: `"-Pandroid.testInstrumentationRunnerArguments.class=com.example.vinilos.RecuperacionTest"` (instrumentadas) o `--tests "com.example.vinilos.viewmodel.CollectorsViewModelTest"` (unitarias).
- Los reportes quedan en `app/build/reports/` (`tests/`, `androidTests/` y `lint-results-debug.html`). Referencia del lint: 0 errores y 48 avisos esperados.

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
