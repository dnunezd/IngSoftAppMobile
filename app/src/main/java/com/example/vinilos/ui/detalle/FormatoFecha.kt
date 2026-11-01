package com.example.vinilos.ui.detalle

import java.text.ParseException
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

fun formatearFechaLanzamiento(fechaIso: String): String {
    val utc = TimeZone.getTimeZone("UTC")
    val timestamp = fechaIso.contains('T')
    val patron = when {
        !timestamp -> "yyyy-MM-dd"
        fechaIso.contains('.') -> "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
        else -> "yyyy-MM-dd'T'HH:mm:ssZ"
    }
    // RFC 822 evita el patrón XXX, no disponible en SimpleDateFormat de Android API 23.
    val texto = if (timestamp) {
        fechaIso.replace(Regex("Z$"), "+0000").replace(Regex("([+-]\\d{2}):(\\d{2})$"), "$1$2")
    } else fechaIso
    val entrada = SimpleDateFormat(patron, Locale.ROOT).apply {
        timeZone = utc
        isLenient = false
    }
    val posicion = ParsePosition(0)
    val fecha = entrada.parse(texto, posicion)
    if (fecha == null || posicion.index != texto.length) {
        throw ParseException("Fecha ISO inválida: $fechaIso", posicion.errorIndex.coerceAtLeast(0))
    }
    return SimpleDateFormat("d 'de' MMMM 'de' yyyy", Locale("es")).apply {
        timeZone = utc
    }.format(fecha)
}
