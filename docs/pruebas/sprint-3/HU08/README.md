# HU08 · Asociar canciones a un álbum: pruebas y evidencia

## Pruebas E2E (T08-5)

Clase `AgregarCancionE2ETest` (cambio de OpenSpec [`add-e2e-agregar-tracks`](../../../../openspec/changes/add-e2e-agregar-tracks)). Recorre la app desde el ingreso, como la persona. ESC01 a ESC04 van contra BackVynils; como agregan canciones, se corren contra el backend **local**. Los escenarios del Coleccionista usan un álbum propio (`POST /albums`) que se borra al terminar con sus canciones. ESC05 y ESC06 usan `MockWebServer` para provocar los errores.

| ID         | Descripción del escenario                                                                     | Resultado esperado                                                                                                                                                                               | Backend          | Evidencia                 |
|------------|-----------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------|---------------------------|
| FN08-ESC01 | El Visitante abre el detalle de un álbum                                                      | La sección de canciones no muestra "Agregar canción" y no se envía nada                                                                                                                          | BackVynils local | [PDF](e2e/FN08_ESC01.pdf) |
| FN08-ESC02 | El Coleccionista agrega la canción "Pista E2E" de 3:45 a un álbum sin canciones               | El formulario abre con "Campo obligatorio" en ambos campos y "Guardar" deshabilitado; al llenarlo se habilita; al guardar aparece "Canción agregada" y el detalle muestra la canción con su duración; el backend la guarda | BackVynils local | [PDF](e2e/FN08_ESC02.pdf) |
| FN08-ESC03 | Escribe duraciones con formato inválido ("3:75" y "345") y luego una válida                   | "Usa el formato m:ss, por ejemplo 5:05" con "Guardar" deshabilitado; con "3:45" el error desaparece y "Guardar" se habilita; no se envía nada                                                     | BackVynils local | [PDF](e2e/FN08_ESC03.pdf) |
| FN08-ESC04 | Llena el formulario y pulsa "Cancelar"                                                        | Vuelve al detalle sin la canción y sin enviar nada; al abrir de nuevo, los campos están vacíos                                                                                                   | BackVynils local | [PDF](e2e/FN08_ESC04.pdf) |
| FN08-ESC05 | Guarda una canción y el guardado tarda y falla                                                | Mientras guarda, "Guardando..." deshabilitado y un segundo toque no envía nada; luego "No se pudo agregar la canción, intenta de nuevo" con el nombre y la duración conservados; al reintentar se agrega | `MockWebServer`  | [PDF](e2e/FN08_ESC05.pdf) |
| FN08-ESC06 | Guarda una canción y el álbum ya no existe (404)                                              | "El álbum ya no está disponible", sin "Canción agregada"; "Volver al álbum" regresa al detalle sin la canción                                                                                     | `MockWebServer`  | [PDF](e2e/FN08_ESC06.pdf) |

Criterios de la HU08 cubiertos por las E2E:

| #   | Criterio                                                                              | Escenarios   |
|-----|---------------------------------------------------------------------------------------|--------------|
| 1   | Solo el Coleccionista ve la acción para agregar una canción                           | ESC01, ESC02 |
| 2   | El formulario abre vacío con "Campo obligatorio" y "Guardar" deshabilitado            | ESC02, ESC04 |
| 3   | Formato de duración `m:ss`                                                            | ESC03        |
| 4   | Al guardar, "Canción agregada" y el detalle muestra la canción                        | ESC02, ESC05 |
| 5   | Mientras guarda, "Guardando..." deshabilitado y una sola petición                     | ESC05        |
| 6   | "Cancelar" cierra sin guardar y el formulario vuelve vacío                            | ESC04        |
| 7   | Error al guardar: mensaje, valores conservados y reintento                            | ESC05        |
| 8   | Álbum que ya no existe: mensaje y regreso                                             | ESC06        |

### Resultados

Samsung SM-S938B (Android 17), 2026-11-22, contra BackVynils local:

| Ejecución                                                                       | Resultado                                                                       |
|---------------------------------------------------------------------------------|---------------------------------------------------------------------------------|
| `AgregarCancionE2ETest` junto con las E2E de HU09 y HU15                        | `OK (16 tests)`, sin escenarios omitidos; todas las capturas en "Ok"            |
| Backend después de la corrida                                                   | Los 4 álbumes de ejemplo; ningún álbum de prueba                                |

### Cómo correrlas

```bash
./gradlew installDebug installDebugAndroidTest -PbaseUrl=http://localhost:3000/   # BackVynils local
adb reverse tcp:3000 tcp:3000                                                     # solo en un teléfono
adb shell rm -rf /sdcard/Android/data/com.example.vinilos/files/e2e-evidencia
adb shell am instrument -w -e class com.example.vinilos.AgregarCancionE2ETest com.example.vinilos.test/androidx.test.runner.AndroidJUnitRunner
adb pull /sdcard/Android/data/com.example.vinilos/files/e2e-evidencia ./e2e-evidencia
```
