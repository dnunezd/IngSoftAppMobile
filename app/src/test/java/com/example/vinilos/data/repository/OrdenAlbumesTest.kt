package com.example.vinilos.data.repository

import com.example.vinilos.data.model.AlbumResumen
import org.junit.Assert.assertEquals
import org.junit.Test

class OrdenAlbumesTest {

    private fun album(id: Int, nombre: String) =
        AlbumResumen(id = id, nombre = nombre, portada = null, genero = "Rock", artista = "", anio = null)

    private fun ordenar(vararg albumes: AlbumResumen) = albumes.sortedWith(ordenCatalogo)

    @Test
    fun losAlbumesDeEjemploQuedanEnOrdenAlfabetico() {
        val ordenados = ordenar(
            album(100, "Buscando América"),
            album(101, "Poeta del pueblo"),
            album(102, "A Night at the Opera"),
            album(103, "A Day at the Races")
        )

        assertEquals(
            listOf("A Day at the Races", "A Night at the Opera", "Buscando América", "Poeta del pueblo"),
            ordenados.map { it.nombre }
        )
    }

    @Test
    fun elOrdenNoDistingueMayusculasNiTildes() {
        val ordenados = ordenar(
            album(1, "Óleo"),
            album(2, "álbum blanco"),
            album(3, "Zafiro"),
            album(4, "Album azul"),
            album(5, "oasis")
        )

        assertEquals(
            listOf("Album azul", "álbum blanco", "oasis", "Óleo", "Zafiro"),
            ordenados.map { it.nombre }
        )
    }

    @Test
    fun losNombresEquivalentesSeDesempatanPorId() {
        val ordenados = ordenar(album(7, "Ébano"), album(2, "ebano"), album(5, "EBANO"))

        assertEquals(listOf(2, 5, 7), ordenados.map { it.id })
    }
}
