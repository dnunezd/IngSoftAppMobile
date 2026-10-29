package com.example.vinilos.ui.detalle

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
}
