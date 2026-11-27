package com.example.vinilos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vinilos.data.model.NewAlbum
import com.example.vinilos.data.remote.LoadError
import com.example.vinilos.data.repository.AlbumRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CreateAlbumViewModel(private val repositorio: AlbumRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateAlbumUiState())
    val uiState: StateFlow<CreateAlbumUiState> = _uiState.asStateFlow()

    fun cambiarNombre(valor: String) = editar(AlbumField.Nombre) { copy(nombre = valor) }

    fun cambiarPortada(valor: String) = editar(AlbumField.Portada) { copy(portada = valor) }

    fun cambiarFecha(millis: Long) = editar(AlbumField.Fecha) { copy(fechaMillis = millis) }

    fun cambiarGenero(valor: String) = editar(AlbumField.Genero) { copy(genero = valor) }

    fun cambiarDisquera(valor: String) = editar(AlbumField.Disquera) { copy(disquera = valor) }

    fun cambiarDescripcion(valor: String) =
        editar(AlbumField.Descripcion) { copy(descripcion = valor.take(LimiteDescripcion)) }

    /** Editing a field clears only its own error; while submitting or after creation the data no longer changes. */
    private fun editar(campo: AlbumField, cambio: CreateAlbumUiState.() -> CreateAlbumUiState) {
        _uiState.update { estado ->
            if (estado.enviando || estado.envio is EnvioAlbum.Creado) estado
            else estado.cambio().copy(errores = estado.errores - campo)
        }
    }

    /**
     * Validates and submits. The backend doesn't prevent duplicate albums, so this guard (and not just
     * the disabled button) is what prevents submitting twice with two quick taps.
     */
    fun create() {
        val estado = _uiState.value
        if (estado.enviando || estado.envio is EnvioAlbum.Creado) return

        val errores = validarAlbum(estado)
        if (errores.isNotEmpty()) {
            _uiState.value = estado.copy(
                errores = errores,
                intentosFallidos = estado.intentosFallidos + 1,
                envio = EnvioAlbum.Editando
            )
            return
        }

        _uiState.value = estado.copy(errores = emptyMap(), envio = EnvioAlbum.Enviando)
        val album = NewAlbum(
            nombre = estado.nombre.trim(),
            portada = estado.portada.trim(),
            fechaLanzamientoMillis = requireNotNull(estado.fechaMillis),
            descripcion = estado.descripcion.trim(),
            genero = requireNotNull(estado.genero),
            disquera = requireNotNull(estado.disquera)
        )
        viewModelScope.launch {
            val envio = repositorio.createAlbum(album).fold(
                onSuccess = { EnvioAlbum.Creado(it) },
                onFailure = {
                    when (it) {
                        is LoadError -> EnvioAlbum.Fallido
                        else -> throw it
                    }
                }
            )
            _uiState.update { it.copy(envio = envio) }
        }
    }
}
