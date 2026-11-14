package com.example.vinilos.viewmodel

/** Estados del botón de favorito en el detalle de artista (HU10). */
sealed interface FavoritoUiState {
    /** Comprobando si el artista ya es favorito del coleccionista: botón inactivo, sin texto de error. */
    data object Comprobando : FavoritoUiState

    /** Falló la comprobación inicial: botón inactivo, con opción de reintentar. */
    data object ErrorComprobacion : FavoritoUiState

    /** El artista no es favorito: botón "Agregar Favorito" activo. */
    data object NoFavorito : FavoritoUiState

    /** Guardando el favorito: botón "Guardando..." inactivo. */
    data object Guardando : FavoritoUiState

    /**
     * El artista ya es favorito: botón "Favorito" inactivo. [mostrarConfirmacion] sigue el mismo
     * patrón que `AlbumsUiState`/`avisoMostrado()`: la pantalla muestra el mensaje transitorio y
     * llama a [FavoritoViewModel.confirmacionMostrada] para apagarlo.
     */
    data class Favorito(val mostrarConfirmacion: Boolean) : FavoritoUiState

    /** Falló el guardado por red o servidor: mensaje de error, botón disponible para reintentar. */
    data object ErrorGuardado : FavoritoUiState

    /** El artista ya no existe en el backend: mensaje y botón para volver al listado. */
    data object NoDisponible : FavoritoUiState
}
