# HU07: Crear un álbum (parte 1)

Evidencia de la primera parte de la historia. La especificación está en `openspec/changes/add-crear-album/` y los resultados por dispositivo en [`resultados.md`](resultados.md).

Esta parte implementa la funcionalidad **sin mejoras de desempeño**, para que sirva de línea base. La segunda parte las aplica y las compara con esta base (`design.md`, sección "Mejoras futuras").

## Matriz de trazabilidad

| # | Criterio de aceptación | Unitarias | Verificación en dispositivo |
|---|---|---|---|
| 1 | El Coleccionista abre el formulario; el Visitante no ve la opción | `SessionViewModelTest` | Botón visible y oculto según el rol |
| 2 | Pide nombre, portada, fecha, descripción, género y disquera | `AlbumRepositoryTest`, `CrearAlbumViewModelTest` | Campos y opciones del backend |
| 3 | Guarda, muestra "Álbum creado" y regresa | `CrearAlbumViewModelTest`, `AlbumsViewModelTest` | Regreso, aviso, insignia y datos en el backend |
| 4 | Marca cada campo faltante con "Campo obligatorio" | `ValidacionAlbumTest`, `CrearAlbumViewModelTest` | Errores y sin envío al servidor |
| 5 | "Cancelar" cierra sin guardar ni enviar, salvo durante el envío | No aplica (navegación) | Regreso, sin envío, formulario vacío al reabrir |
| 6 | Selector de fecha sin escribir y sin errores de un día | `FechaLanzamientoTest`, `AlbumRepositoryTest` | Días futuros bloqueados y fecha guardada igual |
| 7 | Desactiva el botón y muestra "Creando..." | `CrearAlbumViewModelTest` | Tres toques crean un solo álbum |
| 8 | Error conservando los datos y con reintento | `CrearAlbumViewModelTest`, `AlbumRepositoryTest` | Backend detenido, mensaje y reintento exitoso |
| 9 | "Ingresa una URL válida" sin enviar nada, también sin dominio | `ValidacionAlbumTest`, `CrearAlbumViewModelTest` | Mensaje y sin envío al servidor |
| 10 | Al girar conserva lo escrito o elegido | Estado en `CrearAlbumViewModel` | Formulario, selector abierto y envío en curso |

