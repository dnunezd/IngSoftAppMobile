# Proposal

## Why

El cambio `corregir-accesibilidad-scanner` cubrió las 8 pantallas que había antes de HU08, HU09 y HU15. Ya están en `develop` las pantallas de agregar canción (HU08), comentar un álbum (HU09) y asociar un álbum a un artista (HU15), y repetir el ejercicio sobre toda la app encontró hallazgos nuevos en ellas y en el detalle del álbum. El detalle cambió con esas HU, y uno de sus hallazgos es una regresión de la corrección anterior del promedio.

## What Changes

Revisión hecha en el emulador API 35 con el backend local. Se hicieron 26 escaneos del Accessibility Scanner, incluidos los estados con datos (formulario lleno, diálogo de descarte, comentario publicado y álbum elegido), además de la fuente al 200 %, el modo oscuro y el árbol de accesibilidad para TalkBack.

- **Contraste:**
  - Los botones deshabilitados "Publicar comentario", "Guardar" y "Asociar" usan los colores por defecto de Material: texto al 38 % sobre fondo al 12 %. Pasan a un par de colores de la paleta con al menos 4,5:1.
  - El diálogo "¿Descartar comentario?" usa el fondo lila por defecto de Material, y "Descartar y salir" no alcanza el contraste. El tema define los colores de contenedor de superficie con la paleta de la app.
  - En la pantalla de comentario publicado, el chip "Comentario publicado" y las iniciales del avatar pasan del naranja de marca al naranja oscuro sobre su fondo claro.
- **Destinos táctiles:**
  - Las estrellas del resumen del comentario publicado son botones deshabilitados de 32 dp. Pasan a ser solo de lectura, con una sola descripción ("4 de 5 estrellas").
  - El asa para arrastrar la ventana de asociar mide 32 dp de ancho y pasa a 48 dp.
- **Elementos para TalkBack y el Scanner:**
  - Cada pista del detalle del álbum se anuncia como un solo elemento ("03, Prueba, 3:07"). Hoy el número, el nombre y la duración son elementos sueltos, y el Scanner los marca como descripciones duplicadas cuando dos canciones se llaman igual o duran lo mismo.
  - El promedio del detalle vuelve a exponer su número como texto. La corrección anterior lo había juntado en una sola descripción, y el Scanner marca ese número como "texto no expuesto".
  - Se marcan como encabezados "¿Qué te pareció este vinilo?", "¡Comentario publicado exitosamente!", "Datos de la canción" y "Asociar álbum a …".
  - Las portadas de Comentar y de Agregar canción dicen "Portada del álbum, " seguido del nombre, como en el detalle.
- **Fuente al 200 %:**
  - El número de pista no se parte en dos líneas.
  - El contador "0 / 400" de Comentar no se apila en vertical.
  - En la ventana de asociar, la etiqueta "Ya asociado" pasa debajo del título, que se truncaba.
- **Lint:** "Guardando..." pasa a "Guardando…" (aviso `TypographyEllipsis`).
- **Pruebas:** `AccesibilidadPantallasTest` agrega las pantallas nuevas con la fuente normal y al 200 %. `ContrasteDePaletaTest` agrega los pares nuevos.

Sin cambios de comportamiento, navegación ni datos.

## Capabilities

### New Capabilities

Ninguna.

### Modified Capabilities

- `accesibilidad`: agrega requisitos para controles deshabilitados, diálogos y hojas, elementos compuestos (pistas, promedio y estrellas) y las pantallas de HU08, HU09 y HU15. La capacidad vive en el cambio `corregir-accesibilidad-scanner`, que aún no está archivado, así que este cambio la amplía con requisitos nuevos (ADDED) y no modifica los existentes.

## Impact

- **Tema:**
  - `ui/theme/Theme.kt`: colores de contenedor de superficie.
  - `ui/theme/Color.kt`: par de colores para botones deshabilitados.
- **Pantallas:**
  - `ui/detalle/ContenidoAlbum.kt`: pistas y promedio.
  - `ui/comentar/PantallaComentarAlbum.kt`.
  - `ui/crear/PantallaAgregarCancion.kt`.
  - `ui/artistas/VentanaAsociarAlbum.kt`.
- **Recursos:** `res/values/strings.xml` ("Guardando…").
- **Pruebas:**
  - `AccesibilidadPantallasTest` y `ContrasteDePaletaTest`.
  - Pueden cambiar las pruebas de esas pantallas que buscan el número de pista o el promedio por texto suelto.
- **Fuera de alcance, documentado:**
  - Las portadas con texto impreso, que vienen del backend.
  - El fondo oscurecido de `ModalBottomSheet` y sus etiquetas "Close sheet" y "Drag handle", que son de Material y siguen el idioma del dispositivo.
  - Que Agregar canción muestre "Campo obligatorio" en rojo antes de que el usuario escriba. Es una decisión de HU08, no de accesibilidad.
