package com.example.vinilos.ui.detalle

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatoFechaTest {

    @Test
    fun fechaCalendarioSePresentaEnEspanolSinCambiarElDia() {
        assertEquals("1 de agosto de 1984", formatearFechaLanzamiento("1984-08-01"))
    }
}
