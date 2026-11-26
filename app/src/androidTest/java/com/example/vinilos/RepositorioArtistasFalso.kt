package com.example.vinilos

import com.example.vinilos.data.model.Artist
import com.example.vinilos.data.model.ArtistType
import com.example.vinilos.data.model.ArtistDetail
import com.example.vinilos.data.remote.LoadError
import com.example.vinilos.data.repository.ArtistRepository

/** Network-free artist repository for UI and navigation tests. */
class RepositorioArtistasFalso(
    var resultado: Result<List<Artist>> = Result.success(emptyList()),
    var detalles: Map<Pair<ArtistType, Int>, Result<ArtistDetail>> = emptyMap(),
    /** Saved list and details (add-cache-consultas); empty, no cache, as before. */
    var guardados: List<Artist> = emptyList(),
    var detallesGuardados: Map<Pair<ArtistType, Int>, ArtistDetail> = emptyMap(),
    /** Responses for associating an album (HU15), in order; the last one repeats. */
    asociaciones: List<Result<Unit>> = listOf(Result.success(Unit))
) : ArtistRepository {
    var consultas = 0
    val consultasDetalle = mutableListOf<Pair<ArtistType, Int>>()

    /** Each call to [asociarAlbum]: type, artist id, and album id. */
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
