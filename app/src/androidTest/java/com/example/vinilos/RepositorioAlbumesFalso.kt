package com.example.vinilos

import com.example.vinilos.data.model.Album
import com.example.vinilos.data.model.NewAlbum
import com.example.vinilos.data.model.AlbumSummary
import com.example.vinilos.data.model.Track
import com.example.vinilos.data.remote.LoadError
import com.example.vinilos.data.repository.AlbumRepository

/**
 * Repositorio de álbumes sin red para las pruebas de navegación: el detalle se resuelve por id y el
 * catálogo entrega las respuestas de [catalogo] en orden; la última se repite en las consultas
 * siguientes (así una prueba puede simular que al deslizar llega un álbum nuevo).
 */
class RepositorioAlbumesFalso(
    private val resultados: Map<Int, Result<Album>> = emptyMap(),
    catalogo: List<Result<List<AlbumSummary>>> = listOf(Result.success(emptyList()))
) : AlbumRepository {
    var consultas = 0
    var ultimoId: Int? = null
    var consultasCatalogo = 0
    val cancionesEnviadas = mutableListOf<Pair<Int, Track>>()
    var resultadoAddTrack: Result<Unit> = Result.failure(LoadError())

    private val respuestasDetalle = resultados.toMutableMap()
    private val respuestasCatalogo = ArrayDeque(catalogo)

    override suspend fun getAlbum(id: Int): Result<Album> {
        consultas++
        ultimoId = id
        return respuestasDetalle[id] ?: Result.failure(com.example.vinilos.data.repository.AlbumNotAvailable())
    }

    override suspend fun getAlbums(): Result<List<AlbumSummary>> {
        consultasCatalogo++
        return if (respuestasCatalogo.size > 1) respuestasCatalogo.removeFirst() else respuestasCatalogo.first()
    }

    /** Sin caché ni red, como cuando este falso heredaba del repositorio real sin DAO. */
    override suspend fun getCachedAlbums(): List<AlbumSummary> = emptyList()
    override suspend fun getCachedAlbum(id: Int): Album? = null
    override suspend fun createAlbum(album: NewAlbum): Result<Int> = Result.failure(LoadError())
    override suspend fun addTrack(
        albumId: Int,
        cancion: com.example.vinilos.data.model.Track
    ): Result<Unit> {
        cancionesEnviadas += albumId to cancion
        return resultadoAddTrack.onSuccess {
            respuestasDetalle[albumId]?.getOrNull()?.let { album ->
                respuestasDetalle[albumId] = Result.success(album.copy(canciones = album.canciones + cancion))
            }
        }
    }
}
