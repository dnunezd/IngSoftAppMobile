package com.example.vinilos.ui.detalle

import com.example.vinilos.data.model.Comentario
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PromedioCalificacionesTest {

    @Test
    fun promedioAritmeticoSeRedondeaAUnDecimalEnEspanol() {
        val comentarios = listOf(
            Comentario("Primero", 5), Comentario("Segundo", 5), Comentario("Tercero", 4)
        )

        assertEquals("4,7", promedioCalificaciones(comentarios))
    }

    @Test
    fun sinComentariosNoExistePromedio() {
        assertNull(promedioCalificaciones(emptyList()))
    }
}
