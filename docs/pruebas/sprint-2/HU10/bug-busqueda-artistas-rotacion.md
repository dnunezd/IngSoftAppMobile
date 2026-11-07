# Bug conocido: texto del buscador de artistas truncado al girar la pantalla

**Dónde aparece**: prueba E2E `ArtistasE2ETest.alGirarLaPantallaSeConservanLaBusquedaYElListadoSinVolverAConsultar` (FN03-ESC07).

**Historia dueña**: HU03 (listado de artistas). Se detectó e investigó durante las pruebas de HU10 porque ambas historias comparten `PantallaArtistas.kt`, pero el bug es anterior y no fue introducido por HU10.

## Síntoma

Al escribir un término de búsqueda completo y luego girar la pantalla (cambio real de orientación, no solo recreación de la Activity), el campo de texto del buscador muestra solo una porción del término ingresado (por ejemplo `"ub"` en vez del término completo). El listado filtrado, el contador y `artistas.consultas` (que confirma que no se repitió la consulta al backend) sí se conservan correctamente; **solo el texto visible del campo de búsqueda se trunca**.

## Hipótesis descartadas

1. **Colisión de clave autogenerada dentro de `rememberSaveable`**: se agregó una clave fija (`rememberSaveable(key = "buscador_artistas_texto")`) al estado local del campo. No tuvo efecto: el truncado persistió de forma idéntica.
2. **Región de composición del IME no confirmada (autoCorrect)**: se desactivó `autoCorrect` en el `KeyboardOptions` del campo, para evitar que el teclado mantenga texto subrayado sin confirmar durante la recreación de la Activity. Tampoco tuvo efecto observable.
3. **Clave posicional en `LazyColumn`**: se agregó key explícita (`"titular"`, `"buscador"`) a los dos primeros `item { }` de la `LazyColumn`, para que la identidad de `Buscador` no dependa de su posición ni de cuántos items dinámicos vienen después. Verificado en dispositivo/emulador: **no tuvo efecto**, el truncado persistió de forma idéntica (mismo `"ub"`).
4. **El teclado se oculta sin confirmar el texto compuesto**: se agregó `rule.onNodeWithTag(TagBuscadorArtistas).performImeAction()` antes de `Espresso.closeSoftKeyboard()`, para forzar al IME a confirmar (`finishComposingText`) el texto mediante la acción "Buscar" ya configurada en el campo (`ImeAction.Search`), tal como haría un usuario real al presionar Buscar antes de girar el teléfono. Verificado en dispositivo/emulador: **tampoco tuvo efecto**, el truncado persistió de forma idéntica.

## Conclusión tras 4 intentos

Se agotaron las hipótesis razonables a nivel de nuestro código (estado local, keying de `LazyColumn`, y cierre/confirmación del IME). Las cuatro correcciones fueron verificadas en dispositivo/emulador real y **ninguna cambió el síntoma**, que se reproduce de forma idéntica (truncado a `"ub"`) en cada intento. Esta consistencia exacta ante causas tan distintas sugiere que el origen está **fuera del código de la app**: muy probablemente en cómo el `InputMethodManager`/IME de la plataforma (AOSP, emulador API 23) entrega el commit final de texto exactamente durante la ventana de destrucción/recreación de la Activity por el giro real de pantalla — algo que no es observable ni corregible sin depurar directamente el flujo de `InputConnection` a nivel de plataforma.

No se detectó este problema con `escenario.recreate()` (recreación programática sin IME real de por medio) en otras pruebas de este mismo archivo; solo aparece con el giro físico/real (`requestedOrientation`), lo que refuerza que el IME real del dispositivo es el factor diferencial, no la lógica de la app.

## Estado actual

- ✅ Compila sin errores en los 4 intentos; sin regresiones en pruebas unitarias (JVM).
- ❌ El truncado persiste después de 4 correcciones distintas, todas verificadas en dispositivo/emulador real.
- 🛑 Se decide **detener los intentos de corrección a ciegas**: sin poder depurar directamente el `InputConnection` a nivel de plataforma, seguir cambiando código de la app tiene baja probabilidad de resolverlo y sigue consumiendo tiempo/tokens.

## Recomendación

Documentar este caso como **riesgo conocido de HU03** (no bloqueante para HU10, que solo comparte el archivo `PantallaArtistas.kt` pero no introdujo el bug) y reportarlo a quien mantiene esa historia. Si se requiere resolverlo a futuro, el siguiente paso sería depurar con Logcat/Layout Inspector en el dispositivo real mientras se gira físicamente con el teclado abierto, para capturar la secuencia exacta de eventos de `InputConnection` entre el commit de texto y la recreación de la Activity.
