package com.example.vinilos.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Test

class TrackValidationTest {
    @Test
    fun camposEmptysSonObligatorios() {
        assertEquals(
            mapOf(
                TrackField.Nombre to TrackError.Obligatorio,
                TrackField.Duracion to TrackError.Obligatorio
            ),
            validarTrack("", "")
        )
    }

    @Test
    fun nombreConEspaciosEsObligatorio() {
        assertEquals(
            mapOf(TrackField.Nombre to TrackError.Obligatorio),
            validarTrack(" \t ", "5:05")
        )
    }

    @Test
    fun aceptaMinutosDeCualquierLongitudYSegundosEntreCeroYCincuentaYNueve() {
        listOf("0:00", "5:05", "12:34", "5:00", "5:59").forEach { duracion ->
            assertEquals("Duración válida: $duracion", emptyMap<TrackField, TrackError>(), validarTrack("Canción", duracion))
        }
    }

    @Test
    fun duracionesQueNoCumplenMssSonInvalidas() {
        listOf("5:60", "5:9", ":05", "5", "5:05:00", " 5:05", "5:05 ", "cinco:05").forEach { duracion ->
            assertEquals(
                "Duración inválida: $duracion",
                mapOf(TrackField.Duracion to TrackError.DuracionInvalida),
                validarTrack("Canción", duracion)
            )
        }
    }

    @Test
    fun duracionVaciaEsObligatoriaYNoErrorDeFormato() {
        assertEquals(
            mapOf(TrackField.Duracion to TrackError.Obligatorio),
            validarTrack("Canción", "")
        )
    }
}
