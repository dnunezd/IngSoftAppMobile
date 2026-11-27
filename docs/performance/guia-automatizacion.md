# Guía: perfilamiento automatizado

`scripts/perfilamiento/Perfilar.ps1` hace la [guía manual](guia-manual.md) sin tocar el teléfono:
maneja la app con los mismos pasos de cada HU del plan de pruebas de rendimiento del Sprint 3 (HU01,
HU02, HU03, HU04, HU07, HU08, HU09, HU10 y HU15; cada paso, una acción y su espera: 5, 3 o 2 s según el plan, y 5 s en HU08, HU09 y HU15),
deja los datos de prueba del backend como los espera cada HU, reinicia la app sin datos antes de
cada grabación, anota el segundo de la grabación en que ocurre cada paso, toma las capturas de la
ventana de Android Studio y arma el análisis de cada HU. Lo manual es iniciar y detener las tareas del
Profiler, dejar a la vista lo que se captura y exportar lo
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

Hay dos fases, cada una con el código que le corresponde:

| Fase | Código | Comandos |
|---|---|---|
| `before` | Sprint 3, antes de las micro-optimizaciones: el código de esta rama (`develop` con HU08, HU09 y HU15) | `-Accion Preparar` y luego `-Accion Ejecutar -Fase before` |
| `after` | Sprint 3, después de las micro-optimizaciones | `-Accion Preparar` con el código optimizado y luego `-Accion Ejecutar -Fase after` |
| `baseline` | Sprint 2, la línea base: el release v2.1.0 | `-Accion Preparar -Version v2.1.0` y luego `-Accion Ejecutar -Version v2.1.0 -Fase baseline` |
| `latest` | Sprint 2, con las micro-optimizaciones de ese sprint | `-Accion Preparar` y luego `-Accion Ejecutar -Fase latest` |

Un tag anterior a una HU no tiene su pantalla: `baseline` solo sirve para las HU que esa versión ya
incluye (por ejemplo, v3.0.0 no tiene HU08).

```powershell
.\scripts\perfilamiento\Perfilar.ps1 -Accion Preparar                        # compila e instala la rama
.\scripts\perfilamiento\Perfilar.ps1 -Accion Validar                         # revisa que todo esté listo
.\scripts\perfilamiento\Perfilar.ps1 -Accion Ejecutar -Fase before -Escenarios hu01
```

El script hace pausas que esperan respuesta: hay que correrlo en una terminal interactiva (PowerShell
o la terminal de Android Studio), no en segundo plano.

- `-Version` es el tag o commit que se perfila. Sin ella, se perfila el código de la rama actual.
  `Preparar` anota en `build\perfilamiento\instalada.txt` de qué código es la app que instaló, y
  `Validar` no deja grabar si la app del teléfono es de otro (por ejemplo, la de baseline al grabar
  latest).
- `-Fase` es `before`, `after`, `baseline` o `latest` (por defecto `before`) y es la última carpeta de los resultados.
- `-Propietario` es quien perfila (p. ej. `jeffrey-diaz`). Por defecto, el primer nombre y el primer
  apellido de `git config user.name`, en minúsculas y sin tildes. Con un nombre de tres palabras no se
  sabe cuál es el apellido: conviene pasarlo.
- `-Dispositivo` es el teléfono en la carpeta (p. ej. `motorola-edge-30`, `samsung-s25-ultra`). Por
  defecto, el nombre comercial que informa el teléfono o, si no lo informa, su modelo.
- `-CarpetaResultados` cambia la carpeta raíz de los resultados (por defecto `docs/performance/resultados`
  de este repositorio); sirve para correr el script desde otra copia de la rama.
- `-Escenarios hu01,hu02,...` elige las HU (por defecto las nueve: `hu01`, `hu02`, `hu03`, `hu04`,
  `hu07`, `hu08`, `hu09`, `hu10` y `hu15`) y `-Grabaciones telemetria` o
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
| 1 | Reinicio sin datos (`force-stop`, `pm clear`, `am start`) | View Live Telemetry con **Now** y Network Inspector en App Inspection | `live-telemetry.png` y `connections.png` (capturas del script, con el tramo de los pasos a la vista), `connections.json` (Network Inspector > **Export Connections**, antes de detener), `live-telemetry.asdb` (tras **Stop Recording**: **Past Recordings** > **Export recording**) |
| 2 | Reinicio sin datos | Track Memory Consumption (Java/Kotlin Allocations), **Full** | `track-memory.png` (captura del script de la tabla filtrada por `com.example.vinilos` y ordenada por Allocations), `track-memory.asdb` (**Past Recordings** > **Export recording**) |

En cada grabación:

1. Cuando la tarea ya graba, el script pide escribir lo que marca el cronómetro **Recording**
   (`mm:ss`). Con eso convierte la hora de cada paso en el segundo de la grabación (±2 s).
