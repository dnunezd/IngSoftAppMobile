# HU10 · Agregar artistas favoritos: pruebas y evidencia

Especificación: [`openspec/changes/add-artistas-favoritos/specs/favoritos-artista/spec.md`](../../../../openspec/changes/add-artistas-favoritos/specs/favoritos-artista/spec.md). Diseño técnico en [`design.md`](../../../../openspec/changes/add-artistas-favoritos/design.md). Tareas y su estado en [`tasks.md`](../../../../openspec/changes/add-artistas-favoritos/tasks.md).

Todas las pruebas unitarias (JVM) se escribieron con TDD: ROJO (falla antes de implementar) y VERDE (pasa con el código mínimo), una prueba a la vez. Las pruebas de Compose UI (instrumentadas) se escribieron junto con la implementación y ya se ejecutaron en dispositivo físico y en emulador (ver "Resultado de la última ejecución").

**Nota sobre la pantalla:** el botón se integró primero en `PantallaArtistaProvisional` (placeholder de HU03). Cuando HU04 se fusionó a `develop`, la integración se migró a la pantalla de detalle real (`PantallaDetalleArtista`) sin cambiar el contrato del ViewModel; la clase de pruebas pasó a llamarse `PantallaDetalleArtistaFavoritoTest`. Después, a pedido del usuario y con mockups de referencia, se rediseñó visualmente la sección de favorito (botón en píldora con ícono, nota de bloqueo temporal, tarjetas de error/no-disponible), sin tocar el resto de la pantalla (biografía, estadísticas, discografía), que es territorio de HU04.

## Cada criterio de aceptación tiene al menos una prueba automatizada

| # | Criterio | Unitarias (JVM) | Compose UI (instrumentadas) |
|---|---|---|---|
| 1 | Marcar como favorito desde el detalle; el botón pasa a mostrar "Favorito" | `FavoritoViewModelTest.noFavoritoPasaAGuardandoYLuegoAFavoritoConConfirmacion`, `FavoriteRepositoryTest.agregarFavoritoUsaElEndpointDeBandaCuandoElTipoEsBanda`, `…agregarFavoritoUsaElEndpointDeMusicoCuandoElTipoEsMusico` | `PantallaDetalleArtistaFavoritoTest.favoritoMuestraElBotonDesactivadoConSuTexto` |
| 2 | Si ya es favorito, el botón aparece como "Favorito" y desactivado | `FavoritoViewModelTest.comprobandoPasaAFavoritoCuandoElArtistaYaEsFavorito`, `FavoriteRepositoryTest.esFavoritoEncuentraElArtistaPorIdEnElListadoDelColeccionista` | `PantallaDetalleArtistaFavoritoTest.favoritoMuestraElBotonDesactivadoConSuTexto` |
| 3 | El botón refleja el estado de favorito cada vez que se abre el detalle (persistencia entre sesiones de la app) | `FavoritoViewModelTest.comprobandoPasaANoFavoritoCuandoElArtistaNoEsFavorito`, `FavoriteRepositoryTest.esFavoritoDevuelveFalsoCuandoElIdNoEstaEnElListado` | `PantallaDetalleArtistaFavoritoTest.noFavoritoMuestraElBotonActivoParaAgregar` |
| 4 | Si falla la comprobación, el botón aparece desactivado con opción de reintentar | `FavoritoViewModelTest.comprobandoPasaAErrorComprobacionCuandoFallaLaConsulta`, `…reintentarComprobacionPasaDeErrorComprobacionAFavoritoOtraVez`, `FavoriteRepositoryTest.esFavoritoDevuelveErrorDeCargaSiLaConsultaFalla` | `PantallaDetalleArtistaFavoritoTest.errorComprobacionMuestraElBotonDesactivadoYPermiteReintentar` |
| 5 | El Visitante no ve el botón de favorito | — (decisión de composición en `VinilosNavHost`, ver design.md) | `PantallaDetalleArtistaFavoritoTest.visitanteNoVeNingunBotonDeFavorito` |
| 6 | Si el artista ya no existe al guardar, "El artista ya no está disponible" + botón para volver al listado actualizado | `FavoritoViewModelTest.guardarFavoritoPasaANoDisponibleCuandoElArtistaYaNoExiste` | `PantallaDetalleArtistaFavoritoTest.noDisponibleMuestraElMensajeYElBotonParaVolverAlListado`, `…noDisponibleUsaUnCallbackDeVolverAlListadoDistintoDeAtras`, `VinilosNavHostTest.alVolverDesdeArtistaNoDisponibleElListadoSeActualizaSinElArtistaEliminado` |
| 7 | Si falla la conexión/servicio al guardar, "No se pudo guardar el favorito, intenta de nuevo" + botón disponible para reintentar | `FavoritoViewModelTest.guardarFavoritoPasaAErrorGuardadoYPermiteReintentar`, `FavoriteRepositoryTest.agregarFavoritoDevuelveErrorDeCargaSiFallaLaConexion` | `PantallaDetalleArtistaFavoritoTest.errorGuardadoMuestraElMensajeYElBotonDisponibleParaReintentar`, `…errorGuardadoNoMuestraNingunaConfirmacion` |
| 8 | Mientras se guarda, el botón se desactiva y muestra "Guardando...", evitando toques repetidos | `FavoritoViewModelTest.segundoToqueMientrasGuardandoSeIgnora` | `PantallaDetalleArtistaFavoritoTest.guardandoMuestraElBotonDesactivadoConSuTexto` |
| 9 | Al guardar correctamente, mensaje breve de confirmación con el nombre del artista, que desaparece solo tras unos segundos; no aparece si falla | `FavoritoViewModelTest.confirmacionMostradaOcultaElMensajeDeFavorito` | `PantallaDetalleArtistaFavoritoTest.favoritoConConfirmacionMuestraElMensajeConElNombre`, `…laConfirmacionDesapareceSolaDespuesDeUnosSegundos` |

