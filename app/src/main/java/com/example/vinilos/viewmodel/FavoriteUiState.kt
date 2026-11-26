package com.example.vinilos.viewmodel

/** Estados del botón de favorito en el detalle de artista (HU10). */
sealed interface FavoriteUiState {
    /** Comprobando si el artista ya es favorito del coleccionista: botón inactivo, sin texto de error. */
    data object Comprobando : FavoriteUiState

    /** Falló la comprobación inicial: botón inactivo, con opción de reintentar. */
    data object ErrorComprobacion : FavoriteUiState

    /** El artista no es favorito: botón "Agregar Favorito" activo. */
    data object NoFavorito : FavoriteUiState

    /** Guardando el favorito: botón "Guardando..." inactivo. */
    data object Guardando : FavoriteUiState

    /**
     * El artista ya es favorito: botón "Favorito" inactivo. [mostrarConfirmacion] sigue el mismo
     * patrón que `AlbumsUiState`/`avisoMostrado()`: la pantalla muestra el mensaje transitorio y
     * llama a [FavoritoViewModel.confirmacionMostrada] para apagarlo.
     */
    data class Favorito(val mostrarConfirmacion: Boolean) : FavoriteUiState

    /** Falló el guardado por red o servidor: mensaje de error, botón disponible para reintentar. */
    data object ErrorGuardado : FavoriteUiState

    /** El artista ya no existe en el backend: mensaje y botón para volver al listado. */
    data object NotAvailable : FavoriteUiState
}
