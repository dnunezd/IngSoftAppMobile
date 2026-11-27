# Guía manual de perfilamiento

Pasos de HU01, HU02, HU03, HU04, HU07, HU08, HU09, HU10 y HU15, según el **plan de pruebas de
rendimiento del Sprint 3**. Cada paso es una acción seguida de la espera que indica la columna
**Espera** (5, 3 o 2 s). En los envíos (crear, guardar, publicar y asociar) se espera la respuesta del
backend, hasta 10 s. Antes del primer paso, la app queda 5 s quieta en el ingreso.

Por cada HU se hacen dos grabaciones con los mismos pasos:

1. **Live Telemetry y Network Inspector** al mismo tiempo.
2. **Memoria** (Java/Kotlin Allocations), después de reiniciar la app.

## Preparar (una vez)

Con el backend arriba (`docker compose up -d` en BackVynils), en PowerShell desde la raíz del repositorio:

```powershell
.\scripts\perfilamiento\Perfilar.ps1 -Accion Preparar
```

Compila e instala la app de la rama actual (fase `latest`) en su variante `perf` depurable, porque
Network Inspector y la grabación de memoria lo exigen. Con `-Version <tag>` compila otra versión (por
ejemplo, la línea base del Sprint 2 fue `-Version v2.1.0`). Para hacer estos mismos pasos de forma
automática, ver [guia-automatizacion.md](guia-automatizacion.md).

Para usar `adb` en la consola (solo esa consola; en cada consola nueva hay que repetirlo):

```powershell
$env:PATH = "$env:LOCALAPPDATA\Android\Sdk\platform-tools;$env:PATH"
adb reverse tcp:3000 tcp:3000
```

Para dejarlo permanente, correr esto una vez y abrir otra consola:

```powershell
[Environment]::SetEnvironmentVariable('Path', [Environment]::GetEnvironmentVariable('Path', 'User') + ";$env:LOCALAPPDATA\Android\Sdk\platform-tools", 'User')
```

## Cada HU