Cobertura: 9/9 criterios con al menos una prueba automatizada (unitaria o de Compose UI).

## Protecciones adicionales cubiertas por pruebas

| Comportamiento | Prueba |
|---|---|
| Respuesta tardía de una comprobación anterior no sobrescribe la vigente | `FavoritoViewModelTest.respuestaTardiaDeComprobacionNoReemplazaLaConsultaVigente` |
| Cancelación de la comprobación no se reporta como error de carga | `FavoriteRepositoryTest.cancelarLaComprobacionPropagaLaCancelacionSinErrorDeCarga` |

## Resultado de la última ejecución

- **Pruebas unitarias (JVM, `.\gradlew.bat testDebugUnitTest`)**: sin fallos, incluye las 8 de `FavoriteRepositoryTest` y las 11 de `FavoritoViewModelTest` de HU10, sin regresiones en el resto del proyecto.
- **Lint (`lintDebug`)**: sin errores.
- **Pruebas de Compose UI de HU10 (`PantallaDetalleArtistaFavoritoTest`, 11 pruebas)**: ejecutadas en emulador (`Perfil_prueba_23`, API 23) de forma aislada y como parte de la suite completa — **sin fallos**. Durante la primera ejecución se detectó y corrigió un bug en la propia prueba (`laConfirmacionDesapareceSolaDespuesDeUnosSegundos`: estado local declarado sin `remember`, se reiniciaba en cada recomposición) — commit `2afcd30`.
- **Suite instrumentada completa (`connectedDebugAndroidTest`, 142 pruebas)**: ejecutada en emulador tras integrar HU10 — **141/142 OK**. El único fallo es `ArtistasE2ETest.alGirarLaPantallaSeConservanLaBusquedaYElListadoSinVolverAConsultar`, un bug pre-existente de HU03 (no introducido por HU10, ver abajo).
- **Verificación manual en dispositivo físico (Samsung SM-A356E)**: confirmada por el usuario — favorito, estados y rediseño visual funcionando correctamente.

## Rediseño visual de la sección de favorito

A pedido del usuario, que compartió mockups de la pantalla "Detalle Del Artista", se rediseñó **solo la sección de favorito** (sin tocar biografía/estadísticas/discografía, territorio de HU04):

- Botón en forma de píldora con ícono de corazón (vacío/lleno); textos actualizados a "Agregar Favorito" y "Favorito".
- Nota de bloqueo temporal bajo el botón cuando ya es favorito, explicando por qué queda inactivo.
- Tarjeta de error al guardar (estilo visual consistente con `TarjetaError` de HU07: fondo `errorContainer`, ícono de advertencia) con botón "Reintentar guardar". El texto de error se mantuvo sin cambios (genérico, honesto) porque el `ViewModel` no distingue causas específicas (se descartó explícitamente inventar un diagnóstico tipo "red inestable").
- Tarjeta restyled para "artista no disponible".
- Sin dependencias nuevas: los íconos usados (`Favorite`, `FavoriteBorder`, `Lock`, `Refresh`, `Warning`) ya están incluidos en `material-icons-core`.

