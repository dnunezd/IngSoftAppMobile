package com.example.vinilos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vinilos.data.remote.LoadError
import com.example.vinilos.data.repository.AlbumRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AlbumsViewModel(private val repositorio: AlbumRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<AlbumsUiState>(AlbumsUiState.Loading)
    val uiState: StateFlow<AlbumsUiState> = _uiState.asStateFlow()

    private val _albumNuevo = MutableStateFlow<NewAlbumCatalogo?>(null)
    val albumNuevo: StateFlow<NewAlbumCatalogo?> = _albumNuevo.asStateFlow()

    private var carga: Job? = null
    private var consultaVigente = 0L

    init {
        cargar()
    }

    fun retry() {
        if (_uiState.value == AlbumsUiState.Error) cargar()
    }

    /**
     * Pull-to-refresh: only once the catalog is already loaded (list or empty). The list stays
     * visible while the query runs and, if the query fails, it's kept and the notice is shown.
     */
    fun actualizar() {
        val anterior = _uiState.value
        _uiState.value = anterior.conIndicadores(actualizando = true, errorAlActualizar = false) ?: return
        consultar(alFallar = anterior.conIndicadores(actualizando = false, errorAlActualizar = true)!!)
    }

    /**
     * An album was created in the form. This ViewModel stayed alive on the stack, so it needs to query
     * again. With the catalog on screen, the repository has already saved the created album: what's
     * saved is shown immediately, with the refresh indicator, and the query revalidates it
     * (optimizar-cache-e-imagenes, D7). If what's saved doesn't include it, it refreshes as on pull-to-refresh. With the
     * catalog in error or loading it's loaded again, and that load also starts from what's saved.
     */
    fun albumCreado(id: Int) {
        _albumNuevo.value = NewAlbumCatalogo(id)
        val anterior = _uiState.value
        _uiState.value = anterior.conIndicadores(actualizando = true, errorAlActualizar = false) ?: return cargar()
        lanzar { consulta ->
            val guardados = repositorio.getCachedAlbums()
            if (consulta != consultaVigente) return@lanzar
            val alFallar = if (guardados.any { it.id == id }) {
                val conElCreado = AlbumsUiState.Success(guardados, actualizando = true)
                _uiState.value = conElCreado
                conElCreado.copy(actualizando = false, errorAlActualizar = true)
            } else {
                anterior.conIndicadores(actualizando = false, errorAlActualizar = true)!!
            }
            consultarAhora(consulta, alFallar)
        }
    }

    fun avisoCreadoMostrado() {
        _albumNuevo.value = _albumNuevo.value?.copy(avisoPendiente = false)
    }

    fun desplazadoAlNuevo() {
        _albumNuevo.value = _albumNuevo.value?.copy(desplazamientoPendiente = false)
    }

    /** The screen already showed the notice that the refresh failed. */
    fun avisoMostrado() {
        val estado = _uiState.value
        val actualizando = (estado as? AlbumsUiState.Success)?.actualizando
            ?: (estado as? AlbumsUiState.Empty)?.actualizando ?: return
        _uiState.value = estado.conIndicadores(actualizando, errorAlActualizar = false)!!
    }

    /** Copy of [AlbumsUiState.Exito] or [AlbumsUiState.Vacio] with other indicators; null for the rest. */
    private fun AlbumsUiState.conIndicadores(actualizando: Boolean, errorAlActualizar: Boolean) =
        when (this) {
            is AlbumsUiState.Success -> copy(actualizando = actualizando, errorAlActualizar = errorAlActualizar)
            is AlbumsUiState.Empty -> copy(actualizando = actualizando, errorAlActualizar = errorAlActualizar)
            else -> null
        }

    /**
     * Initial load (add-cache-catalogo, D5): if there's a saved catalog it's shown immediately as
     * if a pull-to-refresh were in progress, and a query failure only triggers the notice. With
     * nothing saved, it behaves as before: `Loading` and `Error` if it fails.
     */
    private fun cargar() {
        _uiState.value = AlbumsUiState.Loading
        lanzar { consulta ->
            val guardados = repositorio.getCachedAlbums()
            if (consulta != consultaVigente) return@lanzar
            val alFallar = if (guardados.isEmpty()) {
                AlbumsUiState.Error
            } else {
                val enPantalla = AlbumsUiState.Success(guardados, actualizando = true)
                _uiState.value = enPantalla
                enPantalla.copy(actualizando = false, errorAlActualizar = true)
            }
            consultarAhora(consulta, alFallar)
        }
    }

    private fun consultar(alFallar: AlbumsUiState) = lanzar { consulta -> consultarAhora(consulta, alFallar) }

    /** Cancels whatever is in progress and launches [bloque] as the current load. */
    private fun lanzar(bloque: suspend (consulta: Long) -> Unit) {
        val consulta = ++consultaVigente
        carga?.cancel()
        carga = viewModelScope.launch { bloque(consulta) }
    }

    private suspend fun consultarAhora(consulta: Long, alFallar: AlbumsUiState) {
        val resultado = repositorio.getAlbums()
        if (consulta != consultaVigente) return
        _uiState.value = resultado.fold(
            onSuccess = { albumes ->
                if (albumes.isEmpty()) AlbumsUiState.Empty() else AlbumsUiState.Success(albumes)
            },
            onFailure = {
                when (it) {
                    is LoadError -> alFallar
                    else -> throw it
                }
            }
        )
    }
}
