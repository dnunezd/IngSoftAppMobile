package com.example.vinilos.viewmodel

/** Favorite button states in the artist detail screen (HU10). */
sealed interface FavoriteUiState {
    /** Checking whether the artist is already the collector's favorite: inactive button, no error text. */
    data object Comprobando : FavoriteUiState

    /** The initial check failed: inactive button, with a retry option. */
    data object ErrorComprobacion : FavoriteUiState

    /** The artist is not a favorite: active "Add Favorite" button. */
    data object NoFavorito : FavoriteUiState

    /** Saving the favorite: inactive "Saving..." button. */
    data object Guardando : FavoriteUiState

    /**
     * The artist is already a favorite: inactive "Favorite" button. [mostrarConfirmacion] follows the same
     * pattern as `AlbumsUiState`/`avisoMostrado()`: the screen shows the transient message and
     * calls [FavoritoViewModel.confirmacionMostrada] to turn it off.
     */
    data class Favorito(val mostrarConfirmacion: Boolean) : FavoriteUiState

    /** Saving failed due to network or server issues: error message, button available to retry. */
    data object ErrorGuardado : FavoriteUiState

    /** The artist no longer exists on the backend: message and button to return to the list. */
    data object NotAvailable : FavoriteUiState
}
