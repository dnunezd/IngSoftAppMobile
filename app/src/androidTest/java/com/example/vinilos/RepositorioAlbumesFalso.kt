package com.example.vinilos

import com.example.vinilos.data.model.Album
import com.example.vinilos.data.model.AlbumResumen
import com.example.vinilos.data.remote.ServiceAdapter
import com.example.vinilos.data.repository.AlbumRepository

/**
 * Repositorio de álbumes sin red para las pruebas de navegación: el detalle se resuelve por id y el
 * catálogo entrega las respuestas de [catalogo] en orden; la última se repite en las consultas
 * siguientes (así una prueba puede simular que al deslizar llega un álbum nuevo).
 */
class RepositorioAlbumesFalso(
    private val resultados: Map<Int, Result<Album>> = emptyMap(),
    catalogo: List<Result<List<AlbumResumen>>> = listOf(Result.success(emptyList()))
) : AlbumRepository(ServiceAdapter("http://localhost/")) {
    var consultas = 0
    var ultimoId: Int? = null
    var consultasCatalogo = 0

    private val respuestasCatalogo = ArrayDeque(catalogo)

    override suspend fun obtenerAlbum(id: Int): Result<Album> {
        consultas++
        ultimoId = id
        return resultados[id] ?: Result.failure(com.example.vinilos.data.repository.AlbumNoDisponible())
    }

    override suspend fun obtenerAlbumes(): Result<List<AlbumResumen>> {
        consultasCatalogo++
        return if (respuestasCatalogo.size > 1) respuestasCatalogo.removeFirst() else respuestasCatalogo.first()
    }
}
