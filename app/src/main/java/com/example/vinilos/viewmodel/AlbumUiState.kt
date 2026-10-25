package com.example.vinilos.viewmodel

import com.example.vinilos.data.model.Album

sealed interface AlbumUiState {
    data object Cargando : AlbumUiState
    data class Exito(val album: Album) : AlbumUiState
}
