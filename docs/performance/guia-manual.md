# Guía manual de perfilamiento

Pasos de HU01, HU02, HU03, HU04, HU07, HU08, HU09, HU10 y HU15, según el **plan de pruebas de
rendimiento del Sprint 3**. En HU01 a HU07 y HU10, cada paso es una acción seguida de la espera que
indica la columna **Espera** (5, 3 o 2 s); en HU08, HU09 y HU15, una acción y 5 s. En los envíos (crear, guardar, publicar y asociar) se espera la respuesta del
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
   `jeffrey-diaz--motorola-edge-30/HU08/before/`). En el Sprint 3 la fase es `before` (antes de las
   micro-optimizaciones) o `after` (después de ellas); en el Sprint 2 fueron `baseline` y `latest`:

   ```
   <propietario>--<dispositivo>/
   ├── dispositivo.txt
   └── HU01/
       ├── before/
       │   ├── connections.json
       │   ├── connections.png
       │   ├── live-telemetry.asdb
       │   ├── live-telemetry.png
       │   ├── track-memory.asdb
       │   └── track-memory.png
       └── after/   (los mismos 6 archivos)
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
| Álbum 101, Poeta del pueblo | HU08 (canción nueva) |
| Álbum 103, A Day at the Races (sin comentarios) | HU09 (comentario nuevo) |
| Álbum 102, A Night at the Opera | HU15 (se asocia a Rubén Blades) |
| Músico 100, Rubén Blades Bellido de Luna (álbumes 100 y 101) | HU04 y HU15 |
| Banda 101, Queen | HU10 |

La API no permite deshacer lo que crean HU08, HU09, HU10 y HU15, así que se deshace en la base del
Docker local, desde la carpeta de BackVynils. Hay que hacerlo **antes de cada grabación** (el script
automatizado lo hace solo, antes y después):

```powershell
# HU08: canciones "Perf" de Poeta del pueblo
docker compose exec db psql -U postgres -d vinyls -c "DELETE FROM track WHERE \"albumId\" = 101 AND name LIKE 'Perf%';"
# HU09: comentarios "Perf" de A Day at the Races
docker compose exec db psql -U postgres -d vinyls -c "DELETE FROM comment WHERE \"albumId\" = 103 AND description LIKE 'Perf%';"
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

## HU08 · Asociar canciones a un álbum

En HU08, HU09 y HU15 **cada paso es una acción y luego esperar 5 s**; en el envío, primero se espera la
respuesta (hasta 10 s) y luego los 5 s.

| # | Acción | Seg. |
|---|---|---|
| 1 | Tarjeta **Coleccionista** > **Entrar** > **Manolo Bellon** | |
| 2 | **Ver álbumes** | |
| 3 | Abrir **Poeta del pueblo** | |
| 4 | **Agregar canción** | |
| 5 | Escribir el nombre (valores abajo); **Guardar** sigue deshabilitado | |
| 6 | Escribir la duración; **Guardar** se habilita | |
| 7 | Tocar **Guardar** una sola vez; la app vuelve al detalle con el aviso **Canción agregada** (esperar hasta 10 s) | |

| Campo | Valor |
|---|---|
| Nombre | `Perf` |
| Duración | `5:05` |

Antes y después de **cada** grabación, borrar la canción creada (ver [Datos de prueba](#datos-de-prueba)),
para que la siguiente empiece igual.

**Qué analizar:**
- **Red:** `POST /albums/101/tracks`: tamaño, código de respuesta y duración. Después, el `GET /albums/101`
  que refresca el detalle.
- **CPU:** al abrir el formulario, al validar la duración con cada tecla y al guardar. Actividad del hilo
  principal mientras espera la respuesta.
- **Memoria:** al abrir el formulario, mientras se escribe y al volver al detalle.
- **Asignaciones:** lo que se crea mientras se escribe (estado del formulario) y al actualizar el detalle.

## HU09 · Comentar un álbum

| # | Acción | Seg. |
|---|---|---|
| 1 | Tarjeta **Coleccionista** > **Entrar** > **Manolo Bellon** | |
| 2 | **Ver álbumes** | |
| 3 | Abrir **A Day at the Races** (sin comentarios) | |
| 4 | **Comentar álbum** (botón abajo) | |
| 5 | Tocar la cuarta estrella (**Bueno (4/5)**) | |
| 6 | Escribir la opinión (valor abajo) y cerrar el teclado; **Publicar comentario** se habilita | |
| 7 | Tocar **Publicar comentario** una sola vez y tocar el botón otra vez mientras dice **Publicando...**; sale **¡Comentario publicado exitosamente!** (esperar hasta 10 s) | |
| 8 | **Aceptar**; la app vuelve al detalle con el comentario | |

| Campo | Valor |
|---|---|
| Opinión | `Perf: comentario del recorrido de perfilamiento.` |

Si al salir aparece **¿Descartar comentario?**, anotarlo, pero no forzarlo saliendo a propósito con el
borrador.

Antes y después de **cada** grabación, borrar el comentario creado (ver [Datos de prueba](#datos-de-prueba)),
para que la siguiente empiece igual.

**Qué analizar:**
- **Red:** `POST /albums/103/comments`: tamaño, código de respuesta y duración. Que el segundo toque no
  genere otro `POST`. Después, el `GET /albums/103` que refresca el detalle.
- **CPU:** mientras se escribe y al publicar. Actividad del hilo principal durante la petición y el
  indicador de carga.
- **Memoria:** del formulario y del texto, y al volver al detalle.
- **Asignaciones:** lo que se crea al cambiar el estado (calificación, texto, envío).

Si la publicación tarda, grabar además un **System Trace** durante ese paso.

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

## HU15 · Asociar un álbum a un artista

Se usa un músico: en una banda, la app reemplaza toda su lista de álbumes, y un error en el recorrido
podría quitarle los que ya tiene.

| # | Acción | Seg. |
|---|---|---|
| 1 | Tarjeta **Coleccionista** > **Entrar** > **Manolo Bellon** | |
| 2 | **Ver artistas** | |
| 3 | Abrir **Rubén Blades Bellido de Luna** | |
| 4 | Bajar hasta los álbumes y tocar **Agregar álbum** | |
| 5 | En **Buscar álbum por título...**, escribir `Night` | |
| 6 | Tocar **A Night at the Opera**; **Asociar** se habilita | |
| 7 | Tocar **Asociar** una sola vez; la ventana se cierra con el aviso **Álbum asociado** (esperar hasta 10 s) | |
| 8 | Bajar hasta los álbumes; debe aparecer **A Night at the Opera** | |

Antes y después de **cada** grabación, quitar la asociación (ver [Datos de prueba](#datos-de-prueba)); si
no, en la siguiente sale **Ya asociado**.

**Qué analizar:**
- **Red:**
  - `GET /albums` al abrir la ventana. Escribir en el buscador no debería generar peticiones.
  - `POST /musicians/100/albums/102`: tamaño, código de respuesta y duración.
  - El `GET /musicians/100` que refresca el detalle.
- **CPU:** con cada tecla de la búsqueda (filtrar y redibujar los resultados) y al asociar. Actividad del
  hilo principal durante la petición.
- **Memoria:** de la ventana y sus resultados, con las portadas, y al cerrarla.
- **Asignaciones:** lo que se crea al cambiar la búsqueda (`String`, listas filtradas) y las clases de
  álbum (`AlbumSummary`, `ArtistAlbum`).

Si la asociación tarda, grabar además un **System Trace** durante ese paso.
