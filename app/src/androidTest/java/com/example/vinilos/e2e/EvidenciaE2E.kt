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
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onRoot
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Evidencia de las pruebas E2E: un pantallazo por paso, un informe en Markdown y un PDF con el
 * resumen y las capturas, usando solo las APIs de Jetpack Compose Testing ya incluidas en el
 * proyecto (`captureToImage`) y `PdfDocument` de Android, sin dependencias nuevas.
 *
 * Se guarda únicamente en el almacenamiento propio de la app en el dispositivo
 * (`.../files/e2e-evidencia/<carpeta>/`), fuera del repositorio: no es rastreable por git. Si se
 * trae al PC con `adb pull`, queda bajo una carpeta `e2e-evidencia/` ignorada (ver `.gitignore`).
 */
object EvidenciaE2E {

    private const val CARPETA_BASE = "e2e-evidencia"
    private val formatoHora = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale("es"))

    private class Paso(val nombre: String, val estado: String, val captura: File?, val hora: String)

    /** Pasos registrados en esta ejecución, por carpeta: el PDF muestra solo la corrida actual. */
    private val pasos = mutableMapOf<String, MutableList<Paso>>()

    private fun directorio(carpeta: String): File {
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        return File(contexto.getExternalFilesDir(null), "$CARPETA_BASE/$carpeta").apply { mkdirs() }
    }

    /**
     * Captura el árbol Compose visible, agrega una fila al informe de [carpeta] para [paso] y
     * regenera `<carpeta>.pdf` con todos los pasos registrados hasta ahora.
     */
    fun capturar(rule: ComposeTestRule, carpeta: String, paso: String) {
        val directorio = directorio(carpeta)
        try {
            val archivo = File(directorio, "$paso.png")
            val bitmap: Bitmap = capturarBitmap(rule)
            FileOutputStream(archivo).use { salida -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, salida) }
            registrar(directorio, carpeta, paso, archivo, "Ok")
        } catch (e: Throwable) {
            // Si no hay árbol Compose visible (p.ej. tras cerrar la actividad) o la API no soporta
            // la captura (ver más abajo) se omite sin fallar la prueba: la evidencia es un
            // complemento, no una aserción.
            registrar(directorio, carpeta, paso, null, "Sin captura: ${e.message}")
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
            // La pantalla del dispositivo puede ir un cuadro por detrás del árbol Compose (tras
            // navegar, cerrar el teclado o recibir datos): se espera a que quede quieta.
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
            // Igual que la captura: un PDF fallido no debe hacer fallar la prueba.
        }
    }

    // Página A4 en puntos (1/72 de pulgada).
    private const val ANCHO = 595
    private const val ALTO = 842
    private const val MARGEN = 40f

    /** Portada con el dispositivo y la tabla de pasos, y luego una página por paso con su captura. */
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
                    // Media resolución basta para leer la captura y mantiene el PDF liviano.
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
