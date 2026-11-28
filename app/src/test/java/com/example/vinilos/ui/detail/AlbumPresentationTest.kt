package com.example.vinilos.ui.detail

import com.example.vinilos.R
import com.example.vinilos.data.model.Album
import com.example.vinilos.data.model.Performer
import com.example.vinilos.data.model.Track
import com.example.vinilos.data.model.Comment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.lang.reflect.Modifier

class AlbumPresentationTest {
    private val album = Album(
        id = 100,
        nombre = "Buscando América",
        portada = null,
        descripcion = "Salsa",
        genero = "Salsa",
        disquera = "Elektra",
        fechaLanzamiento = "1984-08-01",
        canciones = emptyList(),
        artistas = emptyList(),
        comentarios = emptyList()
    )

    @Test
    fun entregaLaFechaFormateadaFueraDeCompose() {
        assertEquals("1 de agosto de 1984", presentarAlbum(album).fechaLanzamiento)
    }

    @Test
    fun sinTrackesSeleccionaElMensajeDeEmpty() {
        assertEquals(R.string.detalle_sin_canciones, presentarAlbum(album).mensajeTrackes)
    }

    @Test
    fun sinCommentsSeleccionaElMensajeDeEmpty() {
        assertEquals(R.string.detalle_sin_comentarios, presentarAlbum(album).mensajeComments)
    }

    @Test
    fun entregaElPromedioDeLasCalificacionesDisponibles() {
        val conComments = album.copy(
            comentarios = listOf(Comment("Bueno", 5), Comment("Sin nota", null), Comment("Bien", 4))
        )

        assertEquals("4,5", presentarAlbum(conComments).promedio)
    }

    @Test
    fun losCommentsPresentadosNoExponenIdentidadDeAutor() {
        val comentario = Comment("Gran disco", 5)
        val presentado = presentarAlbum(album.copy(comentarios = listOf(comentario)))
        val campos = presentado.album.comentarios.single().javaClass.declaredFields
            .filterNot { it.isSynthetic || Modifier.isStatic(it.modifiers) }
            .map { it.name }.toSet()

        assertEquals(setOf("descripcion", "calificacion"), campos)
        assertEquals(listOf(comentario), presentado.album.comentarios)
    }

    @Test
    fun albumCompletoConservaSusDatosSinMensajesDeEmpty() {
        val completo = album.copy(
            portada = "https://example.test/album.jpg",
            canciones = listOf(Track("Decisiones", "5:02")),
            artistas = listOf(Performer("Rubén Blades", "https://example.test/artista.jpg")),
            comentarios = listOf(Comment("Gran disco", 5))
        )

        val presentado = presentarAlbum(completo)

        assertEquals(completo, presentado.album)
        assertNull(presentado.mensajeTrackes)
        assertNull(presentado.mensajeComments)
    }
}
