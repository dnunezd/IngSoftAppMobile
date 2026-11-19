# Tasks

## 1. Paleta y prueba de contraste

- [x] 1.1 Calcular la razón de contraste de los pares reales (`Naranja`/blanco, `Naranja`/`Fondo`, `TitularGris`/`Fondo`, chips, avatares) y elegir el valor de `NaranjaContraste` (≥ 4,5:1)
- [x] 1.2 Agregar `NaranjaContraste` y el gris de titular en `ui/theme/Color.kt`
- [x] 1.3 Prueba unitaria que verifica la razón de cada par fondo/texto de la paleta

## 2. Contraste por pantalla

- [x] 2.1 Ingreso: titular "Beats,", "Sound", sticker, chip "Acceso Libre" y botones "Entrar"
- [x] 2.2 Coleccionistas: chips, "eres?" e iniciales de los avatares
- [x] 2.3 Inicio: "VINILO CLUB", "Ver álbumes" y "Ver artistas"
- [x] 2.4 Catálogo y Crear álbum: chips "VINILO CATALOG", "álbumes", "álbum", tarjetas y botón "Crear álbum"
- [x] 2.5 Detalle de álbum: género, encabezados de sección, número de pista, calificación y estrellas
- [x] 2.6 Artistas: chips "LEYENDAS & BANDAS" y "artistas"

## 3. Semántica

- [x] 3.1 Ocultar a TalkBack los iconos decorativos (flecha de "Entrar", iconos del encabezado de Coleccionistas) y unir el botón en un solo elemento
- [x] 3.2 Marcar los encabezados de pantalla y de sección con `heading()` y revisar el encabezado "Artistas" del listado

## 4. Regresión automática

- [x] 4.1 Prueba instrumentada con `enableAccessibilityChecks()` que recorre las 8 pantallas con repositorios falsos
- [x] 4.2 Ejecutar la prueba en el emulador API 35

## 5. Verificación

- [x] 5.1 Repetir el Accessibility Scanner en las 8 pantallas y confirmar 0 sugerencias de contraste y de descripciones
- [x] 5.2 Publicar capturas y reporte antes/después en la página "Sprint 3 · Accesibilidad" de la wiki
- [x] 5.3 Ejecutar `lintDebug` y las pruebas unitarias; confirmar que las E2E existentes siguen pasando

## 6. Fuente ampliada, modo oscuro y TalkBack

- [x] 6.1 Recorrer las 8 pantallas con la fuente al 200 % y registrar solapamientos y recortes
- [x] 6.2 Reemplazar las alturas fijas de barras, campos y botones por alturas mínimas
- [x] 6.3 Titular de Ingreso, cabecera de Coleccionistas y fila de perfil: flujo que crece con el texto
- [x] 6.4 Recorrer las 8 pantallas con el modo oscuro del sistema y confirmar que se mantiene el tema claro
- [x] 6.5 TalkBack: botón "Entrar" como un solo elemento, etiquetas de los campos de "Crear álbum" y descripción del promedio en estrellas
- [x] 6.6 Pruebas instrumentadas con fuente 1x y 2x, y del botón y de las etiquetas del formulario
- [x] 6.7 Ocultar a TalkBack los textos de ambientación (`Modifier.decorativo()`) sin cambiar el diseño
- [x] 6.8 Ejecutar la suite completa y la prueba de accesibilidad en el emulador API 35
