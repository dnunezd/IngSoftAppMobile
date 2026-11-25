package com.example.vinilos.data.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale
import java.util.TimeZone

class FormatoFechaTest {

    @Test
    fun fechaCalendarioSePresentaEnEspanolSinCambiarElDia() {
        assertEquals("1 de agosto de 1984", formatearFechaLanzamiento("1984-08-01"))
    }

    @Test
    fun timestampUtcSePresentaConSuFechaDeLanzamiento() {
        assertEquals("1 de agosto de 1984", formatearFechaLanzamiento("1984-08-01T00:00:00.000Z"))
    }

    @Test
    fun fechaNoCambiaConLaZonaNiElIdiomaDelTelefono() {
        val zonaOriginal = TimeZone.getDefault()
        val idiomaOriginal = Locale.getDefault()
        try {
            Locale.setDefault(Locale.JAPAN)
            for (zona in listOf("America/Bogota", "Pacific/Honolulu", "Pacific/Kiritimati", "UTC")) {
                TimeZone.setDefault(TimeZone.getTimeZone(zona))
                assertEquals(zona, "1 de agosto de 1984", formatearFechaLanzamiento("1984-08-01"))
                assertEquals(
                    zona, "1 de agosto de 1984",
                    formatearFechaLanzamiento("1984-08-01T00:00:00.000Z")
                )
            }
        } finally {
            TimeZone.setDefault(zonaOriginal)
            Locale.setDefault(idiomaOriginal)
        }
    }

    @Test
    fun timestampConDesplazamientoSeInterpretaEnUtc() {
        assertEquals("1 de agosto de 1984", formatearFechaLanzamiento("1984-07-31T19:00:00-05:00"))
    }

    @Test
    fun laFechaLargaSigueElIdiomaDeLaApp() {
        assertEquals("August 1, 1984", formatearFechaLanzamiento("1984-08-01", java.util.Locale.ENGLISH))
        assertEquals("1 de agosto de 1984", formatearFechaLanzamiento("1984-08-01", Espanol))
    }

    @Test
    fun laFechaONulaFormateaEnElIdiomaYDescartaLasInvalidas() {
        assertEquals("1 de enero de 1970", formatearFechaONula("1970-01-01T00:00:00.000Z"))
        assertEquals("January 1, 1970", formatearFechaONula("1970-01-01T00:00:00.000Z", java.util.Locale.ENGLISH))
        assertEquals(null, formatearFechaONula("2020-99-99T00:00:00.000Z"))
        assertEquals(null, formatearFechaONula(null))
    }
}
