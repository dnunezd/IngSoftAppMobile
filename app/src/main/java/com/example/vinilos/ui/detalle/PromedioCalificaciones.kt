package com.example.vinilos.ui.detalle

import com.example.vinilos.data.model.Comentario
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

fun promedioCalificaciones(comentarios: List<Comentario>): String? {
    val promedio = comentarios.mapNotNull { it.calificacion }.average()
    return DecimalFormat("0.0", DecimalFormatSymbols(Locale("es"))).apply {
        roundingMode = RoundingMode.HALF_UP
    }.format(promedio)
}
