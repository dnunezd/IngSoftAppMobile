package com.example.vinilos

import com.example.vinilos.data.model.Album
import com.example.vinilos.data.model.AlbumNuevo
import com.example.vinilos.data.model.AlbumResumen
import com.example.vinilos.data.model.Cancion
import com.example.vinilos.data.remote.ErrorDeCarga
import com.example.vinilos.data.repository.AlbumRepository

/**
 * Repositorio de álbumes sin red para las pruebas de navegación: el detalle se resuelve por id y el
 * catálogo entrega las respuestas de [catalogo] en orden; la última se repite en las consultas
 * siguientes (así una prueba puede simular que al deslizar llega un álbum nuevo).
 */
class RepositorioAlbumesFalso(
    private val resultados: Map<Int, Result<Album>> = emptyMap(),
    catalogo: List<Result<List<AlbumResumen>>> = listOf(Result.success(emptyList()))
) : AlbumRepository {
    var consultas = 0
    var ultimoId: Int? = null
    var consultasCatalogo = 0
    val cancionesEnviadas = mutableListOf<Pair<Int, Cancion>>()
    var resultadoAgregarCancion: Result<Unit> = Result.failure(ErrorDeCarga())

    private val respuestasDetalle = resultados.toMutableMap()
    private val respuestasCatalogo = ArrayDeque(catalogo)

    override suspend fun obtenerAlbum(id: Int): Result<Album> {
        consultas++
        ultimoId = id
        return respuestasDetalle[id] ?: Result.failure(com.example.vinilos.data.repository.AlbumNoDisponible())
    }

    override suspend fun obtenerAlbumes(): Result<List<AlbumResumen>> {
        consultasCatalogo++
        return if (respuestasCatalogo.size > 1) respuestasCatalogo.removeFirst() else respuestasCatalogo.first()
    }

    /** Sin caché ni red, como cuando este falso heredaba del repositorio real sin DAO. */
    override suspend fun obtenerAlbumesGuardados(): List<AlbumResumen> = emptyList()
    override suspend fun obtenerAlbumGuardado(id: Int): Album? = null
    override suspend fun crearAlbum(album: AlbumNuevo): Result<Int> = Result.failure(ErrorDeCarga())
    override suspend fun agregarCancion(
        albumId: Int,
        cancion: com.example.vinilos.data.model.Cancion
    ): Result<Unit> {
        cancionesEnviadas += albumId to cancion
        return resultadoAgregarCancion.onSuccess {
            respuestasDetalle[albumId]?.getOrNull()?.let { album ->
                respuestasDetalle[albumId] = Result.success(album.copy(canciones = album.canciones + cancion))
            }
        }
    }
}
