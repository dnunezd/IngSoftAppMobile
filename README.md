# IngSoftAppMobile · Vinilos

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

## Requisitos

| Herramienta | Versión |
|---|---|
| JDK | 17 |
| Android Studio | Una versión compatible con AGP 8.6.0 (Koala Feature Drop, 2024.1.2, o superior) |
| Android SDK | Plataforma 34 (`minSdk` 26, `compileSdk` y `targetSdk` 34) |
| Gradle | 8.7, ya incluido con el wrapper (`gradlew`) |
| Docker | Para levantar el backend BackVynils |
| Dispositivo Android | Físico o emulador (API 26 o superior) |

Stack: Kotlin 1.9.0, Jetpack Compose (BOM 2024.04.01) con Material 3, MVVM + Repository, Retrofit 2.9.0 + OkHttp 4.12.0, Navigation Compose 2.7.7 y corrutinas 1.8.0.

## Puesta en marcha

Los comandos se ejecutan desde la raíz del repositorio. En Windows con PowerShell o CMD usa `gradlew.bat` en lugar de `./gradlew`.

### 1. Abrir el proyecto

Abre la **raíz del repositorio** en Android Studio y deja que sincronice Gradle. Android Studio crea `local.properties` con la ruta del SDK. Si compilas solo por terminal y Gradle dice `SDK location not found`, crea ese archivo (no se versiona):

```properties
sdk.dir=C\:\\Users\\TU_USUARIO\\AppData\\Local\\Android\\Sdk
```

o define la variable de entorno `ANDROID_HOME`.

### 2. Levantar el backend (BackVynils)

En la carpeta del repositorio de BackVynils:

```bash
docker compose up -d --build
curl http://localhost:3000/collectors
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
- **Si `/collectors` responde `[]`**, carga los datos de ejemplo (el archivo trae una marca BOM al inicio que `psql` no tolera, por eso el `sed`):

  ```bash
  sed '1s/^\xEF\xBB\xBF//' sql/data.sql | docker compose exec -T db psql -U postgres -d vinyls
  ```

### 3. Conectar la app con el backend

La URL base sale de la propiedad de Gradle `baseUrl` y vale `http://10.0.2.2:3000/` por defecto.

| Dónde corres la app | Qué hacer |
|---|---|
| **Emulador** | Nada: `10.0.2.2` es el `localhost` de tu computador visto desde el emulador. |
| **Dispositivo físico** | Ejecuta `adb reverse tcp:3000 tcp:3000` y compila con `-PbaseUrl=http://localhost:3000/`. |

Para no repetir `-PbaseUrl` en cada comando, guárdala una vez en `~/.gradle/gradle.properties` (en tu usuario, fuera del repositorio):

```properties
baseUrl=http://localhost:3000/
```

El tráfico HTTP sin cifrar solo está permitido hacia `localhost` y `10.0.2.2` (`app/src/main/res/xml/network_security_config.xml`). Si usas la IP de tu computador en la red local, agrégala allí o la app no podrá conectarse (`Cleartext HTTP traffic not permitted`).

### 4. Compilar e instalar

```bash
./gradlew assembleDebug                                     # APK en app/build/outputs/apk/debug/
./gradlew installDebug -PbaseUrl=http://localhost:3000/     # instala en el dispositivo conectado
```

Si hay más de un dispositivo conectado (`adb devices`), elige uno con la variable `ANDROID_SERIAL`:

```bash
ANDROID_SERIAL=<serial> ./gradlew installDebug              # PowerShell: $env:ANDROID_SERIAL="<serial>"
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
| Unitarias | `./gradlew testDebugUnitTest` | 43 | Nada (corren en el computador) |
| Lint | `./gradlew lint` | | Nada |
| Instrumentadas sin E2E | `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.notClass=com.example.vinilos.IngresoE2ETest` | 45 | Dispositivo desbloqueado |
| Todas las instrumentadas (con E2E) | `./gradlew connectedDebugAndroidTest -PbaseUrl=http://localhost:3000/` | 50 (1 se omite) | Dispositivo desbloqueado, backend con datos y `adb reverse` |
| Todo | `./gradlew lint testDebugUnitTest connectedDebugAndroidTest -PbaseUrl=http://localhost:3000/` | | Lo de las dos filas anteriores |

Las unitarias corren en pocos segundos con la compilación ya hecha (la primera vez, con Gradle descargando dependencias, tardan más). Las instrumentadas instalan la app y las pruebas en el dispositivo y tardan alrededor de un minuto.

### Qué cubre cada prueba

| Ubicación | Qué verifica |
|---|---|
| `src/test` · `data/` | `CollectorRepository` con `MockWebServer`: datos, lista vacía, 404, 500, sin conexión, tiempo de espera, JSON inválido, elementos incompletos y cancelación; el DTO ignora los campos que no usa |
| `src/test` · `viewmodel/` | `CollectorsViewModel` (estados cargando, éxito, vacío y error, reintento, búsqueda sin distinguir mayúsculas ni tildes) y `SessionViewModel` (rol, coleccionista activo, acciones de coleccionista) |
| `src/test` · `ui/ingreso/` | Iniciales del avatar |
| `src/androidTest` · `ui/` | Pantallas de Compose en cada estado: ingreso, selector de coleccionista y pantalla de inicio (textos literales de la HU00, callbacks, saludo según el perfil) |
| `src/androidTest` · `navigation/` | El grafo completo con un repositorio falso: cada transición, "Atrás", cambio de perfil y sesión |
| `src/androidTest` · `RotacionTest` | Recrea la `Activity` (girar la pantalla): se conservan lista, búsqueda y sesión sin repetir la consulta |
| `src/androidTest` · `IngresoE2ETest` | **E2E contra BackVynils real**: Visitante, Coleccionista con la lista real, buscador, "Atrás" y cambio de perfil (más la de recuperación, ver abajo) |

