package com.example.vinilos.data.remote

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CollectorDtoTest {

    private val gson = Gson()

    @Test
    fun deserializaSoloIdNombreYCorreoIgnorandoElDetalle() {
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

        val resultado = gson.fromJson(json, Array<CollectorDto>::class.java).toList()

        assertEquals(listOf(CollectorDto(100, "Manolo Bellon", "manollo@caracol.com.co")), resultado)
    }

    @Test
    fun uncampoAusenteLlegaComoNulo() {
        val json = """[{"id": 7, "name": "Sin correo"}]"""

        val resultado = gson.fromJson(json, Array<CollectorDto>::class.java).single()

        assertEquals(7, resultado.id)
        assertNull(resultado.email)
    }
}
