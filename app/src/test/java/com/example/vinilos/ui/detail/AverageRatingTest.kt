package com.example.vinilos.ui.detail

import com.example.vinilos.data.model.Comment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AverageRatingTest {

    @Test
    fun promedioAritmeticoSeRedondeaAUnDecimalEnEspanol() {
        val comentarios = listOf(
            Comment("Primero", 5), Comment("Segundo", 5), Comment("Tercero", 4)
        )

        assertEquals("4,7", promedioCalificaciones(comentarios))
    }

    @Test
    fun sinCommentsNoExistePromedio() {
        assertNull(promedioCalificaciones(emptyList()))
    }

    @Test
    fun comentariosSinCalificacionNoTienenPromedio() {
        assertNull(promedioCalificaciones(listOf(Comment("Sin nota", null))))
    }

    @Test
    fun unEmpateEnElRedondeoSubeAlDecimalSiguiente() {
        val comentarios = listOf(4, 4, 4, 5).map { Comment("Reseña", it) }

        assertEquals("4,3", promedioCalificaciones(comentarios))
    }

    @Test
    fun elPromedioSeRedondeaAEstrellasEnterasParaDibujarlas() {
        assertEquals(5, estrellasLlenas("4,7"))
        assertEquals(4, estrellasLlenas("4,3"))
        assertEquals(0, estrellasLlenas(null))
    }

    @Test
    fun elPromedioUsaElSeparadorDecimalDelIdioma() {
        val comentarios = listOf(
            com.example.vinilos.data.model.Comment("a", 4),
            com.example.vinilos.data.model.Comment("b", 5)
        )
        assertEquals("4,5", promedioCalificaciones(comentarios))
        assertEquals("4.5", promedioCalificaciones(comentarios, java.util.Locale.ENGLISH))
        assertEquals(5, estrellasLlenas("4.5"))
        assertEquals(5, estrellasLlenas("4,5"))
    }
}
