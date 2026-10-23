package com.example.vinilos.ui.ingreso

import org.junit.Assert.assertEquals
import org.junit.Test

class InicialesTest {

    @Test
    fun usaLaPrimeraLetraDeLasDosPrimerasPalabras() {
        // Given
        val manolo = "Manolo Bellon"
        val jaime = "Jaime Monsalve"

        // When
        val inicialesManolo = iniciales(manolo)
        val inicialesJaime = iniciales(jaime)

        // Then
        assertEquals("MB", inicialesManolo)
        assertEquals("JM", inicialesJaime)
    }

    @Test
    fun conUnaSolaPalabraUsaUnaInicial() {
        // Given
        val nombre = "Manolo"

        // When
        val resultado = iniciales(nombre)

        // Then
        assertEquals("M", resultado)
    }

    @Test
    fun ignoraPalabrasDeMasYEspaciosSobrantes() {
        // Given
        val nombre = "  ana   beatriz  de la Cruz "

        // When
        val resultado = iniciales(nombre)

        // Then
        assertEquals("AB", resultado)
    }

    @Test
    fun devuelveMayusculas() {
        // Given
        val nombre = "josé pérez"

        // When
        val resultado = iniciales(nombre)

        // Then
        assertEquals("JP", resultado)
    }

    @Test
    fun conNombreVacioDevuelveSignoDeInterrogacion() {
        // Given
        val nombre = "   "

        // When
        val resultado = iniciales(nombre)

        // Then
        assertEquals("?", resultado)
    }
}
