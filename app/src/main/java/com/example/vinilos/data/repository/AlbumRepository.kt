package com.example.vinilos.data.repository

import com.example.vinilos.data.model.Album
import com.example.vinilos.data.remote.ServiceAdapter

open class AlbumRepository(private val adaptador: ServiceAdapter) {

    open suspend fun obtenerAlbum(id: Int): Result<Album> =
        Result.failure(UnsupportedOperationException("Consulta de álbum pendiente"))
}
