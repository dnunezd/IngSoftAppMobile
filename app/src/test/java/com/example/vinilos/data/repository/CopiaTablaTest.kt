package com.example.vinilos.data.repository

import com.example.vinilos.data.local.CambiosDeTabla
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Copia en memoria y versiones de lo escrito (optimizar-cache-e-imagenes, D4 y D5). */
class CopiaTablaTest {

    private data class Fila(val id: Int, val nombre: String)

    private fun copiaCompleta(vararg filas: Fila) = CopiaTabla<Int, Fila> { it.id }.apply {
        aplicar(CambiosDeTabla.reemplazar(filas.toList()))
    }

    @Test
    fun unaCopiaNuevaEsDesconocidaYPideReemplazarLaTabla() {
        val copia = CopiaTabla<Int, Fila> { it.id }

        assertFalse(copia.completa)
        assertEquals(CambiosDeTabla.reemplazar<Int, Fila>(listOf(Fila(1, "A"))), copia.cambiosPara(listOf(Fila(1, "A"))))
    }

    @Test
    fun reemplazarLaTablaDejaLaCopiaCompleta() {
        val copia = copiaCompleta(Fila(1, "A"))

        assertTrue(copia.completa)
        assertEquals(Fila(1, "A"), copia.fila(1))
    }

    @Test
    fun conLaCopiaCompletaSoloSeGuardanLasFilasNuevasOCambiadasYSeBorranLasQueNoLlegan() {
        val copia = copiaCompleta(Fila(1, "A"), Fila(2, "B"), Fila(3, "C"))

        val cambios = copia.cambiosPara(listOf(Fila(1, "A"), Fila(2, "B nuevo"), Fila(4, "D")))

        assertEquals(CambiosDeTabla.diferencias(listOf(Fila(2, "B nuevo"), Fila(4, "D")), listOf(3)), cambios)
    }

    @Test
    fun sinCambiosNoHayNadaQueEscribir() {
        val copia = copiaCompleta(Fila(1, "A"), Fila(2, "B"))

        assertTrue(copia.cambiosPara(listOf(Fila(2, "B"), Fila(1, "A"))).vacios)
    }

    @Test
    fun aplicarDiferenciasActualizaLaCopia() {
        val copia = copiaCompleta(Fila(1, "A"), Fila(2, "B"))

        copia.aplicar(CambiosDeTabla.diferencias(listOf(Fila(3, "C")), listOf(1)))

        assertNull(copia.fila(1))
        assertEquals(Fila(3, "C"), copia.fila(3))
        assertTrue(copia.cambiosPara(listOf(Fila(2, "B"), Fila(3, "C"))).vacios)
    }

    @Test
    fun unaCopiaParcialConoceSoloLasFilasEscritasUnaAUna() {
        val copia = CopiaTabla<Int, Fila> { it.id }

        copia.guardada(Fila(7, "G"))

        assertFalse(copia.completa)
        assertEquals(Fila(7, "G"), copia.fila(7))
        assertNull(copia.fila(8))
        assertTrue(copia.cambiosPara(listOf(Fila(7, "G"))).reemplazarTodo)
    }

    @Test
    fun descartarDejaLaCopiaDesconocida() {
        val copia = copiaCompleta(Fila(1, "A"))

        copia.descartar()

        assertFalse(copia.completa)
        assertNull(copia.fila(1))
    }

    @Test
    fun lasVersionesSoloCoincidenSiExistenYSonIguales() {
        val versiones = Versiones<String>()

        assertFalse(versiones.igual("catalogo", null))
        versiones.anotar("catalogo", "W/\"a\"")
        assertTrue(versiones.igual("catalogo", "W/\"a\""))
        assertFalse(versiones.igual("catalogo", "W/\"b\""))
        assertFalse(versiones.igual("catalogo", null))
        versiones.anotar("catalogo", null)
        assertFalse(versiones.igual("catalogo", "W/\"a\""))
    }

    @Test
    fun descartarUnaVersionNoTocaLasDemas() {
        val versiones = Versiones<Int>()
        versiones.anotar(1, "a")
        versiones.anotar(2, "b")

        versiones.descartar(1)

        assertFalse(versiones.igual(1, "a"))
        assertTrue(versiones.igual(2, "b"))
        versiones.descartarTodas()
        assertFalse(versiones.igual(2, "b"))
    }

    @Test
    fun unaVersionCompuestaSoloExisteSiTodasSusPartesTienenVersion() {
        assertEquals("a|b", versionCompuesta("a", "b"))
        assertNull(versionCompuesta("a", null))
    }
}
