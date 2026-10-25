package com.example.vinilos.ui.detalle

import com.example.vinilos.data.model.Comentario
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

fun promedioCalificaciones(comentarios: List<Comentario>): String? {
    val calificaciones = comentarios.mapNotNull { it.calificacion }
    if (calificaciones.isEmpty()) return null
    val promedio = calificaciones.average()
    return DecimalFormat("0.0", DecimalFormatSymbols(Locale("es"))).apply {
        roundingMode = RoundingMode.HALF_UP
    }.format(promedio)
}
