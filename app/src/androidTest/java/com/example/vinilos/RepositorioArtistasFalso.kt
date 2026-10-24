package com.example.vinilos

import com.example.vinilos.data.model.Artist
import com.example.vinilos.data.remote.ArtistApi
import com.example.vinilos.data.remote.BandDto
import com.example.vinilos.data.remote.MusicianDto
import com.example.vinilos.data.repository.ArtistRepository

/** Repositorio de artistas sin red para las pruebas de UI y de navegación. */
class RepositorioArtistasFalso(var resultado: Result<List<Artist>> = Result.success(emptyList())) : ArtistRepository(
    object : ArtistApi {
        override suspend fun getMusicians(): List<MusicianDto> = error("no se usa")
        override suspend fun getBands(): List<BandDto> = error("no se usa")
    }
) {
    var consultas = 0

    override suspend fun obtenerArtistas(): Result<List<Artist>> {
        consultas++
        return resultado
    }
}
