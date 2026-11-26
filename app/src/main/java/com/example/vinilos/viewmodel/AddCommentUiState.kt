package com.example.vinilos.viewmodel

import com.example.vinilos.data.model.Album

/** Máximo de caracteres de la opinión, como el contador "57 / 400" del mockup. */
const val LimiteOpinion = 400

/** El álbum que se comenta (add-comentar-album, D3). */
sealed interface CargaAlbum {
    data object Loading : CargaAlbum
    data class Listo(val album: Album) : CargaAlbum
    data object Error : CargaAlbum
}

sealed interface EnvioComment {
    data object Editando : EnvioComment
    data object Enviando : EnvioComment
    data object Fallido : EnvioComment
    data object Publicado : EnvioComment
}

/** Texto de cada calificación: "Muy malo (1/5)" a "Excelente (5/5)". */
enum class RatingLevel { MuyMalo, Malo, Regular, Bueno, Excelente }

/** Nivel de una calificación de 1 a 5; `null` si no hay calificación o está fuera de rango. */
fun nivelCalificacion(calificacion: Int?): RatingLevel? =
    calificacion?.let { RatingLevel.entries.getOrNull(it - 1) }

/**
 * Estado de la pantalla de comentar un álbum (add-comentar-album, D2). Como el de crear álbum, no es
 * sellado: lo escrito se conserva en todas las etapas del envío. [confirmarDescarte] abre el diálogo
 * "¿Descartar comentario?" y [salir] le indica a la navegación que cierre la pantalla (D5).
 */
data class AddCommentUiState(
    val album: CargaAlbum = CargaAlbum.Loading,
    val calificacion: Int? = null,
    val opinion: String = "",
    val envio: EnvioComment = EnvioComment.Editando,
    val confirmarDescarte: Boolean = false,
    val salir: Boolean = false
) {
    val enviando: Boolean get() = envio == EnvioComment.Enviando

    val publicado: Boolean get() = envio == EnvioComment.Publicado

    val puedePublicar: Boolean
        get() = album is CargaAlbum.Listo && calificacion != null && opinion.isNotBlank() && !enviando && !publicado

    val tieneBorrador: Boolean get() = !publicado && (calificacion != null || opinion.isNotBlank())
}
