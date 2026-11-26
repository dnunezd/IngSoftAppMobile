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
     * Deslizar para actualizar: solo con el catálogo ya cargado (lista o vacío). La lista sigue a la
     * vista mientras se consulta y, si la consulta falla, se conserva y se activa el aviso.
     */
    fun actualizar() {
        val anterior = _uiState.value
        _uiState.value = anterior.conIndicadores(actualizando = true, errorAlActualizar = false) ?: return
        consultar(alFallar = anterior.conIndicadores(actualizando = false, errorAlActualizar = true)!!)
    }

    /**
     * Se creó un álbum en el formulario. Este ViewModel siguió vivo en la pila, así que hay que volver
     * a consultar. Con el catálogo a la vista, el repositorio ya guardó el álbum creado: se muestra lo
     * guardado de inmediato, con el indicador de actualización, y la consulta lo revalida
     * (optimizar-cache-e-imagenes, D7). Si lo guardado no lo trae, se actualiza como al deslizar. Con el
     * catálogo en error o cargando se carga de nuevo, y la carga también empieza por lo guardado.
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

    /** La pantalla ya mostró el aviso de que falló la actualización. */
    fun avisoMostrado() {
        val estado = _uiState.value
        val actualizando = (estado as? AlbumsUiState.Success)?.actualizando
            ?: (estado as? AlbumsUiState.Empty)?.actualizando ?: return
        _uiState.value = estado.conIndicadores(actualizando, errorAlActualizar = false)!!
    }

    /** Copia de [AlbumsUiState.Exito] o [AlbumsUiState.Vacio] con otros indicadores; null en los demás. */
    private fun AlbumsUiState.conIndicadores(actualizando: Boolean, errorAlActualizar: Boolean) =
        when (this) {
            is AlbumsUiState.Success -> copy(actualizando = actualizando, errorAlActualizar = errorAlActualizar)
            is AlbumsUiState.Empty -> copy(actualizando = actualizando, errorAlActualizar = errorAlActualizar)
            else -> null
        }

    /**
     * Carga inicial (add-cache-catalogo, D5): si hay un catálogo guardado se muestra de inmediato como
     * si se estuviera deslizando para actualizar, y un fallo de la consulta solo activa el aviso. Sin
     * nada guardado, se comporta como antes: `Cargando` y `Error` si falla.
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

    /** Cancela lo que esté en curso y lanza [bloque] como la carga vigente. */
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
