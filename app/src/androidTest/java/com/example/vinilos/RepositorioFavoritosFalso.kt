package com.example.vinilos

import com.example.vinilos.data.model.ArtistType
import com.example.vinilos.data.repository.FavoriteRepository

/** Network-free favorite repository for navigation tests (HU10). */
class RepositorioFavoritosFalso(
    var esFavorito: Result<Boolean> = Result.success(false),
    var agregarResultado: Result<Unit> = Result.success(Unit),
    /** Saved confirmed favorites (add-cache-consultas); none, no cache, as before. */
    var favoritosGuardados: Set<Pair<Int, Int>> = emptySet()
) : FavoriteRepository {
    var consultas = 0
    var guardados = 0

    override suspend fun esFavorito(collectorId: Int, artistId: Int): Result<Boolean> {
        consultas++
        return esFavorito
    }

    override suspend fun addFavorite(collectorId: Int, artistId: Int, tipo: ArtistType): Result<Unit> {
        guardados++
        return agregarResultado
    }

    override suspend fun esFavoritoGuardado(collectorId: Int, artistId: Int): Boolean =
        (collectorId to artistId) in favoritosGuardados
}
