# Regresión entre versiones: v1.1.0 → v2.2.0 → develop

Compara cada entrega con la anterior. Una regresión solo puede medirse sobre lo que ya existía, así que hay dos comparaciones:

- **v1.1.0 → v2.2.0 (iteración 1 → 2):** lo que había en la v1.1.0 (HU00 a HU03).
- **v2.2.0 → develop (iteración 2 → 3):** lo que había en la v2.2.0 (suma HU04, HU07 y HU10). Lo nuevo de `develop` (HU08, HU09 y HU15) no tiene con qué compararse.

`develop` es el commit `827d420` (25/11/2026), que incluye la HU08 (#115), la accesibilidad completa e inglés (#120) y las E2E de #116. No es un release: es el candidato a la v3.1.0.

Se usaron dos métodos que se complementan:

| | [1. E2E de cada versión](#1-regresión-con-las-e2e) (principal) | [2. Recorrido por adb](#2-recorrido-por-adb-complementario) (complementario) |
|---|---|---|
| Qué compara | Las capturas que toman las E2E en cada paso, escenario por escenario | Una captura por pantalla, con el mismo recorrido en todas las versiones |
| Funcional | Sí: cada prueba pasa o falla | No |
| Puntos de comparación | 58 (1 → 2) y 143 (2 → 3), con errores, reintentos, rotación y estados vacíos | 14 (1 → 2) y 16 (2 → 3), solo el camino feliz |
| Necesita el código de la versión | Sí: cada versión corre su propia copia de las pruebas | No: maneja la app desde afuera |
| Dispositivo de esta corrida | Samsung SM-S938B (Android 17) | Emulador API 35 |

## 1. Regresión con las E2E

### Cómo se hizo

1. Se compilaron la app y sus pruebas desde cada etiqueta (`v1.1.0`, `v2.2.0`) y desde `develop`, con `-PbaseUrl=http://localhost:3000/`.
2. [`correr_e2e.py`](../../../../herramientas/regresion-visual/correr_e2e.py) instala cada versión y corre sus E2E en el mismo Samsung, contra el mismo BackVynils local, guarda el resultado de cada prueba y trae las capturas de `EvidenciaE2E`.
3. **Cada versión corre su propia copia de las pruebas.** Las de una versión no compilan contra el código de la siguiente (desde la v2.2.0 los repositorios son interfaces, #92), pero los escenarios y los nombres de los pasos con captura son idénticos en las tres versiones, así que cada captura tiene su pareja exacta (`FN01_ESC03/02_backend_caido.png`). Entre versiones, las pruebas solo cambiaron en dos verificaciones, por cambios intencionales: `ArtistasE2ETest` ya no busca "Detalle del artista" (la pantalla provisional pasó a ser el detalle real, HU04) y `ArtistaDetalleE2ETest` verifica la fecha con su nuevo formato.
4. Cada versión se corrió **dos veces**. Un paso que cambia entre dos corridas de la misma versión es **inestable** y no se usa para comparar versiones.
5. [`comparar_e2e.py`](../../../../herramientas/regresion-visual/comparar_e2e.py) compara paso por paso (con la alineación vertical de `comparar.py`) y cruza el estado de cada prueba en las dos versiones. Cada paso que cambió se revisó a mano.

Clases: `IngresoE2ETest`, `CatalogoE2ETest`, `AlbumDetalleE2ETest`, `ArtistasE2ETest` y `AlbumDetalleBackendRealTest` (v1.1.0), más `ArtistaDetalleE2ETest`, `ArtistasFavoritosE2ETest`, `CrearAlbumE2ETest` y las cinco `*SinConexionE2ETest` (v2.2.0 y develop).

### Resultado de las pruebas

| Versión | Pruebas | Resultado (las dos corridas) |
|---|---|---|
| v1.1.0 | 19 | 19 OK |
| v2.2.0 | 51 | 50 OK, 1 omitida |
| develop | 51 | 50 OK, 1 omitida |

La omitida es `FN04-ESC04` ("artista sin álbumes ni premios"): se salta sola porque el backend no tiene un artista así. **Ninguna prueba que pasaba en una versión deja de pasar en la siguiente** ([`pruebas.csv` 1 → 2](e2e/v1.1.0-a-v2.2.0/pruebas.csv), [2 → 3](e2e/v2.2.0-a-develop/pruebas.csv)).

### Ruido

[`e2e/ruido/`](e2e/ruido)

| Versión | Pasos | Iguales en las dos corridas | Inestables |
|---|---|---|---|
| v1.1.0 | 58 | 56 | 2: el catálogo capturado a veces mientras aún dice "Cargando catálogo de vinilos..." (`FN00_ESC02/04`, `FN02_ESC01/02`) |
| v2.2.0 | 143 | 139 | 4: lo mismo en `FN02_ESC01/02`; en `FN07_ESC02` y `ESC07` el álbum creado lleva la hora en el nombre y el catálogo queda en otra posición |
| develop | 143 | 143 | 0 |

### v1.1.0 → v2.2.0

58 pasos: **42 iguales, 2 inestables, 14 con cambios**. [Imágenes de los cambios](e2e/v1.1.0-a-v2.2.0) · [`resultado.csv`](e2e/v1.1.0-a-v2.2.0/resultado.csv)

| Pasos | Qué cambió | Clasificación |
|---|---|---|
| 11 pasos del catálogo y del detalle (`FN00_ESC01/03`, `FN01_ESC01/02`, `FN01_ESC02/02` y `/03`, `FN01_ESC03/02` y `/03`, `FN01_ESC04/02` y `/03`, `FN01_ESC05/02` y `/03`, `FN02_ESC01/06`), entre 0,2 % y 2,6 % | Mientras carga o sin portada se ve un ícono de disco en lugar de la ilustración "LP EDITION", y la portada tiene un fondo gris propio | Esperado: `PortadaAlbum` con "imagen genérica liviana" (`optimizar-cache-e-imagenes`, #95) |
| [`FN00_ESC03/05`](e2e/v1.1.0-a-v2.2.0/FN00_ESC03/05_catalogo_coleccionista.jpg) (13,1 %), [`FN01_ESC01/04`](e2e/v1.1.0-a-v2.2.0/FN01_ESC01/04_catalogo_coleccionista.jpg) (3,3 %) | Lo anterior y el botón "Crear álbum" para el Coleccionista | Esperado: #95 y HU07 |
| [`FN03_ESC04/03`](e2e/v1.1.0-a-v2.2.0/FN03_ESC04/03_detalle_objetivo.jpg) (6,5 %) | La pantalla provisional del artista pasó a ser el detalle real | Esperado: HU04 |

**Regresiones: ninguna.**

### v2.2.0 → develop

143 pasos: **122 iguales, 4 inestables, 17 con cambios**. [Imágenes de los cambios](e2e/v2.2.0-a-develop) · [`resultado.csv`](e2e/v2.2.0-a-develop/resultado.csv)

| Pasos | Qué cambió | Clasificación |
|---|---|---|
| [`FN00_ESC02/02`](e2e/v2.2.0-a-develop/FN00_ESC02/02_listado_coleccionistas.jpg), `FN00_ESC03/02` y `/03` (0,6 % a 1,1 %) | Avatares y flechas del selector con colores más oscuros | Esperado: contraste (Accessibility Scanner, #113) |
| 13 pasos de `FN10_ESC02` a `ESC08` (0,9 % a 4,2 %) | "Agregar álbum" junto a "Álbumes" en el detalle del artista del Coleccionista, y "Agregar Favorito" más oscuro | Esperado: HU15 y #113 |
| [`FN10_ESC04/04`](e2e/v2.2.0-a-develop/FN10_ESC04/04_app_abierta_de_nuevo.jpg) (0,11 %) | El saludo muestra el coleccionista de prueba, cuyo nombre lleva la hora | Dato dinámico, no es un cambio |

Quedaron idénticos todos los pasos de HU00 a HU04 que recorre el Visitante y los estables de crear un álbum (HU07); los escenarios sin conexión no capturan, pero siguen pasando. **Regresiones: ninguna.**

## 2. Recorrido por adb (complementario)

### Cómo se hizo

[`capturar.py`](../../../../herramientas/regresion-visual/capturar.py) recorre la app desde afuera con adb: lee la pantalla con `uiautomator dump`, toca por texto y captura con `screencap`. No usa el código de ninguna versión, así que el mismo recorrido sirve para las tres. Las tres versiones se capturaron seguidas en el emulador (API 35, 1080×2400), sin animaciones, con la barra de estado en modo demo y con la app en español (`cmd locale set-app-locales`, porque `develop` también tiene inglés). `comparar.py` quita las barras del sistema y alinea las capturas antes de comparar.

### Ruido

[`recorrido/ruido/`](recorrido/ruido): 0 % en 45 de 46 pantallas repetidas; 0,65 % en `09_detalle_album_canciones` de `develop`, donde el botón flotante "Comentar álbum" no se mueve con la lista. La primera versión del recorrido deslizaba rápido y daba hasta 9,5 % por la inercia; se cambió a un gesto lento.

### v1.1.0 → v2.2.0

14 pantallas: 9 iguales y 5 con cambios. [Imágenes](recorrido/v1.1.0-a-v2.2.0)

| Pantalla | % | Qué cambió | Clasificación |
|---|---|---|---|
| [06 Catálogo (Coleccionista)](recorrido/v1.1.0-a-v2.2.0/06_catalogo_coleccionista.jpg) | 21,98 | "Crear álbum"; portadas completas en lugar de recortadas | HU07; portadas: ver abajo |
| [16 Catálogo (Visitante)](recorrido/v1.1.0-a-v2.2.0/16_catalogo_visitante.jpg) | 20,19 | Las mismas portadas | Ver abajo |
| [08](recorrido/v1.1.0-a-v2.2.0/08_detalle_album.jpg) y [09](recorrido/v1.1.0-a-v2.2.0/09_detalle_album_canciones.jpg) Detalle del álbum | 0,41 | Fondo gris detrás de la portada; foto del artista con otro encuadre | #95 |
| [13 Detalle del artista](recorrido/v1.1.0-a-v2.2.0/13_detalle_artista.jpg) | 12,73 | La pantalla provisional pasó a ser el detalle real | HU04 y HU10 |

**Las portadas recortadas dependen del dispositivo, no de la versión.** En el emulador, la v1.1.0 mostraba las portadas del catálogo ampliadas y recortadas; en el Samsung (método 1), la misma v1.1.0 las muestra completas, igual que la v2.2.0. Así que no es un cambio visual de la v2.2.0: lo más probable es que la v1.1.0 calculara mal el tamaño de la portada en algunas pantallas y que eso dejara de pasar al reescribirse `PortadaAlbum` en #95. Ninguna versión pierde información.

### v2.2.0 → develop

16 pantallas: 10 iguales y 6 con cambios. [Imágenes](recorrido/v2.2.0-a-develop)

| Pantalla | % | Qué cambió | Clasificación |
|---|---|---|---|
| [03](recorrido/v2.2.0-a-develop/03_selector_coleccionistas.jpg) y [04](recorrido/v2.2.0-a-develop/04_selector_busqueda.jpg) Selector | 1,03 y 0,56 | Avatares y flechas más oscuros | #113 |
| [08 Detalle del álbum](recorrido/v2.2.0-a-develop/08_detalle_album.jpg) | 2,77 | "Comentar álbum" | HU09 |
| [09 Detalle del álbum, canciones](recorrido/v2.2.0-a-develop/09_detalle_album_canciones.jpg) | 7,55 | "Agregar canción" y "Comentar álbum" | HU08 y HU09 |
| [13](recorrido/v2.2.0-a-develop/13_detalle_artista.jpg) y [14](recorrido/v2.2.0-a-develop/14_detalle_artista_albumes.jpg) Detalle del artista | 5,47 y 5,71 | "Agregar álbum" y "Agregar Favorito" más oscuro | HU15 y #113 |

Las dos pantallas que el método 1 no ve (porque las E2E no las capturan con el Coleccionista) son el detalle del álbum con "Agregar canción" y "Comentar álbum": el recorrido sí las cubre.

## Conclusión

**No hay regresiones de la v1.1.0 a la v2.2.0 ni de la v2.2.0 a `develop`.** Todas las pruebas que pasaban siguen pasando y cada diferencia visual corresponde a algo planeado: las historias nuevas (HU04, HU07, HU08, HU09, HU10 y HU15), la "imagen genérica liviana" (#95) y las correcciones de contraste (#113).

## Qué tan confiable es

**Lo que está bien planteado:**

- Comparar solo lo que existía en la versión anterior es lo correcto: una pantalla nueva no tiene línea base, y su regresión se mide en la siguiente entrega.
- Las líneas base salen de las etiquetas del repositorio y se pueden regenerar.
- El ruido se midió con dos corridas por versión, y los pasos inestables se excluyen en lugar de contarse como cambios.
- Con las E2E, la parte funcional (pasa o falla) y la visual se miden en los mismos puntos, los que definió el equipo para cada escenario.
- Los dos métodos coinciden en todo, salvo en las portadas de la v1.1.0, y esa diferencia se explica por el dispositivo.

**Lo que hay que tener en cuenta:**

1. **Detecta cambios, no regresiones.** Entre releases casi todo cambio es intencional. El porcentaje solo dice dónde mirar; la clasificación se hace a mano contra lo que planeaba cada entrega.
2. **"La misma prueba" es la copia de cada versión.** Las pruebas viejas no compilan contra el código nuevo. Aquí las copias son equivalentes (mismos escenarios y pasos, y solo dos verificaciones adaptadas a cambios intencionales), pero hay que revisarlo en cada corrida.
3. **`develop` no es un release.** La comparación 2 → 3 vale para `827d420`; lo ideal es repetirla sobre la etiqueta `v3.1.0`.
4. **Datos dinámicos.** Los escenarios que crean datos ponen la hora en los nombres; esos pasos salen como inestables o con diferencias mínimas que hay que reconocer como tales.
5. **Cobertura.** Solo se compara lo que las E2E capturan (las pruebas sin conexión no capturan nada) y lo que recorre `capturar.py`. No se compara la app en inglés, en modo oscuro o con la fuente ampliada.
6. **Dispositivo.** Cada método corrió en un solo dispositivo, y las portadas muestran que el resultado puede depender de él. Para la entrega conviene correr el método 1 también en la API 23.
7. **Estado del backend.** Todo se corrió contra el mismo BackVynils, que tiene datos de pruebas anteriores (por ejemplo, canciones "Prueba" y "abc" en "Buscando América"). Son iguales para todas las versiones y no afectan la comparación, pero esas capturas no sirven como imagen "oficial" de la app.
8. **APK de depuración**, no los publicados (que apuntan al backend desplegado). El código de la interfaz es el mismo.

## Cómo repetirla

Ver [`herramientas/regresion-visual/README.md`](../../../../herramientas/regresion-visual/README.md).
