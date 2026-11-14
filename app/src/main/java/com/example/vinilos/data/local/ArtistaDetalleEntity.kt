package com.example.vinilos.data.local

import androidx.room.Entity
import com.example.vinilos.data.model.AlbumArtista
import com.example.vinilos.data.model.ArtistType
import com.example.vinilos.data.model.IntegranteArtista
import com.example.vinilos.data.model.PremioArtista

/** Detalle de un artista que el usuario abrió (HU04), con los mismos campos que `ArtistaDetalle`. */
@Entity(tableName = "artista_detalle", primaryKeys = ["tipo", "id"])
data class ArtistaDetalleEntity(
    val tipo: ArtistType,
    val id: Int,
    val nombre: String,
    val imagen: String?,
    val descripcion: String?,
    val fecha: String?,
    val integrantes: List<IntegranteArtista>,
    val albumes: List<AlbumArtista>,
    val premios: List<PremioArtista>
)