2. Corre el recorrido de la HU (`perfilamiento/.../Recorridos.kt`): 5 s de reposo en el ingreso y
   luego los pasos de la guía, cada uno seguido de su espera (5, 3 o 2 s; en los envíos, la respuesta
   del backend, hasta 10 s). Si el paso abre otra pantalla, la espera cuenta desde que aparece.
3. Guía las exportaciones: deja la carpeta de destino en el portapapeles, dice el nombre del archivo y
   no sigue hasta encontrarlo (o hasta que se salte con `s`).
4. Escribe en el análisis de la HU el bloque de esa grabación: la tabla de pasos con la columna **Seg.**, la
   duración y lo medido por línea de comandos (memoria PSS al empezar y al terminar, cuadros
   dibujados y lentos, percentil 90).

Los `.asdb` se vuelven a abrir en Android Studio con **Past Recordings** > **Import recording**.

Datos del backend:

Antes y después de cada grabación deja los datos como los espera la HU, para que todas las
grabaciones vean lo mismo y no se acumule lo que crean los recorridos. Lo que la API no deja deshacer
lo hace en la base del Docker local (`docker compose ps -q db` + `psql`), y luego comprueba por la API
que quedó bien:

| HU | Qué deja | Cómo |
|---|---|---|
| HU07 | Sin álbumes `Perf*` ("Perf 1" en la grabación 1, "Perf 2" en la 2) | `DELETE /albums/{id}` de la API. Si el filtro abarcara todo el catálogo, no borra nada. |
| HU08 | Poeta del pueblo (101) sin la canción `Perf` | `psql` (la API no borra canciones) |
| HU09 | A Day at the Races (103) sin el comentario `Perf` | `psql` (la API no borra comentarios) |
| HU10 | Queen sin ser favorito del coleccionista 100 | `psql`: el `DELETE` de favoritos de BackVynils responde 204 sin borrar nada |
| HU15 | A Night at the Opera (102) sin asociar a Rubén Blades (100) | `psql` (la API no quita esa asociación) |

## Qué deja en `docs/performance/resultados/`

```
<propietario>--<dispositivo>/       p. ej. jeffrey-diaz--motorola-edge-30
├── dispositivo.txt                 datos del teléfono (se escribe a mano, ver el de cada carpeta)
└── HU01/ … HU15/                   HU01, HU02, HU03, HU04, HU07, HU08, HU09, HU10 y HU15
    ├── baseline/                   la fase (-Fase)
    │   ├── connections.json
    │   ├── connections.png
    │   ├── live-telemetry.asdb
    │   ├── live-telemetry.png
    │   ├── track-memory.asdb
    │   └── track-memory.png
    └── latest/                     los mismos 6 archivos
analisis/<propietario>--<dispositivo>/
├── baseline/HU01.md … HU15.md
└── latest/HU01.md … HU15.md
```

El análisis va aparte, en `analisis/`, para que cada carpeta de HU tenga solo lo grabado en Studio.
Cada `analisis/<propietario>--<dispositivo>/<fase>/HUxx.md` trae el dispositivo y la versión, un bloque por grabación, la lista de "Qué analizar"
de la guía para esa HU y las secciones de hallazgos y pasos con más carga, que se completan a mano
cruzando los segundos con lo grabado. Repetir una grabación reemplaza solo su bloque: lo escrito a
mano se conserva.

## A tener en cuenta

- Los recorridos usan los datos de ejemplo de BackVynils: álbumes 100 (Buscando América), 103 (A Day at the Races), 101 (Poeta
  del pueblo) y 102 (A Night at the Opera), músico 100 (Rubén Blades Bellido de Luna), banda 101 (Queen)
  y coleccionista 100 (Manolo Bellon). El detalle está en [Datos de prueba](guia-manual.md#datos-de-prueba).
- La HU10 no quita el favorito, aunque el plan lo pide: la app no permite quitar favoritos.
- Los recorridos buscan algunos textos en español ("Agregar Favorito", "Aceptar" del calendario). Desde
  API 33 el script fija la app en español tras cada `pm clear`; por debajo, el teléfono debe estar en español.
- UI Automator corre en su propio proceso, pero para encontrar los elementos la app arma su árbol de
  accesibilidad: en Allocations pueden aparecer objetos `AccessibilityNodeInfo` que no son de la app.
- En la HU03, HU07, HU08, HU09 y HU15 el texto se escribe letra por letra (un cambio de texto por letra), para que se
  vea lo que cuesta cada cambio. Se pone directo en el campo y no con teclas simuladas, porque el
  teclado del teléfono las autocorrige ("perfilamiento" quedaba "Perfila miento") y las vuelve muy lentas.
