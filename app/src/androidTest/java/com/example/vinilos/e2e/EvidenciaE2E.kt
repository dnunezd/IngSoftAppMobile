package com.example.vinilos.e2e

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
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
import org.junit.rules.TestWatcher
import org.junit.runner.Description
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
 * Con `connectedAndroidTest` (Gradle o Android Studio) se guarda en `additionalTestOutputDir`, que
 * AGP copia al PC, bajo `app/build/outputs/connected_android_test_additional_output/`, antes de
 * desinstalar la app. Con `am instrument`, que no pasa ese argumento, queda en el almacenamiento
 * propio de la app en el dispositivo (`.../files/e2e-evidencia/<carpeta>/`); si se trae al PC con
 * `adb pull`, queda bajo una carpeta `e2e-evidencia/` ignorada (ver `.gitignore`). En ningún caso
 * es rastreable por git.
 *
 * Cada carpeta muestra solo la ejecución actual: la primera captura de la carpeta en la ejecución
 * borra lo que dejaron las anteriores.
 */
object EvidenciaE2E {

    private const val CARPETA_BASE = "e2e-evidencia"
    private val formatoHora = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale("es"))

    private class Paso(val nombre: String, val estado: String, val captura: File?, val hora: String)

    /** Pasos registrados en esta ejecución, por carpeta. */
    private val pasos = mutableMapOf<String, MutableList<Paso>>()

    /** Carpetas con pasos nuevos desde la última vez que se generó su PDF. */
    private val pdfPendientes = mutableSetOf<String>()

    /**
     * Usa `additionalTestOutputDir` si AGP lo pasa y se puede escribir ahí; si no, el almacenamiento
     * propio de la app.
     */
    private fun directorio(carpeta: String): File {
        val deAgp = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
            ?.let { File(it, "$CARPETA_BASE/$carpeta") }
        if (deAgp != null && (deAgp.isDirectory || deAgp.mkdirs())) return deAgp
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        return File(contexto.getExternalFilesDir(null), "$CARPETA_BASE/$carpeta").apply { mkdirs() }
    }

    /**
     * Captura el árbol Compose visible y agrega una fila al informe de [carpeta] para [paso]. El PDF
     * lo genera [ReglaPdf] al terminar la prueba, así que la clase de prueba debe declararla.
     */
    fun capturar(rule: ComposeTestRule, carpeta: String, paso: String) {
        val directorio = directorio(carpeta)
        if (carpeta !in pasos) {
            directorio.listFiles()?.forEach { it.deleteRecursively() }
            pasos[carpeta] = mutableListOf()
        }
        try {
            val archivo = File(directorio, "$paso.png")
            val bitmap: Bitmap = capturarBitmap(rule)
            FileOutputStream(archivo).use { salida -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, salida) }
            registrar(directorio, carpeta, paso, archivo, "Ok")
        } catch (e: Throwable) {
            // Si no hay árbol Compose visible (p.ej. tras cerrar la actividad) o la API no soporta
            // la captura (ver más abajo) se omite sin fallar la prueba: la evidencia es un
            // complemento, no una aserción. El mensaje queda en una sola línea para no romper la
            // tabla del informe.
            registrar(directorio, carpeta, paso, null, "Sin captura: ${e.message}".replace(Regex("\\s+"), " "))
        }
    }

    /**
     * Regla JUnit que genera, al terminar cada prueba (pase o falle), el PDF de las carpetas en las
     * que capturó. Así el PDF se arma una vez por prueba y no en cada captura.
     */
    class ReglaPdf : TestWatcher() {
        override fun finished(description: Description) = generarPdfsPendientes()
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

    private fun registrar(directorio: File, carpeta: String, paso: String, captura: File?, estado: String) {
        val hora = formatoHora.format(Date())
        val informe = File(directorio, "informe.md")
        if (!informe.exists()) {
            informe.writeText("# Evidencia E2E\n\n| Paso | Estado | Captura | Hora |\n|---|---|---|---|\n")
        }
        informe.appendText("| $paso | $estado | ${captura?.name ?: "(sin captura)"} | $hora |\n")

        pasos.getValue(carpeta).add(Paso(paso, estado, captura, hora))
        pdfPendientes += carpeta
    }

    private fun generarPdfsPendientes() {
        pdfPendientes.forEach { carpeta ->
            try {
                generarPdf(File(directorio(carpeta), "$carpeta.pdf"), carpeta, pasos.getValue(carpeta))
            } catch (e: Throwable) {
                // Igual que la captura: un PDF fallido no debe hacer fallar la prueba.
            }
        }
        pdfPendientes.clear()
    }

    // Página A4 en puntos (1/72 de pulgada).
    private const val ANCHO = 595
    private const val ALTO = 842
    private const val MARGEN = 40f
    private const val INTERLINEA = 14f

    // Columnas de la tabla de la portada; el estado va al final para tener más espacio.
    private const val COLUMNA_HORA = 260f
    private const val COLUMNA_ESTADO = 385f

    /**
     * Portada con el dispositivo y la tabla de pasos (sigue en otra página si no cabe), y luego una
     * página por paso con su captura. El estado se muestra completo, en varias líneas si hace falta.
     */
    private fun generarPdf(destino: File, carpeta: String, lista: List<Paso>) {
        val titulo = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 18f; isFakeBoldText = true }
        val subtitulo = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 14f; isFakeBoldText = true }
        val texto = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 11f }
        val borde = Paint().apply { style = Paint.Style.STROKE; color = Color.LTGRAY }

        val pdf = PdfDocument()
        try {
            var numero = 1
            fun nuevaPagina() = pdf.startPage(PdfDocument.PageInfo.Builder(ANCHO, ALTO, numero++).create())
            fun encabezadoTabla(canvas: Canvas, y: Float) {
                canvas.drawText("Paso", MARGEN, y, subtitulo)
                canvas.drawText("Hora", COLUMNA_HORA, y, subtitulo)
                canvas.drawText("Estado", COLUMNA_ESTADO, y, subtitulo)
            }

            var portada = nuevaPagina()
            var y = MARGEN + 18f
            with(portada.canvas) {
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
                encabezadoTabla(this, y)
            }
            lista.forEach { paso ->
                val estado = lineas(paso.estado, texto, ANCHO - MARGEN - COLUMNA_ESTADO)
                if (y + 18f + (estado.size - 1) * INTERLINEA > ALTO - MARGEN) {
                    pdf.finishPage(portada)
                    portada = nuevaPagina()
                    y = MARGEN + 14f
                    encabezadoTabla(portada.canvas, y)
                }
                y += 18f
                with(portada.canvas) {
                    drawText(paso.nombre, MARGEN, y, texto)
                    drawText(paso.hora, COLUMNA_HORA, y, texto)
                    estado.forEachIndexed { i, linea -> drawText(linea, COLUMNA_ESTADO, y + i * INTERLINEA, texto) }
                }
                y += (estado.size - 1) * INTERLINEA
            }
            pdf.finishPage(portada)

            lista.forEach { paso ->
                val pagina = nuevaPagina()
                with(pagina.canvas) {
                    y = MARGEN + 14f
                    drawText(paso.nombre, MARGEN, y, subtitulo)
                    y += 4f
                    lineas("${paso.estado} · ${paso.hora}", texto, ANCHO - 2 * MARGEN).forEach { linea ->
                        y += INTERLINEA
                        drawText(linea, MARGEN, y, texto)
                    }
                    y += INTERLINEA
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

    /** Parte [texto] en líneas de hasta [ancho] puntos con [paint], cortando en espacios cuando se puede. */
    private fun lineas(texto: String, paint: Paint, ancho: Float): List<String> {
        val resultado = mutableListOf<String>()
        var resto = texto
        while (resto.isNotEmpty()) {
            var corte = paint.breakText(resto, true, ancho, null).coerceAtLeast(1)
            if (corte < resto.length) {
                val espacio = resto.lastIndexOf(' ', corte)
                if (espacio > 0) corte = espacio
            }
            resultado += resto.substring(0, corte).trimEnd()
            resto = resto.substring(corte).trimStart()
        }
        return resultado.ifEmpty { listOf("") }
    }
}
