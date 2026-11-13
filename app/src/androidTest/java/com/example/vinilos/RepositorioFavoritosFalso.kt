package com.example.vinilos

import com.example.vinilos.data.model.ArtistType
import com.example.vinilos.data.repository.FavoriteRepository

/** Repositorio de favoritos sin red para las pruebas de navegación (HU10). */
class RepositorioFavoritosFalso(
    var esFavorito: Result<Boolean> = Result.success(false),
    var agregarResultado: Result<Unit> = Result.success(Unit),
    /** Favoritos confirmados guardados (add-cache-consultas); ninguno, sin caché, como antes. */
    var favoritosGuardados: Set<Pair<Int, Int>> = emptySet()
) : FavoriteRepository {
    var consultas = 0
    var guardados = 0

    override suspend fun esFavorito(collectorId: Int, artistId: Int): Result<Boolean> {
        consultas++
        return esFavorito
    }

    override suspend fun agregarFavorito(collectorId: Int, artistId: Int, tipo: ArtistType): Result<Unit> {
        guardados++
        return agregarResultado
    }

    override suspend fun esFavoritoGuardado(collectorId: Int, artistId: Int): Boolean =
        (collectorId to artistId) in favoritosGuardados
}
