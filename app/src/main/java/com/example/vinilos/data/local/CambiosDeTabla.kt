package com.example.vinilos.data.local

/**
 * Qué escribir en una tabla de la caché (optimizar-cache-e-imagenes, D5 y D6).
 *
 * - Con [reemplazarTodo], se borra la tabla (o la parte que corresponda) y se insertan [guardar]: es lo que se
 *   hace cuando no se sabe qué hay guardado.
 * - Sin él, solo se guardan las filas nuevas o modificadas de [guardar] y se borran las claves de [borrar].
 */
data class CambiosDeTabla<K, E>(
    val reemplazarTodo: Boolean,
    val guardar: List<E>,
    val borrar: List<K> = emptyList()
) {
    /** No hay nada que escribir. */
    val vacios: Boolean get() = !reemplazarTodo && guardar.isEmpty() && borrar.isEmpty()

    companion object {
        fun <K, E> reemplazar(filas: List<E>) = CambiosDeTabla<K, E>(reemplazarTodo = true, guardar = filas)

        fun <K, E> diferencias(guardar: List<E>, borrar: List<K>) =
            CambiosDeTabla(reemplazarTodo = false, guardar = guardar, borrar = borrar)

        fun <K, E> ninguno() = CambiosDeTabla<K, E>(reemplazarTodo = false, guardar = emptyList())
    }
}

/**
 * SQLite admite como máximo 999 parámetros por consulta en las versiones de Android más antiguas (API 23), así
 * que los borrados por ids se hacen en grupos.
 */
internal const val TAMANO_GRUPO_BORRADO = 500

internal suspend fun <K> enGrupos(claves: List<K>, accion: suspend (List<K>) -> Unit) {
    claves.chunked(TAMANO_GRUPO_BORRADO).forEach { accion(it) }
}
