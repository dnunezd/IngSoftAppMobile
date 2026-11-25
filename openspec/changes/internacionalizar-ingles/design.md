# Design

## Context

Estado actual:
- Hay 233 textos en `values/strings.xml`, sin carpetas por idioma. No hay textos visibles escritos directo en el código; los literales en español del código son mensajes de excepciones internas y vistas previas.
- **Español fijado a mano:**
  - `FechaLanzamiento.formatearFechaLanzamiento` usa el patrón `"d 'de' MMMM 'de' yyyy"` con `Locale("es")`. Lo usan el detalle de álbum y `ArtistRepositoryImpl.fechaEnEspanol`, que arma el texto ya formateado en la capa de datos.
  - `promedioCalificaciones` usa la coma decimal española, y `estrellasLlenas` asume esa coma.
  - `PantallaCrearAlbum.EnEspanol` obliga al selector de fecha a mostrarse en español.
  - `ordenAlfabetico()` usa un `Collator` español.
- **Pruebas:** las instrumentadas buscan textos en español (unas 200 búsquedas en 29 archivos). El emulador de trabajo está en inglés. Con un `values-en`, la app saldría en inglés en ese emulador y esas pruebas fallarían.
- **Mayúsculas:** varias etiquetas usan `uppercase()` sin idioma, es decir, con el idioma por defecto de la JVM.

## Goals / Non-Goals

**Goals:**
- Inglés completo, con el español como idioma por defecto y sin cambiar el comportamiento en español.
- Que las fechas, los promedios y el selector de fecha coincidan siempre con el idioma de los textos.
- Que la suite de pruebas siga pasando en un emulador en inglés.

**Non-Goals:**
- Un selector de idioma dentro de la app: se usan los ajustes del sistema.
- Agregar `appcompat`.
- Que las instrumentadas en API 23 sigan en español con el sistema en inglés: ahí dependen del idioma del dispositivo.

## Decisions

1. **El idioma de la app es el de sus recursos.** Se agrega un texto `idioma` ("es" en `values` y "en" en `values-en`). Un helper `idiomaDeLaApp(context)` lo lee y lo convierte en `Locale`. Así, los formatos usan exactamente el idioma que Android eligió para los textos, también cuando el dispositivo está en un tercer idioma y la app cae al español.
   *Alternativa descartada:* `Locale.getDefault()`. Con el dispositivo en francés daría fechas en francés junto a textos en español.
2. **La fecha larga se formatea con el idioma.** `formatearFechaLanzamiento(fechaIso, idioma)` usa `DateFormat.getDateInstance(DateFormat.LONG, idioma)`, que da "1 de enero de 1970" y "January 1, 1970". El parámetro tiene el español por defecto, para que las pruebas unitarias actuales no cambien.
   - `ArtistRepositoryImpl` recibe un proveedor del idioma (`() -> Locale`) desde `AppContainer`, que lo toma de `idiomaDeLaApp(context)`. Como es un proveedor, un cambio de idioma con la app abierta se nota en la siguiente lectura.
   - En el detalle de álbum, la fecha se formatea en la presentación con el idioma del `LocalContext`.
3. **El promedio sigue el idioma, y las estrellas no dependen del texto.** `promedioCalificaciones(comentarios, idioma)` usa los símbolos decimales del idioma. `estrellasLlenas` acepta coma o punto, como ya lo hace al reemplazar la coma.
4. **El selector de fecha usa el idioma de la app.** `EnEspanol` pasa a ser `EnIdiomaDeLaApp`, que aplica `idiomaDeLaApp` a la configuración del selector. Se mantiene la razón original: el estado del calendario toma el idioma al crearse.
5. **Las mayúsculas usan el idioma de la app.** Los `uppercase()` de etiquetas pasan a `uppercase(idioma)`.
6. **Idioma por aplicación en Android 13 o superior.** Se agrega `res/xml/locales_config.xml` con `es` y `en`, y `android:localeConfig` en el manifiesto. No requiere `appcompat`.
7. **El runner de pruebas fija el español.** Se crea `VinilosTestRunner`, que extiende `AndroidJUnitRunner`. En `onStart`, con API 33 o superior, fija los idiomas de la app a `es` con `LocaleManager` antes de que se abra cualquier actividad. Se configura en `testInstrumentationRunner`.
   *Alternativa descartada:* pedir que el emulador esté en español. Rompería las pruebas de cualquiera del equipo con el emulador en inglés.
8. **Pruebas de la traducción.**
   - Una prueba unitaria lee `values/strings.xml` y `values-en/strings.xml` y exige las mismas claves (`string` y `plurals`) y los mismos marcadores (`%1$s`, `%d`, …).
   - Una prueba instrumentada envuelve las pantallas en un contexto inglés (`createConfigurationContext` con `en`, mediante `LocalContext` y `LocalConfiguration`). Comprueba textos clave ("Enter as Visitor") y repite las comprobaciones de accesibilidad de Compose.
9. **Lo que no se traduce.** `GenerosAlbum` y `DisquerasAlbum` son valores del backend y se muestran y envían igual. El `Collator` de orden sigue en español porque los nombres del catálogo están en español; ordena bien también en inglés.

## Risks / Trade-offs

- **Textos en inglés más largos o más cortos** pueden cambiar el diseño. Se mitiga con capturas en inglés de las pantallas principales y con la prueba de accesibilidad con la fuente al 200 % en inglés.
- **El runner cambia el idioma de la app en el emulador.** En la práctica no persiste: la suite desinstala la app al terminar, y con ella su idioma elegido (comprobado con `cmd locale get-app-locales`).
- **`stringResource` lee `LocalResources`** (Compose 1.12). Para cambiar el idioma solo dentro de una composición, como hacen el selector de fecha y la prueba en inglés, hay que proveer también `LocalResources`. Sin eso, el título del selector quedaba en el idioma del sistema; era un error que ya existía ("Selected date" con la app en español) y quedó corregido.
- **API 23:** sin `LocaleManager`, las instrumentadas en un emulador API 23 en inglés mostrarían la app en inglés. Se documenta.
