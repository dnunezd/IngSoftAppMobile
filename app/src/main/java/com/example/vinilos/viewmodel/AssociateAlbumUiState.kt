package com.example.vinilos.viewmodel

import com.example.vinilos.data.model.AlbumSummary

/** Catalog albums offered in the associate sheet (HU15). */
sealed interface ListadoAsociable {
    data object Loading : ListadoAsociable
    data class Listo(val albumes: List<AlbumSummary>) : ListadoAsociable
    data object Error : ListadoAsociable
}

/** Sheet to add an album to an artist (HU15, add-agregar-album-artista D2 and D4). */
data class AssociateAlbumUiState(
    val abierta: Boolean = false,
    val busqueda: String = "",
    val seleccionado: Int? = null,
    val asociando: Boolean = false,
    val errorAlAsociar: Boolean = false,
    /** Ids of the albums the artist already has: they're shown disabled with "Already associated". */
    val asociados: Set<Int> = emptySet(),
    val listado: ListadoAsociable = ListadoAsociable.Loading,
    /** Just-associated album, so the detail screen adds it and reports it; cleared with `asociadoNotificado()`. */
    val albumAsociado: AlbumSummary? = null
) {
    private val todos: List<AlbumSummary>
        get() = (listado as? ListadoAsociable.Listo)?.albumes.orEmpty()

    /**
     * Albums matching the search (accent- and case-insensitive), including already-associated ones. It's
     * computed once per state: the screen reads it several times on each recomposition.
     */
    val visibles: List<AlbumSummary> by lazy {
        val texto = normalizar(busqueda.trim())
        if (texto.isEmpty()) todos else todos.filter { normalizar(it.nombre).contains(texto) }
    }

    /** "No albums to associate": the catalog is empty or the artist already has them all. */
    val sinDisponibles: Boolean
        get() = listado is ListadoAsociable.Listo && todos.all { it.id in asociados }

    /** The search doesn't match any album. */
    val sinResultados: Boolean
        get() = listado is ListadoAsociable.Listo && busqueda.isNotBlank() && visibles.isEmpty()

    /** Only with a chosen album that's currently visible: one filtered out by the search isn't associated. */
    val puedeAsociar: Boolean
        get() = seleccionado != null && !asociando && visibles.any { it.id == seleccionado }

    /** The same state without the selection if the chosen album stopped being visible (reviewed in PR #114). */
    internal fun sinSeleccionOculta(): AssociateAlbumUiState =
        if (seleccionado != null && visibles.none { it.id == seleccionado }) copy(seleccionado = null) else this

    fun yaAsociado(id: Int): Boolean = id in asociados
}
