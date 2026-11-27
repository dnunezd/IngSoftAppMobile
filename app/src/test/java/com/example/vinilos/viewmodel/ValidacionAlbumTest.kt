package com.example.vinilos.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidacionAlbumTest {

    private val completo = CreateAlbumUiState(
        nombre = "Abbey Road",
        portada = "https://example.com/abbey.jpg",
        descripcion = "Undécimo álbum",
        fechaMillis = -8_380_800_000L,
        genero = "Rock",
        disquera = "EMI"
    )

    @Test
    fun formularioEmptyMarcaLosSeisCamposComoObligatorios() {
        assertEquals(
            AlbumField.entries.associateWith { FieldError.Obligatorio },
            validarAlbum(CreateAlbumUiState())
        )
    }

    @Test
    fun soloEspaciosCuentaComoCampoEmpty() {
        val errores = validarAlbum(completo.copy(nombre = "   ", descripcion = "\n "))

        assertEquals(
            mapOf(
                AlbumField.Nombre to FieldError.Obligatorio,
                AlbumField.Descripcion to FieldError.Obligatorio
            ),
            errores
        )
    }

    @Test
    fun portadaConOtroProtocoloEsUrlInvalida() {
        assertEquals(
            mapOf(AlbumField.Portada to FieldError.UrlInvalida),
            validarAlbum(completo.copy(portada = "ftp://servidor/portada.png"))
        )
    }

    @Test
    fun portadaVaciaEsObligatoriaYNoUrlInvalida() {
        assertEquals(
            mapOf(AlbumField.Portada to FieldError.Obligatorio),
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

    @Test
    fun laPortadaDebeTenerDominio() {
        for (portada in listOf("https://", "http://", "HTTPS://", "https:// ", "https:///b.png", "https://?a=1", "https://#a", "https://:8080", "https://.")) {
            assertFalse(portada, portadaValida(portada))
        }
    }

    @Test
    fun elDominioAdmitePuertoRutaYConsulta() {
        for (portada in listOf("https://a.co", "http://localhost:3000/b.png", "https://a.co?x=1", "https://a.co/b c.png", "https://música.co/b.png")) {
            assertTrue(portada, portadaValida(portada))
        }
    }
}
