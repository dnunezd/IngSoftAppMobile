package com.example.vinilos.data.model

import java.text.DateFormat
import java.text.ParseException
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

// The date picker returns UTC midnight of the chosen day. All these functions work in
// UTC so that day doesn't change with the phone's time zone. They don't use java.time because minSdk is 23.
private val Utc: TimeZone = TimeZone.getTimeZone("UTC")

/** App's default language (values/). The formats use it when no other is specified. */
val Espanol: Locale = Locale.forLanguageTag("es")

private fun formatear(patron: String, millis: Long): String =
    SimpleDateFormat(patron, Locale.ROOT).apply { timeZone = Utc }.format(Date(millis))

/** Date expected by the backend: `1969-09-26T00:00:00.000Z`. */
fun fechaIsoUtc(millis: Long): String = formatear("yyyy-MM-dd'T'00:00:00.000'Z'", millis)

/** Date the user sees in the form: `26/09/1969`. */
fun fechaVisible(millis: Long): String = formatear("dd/MM/yyyy", millis)

/**
 * UTC midnight of the phone's local day, to compare against the date picker's dates. Without this,
 * at 8 p.m. in Bogotá it would already be "tomorrow" and a future day could be selected.
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
 * Backend date (`1984-08-01` or `1984-08-01T00:00:00.000Z`) as the user sees it, in the app's [idioma]:
 * `1 de agosto de 1984` or `August 1, 1984`. Throws [ParseException] if it's not a valid ISO date.
 */
fun formatearReleaseDate(fechaIso: String, idioma: Locale = Espanol): String {
    val timestamp = fechaIso.contains('T')
    val patron = when {
        !timestamp -> "yyyy-MM-dd"
        fechaIso.contains('.') -> "yyyy-MM-dd'T'HH:mm:ss.SSSZ"
        else -> "yyyy-MM-dd'T'HH:mm:ssZ"
    }
    // RFC 822 avoids the XXX pattern, not available in Android API 23's SimpleDateFormat.
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
    return DateFormat.getDateInstance(DateFormat.LONG, idioma).apply {
        timeZone = Utc
    }.format(fecha)
}

/**
 * Like [formatearFechaLanzamiento], but a missing or unparseable date gives `null`. The
 * repository uses it to discard invalid dates and the screen uses it to display them in the app's language.
 */
fun formatearFechaONula(fechaIso: String?, idioma: Locale = Espanol): String? =
    fechaIso?.let {
        try {
            formatearReleaseDate(it, idioma)
        } catch (_: ParseException) {
            null
        }
    }
