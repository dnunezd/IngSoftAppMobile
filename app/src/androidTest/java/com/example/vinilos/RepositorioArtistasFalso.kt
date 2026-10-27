package com.example.vinilos

import com.example.vinilos.data.model.Artist
import com.example.vinilos.data.remote.ServiceAdapter
import com.example.vinilos.data.repository.ArtistRepository

/** Repositorio de artistas sin red para las pruebas de UI y de navegación. */
class RepositorioArtistasFalso(var resultado: Result<List<Artist>> = Result.success(emptyList())) :
    ArtistRepository(ServiceAdapter("http://localhost/")) {
    var consultas = 0

    override suspend fun obtenerArtistas(): Result<List<Artist>> {
        consultas++
        return resultado
    }
}