1. Dejar los datos de prueba como los espera la HU (ver la sección de cada HU) y reiniciar la app sin
   datos guardados:
   ```powershell
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
3. Volver a dejar los datos de prueba y reiniciar la app con el mismo comando.
4. **Grabación 2:**
   1. En el Profiler, iniciar **Track Memory Consumption (Java/Kotlin Allocations)**.
   2. Repetir los pasos.
   3. Detener, seleccionar el tramo de los pasos, filtrar por `com.example.vinilos`, ordenar por
      **Allocations** y capturar la tabla como `track-memory.png`.
   4. En **Past Recordings**, **Export recording** como `track-memory.asdb`.
5. Guardar todo en `docs/performance/resultados/<propietario>--<dispositivo>/HUxx/<fase>/` (p. ej.
   `jeffrey-diaz--motorola-edge-30/HU08/latest/`). La fase es `baseline` (la versión antes de optimizar)
   o `latest` (la app de la rama actual):

   ```
   <propietario>--<dispositivo>/
   ├── dispositivo.txt
   └── HU01/
       ├── baseline/
       │   ├── connections.json
       │   ├── connections.png
       │   ├── live-telemetry.asdb
       │   ├── live-telemetry.png
       │   ├── track-memory.asdb
       │   └── track-memory.png
       └── latest/   (los mismos 6 archivos)
   ```

   Lo mismo para HU02, HU03, HU04, HU07, HU08, HU09, HU10 y HU15. Los segundos anotados y lo encontrado
   van aparte, en `analisis/<propietario>--<dispositivo>/<fase>/HU01.md`. El script automatizado
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

## Datos de prueba

Los recorridos usan los datos de ejemplo de BackVynils (`sql/data.sql`):

| Dato | Uso |
|---|---|
| Coleccionista 100, Manolo Bellon | HU07, HU08, HU09, HU10 y HU15 |
| Álbum 100, Buscando América (artistas, canciones y comentarios) | HU02 y HU04 |
| Álbum 101, Poeta del pueblo | HU08 (canción nueva) y HU09 (comentario nuevo) |
| Álbum 102, A Night at the Opera | HU15 (se asocia a Rubén Blades) |
| Músico 100, Rubén Blades Bellido de Luna (álbumes 100 y 101) | HU04 y HU15 |
| Banda 101, Queen | HU10 |

La API no permite deshacer lo que crean HU08, HU09, HU10 y HU15, así que se deshace en la base del
Docker local, desde la carpeta de BackVynils. Hay que hacerlo **antes de cada grabación** (el script
automatizado lo hace solo, antes y después):

```powershell
# HU08: canciones "Perf" de Poeta del pueblo
docker compose exec db psql -U postgres -d vinyls -c "DELETE FROM track WHERE \"albumId\" = 101 AND name LIKE 'Perf %';"
# HU09: comentarios "Perf" de Poeta del pueblo
docker compose exec db psql -U postgres -d vinyls -c "DELETE FROM comment WHERE \"albumId\" = 101 AND description LIKE 'Perf %';"
# HU10: favoritos de Manolo Bellon (la app no deja quitarlos y DELETE de la API no borra)
docker compose exec db psql -U postgres -d vinyls -c 'DELETE FROM performer_collectors_collector WHERE "collectorId" = 100;'
# HU15: A Night at the Opera asociado a Rubén Blades
docker compose exec db psql -U postgres -d vinyls -c 'DELETE FROM performer_albums_album WHERE "performerId" = 100 AND "albumId" = 102;'
```

Para volver a los datos de ejemplo de HU10 (Rubén Blades como favorito del coleccionista 100):

```powershell
docker compose exec db psql -U postgres -d vinyls -c 'INSERT INTO performer_collectors_collector VALUES (100, 100) ON CONFLICT DO NOTHING;'
```

---

## HU01 · Catálogo de álbumes (plan 5.1)

| # | Acción | Espera | Seg. |
|---|---|---|---|
| 1 | Abrir la app y entrar al catálogo: tarjeta **Visitante** > **Entrar** > **Ver álbumes** | 5 s | |
| 2 | Desplazarse hasta la mitad de la lista | 3 s | |
| 3 | Desplazarse hasta el final y regresar al inicio, sin arrastrar más allá del tope (eso dispara «deslizar para actualizar» y agrega otra consulta) | 5 s | |

**Qué analizar:**
- **Red:** `GET /albums` y la descarga de cada portada. Revisar su tamaño, su duración y si hay solicitudes repetidas.
- **CPU:** el pico al llegar los datos y dibujar la lista, y el uso al desplazarla.
- **Memoria:** antes y después de cargar, y si se queda arriba al volver al inicio.
- **Asignaciones:** al construir los elementos de la lista (`AlbumSummary`) y los bitmaps de las portadas.

## HU02 · Detalle de álbum (plan 5.2)

| # | Acción | Espera | Seg. |
|---|---|---|---|
| 1 | Tarjeta **Visitante** > **Entrar** > **Ver álbumes** | 3 s | |
| 2 | Seleccionar **Buscando América** (tiene artistas, canciones y comentarios) | 5 s | |
| 3 | Desplazarse por el contenido hasta el final | 3 s | |
| 4 | **Atrás**, al catálogo, y abrir **Buscando América** otra vez | 5 s | |

**Qué analizar:**
- **Red:** `GET /albums/100` en la primera apertura, y si se repite en la segunda (la caché HTTP responde 304).
- **CPU y memoria:** primera apertura frente a la segunda; las imágenes con Coil al reabrir.
- **Asignaciones:** clases del detalle (`AlbumDto`, `Album`) y objetos del parseo del JSON.

## HU03 · Listado y búsqueda de artistas (plan 5.3)

| # | Acción | Espera | Seg. |
|---|---|---|---|
| 1 | Entrar al listado: tarjeta **Visitante** > **Entrar** > **Ver artistas** | 5 s | |
| 2 | En el buscador, escribir `Juan` | 3 s | |
| 3 | Borrar el texto y escribir `Rock` | 3 s | |
| 4 | Borrar nuevamente, cerrar el teclado y desplazarse por la lista | 3 s | |

No rotar la pantalla en este escenario: en emuladores API 23 la rotación puede corromper el texto del
buscador (bug conocido). Si ocurre, se anota aparte.

**Qué analizar:**
- **Red:** `GET /musicians` y `GET /bands` al entrar. Escribir en el buscador no debería generar peticiones.
- **CPU:** trabajo del hilo principal con cada letra (filtrar y recomponer la lista).
- **Asignaciones:** `String` y `StringBuilder` durante la búsqueda; clases de artista (`ArtistDto`, `Artist`).

## HU04 · Detalle de artista (plan 5.4)

| # | Acción | Espera | Seg. |
|---|---|---|---|
| 1 | Tarjeta **Visitante** > **Entrar** > **Ver artistas** | 3 s | |
| 2 | Abrir **Rubén Blades Bellido de Luna** (artista con discografía) | 5 s | |
| 3 | Desplazarse por la biografía y las estadísticas hasta su discografía | 3 s | |
| 4 | Abrir **Buscando América** desde la discografía | 5 s | |
| 5 | **Atrás**, al artista | 3 s | |
| 6 | **Atrás**, al listado | 5 s | |

**Qué analizar:**
- **Red:**
  - `GET /musicians/100` y `GET /performerprizes` al abrir el artista. `/performerprizes` trae las
    premiaciones de todos los artistas: revisar su tamaño.
  - `GET /albums/100` al abrir el álbum.
  - Si se repiten al volver.
- **CPU y memoria:** al abrir el detalle, que tiene imagen y varias secciones, y la fluidez al navegar.
- **Asignaciones:** el detalle del artista y los objetos de las premiaciones.

## HU07 · Crear un álbum (plan 5.5)

| # | Acción | Espera | Seg. |
|---|---|---|---|
| 1 | Tarjeta **Coleccionista** > **Entrar** > **Manolo Bellon** > **Ver álbumes** > **Crear álbum** (botón abajo a la derecha) | 3 s | |
| 2 | Llenar el formulario con datos válidos (valores abajo) | 2 s | |
| 3 | Tocar **Crear álbum** una sola vez y esperar la respuesta; la app vuelve al catálogo | hasta 10 s | |
| 4 | Verificar el álbum nuevo en el catálogo | 5 s | |

| Campo | Valor |
|---|---|
| Nombre del álbum | `Perf 1` en la grabación 1, `Perf 2` en la grabación 2 |
| Portada (URL) | `https://upload.wikimedia.org/wikipedia/en/4/42/Beatles_-_Abbey_Road.jpg` |
| Fecha de lanzamiento | día 1 del mes que muestra el calendario > **Aceptar** |
| Género | Rock |
| Disquera | EMI |
| Descripción | `Album creado por el recorrido de perfilamiento.` |

