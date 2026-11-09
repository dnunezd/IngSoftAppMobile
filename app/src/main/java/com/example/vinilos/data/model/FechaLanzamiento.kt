package com.example.vinilos.data.model

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
