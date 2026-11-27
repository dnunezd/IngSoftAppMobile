# Tasks

## 1. Recursos

- [x] 1.1 Crear `res/values-en/strings.xml` con los 233 textos y los 4 plurales traducidos, y el texto `idioma` en ambos idiomas; verificar con la prueba de paridad (2.1)
- [x] 1.2 Agregar `res/xml/locales_config.xml` (`es`, `en`) y `android:localeConfig` en el manifiesto; verificar con `lintDebug` y en los ajustes de idioma de la app del emulador API 35

## 2. Pruebas de traducción

- [x] 2.1 Prueba unitaria de paridad entre `values` y `values-en` (claves y marcadores); verificar que pasa y que falla si se borra un texto en inglés

## 3. Formatos por idioma

- [x] 3.1 Helper `idiomaDeLaApp(context)`; fecha larga con `DateFormat.LONG` y el idioma en `FechaLanzamiento` y en el detalle de álbum; verificar con pruebas unitarias en `es` y `en`
- [x] 3.2 Promedio con los símbolos decimales del idioma, y `estrellasLlenas` que acepte coma y punto; verificar con pruebas unitarias en `es` y `en`
- [x] 3.3 Selector de fecha en el idioma de la app (`EnIdiomaDeLaApp`) y `uppercase` con el idioma de la app; verificar con capturas en el emulador en inglés

## 4. Pruebas instrumentadas

- [x] 4.1 Crear `VinilosTestRunner`, que fija la app en español en API 33 o superior, y configurarlo en `testInstrumentationRunner`; verificar que la suite pasa en el emulador API 35 en inglés
- [x] 4.2 Prueba instrumentada de pantallas en inglés (Ingreso, Inicio, Detalle de álbum, Crear álbum y Comentar) con las comprobaciones de accesibilidad; verificar que pasa

## 5. Verificación y documentación

- [x] 5.1 Capturas en inglés de las pantallas principales en el emulador, con la fuente normal y al 200 %; revisar que no haya textos recortados
- [x] 5.2 Ejecutar `testDebugUnitTest`, `lintDebug` y la suite instrumentada completa en API 35
- [x] 5.3 Actualizar "Pendiente: internacionalización" en la wiki y la descripción del PR #120

## 6. Revisión del PR #120

- [x] 6.1 Fechas del artista en ISO en el repositorio y en Room (versión 3, `3.json`), formateadas en `PantallaDetalleArtista` con `idiomaDeLaApp()`; quitar el proveedor de idioma de `AppContainer`; verificar con pruebas unitarias y con el detalle guardado en inglés
- [x] 6.2 Botón principal mientras se envía con el naranja pleno (4,86:1), indicador de Asociar con `onPrimary` y Crear álbum con `coloresBotonPrincipal`; verificar con `ContrasteDePaletaTest`
- [x] 6.3 Envolver también el contenido del `Dialog` del selector en `EnIdiomaDeLaApp`; verificar con `SelectorFechaEnOtroIdiomaTest` (app en francés), que falla sin el cambio
- [x] 6.4 README y `docs/pruebas` con `VinilosTestRunner` en el comando de `am instrument`, y la nota de API 33
- [x] 6.5 Ejecutar `testDebugUnitTest`, `lintDebug` y la suite instrumentada completa en API 35
