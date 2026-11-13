package com.example.vinilos.data.model

import java.text.ParseException
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// El selector de fecha entrega la medianoche UTC del día elegido. Todas estas funciones trabajan en
// UTC para que ese día no cambie con la zona del teléfono. No usan java.time porque minSdk es 23.
private val Utc: TimeZone = TimeZone.getTimeZone("UTC")

private fun formatear(patron: String, millis: Long): String =
    SimpleDateFormat(patron, Locale.ROOT).apply { timeZone = Utc }.format(Date(millis))

/** Fecha que espera el backend: `1969-09-26T00:00:00.000Z`. */
fun fechaIsoUtc(millis: Long): String = formatear("yyyy-MM-dd'T'00:00:00.000'Z'", millis)

/** Fecha que ve el usuario en el formulario: `26/09/1969`. */
fun fechaVisible(millis: Long): String = formatear("dd/MM/yyyy", millis)

/**
 * Medianoche UTC del día local del teléfono, para compararla con las fechas del selector. Sin esto,
 * a las 8 p. m. en Bogotá ya sería "mañana" y se podría elegir un día futuro.
 */
fun hoyEnUtc(ahoraMillis: Long, zona: TimeZone): Long {
    val local = Calendar.getInstance(zona).apply { timeInMillis = ahoraMillis }
    return Calendar.getInstance(Utc).apply {
        clear()
        set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

fun fechaSeleccionable(fechaMillis: Long, hoyMillis: Long): Boolean = fechaMillis <= hoyMillis

fun anioUtc(millis: Long): Int =
    Calendar.getInstance(Utc).apply { timeInMillis = millis }.get(Calendar.YEAR)

/**
 * Fecha del backend (`1984-08-01` o `1984-08-01T00:00:00.000Z`) como la ve el usuario: `1 de agosto de 1984`.
 * Lanza [ParseException] si no es una fecha ISO válida.
 */
fun formatearFechaLanzamiento(fechaIso: String): String {
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
        timeZone = Utc
        isLenient = false
    }
    val posicion = ParsePosition(0)
    val fecha = entrada.parse(texto, posicion)
    if (fecha == null || posicion.index != texto.length) {
        throw ParseException("Fecha ISO inválida: $fechaIso", posicion.errorIndex.coerceAtLeast(0))
    }
    return SimpleDateFormat("d 'de' MMMM 'de' yyyy", Locale("es")).apply {
        timeZone = Utc
    }.format(fecha)
}
