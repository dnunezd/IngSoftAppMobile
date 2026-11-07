package com.example.vinilos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vinilos.data.model.AlbumNuevo
import com.example.vinilos.data.remote.ErrorDeCarga
import com.example.vinilos.data.repository.AlbumRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CrearAlbumViewModel(private val repositorio: AlbumRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(CrearAlbumUiState())
    val uiState: StateFlow<CrearAlbumUiState> = _uiState.asStateFlow()

    fun cambiarNombre(valor: String) = editar(CampoAlbum.Nombre) { copy(nombre = valor) }

    fun cambiarPortada(valor: String) = editar(CampoAlbum.Portada) { copy(portada = valor) }

    fun cambiarFecha(millis: Long) = editar(CampoAlbum.Fecha) { copy(fechaMillis = millis) }

    fun cambiarGenero(valor: String) = editar(CampoAlbum.Genero) { copy(genero = valor) }

    fun cambiarDisquera(valor: String) = editar(CampoAlbum.Disquera) { copy(disquera = valor) }

    fun cambiarDescripcion(valor: String) =
        editar(CampoAlbum.Descripcion) { copy(descripcion = valor.take(LimiteDescripcion)) }

    /** Editar un campo borra solo su error; durante el envío o tras crear los datos ya no cambian. */
    private fun editar(campo: CampoAlbum, cambio: CrearAlbumUiState.() -> CrearAlbumUiState) {
        _uiState.update { estado ->
            if (estado.enviando || estado.envio is EnvioAlbum.Creado) estado
            else estado.cambio().copy(errores = estado.errores - campo)
        }
    }

    /**
     * Valida y envía. El backend no impide álbumes repetidos, así que esta guarda (y no solo el botón
     * desactivado) es la que evita enviar dos veces con dos toques seguidos.
     */
    fun crear() {
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
        val album = AlbumNuevo(
            nombre = estado.nombre.trim(),
            portada = estado.portada.trim(),
            fechaLanzamientoMillis = requireNotNull(estado.fechaMillis),
            descripcion = estado.descripcion.trim(),
            genero = requireNotNull(estado.genero),
            disquera = requireNotNull(estado.disquera)
        )
        viewModelScope.launch {
            val envio = repositorio.crearAlbum(album).fold(
                onSuccess = { EnvioAlbum.Creado(it) },
                onFailure = {
                    when (it) {
                        is ErrorDeCarga -> EnvioAlbum.Fallido
                        else -> throw it
                    }
                }
            )
            _uiState.update { it.copy(envio = envio) }
        }
    }
}
