package com.example.vinilos.data.model

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class FechaLanzamientoTest {

    private val zonaOriginal: TimeZone = TimeZone.getDefault()

    @After
    fun restaurarZona() {
        TimeZone.setDefault(zonaOriginal)
    }

    private fun medianocheUtc(anio: Int, mes: Int, dia: Int): Long =
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(anio, mes - 1, dia)
        }.timeInMillis

    private fun instanteUtc(anio: Int, mes: Int, dia: Int, hora: Int, minuto: Int): Long =
        medianocheUtc(anio, mes, dia) + (hora * 60L + minuto) * 60_000L

    private val zonas = listOf("America/Bogota", "UTC", "Asia/Tokyo", "Pacific/Kiritimati", "Pacific/Honolulu")

    @Test
    fun laFechaIsoEsElDiaElegidoEnCualquierZona() {
        for (zona in zonas) {
            TimeZone.setDefault(TimeZone.getTimeZone(zona))
            assertEquals(zona, "1969-09-26T00:00:00.000Z", fechaIsoUtc(medianocheUtc(1969, 9, 26)))
        }
    }

    @Test
    fun laFechaVisibleEsElDiaElegidoEnCualquierZona() {
        for (zona in zonas) {
            TimeZone.setDefault(TimeZone.getTimeZone(zona))
            assertEquals(zona, "26/09/1969", fechaVisible(medianocheUtc(1969, 9, 26)))
        }
    }

    @Test
    fun enBogotaDeNocheHoySigueSiendoElDiaLocalAunqueEnUtcSeaManana() {
        val ahora = instanteUtc(2026, 11, 6, 1, 0)

        assertEquals(
            medianocheUtc(2026, 11, 5),
            hoyEnUtc(ahora, TimeZone.getTimeZone("America/Bogota"))
        )
    }

    @Test
    fun enTokioDeMadrugadaHoyEsElDiaLocalAunqueEnUtcSeaAyer() {
        val ahora = instanteUtc(2026, 11, 5, 16, 30)

        assertEquals(
            medianocheUtc(2026, 11, 6),
            hoyEnUtc(ahora, TimeZone.getTimeZone("Asia/Tokyo"))
        )
    }

    @Test
    fun soloSePuedeElegirHastaElDiaDeHoy() {
        val hoy = medianocheUtc(2026, 11, 5)

        assertEquals(true, fechaSeleccionable(medianocheUtc(1969, 9, 26), hoy))
        assertEquals(true, fechaSeleccionable(hoy, hoy))
        assertEquals(false, fechaSeleccionable(medianocheUtc(2026, 11, 6), hoy))
    }

    @Test
    fun elAnioDeUnaFechaSeLeeEnUtc() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Bogota"))

        assertEquals(2026, anioUtc(medianocheUtc(2026, 1, 1)))
    }
}
