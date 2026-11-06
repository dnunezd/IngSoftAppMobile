# Resultados: HU07 parte 1

Ejecución del 2026-11-06 sobre `feature/hu07-crear-album`, con JDK 21. Las pruebas en dispositivo usaron un BackVynils local aislado; nunca el backend de los tutores.

## Suites

| Suite | Resultado | Entorno |
|---|---|---|
| Unitarias (`testDebugUnitTest`) | 192 sin fallos (40 nuevas) | JVM |
| Lint | 0 errores, 48 avisos (los mismos de `develop`) | JVM |
| Instrumentadas sin E2E (`connectedDebugAndroidTest`) | 109 sin fallos | `Pixel_8_API_34` |

## Recorrido en dispositivo

| Dispositivo | Criterios | Giro, teclado, toques y segundo álbum |
|---|---|---|
| Motorola Edge 30 (físico, Android 14) | 27 de 28 (ver nota) | 7 de 7 |
| `Pixel_8_API_34` (emulador) | 29 de 29 | 7 de 7 |
| `Nexus_5X_API_23` (emulador, Android 6) | 29 de 29 | 5 de 5 |

**Nota:** el fallo del Motorola es del script: buscó "Álbum creado" cuando el aviso ya había desaparecido. La captura `09_album_creado.jpg` lo muestra en pantalla, y la comprobación pasó en ese mismo dispositivo en una corrida anterior y en los dos emuladores.

El Motorola no se giró, para no cambiar la configuración del teléfono; el giro se probó en los dos emuladores. El selector también se comprobó en API 23 a 320 dp de ancho.

## Defectos encontrados y corregidos en dispositivo

| Defecto | Corrección |
|---|---|
| El botón "Crear álbum" no tenía nombre para TalkBack: Material oculta el texto del botón extendido | El ícono lleva la descripción |
| El teclado autocorregía la URL de la portada y cambiaba la dirección | Campo sin autocorrección |
| En horizontal, `DatePickerDialog` deformaba el calendario y dejaba "Aceptar" fuera de la pantalla | Diálogo propio con calendario desplazable y botones fijos |
| En un dispositivo en inglés, el selector se mezclaba con los textos en español | El selector se muestra siempre en español |

## Riesgo conocido

Si el servidor guarda el álbum después de vencer el timeout de 10 s, el cliente muestra el error y reintentar crea un duplicado. Se reprodujo en la prueba. Evitarlo exige cambiar el backend, que no impide nombres repetidos (`design.md`, D4).

## Sin verificar

- Compilación con el JDK 17 de la CI.
- Pruebas E2E y de regresión (T07-5 y T07-6).
- TalkBack activo en un dispositivo: se comprobó que el botón expone su nombre, pero no se escuchó el lector.
