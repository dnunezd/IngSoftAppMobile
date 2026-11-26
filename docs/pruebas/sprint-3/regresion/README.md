# Regresión visual entre versiones: v1.1.0 → v2.2.0 → develop

Compara, pantalla por pantalla, cómo se ve la app en cada entrega. Una regresión solo puede medirse sobre lo que ya existía, así que hay dos comparaciones:

- **v1.1.0 → v2.2.0 (iteración 1 → 2):** las 14 pantallas de HU00 a HU03 que ya había en la v1.1.0.
- **v2.2.0 → develop (iteración 2 → 3):** las 16 pantallas de la v2.2.0, que suman HU04, HU07 y HU10. Las pantallas nuevas de `develop` (HU08, HU09 y HU15) no tienen con qué compararse.

`develop` es el commit `827d420` (25/11/2026), que incluye la HU08 (#115), la accesibilidad completa e inglés (#120) y las E2E de #116. No es un release: es el candidato a la v3.1.0.

## Cómo se hizo

1. Se compiló un APK de depuración de cada versión desde su etiqueta (`v1.1.0`, `v2.2.0`) y desde `develop`, todos con `-PbaseUrl=http://localhost:3000/`, para usar el mismo BackVynils local.
2. [`herramientas/regresion-visual/capturar.py`](../../../../herramientas/regresion-visual) recorre la app desde afuera, con adb: lee la pantalla con `uiautomator dump`, toca por texto y guarda una captura por pantalla. No cambia el código de ninguna versión, así que el mismo recorrido sirve para las tres.
3. Las tres versiones se capturaron seguidas en el mismo emulador (Pixel, API 35, 1080×2400), contra el mismo estado del backend, sin animaciones, con la barra de estado fija (modo demo) y con la app en español (`cmd locale set-app-locales`, porque `develop` también tiene inglés).
4. [`comparar.py`](../../../../herramientas/regresion-visual/comparar.py) quita las barras del sistema, alinea verticalmente las dos capturas (el gesto de deslizar no se detiene siempre en el mismo píxel) y cuenta los píxeles que cambian más de 40 niveles en algún canal. Por cada pantalla deja una imagen con la versión base, la nueva y las diferencias en rojo.
5. Cada pantalla que cambió se revisó a mano y se clasificó según lo que planeaba cada entrega.

## Ruido del método

Antes de comparar versiones se capturó cada versión dos veces y se comparó consigo misma: lo que cambie ahí no es regresión, es falta de determinismo ([`ruido/`](ruido)).

| Versión | Pantallas | Iguales | Con diferencia |
|---|---|---|---|
| v1.1.0 | 14 | 14 | 0 |
| v2.2.0 | 16 | 16 | 0 |
| develop | 16 | 15 | 1: `09_detalle_album_canciones`, 0,65 % (el botón flotante "Comentar álbum" no se mueve con la lista, así que al alinear la lista queda corrido) |

La primera versión deslizaba con un gesto rápido y daba hasta 9,5 % de diferencia en esa pantalla por la inercia; se cambió a un gesto lento y se agregó la alineación. Con eso, el piso de ruido es **0 %** en las pantallas sin desplazamiento y **≤ 0,65 %** en la única pantalla desplazada con un elemento fijo. Una diferencia por debajo de 0,1 % se considera igual.

## v1.1.0 → v2.2.0

[Imágenes](v1.1.0-a-v2.2.0) · [`resultado.csv`](v1.1.0-a-v2.2.0/resultado.csv)

| Pantalla | % distinto | Qué cambió | Clasificación |
|---|---|---|---|
| 01 a 05, 10 a 12, 15 (ingreso, selector, búsqueda, inicio, artistas) | 0,00 | Nada | Igual |
| [06 Catálogo (Coleccionista)](v1.1.0-a-v2.2.0/06_catalogo_coleccionista.jpg) | 21,98 | Botón "Crear álbum"; las portadas se ven completas sobre fondo gris en lugar de recortadas | Esperado (HU07) + **cambio visual no documentado** |
| [16 Catálogo (Visitante)](v1.1.0-a-v2.2.0/16_catalogo_visitante.jpg) | 20,19 | Las mismas portadas; el Visitante **no** ve "Crear álbum" | **Cambio visual no documentado** |
| [08 Detalle del álbum](v1.1.0-a-v2.2.0/08_detalle_album.jpg) | 0,41 | Fondo gris redondeado detrás de la portada; foto del artista con otro encuadre | **Cambio visual no documentado** (menor) |
| [09 Detalle del álbum, canciones](v1.1.0-a-v2.2.0/09_detalle_album_canciones.jpg) | 0,41 | Foto del artista con otro encuadre | **Cambio visual no documentado** (menor) |
| [13 Detalle del artista](v1.1.0-a-v2.2.0/13_detalle_artista.jpg) | 12,73 | La pantalla provisional pasó a ser el detalle real, con datos, álbumes, premios y "Agregar Favorito" | Esperado (HU04 y HU10) |

**Origen del cambio de portadas:** el catálogo pide `ContentScale.Crop` en las dos versiones, pero `PortadaAlbum` se reescribió en `optimizar-cache-e-imagenes` (#95): fondo neutro propio y un ícono vectorial como respaldo en lugar de la ilustración. No se pierde información (ahora la portada se ve entera), pero cambió el aspecto de tres pantallas de HU01 y HU02 sin que el cambio de OpenSpec ni el release lo mencionen. Conviene confirmarlo con el equipo.

**Regresiones funcionales: ninguna.** Todo lo que se podía hacer en la v1.1.0 se puede hacer igual en la v2.2.0.

## v2.2.0 → develop

[Imágenes](v2.2.0-a-develop) · [`resultado.csv`](v2.2.0-a-develop/resultado.csv)

| Pantalla | % distinto | Qué cambió | Clasificación |
|---|---|---|---|
| 01, 02, 06, 07, 10 a 12, 16 | 0,00 | Nada | Igual |
| 05 y 15 (inicio) | 0,06 | Antialiasing de los textos, por debajo del umbral | Igual |
| [03 Selector de coleccionistas](v2.2.0-a-develop/03_selector_coleccionistas.jpg) | 1,03 | Avatares y flechas con colores más oscuros | Esperado: contraste (Accessibility Scanner, #113) |
| [04 Selector con búsqueda](v2.2.0-a-develop/04_selector_busqueda.jpg) | 0,56 | Lo mismo | Esperado (#113) |
| [08 Detalle del álbum](v2.2.0-a-develop/08_detalle_album.jpg) | 2,77 | Botón flotante "Comentar álbum" | Esperado (HU09) |
| [09 Detalle del álbum, canciones](v2.2.0-a-develop/09_detalle_album_canciones.jpg) | 7,55 | Botón "Agregar canción" en la sección de canciones y "Comentar álbum" | Esperado (HU08 y HU09) |
| [13 Detalle del artista](v2.2.0-a-develop/13_detalle_artista.jpg) | 5,47 | "Agregar álbum" junto a "Álbumes" y "Agregar Favorito" más oscuro | Esperado (HU15 y #113) |
| [14 Detalle del artista, álbumes](v2.2.0-a-develop/14_detalle_artista_albumes.jpg) | 5,71 | Lo mismo; la lista baja unos píxeles porque el encabezado con el botón es más alto | Esperado (HU15) |

**Regresiones: ninguna.** Todas las diferencias corresponden a algo planeado: las historias nuevas (HU08, HU09 y HU15) y las correcciones de contraste. El formulario de crear álbum (HU07) y los catálogos quedaron idénticos.

## Qué tan confiable es esta comparación

**Lo que está bien planteado:**

- Comparar solo lo que existía en la versión anterior (1 → 2 sobre las pantallas de la v1.1.0, 2 → 3 sobre las de la v2.2.0) es lo correcto: una pantalla nueva no tiene línea base, y su "regresión" se mide en la siguiente entrega.
- Las líneas base salen de las etiquetas del repositorio y se pueden regenerar: el resultado no depende de capturas guardadas a mano.
- El ruido se midió (0 % en 45 de 46 pantallas repetidas), así que una diferencia mayor que 0,65 % no es azar.

**Lo que hay que tener en cuenta:**

1. **Detecta cambios, no regresiones.** Entre releases casi todo cambio es intencional (historias nuevas, rediseños, accesibilidad). El porcentaje solo dice dónde mirar; la clasificación "esperado o regresión" se hace a mano contra lo que planeaba cada entrega. Aquí eso fue viable porque son 30 pantallas.
2. **`develop` no es un release.** La comparación 2 → 3 vale para el commit `827d420`; si `develop` sigue cambiando antes de la v3.1.0 hay que repetirla. Lo ideal es correrla sobre la etiqueta `v3.1.0` cuando exista.
3. **Es visual, no funcional.** Ve que un botón aparece, no que funcione. La regresión funcional se cubre corriendo las pruebas actuales (que incluyen las E2E de las historias anteriores) sobre la versión nueva; la T08-6 pide justamente las dos cosas.
4. **Cubre el camino feliz con datos.** No compara estados de carga, error, vacío o sin conexión, ni la app en inglés, en modo oscuro o con la fuente ampliada.
5. **Un dispositivo y una resolución.** Emulador API 35, 1080×2400. Otra densidad o la API 23 pueden mostrar diferencias que aquí no aparecen.
6. **Depende del estado del backend.** Las tres versiones se capturaron seguidas contra el mismo BackVynils. Ese backend tiene datos de pruebas anteriores (por ejemplo, canciones "Prueba" y "abc" en "Buscando América"); no afectan la comparación porque son iguales para las tres versiones, pero hacen que las capturas no sirvan como imagen "oficial" de la app.
7. **Se compararon APK de depuración, no los APK publicados**, porque los publicados apuntan al backend desplegado. El código de la interfaz es el mismo.
8. **La alineación vertical puede ocultar un corrimiento pequeño.** Por eso cada fila del CSV dice cuántos píxeles se desplazó (`desplazamiento_px`); en estas corridas solo se alinearon las pantallas deslizadas.

**Conclusión:** sirve para responder "¿qué cambió a la vista entre entregas y era lo planeado?" con buena precisión (sin falsos positivos por ruido) y encontró un cambio visual no documentado (las portadas en la v2.2.0). No reemplaza la regresión funcional ni cubre todos los estados de la app.

## Cómo repetirla

Ver [`herramientas/regresion-visual/README.md`](../../../../herramientas/regresion-visual/README.md).
