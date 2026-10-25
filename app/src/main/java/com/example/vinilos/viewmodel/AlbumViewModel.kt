package com.example.vinilos.viewmodel

import androidx.lifecycle.ViewModel
import com.example.vinilos.data.repository.AlbumRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AlbumViewModel(private val id: Int, private val repositorio: AlbumRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<AlbumUiState>(AlbumUiState.Cargando)
    val uiState = _uiState.asStateFlow()
}
