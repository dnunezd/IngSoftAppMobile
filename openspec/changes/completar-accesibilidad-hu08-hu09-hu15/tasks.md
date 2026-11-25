# Tasks

## 1. Paleta y tema

- [x] 1.1 Agregar en `Color.kt` el par de botón deshabilitado y un helper de colores de botón principal; verificar con un par nuevo en `ContrasteDePaletaTest` (≥ 4,5:1)
- [x] 1.2 Definir los `surfaceContainer*` en `Theme.kt` con la paleta; verificar con una captura del diálogo de descarte y del selector de fecha de Crear álbum

## 2. Detalle de álbum (HU02)

- [x] 2.1 Unir cada pista en un solo elemento y dar al número un ancho mínimo de una línea; verificar en el árbol de accesibilidad ("03, Prueba, 3:07") y con una captura a la fuente al 200 %
- [x] 2.3 Unir cada comentario del detalle (texto y calificación) en un solo elemento, hallado al repetir el Scanner con dos comentarios de nota 4; verificar con la prueba `cadaComentarioEsUnSoloElementoAunqueSeRepitaLaCalificacion`
- [x] 2.2 Exponer de nuevo el número del promedio y describir las estrellas como "de 5 estrellas"; verificar que el Scanner no reporte "Unexposed Text" en el detalle

## 3. Comentar álbum (HU09)

- [x] 3.1 Usar los colores de botón deshabilitado en "Publicar comentario" sin perder el estado de envío; verificar con el Scanner en el formulario vacío
- [x] 3.2 Chip "Comentario publicado" e iniciales del avatar con `onPrimaryContainer`; estrellas del resumen de solo lectura con una sola descripción; verificar con el Scanner en el estado publicado
- [x] 3.3 Contador "0 / 400" en una fila; "¿Qué te pareció este vinilo?" y "¡Comentario publicado exitosamente!" como encabezados; portadas con el nombre del álbum; verificar con una captura a la fuente al 200 % y en el árbol

## 4. Agregar canción (HU08)

- [x] 4.1 Colores de botón deshabilitado en "Guardar", "Datos de la canción" como encabezado, portada con el nombre del álbum y "Guardando…" con el carácter de elipsis; verificar con el Scanner y `lintDebug` (sin `TypographyEllipsis`)

## 5. Asociar álbum a un artista (HU15)

- [x] 5.1 Colores de botón deshabilitado en "Asociar" y asa de arrastre de 48 × 48 dp; verificar con el Scanner sin y con álbum elegido
- [x] 5.2 Título de la hoja como encabezado (ya lo era; se verifica en la prueba) y "Ya asociado" debajo del año con la fuente por encima de 1,15; verificar con una captura a la fuente al 200 %

## 6. Pruebas automáticas

- [x] 6.1 Extender `AccesibilidadPantallasTest` con Comentar (formulario y publicado), Agregar canción y la ventana de asociar, con la fuente normal y al 200 %, y comprobar encabezados y elementos combinados; verificar que pasa en el emulador API 35
- [x] 6.2 Ajustar las pruebas de `ui.detalle`, `ui.comentar`, `ui.crear` y `ui.artistas` que busquen textos ahora unidos; verificar el paquete `ui` completo en API 35

## 7. Verificación

- [x] 7.1 Repetir el Accessibility Scanner en todas las pantallas y estados de la revisión; confirmar que solo quedan las sugerencias de portadas del backend y del fondo oscurecido de la hoja inferior
- [x] 7.2 Ejecutar `testDebugUnitTest`, `lintDebug` y la suite instrumentada completa en API 35
- [ ] 7.3 Actualizar la página "Sprint 3 · Adopción de accesibilidad" de la wiki con el reporte de HU08, HU09 y HU15
