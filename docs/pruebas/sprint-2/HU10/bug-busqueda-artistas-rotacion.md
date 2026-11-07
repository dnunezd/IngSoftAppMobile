# Bug conocido: texto del buscador de artistas truncado al girar la pantalla

**Dónde aparece**: prueba E2E `ArtistasE2ETest.alGirarLaPantallaSeConservanLaBusquedaYElListadoSinVolverAConsultar` (FN03-ESC07).

**Historia dueña**: HU03 (listado de artistas). Se detectó e investigó durante las pruebas de HU10 porque ambas historias comparten `PantallaArtistas.kt`, pero el bug es anterior y no fue introducido por HU10.

## Síntoma

Al escribir un término de búsqueda completo y luego girar la pantalla (cambio real de orientación, no solo recreación de la Activity), el campo de texto del buscador muestra solo una porción del término ingresado (por ejemplo `"ub"` en vez del término completo). El listado filtrado, el contador y `artistas.consultas` (que confirma que no se repitió la consulta al backend) sí se conservan correctamente; **solo el texto visible del campo de búsqueda se trunca**.

## Hipótesis descartadas

1. **Colisión de clave autogenerada dentro de `rememberSaveable`**: se agregó una clave fija (`rememberSaveable(key = "buscador_artistas_texto")`) al estado local del campo. No tuvo efecto: el truncado persistió de forma idéntica.
2. **Región de composición del IME no confirmada**: se desactivó `autoCorrect` en el `KeyboardOptions` del campo, para evitar que el teclado mantenga texto subrayado sin confirmar durante la recreación de la Activity. Tampoco tuvo efecto observable.

## Causa más probable (no verificada en dispositivo)

El `Buscador` se compone dentro de un `item { }` de `LazyColumn` **sin key explícita**, igual que el `item` de la tarjeta principal del detalle de artista cuyo bug sí se confirmó y corrigió (ver commit `ef39c90`, favorito que no se autoocultaba). La diferencia es que aquí el síntoma no es un efecto que se reinicia, sino que el **valor restaurado por `rememberSaveable` queda asociado a la posición del item, no a su identidad semántica**: si el número de items que siguen al buscador cambia entre el momento en que Android guarda el estado de la instancia y el momento en que lo restaura (por ejemplo, porque el estado pasó de `Cargando` a `Exito` casi al mismo tiempo que ocurre el giro), el valor restaurado puede no corresponder al que se guardó para ese campo.

## Corrección aplicada

Se agregó una key explícita a los dos primeros `item { }` de la `LazyColumn` de `PantallaArtistas` (`"titular"` y `"buscador"`), para que su identidad no dependa de su posición ni de cuántos items dinámicos vienen después.

## Estado actual

- ✅ Compila sin errores, 238/238 pruebas unitarias (JVM) sin regresiones.
- ⚠️ **No verificado en dispositivo/emulador todavía** (tercer intento sobre este bug). Si el truncado persiste tras esta corrección, el problema no está en el keying del `LazyColumn` y debe tratarse como un defecto de la plataforma/Compose (posible issue conocido de `BasicTextField` con configuración de Compose BOM 2024.04.01) fuera del alcance razonable de corregir sin poder depurar directamente en el dispositivo.

## Recomendación si persiste

Documentarlo como riesgo conocido de HU03 (no bloqueante para HU10) y reportarlo a quien mantiene esa historia, en vez de seguir intentando correcciones a ciegas sin acceso directo al emulador/dispositivo para depurar.
