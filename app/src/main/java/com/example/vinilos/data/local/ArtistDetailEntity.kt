package com.example.vinilos.data.local

import androidx.room.Entity
import com.example.vinilos.data.model.AlbumArtista
import com.example.vinilos.data.model.ArtistType
import com.example.vinilos.data.model.IntegranteArtista
import com.example.vinilos.data.model.ArtistPrize

/** Detail of an artist the user opened (HU04), with the same fields as `ArtistaDetalle`. */
@Entity(tableName = "artista_detalle", primaryKeys = ["tipo", "id"])
data class ArtistDetailEntity(
    val tipo: ArtistType,
    val id: Int,
    val nombre: String,
    val imagen: String?,
    val descripcion: String?,
    val fecha: String?,
    val integrantes: List<IntegranteArtista>,
    val albumes: List<AlbumArtista>,
    val premios: List<ArtistPrize>
)
