package com.example.vinilos.viewmodel

import com.example.vinilos.MainDispatcherRule
import com.example.vinilos.data.model.Album
import com.example.vinilos.data.remote.ServiceAdapter
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
) : AlbumRepository(ServiceAdapter("http://localhost/")) {
    val consultas = mutableListOf<Pair<Int, CompletableDeferred<Result<Album>>>>()

    override suspend fun obtenerAlbum(id: Int): Result<Album> {
        val respuesta = CompletableDeferred<Result<Album>>()
        consultas += id to respuesta
        return if (ignoraCancelacion) withContext(NonCancellable) { respuesta.await() }
        else respuesta.await()
    }
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
}
