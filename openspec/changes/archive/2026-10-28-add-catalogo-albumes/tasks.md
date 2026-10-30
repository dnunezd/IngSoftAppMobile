# Tasks

Reglas para todos los grupos:

- **TDD en tres etapas (OBJ01):** ROJO (la prueba se escribe primero y se ve fallar), VERDE (el código mínimo para que pase) y AZUL (refactor sin cambiar el comportamiento: nombres, duplicación, extracción de componentes, con las pruebas otra vez en verde). El informe de cada grupo registra las tres ejecuciones con su comando.
- **Estética (D10):** la pantalla usa los colores de `MaterialTheme.colorScheme`, la tipografía de `MaterialTheme.typography`, los componentes compartidos y las transiciones de navegación que ya tiene la app; no se agregan colores, fuentes ni animaciones de transición propias.
- **Wiki:** no se modifica en este cambio hasta que el usuario lo indique.

## 1. Inyección del repositorio de álbumes

- [x] 1.1 Escribir primero en `AppContainerTest` la prueba de que `AlbumRepository` se puede reemplazar (`albumesRepositorio`) y verla fallar; luego agregar el parámetro en `AppContainer`. Verificar: `testDebugUnitTest --tests "*AppContainerTest"` pasa.
- [x] 1.2 Pasar `container.albumRepository` desde `MainActivity` a `VinilosNavHost` y quitar el valor por defecto (y los imports de `LocalContext` y `VinilosApplication`); en las pruebas que construyen el NavHost, pasar `RepositorioAlbumesFalso`. Verificar: `assembleDebug assembleDebugAndroidTest` compila y `VinilosNavHostTest` sigue pasando.

## 2. Consumo de `GET /albums` (T01-2, #10)

- [x] 2.1 Escribir primero en `AlbumRepositoryTest` (MockWebServer) las pruebas de `obtenerAlbumes()`: llama a `GET /albums`; convierte nombre, portada, género, artista(s) y año; descarta los álbumes sin `id`, `name` o `genre` sin afectar a los demás; sin `performers`, con lista vacía o con artistas sin nombre, `artista` es `""` (nunca `null`) y los artistas con nombre se conservan; sin `releaseDate` o con una fecha inválida, `anio` es `null`; lista vacía → éxito vacío; 500, desconexión y JSON inválido → `ErrorDeCarga`; la cancelación se relanza. Verlas fallar. (Criterios 2, 4, 5 y 8.)
- [x] 2.2 Escribir primero `OrdenAlbumesTest`: los 4 álbumes de ejemplo quedan "A Day at the Races", "A Night at the Opera", "Buscando América", "Poeta del pueblo"; ignora mayúsculas y tildes; desempata por `id`. Verla fallar. (Criterio 8.)
- [x] 2.3 Implementar `AlbumResumen`, `AlbumApi.obtenerAlbumes()` y `open suspend fun obtenerAlbumes()` en `AlbumRepository`, con conversión tolerante y orden con `Collator` (D1, D2), sin cambiar `obtenerAlbum(id)`. Ampliar `RepositorioAlbumesFalso` con `obtenerAlbumes()`, una cola de resultados y `consultasCatalogo`. Verificar: las pruebas de 2.1 y 2.2 y las de la HU02 en `AlbumRepositoryTest` pasan.

## 3. Estado del catálogo (T01-3, #11)

- [x] 3.1 Escribir primero `AlbumsViewModelTest` (con `MainDispatcherRule` y repositorio falso): `Cargando → Exito`, `Cargando → Vacio`, `Cargando → Error`; `reintentar()` desde `Error` pasa por `Cargando` y llega a `Exito`, y no hace nada en otros estados; `actualizar()` desde `Exito` mantiene la lista con `actualizando = true` y publica la nueva; `actualizar()` desde `Vacio` puede pasar a `Exito`; si falla, conserva la lista con `errorAlActualizar = true` y `avisoMostrado()` lo apaga; una respuesta tardía no reemplaza la consulta vigente; una excepción que no es `ErrorDeCarga` se relanza; la consulta se hace una sola vez al crearse. Verla fallar. (Criterios 3, 4, 5, 9 y 10.)
- [x] 3.2 Implementar `AlbumsUiState` (`sealed interface`) y `AlbumsViewModel` con las convenciones de `AlbumViewModel` (`consultaVigente`, relanzar lo inesperado, reintento solo desde `Error`) (D3). Verificar: `testDebugUnitTest --tests "*AlbumsViewModelTest"` pasa.

