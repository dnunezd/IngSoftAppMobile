package com.example.vinilos.data.repository

import com.example.vinilos.data.local.TableChanges

/**
 * Copia en memoria de lo último que un repositorio escribió en una tabla de Room (optimizar-cache-e-imagenes,
 * D4). Sirve para saber qué cambió sin leer Room:
 *
 * - **Completa:** se conocen todas las filas de la tabla, así que se puede escribir solo la diferencia.
 * - **Parcial:** se conocen algunas filas (por ejemplo, los detalles abiertos). Al abrir la app no se conoce
 *   ninguna; una escritura de la tabla entera la reemplaza y la deja completa.
 *
 * No es segura entre hilos: el repositorio la usa siempre con su `Mutex` de escritura tomado.
 */
internal class CopiaTabla<K, E>(private val claveDe: (E) -> K) {

    private var filas: Map<K, E> = emptyMap()

    var completa: Boolean = false
        private set

    /** La fila guardada con esa clave, si se conoce. */
    fun fila(clave: K): E? = filas[clave]

    /**
     * Qué escribir para que la tabla entera quede igual a [nuevas]: con la copia completa, solo las filas nuevas
     * o distintas y las claves que ya no llegan; si no, reemplazar la tabla.
     */
    fun cambiosPara(nuevas: List<E>): TableChanges<K, E> {
        if (!completa) return TableChanges.reemplazar(nuevas)
        val clavesNuevas = nuevas.mapTo(HashSet(), claveDe)
        val save = nuevas.filter { filas[claveDe(it)] != it }
        val borrar = filas.keys.filter { it !in clavesNuevas }
        return TableChanges.diferencias(save, borrar)
    }

    /** Refleja unos cambios que ya quedaron escritos en Room. */
    fun aplicar(cambios: TableChanges<K, E>) {
        if (cambios.reemplazarTodo) {
            filas = cambios.save.associateBy(claveDe)
            completa = true
        } else {
            filas = filas - cambios.borrar.toSet() + cambios.save.associateBy(claveDe)
        }
    }

    /** Se escribió una sola fila; la copia sigue completa o parcial como estaba. */
    fun guardada(fila: E) {
        filas = filas + (claveDe(fila) to fila)
    }

    /** Se borró una sola fila. */
    fun borrada(clave: K) {
        filas = filas - clave
    }

    /** No se sabe qué quedó en la tabla: la siguiente escritura de la tabla entera la reemplaza. */
    fun descartar() {
        filas = emptyMap()
        completa = false
    }
}

/**
 * Versión (`ETag`) de la última respuesta escrita de cada consulta (optimizar-cache-e-imagenes, D5). Si una
 * respuesta llega con la misma versión, no hace falta compararla ni escribirla.
 */
internal class Versiones<K> {

    private val porConsulta = HashMap<K, String>()

    /** La respuesta tiene versión y es la misma de lo último que se escribió para esa consulta. */
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
 * Versión de algo que se arma con varias respuestas: solo sirve para comparar si todas sus partes tienen versión.
 */
internal fun versionCompuesta(vararg partes: String?): String? =
    if (partes.any { it == null }) null else partes.joinToString("|")