## Correcciones por revisión de PR (@lorennossauniandes)

Revisión del PR `1c833be` en Motorola Edge 30, Pixel 8 (API 34) y Nexus 5X (API 23) con backend local; los criterios 3, 4, 5, 7, 8 y 9 se cumplieron sin observaciones. Se reportaron dos puntos, ambos corregidos:

- **Criterio 6 — el listado no se actualizaba sin el artista eliminado:** al volver desde "El artista ya no está disponible", el `popBackStack()` simple no forzaba una nueva consulta (`ArtistsViewModel` solo consulta en `init`), así que el artista eliminado seguía visible en el listado. Se agregó un callback `onVolverAlListado` propio (distinto de `onAtras`) y una bandera `ClaveArtistaEliminado` en el `SavedStateHandle` de `VinilosNavHost` (mismo patrón que `ClaveAlbumCreado` de HU07) que dispara `ArtistsViewModel.reintentar()` al regresar. Ver `design.md` y `tasks.md` (sección 4.1) para el detalle y las pruebas nuevas: `PantallaDetalleArtistaFavoritoTest.noDisponibleUsaUnCallbackDeVolverAlListadoDistintoDeAtras` y `VinilosNavHostTest.alVolverDesdeArtistaNoDisponibleElListadoSeActualizaSinElArtistaEliminado`.
- **Criterios 1 y 2 — textos del botón no coincidían con la wiki/spec.md:** se optó por la Opción A sugerida por el revisor (alinear el código a la especificación, mismo criterio aplicado en HU07). `strings.xml`: `favoritos_agregar` pasó de "Agregar a Favoritos" a "Agregar Favorito", y `favoritos_favorito` de "En tus favoritos" a "Favorito". Se actualizaron las aserciones de texto en `PantallaDetalleArtistaFavoritoTest`.

Pendiente, fuera de este alcance: el issue de GitHub de la revisión se deja abierto a propósito (no se cierra desde este cambio).

### Segunda ronda de revisión (@lorennossauniandes, Pixel 8 API 34, BackVynils local)

238 pruebas unitarias, `lintDebug` y la suite instrumentada completa (142 pruebas, incluidas las 11 de `PantallaDetalleArtistaFavoritoTest`) sin fallos. La lógica del `ViewModel` (respuestas tardías, doble toque, 404 vs. error de red) quedó validada sin observaciones. Se reportaron 3 puntos corregidos y 4 recomendaciones no bloqueantes:

- **Cambios de HU03 fuera de alcance (corregido):** se revirtieron en `PantallaArtistas.kt` las keys explícitas de los items (`"titular"`/`"buscador"`), la key fija de `rememberSaveable` y `autoCorrect = false` del buscador; y en `ArtistasE2ETest` la llamada a `performImeAction()` agregada a la prueba de rotación. Ninguno de esos 4 intentos cambió el síntoma del bug (confirmado en `bug-busqueda-artistas-rotacion.md`), y en el Pixel 8 la prueba de rotación pasa igual en `develop` que en esta rama, lo que apunta a un problema del emulador donde se ejecutó, no del código. El diagnóstico y los 5 intentos siguen documentados sin cambios; solo se revirtió el código que no correspondía al alcance de HU10.
- **Nota confusa bajo el botón (corregido):** cuando el artista ya es favorito, el texto decía "Botón bloqueado temporalmente para evitar peticiones duplicadas", pero el bloqueo es permanente (ya es favorito), no temporal. Se cambió a "Este artista ya está en tus favoritos." en `strings.xml` (`favoritos_bloqueado_nota`).
- **Nota del `DELETE` en este README (corregido):** ver la corrección agregada en la sección "Riesgo operativo" más abajo.
- **`FavoriteRepository` fuera de `AppContainer` (corregido):** se agregó `favoriteRepository` a `AppContainer` (mismo patrón que `collectorRepository`/`artistRepository`/`albumRepository`, reemplazable en pruebas instrumentadas) y `VinilosNavHost` ya no lo crea por su cuenta; ahora lo recibe como parámetro obligatorio, que `MainActivity` pasa desde `container.favoriteRepository`. Esto evita crear un `ServiceAdapter`/`OkHttpClient` nuevo en cada recomposición y permite que una E2E con `MockWebServer` reemplace este repositorio igual que los demás, de cara a las E2E de T10-5.
- **`Closes` de T10-2/T10-3/T10-4 en la descripción del PR:** pendiente, se gestiona en GitHub al editar la descripción del PR, no en este repositorio.

## Bug pre-existente de HU03 (no introducido por HU10)