## 4. Pantalla del catálogo (T01-3, #11)

- [x] 4.1 Copiar como vectores `ic_cat_graphic_eq`, `ic_cat_music_note`, `ic_cat_album`, `ic_cat_music_off` e `ic_cat_priority_high` desde Material Symbols Outlined (D7) y agregar a `strings.xml` los textos de la HU ("No pudimos cargar el catálogo", "Reintentar", "Aún no hay álbumes en el catálogo") y del mockup ("Álbumes", "VISITANTE", "COLECCIONISTA", "VINILO CATALOG", "Explora", "álbumes", el subtítulo, "Catálogo", el contador plural de lanzamientos y "Cargando catálogo de vinilos..."). Verificar: `assembleDebug` y `lint` sin errores.
- [x] 4.2 Agregar a `PortadaAlbum` los parámetros `contentDescription` y `contentScale`, con los valores actuales por defecto. Verificar: `PortadaAlbumTest` y `ContenidoAlbumTest` siguen pasando.
- [x] 4.3 Escribir primero `PantallaCatalogoTest` (Compose Test, sin backend) con un caso por cada estado visible de la tabla OBJ02 del diseño: cargando; éxito con los 4 álbumes en orden con portada, nombre y género; con y sin artista y año (con `artista = ""` no se muestra la línea del artista ni el texto "null"); contador; portada vacía o inválida → imagen genérica; vacío; error con "No pudimos cargar el catálogo" y "Reintentar" que llama al callback; actualizando; aviso al fallar la actualización (y que llama a `onAvisoMostrado`); etiqueta "VISITANTE"/"COLECCIONISTA"; además, tocar un álbum entrega su id y la flecha llama a `onAtras`. Verla fallar. (Criterios 1 a 6 y 8; OBJ02.)
- [x] 4.4 Implementar `PantallaCatalogo` (D4, D5) con cuadrícula adaptable, encabezado, tarjetas, estados, `Snackbar`, deslizar para actualizar, constantes `Tag*` y vistas previas por estado. Verificar: `PantallaCatalogoTest` pasa y las vistas previas coinciden con los mockups de Stitch.
- [x] 4.5 Escribir primero `CatalogoListaLargaTest` (50 álbumes generados: llegar al último con `performScrollToNode`) y `CatalogoActualizarTest` (el falso devuelve un álbum más en la segunda llamada; con `swipeDown` aparece en su posición alfabética), verlas fallar y hacerlas pasar. Verificar: pasan en el Pixel 8 API 34 y en el Nexus 5X API 23. (Criterios 7 y 10.)

## 5. Integración y navegación (T01-4, #12)

- [x] 5.1 Escribir primero en `VinilosNavHostTest` una prueba por cada ruta de la tabla OBJ02 del diseño: Ingreso → Inicio → Catálogo (Visitante); Ingreso → Coleccionistas → Inicio → Catálogo (Coleccionista); Catálogo → Inicio con la flecha y con "Atrás" del sistema, sin cambiar la sesión; Catálogo → Detalle con el id tocado; Detalle → Catálogo con la flecha y con "Atrás", sin nueva consulta (`consultasCatalogo == 1`); Detalle 404 → "Volver al catálogo". Ampliar `RotacionTest` (50 álbumes, desplazarse, recrear la Activity: misma posición y una sola consulta) y `RecuperacionTest` (error → "Reintentar" → lista). Verlas fallar. (Criterios 1, 4 y 9; OBJ02.)
- [x] 5.2 Conectar el destino `catalogo` con `AlbumsViewModel` (vía `viewModelFactory`) y `PantallaCatalogo`, leyendo la sesión de `SessionViewModel` (D3, D6); eliminar `PantallaCatalogoProvisional`, `AlbumIdProvisional`, sus textos y `PantallaCatalogoProvisionalTest`; ajustar `AlbumDetalleE2ETest` para llegar al detalle tocando el álbum. Verificar: `lint testDebugUnitTest` y todas las instrumentadas sin backend pasan.
- [x] 5.3 Crear `docs/pruebas/sprint-1/HU01/README.md` con la matriz de trazabilidad (OBJ01) y las tablas de estados y rutas (OBJ02), cada fila con el nombre real de la prueba. Verificar: los 10 criterios, los 9 estados y las 8 rutas tienen al menos una prueba que existe y pasa.

