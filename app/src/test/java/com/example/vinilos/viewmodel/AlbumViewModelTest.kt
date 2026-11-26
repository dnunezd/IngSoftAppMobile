package com.example.vinilos.viewmodel

import com.example.vinilos.MainDispatcherRule
import com.example.vinilos.data.model.Album
import com.example.vinilos.data.model.NewAlbum
import com.example.vinilos.data.model.AlbumSummary
import com.example.vinilos.data.model.Comment
import com.example.vinilos.data.remote.LoadError
import com.example.vinilos.data.repository.AlbumRepository
import com.example.vinilos.data.repository.AlbumNotAvailable
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

private class AlbumRepositoryFalso(
    private val ignoraCancelacion: Boolean = false
) : AlbumRepository {
    val consultas = mutableListOf<Pair<Int, CompletableDeferred<Result<Album>>>>()

    override suspend fun getAlbum(id: Int): Result<Album> {
        val respuesta = CompletableDeferred<Result<Album>>()
        consultas += id to respuesta
        return if (ignoraCancelacion) withContext(NonCancellable) { respuesta.await() }
        else respuesta.await()
    }

    /** Detalles guardados en el dispositivo (add-cache-consultas). */
    val guardados = mutableMapOf<Int, Album>()

    /** Si no es null, la lectura de lo guardado espera a que la prueba lo complete. */
    var lecturaEnEspera: CompletableDeferred<Unit>? = null

    override suspend fun getCachedAlbum(id: Int): Album? {
        lecturaEnEspera?.await()
        return guardados[id]
    }

    override suspend fun getAlbums(): Result<List<AlbumSummary>> = error("No se usa en esta prueba")
    override suspend fun getCachedAlbums(): List<AlbumSummary> = error("No se usa en esta prueba")
    override suspend fun createAlbum(album: NewAlbum): Result<Int> = error("No se usa en esta prueba")
    override suspend fun addTrack(
        albumId: Int,
        cancion: com.example.vinilos.data.model.Track
    ): Result<Unit> = error("No se usa en esta prueba")
}

