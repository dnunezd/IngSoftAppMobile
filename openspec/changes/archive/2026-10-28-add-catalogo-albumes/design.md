# Design

## Context

La rama parte de `feature/HU02-consultar-detalle-album` (PR #71 abierto), que ya trae lo que el catálogo necesita: `ServiceAdapter`, `AlbumApi` (solo `GET albums/{id}`), `AlbumDto`, el modelo `Album`, `AlbumRepository.obtenerAlbum(id)`, la portada con Coil (`PortadaAlbum`, con `inicio_albumes` como respaldo), la ruta `album/{id}` y `RepositorioAlbumesFalso`. El destino `catalogo` aún muestra `PantallaCatalogoProvisional`.

`GET /albums` devuelve los mismos campos que el detalle, incluidos `performers` y `releaseDate` como timestamp ISO en UTC (`1984-08-01T00:00:00.000Z`). Los 4 álbumes de ejemplo (100 a 103) son los de los mockups de Stitch, que muestran una cuadrícula de 2 columnas.

Restricciones: Compose BOM 2024.04.01 (Material 3 1.2.1), `minSdk` 23, ninguna dependencia nueva, textos literales de la HU y, cuando la HU no los define, los del mockup.

## Goals / Non-Goals

**Goals:**
- Reutilizar la capa de datos y la portada de la HU02 sin duplicarlas.
- Que un álbum incompleto no tumbe todo el catálogo.
- Que la rotación y el regreso desde el detalle no repitan la consulta.
- Que todos los estados se puedan probar sin backend (repositorio falso y MockWebServer).

**Non-Goals:**
- Paginación: el backend no la ofrece y el volumen esperado es bajo.
- Caché persistente o modo sin conexión.
- Filtrar, buscar o cambiar el orden.

## Decisions

### D1. Conversión tolerante en el catálogo, separada de la del detalle
`AlbumRepository.obtenerAlbumes(): Result<List<AlbumResumen>>` llama a `AlbumApi.obtenerAlbumes(): List<AlbumDto>` dentro de `adaptador.llamar { }` y convierte cada DTO con `mapNotNull`: descarta los que no tienen `id`, `name` o `genre`; `cover`, `performers` y `releaseDate` son opcionales.

Se crea un modelo `AlbumResumen(id, nombre, portada, genero, artista, anio)` en lugar de reutilizar `Album`, porque `Album` exige descripción, disquera, fecha y listas, y su conversión (`requerido`) falla todo el resultado si falta un campo. Esa regla es correcta para el detalle, pero en el catálogo dejaría la lista vacía por un solo registro malo.

- `artista: String` (no nullable): los nombres no vacíos de `performers` unidos con ", ". Si no hay `performers`, la lista está vacía o ningún artista trae nombre, es `""` (cadena vacía), nunca `null`. Un artista sin nombre se omite sin afectar a los demás. La pantalla no dibuja la línea del artista cuando es `""`, para no dejar un hueco en la tarjeta.
- `anio`: los 4 primeros caracteres de `releaseDate` si son dígitos. La fecha llega en UTC y la parte de fecha del ISO es el día UTC, así que no hace falta `SimpleDateFormat` y el año no depende de la zona del teléfono.

*Alternativa descartada:* relajar `requerido` en el detalle. Cambiaría el comportamiento ya verificado de la HU02.

### D2. Orden en el repositorio
El repositorio ordena con `Collator` en español con fuerza `PRIMARY` (igual que `ArtistRepository`) y desempata por `id` para que el orden sea estable. Así el ViewModel y la UI reciben la lista lista, y el orden se prueba con MockWebServer sin UI. El comparador queda en una función interna probada con `OrdenAlbumesTest`.

### D3. Estado y ViewModel (MVVM de la rama)
Se sigue la arquitectura vigente: View (Compose sin estado) ← `StateFlow` ← ViewModel → repositorio `open` → `ServiceAdapter` → Retrofit, con inyección manual desde `AppContainer` y `viewModelFactory` en el NavHost. El ViewModel no conoce Android, Retrofit ni la sesión; el rol lo lee el NavHost de `SessionViewModel`, como hoy hace el destino provisional.

```
sealed interface AlbumsUiState
  data object Cargando
  data class  Exito(albumes: List<AlbumResumen>,
                    actualizando: Boolean = false,
                    errorAlActualizar: Boolean = false)
  data class  Vacio(actualizando: Boolean = false,
                    errorAlActualizar: Boolean = false)
  data object Error
```

Se toman tres convenciones de `AlbumViewModel` (HU02):
- **Resultados tardíos:** un contador `consultaVigente` descarta la respuesta de una consulta que ya fue reemplazada (por ejemplo, deslizar dos veces), además de cancelar el `Job` anterior como `ArtistsViewModel`.
- **Errores:** `ErrorDeCarga` → `Error`; cualquier otra excepción se relanza (`else -> throw it`), para no disfrazar errores de programación como fallos de red.
- **Reintento:** `reintentar()` solo actúa desde `Error` y pasa por `Cargando`.

Comportamiento:
- `init` carga una vez. Como el ViewModel pertenece a la entrada `catalogo` del NavHost, sobrevive a la rotación y al ir y volver del detalle: no hay segunda consulta.
- `actualizar()` (deslizar, desde `Exito` o `Vacio`) pone `actualizando = true` sin quitar la lista. Si tiene éxito, publica la lista nueva (o `Vacio`). Si falla, conserva los álbumes con `actualizando = false` y `errorAlActualizar = true`. La pantalla muestra un `Snackbar` con "No pudimos cargar el catálogo" y llama a `avisoMostrado()`, que vuelve el campo a `false`. Se reutiliza el texto de la HU para no inventar uno nuevo.

*Alternativas descartadas:* pasar a `Error` si falla la actualización, porque haría perder una lista válida; y un evento con `SharedFlow`/`Channel`, porque el proyecto no usa eventos y el aviso modelado como estado se prueba igual que los demás estados y sobrevive a la rotación.

### D4. Pantalla y cuadrícula
`PantallaCatalogo(estado, session, onActualizar, onReintentar, onAvisoMostrado, onSeleccionar, onAtras)` es sin estado, como las demás pantallas.

- `LazyVerticalGrid(GridCells.Adaptive(minSize = 156.dp))`: 2 columnas en vertical, como el mockup, y más en horizontal. El encabezado (titular, subtítulo, "Catálogo" + contador) va como ítems de ancho completo (`GridItemSpan(maxLineSpan)`) para que todo se desplace junto y la posición se conserve como una sola lista.
- `rememberLazyGridState()` es `rememberSaveable`, así que la posición sobrevive a la rotación sin código extra.
- Cada tarjeta usa `key = album.id` y `contentType` para un desplazamiento fluido con listas largas.
- La portada reutiliza `PortadaAlbum`, a la que se agregan los parámetros `contentDescription` y `contentScale` (por defecto, los actuales) para usar `Crop` en la tarjeta sin afectar el detalle. El año se muestra como insignia sobre la portada y debajo, junto al género ("Salsa · 1984"), como en el mockup.
- La etiqueta de rol reutiliza `ChipEtiqueta`. La flecha de atrás sigue el encabezado de `PantallaArtistas`.
- Carga: un `CircularProgressIndicator` con "Cargando catálogo de vinilos..." y tarjetas esqueleto (mockup).
- Error y vacío: tarjeta centrada con el ícono del mockup, el texto de la HU y, en el error, el botón "Reintentar". Se omiten "Restablecer filtros", "Explorar todos los géneros" y los subtítulos que hablan de filtros o búsqueda.
- Tags para pruebas: `TagCatalogo` (se conserva), `TagRolActivo` (se conserva), `TagProgresoCatalogo`, `TagErrorCatalogo`, `TagReintentarCatalogo`, `TagVacioCatalogo`, `TagCuadriculaCatalogo`, `TagContadorCatalogo` y `tagAlbum(id)`.

### D5. Deslizar para actualizar con Material 3 1.2.1
`PullToRefreshContainer` + `rememberPullToRefreshState()` + `Modifier.nestedScroll(state.nestedScrollConnection)` (API `@ExperimentalMaterial3Api`). Un `LaunchedEffect(state.isRefreshing)` llama a `onActualizar()`, y otro sincroniza `startRefresh()`/`endRefresh()` con `actualizando`. El estado `Vacio` se dibuja dentro de un contenedor desplazable para que también se pueda deslizar.

*Alternativa descartada:* subir el BOM para usar `PullToRefreshBox` (M3 1.3). Afecta a toda la app y a las HU ya verificadas.

### D6. Inyección y navegación
- `AppContainer(albumesRepositorio: AlbumRepository? = null)` y `albumRepository = albumesRepositorio ?: AlbumRepository(serviceAdapter)`.
- `MainActivity` pasa `container.albumRepository`; `VinilosNavHost` lo recibe sin valor por defecto (se quitan `LocalContext` y `VinilosApplication`). Las pruebas que construyen el NavHost pasan `RepositorioAlbumesFalso`.
- El destino `catalogo` crea `AlbumsViewModel` con `viewModelFactory`, lee el rol de `sessionViewModel` y al tocar un álbum navega con `Rutas.detalleAlbum(id)` y `launchSingleTop = true`.
- Se eliminan `PantallaCatalogoProvisional`, `AlbumIdProvisional`, `TagCatalogoAlbumProvisional`, los textos `catalogo_album_provisional_*`, `catalogo_rol_coleccionista` (con nombre) y `PantallaCatalogoProvisionalTest`.

### D7. Íconos
Se copian como vectores `res/drawable/ic_cat_*.xml` los Material Symbols Outlined (Apache 2.0) que usa el mockup y que no están en `material-icons-core`: `graphic_eq`, `music_note`, `album`, `music_off` y `priority_high` (rellenos donde el mockup usa `FILL 1`). No se agrega `material-icons-extended`.

### D8. Pruebas y objetivos de calidad (OBJ01 a OBJ03)
Todas las pruebas se escriben con TDD en tres etapas: ROJO (la prueba falla antes de implementar), VERDE (el código mínimo la hace pasar) y AZUL (refactor sin cambiar el comportamiento, con las pruebas otra vez en verde). El informe de cada grupo registra las tres ejecuciones. "Integración" significa aquí instrumentada con Compose Test sobre la app real con un repositorio falso o con MockWebServer; "E2E" significa contra BackVynils real.

`RepositorioAlbumesFalso` se amplía con `obtenerAlbumes()`, una cola de resultados (para simular una segunda respuesta al deslizar) y el contador `consultasCatalogo`.

**OBJ01, matriz de trazabilidad.** Cada criterio tiene al menos una prueba automatizada:

```
#   Criterio                         Unitaria                       Integración (Compose Test)
--  -------------------------------  -----------------------------  ------------------------------------
1   Visitante y Coleccionista        —                              VinilosNavHostTest (ambos roles),
    ven el catálogo                                                 PantallaCatalogoTest (etiqueta rol)
2   Portada, nombre y género         AlbumRepositoryTest            PantallaCatalogoTest
                                     (conversión)
3   Indicador de progreso            AlbumsViewModelTest            PantallaCatalogoTest
                                     (Cargando)
4   Error + "Reintentar"             AlbumRepositoryTest (500,      PantallaCatalogoTest, RecuperacionTest
                                     red, JSON), AlbumsViewModel-
                                     Test (Error → reintentar)
5   Catálogo vacío                   AlbumRepositoryTest,           PantallaCatalogoTest
                                     AlbumsViewModelTest
6   Portada genérica                 —                              PantallaCatalogoTest (URL vacía e
                                                                    inválida), PortadaAlbumTest
7   Muchos álbumes                   —                              CatalogoListaLargaTest (50 álbumes)
8   Orden alfabético                 OrdenAlbumesTest,              PantallaCatalogoTest (orden en
                                     AlbumRepositoryTest            pantalla)
9   Rotación sin recarga             AlbumsViewModelTest (una       RotacionTest (posición y una sola
                                     sola consulta)                 consulta)
10  Deslizar para actualizar         AlbumsViewModelTest            CatalogoActualizarTest
                                     (actualizar)
```

**OBJ02, estados y rutas.** Estados visibles del catálogo, cada uno con su prueba en `PantallaCatalogoTest` (o la indicada):

```
Estado visible                                   Prueba
-----------------------------------------------  --------------------------------------------
Cargando (indicador y texto)                     PantallaCatalogoTest
Éxito con artista y año                          PantallaCatalogoTest
Éxito sin artista ni año                         PantallaCatalogoTest
Éxito con portada por defecto                    PantallaCatalogoTest
Vacío                                            PantallaCatalogoTest
Error con "Reintentar"                           PantallaCatalogoTest
Actualizando (indicador sobre la lista)          PantallaCatalogoTest
Aviso "No pudimos cargar el catálogo" al         PantallaCatalogoTest
fallar la actualización
Etiqueta VISITANTE / COLECCIONISTA               PantallaCatalogoTest
```

Rutas que entran o salen del catálogo, todas en `VinilosNavHostTest`:

```
Ruta                                                       Disparador
---------------------------------------------------------  ----------------------------------
Ingreso → Inicio → Catálogo (Visitante)                     "Ver álbumes"
Ingreso → Coleccionistas → Inicio → Catálogo (Coleccionista) "Ver álbumes"
Catálogo → Inicio                                           Flecha del encabezado
Catálogo → Inicio                                           "Atrás" del sistema
Catálogo → Detalle del álbum (id correcto)                  Tocar una tarjeta
Detalle → Catálogo (sin nueva consulta)                     Flecha del encabezado
Detalle → Catálogo (sin nueva consulta)                     "Atrás" del sistema
Detalle 404 → Catálogo                                      "Volver al catálogo"
```

**OBJ03, E2E (`CatalogoE2ETest`, BackVynils real).** Superan el mínimo de 2 escenarios y se ejecutan en el Motorola Edge 30:

```
Id          Escenario                                                     Backend
----------  ------------------------------------------------------------  ---------------
FN01-ESC01  Visitante y Coleccionista ven los 4 álbumes en orden con       Real
            portada, nombre y género
FN01-ESC02  Recorrer hasta el final y deslizar para actualizar sin cierre  Real
FN01-ESC04  Rotar conserva la posición sin recargar                        Real
FN01-ESC05  Tocar "Buscando América" abre su detalle y "Atrás" vuelve      Real
FN01-ESC03  Backend caído → error → "Reintentar" → lista                   MockWebServer
```

Los IDs `FN01-ESCxx` son los mismos que usan las E2E de la HU00, la HU02 y la HU03 y equivalen a `E2E-01-xx` del inventario de la wiki (FN01-ESC01 = E2E-01-01, y así sucesivamente). Cada escenario guarda su evidencia en su propia carpeta `e2e-evidencia/FN01_ESCxx/`, con sus capturas (la primera en el inicio), su `informe.md` y su PDF (tarea 6.4).

FN01-ESC03 se mantiene en la misma clase, como prevé la wiki, pero no cuenta para OBJ03 porque no usa el backend real; el backend caído real se verifica en la prueba manual M-01-07. Las capturas usan `EvidenciaE2E` con la corrección para API < 26 y esperan a que la portada termine de cargar.

### D9. Compatibilidad y publicación de resultados (OBJ03 y OBJ04)
**Entornos (OBJ04).** La suite instrumentada completa y la verificación manual del flujo (casos M-01) se ejecutan en:

```
Entorno                         Tipo         Android        Papel
------------------------------  -----------  -------------  ------------------------------------
Motorola Edge 30                Físico       14 (API 34)    E2E (OBJ03), suite y manual
Nexus 5X API 23                 Emulador     6.0 (API 23)   Versión mínima: suite y manual
Pixel 8 API 34                  Emulador     14 (API 34)    Suite (entorno de desarrollo)
```

Durante cada ejecución se revisa `adb logcat -b crash` para confirmar 0 cierres inesperados, además del resultado de las pruebas.

**Publicación (OBJ03 y OBJ04).** `e2e-evidencia/` sigue en `.gitignore` porque Gradle la regenera; lo que se publica se copia a una carpeta versionada:

```
docs/pruebas/sprint-1/HU01/
  README.md        Matriz de trazabilidad (OBJ01), estados y rutas (OBJ02), resumen
  resultados.md    Por entorno: unitarias, instrumentadas (totales, fallos), E2E, manual,
                   cierres inesperados; fecha, commit y comando
  manual.md        Casos M-01 con resultado por entorno
  e2e/             informe.md de la ejecución en el dispositivo físico (las capturas se
                   conservan en el equipo de ejecución, no se versionan)
```

OBJ05 (reporte de resultados y gestión de defectos como issues) queda fuera de este cambio por ahora. Si una prueba falla durante la implementación, se corrige dentro de la misma tarea, como en cualquier ciclo TDD.

### D10. Misma estética que el resto de la app
El catálogo debe verse como parte de la app actual (ingreso, inicio, artistas y detalle), no como una copia literal del HTML de Stitch:
- **Colores:** solo `MaterialTheme.colorScheme` (`primary`, `background`, `surface`, `onSurfaceVariant`, etc.) y las constantes ya definidas en `ui/theme/Color.kt`. No se agregan colores sueltos en la pantalla.
- **Tipografía:** `MaterialTheme.typography` del tema; no se agregan fuentes.
- **Componentes:** el encabezado con flecha de atrás, los chips (`ChipEtiqueta`), las tarjetas, el esqueleto de carga y los estados de error y vacío siguen la forma, los radios, las sombras y los espaciados de `PantallaArtistas` y del detalle del álbum.
- **Transiciones:** se usan las del `NavHost` actual (las predeterminadas de Navigation Compose); el catálogo no define animaciones propias de entrada o salida.
- **Verificación:** vistas previas de cada estado comparadas con los mockups y con las pantallas de artistas y detalle, y capturas de las E2E.

## Risks / Trade-offs

- [API experimental de pull-to-refresh en M3 1.2.1; cambia en 1.3] → Queda aislada en un composable privado de la pantalla; si se sube el BOM, solo se cambia ahí.
- [Conflictos con el PR #71: la rama incluye sus commits y toca `AppContainer`, `MainActivity` y `VinilosNavHost`] → Avisar a Lorena; cuando el #71 se integre con squash, rebasar con `git rebase --onto origin/develop 70c48cd`.
- [Superposición con el issue #17 (T02-4) de Lorena] → Acordar que la HU01 conecta el toque del catálogo y que el #17 se cierra o se reduce.
- [Las capturas E2E salen con la portada de respaldo si Coil no terminó de cargar] → Esperar a que la portada se pinte antes de capturar, o documentarlo en el informe.
- [`EvidenciaE2E` falla en API 23 (captura que requiere API 26)] → Incluir la corrección con `UiAutomation.takeScreenshot()` ya propuesta en el PR #71, si Lorena no la integra antes.
- [El orden alfabético difiere del mockup (que no lo respeta)] → Prevalece la HU; se documenta en la wiki (T01-6).
- [OBJ04 en API 23 solo se cubre con emulador; no hay un teléfono físico con API 23] → El objetivo pide dos entornos, y uno con API 23, no que sea físico; se registra así en `resultados.md`.
- [`connectedDebugAndroidTest` desinstala la app y borra la evidencia del dispositivo] → Las E2E se ejecutan con `am instrument` y la evidencia se descarga con `adb pull` antes de otra ejecución.
- [Publicar capturas agranda el repositorio] → Solo se versionan las capturas de la ejecución final en el dispositivo físico.

## Migration Plan

No hay datos ni backend que migrar. Al integrar, `catalogo` deja de ser provisional. Si hay que revertir, basta con revertir el PR.

## Open Questions

Ninguna que cambie el alcance. La redacción final de las capturas de los mockups para la wiki (T01-6) se resuelve al documentar.
