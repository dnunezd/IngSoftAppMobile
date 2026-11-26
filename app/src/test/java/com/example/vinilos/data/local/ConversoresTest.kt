package com.example.vinilos.data.local

import com.example.vinilos.data.model.AlbumArtista
import com.example.vinilos.data.model.Artista
import com.example.vinilos.data.model.Track
import com.example.vinilos.data.model.Comment
import com.example.vinilos.data.model.IntegranteArtista
import com.example.vinilos.data.model.ArtistPrize
import org.junit.Assert.assertEquals
import org.junit.Test

/** Round-trip of every list that gets stored as JSON (add-cache-consultas, D2). */
class ConversoresTest {

    private val conversores = Conversores()

    @Test
    fun canciones() {
        val canciones = listOf(Track("Decisiones", "5:05"), Track("Buscando América", "7:40"))
        assertEquals(canciones, conversores.aTrackes(conversores.deTrackes(canciones)))
        assertEquals(emptyList<Track>(), conversores.aTrackes(conversores.deTrackes(emptyList())))
    }

    @Test
    fun leerDosVecesElMismoJsonDaListasIguales() {
        val json = conversores.dePrizes(listOf(ArtistPrize(7, "Grammy", "Academia", "1978-12-10T00:00:00.000Z")))

        val primera = conversores.aPrizes(json)
        val segunda = conversores.aPrizes(json)

        assertEquals(primera, segunda)
        assertEquals("Grammy", segunda.single().nombre)
    }

    @Test
    fun artistasDelAlbumConImagenNula() {
        val artistas = listOf(Artista("Rubén Blades", null), Artista("Queen", "https://img.example/q.jpg"))
        assertEquals(artistas, conversores.aArtistas(conversores.deArtistas(artistas)))
    }

    @Test
    fun comentariosConCalificacionNula() {
        val comentarios = listOf(Comment("Excelente", 5), Comment("Sin nota", null))
        assertEquals(comentarios, conversores.aComments(conversores.deComments(comentarios)))
    }

    @Test
    fun integrantes() {
        val integrantes = listOf(IntegranteArtista(1, "Freddie Mercury", null), IntegranteArtista(2, "Brian May", "x"))
        assertEquals(integrantes, conversores.aIntegrantes(conversores.deIntegrantes(integrantes)))
    }

    @Test
    fun albumesDelArtistaConPortadaYAnioNulos() {
        val albumes = listOf(AlbumArtista(100, "A Night at the Opera", null, null), AlbumArtista(101, "Jazz", "p", "1978"))
        assertEquals(albumes, conversores.aAlbumesArtista(conversores.deAlbumesArtista(albumes)))
    }

    @Test
    fun premiosConFechaNula() {
        val premios = listOf(ArtistPrize(7, "Grammy", "Academia", null), ArtistPrize(8, "Brit", "BPI", "1990-01-01T00:00:00.000Z"))
        assertEquals(premios, conversores.aPrizes(conversores.dePrizes(premios)))
    }

    @Test
    fun textoConTildesYComillasSeConserva() {
        val canciones = listOf(Track("Pedro Navaja \"versión\" ñ", "7:21"))
        assertEquals(canciones, conversores.aTrackes(conversores.deTrackes(canciones)))
    }
}
