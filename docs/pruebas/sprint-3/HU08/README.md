# HU08 · Asociar canciones a un álbum: pruebas y evidencia

## Pruebas E2E (T08-5)

Clase `AgregarCancionE2ETest` (cambio de OpenSpec [`add-e2e-agregar-tracks`](../../../../openspec/changes/add-e2e-agregar-tracks)). Recorre la app desde el ingreso, como la persona, con los tres escenarios más importantes: una validación del flujo, el flujo completo y el error con reintento. ESC01 y ESC02 van contra BackVynils; como agregan canciones, se corren contra el backend **local**, sobre un álbum propio (`POST /albums`) que se borra al terminar con sus canciones. ESC03 usa `MockWebServer` para provocar el error.

| ID         | Descripción del escenario                                                       | Resultado esperado                                                                                                                                                                                                         | Backend          | Evidencia                 |
|------------|---------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------|---------------------------|
| FN08-ESC01 | El Coleccionista escribe duraciones con formato inválido ("3:75" y "345") y luego una válida | "Usa el formato m:ss, por ejemplo 5:05" con "Guardar" deshabilitado; con "3:45" el error desaparece y "Guardar" se habilita; no se envía nada                                                                        | BackVynils local | [PDF](e2e/FN08_ESC01.pdf) |
| FN08-ESC02 | El Coleccionista agrega la canción "Pista E2E" de 3:45 a un álbum sin canciones | El formulario abre con "Campo obligatorio" en ambos campos y "Guardar" deshabilitado; al llenarlo se habilita; al guardar aparece "Canción agregada" y el detalle muestra la canción con su duración; el backend la guarda | BackVynils local | [PDF](e2e/FN08_ESC02.pdf) |
| FN08-ESC03 | Guarda una canción y el guardado tarda y falla                                  | Mientras guarda, "Guardando..." deshabilitado y un segundo toque no envía nada; luego "No se pudo agregar la canción, intenta de nuevo" con el nombre y la duración conservados; al reintentar se agrega                    | `MockWebServer`  | [PDF](e2e/FN08_ESC03.pdf) |

Criterios de la HU08 cubiertos por las E2E:

| #   | Criterio                                                                   | Escenarios   |
|-----|----------------------------------------------------------------------------|--------------|
| 1   | El Coleccionista ve la acción para agregar una canción                     | ESC01, ESC02 |
| 2   | El formulario abre vacío con "Campo obligatorio" y "Guardar" deshabilitado | ESC02        |
| 3   | Formato de duración `m:ss`; si no se cumple, "Guardar" deshabilitado       | ESC01        |
| 4   | Al guardar, "Canción agregada" y el detalle muestra la canción             | ESC02, ESC03 |
| 5   | Mientras guarda, "Guardando..." deshabilitado y una sola petición          | ESC03        |
| 6   | Error al guardar: mensaje, valores conservados y reintento                 | ESC03        |

Que el Visitante no vea la acción se cubre en `VinilosNavHostTest`; "Cancelar" y el álbum que ya no existe, en `PantallaAgregarCancionTest` (Compose UI) y `AgregarCancionViewModelTest` (unitarias).

### Resultados

Samsung SM-S938B (Android 17), 2026-11-22, contra BackVynils local: `OK (9 tests)` junto con las E2E de HU09 y HU15, sin escenarios omitidos y con todas las capturas en "Ok"; el backend quedó con sus 4 álbumes de ejemplo y ningún álbum de prueba.

### Cómo correrlas

```bash
./gradlew installDebug installDebugAndroidTest -PbaseUrl=http://localhost:3000/   # BackVynils local
adb reverse tcp:3000 tcp:3000                                                     # solo en un teléfono
adb shell rm -rf /sdcard/Android/data/com.example.vinilos/files/e2e-evidencia
adb shell am instrument -w -e class com.example.vinilos.AgregarCancionE2ETest com.example.vinilos.test/androidx.test.runner.AndroidJUnitRunner
adb pull /sdcard/Android/data/com.example.vinilos/files/e2e-evidencia ./e2e-evidencia
```
