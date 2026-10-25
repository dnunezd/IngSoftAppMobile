package com.example.vinilos.ui.detalle

import com.example.vinilos.R
import com.example.vinilos.data.model.Album
import com.example.vinilos.data.model.Comentario
import org.junit.Assert.assertEquals
import org.junit.Test

class AlbumPresentacionTest {
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
    fun sinCancionesSeleccionaElMensajeDeVacio() {
        assertEquals(R.string.detalle_sin_canciones, presentarAlbum(album).mensajeCanciones)
    }

    @Test
    fun sinComentariosSeleccionaElMensajeDeVacio() {
        assertEquals(R.string.detalle_sin_comentarios, presentarAlbum(album).mensajeComentarios)
    }

    @Test
    fun entregaElPromedioDeLasCalificacionesDisponibles() {
        val conComentarios = album.copy(
            comentarios = listOf(Comentario("Bueno", 5), Comentario("Sin nota", null), Comentario("Bien", 4))
        )

        assertEquals("4,5", presentarAlbum(conComentarios).promedio)
    }
}
