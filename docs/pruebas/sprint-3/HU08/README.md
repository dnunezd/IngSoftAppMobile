# HU08 · Asociar canciones a un álbum: pruebas y evidencia

## Pruebas E2E (T08-5)

Clase `AgregarCancionE2ETest` (cambio de OpenSpec [`add-e2e-agregar-tracks`](../../../../openspec/changes/add-e2e-agregar-tracks)). Recorre la app desde el ingreso, como la persona, y cubre todos los casos que pide la [T08-5](https://github.com/dnunezd/IngSoftAppMobile/wiki/HU08). ESC01, ESC02 y ESC04 a ESC07 van contra BackVynils; como agregan canciones, se corren contra el backend **local**. Los del Coleccionista usan un álbum propio (`POST /albums`) que se borra al terminar con sus canciones; ESC07 lo borra a mitad de la prueba para guardar sobre un álbum ya eliminado. ESC03 usa `MockWebServer` para provocar el error con el backend caído.

| ID         | Descripción del escenario                                                                                      | Resultado esperado                                                                                                                                                                                                                       | Backend          | Evidencia                 |
|------------|----------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------|---------------------------|
| FN08-ESC01 | El Coleccionista escribe duraciones con formato inválido ("3:75" y "345") y luego una válida                   | "Usa el formato m:ss, por ejemplo 5:05" con "Guardar" deshabilitado; con "3:45" el error desaparece y "Guardar" se habilita; no se envía nada                                                                                             | BackVynils local | [PDF](e2e/FN08_ESC01.pdf) |
| FN08-ESC02 | El Coleccionista agrega la canción "Pista E2E" de 3:45 a un álbum sin canciones                                | El formulario abre con "Campo obligatorio" en ambos campos y "Guardar" deshabilitado; al llenarlo se habilita; al guardar aparece "Canción agregada" y el detalle muestra la canción con su duración; el backend la guarda                | BackVynils local | [PDF](e2e/FN08_ESC02.pdf) |
| FN08-ESC03 | Guarda una canción y el guardado tarda y falla                                                                 | Mientras guarda, "Guardando…" deshabilitado y un segundo toque no envía nada; luego "No se pudo agregar la canción, intenta de nuevo" con el nombre y la duración conservados; al reintentar se agrega                                  | `MockWebServer`  | [PDF](e2e/FN08_ESC03.pdf) |
| FN08-ESC04 | El Visitante abre el detalle de un álbum                                                                       | La sección de canciones no muestra "Agregar canción" y no se envía nada                                                                                                                                                                  | BackVynils local | [PDF](e2e/FN08_ESC04.pdf) |
| FN08-ESC05 | El Coleccionista llena el formulario y pulsa "Cancelar"                                                        | Vuelve al detalle sin la canción y sin enviar nada; al abrir de nuevo, los campos están vacíos con "Campo obligatorio" y "Guardar" deshabilitado                                                                                          | BackVynils local | [PDF](e2e/FN08_ESC05.pdf) |
| FN08-ESC06 | Escribe el nombre y una duración inválida, gira la pantalla, corrige la duración, vuelve a girar y guarda      | En horizontal se conservan el nombre, la duración y el error de formato, con "Guardar" deshabilitado; al volver a vertical se conserva lo corregido; al guardar se agrega una sola vez y el detalle la muestra                           | BackVynils local | [PDF](e2e/FN08_ESC06.pdf) |
| FN08-ESC07 | Con el formulario lleno, el álbum se elimina en el backend y el Coleccionista guarda                           | "El álbum ya no está disponible" con "Volver", sin "Canción agregada"; al volver, el detalle se actualiza y también muestra "El álbum ya no está disponible" con "Volver al catálogo"                                                     | BackVynils local | [PDF](e2e/FN08_ESC07.pdf) |

Casos de la T08-5 y escenario que cubre cada uno:

| #   | Caso de la T08-5                                                          | Escenarios          |
|-----|---------------------------------------------------------------------------|---------------------|
| 1   | El Visitante no ve la opción                                              | ESC04               |
| 2   | Agregar una canción a un álbum y verla en su detalle                      | ESC02, ESC06        |
| 3   | Con una duración inválida o un campo vacío no se puede guardar            | ESC01, ESC02, ESC05 |
| 4   | Cancelar con datos escritos                                               | ESC05               |
| 5   | Rotar la pantalla                                                         | ESC06               |
| 6   | Guardar sobre un álbum ya eliminado                                       | ESC07               |
| 7   | Con el backend caído, ver el error conservando los datos                  | ESC03               |
| 8   | Limpiar los datos creados al terminar                                     | Todos               |

Criterios de la HU08 cubiertos por las E2E:

| #   | Criterio                                                                   | Escenarios          |
|-----|----------------------------------------------------------------------------|---------------------|
| 1   | Solo el Coleccionista ve la acción para agregar una canción                | ESC02, ESC04        |
| 2   | El formulario abre vacío con "Campo obligatorio" y "Guardar" deshabilitado | ESC02, ESC05        |
| 3   | Formato de duración `m:ss`; si no se cumple, "Guardar" deshabilitado       | ESC01, ESC06        |
| 4   | Al guardar, "Canción agregada" y el detalle muestra la canción             | ESC02, ESC03, ESC06 |
| 5   | Mientras guarda, "Guardando…" deshabilitado y una sola petición          | ESC03               |
| 6   | "Cancelar" cierra sin guardar y el formulario vuelve vacío                 | ESC05               |
| 7   | Error al guardar: mensaje, valores conservados y reintento                 | ESC03               |
| 8   | Álbum que ya no existe: mensaje y regreso                                  | ESC07               |
| 9   | El formulario conserva lo escrito y los errores al girar                   | ESC06               |

Fuera de las E2E, que el Visitante no vea la acción y "Cancelar" sin enviar también se prueban en `VinilosNavHostTest`.

### Resultados

Samsung SM-S938B (Android 17), 2026-11-25, contra BackVynils local: `OK (13 tests)` junto con las E2E de HU09 y HU15, sin escenarios omitidos y con todas las capturas en "Ok"; el backend quedó con sus 4 álbumes de ejemplo y ningún álbum de prueba.

`AlbumesDePrueba` reintenta la creación del álbum de prueba cuando BackVynils responde 500 porque la secuencia de ids de `album` llegó a los ids 100 a 103 de los datos de ejemplo (lo que hizo fallar 4 de 27 corridas en un Pixel 8 durante la revisión del PR). Se comprobó llevando la secuencia a 99: la prueba pasó y su álbum quedó con el id 104.

### Cómo correrlas

```bash
./gradlew installDebug installDebugAndroidTest -PbaseUrl=http://localhost:3000/   # BackVynils local
adb reverse tcp:3000 tcp:3000                                                     # solo en un teléfono
adb shell rm -rf /sdcard/Android/data/com.example.vinilos/files/e2e-evidencia
adb shell am instrument -w -e class com.example.vinilos.AgregarCancionE2ETest com.example.vinilos.test/com.example.vinilos.VinilosTestRunner
adb pull /sdcard/Android/data/com.example.vinilos/files/e2e-evidencia ./e2e-evidencia
```
