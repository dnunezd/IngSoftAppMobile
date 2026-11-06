package com.example.vinilos.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidacionAlbumTest {

    private val completo = CrearAlbumUiState(
        nombre = "Abbey Road",
        portada = "https://example.com/abbey.jpg",
        descripcion = "Undécimo álbum",
        fechaMillis = -8_380_800_000L,
        genero = "Rock",
        disquera = "EMI"
    )

    @Test
    fun formularioVacioMarcaLosSeisCamposComoObligatorios() {
        assertEquals(
            CampoAlbum.entries.associateWith { ErrorCampo.Obligatorio },
            validarAlbum(CrearAlbumUiState())
        )
    }

    @Test
    fun soloEspaciosCuentaComoCampoVacio() {
        val errores = validarAlbum(completo.copy(nombre = "   ", descripcion = "\n "))

        assertEquals(
            mapOf(
                CampoAlbum.Nombre to ErrorCampo.Obligatorio,
                CampoAlbum.Descripcion to ErrorCampo.Obligatorio
            ),
            errores
        )
    }

    @Test
    fun portadaConOtroProtocoloEsUrlInvalida() {
        assertEquals(
            mapOf(CampoAlbum.Portada to ErrorCampo.UrlInvalida),
            validarAlbum(completo.copy(portada = "ftp://servidor/portada.png"))
        )
    }

    @Test
    fun portadaVaciaEsObligatoriaYNoUrlInvalida() {
        assertEquals(
            mapOf(CampoAlbum.Portada to ErrorCampo.Obligatorio),
            validarAlbum(completo.copy(portada = " "))
        )
    }

    @Test
    fun formularioCompletoNoTieneErrores() {
        assertTrue(validarAlbum(completo).isEmpty())
    }

    @Test
    fun losPrefijosValidosNoDistinguenMayusculas() {
        for (portada in listOf("http://a.co/b.png", "https://a.co/b.png", "HTTPS://a.co/b.png", "  https://a.co/b.png")) {
            assertTrue(portada, portadaValida(portada))
        }
    }

    @Test
    fun otrosTextosNoSonPortadasValidas() {
        for (portada in listOf("", "ftp://a.co", "www.a.co/b.png", "httpx://a.co")) {
            assertFalse(portada, portadaValida(portada))
        }
    }
}
