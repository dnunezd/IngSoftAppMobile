package com.example.vinilos.viewmodel

import com.example.vinilos.data.model.Album
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Rules for the form to comment on an album (HU09, add-comentar-album D2). */
class AddCommentUiStateTest {

    private val album = Album(
        id = 100, nombre = "Buscando América", portada = null, descripcion = "Salsa urbana", genero = "Salsa",
        disquera = "Elektra", fechaLanzamiento = "1984-08-01T00:00:00.000Z", canciones = emptyList(),
        artistas = emptyList(), comentarios = emptyList()
    )

    private val completo = AddCommentUiState(album = CargaAlbum.Listo(album), calificacion = 4, opinion = "Gran prensado")

    @Test
    fun conCalificacionYOpinionSePuedePublicar() {
        assertTrue(completo.puedePublicar)
    }

    @Test
    fun sinCalificacionNoSePuedePublicar() {
        assertFalse(completo.copy(calificacion = null).puedePublicar)
    }

    @Test
    fun sinOpinionOConSoloEspaciosNoSePuedePublicar() {
        assertFalse(completo.copy(opinion = "").puedePublicar)
        assertFalse(completo.copy(opinion = "   \n ").puedePublicar)
    }

    @Test
    fun sinElAlbumListoNoSePuedePublicar() {
        assertFalse(completo.copy(album = CargaAlbum.Loading).puedePublicar)
        assertFalse(completo.copy(album = CargaAlbum.Error).puedePublicar)
    }

    @Test
    fun duranteElEnvioOYaPublicadoNoSePuedePublicar() {
        assertFalse(completo.copy(envio = EnvioComment.Enviando).puedePublicar)
        assertFalse(completo.copy(envio = EnvioComment.Publicado).puedePublicar)
    }

    @Test
    fun despuesDeUnErrorSePuedeVolverAPublicar() {
        assertTrue(completo.copy(envio = EnvioComment.Fallido).puedePublicar)
    }

    @Test
    fun hayBorradorConCalificacionOConOpinion() {
        val vacio = AddCommentUiState(album = CargaAlbum.Listo(album))
        assertFalse(vacio.tieneBorrador)
        assertTrue(vacio.copy(calificacion = 2).tieneBorrador)
        assertTrue(vacio.copy(opinion = "Algo").tieneBorrador)
        assertFalse("Solo espacios no es un borrador", vacio.copy(opinion = "  ").tieneBorrador)
    }

    @Test
    fun loPublicadoYaNoEsUnBorrador() {
        assertFalse(completo.copy(envio = EnvioComment.Publicado).tieneBorrador)
    }

    @Test
    fun cadaCalificacionTieneSuNivel() {
        assertEquals(
            listOf(
                RatingLevel.MuyMalo, RatingLevel.Malo, RatingLevel.Regular,
                RatingLevel.Bueno, RatingLevel.Excelente
            ),
            (1..5).map { nivelCalificacion(it) }
        )
        assertNull(nivelCalificacion(null))
        assertNull(nivelCalificacion(0))
        assertNull(nivelCalificacion(6))
    }

    @Test
    fun elLimiteDeLaOpinionEsDe400Caracteres() {
        assertEquals(400, LimiteOpinion)
    }
}
