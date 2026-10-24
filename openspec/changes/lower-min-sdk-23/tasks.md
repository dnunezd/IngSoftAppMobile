# Tasks

## 1. Bajar el mínimo y reubicar los íconos

- [x] 1.1 Cambiar `minSdk = 26` a `minSdk = 23` en `app/build.gradle.kts` y verificar que `./gradlew assembleDebug` compila y que el manifiesto fusionado (`app/build/intermediates/merged_manifests/debug/.../AndroidManifest.xml`) declara `android:minSdkVersion="23"`
- [x] 1.2 Mover con `git mv` `ic_launcher.xml` e `ic_launcher_round.xml` de `app/src/main/res/mipmap-anydpi/` a `app/src/main/res/mipmap-anydpi-v26/` y verificar que `mipmap-anydpi` ya no existe y que `./gradlew assembleDebug` sigue compilando
- [x] 1.3 Verificar que los `.webp` de `mipmap-mdpi` a `mipmap-xxxhdpi` (`ic_launcher` y `ic_launcher_round`) siguen presentes y, con `./gradlew lint`, que no aparecen errores nuevos de `NewApi`, `ObsoleteSdkInt` ni `MissingDefaultResource` respecto de la referencia (0 errores)

## 2. Revisar compatibilidad con API 23

- [x] 2.1 Buscar en `app/src` usos de APIs posteriores a API 23 (`java.time`, `java.util.stream`, `Optional`, `Build.VERSION`, `@RequiresApi`) y verificar que la búsqueda no encuentra ninguno en código de producción, o que cada uno encontrado queda cubierto con alternativa por versión
- [x] 2.2 Ejecutar `./gradlew testDebugUnitTest` y verificar que todas las pruebas unitarias pasan con `minSdk = 23`
- [x] 2.3 Ejecutar `./gradlew compileDebugAndroidTestKotlin` y verificar que las pruebas instrumentadas compilan con el nuevo mínimo
- [x] 2.4 Instalar la APK de depuración en el Samsung (adb inalámbrico, sin iniciar el emulador) y verificar a mano que el ingreso como Visitante y como coleccionista llega a la pantalla de inicio y que el ícono adaptativo sigue viéndose en el lanzador

## 3. Documentación

- [x] 3.1 Actualizar el README: en la tabla de requisitos, `minSdk` 26 a 23 (fila "Android SDK") y "API 26 o superior" a "API 23 o superior" (fila "Dispositivo Android"); verificar con `grep -n "26" README.md` que no queda ninguna referencia al mínimo anterior
- [x] 3.2 Verificar que `openspec validate lower-min-sdk-23 --strict` pasa

## 4. Comprobación en Android 6.0

- [ ] 4.1 Si hay un dispositivo con Android 6.0 (API 23), instalar la APK y verificar a mano los escenarios de `compatibilidad-android` (apertura, ingreso, listado de coleccionistas y ícono de imagen fija). Si no se dispone de uno, dejar esta tarea sin marcar y registrar en el resumen que API 23 no se ejecutó (no iniciar el emulador sin autorización de la persona)

## Workflow follow-up

- Archivar el cambio cuando se cumplan los requisitos de revisión del proyecto (el PR pasa la integración continua de `lint` y pruebas unitarias).
