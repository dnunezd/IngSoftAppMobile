package com.example.vinilos.data.repository

import com.example.vinilos.data.model.ComentarioNuevo
import com.example.vinilos.data.remote.ColeccionistaRefDto
import com.example.vinilos.data.remote.ComentarioApi
import com.example.vinilos.data.remote.ComentarioNuevoDto
import com.example.vinilos.data.remote.ServiceAdapter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Publica comentarios con [ComentarioApi]. La respuesta se ignora: la pantalla de éxito muestra lo que
 * escribió el coleccionista y el detalle del álbum vuelve a consultarse (add-comentar-album, D1 y D7).
 */
class ComentarioRepositoryImpl(
    private val adaptador: ServiceAdapter,
    private val api: ComentarioApi = adaptador.api(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : ComentarioRepository {

    override suspend fun publicarComentario(albumId: Int, comentario: ComentarioNuevo): Result<Unit> =
        withContext(dispatcher) {
            adaptador.llamar {
                api.publicarComentario(albumId, comentario.aDto())
                Unit
            }
        }

    private fun ComentarioNuevo.aDto() = ComentarioNuevoDto(
        description = descripcion,
        rating = calificacion,
        collector = ColeccionistaRefDto(coleccionistaId)
    )
}
