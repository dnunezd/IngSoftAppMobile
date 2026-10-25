package com.example.vinilos.viewmodel

import com.example.vinilos.MainDispatcherRule
import com.example.vinilos.data.model.Album
import com.example.vinilos.data.remote.ServiceAdapter
import com.example.vinilos.data.repository.AlbumRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

private class AlbumRepositoryFalso : AlbumRepository(ServiceAdapter("http://localhost/")) {
    val consultas = mutableListOf<Pair<Int, CompletableDeferred<Result<Album>>>>()

    override suspend fun obtenerAlbum(id: Int): Result<Album> {
        val respuesta = CompletableDeferred<Result<Album>>()
        consultas += id to respuesta
        return respuesta.await()
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
}
