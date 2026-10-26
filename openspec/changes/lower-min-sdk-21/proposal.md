# Proposal

## Why

El enunciado del curso pide que la app se pueda instalar y no genere errores en dispositivos Android "iniciando en la versión Lollipop", y la rúbrica de calidad lo evalúa. Hoy la app exige Android 6.0 (API 23). Además, la primera entrega debe aparecer como release `v1.0.0` sobre `main`, y la versión declarada en la app debe coincidir con ese tag.

## What Changes

- `minSdk` pasa de 23 a 21 (Android 5.0 Lollipop) en `app/build.gradle.kts`. `compileSdk` y `targetSdk` siguen en 34.
- `versionName` pasa a `"1.0.0"` (coincide con el tag `v1.0.0`) y `versionCode` sigue en 1; ambos se suben en cada release.
- `applicationId` pasa de `com.example.vinilos` a `com.misw.vinilos`: el prefijo `com.example` es el de la plantilla de Android Studio y no identifica la app. El `namespace` del código (`com.example.vinilos`) no cambia.
- Se actualiza el README (requisitos, versión y descarga del APK del release) y la spec `compatibilidad-android` (rango soportado desde API 21).
- Sin cambios en pantallas, textos, estados ni flujos.

## Capabilities

### New Capabilities

(Ninguna.)

### Modified Capabilities
- `compatibilidad-android`: la versión mínima soportada pasa de Android 6.0 (API 23) a Android 5.0 (API 21).

## Impact

- Código y configuración: `app/build.gradle.kts` (`minSdk`, `versionName`, `applicationId`). El código Kotlin no usa APIs condicionadas por versión.
- Dependencias: ninguna cambia; todas declaran `minSdk` 21 o menos.
- Instalación: al cambiar `applicationId`, una APK anterior (`com.example.vinilos`) instalada en el dispositivo queda como otra app; conviene desinstalarla.
- Documentación: README y spec `compatibilidad-android`.
- Fuera de alcance: bajar de API 21, subir `compileSdk` o `targetSdk`, cambiar el `namespace` y firmar el APK de release.
