package com.example.vinilos.data.repository

import com.example.vinilos.data.model.ComentarioNuevo

/** Comentarios de los álbumes (HU09). La implementación es [ComentarioRepositoryImpl]. */
interface ComentarioRepository {

    /**
     * Publica el comentario en el álbum. Cualquier fallo (sin conexión, error del servidor, álbum o
     * coleccionista inexistente, datos inválidos) es un `ErrorDeCarga`: el backend responde 404 tanto
     * para el álbum como para el coleccionista, y la pantalla muestra el mismo mensaje para todos.
     */
    suspend fun publicarComentario(albumId: Int, comentario: ComentarioNuevo): Result<Unit>
}
