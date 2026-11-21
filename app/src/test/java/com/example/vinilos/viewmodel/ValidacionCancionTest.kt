package com.example.vinilos.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Test

class ValidacionCancionTest {
    @Test
    fun camposVaciosSonObligatorios() {
        assertEquals(
            mapOf(
                CampoCancion.Nombre to ErrorCancion.Obligatorio,
                CampoCancion.Duracion to ErrorCancion.Obligatorio
            ),
            validarCancion("", "")
        )
    }

    @Test
    fun nombreConEspaciosEsObligatorio() {
        assertEquals(
            mapOf(CampoCancion.Nombre to ErrorCancion.Obligatorio),
            validarCancion(" \t ", "5:05")
        )
    }

    @Test
    fun aceptaMinutosDeCualquierLongitudYSegundosEntreCeroYCincuentaYNueve() {
        listOf("0:00", "5:05", "12:34", "5:00", "5:59").forEach { duracion ->
            assertEquals("Duración válida: $duracion", emptyMap<CampoCancion, ErrorCancion>(), validarCancion("Canción", duracion))
        }
    }

    @Test
    fun duracionesQueNoCumplenMssSonInvalidas() {
        listOf("5:60", "5:9", ":05", "5", "5:05:00", " 5:05", "5:05 ", "cinco:05").forEach { duracion ->
            assertEquals(
                "Duración inválida: $duracion",
                mapOf(CampoCancion.Duracion to ErrorCancion.DuracionInvalida),
                validarCancion("Canción", duracion)
            )
        }
    }

    @Test
    fun duracionVaciaEsObligatoriaYNoErrorDeFormato() {
        assertEquals(
            mapOf(CampoCancion.Duracion to ErrorCancion.Obligatorio),
            validarCancion("Canción", "")
        )
    }
}
