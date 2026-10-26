# Tasks

## 1. Versión y mínimo de Android

- [x] 1.1 En `app/build.gradle.kts`, cambiar `minSdk = 23` a `minSdk = 21`, `versionName` a `"1.0.0"` y `applicationId` a `"com.misw.vinilos"`, sin cambiar `namespace`
- [x] 1.2 Ejecutar `./gradlew lint testDebugUnitTest --continue` y verificar 0 errores de lint (en particular `NewApi` y `ObsoleteSdkInt`) y que todas las pruebas unitarias pasan
- [x] 1.3 Ejecutar `./gradlew assembleDebug compileDebugAndroidTestKotlin` y verificar que la APK compila y que el manifiesto fusionado declara `android:minSdkVersion="21"`

## 2. Documentación

- [x] 2.1 Actualizar el README: requisitos (`minSdk` 21, API 21 o superior), versión de la app y descarga del APK del release
- [x] 2.2 Verificar que `openspec validate lower-min-sdk-21 --strict` pasa

## 3. Comprobación en Android 5.x

- [ ] 3.1 Si hay un emulador o dispositivo con Android 5.0 o 5.1 (API 21 o 22), instalar la APK y verificar a mano los escenarios de `compatibilidad-android`. Si no se dispone de uno, dejar esta tarea sin marcar y registrarlo en el PR

## Workflow follow-up

- Archivar el cambio cuando el PR pase la integración continua y se integre a `develop`.
