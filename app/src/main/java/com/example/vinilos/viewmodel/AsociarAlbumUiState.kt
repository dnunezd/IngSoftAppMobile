package com.example.vinilos.viewmodel

import com.example.vinilos.data.model.AlbumResumen

/** Álbumes del catálogo que se ofrecen en la ventana de asociar (HU15). */
sealed interface ListadoAsociable {
    data object Cargando : ListadoAsociable
    data class Listo(val albumes: List<AlbumResumen>) : ListadoAsociable
    data object Error : ListadoAsociable
}

/** Ventana para agregar un álbum a un artista (HU15, add-agregar-album-artista D2 y D4). */
data class AsociarAlbumUiState(
    val abierta: Boolean = false,
    val busqueda: String = "",
    val seleccionado: Int? = null,
    val asociando: Boolean = false,
    val errorAlAsociar: Boolean = false,
    /** Ids de los álbumes que el artista ya tiene: se ven deshabilitados con "Ya asociado". */
    val asociados: Set<Int> = emptySet(),
    val listado: ListadoAsociable = ListadoAsociable.Cargando,
    /** Álbum recién asociado, para que el detalle lo agregue y avise; se limpia con `asociadoNotificado()`. */
    val albumAsociado: AlbumResumen? = null
) {
    private val todos: List<AlbumResumen>
        get() = (listado as? ListadoAsociable.Listo)?.albumes.orEmpty()

    /**
     * Álbumes que coinciden con la búsqueda (sin tildes ni mayúsculas), incluidos los ya asociados. Se calcula
     * una sola vez por estado: la pantalla lo lee varias veces en cada recomposición.
     */
    val visibles: List<AlbumResumen> by lazy {
        val texto = normalizar(busqueda.trim())
        if (texto.isEmpty()) todos else todos.filter { normalizar(it.nombre).contains(texto) }
    }

    /** "No hay álbumes para asociar": el catálogo está vacío o el artista ya los tiene todos. */
    val sinDisponibles: Boolean
        get() = listado is ListadoAsociable.Listo && todos.all { it.id in asociados }

    /** La búsqueda no coincide con ningún álbum. */
    val sinResultados: Boolean
        get() = listado is ListadoAsociable.Listo && busqueda.isNotBlank() && visibles.isEmpty()

    /** Solo con un álbum elegido que se esté viendo: no se asocia uno que la búsqueda dejó fuera. */
    val puedeAsociar: Boolean
        get() = seleccionado != null && !asociando && visibles.any { it.id == seleccionado }

    /** El mismo estado sin la selección si el álbum elegido dejó de verse (revisión del PR #114). */
    internal fun sinSeleccionOculta(): AsociarAlbumUiState =
        if (seleccionado != null && visibles.none { it.id == seleccionado }) copy(seleccionado = null) else this

    fun yaAsociado(id: Int): Boolean = id in asociados
}
