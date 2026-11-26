package com.example.vinilos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vinilos.data.model.Album
import com.example.vinilos.data.repository.AlbumRepository
import com.example.vinilos.data.repository.AlbumNotAvailable
import com.example.vinilos.data.remote.LoadError
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AlbumViewModel(id: Int, private val repositorio: AlbumRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<AlbumUiState>(AlbumUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private var idActual = id
    private var carga: Job? = null
    private var consultaVigente = 0L

    /**
     * Dueño del aviso "Canción agregada": vive en el ViewModel (sobrevive a una rotación) en vez de
     * depender de la clave de navegación, que se borra apenas se dispara [actualizar] (ver [cancionAgregada]).
     * Así, si la Activity se recrea mientras el aviso está en pantalla, no se repite el GET /albums/{id}.
     */
    private val _avisoTrackAgregada = MutableStateFlow(false)
    val avisoTrackAgregada = _avisoTrackAgregada.asStateFlow()

    /** Se llama una sola vez al volver del formulario de agregar canción con éxito. */
    fun cancionAgregada() {
        _avisoTrackAgregada.value = true
        actualizar()
    }

    /** La pantalla ya mostró el aviso de "Canción agregada". */
    fun avisoTrackAgregadaMostrado() {
        _avisoTrackAgregada.value = false
    }

    fun cargarAlbum(id: Int) {
        idActual = id
        cargar()
    }

    fun retry() {
        if (_uiState.value == AlbumUiState.Error) cargar()
    }

    /** La pantalla ya mostró el aviso de que no se pudo actualizar el detalle guardado. */
    fun avisoMostrado() {
        val estado = _uiState.value
        if (estado is AlbumUiState.Success && estado.errorAlActualizar) {
            _uiState.value = estado.copy(errorAlActualizar = false)
        }
    }

    /**
     * Vuelve a consultar el álbum manteniendo a la vista el que ya se muestra, por ejemplo después de
     * agregarle una canción o comentarlo (add-comentar-album, D7). Sin álbum a la vista, carga como al
     * empezar.
     */
    fun actualizar() {
        cargar(visible = (_uiState.value as? AlbumUiState.Success)?.album)
    }

    init {
        cargar()
    }

    /**
     * Con un detalle guardado, lo muestra de inmediato mientras consulta, y un fallo de la consulta solo
     * activa el aviso. Sin nada guardado, se comporta como antes: `Cargando` y `Error` si falla. Un 404
     * siempre lleva a `NoDisponible` (add-cache-consultas, D5).
     */
    private fun cargar(visible: Album? = null) {
        val consulta = ++consultaVigente
        val id = idActual
        carga?.cancel()
        _uiState.value = visible?.let { AlbumUiState.Success(it, actualizando = true) } ?: AlbumUiState.Loading
        carga = viewModelScope.launch {
            // Con un álbum a la vista, ese hace las veces de lo guardado.
            val guardado = visible ?: repositorio.getCachedAlbum(id)
            if (consulta != consultaVigente) return@launch
            if (visible == null && guardado != null) _uiState.value = AlbumUiState.Success(guardado, actualizando = true)

            val resultado = repositorio.getAlbum(id)
            if (consulta != consultaVigente) return@launch
            _uiState.value = resultado.fold(
                onSuccess = { AlbumUiState.Success(it) },
                onFailure = {
                    when (it) {
                        is AlbumNotAvailable -> AlbumUiState.NotAvailable
                        is LoadError ->
                            guardado?.let { album -> AlbumUiState.Success(album, errorAlActualizar = true) }
                                ?: AlbumUiState.Error
                        else -> throw it
                    }
                }
            )
        }
    }
}
