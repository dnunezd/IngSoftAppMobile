# Design

## Context

Hallazgos de la revisión del 24 de noviembre de 2026, hecha en el emulador API 35 con el backend local. Ver proposal.md, sección "Why".

| HU | Pantalla / estado | Scanner | Fuente al 200 % | TalkBack (árbol) |
|---|---|---|---|---|
| HU00 | Ingreso, Coleccionistas, Inicio | 0 | Sin cambios | Sin cambios |
| HU01 | Catálogo | 2, "Unexposed Text" de la portada de "Buscando América" (ya conocidas) | — | — |
| HU02 | Detalle de álbum (coleccionista) | "Unexposed Text" en el promedio; "Item descriptions" en las pistas ("Prueba" × 2, "1:11" × 3) | El número de pista se parte en dos líneas | Pistas como tres elementos sueltos |
| HU03, HU04, HU10 | Artistas, Detalle de artista (con favorito y "Agregar álbum") | 0 | — | — |
| HU07 | Crear álbum | 0 | — | — |
| HU09 | Comentar, formulario vacío | Contraste de "Publicar comentario" deshabilitado | Contador "0 / 400" apilado en vertical | "¿Qué te pareció este vinilo?" no es encabezado; la portada no dice el nombre del álbum |
| HU09 | Comentar, lleno | 0 | — | Estrellas con su descripción y estado: correcto |
| HU09 | Diálogo de descarte | Contraste de "Descartar y salir" | — | — |
| HU09 | Comentario publicado | Contraste del chip "Comentario publicado" y de las iniciales "MB"; 4 estrellas con destino táctil chico | — | — |
| HU08 | Agregar canción, vacío y lleno | Contraste de "Guardar" deshabilitado (vacío); 0 (lleno) | Sin cambios | "Datos de la canción" no es encabezado; la portada no dice el nombre del álbum |
| HU15 | Ventana de asociar, sin elección y con álbum elegido | Contraste de "Asociar" deshabilitado; asa de 32 dp; "Unexposed Text" del fondo oscurecido y de portadas con texto | Títulos truncados por "Ya asociado" | El título ya es encabezado desde el PR #114: el árbol de uiautomator no muestra esa propiedad; lo confirma la prueba |

Causas que se vieron en el código:
- **Botones deshabilitados:** los tres usan `ButtonDefaults.buttonColors()`, así que el estado deshabilitado es `onSurface` al 38 % sobre `onSurface` al 12 %.
- **Diálogo de descarte:** el tema no define los `surfaceContainer*`, y `AlertDialog` toma el lila por defecto de Material 3. Contra ese fondo, `NaranjaContraste` da 3,96:1.
- **Comentario publicado:**
  - El chip y las iniciales usan `primary` (`NaranjaContraste`) sobre `primaryContainer` (`NaranjaSuave`), que da unos 4,05:1.
  - Las estrellas del resumen reutilizan el selector (`IconButton` deshabilitado de 32 dp).
- **Promedio:** con la revisión del PR #113 la fila quedó con `clearAndSetSemantics`, así que el número deja de estar expuesto como texto y el Scanner lo marca.
- **Pistas:** `FilaCancion` tiene tres `Text` sueltos, y el número tiene un ancho fijo de 28 dp.

## Goals / Non-Goals

**Goals:**
- 0 sugerencias del Scanner en todas las pantallas y estados revisados, salvo las de portadas del backend y la del fondo oscurecido de `ModalBottomSheet`.
- Ningún cambio visual con la fuente normal, salvo el color de los botones deshabilitados, el del chip y el de las iniciales del comentario publicado.

**Non-Goals:**
- Reemplazar `ModalBottomSheet` para quitar el fondo oscurecido o sus etiquetas de Material ("Close sheet", "Drag handle"), que se traducen según el idioma del dispositivo.
- Cambiar la validación de Agregar canción, que muestra "Campo obligatorio" desde el inicio.

## Decisions

