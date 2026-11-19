package com.example.vinilos.viewmodel

import com.example.vinilos.data.model.Album

/** Máximo de caracteres de la opinión, como el contador "57 / 400" del mockup. */
const val LimiteOpinion = 400

/** El álbum que se comenta (add-comentar-album, D3). */
sealed interface CargaAlbum {
    data object Cargando : CargaAlbum
    data class Listo(val album: Album) : CargaAlbum
    data object Error : CargaAlbum
}

sealed interface EnvioComentario {
    data object Editando : EnvioComentario
    data object Enviando : EnvioComentario
    data object Fallido : EnvioComentario
    data object Publicado : EnvioComentario
}

/** Texto de cada calificación: "Muy malo (1/5)" a "Excelente (5/5)". */
enum class NivelCalificacion { MuyMalo, Malo, Regular, Bueno, Excelente }

/** Nivel de una calificación de 1 a 5; `null` si no hay calificación o está fuera de rango. */
fun nivelCalificacion(calificacion: Int?): NivelCalificacion? =
    calificacion?.let { NivelCalificacion.entries.getOrNull(it - 1) }

/**
 * Estado de la pantalla de comentar un álbum (add-comentar-album, D2). Como el de crear álbum, no es
 * sellado: lo escrito se conserva en todas las etapas del envío. [confirmarDescarte] abre el diálogo
 * "¿Descartar comentario?" y [salir] le indica a la navegación que cierre la pantalla (D5).
 */
data class ComentarAlbumUiState(
    val album: CargaAlbum = CargaAlbum.Cargando,
    val calificacion: Int? = null,
    val opinion: String = "",
    val envio: EnvioComentario = EnvioComentario.Editando,
    val confirmarDescarte: Boolean = false,
    val salir: Boolean = false
) {
    val enviando: Boolean get() = envio == EnvioComentario.Enviando

    val publicado: Boolean get() = envio == EnvioComentario.Publicado

    val puedePublicar: Boolean
        get() = album is CargaAlbum.Listo && calificacion != null && opinion.isNotBlank() && !enviando && !publicado

    val tieneBorrador: Boolean get() = !publicado && (calificacion != null || opinion.isNotBlank())
}