El buscador de artistas trunca el texto escrito al girar físicamente la pantalla (`ArtistasE2ETest`, ver [`bug-busqueda-artistas-rotacion.md`](./bug-busqueda-artistas-rotacion.md)). Se investigó a fondo durante la integración de HU10 porque comparte el archivo `PantallaArtistas.kt`, con 4 intentos de corrección distintos, todos verificados en dispositivo/emulador real, ninguno cambió el síntoma. Se concluyó que el origen está fuera del código de la app (comportamiento del IME/plataforma durante la recreación real de la Activity) y se documentó como riesgo conocido de HU03, sin seguir intentando correcciones a ciegas.

## Riesgo operativo: datos de prueba en el backend compartido

Al probar manualmente contra el backend compartido/desplegado (no el local), guardar un favorito hace un `POST` real y persistente. El coleccionista de prueba (normalmente id `100`) puede tener ahora artistas marcados como favoritos por pruebas manuales de esta historia. El usuario se encarga de avisar al equipo/tutores si esto requiere atención.

**Corrección (revisión de PR, @lorennossauniandes):** BackVynils sí expone un endpoint para deshacer el favorito: `DELETE /collectors/{collectorId}/musicians/{musicianId}` y `DELETE /collectors/{collectorId}/bands/{bandId}` (`collectorperformer.controller.ts`). Con eso se pueden borrar los favoritos que dejaron las pruebas manuales en el backend desplegado; la afirmación original de que "no hay endpoint DELETE" era incorrecta.

