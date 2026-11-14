# Guía manual de perfilamiento

Pasos de HU01, HU02, HU03, HU04, HU07 y HU10. **Cada paso es una acción y luego esperar 5 s.**

Por cada HU se hacen dos grabaciones con los mismos pasos:

1. **Live Telemetry y Network Inspector** al mismo tiempo.
2. **Memoria** (Java/Kotlin Allocations), después de reiniciar la app.

## Preparar (una vez)

Con el backend arriba (`docker compose up -d` en BackVynils), en PowerShell desde la raíz del repositorio:

```powershell
$env:PATH = "$env:LOCALAPPDATA\Android\Sdk\platform-tools;$env:PATH"
adb reverse tcp:3000 tcp:3000
adb install -r build\perfilamiento\vinilos-v2.1.0-perf-depurable.apk
```

Es la línea base v2.1.0 (adjunta también al release). Para otra versión, se genera con
`.\scripts\perfilamiento\Perfilar.ps1 -Accion Preparar -Version <tag>`. Se usa el APK depurable
porque Network Inspector y la grabación de memoria lo exigen. Para hacer estos mismos pasos de forma
automática, ver [guia-automatizacion.md](guia-automatizacion.md).

La primera línea pone `adb` al alcance solo en esa consola: en cada consola nueva hay que volver a
correrla. Para dejarlo permanente, correr esto una vez y abrir otra consola:

```powershell
[Environment]::SetEnvironmentVariable('Path', [Environment]::GetEnvironmentVariable('Path', 'User') + ";$env:LOCALAPPDATA\Android\Sdk\platform-tools", 'User')
```

## Cada HU

1. Reiniciar la app sin datos guardados (la primera línea es la del `PATH`, por si la consola es nueva):
   ```powershell
   $env:PATH = "$env:LOCALAPPDATA\Android\Sdk\platform-tools;$env:PATH"
   adb shell am force-stop com.example.vinilos.perf; adb shell pm clear com.example.vinilos.perf; adb shell am start -n com.example.vinilos.perf/com.example.vinilos.MainActivity
   ```
2. **Grabación 1:**
   1. En el Profiler, iniciar **View Live Telemetry** (con **Now**) sobre `com.example.vinilos.perf`.
   2. En App Inspection, abrir **Network Inspector** sobre el mismo proceso.
   3. Hacer los pasos y anotar el segundo de la grabación en que haces cada uno.
   4. Capturar Live Telemetry (CPU y memoria, con el tramo de los pasos a la vista) como
      `live-telemetry.png`.
   5. En Network Inspector, seleccionar el tramo de los pasos y capturar la Connection View como
      `connections.png`; luego **Export Connections** como `connections.json`.
   6. Detener la grabación (**Stop Recording**) y, en **Past Recordings**, **Export recording** como
      `live-telemetry.asdb`.
3. Reiniciar la app con el mismo comando.
4. **Grabación 2:**
   1. En el Profiler, iniciar **Track Memory Consumption (Java/Kotlin Allocations)**.
   2. Repetir los pasos.
   3. Detener, seleccionar el tramo de los pasos, filtrar por `com.example.vinilos`, ordenar por
      **Allocations** y capturar la tabla como `track-memory.png`.
   4. En **Past Recordings**, **Export recording** como `track-memory.asdb`.
5. Guardar todo en `docs/performance/resultados/<propietario>-<dispositivo>/<fase>/HUxx/` (p. ej.
   `jeffrey-motorola-edge-30/Before/HU01/`, donde `Before` es la primera fase):

   ```
   <propietario>-<dispositivo>/Before/HU01/
   ├── connections.json
   ├── connections.png
   ├── live-telemetry.asdb
   ├── live-telemetry.png
   ├── track-memory.asdb
   └── track-memory.png
   ```

   Los segundos anotados y lo encontrado van aparte, en
   `<propietario>-<dispositivo>/analisis/Before/HU01.md`. El script automatizado
   ([guia-automatizacion.md](guia-automatizacion.md)) deja la misma estructura y arma ese análisis.

