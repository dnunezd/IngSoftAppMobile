package com.example.vinilos.viewmodel

import com.example.vinilos.MainDispatcherRule
import com.example.vinilos.data.model.Album
import com.example.vinilos.data.model.AlbumNuevo
import com.example.vinilos.data.model.AlbumResumen
import com.example.vinilos.data.remote.ErrorDeCarga
import com.example.vinilos.data.repository.AlbumRepository
import com.example.vinilos.data.repository.AlbumNoDisponible
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

    override suspend fun obtenerAlbum(id: Int): Result<Album> {
        val respuesta = CompletableDeferred<Result<Album>>()
        consultas += id to respuesta
        return if (ignoraCancelacion) withContext(NonCancellable) { respuesta.await() }
        else respuesta.await()
    }

    /** Detalles guardados en el dispositivo (add-cache-consultas). */
    val guardados = mutableMapOf<Int, Album>()

    /** Si no es null, la lectura de lo guardado espera a que la prueba lo complete. */
    var lecturaEnEspera: CompletableDeferred<Unit>? = null

    override suspend fun obtenerAlbumGuardado(id: Int): Album? {
        lecturaEnEspera?.await()
        return guardados[id]
    }

    override suspend fun obtenerAlbumes(): Result<List<AlbumResumen>> = error("No se usa en esta prueba")
    override suspend fun obtenerAlbumesGuardados(): List<AlbumResumen> = error("No se usa en esta prueba")
    override suspend fun crearAlbum(album: AlbumNuevo): Result<Int> = error("No se usa en esta prueba")
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
    fun cargandoPasaAExitoConElAlbumDelIdSolicitado() = runTest {
        val repositorio = AlbumRepositoryFalso()
        val viewModel = AlbumViewModel(101, repositorio)

        assertEquals(AlbumUiState.Cargando, viewModel.uiState.value)
        advanceUntilIdle()
        assertEquals(listOf(101), repositorio.consultas.map { it.first })

        repositorio.consultas.single().second.complete(Result.success(album))
        advanceUntilIdle()

        assertEquals(AlbumUiState.Exito(album), viewModel.uiState.value)
    }

    @Test
    fun cargandoPasaANoDisponibleCuandoElAlbumNoExiste() = runTest {
        val repositorio = AlbumRepositoryFalso()
        val viewModel = AlbumViewModel(999, repositorio)
        assertEquals(AlbumUiState.Cargando, viewModel.uiState.value)
        advanceUntilIdle()

        repositorio.consultas.single().second.complete(Result.failure(AlbumNoDisponible()))
        advanceUntilIdle()

        assertEquals(AlbumUiState.NoDisponible, viewModel.uiState.value)
    }

    @Test
    fun cargandoPasaAErrorCuandoFallaLaConsulta() = runTest {
        val repositorio = AlbumRepositoryFalso()
        val viewModel = AlbumViewModel(101, repositorio)
        assertEquals(AlbumUiState.Cargando, viewModel.uiState.value)
        advanceUntilIdle()

        repositorio.consultas.single().second.complete(Result.failure(ErrorDeCarga()))
        advanceUntilIdle()

        assertEquals(AlbumUiState.Error, viewModel.uiState.value)
    }

    @Test
    fun reintentarPasaDeErrorACargandoYExitoConElMismoId() = runTest {
        val repositorio = AlbumRepositoryFalso()
        val viewModel = AlbumViewModel(101, repositorio)
        advanceUntilIdle()
        repositorio.consultas.single().second.complete(Result.failure(ErrorDeCarga()))
        advanceUntilIdle()
        assertEquals(AlbumUiState.Error, viewModel.uiState.value)

        viewModel.reintentar()
        assertEquals(AlbumUiState.Cargando, viewModel.uiState.value)
        advanceUntilIdle()
        assertEquals(listOf(101, 101), repositorio.consultas.map { it.first })
        repositorio.consultas.last().second.complete(Result.success(album))
        advanceUntilIdle()

        assertEquals(AlbumUiState.Exito(album), viewModel.uiState.value)
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
            assertEquals(AlbumUiState.Exito(vigente), viewModel.uiState.value)

            repositorio.consultas[0].second.complete(Result.success(album))
            advanceUntilIdle()

            assertEquals(AlbumUiState.Exito(vigente), viewModel.uiState.value)
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
        repositorio.consultas.single().second.complete(Result.failure(AlbumNoDisponible()))
        advanceUntilIdle()

        viewModel.reintentar()
        advanceUntilIdle()

        assertEquals(AlbumUiState.NoDisponible, viewModel.uiState.value)
        assertEquals(1, repositorio.consultas.size)
    }

    // ---------- Detalle guardado (add-cache-consultas, D5) ----------

    private val actualizado = album.copy(nombre = "Buscando América (remasterizado)")

    @Test
    fun conDetalleGuardadoSeMuestraDeInmediatoYLuegoLaRespuesta() = runTest {
        val repositorio = AlbumRepositoryFalso().apply { guardados[101] = album }
        val viewModel = AlbumViewModel(101, repositorio)
        advanceUntilIdle()

        assertEquals(AlbumUiState.Exito(album, actualizando = true), viewModel.uiState.value)

        repositorio.consultas.single().second.complete(Result.success(actualizado))
        advanceUntilIdle()

        assertEquals(AlbumUiState.Exito(actualizado), viewModel.uiState.value)
    }

    @Test
    fun conDetalleGuardadoYRedMalSeConservaConElAviso() = runTest {
        val repositorio = AlbumRepositoryFalso().apply { guardados[101] = album }
        val viewModel = AlbumViewModel(101, repositorio)
        advanceUntilIdle()

        repositorio.consultas.single().second.complete(Result.failure(ErrorDeCarga()))
        advanceUntilIdle()

        assertEquals(AlbumUiState.Exito(album, errorAlActualizar = true), viewModel.uiState.value)
    }

    @Test
    fun avisoMostradoApagaElAvisoSinQuitarElDetalle() = runTest {
        val repositorio = AlbumRepositoryFalso().apply { guardados[101] = album }
        val viewModel = AlbumViewModel(101, repositorio)
        advanceUntilIdle()
        repositorio.consultas.single().second.complete(Result.failure(ErrorDeCarga()))
        advanceUntilIdle()

        viewModel.avisoMostrado()

        assertEquals(AlbumUiState.Exito(album), viewModel.uiState.value)
    }

    @Test
    fun un404TerminaEnNoDisponibleAunqueHayaDetalleGuardado() = runTest {
        val repositorio = AlbumRepositoryFalso().apply { guardados[101] = album }
        val viewModel = AlbumViewModel(101, repositorio)
        advanceUntilIdle()

        repositorio.consultas.single().second.complete(Result.failure(AlbumNoDisponible()))
        advanceUntilIdle()

        assertEquals(AlbumUiState.NoDisponible, viewModel.uiState.value)
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

        assertEquals(AlbumUiState.Exito(otro, actualizando = true), viewModel.uiState.value)
        assertEquals(listOf(102), repositorio.consultas.map { it.first })
        repositorio.consultas.single().second.complete(Result.success(otro))
        advanceUntilIdle()
    }
}