Después de cada grabación, borrar los álbumes creados (la lista se guarda primero en una variable: si
se encadena directo a `Where-Object`, el filtro deja pasar todo el catálogo):

```powershell
$albumes = Invoke-RestMethod http://localhost:3000/albums
$albumes | Where-Object { $_.name -like 'Perf*' } | ForEach-Object { Invoke-RestMethod -Method Delete "http://localhost:3000/albums/$($_.id)" }
```

**Qué analizar:**
- **Red:** `GET /collectors` al entrar y `POST /albums`: tamaño, código de respuesta y duración. Después,
  el refresco del catálogo.
- **CPU:** al abrir el formulario, al cargar la vista previa de la portada y en el hilo principal durante el envío.
- **Asignaciones:** lo que se crea mientras se escribe en el formulario.

## HU08 · Asociar tracks con un álbum (plan 5.6)

Antes de cada grabación, borrar las canciones `Perf` de Poeta del pueblo (ver [Datos de prueba](#datos-de-prueba)).

| # | Acción | Espera | Seg. |
|---|---|---|---|
| 1 | Tarjeta **Coleccionista** > **Entrar** > **Manolo Bellon** > **Ver álbumes** > **Poeta del pueblo** | 3 s | |
| 2 | **Agregar canción** | 3 s | |
| 3 | Escribir el nombre (`Perf 1` o `Perf 2`) y la duración `5:05`; el botón **Guardar** se habilita al completarlos | 2 s | |
| 4 | Tocar **Guardar** una sola vez y esperar la respuesta; la app vuelve al detalle del álbum | hasta 10 s | |
| 5 | Verificar la canción nueva en el detalle del álbum | 5 s | |

**Qué analizar:**
- **Red:** `POST /albums/101/tracks` (tamaño, código de respuesta y duración) y la consulta del detalle al volver.
- **CPU:** al abrir el formulario, al escribir y validar los campos, y en el hilo principal durante el envío.
- **Memoria:** inicial, durante el diligenciamiento y al terminar.
- **Asignaciones:** el estado del formulario y la actualización del detalle del álbum.

## HU09 · Comentar un álbum (plan 5.7)

Antes de cada grabación, borrar los comentarios `Perf` de Poeta del pueblo (ver [Datos de prueba](#datos-de-prueba)).

| # | Acción | Espera | Seg. |
|---|---|---|---|
| 1 | Tarjeta **Coleccionista** > **Entrar** > **Manolo Bellon** > **Ver álbumes** > **Poeta del pueblo** | 3 s | |
| 2 | **Comentar** | 3 s | |
| 3 | Elegir **4 estrellas** y escribir `Perf 1: comentario del recorrido de perfilamiento.` (`Perf 2` en la grabación 2) | 2 s | |
| 4 | Tocar **Publicar** una sola vez y esperar la respuesta | hasta 10 s | |
| 5 | Verificar que el comentario quedó publicado y tocar **Aceptar** (vuelve al detalle) | 5 s | |

**Qué analizar:**
- **Red:** `POST /albums/101/comments` (tamaño, código de respuesta y duración) y la consulta del detalle al volver.
- **CPU:** al elegir la calificación, al escribir la opinión y en el hilo principal durante la publicación.
- **Memoria:** del formulario y del texto.
- **Asignaciones:** el estado de la pantalla de comentar con cada letra.

## HU10 · Agregar artistas a favoritos (plan 5.8)

Antes de cada grabación, quitar los favoritos del coleccionista 100 para que Queen empiece sin serlo
(ver [Datos de prueba](#datos-de-prueba)).

| # | Acción | Espera | Seg. |
|---|---|---|---|
| 1 | Tarjeta **Coleccionista** > **Entrar** > **Manolo Bellon** > **Ver artistas** | 3 s | |
| 2 | Abrir **Queen** (no es favorito) | 3 s | |
| 3 | Tocar **Agregar Favorito** una vez | 3 s | |
| 4 | Verificar el estado: el botón muestra **Favorito** | 3 s | |
| 5 | **Atrás**, al listado, y abrir **Queen** otra vez | 3 s | |
| 6 | Comprobar que sigue en **Favorito** | 5 s | |

El plan incluye además **quitar el favorito y volver a marcarlo**. No se hace porque la app no permite
quitar favoritos (y en BackVynils el `DELETE` responde 204 sin borrar).

**Qué analizar:**
- **Red:**
  - `GET /collectors/100/performers`, que comprueba si ya es favorito.
  - `POST /collectors/100/bands/101`.
  - Si al reabrir vuelve a consultar.
- **CPU y memoria:** en el cambio de estado del botón y en el hilo principal durante la acción.
- **Asignaciones:** las clases de favorito y de coleccionista.

## HU15 · Asociar un álbum a un artista (plan 5.9)

Antes de cada grabación, quitar la asociación de A Night at the Opera con Rubén Blades (ver
[Datos de prueba](#datos-de-prueba)). Se usa un músico y no una banda para no tocar las asociaciones
existentes: en las bandas la app reemplaza la lista completa de álbumes.

| # | Acción | Espera | Seg. |
|---|---|---|---|
| 1 | Tarjeta **Coleccionista** > **Entrar** > **Manolo Bellon** > **Ver artistas** > **Rubén Blades Bellido de Luna** | 3 s | |
| 2 | **Agregar álbum** (debajo de la discografía) | 3 s | |
| 3 | Buscar `Opera` y cerrar el teclado | 3 s | |
| 4 | Seleccionar **A Night at the Opera**; se habilita **Asociar** | 2 s | |
| 5 | Tocar **Asociar** una sola vez y esperar la respuesta; la ventana se cierra | hasta 10 s | |
| 6 | Verificar el álbum en la discografía de Rubén Blades | 5 s | |

**Qué analizar:**
- **Red:** `GET /albums` al abrir la ventana, la asociación del álbum 102 con el músico 100 y la consulta del artista después.
- **CPU:** durante la búsqueda (cada letra) y en el hilo principal durante la asociación.
- **Memoria:** de la ventana y sus resultados (portadas).
- **Asignaciones:** lo que se crea con cada cambio de la búsqueda y al actualizar la discografía.
