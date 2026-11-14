# Guía: perfilamiento automatizado

Cómo correr el plan de pruebas de rendimiento en un dispositivo con `scripts/perfilamiento/Perfilar.ps1`.
El script maneja la app, captura los datos y las pantallas, y se detiene con instrucciones exactas
cuando hay que hacer algo a mano en el Profiler de Android Studio.

## Qué se necesita

- Windows con PowerShell, `adb` en el `PATH` y `JAVA_HOME` apuntando a un JDK 17 o 21.
- Docker Desktop corriendo y el repositorio BackVynils clonado junto a este (`..\BackVynils`);
  si está en otra carpeta, pasar `-RutaBackend <carpeta>`.
- El teléfono conectado (mejor por USB), desbloqueado y con la pantalla encendida.
- Android Studio abierto con este proyecto.

## Pasos

1. Levantar el backend, en la carpeta de BackVynils: `docker compose up -d --build`.
2. Compilar e instalar: `.\scripts\perfilamiento\Perfilar.ps1 -Accion Preparar`.
   Compila las dos variantes del APK (profileable y depurable); `Ejecutar` instala sola la que
   necesita cada pasada.
3. Revisar que todo esté listo: `.\scripts\perfilamiento\Perfilar.ps1 -Accion Validar`.
4. Ejecutar el plan: `.\scripts\perfilamiento\Perfilar.ps1 -Accion Ejecutar`.
   Para un solo escenario: `-Escenarios hu01`. Para elegir pasadas: `-Pasadas telemetria,asignaciones`.
   Sin pausas de Android Studio: `-SinStudio`.

Con varios dispositivos conectados, elegir uno con `-Serial <serial>` (el de `adb devices`).

## Pasadas de cada HU

| Pasada | Qué se captura en Android Studio | APK |
|---|---|---|
| `telemetria` | View Live Telemetry (CPU, hilos y memoria en vivo) | profileable |
| `traza` | System Trace: en Studio para HU03, HU07 y HU10; por línea de comandos en las demás | profileable |
| `asignaciones` | Java/Kotlin Allocations (objetos creados) y Network Inspector (peticiones al backend) | depurable |

El APK profileable da cifras representativas de CPU, memoria y fluidez. Java/Kotlin Allocations,
Network Inspector y Heap Dump exigen el depurable, que es más lento: sus cifras no se comparan con
las de las otras pasadas.

## Qué instala

| Paquete | Qué es |
|---|---|
| `com.example.vinilos.perf` | La app, compilada como `release` y firmada con la llave de depuración, en dos variantes (*profileable* y depurable) que se reemplazan entre sí. Convive con la app normal. |
| `com.example.vinilos.perfilamiento` | Los recorridos (UI Automator). Corren en su propio proceso para no alterar la medición. |

## Qué deja en `docs/performance/resultados/<dispositivo>/`

- `HUxx/live-telemetry.png`, `HUxx/system-trace.png`, `HUxx/allocations.png` y
  `HUxx/network-inspector.png`: capturas de la ventana de Android Studio.
- `HUxx/system-trace.perfetto-trace`: traza del sistema grabada por línea de comandos; se abre en el
  Profiler (pestaña *Past Recordings* > importar) para verla igual que una grabada desde Studio.
- `HUxx/<pasada>-gfxinfo.txt` y `HUxx/<pasada>-telefono.png`: fluidez y pantalla final del teléfono.
- `caida-backend/` y `memoria-navegacion/`: las dos pruebas transversales.
- `registro-<fecha>.md`: la tabla del plan (§13) con memoria y cuadros lentos ya diligenciados.
  CPU, hilo principal y hallazgos se completan a mano a partir de las capturas.

Los `.hprof` no se versionan (están en `.gitignore`).

## Diferencias con el plan

- **HU10:** la app solo permite agregar favoritos, no quitarlos. El recorrido agrega una vez y
  comprueba que el estado persiste al volver a abrir el artista; el script quita los favoritos por la
  API antes de cada pasada.
- **HU07:** se envía una vez por pasada. El álbum creado (`Perf <hora>`) se borra por la API al terminar, para que todos los
  dispositivos vean los mismos datos.
- **Caída del backend:** se corre dos veces, con las consultas guardadas y sin ellas, porque la app
  responde distinto en cada caso.
- **Caché:** al empezar se borran los datos de la app (caché fría). Con `-CacheCaliente` no se borran;
  queda anotado en el registro.
