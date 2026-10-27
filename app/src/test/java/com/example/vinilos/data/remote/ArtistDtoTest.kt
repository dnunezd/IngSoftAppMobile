package com.example.vinilos.data.remote

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ArtistDtoTest {

    private val gson = Gson()

    @Test
    fun musicoDeserializaSoloIdNombreEImagenIgnorandoElDetalle() {
        val json = """
            [{
              "id": 100,
              "name": "Rubén Blades Bellido de Luna",
              "image": "https://img/ruben.jpg",
              "description": "Cantante panameño",
              "birthDate": "1948-07-16T00:00:00.000Z",
              "albums": [{"id": 100, "name": "Buscando América"}],
              "performerPrizes": [{"id": 100, "premiationDate": "1978-12-10T00:00:00.000Z"}]
            }]
        """.trimIndent()

        val resultado = gson.fromJson(json, Array<MusicianDto>::class.java).toList()

        assertEquals(listOf(MusicianDto(100, "Rubén Blades Bellido de Luna", "https://img/ruben.jpg")), resultado)
    }

    @Test
    fun bandaDeserializaSoloIdNombreEImagenIgnorandoElDetalle() {
        val json = """
            [{
              "id": 101,
              "name": "Queen",
              "image": "https://img/queen.jpg",
              "description": "Banda británica",
              "creationDate": "1970-01-01T00:00:00.000Z",
              "albums": [],
              "musicians": [{"id": 1, "name": "Freddie Mercury"}],
              "performerPrizes": []
            }]
        """.trimIndent()

        val resultado = gson.fromJson(json, Array<BandDto>::class.java).toList()

        assertEquals(listOf(BandDto(101, "Queen", "https://img/queen.jpg")), resultado)
    }

    @Test
    fun unaImagenAusenteLlegaComoNula() {
        val resultado = gson.fromJson("""[{"id": 7, "name": "Sin imagen"}]""", Array<MusicianDto>::class.java).single()

        assertNull(resultado.image)
    }
}