**Nota (E2E, T10-5):** el endpoint existe, pero no borra. En `collectorperformer.service.ts` (`deleteCollectorPerformer`) el resultado de `favoritePerformers.filter(...)` no se asigna: responde 204 y el favorito sigue. Además, el id del artista llega como texto (`main.ts` no convierte los `@Param` y no hay `ParseIntPipe`), así que `e.id !== performerId` compara un número con un texto y sería `true` siempre aunque se asignara el resultado. La corrección necesita las dos partes: `collector.favoritePerformers = collector.favoritePerformers.filter(e => e.id !== Number(performerId))`. Para quitar un favorito hay que borrar la fila en la tabla `performer_collectors_collector` o borrar el coleccionista (`DELETE /collectors/{id}`), que se lleva sus favoritos. Por eso las E2E crean su propio coleccionista (ver [Pruebas E2E](#pruebas-e2e-t10-5)).

## Pruebas E2E (T10-5)

Clase `ArtistasFavoritosE2ETest` (cambio de OpenSpec [`add-e2e-artistas-favoritos`](../../../../openspec/changes/add-e2e-artistas-favoritos/)). Recorre la app desde el ingreso, como la persona. ESC01 a ESC05 van contra BackVynils; como agregan favoritos, se corren contra el backend **local**. Cada escenario que agrega favoritos crea su propio coleccionista (`POST /collectors`) y lo borra al terminar (`DELETE /collectors/{id}`), lo que también borra sus favoritos. ESC06 a ESC08 usan `MockWebServer` para provocar los errores.

| ID | Descripción del escenario | Resultado esperado | Backend | Evidencia |
|---|---|---|---|---|
| FN10-ESC01 | El Visitante abre el detalle de una banda y de un músico | Ninguno de los dos muestra el botón de favorito ni consulta los favoritos | BackVynils local | [PDF](e2e/FN10_ESC01.pdf) |
| FN10-ESC02 | El Coleccionista agrega una banda como favorita | "Agregar Favorito" pasa a "Favorito" desactivado; aparece "<banda> se agregó a tus favoritos" y desaparece sola; el backend la devuelve en `GET /collectors/{id}/performers` | BackVynils local | [PDF](e2e/FN10_ESC02.pdf) |
| FN10-ESC03 | El Coleccionista agrega un músico como favorito | Lo mismo que ESC02, con el músico | BackVynils local | [PDF](e2e/FN10_ESC03.pdf) |
| FN10-ESC04 | Agrega un favorito, cierra la app, la vuelve a abrir y entra con el mismo coleccionista | El botón sigue como "Favorito" desactivado, sin confirmación, y se consultó de nuevo el backend | BackVynils local | [PDF](e2e/FN10_ESC04.pdf) |
| FN10-ESC05 | Abre el detalle de un artista que ya es su favorito | "Favorito" desactivado con "Este artista ya está en tus favoritos."; tocarlo no envía nada | BackVynils local | [PDF](e2e/FN10_ESC05.pdf) |
| FN10-ESC06 | No se puede comprobar si el artista es favorito | Botón desactivado, "No pudimos comprobar si es tu favorito" y "Reintentar"; al reintentar con el servicio arriba queda "Agregar Favorito" | `MockWebServer` | [PDF](e2e/FN10_ESC06.pdf) |
| FN10-ESC07 | Agrega un favorito y el guardado tarda y falla | Mientras guarda, "Guardando..." desactivado y un segundo toque no envía nada; luego "No se pudo guardar el favorito, intenta de nuevo" sin confirmación; "Reintentar guardar" lo guarda | `MockWebServer` | [PDF](e2e/FN10_ESC07.pdf) |
| FN10-ESC08 | Agrega un favorito y el artista ya no existe (404) | "El artista ya no está disponible"; "Volver al listado de artistas" muestra el listado ya sin ese artista | `MockWebServer` | [PDF](e2e/FN10_ESC08.pdf) |

Criterios de la [HU10](https://github.com/dnunezd/IngSoftAppMobile/wiki/HU10) cubiertos por las E2E:

| # | Criterio | Escenarios |
|---|---|---|
| 1 | Marcar como favorito una banda o un músico; el botón pasa a "Favorito" | ESC02, ESC03 |
| 2 | Si ya es favorito, "Favorito" y desactivado | ESC04, ESC05 |
| 3 | El botón refleja el favorito aunque se cierre y vuelva a abrir la app | ESC04 |
| 4 | Si no se puede comprobar, botón desactivado con opción de reintentar | ESC06 |
| 5 | El Visitante no ve el botón | ESC01 |
| 6 | Artista que ya no existe: mensaje y volver al listado actualizado | ESC08 |
| 7 | Error al guardar: mensaje y botón disponible para reintentar | ESC07 |
| 8 | Mientras guarda, "Guardando..." y desactivado | ESC07 |
| 9 | Confirmación breve al guardar; no aparece si falla | ESC02, ESC03, ESC07, ESC08 |

**Cerrar y volver a abrir (ESC04):** se cierra la Activity y se abre de nuevo dentro del mismo proceso de prueba, porque matar el proceso termina la instrumentación. La HU10 no guarda nada en el dispositivo: el botón sale de `GET /collectors/{id}/performers`, y la prueba comprueba que esa consulta se repite.

### Resultados

Emulador Pixel 8 (API 34), 2026-11-10, contra BackVynils local:

| Ejecución | Resultado |
|---|---|
| `ArtistasFavoritosE2ETest`, 3 corridas seguidas | `OK (8 tests)` en cada una, sin escenarios omitidos; 31 capturas en "Ok" (evidencia de esta carpeta: la última corrida) |
| Backend después de cada corrida | Los 2 coleccionistas de ejemplo con su favorito de ejemplo; ningún coleccionista ni favorito de prueba |
| Suite instrumentada completa | `OK (152 tests)`, ninguna omitida ni ignorada |
| `testDebugUnitTest` y `lintDebug` | 238 sin fallos; 0 errores de lint |

### Cómo correrlas

```bash
./gradlew installDebug installDebugAndroidTest -PbaseUrl=http://10.0.2.2:3000/   # BackVynils local
adb shell rm -rf /sdcard/Android/data/com.example.vinilos/files/e2e-evidencia
adb shell am instrument -w -e class com.example.vinilos.ArtistasFavoritosE2ETest com.example.vinilos.test/androidx.test.runner.AndroidJUnitRunner
adb pull /sdcard/Android/data/com.example.vinilos/files/e2e-evidencia ./e2e-evidencia
```

En un teléfono, usa `-PbaseUrl=http://localhost:3000/` y `adb reverse tcp:3000 tcp:3000`.

## Pendiente (no bloquea el cierre de T10-2/T10-3/T10-4)

- **Pruebas E2E de HU10:** hechas en un cambio aparte (`add-e2e-artistas-favoritos`); ver [Pruebas E2E](#pruebas-e2e-t10-5).
- "Quitar de favoritos" desde la UI: es una historia de usuario distinta (no HU10). La API expone `DELETE`, pero hoy no borra (ver la nota más arriba), y esta historia no implementa el botón para removerlos desde la app.
- Diseño visual del resto de la pantalla de detalle (biografía, estadísticas, discografía): a cargo de HU04, fuera de este alcance.

### Comando para repetir las pruebas instrumentadas

Con un emulador o dispositivo conectado:

```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot"
.\gradlew.bat connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=com.example.vinilos.ui.artistas.PantallaDetalleArtistaFavoritoTest"
```
