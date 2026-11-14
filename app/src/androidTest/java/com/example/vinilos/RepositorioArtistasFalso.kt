package com.example.vinilos

import com.example.vinilos.data.model.Artist
import com.example.vinilos.data.model.ArtistType
import com.example.vinilos.data.model.ArtistaDetalle
import com.example.vinilos.data.remote.ErrorDeCarga
import com.example.vinilos.data.repository.ArtistRepository

/** Repositorio de artistas sin red para las pruebas de UI y de navegación. */
class RepositorioArtistasFalso(
    var resultado: Result<List<Artist>> = Result.success(emptyList()),
    var detalles: Map<Pair<ArtistType, Int>, Result<ArtistaDetalle>> = emptyMap(),
    /** Listado y detalles guardados (add-cache-consultas); vacíos, sin caché, como antes. */
    var guardados: List<Artist> = emptyList(),
    var detallesGuardados: Map<Pair<ArtistType, Int>, ArtistaDetalle> = emptyMap()
) : ArtistRepository {
    var consultas = 0
    val consultasDetalle = mutableListOf<Pair<ArtistType, Int>>()

    override suspend fun obtenerArtistas(): Result<List<Artist>> {
        consultas++
        return resultado
    }

    override suspend fun obtenerDetalle(tipo: ArtistType, id: Int): Result<ArtistaDetalle> {
        consultasDetalle += tipo to id
        return detalles[tipo to id] ?: Result.failure(ErrorDeCarga())
    }

    override suspend fun obtenerArtistasGuardados(): List<Artist> = guardados

    override suspend fun obtenerDetalleGuardado(tipo: ArtistType, id: Int): ArtistaDetalle? =
        detallesGuardados[tipo to id]
}
