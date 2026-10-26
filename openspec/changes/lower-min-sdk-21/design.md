# Design

## Context

`app/build.gradle.kts` declara `minSdk = 23`, `versionName = "1.0"` y `applicationId = "com.example.vinilos"`. El cambio archivado `2026-10-25-lower-min-sdk-23` ya revisó la compatibilidad hasta API 23: los íconos adaptativos están en `mipmap-anydpi-v26` con `.webp` de respaldo, el código no usa `java.time`, `java.util.stream` ni `Build.VERSION`, y todas las dependencias declaran `minSdk` 21 o menos (Compose BOM 2024.04.01, Material 3, Activity 1.9.0, Navigation 2.7.7, Lifecycle 2.7.0, Core KTX 1.13.1, Retrofit 2.9.0, OkHttp 4.12.0, Espresso 3.7.0).

Lo que cambia entre API 21 y 23:

- **Barras del sistema**: `MainActivity` usa `enableEdgeToEdge` con `SystemBarStyle.light`. Los íconos oscuros en la barra de estado existen desde API 23; en API 21 y 22 Activity usa el `darkScrim` del estilo, que hoy es transparente, así que los íconos de la barra de estado se verán claros sobre el fondo claro de la app.
- **`networkSecurityConfig`**: ya se ignoraba en API 23 (solo aplica desde API 24); en API 21 y 22 el HTTP sin cifrar también queda permitido por defecto.
- **Vectores**: los del proyecto solo usan atributos de API 21.

## Goals / Non-Goals

**Goals:**
- Que la app compile, pase `lint` (0 errores) y las pruebas unitarias con `minSdk = 21`.
- Que la versión declarada (`versionName`) coincida con el tag del release.

**Non-Goals:**
- Dar soporte por debajo de API 21, subir `compileSdk` o `targetSdk`.
- Cambiar el `namespace` ni los paquetes del código.
- Ajustar el color de la barra de estado en API 21 y 22.

## Decisions

**1. Bajar solo `minSdk`, sin desugaring.** El código no usa APIs de Java 8+ de la biblioteca estándar. Si una historia futura las usa, `lint` (`NewApi`) lo marcará.

**2. Cambiar `applicationId` sin cambiar `namespace`.** `applicationId` es la identidad de la app instalada y es lo que se publica en el APK del release; `namespace` determina el paquete de `R` y `BuildConfig`. Cambiar solo el primero evita tocar los imports de todo el código y de las ramas abiertas.

**3. Versión semántica alineada con el tag.** `versionName` sigue el formato `MAYOR.MENOR.PARCHE` y coincide con el tag (`1.0.0` ↔ `v1.0.0`); `versionCode` es un entero que se incrementa en cada release.

**4. Aceptar la barra de estado clara en API 21 y 22.** Afecta solo a la legibilidad de la hora y los íconos del sistema en Android 5.x, no a la app. Se puede corregir más adelante con un `darkScrim` semitransparente.

## Risks / Trade-offs

- **[Algo falla solo en API 21 o 22 y `lint` no lo ve]** → Las dependencias ya declaran `minSdk` 21; verificar a mano en un emulador o dispositivo con Android 5.x antes del release. Si no hay uno disponible, dejarlo como pendiente declarado.
- **[Una APK anterior queda instalada como otra app]** → Por el cambio de `applicationId`; desinstalarla antes de instalar la nueva.
