package com.example.vinilos

import com.example.vinilos.data.model.Artist
import com.example.vinilos.data.model.ArtistType
import com.example.vinilos.data.model.ArtistDetail
import com.example.vinilos.data.remote.LoadError
import com.example.vinilos.data.repository.ArtistRepository

/** Repositorio de artistas sin red para las pruebas de UI y de navegación. */
class RepositorioArtistasFalso(
    var resultado: Result<List<Artist>> = Result.success(emptyList()),
    var detalles: Map<Pair<ArtistType, Int>, Result<ArtistDetail>> = emptyMap(),
    /** Listado y detalles guardados (add-cache-consultas); vacíos, sin caché, como antes. */
    var guardados: List<Artist> = emptyList(),
    var detallesGuardados: Map<Pair<ArtistType, Int>, ArtistDetail> = emptyMap(),
    /** Respuestas de asociar un álbum (HU15), en orden; la última se repite. */
    asociaciones: List<Result<Unit>> = listOf(Result.success(Unit))
) : ArtistRepository {
    var consultas = 0
    val consultasDetalle = mutableListOf<Pair<ArtistType, Int>>()

    /** Cada llamada a [asociarAlbum]: tipo, id del artista e id del álbum. */
    val asociacionesPedidas = mutableListOf<Triple<ArtistType, Int, Int>>()
    private val respuestasAsociar = ArrayDeque(asociaciones)

    override suspend fun getArtists(): Result<List<Artist>> {
        consultas++
        return resultado
    }

    override suspend fun getDetail(tipo: ArtistType, id: Int): Result<ArtistDetail> {
        consultasDetalle += tipo to id
        return detalles[tipo to id] ?: Result.failure(LoadError())
    }

    override suspend fun getCachedArtists(): List<Artist> = guardados

    override suspend fun getCachedDetail(tipo: ArtistType, id: Int): ArtistDetail? =
        detallesGuardados[tipo to id]

    override suspend fun associateAlbum(tipo: ArtistType, idArtista: Int, idAlbum: Int): Result<Unit> {
        asociacionesPedidas += Triple(tipo, idArtista, idAlbum)
        return if (respuestasAsociar.size > 1) respuestasAsociar.removeFirst() else respuestasAsociar.first()
    }
}
