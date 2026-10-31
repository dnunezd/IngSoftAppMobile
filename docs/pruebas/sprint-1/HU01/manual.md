# HU01 · Verificación manual del flujo (OBJ04)

Los casos se ajustaron a la decisión de no modificar la base de datos: lo que necesitaba cambiar datos (portada inválida, 50 álbumes, catálogo vacío, álbum nuevo) se cubre con las pruebas automáticas indicadas. En la verificación manual se recorre el flujo real con los 4 álbumes de BackVynils.

Ejecutada el 2026-10-28 por ADB (toques, deslizamientos y giros reales en el dispositivo; textos y orden leídos con `uiautomator`; portadas revisadas en las capturas). El backend se detuvo de verdad con `docker stop` en M-01-07.

Entornos: **Motorola Edge 30** (físico, Android 14, `adb reverse tcp:3000 tcp:3000` y app instalada con `-PbaseUrl=http://localhost:3000/`) y **Nexus 5X API 23** (emulador, backend en `10.0.2.2`).

| Caso | Pasos | Resultado esperado | Motorola Edge 30 | Nexus 5X API 23 |
|---|---|---|---|---|
| M-01-01 | Entrar como Visitante y pulsar "Ver álbumes". | Etiqueta VISITANTE; 4 álbumes con portada, nombre y género en orden: A Day at the Races, A Night at the Opera, Buscando América, Poeta del pueblo. | Cumple | Cumple |
| M-01-02 | Revisar la portada de cada álbum. Automática: `PantallaCatalogoTest.unaPortadaVaciaOInvalidaMuestraLaImagenGenericaSinRomperLaLista`. | Cada álbum tiene su portada o la imagen genérica; la lista sigue funcionando. | Cumple: las 4 portadas reales | Cumple: 3 reales y la genérica en A Night at the Opera (limitación de Android 6 con Wikimedia) |
| M-01-03 | Recorrer la lista hasta el final y volver arriba. Automática con 50 álbumes: `CatalogoListaLargaTest`. | El desplazamiento es fluido y la app no se cierra. | Cumple | Cumple |
| M-01-04 | Con el catálogo abierto, deslizar hacia abajo desde arriba. Automática con un álbum nuevo: `CatalogoActualizarTest`. | Aparece el indicador, la lista se mantiene y la app no se cierra. | Cumple | Cumple |
| M-01-05 | Desplazarse hasta "Poeta del pueblo" y girar la pantalla (y volver a girar). Automática con 50 álbumes: `RotacionTest.alGirarElCatalogoConservaSuPosicionSinVolverAConsultar`. | Se conserva la posición y no aparece de nuevo el indicador de carga. | Cumple (4 columnas en horizontal) | Cumple (3 columnas en horizontal) |
| M-01-06 | Catálogo vacío. Automática: `PantallaCatalogoTest.sinAlbumesMuestraElMensajeDeCatalogoVacio`, `CatalogoActualizarTest.alDeslizarElCatalogoVacioMuestraLosAlbumesQueLleguen`. | "Aún no hay álbumes en el catálogo". | No aplica (sin cambios en la BD) | No aplica |
| M-01-07 | Detener el backend (`docker stop backvynils-web-1`) y abrir el catálogo; levantarlo (`docker start backvynils-web-1`) y pulsar "Reintentar". | "No pudimos cargar el catálogo" con "Reintentar"; al reintentar se ve la lista; la app no se cierra. | Cumple | Cumple |
| M-01-08 | Entrar como Coleccionista (Manolo Bellon) y abrir el catálogo. | Etiqueta COLECCIONISTA; el mismo catálogo que ve el Visitante. | Cumple | Cumple |
| M-01-09 | Tocar "Buscando América" y volver con "Atrás". | Se abre su detalle; al volver se ve el catálogo en la misma posición, sin indicador de carga. | Cumple | Cumple |

Cierres inesperados durante la verificación manual (`adb logcat -b crash`): Motorola **0** · Nexus 5X **0**.

Resultado: 8/8 casos aplicables cumplen en los dos entornos (M-01-06 no aplica porque no se modifica la base de datos; lo cubren las pruebas automáticas indicadas).
