package com.example.vinilos.e2e

import android.graphics.Bitmap
import android.os.Build
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onRoot
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Evidencia de las pruebas E2E: un pantallazo por paso y un informe en Markdown, usando solo las
 * APIs de Jetpack Compose Testing ya incluidas en el proyecto (`captureToImage`), sin dependencias
 * nuevas.
 *
 * Se guarda únicamente en el almacenamiento propio de la app en el dispositivo
 * (`.../files/e2e-evidencia/<carpeta>/`), fuera del repositorio: no es rastreable por git. Si se
 * trae al PC con `adb pull`, queda bajo una carpeta `e2e-evidencia/` ignorada (ver `.gitignore`).
 */
object EvidenciaE2E {

    private const val CARPETA_BASE = "e2e-evidencia"
    private val formatoHora = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale("es"))

    private fun directorio(carpeta: String): File {
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        return File(contexto.getExternalFilesDir(null), "$CARPETA_BASE/$carpeta").apply { mkdirs() }
    }

    /** Captura el árbol Compose visible y agrega una fila al informe de [carpeta] para [paso]. */
    fun capturar(rule: ComposeTestRule, carpeta: String, paso: String) {
        val directorio = directorio(carpeta)
        try {
            val archivo = File(directorio, "$paso.png")
            val bitmap: Bitmap = capturarBitmap(rule)
            FileOutputStream(archivo).use { salida -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, salida) }
            registrar(directorio, paso, archivo.name, "Ok")
        } catch (e: Throwable) {
            // Si no hay árbol Compose visible (p.ej. tras cerrar la actividad) o la API no soporta
            // la captura (ver más abajo) se omite sin fallar la prueba: la evidencia es un
            // complemento, no una aserción.
            registrar(directorio, paso, "(sin captura)", "Sin captura: ${e.message}")
        }
    }

    /**
     * `captureToImage()` usa `PixelCopy`, disponible solo desde API 26. En API 23-25 se captura la
     * pantalla completa con `UiAutomation` (disponible desde API 18, sin dependencias nuevas); esa
     * captura incluye la barra de estado y los botones de navegación, a diferencia de la de
     * `captureToImage()`.
     */
    private fun capturarBitmap(rule: ComposeTestRule): Bitmap =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            rule.onRoot().captureToImage().asAndroidBitmap()
        } else {
            InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        }

    private fun registrar(directorio: File, paso: String, archivo: String, estado: String) {
        val informe = File(directorio, "informe.md")
        if (!informe.exists()) {
            informe.writeText("# Evidencia E2E\n\n| Paso | Estado | Captura | Hora |\n|---|---|---|---|\n")
        }
        informe.appendText("| $paso | $estado | $archivo | ${formatoHora.format(Date())} |\n")
    }
}