## Qué se analiza

Se toma como referencia el codelab
[Cómo perfilar el rendimiento de una aplicación con Android Studio](https://thesoftwaredesignlab.github.io/SE4MACodelabs/android-profiling/index.html#4).

| Vista | Qué se mira |
|---|---|
| CPU (Live Telemetry) | El uso de la app frente al resto del sistema y qué hilo lleva la carga: el principal (nombre del paquete), `RenderThread` (dibuja la pantalla) o los hilos `OkHttp` (red). También cuántos hilos hay vivos. |
| Memoria (Live Telemetry) | Cuánto sube al llegar los datos y al armar la pantalla, qué capa crece (Java, nativa o gráficos) y las marcas de recolección de basura. Esas marcas son normales al inicio; seguidas y en reposo indican objetos de más. |
| Red (Network Inspector) | En qué paso ocurre cada petición. En la Connection View: tamaño, tipo, código de respuesta y duración. También si hay peticiones repetidas o innecesarias. |
| Memoria (Allocations) | Qué clases se crean más, en cantidad y en tamaño. Filtrar por `com.example.vinilos` para ver las de la app. Agrupar por pila de llamadas para ver qué método asigna más. |

Al final, con los segundos anotados, se cruza todo para encontrar **los pasos con más carga** de cada HU.

---

## HU01 · Catálogo de álbumes

| # | Acción | Seg. |
|---|---|---|
| 1 | Tarjeta **Visitante** > **Entrar** | |
| 2 | **Ver álbumes** | |
| 3 | Bajar hasta el final de la lista | |
| 4 | Subir hasta el inicio, sin arrastrar más allá del tope: eso dispara «deslizar para actualizar» y agrega otra consulta | |

**Qué analizar:**
- **Red:** `GET /albums` y la descarga de cada portada. Revisar su tamaño y duración.
- **CPU:** el pico al llegar los datos y dibujar la lista, y el uso al desplazarla.
- **Memoria:** cuánto suben las portadas y si la memoria se queda arriba al volver al inicio.
- **Asignaciones:** las clases de álbum (`AlbumDto`, `AlbumResumen`) y los bitmaps de las portadas.

## HU02 · Detalle de álbum

| # | Acción | Seg. |
|---|---|---|
| 1 | Tarjeta **Visitante** > **Entrar** | |
| 2 | **Ver álbumes** | |
| 3 | Abrir **Buscando América** | |
| 4 | Bajar hasta el final (canciones y comentarios) | |
| 5 | **Atrás**, al catálogo | |
| 6 | Abrir **Buscando América** otra vez | |

**Qué analizar:**
- **Red:** `GET /albums/100` en la primera apertura, y si se repite en la segunda (la app guarda la consulta).
- **CPU y memoria:** primera apertura frente a la segunda.
- **Asignaciones:** clases del detalle (`AlbumDto`, `Album`) y objetos del parseo del JSON.

## HU03 · Listado y búsqueda de artistas

| # | Acción | Seg. |
|---|---|---|
| 1 | Tarjeta **Visitante** > **Entrar** | |
| 2 | **Ver artistas** (bajar en Inicio si no se ve) | |
| 3 | Tocar el buscador y escribir `Queen` | |
| 4 | Borrar y escribir `Juan`; no tiene coincidencias y debe salir "No se encontraron artistas" | |
| 5 | Borrar el texto y cerrar el teclado | |

**Qué analizar:**
- **Red:** `GET /musicians` y `GET /bands` al entrar. Escribir en el buscador no debería generar peticiones.
- **CPU:** trabajo del hilo principal con cada tecla (filtrar y redibujar la lista).
- **Asignaciones:** `String` y `StringBuilder` durante la búsqueda; clases de artista (`ArtistDto`, `Artist`).

## HU04 · Detalle de artista

| # | Acción | Seg. |
|---|---|---|
| 1 | Tarjeta **Visitante** > **Entrar** | |
| 2 | **Ver artistas** | |
| 3 | Abrir **Rubén Blades Bellido de Luna** | |
| 4 | Bajar hasta los álbumes de su discografía | |
| 5 | Abrir **Buscando América** | |
| 6 | **Atrás**, al artista | |
| 7 | **Atrás**, al listado | |

**Qué analizar:**
- **Red:**
  - `GET /musicians/100` y `GET /performerprizes` al abrir el artista. `/performerprizes` trae las
    premiaciones de todos los artistas: revisar su tamaño.
  - `GET /albums/100` al abrir el álbum.
  - Si se repiten al volver.
- **CPU y memoria:** al abrir el detalle, que tiene imagen y varias secciones.
- **Asignaciones:** `ArtistaDetalle` y los objetos de las premiaciones.

## HU07 · Crear un álbum

| # | Acción | Seg. |
|---|---|---|
| 1 | Tarjeta **Coleccionista** > **Entrar** > **Manolo Bellon** | |
| 2 | **Ver álbumes** | |
| 3 | **Crear álbum** (botón abajo a la derecha) | |
| 4 | Llenar el formulario (valores abajo) | |
| 5 | Tocar **Crear álbum**; la app vuelve al catálogo | |

| Campo | Valor |
|---|---|
| Nombre del álbum | `Perf 1` en la grabación 1, `Perf 2` en la grabación 2 |
| Portada (URL) | `https://upload.wikimedia.org/wikipedia/en/4/42/Beatles_-_Abbey_Road.jpg` |
| Fecha de lanzamiento | día 1 del mes que muestra el calendario > **Aceptar** |
| Género | Rock |
| Disquera | EMI |
| Descripción | `Album creado por el recorrido de perfilamiento.` |

Después de las dos grabaciones, borrar los álbumes creados:

```powershell
(Invoke-RestMethod http://localhost:3000/albums) | Where-Object { $_.name -like 'Perf*' } | ForEach-Object { Invoke-RestMethod -Method Delete "http://localhost:3000/albums/$($_.id)" }
```

**Qué analizar:**
- **Red:** `GET /collectors` al entrar y `POST /albums`: tamaño, código de respuesta y duración. Después,
  el refresco del catálogo.
- **CPU:** al abrir el formulario, al cargar la vista previa de la portada y al enviar.
- **Asignaciones:** lo que se crea mientras se escribe en el formulario, y las clases `AlbumNuevo` y
  `OpcionesAlbum`.

## HU10 · Agregar artistas a favoritos

Antes de **cada** grabación, quitar los favoritos del coleccionista 100, para que Queen empiece sin
serlo. La app no deja quitarlos y la API tampoco: `DELETE /collectors/100/bands/101` responde 204 pero
no borra nada (en BackVynils, `deleteCollectorPerformer` filtra la lista y no guarda el resultado).
Por eso se quitan en la base del Docker local, desde la carpeta de BackVynils:

```powershell
docker compose exec db psql -U postgres -d vinyls -c 'DELETE FROM performer_collectors_collector WHERE "collectorId" = 100;'
```

Para volver a los datos de ejemplo (Rubén Blades como favorito del coleccionista 100):

```powershell
docker compose exec db psql -U postgres -d vinyls -c 'INSERT INTO performer_collectors_collector VALUES (100, 100) ON CONFLICT DO NOTHING;'
```

| # | Acción | Seg. |
|---|---|---|
| 1 | Tarjeta **Coleccionista** > **Entrar** > **Manolo Bellon** | |
| 2 | **Ver artistas** | |
| 3 | Abrir **Queen** | |
| 4 | **Agregar Favorito**; el botón cambia a **Favorito** | |
| 5 | **Atrás**, al listado | |
| 6 | Abrir **Queen** otra vez; debe seguir en **Favorito** | |

**Qué analizar:**
- **Red:**
  - `GET /collectors/100/performers`, que comprueba si ya es favorito.
  - `POST /collectors/100/bands/101`.
  - Si al reabrir vuelve a consultar.
- **CPU y memoria:** en el cambio de estado del botón.
- **Asignaciones:** las clases de favorito y de coleccionista.