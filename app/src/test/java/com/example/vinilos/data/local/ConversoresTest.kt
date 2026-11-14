package com.example.vinilos.data.local

import com.example.vinilos.data.model.AlbumArtista
import com.example.vinilos.data.model.Artista
import com.example.vinilos.data.model.Cancion
import com.example.vinilos.data.model.Comentario
import com.example.vinilos.data.model.IntegranteArtista
import com.example.vinilos.data.model.PremioArtista
import org.junit.Assert.assertEquals
import org.junit.Test

/** Ida y vuelta de cada lista que se guarda como JSON (add-cache-consultas, D2). */
class ConversoresTest {

    private val conversores = Conversores()

    @Test
    fun canciones() {
        val canciones = listOf(Cancion("Decisiones", "5:05"), Cancion("Buscando América", "7:40"))
        assertEquals(canciones, conversores.aCanciones(conversores.deCanciones(canciones)))
        assertEquals(emptyList<Cancion>(), conversores.aCanciones(conversores.deCanciones(emptyList())))
    }

    @Test
    fun artistasDelAlbumConImagenNula() {
        val artistas = listOf(Artista("Rubén Blades", null), Artista("Queen", "https://img.example/q.jpg"))
        assertEquals(artistas, conversores.aArtistas(conversores.deArtistas(artistas)))
    }

    @Test
    fun comentariosConCalificacionNula() {
        val comentarios = listOf(Comentario("Excelente", 5), Comentario("Sin nota", null))
        assertEquals(comentarios, conversores.aComentarios(conversores.deComentarios(comentarios)))
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
        val premios = listOf(PremioArtista(7, "Grammy", "Academia", null), PremioArtista(8, "Brit", "BPI", "1 de enero de 1990"))
        assertEquals(premios, conversores.aPremios(conversores.dePremios(premios)))
    }

    @Test
    fun textoConTildesYComillasSeConserva() {
        val canciones = listOf(Cancion("Pedro Navaja \"versión\" ñ", "7:21"))
        assertEquals(canciones, conversores.aCanciones(conversores.deCanciones(canciones)))
    }
}