1. **Colores del botón deshabilitado.** Se define un par en `Color.kt`: fondo `SuperficieVariante` `#F0EDEC` y texto `TextoTenue` `#63636B`, que dan unos 5,1:1. Los tres botones lo usan mediante un helper común, por ejemplo `coloresBotonPrincipal()`, que conserva los colores normales y el de "enviando" de Publicar. Se verifica con un par nuevo en `ContrasteDePaletaTest`.
   *Alternativa descartada:* dejarlo, porque WCAG exime a los controles inactivos. Se descarta porque el objetivo es que el Scanner no reporte nada que la app pueda corregir.
2. **Contenedores de superficie en el tema.** `surfaceContainerLowest` y `surfaceContainerHigh` pasan a `Superficie`, `surfaceContainerLow` y `surfaceContainer` a `Fondo`, y `surfaceContainerHighest` a `SuperficieVariante`. Así los diálogos y menús toman colores de la paleta en toda la app, no solo en este diálogo. `NaranjaContraste` sobre blanco da 4,9:1.
   *Alternativa descartada:* poner `containerColor` en cada `AlertDialog`. Se descarta porque deja abiertos los demás componentes que usan esos tokens.
3. **Chip y avatar del comentario publicado.** El texto y el icono pasan a `onPrimaryContainer` (`NaranjaOscuro`, 5,5:1 sobre el fondo claro), que es el par que ya usan los chips de Inicio.
4. **Estrellas de solo lectura.** Se agrega una variante no interactiva del indicador de estrellas: iconos sin `IconButton` y una sola descripción con `comentar_estrella` ("4 de 5 estrellas"). El selector del formulario no cambia.
5. **Promedio.** Se vuelve a exponer el número como texto. Las estrellas llevan la descripción "de 5 estrellas", sin repetir el número, dentro de la columna que ya agrupa "Promedio de calificaciones". TalkBack lee "Promedio de calificaciones, 5,0, de 5 estrellas", y el Scanner ve el número como texto. La spec de la revisión anterior ("5,0 de 5 estrellas" en un solo elemento) se cumple igual: un solo elemento, y el número no se repite.
6. **Pistas.** La fila de cada pista se une en un solo elemento (`semantics(mergeDescendants = true)`), y el número usa un ancho mínimo con una sola línea (`widthIn(min = 28.dp)`, `maxLines = 1`).
7. **Asa de arrastre.** Se pasa `dragHandle = { BottomSheetDefaults.DragHandle(Modifier.padding(horizontal = 8.dp)) }`: el asa visible sigue midiendo 32 × 4 dp y el área táctil llega a 48 × 48 dp.
8. **Fuente al 200 %.**
   - El contador de Comentar va en una fila: la etiqueta con `weight(1f)` y el contador sin salto de línea.
   - En la ventana de asociar, con la fuente por encima de 1,15, la etiqueta "Ya asociado" pasa debajo del año, el mismo umbral que el titular de Ingreso.
9. **Encabezados y portadas.**
   - Los títulos nuevos usan `Modifier.encabezado()`, ya existente.
   - Las portadas de Comentar, Agregar canción y el resumen publicado reciben `detalle_portada_con_nombre`.
10. **Pruebas.** `AccesibilidadPantallasTest` agrega, con la fuente normal y al 200 %:
    - Comentar en formulario y publicado.
    - Agregar canción.
    - La ventana de asociar.
    - Comprobaciones de encabezados y de elementos combinados (pista como un nodo, promedio con el número expuesto, estrellas del resumen sin acción de clic).

## Risks / Trade-offs

- **Los `surfaceContainer*` del tema cambian el fondo de todos los diálogos y menús** (selector de fecha, menú de género y disquera). Se mitiga revisando con capturas Crear álbum, el diálogo de descarte y el selector de fecha.
- **Pruebas que buscan por texto suelto.** Unir la fila de la pista y separar el promedio puede romper pruebas que buscan "01" o "4,0 de 5 estrellas". Se mitiga corriendo `ui.detalle`, `ui.comentar`, `ui.crear` y `ui.artistas` y ajustando esas búsquedas.
- **Las portadas del backend con texto impreso** seguirán apareciendo en el Scanner. Se documenta como pendiente que no es de la app.
