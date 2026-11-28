package com.example.vinilos.data.local

/**
 * What to write to a cache table (optimizar-cache-e-imagenes, D5 and D6).
 *
 * - With [reemplazarTodo], the table (or the corresponding part) is deleted and [guardar] is inserted: this is
 *   what happens when it's not known what is stored.
 * - Without it, only the new or modified rows from [guardar] are saved and the keys in [borrar] are deleted.
 */
data class TableChanges<K, E>(
    val reemplazarTodo: Boolean,
    val save: List<E>,
    val borrar: List<K> = emptyList()
) {
    /** There is nothing to write. */
    val vacios: Boolean get() = !reemplazarTodo && save.isEmpty() && borrar.isEmpty()

    companion object {
        fun <K, E> reemplazar(filas: List<E>) = TableChanges<K, E>(reemplazarTodo = true, save = filas)

        fun <K, E> diferencias(save: List<E>, borrar: List<K>) =
            TableChanges(reemplazarTodo = false, save = save, borrar = borrar)

        fun <K, E> ninguno() = TableChanges<K, E>(reemplazarTodo = false, save = emptyList())
    }
}

/**
 * SQLite supports at most 999 parameters per query on older Android versions (API 23), so
 * deletions by id are done in batches.
 */
internal const val TAMANO_GRUPO_BORRADO = 500

internal suspend fun <K> enGrupos(claves: List<K>, accion: suspend (List<K>) -> Unit) {
    claves.chunked(TAMANO_GRUPO_BORRADO).forEach { accion(it) }
}
