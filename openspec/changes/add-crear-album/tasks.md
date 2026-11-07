# Tasks

Reglas para todos los grupos:

- **TDD:** cada prueba unitaria se escribe primero y se ve fallar antes de implementar.
- **Alcance de pruebas:** solo unitarias. No se agregan E2E, regresión ni instrumentadas (T07-5 y T07-6 quedan diferidas), pero las instrumentadas existentes deben seguir compilando.
- **Estética (D8):** colores y tipografía de `MaterialTheme` y componentes de las HU ya implementadas. De `Downloads/HU07` solo se toma la estructura.
- **Textos (D9):** en `strings.xml`, literales de los criterios.
- **Wiki:** no se modifica hasta que se indique.
- **Rama:** `feature/hu07-crear-album`, basada en `origin/develop`.
- **Parte 1 sin mejoras:** este cambio no aplica M1 a M4 de `design.md`: nada de despachadores, Room, R8 ni ajustes de Coil, y se reutilizan los patrones actuales. Debe quedar como línea base medible, preparado según D11. Sin avisos de lint nuevos.

## 1. Consumo de `POST /albums` (T07-2, #36)

- [x] 1.1 Escribir `FechaLanzamientoTest`: `fechaIsoUtc` y `fechaVisible` del 26/09/1969 (milisegundos negativos) con la zona por defecto en Bogotá, UTC y Tokio; `hoyEnUtc` a las 8 p. m. en Bogotá devuelve el día local y no el día UTC; lo mismo en Tokio a la 1 a. m.
- [x] 1.2 Implementar `data/model/FechaLanzamiento.kt` con `SimpleDateFormat` en UTC (D2).
- [x] 1.3 Escribir en `AlbumRepositoryTest` (MockWebServer) las pruebas de `crearAlbum`: `POST /albums`; cuerpo con exactamente los 6 campos; devuelve el `id`; 400, 500, desconexión y respuesta sin `id` llegan como `ErrorDeCarga`; la cancelación se relanza.
- [x] 1.4 Implementar `AlbumNuevo`, `CrearAlbumDto`, `AlbumApi.crearAlbum`, `AlbumRepository.crearAlbum` y `OpcionesAlbum.kt` (D1).

## 2. Lógica del formulario (T07-3, #37)

- [x] 2.1 Escribir `ValidacionAlbumTest`: todo vacío marca 6 errores; solo espacios cuenta como vacío; `ftp://` marca `UrlInvalida`; `http://`, `https://` y `HTTPS://` son válidos; portada vacía marca `Obligatorio` y no `UrlInvalida`; formulario completo no marca errores.
- [x] 2.2 Implementar `CrearAlbumUiState`, `EnvioAlbum` y `validarAlbum` (D3, D4).
- [x] 2.3 Escribir `CrearAlbumViewModelTest`: con errores no llama al repositorio y aumenta `intentosFallidos`; editar borra solo el error de ese campo; la descripción se limita a 500; `Editando → Enviando → Creado(id)`; dos llamadas a `crear()` hacen una sola petición; envía textos recortados y la fecha elegida; `Enviando → Fallido` conserva los datos; reintentar desde `Fallido` envía de nuevo; mientras envía o tras crear no cambian los datos.
- [x] 2.4 Implementar `CrearAlbumViewModel`.

## 3. Pantalla del formulario (T07-3, #37)

- [x] 3.1 Agregar `error` y `errorContainer` al tema, los vectores `ic_crear_*` y los textos.
- [x] 3.2 Extraer `EncabezadoConRol` del catálogo sin cambiar su aspecto.
- [x] 3.3 Implementar `ui/crear/PantallaCrearAlbum.kt` sin estado, con `Tag*`, `Column` con `verticalScroll`, campos, listas, aviso superior, error de envío y botones (D8). Incluir `@Preview` de vacío, errores, enviando y fallido.
- [x] 3.4 Implementar el selector con `DatePicker(showModeToggle = false)`, `SelectableDates` y `rememberSaveable` para el diálogo (D2, D8).
- [x] 3.5 Agregar `android:windowSoftInputMode="adjustResize"` a `MainActivity`.

## 4. Catálogo (T07-4, #38)

- [x] 4.1 Escribir en `AlbumsViewModelTest` las pruebas de `albumCreado`: desde `Exito` y `Vacio` actualiza y conserva la lista; desde `Error` y `Cargando` vuelve a cargar; activa el aviso y el desplazamiento una sola vez; el aviso se apaga con `avisoCreadoMostrado` y el desplazamiento con `desplazadoAlNuevo`; la marca sigue tras `actualizar()`; un segundo álbum reemplaza la marca; si la consulta falla sigue el aviso de creado y el de actualización.
- [x] 4.2 Implementar `AlbumNuevoCatalogo` y `AlbumsViewModel.albumCreado` (D5).
- [x] 4.3 Agregar a `PantallaCatalogo`, con parámetros por defecto para no tocar las instrumentadas: botón "Crear álbum" (D10) según `session.collectorActionsEnabled`, aviso "Álbum creado", insignia "NUEVO", desplazamiento y margen inferior de la cuadrícula y del aviso (D6, D8).
- [x] 4.4 Actualizar el comentario de `PantallaCatalogo` que dice que es igual para ambos roles.

## 5. Navegación (T07-4, #38)

- [x] 5.1 Agregar `Rutas.CrearAlbum`, el destino y su `viewModelFactory`.
- [x] 5.2 Conectar el resultado con `savedStateHandle` hacia `albumCreado` y borrar la clave (D5).
- [x] 5.3 Agregar el `popBackStack` protegido, `BackHandler` durante el envío y el regreso si no hay sesión de coleccionista (D7).

## 6. Verificación

- [x] 6.1 Ejecutar `lint testDebugUnitTest --continue`: 0 errores de lint y todas las unitarias pasan.
- [x] 6.2 Ejecutar `compileDebugAndroidTestKotlin` para confirmar que las instrumentadas compilan.
- [x] 6.3 Verificar en dispositivo, contra un backend local y **nunca contra el backend de los tutores**, los 10 criterios, el límite de fechas, la insignia, el teclado sobre los campos y el idioma del calendario. Borrar después los álbumes creados con `DELETE /albums/{id}`. Hecho con un recorrido automatizado en un Motorola Edge 30, `Pixel_8_API_34` y `Nexus_5X_API_23`; resultados en `docs/pruebas/sprint-2/HU07/resultados.md`.
- [x] 6.4 Ejecutar `openspec validate add-crear-album --strict`.

## 7. Pendiente, fuera de este cambio

- [ ] 7.1 T07-5 (#39): pruebas E2E. Diferida.
- [ ] 7.2 T07-6 (#40): regresión. Diferida.
- [ ] 7.3 T07-7 (#41): actualizar wiki, criterios y mockups cuando se indique, y resolver T07-1 (#5).
- [ ] 7.4 Al cerrar la parte 1, repetir la medición para fijar la línea base (D11).
- [ ] 7.5 Segunda parte: crear los cambios `mejorar-desempeno` (M1 a M3) y `add-cache-catalogo` (M4) a partir de la sección "Mejoras futuras" de `design.md`, y comparar con la línea base.
