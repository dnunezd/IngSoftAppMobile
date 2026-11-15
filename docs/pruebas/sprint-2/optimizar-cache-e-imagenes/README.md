# Evidencia: optimizar la caché y las imágenes de respaldo (optimizar-cache-e-imagenes)

Tareas 2.4, 7.3 y 9.3 (solo la parte de red) de `openspec/changes/optimizar-cache-e-imagenes/tasks.md`.

- **Dispositivo:** Samsung SM-S938B, Android 17, por adb inalámbrico.
- **Fecha:** 14 de noviembre de 2026.
- **App:** debug de la rama `feature/optimizar-cache-e-imagenes`.
- **Backend:** BackVynils local (Docker), detrás de un proxy local de prueba en el PC. El proxy reenvía todo a `localhost:3000`, y la app se compiló con `-PbaseUrl=http://localhost:3001/` y `adb reverse tcp:3001 tcp:3001`. El backend no se modificó.

## Imagen genérica (tarea 2.4)

El backend local no tiene álbumes con portada inválida ni artistas sin imagen. Para verlos sin crear datos, el proxy cambió solo la respuesta que recibe la app: al álbum 101 («Poeta del pueblo») le puso la portada `https://portada.invalid/no-existe.png`, y al intérprete del álbum 100 («Buscando América») le quitó la imagen.

| # | Pantalla | Qué se ve | Captura |
|---|---|---|---|
| 1 | Inicio | Las dos tarjetas conservan sus ilustraciones. | [01-inicio.png](01-inicio.png) |
| 2 | Catálogo | «Poeta del pueblo» muestra el ícono de disco sobre fondo neutro; los demás, su portada. | [02-catalogo.png](02-catalogo.png) |
| 3 | Detalle de «Poeta del pueblo» | La portada de 220 dp muestra el mismo disco, proporcionado. | [03-detalle-portada-invalida.png](03-detalle-portada-invalida.png) |
| 4 | Detalle de «Buscando América» | El artista sin imagen muestra la silueta de HU03. | [04-detalle-artista-sin-imagen.png](04-detalle-artista-sin-imagen.png) |

## Álbum creado sin esperar la consulta (tarea 7.3)

El proxy retrasó 5 s la respuesta de `GET /albums`, para que se viera si el álbum aparece antes. Se entró como Manolo Bellon, se abrió el catálogo y se creó «Prueba Optimizacion Cache» desde el formulario.

Registro del proxy (hora en que respondió cada petición):

```
19:16:24  POST /albums  200
19:16:29  GET  /albums  200
```

| # | Momento | Qué se ve | Captura |
|---|---|---|---|
| 5 | 19:16:26, antes de que responda `GET /albums` | El álbum creado ya está en el catálogo, en su posición, con «NUEVO» y el aviso «Álbum creado». | [05-creado-antes-de-la-consulta.png](05-creado-antes-de-la-consulta.png) |
| 6 | Después de la respuesta | El catálogo sigue mostrando el álbum creado con «NUEVO». | [06-creado-tras-la-consulta.png](06-creado-tras-la-consulta.png) |

Girar la pantalla no se probó por adb: cambiar la rotación del teléfono modifica un ajuste del sistema. El mecanismo que evita repetir el aviso y la consulta al girar está en `VinilosNavHost` y este cambio no lo tocó.

El álbum «Prueba Optimizacion Cache» quedó creado en el backend local.

## Caché HTTP: la recarga recibe 304 (parte de red de la tarea 9.3)

Otro proxy local (`localhost:3002`) reenvió todas las cabeceras sin cambiar nada y registró cada petición. Se entró como Visitante, se abrió el catálogo y se deslizó dos veces para actualizar:

```
19:23:35 GET /albums | If-None-Match=None                            | 200 | ETag=W/"146b-ko+v7ICMMnbJ4DJ1dd7SgYdozXY" | 5227 bytes
19:23:43 GET /albums | If-None-Match=W/"146b-ko+v7ICMMnbJ4DJ1dd7SgYdozXY" | 304 | ETag=W/"146b-ko+v7ICMMnbJ4DJ1dd7SgYdozXY" | 0 bytes
19:23:48 GET /albums | If-None-Match=W/"146b-ko+v7ICMMnbJ4DJ1dd7SgYdozXY" | 304 | ETag=W/"146b-ko+v7ICMMnbJ4DJ1dd7SgYdozXY" | 0 bytes
```

Cada recarga envió la `ETag` guardada y recibió 304 sin cuerpo, y el catálogo siguió mostrando los 5 álbumes sin aviso de error ([07-catalogo-tras-304.png](07-catalogo-tras-304.png)). Esto no reemplaza el perfilamiento «After» con el Profiler, que mide las escrituras en Room y las asignaciones.
