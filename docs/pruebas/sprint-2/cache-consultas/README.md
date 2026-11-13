# Evidencia: consultas sin conexión (add-cache-consultas)

Tarea 6.2 de `openspec/changes/add-cache-consultas/tasks.md`.

- **Dispositivo:** Samsung SM-S938B, Android 17.
- **Fecha:** 13 de noviembre de 2026.
- **App:** debug de la rama `feature/cache-consultas`, compilada con `-PbaseUrl=http://localhost:3000/` y `adb reverse tcp:3000 tcp:3000`.
- **Backend:** BackVynils local (Docker).

## Cómo se simuló la falta de conexión

La tarea pedía activar el modo avión. Con el modo avión, el Samsung pierde la conexión inalámbrica con adb, y no se podrían tomar las capturas. Por eso, con el acuerdo del usuario, se apagó el backend (`docker compose stop`) en lugar de usar el modo avión. Para la app es lo mismo: cada consulta falla con un error de conexión. El Wi-Fi del teléfono siguió encendido, así que las imágenes que Coil no tenía guardadas sí se podían descargar; esta prueba no mide la caché de imágenes.

## Pasos

1. Se borraron los datos de la app (`pm clear`), para empezar sin nada guardado.
2. **Con el backend encendido:** se entró como Manolo Bellon, se abrió el catálogo (sin abrir ningún álbum), se volvió al inicio y se abrió el listado de artistas y el detalle de Rubén Blades Bellido de Luna, que ya era favorito de Manolo.
3. Se apagó el backend y se cerró la app (`am force-stop`).
4. **Con el backend apagado:** se abrió la app desde cero y se recorrieron las cinco pantallas.

Los pasos 2 a 4 se hicieron por adb con un script que toca por texto y comprueba que aparece el aviso y que no aparece el mensaje de error. La captura del listado de artistas se repitió volviendo a entrar a la pantalla, porque en la primera el aviso, que dura unos segundos, ya se había ocultado al tomarla.

## Resultado

| # | Pantalla | Qué se ve | Captura |
|---|---|---|---|
| 1 | Selector de coleccionistas | Los dos coleccionistas guardados, el buscador y el aviso "No pudimos actualizar los coleccionistas". Sin "No pudimos cargar los coleccionistas". Se pudo entrar como Manolo Bellon. | [01-coleccionistas.png](01-coleccionistas.png) |
| 2 | Detalle de álbum | "Buscando América", que nunca se abrió con conexión: portada, género, disquera, promedio, fecha, descripción y canciones, con el aviso "No pudimos actualizar el álbum". Sin "No pudimos cargar el álbum". | [02-detalle-album.png](02-detalle-album.png), [02b-detalle-album-canciones.png](02b-detalle-album-canciones.png) |
| 3 | Listado de artistas | Los músicos y bandas guardados, con el aviso "No se pudo actualizar el listado de artistas". Sin "No se pudo cargar el listado de artistas". | [03-artistas.png](03-artistas.png) |
| 4 | Detalle de un artista ya visto | Rubén Blades Bellido de Luna con fecha, descripción y álbumes, con el aviso "No se pudo actualizar el artista". Sin "No se pudo cargar el artista". | [04-detalle-artista.png](04-detalle-artista.png) |
| 5 | Favorito confirmado | El botón "Favorito" desactivado y la nota "Este artista ya está en tus favoritos.". Sin "No pudimos comprobar si es tu favorito". | [05-favorito.png](05-favorito.png) |

Las cinco pantallas se comportaron como piden los specs del cambio.
