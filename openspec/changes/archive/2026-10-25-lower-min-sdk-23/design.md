# Design

## Context

La app declara `minSdk = 26`, `compileSdk = 34` y `targetSdk = 34` (`app/build.gradle.kts`). Ver `proposal.md` - Why. Lo observado al revisar qué pasa al bajar a 23:

- **Dependencias**: todas declaran `minSdk` 21 o menos (Compose BOM 2024.04.01, Material 3, Activity 1.9.0, Navigation 2.7.7, Lifecycle 2.7.0, Core KTX 1.13.1, Retrofit 2.9.0, OkHttp 4.12.0, Espresso 3.7.0). El merge del manifiesto no fallará por ellas.
- **Código Kotlin**: no hay `Build.VERSION`, `@RequiresApi` ni APIs `java.time`/`java.util.stream`; `enableEdgeToEdge` de Activity 1.9.0 ya contempla versiones antiguas. El proyecto compila a Java 8 y no usa desugaring.
- **Íconos del lanzador**: `ic_launcher.xml` e `ic_launcher_round.xml` son `<adaptive-icon>` y están en `res/mipmap-anydpi/` sin calificador de versión. Eso solo es válido con `minSdk >= 26`. Existen los `.webp` de respaldo en `mipmap-mdpi` a `mipmap-xxxhdpi`. `ic_launcher_foreground.xml` usa un degradado con `aapt:attr`, que el framework no dibuja antes de API 24, pero solo se referencia desde el ícono adaptativo (API 26+).
- **Manifiesto**: `networkSecurityConfig` se ignora antes de API 24 y `dataExtractionRules` antes de API 31 (ya lo estaba); `fullBackupContent` cubre hasta API 30. El manifiesto ya usa `tools:targetApi="31"`.
- **Otros recursos**: los vectores del proyecto solo usan atributos de API 21 (`strokeLineCap`, `fillType`); el tema hereda de `android:Theme.Material.Light.NoActionBar` (API 21).
- **Entorno de prueba**: la verificación en dispositivo se hace en el Samsung físico, y el único emulador instalado es API 35. Ninguno corre Android 6.0.

## Goals / Non-Goals

**Goals:**
- Que la app compile, pase `lint` y las pruebas unitarias con `minSdk = 23` sin cambiar el comportamiento visible.
- Que el ícono del lanzador resuelva correctamente en API 23 a 25.

**Non-Goals:**
- Dar soporte por debajo de API 23, ni subir `compileSdk` o `targetSdk`.
- Cambiar el diseño del ícono: solo se reubican los XML existentes.
- Reproducir en CI una ejecución en API 23 (el flujo actual solo corre `lint` y unitarias).

## Decisions

**1. Bajar solo `minSdk`, sin desugaring.** El código no usa APIs de Java 8+ de la biblioteca estándar que falten en API 23, así que no se activa `isCoreLibraryDesugaringEnabled`. Alternativa: activarlo por si acaso; se descarta porque añade una dependencia y tiempo de compilación sin necesidad hoy. Si una historia futura usa `java.time`, `lint` lo marcará (`NewApi`) y se decide entonces.

**2. Mover los íconos adaptativos a `mipmap-anydpi-v26`** (con `git mv`, mismo nombre de archivo y contenido). Es la convención de Android para este caso: API 26+ toma el adaptativo y API 23 a 25 cae a los `.webp` de `mipmap-*dpi`, que ya están en el proyecto. Alternativa: borrar el adaptativo y dejar solo los `.webp`; se descarta porque degradaría el ícono en la mayoría de dispositivos. El nodo `<monochrome>` (API 33) se mantiene tal cual: las versiones anteriores lo ignoran.

**3. Aceptar que en API 23 no se aplique `network_security_config.xml`.** La configuración restringe el HTTP sin cifrar a `10.0.2.2` y `localhost` y solo la leen API 24+. En API 23 el HTTP sin cifrar queda permitido por defecto. Se acepta porque la app solo habla con el backend local de desarrollo (HTTP) y no hay otro destino; la política de HTTP sin cifrar está fuera de alcance. Alternativa: `android:usesCleartextTraffic="false"` condicionado por versión; se descarta porque rompería el backend local en API 23.

**4. Verificar con `lint` y pruebas, y ejecución real en API 23 como paso separado.** `lint` con `minSdk = 23` marca `NewApi`/`ObsoleteSdkInt` y recursos incompatibles; es la comprobación automatizable y ya corre en CI. La ejecución en un Android 6.0 real o emulado no se puede hacer con el Samsung ni con el AVD API 35. El emulador solo se inicia si la persona lo autoriza; si no hay dispositivo API 23, esa comprobación queda como pendiente declarado y no como verificada.

## Risks / Trade-offs

- **[Algo falla solo en API 23 y `lint` no lo ve]** (por ejemplo, un comportamiento de Compose o de OkHttp en Android 6.0) → Dependencias que ya declaran `minSdk` 21; ejecutar el flujo de ingreso en un API 23 si la persona dispone de uno; hasta entonces, no declarar la spec como verificada en ese nivel.
- **[En API 23 se pierde la restricción de HTTP sin cifrar a hosts locales]** → Aceptado (Decisión 3); solo afecta a builds de desarrollo y a dispositivos de Android 6.0.
- **[El ícono en API 23 a 25 se ve distinto al adaptativo]** → Es el comportamiento estándar; usa los `.webp` ya existentes.
