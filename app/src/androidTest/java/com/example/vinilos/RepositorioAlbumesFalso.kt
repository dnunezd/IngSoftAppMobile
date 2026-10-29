package com.example.vinilos

import com.example.vinilos.data.model.Album
import com.example.vinilos.data.remote.ServiceAdapter
import com.example.vinilos.data.repository.AlbumRepository

/** Repositorio de álbumes sin red para las pruebas de navegación: resuelve por id. */
class RepositorioAlbumesFalso(private val resultados: Map<Int, Result<Album>> = emptyMap()) :
    AlbumRepository(ServiceAdapter("http://localhost/")) {
    var consultas = 0
    var ultimoId: Int? = null

    override suspend fun obtenerAlbum(id: Int): Result<Album> {
        consultas++
        ultimoId = id
        return resultados[id] ?: Result.failure(com.example.vinilos.data.repository.AlbumNoDisponible())
    }
}
