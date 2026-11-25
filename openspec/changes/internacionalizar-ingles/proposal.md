# Proposal

## Why

La revisión de accesibilidad dejó la internacionalización como pendiente: la app solo tiene textos en español y fija ese idioma a mano en fechas, números y el selector de fecha. Se pide agregar el inglés en el mismo PR de accesibilidad (#120), para que quien tenga el dispositivo en inglés use la app en su idioma, con las mismas descripciones para TalkBack.

## What Changes

- **Traducción completa al inglés:** `values-en/strings.xml` con los 233 textos y los plurales, incluidas las descripciones de accesibilidad ("Enter as Visitor", "Album cover, …", "5.0, out of 5 stars"). El español sigue siendo el idioma por defecto.
- **Elección del idioma:**
  - La app sigue el idioma del dispositivo: español o inglés, y español para cualquier otro.
  - En Android 13 o superior también se puede elegir por aplicación en los ajustes del sistema, mediante `localeConfig`.
- **Formatos por idioma:**
  - La fecha larga sigue el idioma de la app: "1 de enero de 1970" o "January 1, 1970". Hoy se fija en español, en `FechaLanzamiento` y en el repositorio de artistas, que la guarda ya formateada.
  - El promedio sigue el idioma de la app: "4,5" o "4.5".
  - El selector de fecha se muestra en el idioma de la app, no en el del sistema.
  - Las mayúsculas de las etiquetas usan el idioma de la app.
- **Sin traducir, a propósito:**
  - Los datos que vienen del backend: nombres, descripciones, géneros, disqueras y comentarios.
  - Los valores de género y disquera que se envían al crear un álbum.
  - Los textos de ambientación que ya están en inglés en el diseño ("Beats & Sound", "AUDIO WAVE", "VINILO CATALOG").
- **Pruebas:**
  - Las instrumentadas siguen en español aunque el dispositivo esté en inglés, mediante un runner que fija el idioma de la app.
  - Una prueba unitaria verifica que el inglés tenga las mismas claves y los mismos parámetros que el español.
  - Una prueba instrumentada muestra pantallas en inglés y repite sobre ellas las comprobaciones de accesibilidad.

## Capabilities

### New Capabilities
- `internacionalizacion`: idiomas disponibles, cómo se elige el idioma, formatos de fecha y número por idioma, y qué contenido no se traduce.

### Modified Capabilities

Ninguna. Los requisitos funcionales de cada HU no cambian; cambia el idioma en que se muestran.

## Impact

- **Recursos:**
  - `res/values-en/strings.xml` (nuevo).
  - `res/xml/locales_config.xml` (nuevo).
  - `AndroidManifest.xml` (`android:localeConfig`).
  - Un texto nuevo `idioma` en cada idioma, para que la app sepa en qué idioma muestra sus textos.
- **Código:**
  - `data/model/FechaLanzamiento.kt` (fecha larga por idioma).
  - `ArtistRepositoryImpl`, `PantallaDetalleArtista` y `VinilosDb` (fechas del artista en ISO, formateadas en la pantalla; Room a la versión 3).
  - `ui/detalle/PromedioCalificaciones.kt` (separador decimal).
  - `ui/crear/PantallaCrearAlbum.kt` (selector de fecha).
  - Los `uppercase()` de etiquetas.
- **Pruebas:**
  - `app/build.gradle.kts` (runner de pruebas instrumentadas).
  - Pruebas unitarias de fecha y promedio, que pasan a recibir el idioma.
  - Pruebas nuevas de paridad de traducciones y de pantallas en inglés.
- **Documentación:** se actualiza "Pendiente: internacionalización" en la wiki.
- **Fuera de alcance:**
  - Otros idiomas.
  - Un selector de idioma dentro de la app.
  - Traducir datos del backend.
  - Probar en inglés en API 23: las instrumentadas en API 23 siguen dependiendo del idioma del dispositivo.
