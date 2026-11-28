# Resultados: HU09 Comentar un álbum

> Los nombres de las clases, archivos y pruebas se actualizaron a los del código actual después del PR [#122](https://github.com/dnunezd/IngSoftAppMobile/pull/122) (código en inglés). Los resultados son los de la fecha de ejecución.

Ejecución del 2026-11-18 sobre `feature/HU09-comentar-album`, con el cambio OpenSpec `add-comentar-album`. Las pruebas en dispositivo usaron un BackVynils local en Docker (`-PbaseUrl=http://localhost:3000/` con `adb reverse`), nunca el backend de los tutores. Las pruebas E2E son la tarea T09-5 (#51) y no son parte de este cambio.

## Suites

| Suite | Resultado | Entorno |
|---|---|---|
| Unitarias (`testDebugUnitTest`) | 449 sin fallos (45 nuevas, escritas antes del código) | JVM |
| Lint (`lintDebug`) | 0 errores; solo los 2 avisos de navigation 2.10, que son intencionales | JVM |
| Instrumentadas (`connectedDebugAndroidTest`) | 224 pruebas: 223 pasan y 1 se omite (la de siempre, `ArtistDetailE2ETest.unArtistaSinAlbumesNiPremiosMuestraLosMensajesDeVacio`) | Emulador Pixel 8 (API 34) |
| OpenSpec | `openspec validate add-comentar-album --strict` pasa | — |

Pruebas unitarias nuevas:

| Clase | Pruebas | Qué cubre |
|---|---|---|
| `CommentRepositoryTest` | 5 | Ruta y cuerpo del `POST`, y 404, 400, 500 y sin conexión como `ErrorDeCarga` |
| `AddCommentUiStateTest` | 10 | `puedePublicar`, `tieneBorrador`, niveles de calificación y límite de 400 |
| `AddCommentViewModelTest` | 24 | Carga del álbum (incluido un 404), edición, envío, guarda contra dos toques, error, salida y diálogo |
| `AlbumViewModelTest` | 3 | `actualizar()` después de comentar |
| `RepositoriosDispatcherTest` | 1 | Publicar corre en el dispatcher inyectado |
| `AppContainerTest` | 2 | El contenedor crea y permite reemplazar el repositorio |

## Recorrido en el emulador (Pixel 8, API 34)

| # | Criterio o comportamiento | Resultado | Captura |
|---|---|---|---|
| 1 | El Visitante no ve "Comentar álbum" en el detalle | Cumple | `01_detalle_visitante.jpg` |
| 2 | El Coleccionista ve "Comentar álbum" | Cumple | `02_detalle_coleccionista.jpg` |
| 3 | Se ve el nombre del álbum, sus artistas, su año y el coleccionista | Cumple | `03_formulario_vacio.jpg` |
| 4 | 5 estrellas sin elegir, "Escribe tu opinión", "0 / 400" y "Publicar comentario" deshabilitado | Cumple | `03_formulario_vacio.jpg` |
| 5 | Con una calificación y sin opinión, el botón sigue deshabilitado; al salir pregunta "¿Descartar comentario?" | Cumple | `04_dialogo_descartar.jpg` |
| 6 | "Continuar escribiendo" conserva la calificación | Cumple | — |
| 7 | Con calificación y opinión: "Excelente (5/5)", "29 / 400" y el botón habilitado | Cumple | `05_formulario_lleno.jpg` |
| 8 | Al girar a horizontal y volver, la calificación y la opinión se conservan | Cumple | — |
| 9 | Con el backend detenido: "No se pudo publicar, intenta de nuevo", sin perder lo escrito | Cumple | `06_error_publicar.jpg` |
| 10 | Con el backend arriba, reintentar publica y muestra "Comentario publicado" con el resumen | Cumple | `07_comentario_publicado.jpg` |
| 11 | "Aceptar" vuelve al detalle, que muestra el comentario nuevo; el botón flotante no lo tapa | Cumple | `08_detalle_actualizado.jpg` |
| 12 | Con borrador, "Descartar y salir" vuelve al detalle sin publicar | Cumple | — |

El backend guardó el comentario con el coleccionista 101 (Jaime Monsalve) y la calificación 5. Después del recorrido se borró con `DELETE /albums/100/comments/1`: el álbum quedó solo con su comentario original (id 100).

## Defecto encontrado y corregido en el recorrido

| Defecto | Corrección |
|---|---|
| El botón "Comentar álbum" no tenía nombre para la accesibilidad: Material oculta el texto del botón extendido, como pasó en HU07 | El ícono lleva la descripción "Comentar álbum". Se verificó de nuevo en el emulador |

## Correcciones de la revisión del PR

| Punto (@jeffrey-uandes) | Corrección |
|---|---|
| Un 404 al cargar el álbum sin guardar dejaba la pantalla en "Cargando" (y en la app la excepción escapaba del `launch`) | `AlbumNoDisponible` también lleva a `CargaAlbum.Error` con "Reintentar"; prueba nueva en `AddCommentViewModelTest` |
| Textos distintos del mockup | Subtítulo "Tu experiencia sobre el máster, prensado y audio" y "Tu apreciación audiófila ya forma parte…" |
| Faltaba la ilustración del vinilo en el éxito | Vinilo con la marca de publicado (`07_comentario_publicado.jpg`, recapturada) |
| Imports | `Session` importado en `VinilosNavHost` y orden corregido en `VinilosNavHostTest` |

## Pruebas E2E (T09-5)

Clase `AddCommentE2ETest` (cambio de OpenSpec [`add-e2e-comentar-album`](../../../../openspec/changes/add-e2e-comentar-album)). Recorre la app desde el ingreso, como la persona, con los tres escenarios más importantes: salir sin publicar, el flujo completo y el error con reintento. ESC01 y ESC02 van contra BackVynils; como publican comentarios, se corren contra el backend **local**, sobre un álbum propio (`POST /albums`) que se borra al terminar con sus comentarios. ESC03 usa `MockWebServer` para provocar el error.

| ID         | Descripción del escenario                                               | Resultado esperado                                                                                                                                                                                                                                                     | Backend          | Evidencia                 |
|------------|-------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------|---------------------------|
| FN09-ESC01 | El Coleccionista sale sin escribir y luego sale con calificación y opinión | Sin nada escrito vuelve al detalle sin preguntar; con algo escrito aparece "¿Descartar comentario?"; "Continuar escribiendo" conserva "Regular (3/5)" y la opinión; "Descartar y salir" vuelve sin publicar                                                        | BackVynils local | [PDF](e2e/FN09_ESC01.pdf) |
| FN09-ESC02 | El Coleccionista califica con 4 estrellas, escribe su opinión y publica | La pantalla muestra el álbum y "<coleccionista> (Coleccionista)" con "Publicar comentario" deshabilitado; luego "Bueno (4/5)" y el contador; al publicar, "Comentario publicado"; al aceptar, el detalle muestra el comentario; el backend lo guarda con calificación 4 | BackVynils local | [PDF](e2e/FN09_ESC02.pdf) |
| FN09-ESC03 | Publica y la publicación tarda y falla                                  | Mientras publica, "Publicando..." deshabilitado y un segundo toque no envía nada; luego "No se pudo publicar, intenta de nuevo" con "Excelente (5/5)" y la opinión conservados; al reintentar se publica y el detalle lo muestra                                        | `MockWebServer`  | [PDF](e2e/FN09_ESC03.pdf) |

Criterios de la HU09 cubiertos por las E2E:

| #   | Criterio                                                        | Escenarios   |
|-----|-----------------------------------------------------------------|--------------|
| 1   | El Coleccionista ve "Comentar álbum"                            | ESC01, ESC02 |
| 2   | La pantalla muestra el álbum y el coleccionista que comenta     | ESC02        |
| 3   | Calificación con su texto y opinión con contador                | ESC02, ESC03 |
| 4   | "Publicar comentario" deshabilitado sin calificación ni opinión | ESC02        |
| 5   | Comentario publicado y visible en el detalle                    | ESC02, ESC03 |
| 6   | Mientras publica, "Publicando..." deshabilitado y un solo envío | ESC03        |
| 7   | Error al publicar: mensaje y lo escrito conservado              | ESC03        |
| 8   | Descartar el comentario al salir                                | ESC01        |

La opinión en blanco y el límite de 400 caracteres se cubren en `AddCommentViewModelTest` y `AddCommentUiStateTest` (unitarias). Que el Visitante **no** vea "Comentar álbum" no tiene ahora ninguna prueba automatizada: lo cubría la E2E del Visitante, que se cambió por la de descartar.

Samsung SM-S938B (Android 17), 2026-11-22, contra BackVynils local: `OK (9 tests)` junto con las E2E de HU08 y HU15, sin escenarios omitidos y con todas las capturas en "Ok"; el backend quedó sin álbumes ni comentarios de prueba. Para correrlas, ver la sección E2E de [HU08](../HU08/README.md#cómo-correrlas) con la clase `com.example.vinilos.AddCommentE2ETest`.

## Sin verificar

- Android 6 (API 23) y Android 17 (API 37).
- El flujo con TalkBack activo.
