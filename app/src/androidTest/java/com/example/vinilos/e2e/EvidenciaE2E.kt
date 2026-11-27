package com.example.vinilos.e2e

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.os.Build
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onRoot
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Evidence for the E2E tests: a screenshot per step, a Markdown report and a PDF with the
 * summary and screenshots, using only the Jetpack Compose Testing APIs already included in the
 * project (`captureToImage`) and Android's `PdfDocument`, with no new dependencies.
 *
 * It is saved only in the app's own storage on the device
 * (`.../files/e2e-evidencia/<folder>/`), outside the repository: it is not tracked by git. If
 * it's pulled to the PC with `adb pull`, it ends up under an `e2e-evidencia/` folder that is
 * ignored (see `.gitignore`).
 */
object EvidenciaE2E {

    private const val CARPETA_BASE = "e2e-evidencia"
    private val formatoHora = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.forLanguageTag("es"))

    private class Paso(val nombre: String, val estado: String, val captura: File?, val hora: String)

    /** Steps recorded in this run, by folder: the PDF shows only the current run. */
    private val pasos = mutableMapOf<String, MutableList<Paso>>()

    private fun directorio(carpeta: String): File {
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        return File(contexto.getExternalFilesDir(null), "$CARPETA_BASE/$carpeta").apply { mkdirs() }
    }

    /**
     * Captures the visible Compose tree, adds a row to [carpeta]'s report for [paso] and
     * regenerates `<carpeta>.pdf` with all the steps recorded so far.
     */
    fun capturar(rule: ComposeTestRule, carpeta: String, paso: String) {
        val directorio = directorio(carpeta)
        try {
            val archivo = File(directorio, "$paso.png")
            val bitmap: Bitmap = capturarBitmap(rule)
            FileOutputStream(archivo).use { salida -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, salida) }
            registrar(directorio, carpeta, paso, archivo, "Ok")
        } catch (e: Throwable) {
            // If there's no visible Compose tree (e.g. after closing the activity) or the API doesn't
            // support capturing (see below), it's skipped without failing the test: the evidence is a
            // complement, not an assertion.
            registrar(directorio, carpeta, paso, null, "Sin captura: ${e.message}")
        }
    }

    /**
     * `captureToImage()` uses `PixelCopy`, available only since API 26. On API 23-25 the full
     * screen is captured with `UiAutomation` (available since API 18, no new dependencies); that
     * capture includes the status bar and the navigation buttons, unlike the one from
     * `captureToImage()`.
     *
     * With a popup open (a dialog or a bottom sheet) there is more than one Compose root and
     * `onRoot()` doesn't know which one to capture; the full screen is also used in that case, which
     * shows the window over the background screen.
     */
    private fun capturarBitmap(rule: ComposeTestRule): Bitmap =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && rule.onAllNodes(isRoot()).fetchSemanticsNodes().size == 1) {
            rule.onRoot().captureToImage().asAndroidBitmap()
        } else {
            // The device screen may lag one frame behind the Compose tree (after
            // navigating, closing the keyboard, or receiving data): it waits until it settles.
            rule.waitForIdle()
            val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
            runCatching { uiAutomation.waitForIdle(500, 5_000) }
            uiAutomation.takeScreenshot()
        }

    private fun registrar(directorio: File, carpeta: String, paso: String, captura: File?, estado: String) {
        val hora = formatoHora.format(Date())
        val informe = File(directorio, "informe.md")
        if (!informe.exists()) {
            informe.writeText("# Evidencia E2E\n\n| Paso | Estado | Captura | Hora |\n|---|---|---|---|\n")
        }
        informe.appendText("| $paso | $estado | ${captura?.name ?: "(sin captura)"} | $hora |\n")

        val lista = pasos.getOrPut(carpeta) { mutableListOf() }
        lista.add(Paso(paso, estado, captura, hora))
        try {
            generarPdf(File(directorio, "$carpeta.pdf"), carpeta, lista)
        } catch (e: Throwable) {
            // Same as the screenshot: a failed PDF should not fail the test.
        }
    }

    // A4 page in points (1/72 inch).
    private const val ANCHO = 595
    private const val ALTO = 842
    private const val MARGEN = 40f

    /** Cover page with the device and the step table, then one page per step with its screenshot. */
    private fun generarPdf(destino: File, carpeta: String, lista: List<Paso>) {
        val titulo = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 18f; isFakeBoldText = true }
        val subtitulo = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 14f; isFakeBoldText = true }
        val texto = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 11f }
        val borde = Paint().apply { style = Paint.Style.STROKE; color = Color.LTGRAY }

        val pdf = PdfDocument()
        try {
            var numero = 1
            val portada = pdf.startPage(PdfDocument.PageInfo.Builder(ANCHO, ALTO, numero++).create())
            with(portada.canvas) {
                var y = MARGEN + 18f
                drawText("Evidencia E2E: $carpeta", MARGEN, y, titulo)
                y += 24f
                drawText(
                    "Dispositivo: ${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE} " +
                        "(API ${Build.VERSION.SDK_INT})",
                    MARGEN, y, texto,
                )
                y += 16f
                drawText("Generado: ${lista.last().hora}", MARGEN, y, texto)
                y += 32f
                drawText("Paso", MARGEN, y, subtitulo)
                drawText("Estado", 300f, y, subtitulo)
                drawText("Hora", 430f, y, subtitulo)
                lista.forEach { paso ->
                    y += 18f
                    drawText(paso.nombre, MARGEN, y, texto)
                    drawText(paso.estado.take(22), 300f, y, texto)
                    drawText(paso.hora, 430f, y, texto)
                }
            }
            pdf.finishPage(portada)

            lista.forEach { paso ->
                val pagina = pdf.startPage(PdfDocument.PageInfo.Builder(ANCHO, ALTO, numero++).create())
                with(pagina.canvas) {
                    var y = MARGEN + 14f
                    drawText(paso.nombre, MARGEN, y, subtitulo)
                    y += 18f
                    drawText("${paso.estado} · ${paso.hora}", MARGEN, y, texto)
                    y += 14f
                    // Half resolution is enough to read the screenshot and keeps the PDF lightweight.
                    val opciones = BitmapFactory.Options().apply { inSampleSize = 2 }
                    paso.captura?.let { BitmapFactory.decodeFile(it.path, opciones) }?.let { bitmap ->
                        val escala = minOf(
                            (ANCHO - 2 * MARGEN) / bitmap.width,
                            (ALTO - MARGEN - y) / bitmap.height,
                        )
                        val izquierda = (ANCHO - bitmap.width * escala) / 2
                        val area = RectF(izquierda, y, izquierda + bitmap.width * escala, y + bitmap.height * escala)
                        drawBitmap(bitmap, null, area, Paint(Paint.FILTER_BITMAP_FLAG))
                        drawRect(area, borde)
                        bitmap.recycle()
                    }
                }
                pdf.finishPage(pagina)
            }
            FileOutputStream(destino).use { pdf.writeTo(it) }
        } finally {
            pdf.close()
        }
    }
}
