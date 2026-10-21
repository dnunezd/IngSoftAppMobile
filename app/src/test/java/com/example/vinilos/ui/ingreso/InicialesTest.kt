package com.example.vinilos.ui.ingreso

import org.junit.Assert.assertEquals
import org.junit.Test

class InicialesTest {

    @Test
    fun usaLaPrimeraLetraDeLasDosPrimerasPalabras() {
        assertEquals("MB", iniciales("Manolo Bellon"))
        assertEquals("JM", iniciales("Jaime Monsalve"))
    }

    @Test
    fun conUnaSolaPalabraUsaUnaInicial() {
        assertEquals("M", iniciales("Manolo"))
    }

    @Test
    fun ignoraPalabrasDeMasYEspaciosSobrantes() {
        assertEquals("AB", iniciales("  ana   beatriz  de la Cruz "))
    }

    @Test
    fun devuelveMayusculas() {
        assertEquals("JP", iniciales("josé pérez"))
    }

    @Test
    fun conNombreVacioDevuelveSignoDeInterrogacion() {
        assertEquals("?", iniciales("   "))
    }
}
