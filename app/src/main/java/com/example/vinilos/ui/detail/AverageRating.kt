package com.example.vinilos.ui.detail

import com.example.vinilos.data.model.Comment
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.roundToInt
import com.example.vinilos.data.model.Espanol

fun promedioCalificaciones(comentarios: List<Comment>, idioma: Locale = Espanol): String? {
    val calificaciones = comentarios.mapNotNull { it.calificacion }
    if (calificaciones.isEmpty()) return null
    val promedio = calificaciones.average()
    return DecimalFormat("0.0", DecimalFormatSymbols(idioma)).apply {
        roundingMode = RoundingMode.HALF_UP
    }.format(promedio)
}

/** Rounds the already-formatted average (comma or dot, depending on the locale) to a whole number of stars. */
fun estrellasLlenas(promedio: String?): Int {
    if (promedio == null) return 0
    val valor = promedio.replace(',', '.').toDouble()
    return valor.roundToInt().coerceIn(0, 5)
}
