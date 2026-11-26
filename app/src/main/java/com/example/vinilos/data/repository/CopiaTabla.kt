package com.example.vinilos.data.repository

import com.example.vinilos.data.local.TableChanges

/**
 * In-memory copy of the last thing a repository wrote to a Room table (optimizar-cache-e-imagenes,
 * D4). Used to know what changed without reading Room:
 *
 * - **Complete:** every row of the table is known, so only the difference needs to be written.
 * - **Partial:** only some rows are known (for example, the opened details). When the app opens, none
 *   is known; a write of the whole table replaces it and leaves it complete.
 *
 * It is not thread-safe: the repository always uses it with its write `Mutex` held.
 */
internal class CopiaTabla<K, E>(private val claveDe: (E) -> K) {

    private var filas: Map<K, E> = emptyMap()

    var completa: Boolean = false
        private set

    /** The stored row with that key, if known. */
    fun fila(clave: K): E? = filas[clave]

    /**
     * What to write so the whole table ends up equal to [nuevas]: with the complete copy, only the new
     * or different rows and the keys that no longer arrive; otherwise, replace the table.
     */
    fun cambiosPara(nuevas: List<E>): TableChanges<K, E> {
        if (!completa) return TableChanges.reemplazar(nuevas)
        val clavesNuevas = nuevas.mapTo(HashSet(), claveDe)
        val save = nuevas.filter { filas[claveDe(it)] != it }
        val borrar = filas.keys.filter { it !in clavesNuevas }
        return TableChanges.diferencias(save, borrar)
    }

    /** Reflects changes that have already been written to Room. */
    fun aplicar(cambios: TableChanges<K, E>) {
        if (cambios.reemplazarTodo) {
            filas = cambios.save.associateBy(claveDe)
            completa = true
        } else {
            filas = filas - cambios.borrar.toSet() + cambios.save.associateBy(claveDe)
        }
    }

    /** A single row was written; the copy remains complete or partial as it was. */
    fun guardada(fila: E) {
        filas = filas + (claveDe(fila) to fila)
    }

    /** A single row was deleted. */
    fun borrada(clave: K) {
        filas = filas - clave
    }

    /** It's unknown what ended up in the table: the next write of the whole table replaces it. */
    fun descartar() {
        filas = emptyMap()
        completa = false
    }
}

/**
 * Version (`ETag`) of the last written response of each query (optimizar-cache-e-imagenes, D5). If a
 * response arrives with the same version, there's no need to compare it or write it.
 */
internal class Versiones<K> {

    private val porConsulta = HashMap<K, String>()

    /** The response has a version and it's the same as the last one written for that query. */
    fun igual(consulta: K, version: String?): Boolean = version != null && porConsulta[consulta] == version

    fun anotar(consulta: K, version: String?) {
        if (version == null) porConsulta.remove(consulta) else porConsulta[consulta] = version
    }

    fun descartar(consulta: K) {
        porConsulta.remove(consulta)
    }

    fun descartarTodas() {
        porConsulta.clear()
    }
}

/**
 * Version of something assembled from several responses: it's only usable to compare if all of its parts have a version.
 */
internal fun versionCompuesta(vararg partes: String?): String? =
    if (partes.any { it == null }) null else partes.joinToString("|")
