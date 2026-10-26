# Proposal

## Why

Hoy la app exige Android 8.0 (API 26) o superior (`minSdk = 26`), lo que deja fuera a los dispositivos con Android 6.0 y 7.x. Bajar el mínimo a API 23 (Android 6.0) amplía los dispositivos en los que se puede instalar y probar Vinilos, sin cambiar lo que la persona ve ni hace.

## What Changes

- `minSdk` pasa de 26 a 23 en `app/build.gradle.kts`. `compileSdk` y `targetSdk` siguen en 34.
- Los íconos adaptativos del lanzador (`ic_launcher` e `ic_launcher_round`, hoy en `mipmap-anydpi`) pasan a `mipmap-anydpi-v26`: los íconos adaptativos solo existen desde API 26 y, con `minSdk` 23, el recurso sin calificador no compila ni se resuelve en API 23 a 25. En esas versiones se usan los `.webp` ya existentes en `mipmap-*dpi`.
- Se revisa que ningún código, recurso ni dependencia use APIs posteriores a API 23 sin alternativa, y que `lint` no reporte errores nuevos.
- Se actualiza el README (tabla de requisitos y dispositivo soportado: "API 23 o superior") y se deja constancia en una nueva spec del rango de Android soportado.
- Sin cambios visibles en pantallas, textos, estados ni flujos; la única diferencia visible es que en Android 6.0 a 7.1 el ícono del lanzador no es adaptativo.

## Capabilities

### New Capabilities
- `compatibilidad-android`: rango de versiones de Android en las que se puede instalar y usar la app, y que el comportamiento visible (ingreso, inicio, carga de coleccionistas) es el mismo en todo ese rango.

### Modified Capabilities

(Ninguna: los requisitos de `ingreso-plataforma`, `pantalla-inicio` y `sesion-usuario` no cambian.)

## Impact

- Código y recursos: `app/build.gradle.kts` (`minSdk`) y `app/src/main/res/mipmap-anydpi/*` (movidos a `mipmap-anydpi-v26`). El código Kotlin no usa `Build.VERSION` ni APIs condicionadas por versión, así que no se prevén cambios.
- Dependencias: ninguna cambia; las de la app (Compose BOM 2024.04.01, Material 3, Activity 1.9.0, Navigation 2.7.7, Lifecycle 2.7.0, Retrofit 2.9.0, OkHttp 4.12.0, Core KTX 1.13.1) ya declaran `minSdk` 21.
- Documentación: README (requisitos y dispositivos) y la nueva spec `compatibilidad-android`.
- Fuera de alcance: bajar de API 23, subir `compileSdk` o `targetSdk`, incluir `desugaring` de bibliotecas Java 8+ (el código no las usa) y cambiar la política de HTTP sin cifrar.