@OptIn(ExperimentalCoroutinesApi::class)
class AlbumViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private val album = Album(
        101, "Buscando America", null, "Descripcion", "Salsa", "Elektra",
        "1984-08-01", emptyList(), emptyList(), emptyList()
    )

    @Test
    fun cargandoPasaASuccessConElAlbumDelIdSolicitado() = runTest {
        val repositorio = AlbumRepositoryFalso()
        val viewModel = AlbumViewModel(101, repositorio)

        assertEquals(AlbumUiState.Loading, viewModel.uiState.value)
        advanceUntilIdle()
        assertEquals(listOf(101), repositorio.consultas.map { it.first })

        repositorio.consultas.single().second.complete(Result.success(album))
        advanceUntilIdle()

        assertEquals(AlbumUiState.Success(album), viewModel.uiState.value)
    }

    @Test
    fun cargandoPasaANotAvailableCuandoElAlbumNoExiste() = runTest {
        val repositorio = AlbumRepositoryFalso()
        val viewModel = AlbumViewModel(999, repositorio)
        assertEquals(AlbumUiState.Loading, viewModel.uiState.value)
        advanceUntilIdle()

        repositorio.consultas.single().second.complete(Result.failure(AlbumNotAvailable()))
        advanceUntilIdle()

        assertEquals(AlbumUiState.NotAvailable, viewModel.uiState.value)
    }

    @Test
    fun cargandoPasaAErrorCuandoFallaLaConsulta() = runTest {
        val repositorio = AlbumRepositoryFalso()
        val viewModel = AlbumViewModel(101, repositorio)
        assertEquals(AlbumUiState.Loading, viewModel.uiState.value)
        advanceUntilIdle()

        repositorio.consultas.single().second.complete(Result.failure(LoadError()))
        advanceUntilIdle()

        assertEquals(AlbumUiState.Error, viewModel.uiState.value)
    }

    @Test
    fun retryPasaDeErrorALoadingYSuccessConElMismoId() = runTest {
        val repositorio = AlbumRepositoryFalso()
        val viewModel = AlbumViewModel(101, repositorio)
        advanceUntilIdle()
        repositorio.consultas.single().second.complete(Result.failure(LoadError()))
        advanceUntilIdle()
        assertEquals(AlbumUiState.Error, viewModel.uiState.value)

        viewModel.retry()
        assertEquals(AlbumUiState.Loading, viewModel.uiState.value)
        advanceUntilIdle()
        assertEquals(listOf(101, 101), repositorio.consultas.map { it.first })
        repositorio.consultas.last().second.complete(Result.success(album))
        advanceUntilIdle()

        assertEquals(AlbumUiState.Success(album), viewModel.uiState.value)
    }

    @Test
    fun respuestaTardiaNoReemplazaLaConsultaVigente() = runTest {
        val repositorio = AlbumRepositoryFalso(ignoraCancelacion = true)
        val viewModel = AlbumViewModel(101, repositorio)
        advanceUntilIdle()

        viewModel.cargarAlbum(102)
        advanceUntilIdle()
        try {
            assertEquals(listOf(101, 102), repositorio.consultas.map { it.first })
            val vigente = album.copy(id = 102, nombre = "Álbum vigente")
            repositorio.consultas[1].second.complete(Result.success(vigente))
            advanceUntilIdle()
            assertEquals(AlbumUiState.Success(vigente), viewModel.uiState.value)

            repositorio.consultas[0].second.complete(Result.success(album))
            advanceUntilIdle()

            assertEquals(AlbumUiState.Success(vigente), viewModel.uiState.value)
        } finally {
            repositorio.consultas.forEach { it.second.complete(Result.success(album)) }
            advanceUntilIdle()
        }
    }

    @Test
    fun noDisponibleNoPermiteReintentar() = runTest {
        val repositorio = AlbumRepositoryFalso()
        val viewModel = AlbumViewModel(999, repositorio)
        advanceUntilIdle()
        repositorio.consultas.single().second.complete(Result.failure(AlbumNotAvailable()))
        advanceUntilIdle()

        viewModel.retry()
        advanceUntilIdle()

        assertEquals(AlbumUiState.NotAvailable, viewModel.uiState.value)
        assertEquals(1, repositorio.consultas.size)
    }

    // ---------- Detalle guardado (add-cache-consultas, D5) ----------

    private val actualizado = album.copy(nombre = "Buscando América (remasterizado)")

    @Test
    fun conDetalleGuardadoSeMuestraDeInmediatoYLuegoLaRespuesta() = runTest {
        val repositorio = AlbumRepositoryFalso().apply { guardados[101] = album }
        val viewModel = AlbumViewModel(101, repositorio)
        advanceUntilIdle()

        assertEquals(AlbumUiState.Success(album, actualizando = true), viewModel.uiState.value)

        repositorio.consultas.single().second.complete(Result.success(actualizado))
        advanceUntilIdle()

        assertEquals(AlbumUiState.Success(actualizado), viewModel.uiState.value)
    }

    @Test
    fun conDetalleGuardadoYRedMalSeConservaConElAviso() = runTest {
        val repositorio = AlbumRepositoryFalso().apply { guardados[101] = album }
        val viewModel = AlbumViewModel(101, repositorio)
        advanceUntilIdle()

        repositorio.consultas.single().second.complete(Result.failure(LoadError()))
        advanceUntilIdle()

        assertEquals(AlbumUiState.Success(album, errorAlActualizar = true), viewModel.uiState.value)
    }

    @Test
    fun avisoMostradoApagaElAvisoSinQuitarElDetalle() = runTest {
        val repositorio = AlbumRepositoryFalso().apply { guardados[101] = album }
        val viewModel = AlbumViewModel(101, repositorio)
        advanceUntilIdle()
        repositorio.consultas.single().second.complete(Result.failure(LoadError()))
        advanceUntilIdle()

        viewModel.avisoMostrado()

        assertEquals(AlbumUiState.Success(album), viewModel.uiState.value)
    }

    @Test
    fun un404TerminaEnNotAvailableAunqueHayaDetalleGuardado() = runTest {
        val repositorio = AlbumRepositoryFalso().apply { guardados[101] = album }
        val viewModel = AlbumViewModel(101, repositorio)
        advanceUntilIdle()

        repositorio.consultas.single().second.complete(Result.failure(AlbumNotAvailable()))
        advanceUntilIdle()

        assertEquals(AlbumUiState.NotAvailable, viewModel.uiState.value)
    }

    @Test
    fun otroAlbumPedidoDuranteLaLecturaDeLoGuardadoGana() = runTest {
        val otro = album.copy(id = 102, nombre = "Otro álbum")
        val repositorio = AlbumRepositoryFalso().apply {
            guardados[101] = album
            guardados[102] = otro
            lecturaEnEspera = CompletableDeferred()
        }
        val viewModel = AlbumViewModel(101, repositorio)
        advanceUntilIdle()

        viewModel.cargarAlbum(102)
        repositorio.lecturaEnEspera!!.complete(Unit)
        advanceUntilIdle()

        assertEquals(AlbumUiState.Success(otro, actualizando = true), viewModel.uiState.value)
        assertEquals(listOf(102), repositorio.consultas.map { it.first })
        repositorio.consultas.single().second.complete(Result.success(otro))
        advanceUntilIdle()
    }

    // ---------- Actualizar después de comentar (add-comentar-album, D7) ----------

    private suspend fun kotlinx.coroutines.test.TestScope.conAlbumVisible(repositorio: AlbumRepositoryFalso): AlbumViewModel {
        val viewModel = AlbumViewModel(101, repositorio)
        advanceUntilIdle()
        repositorio.consultas.single().second.complete(Result.success(album))
        advanceUntilIdle()
        return viewModel
    }

    @Test
    fun actualizarMantieneElAlbumALaVistaYLoReemplazaConLaRespuesta() = runTest {
        val repositorio = AlbumRepositoryFalso()
        val viewModel = conAlbumVisible(repositorio)
        val comentado = album.copy(comentarios = listOf(Comment("Gran prensado", 5)))

        viewModel.actualizar()
        advanceUntilIdle()
        assertEquals(AlbumUiState.Success(album, actualizando = true), viewModel.uiState.value)
        assertEquals(listOf(101, 101), repositorio.consultas.map { it.first })

        repositorio.consultas.last().second.complete(Result.success(comentado))
        advanceUntilIdle()

        assertEquals(AlbumUiState.Success(comentado), viewModel.uiState.value)
    }

    @Test
    fun siActualizarFallaConservaElAlbumConElAviso() = runTest {
        val repositorio = AlbumRepositoryFalso()
        val viewModel = conAlbumVisible(repositorio)

        viewModel.actualizar()
        advanceUntilIdle()
        repositorio.consultas.last().second.complete(Result.failure(LoadError()))
        advanceUntilIdle()

        assertEquals(AlbumUiState.Success(album, errorAlActualizar = true), viewModel.uiState.value)
    }

    @Test
    fun actualizarSinAlbumALaVistaCargaComoAlEmpezar() = runTest {
        val repositorio = AlbumRepositoryFalso()
        val viewModel = AlbumViewModel(101, repositorio)
        advanceUntilIdle()
        repositorio.consultas.single().second.complete(Result.failure(LoadError()))
        advanceUntilIdle()

        viewModel.actualizar()

        assertEquals(AlbumUiState.Loading, viewModel.uiState.value)
    }

    // ---------- Aviso "Canción agregada" (revisión de PR: no debe repetir el GET al rotar) ----------

    @Test
    fun cancionAgregadaPrendeElAvisoYDisparaUnaSolaActualizacion() = runTest {
        val repositorio = AlbumRepositoryFalso()
        val viewModel = conAlbumVisible(repositorio)
        assertEquals(false, viewModel.avisoTrackAgregada.value)

        viewModel.cancionAgregada()
        advanceUntilIdle()

        assertEquals(true, viewModel.avisoTrackAgregada.value)
        assertEquals(listOf(101, 101), repositorio.consultas.map { it.first })
    }

    @Test
    fun avisoTrackAgregadaMostradoApagaElAvisoSinRepetirLaConsulta() = runTest {
        val repositorio = AlbumRepositoryFalso()
        val viewModel = conAlbumVisible(repositorio)
        viewModel.cancionAgregada()
        advanceUntilIdle()
        repositorio.consultas.last().second.complete(Result.success(album))
        advanceUntilIdle()

        viewModel.avisoTrackAgregadaMostrado()

        assertEquals(false, viewModel.avisoTrackAgregada.value)
        assertEquals(2, repositorio.consultas.size)
    }

    @Test
    fun elAvisoDeTrackAgregadaSobreviveUnaRecreacionDelViewModelAunNoMostrado() = runTest {
        // Simula "rotar mientras se ve el aviso": el ViewModel no se destruye (sobrevive a cambios de
        // configuración), así que su estado no se reinicia como sí pasaría con una clave de nav.
        val repositorio = AlbumRepositoryFalso()
        val viewModel = conAlbumVisible(repositorio)

        viewModel.cancionAgregada()
        advanceUntilIdle()
        repositorio.consultas.last().second.complete(Result.success(album))
        advanceUntilIdle()

        // El aviso sigue activo (no se ha marcado como mostrado) y la consulta no se repite más.
        assertEquals(true, viewModel.avisoTrackAgregada.value)
        assertEquals(2, repositorio.consultas.size)
    }
}
