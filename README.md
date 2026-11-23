# IngSoftAppMobile · Vinilos

[![CI](https://github.com/dnunezd/IngSoftAppMobile/actions/workflows/ci.yml/badge.svg)](https://github.com/dnunezd/IngSoftAppMobile/actions/workflows/ci.yml)

Proyecto educativo del **equipo 9** del curso de Ingeniería de software para aplicaciones móviles: Daniel Alejandro Nuñez Donado · Jeffrey Alexander Díaz Pérez · Michael Andrés Mora Poveda · Ivette Lorena Nossa Ruiz.

**Vinilos** es una app Android para consultar y administrar un catálogo de álbumes musicales: el Visitante explora álbumes y artistas, y el Coleccionista aporta información al catálogo. Consume **BackVynils**, una API REST ya existente (NestJS + PostgreSQL).

La documentación del proyecto (historias de usuario, arquitectura y estrategia de pruebas de cada sprint, actas y diseño en Figma) está en la [wiki](https://github.com/dnunezd/IngSoftAppMobile/wiki).

## Historias por sprint

### Sprint 1 · release [`v1.1.0`](https://github.com/dnunezd/IngSoftAppMobile/releases/tag/v1.1.0)

Cerró el 1 de noviembre de 2026.

| Historia                       | Qué hace                                                                                                                                   | Responsable  | Pull Requests                                                                                                                                                                      |
|--------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------|--------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| HU00 · Ingreso a la plataforma | Ingreso como Visitante o Coleccionista, selector de coleccionista, sesión por rol y pantalla de inicio                                     | Daniel Nuñez | [#64](https://github.com/dnunezd/IngSoftAppMobile/pull/64), [#75](https://github.com/dnunezd/IngSoftAppMobile/pull/75)                                                             |
| HU01 · Catálogo de álbumes     | Álbumes en orden alfabético con portada, nombre y género; carga, vacío, error con "Reintentar", deslizar para actualizar y paso al detalle | Jeffrey Díaz | [#72](https://github.com/dnunezd/IngSoftAppMobile/pull/72), [#78](https://github.com/dnunezd/IngSoftAppMobile/pull/78)                                                             |
| HU02 · Detalle de álbum        | Portada, datos del álbum, artistas, canciones, comentarios y promedio; mensaje propio para un álbum inexistente (404)                      | Lorena Nossa | [#71](https://github.com/dnunezd/IngSoftAppMobile/pull/71), [#77](https://github.com/dnunezd/IngSoftAppMobile/pull/77), [#80](https://github.com/dnunezd/IngSoftAppMobile/pull/80) |
| HU03 · Listado de artistas     | Músicos y bandas en orden alfabético con búsqueda por nombre (sin distinguir mayúsculas ni tildes)                                         | Michael Mora | [#66](https://github.com/dnunezd/IngSoftAppMobile/pull/66), [#76](https://github.com/dnunezd/IngSoftAppMobile/pull/76)                                                             |

Base técnica del sprint:
- `ServiceAdapter` como único punto de acceso al backend ([#68](https://github.com/dnunezd/IngSoftAppMobile/pull/68)).
- Soporte desde Android 6.0 (API 23).
- CI con lint y pruebas unitarias.
- Backend desplegado de los tutores como valor por defecto ([#79](https://github.com/dnunezd/IngSoftAppMobile/pull/79)).

Evidencia de pruebas en [`docs/pruebas/sprint-1/`](docs/pruebas/sprint-1).

### Sprint 2 · release [`v2.2.0`](https://github.com/dnunezd/IngSoftAppMobile/releases/tag/v2.2.0)

Cerró el 15 de noviembre de 2026, con los releases [`v2.0.0`](https://github.com/dnunezd/IngSoftAppMobile/releases/tag/v2.0.0), [`v2.1.0`](https://github.com/dnunezd/IngSoftAppMobile/releases/tag/v2.1.0) y [`v2.2.0`](https://github.com/dnunezd/IngSoftAppMobile/releases/tag/v2.2.0).

| Historia                          | Qué hace                                                                                                                   | Responsable  | Pull Requests                                                                                                          |
|-----------------------------------|----------------------------------------------------------------------------------------------------------------------------|--------------|------------------------------------------------------------------------------------------------------------------------|
| HU04 · Detalle de artista         | Datos del músico o la banda, integrantes, álbumes con paso a su detalle y premios                                          | Daniel Nuñez | [#82](https://github.com/dnunezd/IngSoftAppMobile/pull/82), [#84](https://github.com/dnunezd/IngSoftAppMobile/pull/84) |
| HU07 · Crear un álbum             | Formulario solo para el Coleccionista, validación, selector de fecha, envío con reintento y álbum nuevo marcado en el catálogo | Jeffrey Díaz | [#83](https://github.com/dnunezd/IngSoftAppMobile/pull/83), [#85](https://github.com/dnunezd/IngSoftAppMobile/pull/85) |
| HU10 · Agregar artistas favoritos | Botón de favorito en el detalle del artista, con confirmación, errores y bloqueo si ya es favorito                         | Lorena Nossa | [#86](https://github.com/dnunezd/IngSoftAppMobile/pull/86), [#90](https://github.com/dnunezd/IngSoftAppMobile/pull/90) |

Base técnica del sprint:
- Caché sin conexión con Room para todas las consultas de lectura ([#89](https://github.com/dnunezd/IngSoftAppMobile/pull/89), [#93](https://github.com/dnunezd/IngSoftAppMobile/pull/93)).
- Capa de datos con interfaces, dispatcher inyectado y 404 en el `ServiceAdapter` ([#92](https://github.com/dnunezd/IngSoftAppMobile/pull/92)).
- Micro-optimizaciones y perfilamiento en tres dispositivos físicos ([#95](https://github.com/dnunezd/IngSoftAppMobile/pull/95)) e inspecciones de Inspect Code ([#96](https://github.com/dnunezd/IngSoftAppMobile/pull/96)).

Evidencia de pruebas en [`docs/pruebas/sprint-2/`](docs/pruebas/sprint-2).

### Sprint 3 · en curso

Va del 16 al 28 de noviembre de 2026. Su primer release es **`v3.0.0` – Sprint 3 (Week 7)**, con HU09 y HU15; la versión final del sprint será **`v3.1.0`**. Estado al 22 de noviembre, al cierre de la semana 7: el [milestone](https://github.com/dnunezd/IngSoftAppMobile/milestone/4) tiene 9 issues cerrados y 13 abiertos.

| Historia                                     | En `develop`                                                                                                                                                                    | En revisión                                                                                                                                                       |
|----------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| HU08 · Asociar tracks con un álbum (Lorena Nossa) | —                                                                                                                                                                          | Formulario de nombre y duración `m:ss` desde el detalle del álbum ([#115](https://github.com/dnunezd/IngSoftAppMobile/pull/115)), con mejoras pedidas en la revisión |
| HU09 · Comentar un álbum (Michael Mora)      | Calificación de 1 a 5 y opinión desde el detalle del álbum, con confirmación, error con lo escrito conservado y aviso al salir sin publicar ([#112](https://github.com/dnunezd/IngSoftAppMobile/pull/112)) | E2E `FN09-ESC01` a `ESC03` ([#116](https://github.com/dnunezd/IngSoftAppMobile/pull/116))                                                                          |
| HU15 · Agregar un álbum a un artista (Jeffrey Díaz) | Ventana con búsqueda para asociar un álbum a un músico o a una banda desde su detalle, con "Ya asociado" y reintento ([#114](https://github.com/dnunezd/IngSoftAppMobile/pull/114)) | E2E `FN15-ESC01` a `ESC03` ([#116](https://github.com/dnunezd/IngSoftAppMobile/pull/116))                                                                          |

- **Accesibilidad:** hallazgos del Accessibility Scanner corregidos en las ocho pantallas de los Sprints 1 y 2, con fuente ampliada y TalkBack ([#113](https://github.com/dnunezd/IngSoftAppMobile/pull/113)).
- **Pruebas de reconocimiento:** exploración sistemática con DroidBot y pruebas aleatorias con Monkey de Android en un Motorola Edge 30, sin cierres inesperados ni ANR. Resultados en la [página del Sprint 3](https://github.com/dnunezd/IngSoftAppMobile/wiki/Sprint-3#pruebas-de-reconocimiento) de la wiki.
- **Pendiente para `v3.1.0`:** integrar HU08 y los E2E de HU08, HU09 y HU15, la regresión funcional de todas las historias, la revisión de accesibilidad de las pantallas nuevas y su perfilamiento.

Evidencia de pruebas en [`docs/pruebas/sprint-3/`](docs/pruebas/sprint-3).

## Versión y APK

La versión publicada es `2.2.0` (`versionCode` 5), en el release [`v2.2.0` – Sprint 2 (Week 6)](https://github.com/dnunezd/IngSoftAppMobile/releases/tag/v2.2.0) sobre `main`, con el APK `vinilos-v2.2.0.apk` adjunto. El identificador de la app es `com.example.vinilos` y funciona desde Android 6.0 (API 23). Para instalarla sin compilar, descarga el APK del release en el teléfono y ábrelo.

El siguiente release es **`v3.0.0` – Sprint 3 (Week 7)**: `app/build.gradle.kts` ya tiene `versionName` `3.0.0` y `versionCode` 6. La versión final del Sprint 3 será **`v3.1.0`**. Los releases siguen el formato `v<versión> – Sprint <n> (Week <m>)`, como `v2.2.0 – Sprint 2 (Week 6)`.

Para cada release:

1. Sube `versionCode` en 1 y deja `versionName` igual al tag (por ejemplo `2.0.0` para `v2.0.0`), ambos en `app/build.gradle.kts`.
2. Genera el APK con `./gradlew assembleDebug`. Queda en `app/build/outputs/apk/debug/app-debug.apk` y apunta al backend desplegado. La variante release no está firmada y Android no la instala.
3. Adjúntalo al release como `vinilos-v<versión>.apk`.

Antes de generarlo, comprueba que el backend desplegado responda (ver [opción A](#opción-a-backend-desplegado-por-los-tutores)). El 22 de noviembre de 2026 no respondía (tiempo de espera agotado), así que los APK de `v2.2.0` y `v3.0.0` se compilaron con `-PbaseUrl=http://localhost:3000/`: solo funcionan con BackVynils local y `adb reverse tcp:3000 tcp:3000`.

## Requisitos

| Herramienta    | Versión                                                                                     |
|----------------|---------------------------------------------------------------------------------------------|
| JDK            | 17 o superior (Gradle 9.9.1 y AGP 9.5.1 no arrancan con uno más viejo); compila con 17 y 21 |
| Android Studio | Compatible con AGP 9.5.1                                                                    |
| Android SDK    | Plataforma 37 (`minSdk` 23, `compileSdk` y `targetSdk` 37), con `adb` en `platform-tools`   |
| Docker         | Docker Desktop o Docker Engine con Compose, solo para el backend local                      |
| Dispositivo    | Físico o emulador con Android 6.0 (API 23) o superior                                       |

Stack: Kotlin 2.4, Jetpack Compose con Material 3, MVVM + Repository, Retrofit + OkHttp, Room, Coil, Navigation Compose y corrutinas. Gradle 9.9.1 viene con el wrapper.

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

| Dónde corres la app                       | Qué hacer                                                                                                           |
|-------------------------------------------|---------------------------------------------------------------------------------------------------------------------|
| Cualquier dispositivo, backend desplegado | Nada: es el valor por defecto                                                                                       |
| Emulador, backend local                   | Compila con `-PbaseUrl=http://10.0.2.2:3000/` (`10.0.2.2` es el computador visto desde el emulador)                 |
| Dispositivo físico, backend local         | Ejecuta `adb reverse tcp:3000 tcp:3000` (repítelo si se reconecta) y compila con `-PbaseUrl=http://localhost:3000/` |

El HTTP sin cifrar solo está permitido hacia `localhost` y `10.0.2.2` (`app/src/main/res/xml/network_security_config.xml`).

### 4. Compilar e instalar

```bash
./gradlew assembleDebug     # APK en app/build/outputs/apk/debug/
./gradlew installDebug      # instala en el dispositivo conectado
```

Agrega `-PbaseUrl=...` para usar el backend local. Si hay varios dispositivos conectados, elige uno con `ANDROID_SERIAL=<serial>`.

## Pruebas

| Tipo                                  | Comando                                                                                                                                                                                                                                                                                                                                                                                                                                  | Cantidad           | Necesita                                                            |
|---------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------|---------------------------------------------------------------------|
| Lint y unitarias (lo que corre la CI) | `./gradlew lint testDebugUnitTest --continue`                                                                                                                                                                                                                                                                                                                                                                                            | 507 unitarias      | Nada                                                                |
| Instrumentadas sin E2E                | `./gradlew connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.notClass=com.example.vinilos.IngresoE2ETest,com.example.vinilos.CatalogoE2ETest,com.example.vinilos.AlbumDetalleE2ETest,com.example.vinilos.navigation.AlbumDetalleBackendRealTest,com.example.vinilos.ArtistasE2ETest,com.example.vinilos.ArtistaDetalleE2ETest,com.example.vinilos.CrearAlbumE2ETest,com.example.vinilos.ArtistasFavoritosE2ETest"` | 224                | Dispositivo                                                         |
| Todas las instrumentadas (con E2E)    | `./gradlew connectedDebugAndroidTest -PbaseUrl=http://localhost:3000/`                                                                                                                                                                                                                                                                                                                                                                   | 265 (224 + 41 E2E) | Dispositivo, backend local con datos y `adb reverse` en un teléfono |

- El dispositivo debe estar **desbloqueado y con la pantalla encendida** durante toda la corrida.
- Pasa `-PbaseUrl` también a las instrumentadas: la URL queda fija en la APK de prueba. Las E2E que crean datos deben correr contra el backend local.
- Para una sola clase: `"-Pandroid.testInstrumentationRunnerArguments.class=com.example.vinilos.RotacionTest"` (instrumentadas) o `--tests "com.example.vinilos.viewmodel.CollectorsViewModelTest"` (unitarias).
- Los reportes quedan en `app/build/reports/` (`tests/`, `androidTests/` y `lint-results-debug.html`). El lint debe quedar con 0 errores; los 7 avisos son esperados (dependencias y Gradle).
- La [CI](.github/workflows/ci.yml) corre lint y unitarias con JDK 17 en cada Pull Request y en cada push a `develop` y `main`. Las instrumentadas necesitan un dispositivo y se corren a mano antes de integrar.

### Pruebas E2E y su evidencia

Las E2E recorren la app contra el backend real. Cada escenario se nombra con el id de su historia (`FN03-ESC07`) y guarda en el dispositivo capturas, un informe y un PDF en `.../files/e2e-evidencia/<escenario>/`. Esa carpeta no se versiona; los PDF que se entregan se copian a `docs/pruebas/<sprint>/<HU>/e2e/`.

| Clase                         | Historia | Escenarios                                                                    |
|-------------------------------|----------|-------------------------------------------------------------------------------|
| `IngresoE2ETest`              | HU00     | `FN00-ESC01` a `ESC03`, más una prueba de navegación                          |
| `CatalogoE2ETest`             | HU01     | `FN01-ESC01` a `ESC05`                                                        |
| `AlbumDetalleE2ETest`         | HU02     | `FN02-ESC01`                                                                  |
| `AlbumDetalleBackendRealTest` | HU02     | `FN02-ESC02` y `ESC03`                                                        |
| `ArtistasE2ETest`             | HU03     | `FN03-ESC01` a `ESC07`                                                        |
| `ArtistaDetalleE2ETest`       | HU04     | `FN04-ESC01` a `ESC07`                                                        |
| `CrearAlbumE2ETest`           | HU07     | `FN07-ESC01` a `ESC07` (crea y borra álbumes: correr contra BackVynils local) |
| `ArtistasFavoritosE2ETest`    | HU10     | `FN10-ESC01` a `ESC08` (agrega favoritos: correr contra BackVynils local)     |

Total: **41 pruebas E2E**. Para correrlas y traer la evidencia (con `connectedDebugAndroidTest` la app se desinstala al terminar y la evidencia se pierde):

```bash
./gradlew installDebug installDebugAndroidTest -PbaseUrl=http://localhost:3000/
adb reverse tcp:3000 tcp:3000                    # solo en un teléfono
adb shell rm -rf /storage/emulated/0/Android/data/com.example.vinilos/files/e2e-evidencia
adb shell am instrument -w -e class com.example.vinilos.IngresoE2ETest,com.example.vinilos.CatalogoE2ETest,com.example.vinilos.AlbumDetalleE2ETest,com.example.vinilos.navigation.AlbumDetalleBackendRealTest,com.example.vinilos.ArtistasE2ETest,com.example.vinilos.ArtistaDetalleE2ETest,com.example.vinilos.CrearAlbumE2ETest,com.example.vinilos.ArtistasFavoritosE2ETest com.example.vinilos.test/androidx.test.runner.AndroidJUnitRunner
adb pull /storage/emulated/0/Android/data/com.example.vinilos/files/e2e-evidencia ./e2e-evidencia
```

Debe terminar en `OK (41 tests)`. En Git Bash antepone `MSYS_NO_PATHCONV=1` a los `adb` que llevan rutas del dispositivo.

## Problemas frecuentes

| Síntoma                                                                                           | Causa y solución                                                                                                                                                                                                  |
|---------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `No compose hierarchies found in the app`                                                         | El dispositivo está bloqueado o con la pantalla apagada.                                                                                                                                                          |
| "No pudimos cargar los coleccionistas" o fallan las E2E                                           | Backend caído, falta `adb reverse` o la app se instaló con otra `baseUrl`. Revisa `curl http://localhost:3000/collectors` y reinstala con `-PbaseUrl`.                                                            |
| El backend desplegado responde `{"statusCode":500,"message":"Internal server error"}`             | Está caído o dormido. Usa el [backend local](#opción-b-backend-local-con-docker).                                                                                                                                 |
| Una E2E que escribe en un campo falla solo en API 23 (toque sin efecto o texto repetido al girar) | El teclado de API 23 sigue abierto o conectado al campo. Ciérralo después de escribir (`Espresso.closeSoftKeyboard()`) y quita el foco antes de girar.                                                            |
| `Cleartext HTTP traffic not permitted`                                                            | El host del backend no está en `network_security_config.xml`.                                                                                                                                                     |
| `more than one device/emulator`                                                                   | Elige un dispositivo con `ANDROID_SERIAL`.                                                                                                                                                                        |
| `Your project path contains non-ASCII characters` (Windows)                                       | La ruta del repositorio tiene tildes. Clónalo en una ruta sin ellas (por ejemplo `C:\dev\IngSoftAppMobile`); `android.overridePathCheck=true` no basta, porque las unitarias fallan con `ClassNotFoundException`. |
| Gradle falla al arrancar y solo muestra una versión de Java (por ejemplo `25.0.3`)                | Gradle 9.9.1 no soporta ese JDK: define `JAVA_HOME` con un JDK 17 o 21.                                                                                                                                           |

## Estructura y arquitectura

```
app/src/main/java/com/example/vinilos/
  data/        model, remote (ServiceAdapter, interfaces Retrofit y DTO), local (Room: caché sin conexión), repository
  viewmodel/   SessionViewModel y un ViewModel con su estado por pantalla
  ui/          pantallas Compose por funcionalidad y tema
  navigation/  VinilosNavHost: grafo de navegación
app/src/test/ y app/src/androidTest/   pruebas unitarias e instrumentadas
docs/pruebas/  evidencia de pruebas por sprint e historia
openspec/      specs vigentes (specs/), cambios en curso (changes/) y archivados (changes/archive/)
```

**MVVM** con capa de datos por **Repository**: la vista dibuja el estado que expone el `ViewModel` y le envía los eventos; los repositorios son interfaces con su implementación (`*RepositoryImpl`) y hablan con el backend solo a través del `ServiceAdapter`, que construye una sola vez el cliente de red y traduce los errores de red y del servidor a `ErrorDeCarga` (un 404, a `RecursoNoEncontrado`). Cada repositorio convierte y ordena en un dispatcher inyectado (`Dispatchers.Default`), fuera del hilo principal.

**Sin conexión:** los repositorios guardan en Room la última respuesta de cada consulta de lectura: catálogo y detalle de álbumes, coleccionistas, listado de artistas, detalle de cada artista abierto y favoritos confirmados. Al abrir una de esas pantallas se muestra lo guardado mientras se consulta el backend; si la consulta falla, se conserva con un aviso breve, y la pantalla de error queda solo para cuando no hay nada guardado. Las escrituras (crear un álbum, agregar un favorito, comentar un álbum y asociar un álbum a un artista) siguen necesitando conexión. El detalle está en los cambios OpenSpec `add-cache-catalogo` y `add-cache-consultas`.

**Escribir solo lo que cambió:** cada repositorio recuerda en memoria lo último que guardó y la versión (`ETag`) de cada respuesta. Si una consulta llega con la misma versión o con el mismo contenido, no escribe en Room; si cambió, guarda solo las filas nuevas o modificadas y borra las que ya no llegan. El cliente de la API tiene además una caché HTTP en `cacheDir/http`: una consulta repetida envía `If-None-Match` y, si nada cambió, el backend responde 304 sin cuerpo. Ver el cambio OpenSpec `optimizar-cache-e-imagenes`.

Los diagramas están en la página de cada sprint en la wiki.

## Flujo de trabajo

- Gitflow con `main`, `develop` y `feature/*`: cada historia se integra a `develop` por Pull Request (*squash and merge*, dos revisores y `Closes #n`), y cada release se etiqueta sobre `main`.
- Commits y títulos de PR con Conventional Commits en español (`feat(catalogo): ...`).
- El comportamiento se especifica con [OpenSpec](https://github.com/Fission-AI/OpenSpec) en `openspec/`, con requisitos y escenarios verificables por capacidad.
