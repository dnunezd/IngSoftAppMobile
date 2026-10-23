package com.example.vinilos.data.remote

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CollectorDtoTest {

    private val gson = Gson()

    @Test
    fun deserializaSoloIdNombreYCorreoIgnorandoElDetalle() {
        // Given
        val json = """
            [{
              "id": 100,
              "name": "Manolo Bellon",
              "telephone": "3502457896",
              "email": "manollo@caracol.com.co",
              "comments": [{"id": 1, "description": "Genial", "rating": 5}],
              "favoritePerformers": [{"id": 100, "name": "Rubén Blades"}],
              "collectorAlbums": [{"id": 1, "price": 35, "status": "Active"}]
            }]
        """.trimIndent()

        // When
        val resultado = gson.fromJson(json, Array<CollectorDto>::class.java).toList()

        // Then
        assertEquals(listOf(CollectorDto(100, "Manolo Bellon", "manollo@caracol.com.co")), resultado)
    }

    @Test
    fun uncampoAusenteLlegaComoNulo() {
        // Given
        val json = """[{"id": 7, "name": "Sin correo"}]"""

        // When
        val resultado = gson.fromJson(json, Array<CollectorDto>::class.java).single()

        // Then
        assertEquals(7, resultado.id)
        assertNull(resultado.email)
    }
}
