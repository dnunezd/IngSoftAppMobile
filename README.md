# IngSoftAppMobile · Vinilos

[![CI](https://github.com/dnunezd/IngSoftAppMobile/actions/workflows/ci.yml/badge.svg)](https://github.com/dnunezd/IngSoftAppMobile/actions/workflows/ci.yml)

Proyecto educativo del **equipo 9** del curso de Ingeniería de software para aplicaciones móviles: Daniel Alejandro Nuñez Donado · Jeffrey Alexander Díaz Pérez · Michael Andrés Mora Poveda · Ivette Lorena Nossa Ruiz.

**Vinilos** es una app Android para consultar y administrar un catálogo de álbumes musicales: el Visitante explora álbumes y artistas, y el Coleccionista aporta información al catálogo. Consume **BackVynils**, una API REST ya existente (NestJS + PostgreSQL).

La documentación del proyecto (historias de usuario, arquitectura y estrategia de pruebas de cada sprint, actas y diseño en Figma) está en la [wiki](https://github.com/dnunezd/IngSoftAppMobile/wiki).

## Historias por sprint

### Sprint 1 · release [`v1.1.0`](https://github.com/dnunezd/IngSoftAppMobile/releases/tag/v1.1.0)

Cerró el 1 de noviembre de 2026.

| Historia | Qué hace | Responsable | Pull Requests |
|---|---|---|---|
| HU00 · Ingreso a la plataforma | Ingreso como Visitante o Coleccionista, selector de coleccionista, sesión por rol y pantalla de inicio | Daniel Nuñez | [#64](https://github.com/dnunezd/IngSoftAppMobile/pull/64), [#75](https://github.com/dnunezd/IngSoftAppMobile/pull/75) |
| HU01 · Catálogo de álbumes | Álbumes en orden alfabético con portada, nombre y género; carga, vacío, error con "Reintentar", deslizar para actualizar y paso al detalle | Jeffrey Díaz | [#72](https://github.com/dnunezd/IngSoftAppMobile/pull/72), [#78](https://github.com/dnunezd/IngSoftAppMobile/pull/78) |
| HU02 · Detalle de álbum | Portada, datos del álbum, artistas, canciones, comentarios y promedio; mensaje propio para un álbum inexistente (404) | Lorena Nossa | [#71](https://github.com/dnunezd/IngSoftAppMobile/pull/71), [#77](https://github.com/dnunezd/IngSoftAppMobile/pull/77), [#80](https://github.com/dnunezd/IngSoftAppMobile/pull/80) |
| HU03 · Listado de artistas | Músicos y bandas en orden alfabético con búsqueda por nombre (sin distinguir mayúsculas ni tildes) | Michael Mora | [#66](https://github.com/dnunezd/IngSoftAppMobile/pull/66), [#76](https://github.com/dnunezd/IngSoftAppMobile/pull/76) |

Base técnica del sprint:
- `ServiceAdapter` como único punto de acceso al backend ([#68](https://github.com/dnunezd/IngSoftAppMobile/pull/68)).
- Soporte desde Android 6.0 (API 23).
- CI con lint y pruebas unitarias.
- Backend desplegado de los tutores como valor por defecto ([#79](https://github.com/dnunezd/IngSoftAppMobile/pull/79)).

Evidencia de pruebas en [`docs/pruebas/sprint-1/`](docs/pruebas/sprint-1/).

### Sprint 2 · en curso

Va del 2 al 15 de noviembre de 2026. Su primer release será **`v2.0.0` – Sprint 2**. Estado al 8 de noviembre, al cierre de la semana 1: el [milestone](https://github.com/dnunezd/IngSoftAppMobile/milestone/3) tiene 8 issues cerrados y 12 abiertos.

| Historia | En `develop` | En revisión | Pendiente |
|---|---|---|---|
| HU04 · Detalle de artista (Daniel Nuñez) | Datos del músico o la banda, integrantes, álbumes con paso a su detalle y premios ([#82](https://github.com/dnunezd/IngSoftAppMobile/pull/82)) | E2E `FN04-ESC01` a `ESC07` ([#84](https://github.com/dnunezd/IngSoftAppMobile/pull/84)) | Wiki y mockups (T04-6) |
| HU07 · Crear un álbum (Jeffrey Díaz) | Parte 1: formulario solo para el Coleccionista, validación, selector de fecha, envío con reintento y álbum nuevo marcado en el catálogo ([#83](https://github.com/dnunezd/IngSoftAppMobile/pull/83)) | E2E `FN07-ESC01` a `ESC07` ([#85](https://github.com/dnunezd/IngSoftAppMobile/pull/85)) | Wiki y mockups (T07-7) y parte 2 de desempeño |
| HU10 · Agregar artistas favoritos (Lorena Nossa) | — | Botón de favorito en el detalle del artista, con confirmación y errores ([#86](https://github.com/dnunezd/IngSoftAppMobile/pull/86)) | E2E (T10-5), wiki y mockups (T10-7) |

- **Regresión:** el equipo decidió no hacer pruebas de regresión en este sprint (T07-6 y T10-6).
- **Pruebas que fallaban en API 23:** se corrigieron dos.
  - En `FN03-ESC07`, el buscador quedaba enfocado al girar y el teclado reenviaba el texto (`ru` → `ruru`); ahora la prueba quita el foco antes de girar.
  - En `CrearAlbumE2ETest` (en #85), el teclado tapaba el siguiente campo; ahora se cierra después de escribir.
- **Verificación de `develop`** (8 de noviembre, contra BackVynils local) en un Motorola Edge 30 (Android 14), un emulador Pixel 8 (API 34) y un emulador Nexus 5X (API 23):

  | Prueba | Resultado |
  |---|---|
  | Lint | 0 errores, 48 avisos esperados |
  | Unitarias | 220 sin fallos |
  | Instrumentadas | 130 sin fallos en cada dispositivo, corridas en paralelo |
  | `ExampleInstrumentedTest` | Sin fallos en los dos emuladores, corrida aparte porque compara el paquete de la app |
- **Pendiente para cerrar la iteración 2:**
  - Optimizar las cinco historias de las iteraciones 1 y 2: micro-optimizaciones con el Lint de Android, corrutinas para evitar ANR y menor consumo de memoria.
  - Perfilarlas en al menos tres dispositivos físicos.
  - Generar el release `v2.0.0` con su APK.
  - Actualizar la wiki.

## Versión y APK

La versión publicada es `1.1.0` (`versionCode` 2), en el release [`v1.1.0`](https://github.com/dnunezd/IngSoftAppMobile/releases/tag/v1.1.0) sobre `main`, con el APK `vinilos-v1.1.0.apk` adjunto. El identificador de la app es `com.example.vinilos` y funciona desde Android 6.0 (API 23). Para instalarla sin compilar, descarga el APK del release en el teléfono y ábrelo.

El primer release del Sprint 2 será **`v2.0.0` – Sprint 2**. `app/build.gradle.kts` ya tiene `versionName` `2.0.0` y `versionCode` 3. Los releases siguen el formato `v<versión> – Sprint <n>`, como `v1.1.0 – Sprint 1`.

Para cada release:

1. Sube `versionCode` en 1 y deja `versionName` igual al tag (por ejemplo `2.0.0` para `v2.0.0`), ambos en `app/build.gradle.kts`.
2. Genera el APK con `./gradlew assembleDebug`. Queda en `app/build/outputs/apk/debug/app-debug.apk` y apunta al backend desplegado. La variante release no está firmada y Android no la instala.
3. Adjúntalo al release como `vinilos-v<versión>.apk`.

Antes de generarlo, comprueba que el backend desplegado responda (ver [opción A](#opción-a-backend-desplegado-por-los-tutores)). El 8 de noviembre de 2026 respondía `500`. Si sigue caído, el APK se puede compilar con `-PbaseUrl=http://localhost:3000/`, pero solo funcionará con BackVynils local y `adb reverse tcp:3000 tcp:3000`.

## Requisitos

| Herramienta | Versión |
|---|---|
| JDK | 17. Gradle 8.7 no arranca con un JDK más nuevo (como el que trae Android Studio): define `JAVA_HOME` con un JDK 17 |
| Android Studio | Compatible con AGP 8.6.0 (Koala Feature Drop 2024.1.2 o superior) |
| Android SDK | Plataforma 34 (`minSdk` 23, `compileSdk` y `targetSdk` 34), con `adb` en `platform-tools` |
| Docker | Docker Desktop o Docker Engine con Compose, solo para el backend local |
| Dispositivo | Físico o emulador con Android 6.0 (API 23) o superior |

Stack: Kotlin 1.9, Jetpack Compose con Material 3, MVVM + Repository, Retrofit + OkHttp, Coil, Navigation Compose y corrutinas. Gradle 8.7 viene con el wrapper.

## Puesta en marcha

Los comandos se ejecutan desde la raíz del repositorio y están escritos para macOS, Linux o Git Bash. En PowerShell usa `.\gradlew.bat`, `curl.exe` y pon entre comillas los argumentos `-P` cuyo nombre lleva puntos. La ruta del repositorio no debe tener tildes (ver [problemas frecuentes](#problemas-frecuentes)).

### 1. Abrir el proyecto

Abre la raíz del repositorio en Android Studio y deja que sincronice Gradle. Si Gradle dice `SDK location not found`, define `ANDROID_HOME` o crea `local.properties` (no se versiona) con `sdk.dir=<ruta del SDK>`.

### 2. Elegir el backend

#### Opción A: backend desplegado por los tutores

Es el valor por defecto (`https://backvynils-q6yc.onrender.com/`). No requiere Docker ni `adb reverse`. Está en un plan gratuito y puede estar dormido o caído; compruébalo antes:

```bash
curl https://backvynils-q6yc.onrender.com/collectors
```

Si no responde con datos, usa la opción B.

#### Opción B: backend local con Docker

Con Docker Desktop abierto, en la carpeta del repositorio de [BackVynils](https://github.com/TheSoftwareDesignLab/BackVynils):

```bash
docker compose up -d --build
curl http://localhost:3000/collectors
```

La respuesta debe incluir a **Manolo Bellon** y **Jaime Monsalve** (ids 100 y 101): las pruebas E2E dependen de esos datos de ejemplo. Si hace falta:

- **PostgreSQL no arranca** (el compose usa `postgres` sin versión, y la más reciente no abre los datos creados con la 13): crea `docker-compose.override.yml` junto al compose y vuelve a levantarlo.

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

La URL sale de la propiedad de Gradle `baseUrl` y queda fija en la APK al compilar.

| Dónde corres la app | Qué hacer |
|---|---|
| Cualquier dispositivo, backend desplegado | Nada: es el valor por defecto |
| Emulador, backend local | Compila con `-PbaseUrl=http://10.0.2.2:3000/` (`10.0.2.2` es el computador visto desde el emulador) |
| Dispositivo físico, backend local | Ejecuta `adb reverse tcp:3000 tcp:3000` (repítelo si se reconecta) y compila con `-PbaseUrl=http://localhost:3000/` |

El HTTP sin cifrar solo está permitido hacia `localhost` y `10.0.2.2` (`app/src/main/res/xml/network_security_config.xml`).

### 4. Compilar e instalar

```bash
./gradlew assembleDebug     # APK en app/build/outputs/apk/debug/
./gradlew installDebug      # instala en el dispositivo conectado
```

Agrega `-PbaseUrl=...` para usar el backend local. Si hay varios dispositivos conectados, elige uno con `ANDROID_SERIAL=<serial>`.

## Pruebas

| Tipo | Comando | Cantidad | Necesita |
|---|---|---|---|
| Lint y unitarias (lo que corre la CI) | `./gradlew lint testDebugUnitTest --continue` | 220 unitarias | Nada |
| Instrumentadas sin E2E | `./gradlew connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.notClass=com.example.vinilos.IngresoE2ETest,com.example.vinilos.CatalogoE2ETest,com.example.vinilos.AlbumDetalleE2ETest,com.example.vinilos.navigation.AlbumDetalleBackendRealTest,com.example.vinilos.ArtistasE2ETest"` | 112 | Dispositivo |
| Todas las instrumentadas (con E2E) | `./gradlew connectedDebugAndroidTest -PbaseUrl=http://localhost:3000/` | 131 (112 + 19 E2E) | Dispositivo, backend local con datos y `adb reverse` en un teléfono |

- El dispositivo debe estar **desbloqueado y con la pantalla encendida** durante toda la corrida.
- Pasa `-PbaseUrl` también a las instrumentadas: la URL queda fija en la APK de prueba. Las E2E que crean datos deben correr contra el backend local.
- Para una sola clase: `"-Pandroid.testInstrumentationRunnerArguments.class=com.example.vinilos.RotacionTest"` (instrumentadas) o `--tests "com.example.vinilos.viewmodel.CollectorsViewModelTest"` (unitarias).
- Los reportes quedan en `app/build/reports/` (`tests/`, `androidTests/` y `lint-results-debug.html`). El lint debe quedar con 0 errores; los 48 avisos son esperados.
- La [CI](.github/workflows/ci.yml) corre lint y unitarias con JDK 17 en cada Pull Request y en cada push a `develop` y `main`. Las instrumentadas necesitan un dispositivo y se corren a mano antes de integrar.

### Pruebas E2E y su evidencia

Las E2E recorren la app contra el backend real. Cada escenario se nombra con el id de su historia (`FN03-ESC07`) y guarda en el dispositivo capturas, un informe y un PDF en `.../files/e2e-evidencia/<escenario>/`. Esa carpeta no se versiona; los PDF que se entregan se copian a `docs/pruebas/<sprint>/<HU>/e2e/`.

| Clase | Historia | Escenarios |
|---|---|---|
| `IngresoE2ETest` | HU00 | `FN00-ESC01` a `ESC03`, más una prueba de navegación |
| `CatalogoE2ETest` | HU01 | `FN01-ESC01` a `ESC05` |
| `AlbumDetalleE2ETest` | HU02 | `FN02-ESC01` |
| `AlbumDetalleBackendRealTest` | HU02 | `FN02-ESC02` y `ESC03` |
| `ArtistasE2ETest` | HU03 | `FN03-ESC01` a `ESC07` |
| `ArtistaDetalleE2ETest` | HU04 | `FN04-ESC01` a `ESC07` |

Total: **26 pruebas E2E**. Para correrlas y traer la evidencia (con `connectedDebugAndroidTest` la app se desinstala al terminar y la evidencia se pierde):

```bash
./gradlew installDebug installDebugAndroidTest -PbaseUrl=http://localhost:3000/
adb reverse tcp:3000 tcp:3000                    # solo en un teléfono
adb shell rm -rf /storage/emulated/0/Android/data/com.example.vinilos/files/e2e-evidencia
adb shell am instrument -w -e class com.example.vinilos.IngresoE2ETest,com.example.vinilos.CatalogoE2ETest,com.example.vinilos.AlbumDetalleE2ETest,com.example.vinilos.navigation.AlbumDetalleBackendRealTest,com.example.vinilos.ArtistasE2ETest,com.example.vinilos.ArtistaDetalleE2ETest com.example.vinilos.test/androidx.test.runner.AndroidJUnitRunner
adb pull /storage/emulated/0/Android/data/com.example.vinilos/files/e2e-evidencia ./e2e-evidencia
```

Debe terminar en `OK (26 tests)`. En Git Bash antepone `MSYS_NO_PATHCONV=1` a los `adb` que llevan rutas del dispositivo.

## Problemas frecuentes

| Síntoma | Causa y solución |
|---|---|
| `No compose hierarchies found in the app` | El dispositivo está bloqueado o con la pantalla apagada. |
| "No pudimos cargar los coleccionistas" o fallan las E2E | Backend caído, falta `adb reverse` o la app se instaló con otra `baseUrl`. Revisa `curl http://localhost:3000/collectors` y reinstala con `-PbaseUrl`. |
| El backend desplegado responde `{"statusCode":500,"message":"Internal server error"}` | Está caído o dormido. Usa el [backend local](#opción-b-backend-local-con-docker). |
| Una E2E que escribe en un campo falla solo en API 23 (toque sin efecto o texto repetido al girar) | El teclado de API 23 sigue abierto o conectado al campo. Ciérralo después de escribir (`Espresso.closeSoftKeyboard()`) y quita el foco antes de girar. |
| `Cleartext HTTP traffic not permitted` | El host del backend no está en `network_security_config.xml`. |
| `more than one device/emulator` | Elige un dispositivo con `ANDROID_SERIAL`. |
| `Your project path contains non-ASCII characters` (Windows) | La ruta del repositorio tiene tildes. Clónalo en una ruta sin ellas (por ejemplo `C:\dev\IngSoftAppMobile`); `android.overridePathCheck=true` no basta, porque las unitarias fallan con `ClassNotFoundException`. |
| Gradle falla al arrancar y solo muestra una versión de Java (por ejemplo `25.0.3`) | Gradle 8.7 no soporta ese JDK: define `JAVA_HOME` con un JDK 17. |

## Estructura y arquitectura

```
app/src/main/java/com/example/vinilos/
  data/        model, remote (ServiceAdapter, interfaces Retrofit y DTO), repository
  viewmodel/   SessionViewModel y un ViewModel con su estado por pantalla
  ui/          pantallas Compose por funcionalidad y tema
  navigation/  VinilosNavHost: grafo de navegación
app/src/test/ y app/src/androidTest/   pruebas unitarias e instrumentadas
docs/pruebas/  evidencia de pruebas por sprint e historia
openspec/      specs vigentes (specs/), cambios en curso (changes/) y archivados (changes/archive/)
```

**MVVM** con capa de datos por **Repository**: la vista dibuja el estado que expone el `ViewModel` y le envía los eventos; los repositorios hablan con el backend solo a través del `ServiceAdapter`, que construye una sola vez el cliente de red y traduce los errores de red y del servidor a `ErrorDeCarga`. Los diagramas están en la página de cada sprint en la wiki.

## Flujo de trabajo

- Gitflow con `main`, `develop` y `feature/*`: cada historia se integra a `develop` por Pull Request (*squash and merge*, dos revisores y `Closes #n`), y cada release se etiqueta sobre `main`.
- Commits y títulos de PR con Conventional Commits en español (`feat(catalogo): ...`).
- El comportamiento se especifica con [OpenSpec](https://github.com/Fission-AI/OpenSpec) en `openspec/`, con requisitos y escenarios verificables por capacidad.