### Ejecutar solo una parte

```bash
# Una clase
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.example.vinilos.ui.inicio.PantallaInicioTest

# Un método
./gradlew connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.example.vinilos.ui.inicio.PantallaInicioTest#sinSesionElSaludoEsElDelVisitante"

# Unitarias de una clase
./gradlew testDebugUnitTest --tests "com.example.vinilos.viewmodel.CollectorsViewModelTest"
```

### E2E de recuperación (con el backend caído)

Comprueba que, con la API detenida, aparece "No pudimos cargar los coleccionistas" con la opción de ingresar como Visitante, y que **"Reintentar" recupera la lista al levantar la API**. Solo corre con `e2eRecuperacion=true` porque necesita que alguien levante el backend mientras corre:

1. Detén la API (en la carpeta de BackVynils): `docker compose stop web`.
2. Lanza la prueba:

   ```bash
   ./gradlew connectedDebugAndroidTest -PbaseUrl=http://localhost:3000/ \
     -Pandroid.testInstrumentationRunnerArguments.e2eRecuperacion=true \
     "-Pandroid.testInstrumentationRunnerArguments.class=com.example.vinilos.IngresoE2ETest#conElBackendCaidoSeVeElErrorYReintentarRecuperaLaLista"
   ```

3. Cuando `adb logcat -s E2E:I` muestre `ERROR_VISIBLE`, levanta la API: `docker compose start web`.
4. La prueba reintenta sola hasta ver la lista (hasta 2 minutos) y registra `RECUPERADO`.

### Antes de correr las instrumentadas

- El dispositivo debe estar **desbloqueado y con la pantalla encendida** durante toda la corrida. Si se bloquea, las pruebas fallan con `No compose hierarchies found in the app`. Para una corrida larga, sube el tiempo de pantalla: `adb shell settings put system screen_off_timeout 600000`.
- Para las E2E: backend arriba con los datos de ejemplo y, en dispositivo físico, `adb reverse tcp:3000 tcp:3000`. Pasa `-PbaseUrl` también a `connectedDebugAndroidTest`: la URL queda fija en la APK que se instala para la prueba.

### Reportes

| Reporte | Ruta |
|---|---|
| Unitarias | `app/build/reports/tests/testDebugUnitTest/index.html` |
| Instrumentadas | `app/build/reports/androidTests/connected/debug/index.html` |
| Lint | `app/build/reports/lint-results-debug.html` |

Referencia actual del lint: 0 errores y 47 avisos, todos de la plantilla de Android Studio (por ejemplo `targetSdk` y versiones de dependencias).

### Problemas frecuentes

| Síntoma | Causa y solución |
|---|---|
| `No compose hierarchies found in the app` | El dispositivo está bloqueado o con la pantalla apagada. Desbloquéalo y repite. |
| La app muestra "No pudimos cargar los coleccionistas" o las E2E fallan | Backend caído, falta `adb reverse`, o la app se instaló con otra `baseUrl`. Revisa `curl http://localhost:3000/collectors` y vuelve a instalar con `-PbaseUrl`. |
| `more than one device/emulator` | Hay varios dispositivos conectados: usa `ANDROID_SERIAL`. |
| `SDK location not found` | Falta `local.properties` o `ANDROID_HOME`. |
| `Permission denied` al ejecutar `./gradlew` | `chmod +x gradlew` (en el repositorio ya va con permiso de ejecución). |
| `Cleartext HTTP traffic not permitted` | El host del backend no está en `network_security_config.xml`. |

## Estructura

```
app/src/main/java/com/example/vinilos/
  data/        model (Collector, Session), remote (Retrofit: CollectorApi, DTO), repository
  viewmodel/   SessionViewModel, CollectorsViewModel y sus estados
  ui/          ingreso, inicio, catalogo (provisional), artistas (provisional), theme
  navigation/  VinilosNavHost: grafo de navegación
app/src/main/res/  textos (strings.xml), vectores del Figma (drawable/) e imágenes (drawable-nodpi/)
app/src/test/ y app/src/androidTest/   pruebas unitarias e instrumentadas
openspec/      especificaciones (specs/) y cambios archivados (changes/archive/)
```

Arquitectura: **MVVM** con capa de datos por **Repository**. La vista dibuja el estado que expone el `ViewModel` y le envía los eventos; solo el repositorio habla con la API y traduce los errores de red y del servidor.

## Flujo de trabajo

- Gitflow: ramas `main`, `develop` y `feature/*`. Cada historia se trabaja en una rama `feature/` y se integra a `develop` con un Pull Request (*squash and merge*, revisado por al menos dos integrantes, con `Closes #n` en la descripción).
- El comportamiento del sistema se documenta con [OpenSpec](https://github.com/Fission-AI/OpenSpec) en `openspec/`: cada capacidad tiene su spec con requisitos y escenarios verificables.
- La definición de terminado de cada sprint está en la [wiki](https://github.com/dnunezd/IngSoftAppMobile/wiki).