Las pruebas E2E (T07-5, #39) están en la sección siguiente. Las de regresión (T07-6, #40) no se harán para esta historia.

## Pruebas E2E (T07-5)

Las pruebas están en [`CrearAlbumE2ETest`](../../../../app/src/androidTest/java/com/example/vinilos/CrearAlbumE2ETest.kt) y la especificación del cambio en `openspec/changes/add-e2e-crear-album/`. Cada escenario genera un PDF con una captura por paso, guardado en [`e2e/`](e2e/).

Como crean álbumes, se corren contra **BackVynils local** (`-PbaseUrl=http://10.0.2.2:3000/` en el emulador) y borran con `DELETE /albums/{id}` todo álbum que hayan creado, aunque fallen. El coleccionista se elige de los datos reales.

| ID | Descripción del escenario | Resultado esperado | Backend | Evidencia |
|---|---|---|---|---|
| FN07-ESC01 | **El Visitante no ve la opción de crear.** Entra como Visitante y abre "Ver álbumes"; vuelve al ingreso, entra como Coleccionista y abre el catálogo. | Como Visitante no aparece "Crear álbum"; como Coleccionista sí. | Real | [PDF](e2e/FN07_ESC01.pdf) |
| FN07-ESC02 | **Crear un álbum válido.** Como Coleccionista abre el formulario, llena los seis campos (fecha: el día 1 del mes actual en el selector) y pulsa "Crear álbum". | Regresa al catálogo con el aviso "Álbum creado" y el álbum marcado como NUEVO. El backend lo guarda una sola vez, con la fecha `yyyy-MM-01T00:00:00.000Z` del día elegido. | Real | [PDF](e2e/FN07_ESC02.pdf) |
| FN07-ESC03 | **Cancelar un formulario con datos.** Llena el formulario y pulsa "Cancelar". | Vuelve al catálogo sin el álbum; no se envía nada al backend. | Real | [PDF](e2e/FN07_ESC03.pdf) |
| FN07-ESC04 | **Campos vacíos.** Pulsa "Crear álbum" con el formulario vacío. | Aparece "Atención requerida" y cada uno de los seis campos dice "Campo obligatorio"; no se envía nada. | Real | [PDF](e2e/FN07_ESC04.pdf) |
| FN07-ESC05 | **URL inválida.** Llena el formulario con la portada `ftp://...` y pulsa "Crear álbum". | La portada dice "Ingresa una URL válida", los demás campos no se marcan y no se envía nada. | Real | [PDF](e2e/FN07_ESC05.pdf) |
| FN07-ESC06 | **Girar con el formulario lleno.** Llena el formulario y gira el teléfono a horizontal. | Se conservan el nombre, la portada, la fecha, el género, la disquera y la descripción. | Real | [PDF](e2e/FN07_ESC06.pdf) |
| FN07-ESC07 | **Backend caído y reintento.** Llena el formulario y pulsa "Crear álbum" con el backend caído; el backend vuelve y pulsa "Reintentar creación". | Aparece "No se pudo crear el álbum, intenta de nuevo" y se conservan los seis datos. Al reintentar se crea un solo álbum y se ve "Álbum creado" con la insignia NUEVO. | MockWebServer | [PDF](e2e/FN07_ESC07.pdf) |

| Criterio | Escenarios |
|---|---|
| 1. Solo el Coleccionista abre el formulario | ESC01 |
| 2. Pide los seis campos | ESC02, ESC04 |
| 3. Guarda, "Álbum creado" y regresa | ESC02, ESC07 |
| 4. "Campo obligatorio" por campo, sin guardar | ESC04 |
| 5. "Cancelar" cierra sin guardar ni enviar | ESC03 |
| 6. Selector de fecha, sin errores de un día | ESC02 (fecha guardada) y ESC06 (fecha conservada) |
| 7. "Creando..." evita álbumes repetidos | ESC02 y ESC07 comprueban un solo álbum por creación; el estado "Creando..." lo cubre `CrearAlbumViewModelTest`, porque contra el backend local la respuesta llega antes de poder capturarlo |
| 8. Error conservando los datos y con reintento | ESC07 |
| 9. "Ingresa una URL válida" sin enviar | ESC05 |
| 10. Al girar se conserva todo | ESC06 |

Resultados en el emulador Pixel 8 (API 34), 2026-11-07, contra BackVynils local: `CrearAlbumE2ETest` `OK (7 tests)` con 27 capturas en "Ok"; suite instrumentada completa `OK (138 tests)`; 220 unitarias sin fallos y 0 errores de lint. Tras cada corrida el backend quedó con sus cuatro álbumes de ejemplo.

En la revisión del PR se corrió la clase 3 veces por dispositivo en un Motorola Edge 30 (API 34), el emulador Pixel 8 (API 34) y un emulador Nexus 5X (API 23), cada uno con su BackVynils local. En API 23, ESC05 y ESC07 fallaban de forma intermitente porque el teclado abierto tapaba el siguiente campo o botón y el toque se perdía; con el cierre del teclado después de escribir en cada campo, la clase pasó 9/9 (7/7 en cada corrida) y el backend quedó con sus álbumes de ejemplo.

```bash
./gradlew installDebug installDebugAndroidTest -PbaseUrl=http://10.0.2.2:3000/
adb shell rm -rf /sdcard/Android/data/com.example.vinilos/files/e2e-evidencia
adb shell am instrument -w -e class com.example.vinilos.CrearAlbumE2ETest com.example.vinilos.test/androidx.test.runner.AndroidJUnitRunner
adb pull /sdcard/Android/data/com.example.vinilos/files/e2e-evidencia ./e2e-evidencia
```

## Capturas

| Catálogo como Visitante | Catálogo como Coleccionista | Campos obligatorios | URL inválida |
|---|---|---|---|
| ![Visitante](capturas/01_catalogo_visitante.jpg) | ![Coleccionista](capturas/02_catalogo_coleccionista.jpg) | ![Errores](capturas/03_campos_obligatorios.jpg) | ![URL](capturas/04_url_invalida.jpg) |

| Selector de fecha | Formulario lleno | Creando | Error y reintento |
|---|---|---|---|
| ![Selector](capturas/05_selector_fecha.jpg) | ![Lleno](capturas/06_formulario_lleno.jpg) | ![Creando](capturas/07_creando.jpg) | ![Error](capturas/08_error_reintentar.jpg) |

| Álbum creado | Insignia NUEVO | Teclado abierto |
|---|---|---|
| ![Creado](capturas/09_album_creado.jpg) | ![Nuevo](capturas/10_insignia_nuevo.jpg) | ![Teclado](capturas/11_teclado.jpg) |

| Selector en horizontal (API 23) | Formulario en horizontal |
|---|---|
| ![Selector horizontal](capturas/12_selector_horizontal_api23.jpg) | ![Formulario horizontal](capturas/13_formulario_horizontal.jpg) |
