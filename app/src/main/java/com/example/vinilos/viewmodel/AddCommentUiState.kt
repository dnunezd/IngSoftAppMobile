package com.example.vinilos.viewmodel

import com.example.vinilos.data.model.Album

/** Maximum character count for the opinion, like the "57 / 400" counter in the mockup. */
const val LimiteOpinion = 400

/** The album being commented on (add-comentar-album, D3). */
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

/** Text for each rating: "Very bad (1/5)" to "Excellent (5/5)". */
enum class RatingLevel { MuyMalo, Malo, Regular, Bueno, Excelente }

/** Level of a rating from 1 to 5; `null` if there is no rating or it's out of range. */
fun nivelCalificacion(calificacion: Int?): RatingLevel? =
    calificacion?.let { RatingLevel.entries.getOrNull(it - 1) }

/**
 * State of the comment-on-album screen (add-comentar-album, D2). Like the create-album screen's, it
 * isn't sealed: what's written is preserved across all submission stages. [confirmarDescarte] opens the
 * "Discard comment?" dialog and [salir] tells the navigation to close the screen (D5).
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
