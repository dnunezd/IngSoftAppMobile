package com.example.vinilos.ui.detalle

import java.text.ParseException
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

fun formatearFechaLanzamiento(fechaIso: String): String {
    val utc = TimeZone.getTimeZone("UTC")
    val entrada = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply {
        timeZone = utc
        isLenient = false
    }
    val posicion = ParsePosition(0)
    val fecha = entrada.parse(fechaIso, posicion)
    if (fecha == null || posicion.index != fechaIso.length) {
        throw ParseException("Fecha ISO inválida: $fechaIso", posicion.errorIndex.coerceAtLeast(0))
    }
    return SimpleDateFormat("d 'de' MMMM 'de' yyyy", Locale("es")).apply {
        timeZone = utc
    }.format(fecha)
}
