# Guía: perfilamiento automatizado

`scripts/perfilamiento/Perfilar.ps1` hace la [guía manual](guia-manual.md) sin tocar el teléfono:
maneja la app con los mismos pasos de cada HU (una acción y 5 s), reinicia la app sin datos antes de
cada grabación, anota el segundo de la grabación en que ocurre cada paso, toma la captura de Live
Telemetry y arma `analisis.md`. Lo manual es iniciar y detener las tareas del Profiler y exportar lo
grabado: el script se detiene con instrucciones exactas cuando toca, copia la carpeta de destino en
el portapapeles y comprueba que cada archivo quedó guardado.

## Qué se necesita

- Windows con PowerShell 7, `adb` (el script lo busca en el SDK si no está en el `PATH`) y un JDK 17 o 21
  en `JAVA_HOME` (si está vacío, usa `%USERPROFILE%\.jdks\jbr-21.0.11` cuando existe).
- Docker Desktop corriendo y BackVynils arriba (`docker compose up -d` en su carpeta, que se espera en
  `..\BackVynils`; si está en otra, `-RutaBackend <carpeta>`).
- El teléfono por USB, desbloqueado, en vertical y con la pantalla encendida. Con varios dispositivos,
  `-Serial <serial>`.
- Android Studio abierto con este proyecto, en esta rama.

## Pasos

```powershell
.\scripts\perfilamiento\Perfilar.ps1 -Accion Preparar -Version v2.1.0   # compila e instala
.\scripts\perfilamiento\Perfilar.ps1 -Accion Validar  -Version v2.1.0   # revisa que todo esté listo
.\scripts\perfilamiento\Perfilar.ps1 -Accion Ejecutar -Version v2.1.0 -Fase starter -Escenarios hu01
```

El script hace pausas que esperan respuesta: hay que correrlo en una terminal interactiva (PowerShell
o la terminal de Android Studio), no en segundo plano.

- `-Version` es el tag o commit que se perfila. Sin ella, se perfila el código de la rama actual.
- `-Fase` es la subcarpeta de resultados: `starter` (por defecto) para la primera fase, la línea base
  sin micro-optimizaciones. La segunda fase se corre con su propio nombre y la versión que la contenga.
- `-Propietario` es quien perfila (primera parte de la carpeta). Por defecto, el primer nombre de
  `git config user.name` en minúsculas y sin tildes.
- `-Escenarios hu01,hu02,...` elige las HU (por defecto las seis) y `-Grabaciones telemetria` o
  `asignaciones` una sola de las dos grabaciones.
- `-SinStudio` corre los recorridos sin pausas de Android Studio: sirve para probar que la app se deja
  manejar antes de grabar.

## Qué hace `Preparar`

1. Para un tag, saca su código en `build\perfilamiento\fuente-<versión>` (un `git worktree` aparte; la
   rama actual no cambia) y, si le faltan, le aplica los parches de `scripts\perfilamiento\parches`:
   - `compilacion-perf.patch`: el tipo de compilación `perf` (como `release`, firmado con la llave de
     depuración, paquete `com.example.vinilos.perf`, depurable).
   - `tags-ui-automator.patch`: `testTagsAsResourceId` en `MainActivity`, para que UI Automator
     encuentre los elementos por su `testTag`.

   Ninguno cambia la lógica de la app.
2. Compila la app apuntando a `http://localhost:3000/` en
   `build\perfilamiento\vinilos-<versión>-perf-depurable.apk`, y los recorridos (`:perfilamiento`,
   siempre de esta rama) en `build\perfilamiento\vinilos-recorridos.apk`.
3. Instala los dos y deja `adb reverse tcp:3000 tcp:3000`.

## Qué hace `Ejecutar` por cada HU

| Grabación | Antes | En Android Studio (pausa guiada) | Archivos |
|---|---|---|---|
| 1 | Reinicio sin datos (`force-stop`, `pm clear`, `am start`) | View Live Telemetry con **Now** y Network Inspector en App Inspection | `live-telemetry.png` (captura del script), `connections.json` (Network Inspector > **Export Connections**, antes de detener), `live-view.asdb` (tras **Stop Recording**: **Past Recordings** > **Export recording**) |
| 2 | Reinicio sin datos | Track Memory Consumption (Java/Kotlin Allocations), **Full** | `memory.asdb` (tras **Stop Recording**: **Past Recordings** > **Export recording**) |

En cada grabación:

1. Cuando la tarea ya graba, el script pide escribir lo que marca el cronómetro **Recording**
   (`mm:ss`). Con eso convierte la hora de cada paso en el segundo de la grabación (±2 s).
2. Corre el recorrido de la HU (`perfilamiento/.../Recorridos.kt`): 5 s de reposo en el ingreso y
   luego los pasos de la guía, cada uno seguido de 5 s. Si el paso abre otra pantalla, los 5 s cuentan
   desde que aparece.
3. Guía las exportaciones: deja la carpeta de destino en el portapapeles, dice el nombre del archivo y
   no sigue hasta encontrarlo (o hasta que se salte con `s`).
4. Escribe en `analisis.md` el bloque de esa grabación: la tabla de pasos con la columna **Seg.**, la
   duración y lo medido por línea de comandos (memoria PSS al empezar y al terminar, cuadros
   dibujados y lentos, percentil 90).

Los `.asdb` se vuelven a abrir en Android Studio con **Past Recordings** > **Import recording**.

Datos del backend:

- Antes de cada grabación de la HU10 quita los favoritos del coleccionista 100, para que Queen empiece
  sin serlo. Lo hace en la base del Docker local (`docker compose ps -q db` + `psql`), porque el
  `DELETE` de favoritos de BackVynils responde 204 sin borrar nada.
- Después de cada grabación de la HU07 borra por la API solo los álbumes `Perf*` ("Perf 1" en la
  grabación 1, "Perf 2" en la 2), para que ambas grabaciones vean el mismo catálogo. Si el filtro
  abarcara todo el catálogo, no borra nada.

## Qué deja en `docs/performance/resultados/`

```
<propietario>-<dispositivo>/        p. ej. jeffrey-motorola-edge-30
└── HU01/ … HU10/
    └── <fase>/                      starter en la primera fase
        ├── live-telemetry.png
        ├── live-view.asdb
        ├── connections.json
        ├── memory.asdb
        └── analisis.md
```

`analisis.md` trae el dispositivo y la versión, un bloque por grabación, la lista de "Qué analizar"
de la guía para esa HU y las secciones de hallazgos y pasos con más carga, que se completan a mano
cruzando los segundos con lo grabado. Repetir una grabación reemplaza solo su bloque: lo escrito a
mano se conserva.

## A tener en cuenta

- Los recorridos usan los datos de ejemplo de BackVynils: álbum 100 (Buscando América), músico 100
  (Rubén Blades Bellido de Luna), banda 101 (Queen) y coleccionista 100 (Manolo Bellon).
- UI Automator corre en su propio proceso, pero para encontrar los elementos la app arma su árbol de
  accesibilidad: en Allocations pueden aparecer objetos `AccessibilityNodeInfo` que no son de la app.
- En la HU03 y la HU07 el texto se escribe letra por letra (un cambio de texto por letra), para que se
  vea lo que cuesta cada cambio. Se pone directo en el campo y no con teclas simuladas, porque el
  teclado del teléfono las autocorrige ("perfilamiento" quedaba "Perfila miento") y las vuelve muy lentas.