## 6. Pruebas E2E contra BackVynils (T01-5, #13)

- [x] 6.1 Aplicar en `EvidenciaE2E` la captura compatible con API < 26 (`UiAutomation.takeScreenshot()` y `catch (e: Throwable)`) si no llegó antes desde la HU02, y esperar a que la portada termine de cargar antes de capturar. Verificar: genera capturas en API 23 y en API 34.
- [x] 6.2 Implementar `CatalogoE2ETest` con E2E-01-01, 01-02, 01-04 y 01-05 contra BackVynils real y E2E-01-03 con MockWebServer (D8), con capturas de `EvidenciaE2E`. Verificar: pasan en el Motorola Edge 30 (`adb reverse tcp:3000 tcp:3000`, `-PbaseUrl=http://localhost:3000/`).
- [x] 6.3 Ejecutar las E2E en el Motorola Edge 30 con `am instrument`, descargar la evidencia con `adb pull` y copiar el `informe.md` a `docs/pruebas/sprint-1/HU01/e2e/` (las capturas se conservan en el equipo de ejecución, no en el repositorio). Verificar: hay una captura por escenario y el informe indica 5/5 superadas (OBJ03: 4 contra el backend real en dispositivo físico).

## 7. Compatibilidad y resultados por entorno (OBJ04)

- [x] 7.1 Ejecutar `lint testDebugUnitTest` y `connectedDebugAndroidTest` en el Pixel 8 API 34, el Nexus 5X API 23 y el Motorola Edge 30, revisando `adb logcat -b crash` en cada uno. Verificar: 0 fallos y 0 cierres inesperados por entorno; registrar en `docs/pruebas/sprint-1/HU01/resultados.md` totales, fallos, fecha, commit, dispositivo y comando.
- [x] 7.2 Ejecutar la verificación manual del flujo (casos M-01 ajustados) en el Motorola Edge 30 y en el Nexus 5X API 23, incluido M-01-07 con el backend detenido de verdad. Verificar: resultado por caso y por entorno en `docs/pruebas/sprint-1/HU01/manual.md`, con 0 cierres inesperados.
## 8. Documentación (T01-6, #14)

- [ ] 8.1 (En espera: no modificar la wiki hasta que el usuario lo indique.) Preparar para la wiki de la HU01: criterio 1 con el flujo Ingreso → (selector) → Inicio → "Ver álbumes" → Catálogo; textos de la HU frente a los del mockup; elementos del mockup fuera de alcance; orden alfabético frente al orden del mockup; enlace a `docs/pruebas/sprint-1/HU01/`. Verificar: borrador revisado por el usuario antes de publicarlo.
- [ ] 8.2 (En espera: no modificar la wiki hasta que el usuario lo indique.) Preparar la actualización de los casos manuales M-01 (M-01-02, 03, 04, 05 y 06 remiten a las pruebas automáticas; M-01-08 indica que el Coleccionista ve lo mismo) y del inventario de pruebas de `Sprint-1.md` (E2E-01-05 nueva; E2E-01-03 con MockWebServer). Verificar: borrador revisado por el usuario antes de publicarlo.
- [x] 8.3 Actualizar `README.md` y `CLAUDE.md` donde dicen que el catálogo es provisional, y mencionar `docs/pruebas/`. Verificar: `grep -i provisional` no devuelve referencias al catálogo.

## Workflow follow-up

- Abrir el PR `feat(catalogo): consultar catálogo de álbumes` hacia `develop` con `Closes #10`, `#11`, `#12`, `#13` y `#14` y el enlace a `docs/pruebas/sprint-1/HU01/`, después de integrar o rebasar el PR #71.
- Avisar a Lorena Nossa del cambio en `AppContainer` y de la navegación catálogo → detalle (issue #17).
- Cambio archivado el 2026-10-28 en la misma rama del PR, a pedido del usuario. Las tareas 8.1 y 8.2 (wiki, issue #14) quedan pendientes y se harán fuera del cambio cuando el usuario lo indique.
