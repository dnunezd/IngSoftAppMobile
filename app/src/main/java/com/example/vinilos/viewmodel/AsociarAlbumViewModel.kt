package com.example.vinilos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vinilos.data.model.ArtistType
import com.example.vinilos.data.remote.ErrorDeCarga
import com.example.vinilos.data.repository.AlbumRepository
import com.example.vinilos.data.repository.ArtistRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Ventana para agregar un álbum a un artista (HU15). Vive en la entrada de navegación del detalle del
 * artista, así que la ventana abierta, la búsqueda y la selección sobreviven a la rotación (D2).
 */
class AsociarAlbumViewModel(
    private val tipo: ArtistType,
    private val idArtista: Int,
    private val albumes: AlbumRepository,
    private val artistas: ArtistRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AsociarAlbumUiState())
    val uiState: StateFlow<AsociarAlbumUiState> = _uiState.asStateFlow()

    private var carga: Job? = null

    /** Abre la ventana desde cero con los álbumes que el artista ya tiene y carga el catálogo. */
    fun abrir(asociados: Set<Int>) {
        _uiState.value = AsociarAlbumUiState(abierta = true, asociados = asociados)
        cargar()
    }

    fun buscar(texto: String) {
        _uiState.update { it.copy(busqueda = texto).sinSeleccionOculta() }
    }

    fun seleccionar(id: Int) {
        _uiState.update {
            if (it.asociando || it.yaAsociado(id)) it else it.copy(seleccionado = id, errorAlAsociar = false)
        }
    }

    /** Cierra sin asociar. Mientras se envía no se puede cancelar: el backend ya pudo recibirlo. */
    fun cancelar() {
        if (_uiState.value.asociando) return
        carga?.cancel()
        _uiState.value = AsociarAlbumUiState()
    }

    fun reintentarCarga() {
        if (_uiState.value.listado == ListadoAsociable.Error) cargar()
    }

    fun asociar() {
        val estado = _uiState.value
        if (!estado.puedeAsociar) return
        val idAlbum = estado.seleccionado ?: return
        val album = (estado.listado as? ListadoAsociable.Listo)?.albumes?.firstOrNull { it.id == idAlbum } ?: return
        _uiState.value = estado.copy(asociando = true, errorAlAsociar = false)
        viewModelScope.launch {
            artistas.asociarAlbum(tipo, idArtista, idAlbum).fold(
                onSuccess = {
                    carga?.cancel()
                    _uiState.value = AsociarAlbumUiState(albumAsociado = album)
                },
                onFailure = {
                    if (it !is ErrorDeCarga) throw it
                    _uiState.update { actual -> actual.copy(asociando = false, errorAlAsociar = true) }
                }
            )
        }
    }

    fun asociadoNotificado() {
        _uiState.update { it.copy(albumAsociado = null) }
    }

    /**
     * Mismo enfoque que el catálogo (D3): lo guardado se muestra de inmediato; si la consulta falla, se
     * conserva, y sin nada guardado se muestra el error con "Reintentar".
     */
    private fun cargar() {
        carga?.cancel()
        _uiState.update { it.copy(listado = ListadoAsociable.Cargando) }
        carga = viewModelScope.launch {
            val guardados = albumes.obtenerAlbumesGuardados()
            if (guardados.isNotEmpty()) _uiState.update { it.copy(listado = ListadoAsociable.Listo(guardados)) }
            albumes.obtenerAlbumes().fold(
                onSuccess = { lista ->
                    _uiState.update { it.copy(listado = ListadoAsociable.Listo(lista)).sinSeleccionOculta() }
                },
                onFailure = {
                    if (it !is ErrorDeCarga) throw it
                    if (guardados.isEmpty()) _uiState.update { estado -> estado.copy(listado = ListadoAsociable.Error) }
                }
            )
        }
    }
}
