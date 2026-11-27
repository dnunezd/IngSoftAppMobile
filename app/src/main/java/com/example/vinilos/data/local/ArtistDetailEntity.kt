package com.example.vinilos.data.local

import androidx.room.Entity
import com.example.vinilos.data.model.ArtistAlbum
import com.example.vinilos.data.model.ArtistType
import com.example.vinilos.data.model.ArtistMember
import com.example.vinilos.data.model.ArtistPrize

/** Detail of an artist the user opened (HU04), with the same fields as `ArtistDetail`. */
@Entity(tableName = "artista_detalle", primaryKeys = ["tipo", "id"])
data class ArtistDetailEntity(
    val tipo: ArtistType,
    val id: Int,
    val nombre: String,
    val imagen: String?,
    val descripcion: String?,
    val fecha: String?,
    val integrantes: List<ArtistMember>,
    val albumes: List<ArtistAlbum>,
    val premios: List<ArtistPrize>
)
