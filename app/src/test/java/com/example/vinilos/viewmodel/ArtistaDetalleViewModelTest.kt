package com.example.vinilos.viewmodel

import com.example.vinilos.MainDispatcherRule
import com.example.vinilos.data.model.Artist
import com.example.vinilos.data.model.ArtistType
import com.example.vinilos.data.model.ArtistaDetalle
import com.example.vinilos.data.remote.ErrorDeCarga
import com.example.vinilos.data.repository.ArtistRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

private class ArtistRepositoryFalso : ArtistRepository {
    val consultas = mutableListOf<Triple<ArtistType, Int, CompletableDeferred<Result<ArtistaDetalle>>>>()

    override suspend fun obtenerDetalle(tipo: ArtistType, id: Int): Result<ArtistaDetalle> {
        val respuesta = CompletableDeferred<Result<ArtistaDetalle>>()
        consultas += Triple(tipo, id, respuesta)
        return respuesta.await()
    }

    override suspend fun obtenerArtistas(): Result<List<Artist>> = error("No se usa en esta prueba")
}

@OptIn(ExperimentalCoroutinesApi::class)
class ArtistaDetalleViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private val detalle = ArtistaDetalle(
        101, "Queen", null, "Banda británica", ArtistType.BANDA, "1 de enero de 1970",
        emptyList(), emptyList(), emptyList()
    )

    @Test
    fun alCrearseConsultaElArtistaYPasaAExito() = runTest {
        val repositorio = ArtistRepositoryFalso()
        val viewModel = ArtistaDetalleViewModel(ArtistType.BANDA, 101, repositorio)

        assertEquals(ArtistaDetalleUiState.Cargando, viewModel.uiState.value)
        advanceUntilIdle()
        val consulta = repositorio.consultas.single()
        assertEquals(ArtistType.BANDA to 101, consulta.first to consulta.second)

        consulta.third.complete(Result.success(detalle))
        advanceUntilIdle()

        assertEquals(ArtistaDetalleUiState.Exito(detalle), viewModel.uiState.value)
    }

    @Test
    fun pasaAErrorCuandoFallaLaConsulta() = runTest {
        val repositorio = ArtistRepositoryFalso()
        val viewModel = ArtistaDetalleViewModel(ArtistType.BANDA, 101, repositorio)
        advanceUntilIdle()

        repositorio.consultas.single().third.complete(Result.failure(ErrorDeCarga()))
        advanceUntilIdle()

        assertEquals(ArtistaDetalleUiState.Error, viewModel.uiState.value)
    }

    @Test
    fun reintentarPasaDeErrorACargandoYExitoConElMismoArtista() = runTest {
        val repositorio = ArtistRepositoryFalso()
        val viewModel = ArtistaDetalleViewModel(ArtistType.BANDA, 101, repositorio)
        advanceUntilIdle()
        repositorio.consultas.single().third.complete(Result.failure(ErrorDeCarga()))
        advanceUntilIdle()

        viewModel.reintentar()
        assertEquals(ArtistaDetalleUiState.Cargando, viewModel.uiState.value)
        advanceUntilIdle()
        assertEquals(
            listOf(ArtistType.BANDA to 101, ArtistType.BANDA to 101),
            repositorio.consultas.map { it.first to it.second }
        )
        repositorio.consultas.last().third.complete(Result.success(detalle))
        advanceUntilIdle()

        assertEquals(ArtistaDetalleUiState.Exito(detalle), viewModel.uiState.value)
    }

    @Test
    fun reintentarSoloActuaDesdeError() = runTest {
        val repositorio = ArtistRepositoryFalso()
        val viewModel = ArtistaDetalleViewModel(ArtistType.BANDA, 101, repositorio)
        advanceUntilIdle()

        viewModel.reintentar()
        advanceUntilIdle()
        assertEquals(1, repositorio.consultas.size)

        repositorio.consultas.single().third.complete(Result.success(detalle))
        advanceUntilIdle()
        viewModel.reintentar()
        advanceUntilIdle()

        assertEquals(1, repositorio.consultas.size)
        assertEquals(ArtistaDetalleUiState.Exito(detalle), viewModel.uiState.value)
    }

    /** Con una sola consulta en curso, ningún resultado tardío puede reemplazar a la vigente. */
    @Test
    fun reintentarDosVecesSeguidasNoLanzaUnaConsultaParalela() = runTest {
        val repositorio = ArtistRepositoryFalso()
        val viewModel = ArtistaDetalleViewModel(ArtistType.BANDA, 101, repositorio)
        advanceUntilIdle()
        repositorio.consultas.single().third.complete(Result.failure(ErrorDeCarga()))
        advanceUntilIdle()

        viewModel.reintentar()
        viewModel.reintentar()
        advanceUntilIdle()

        assertEquals(2, repositorio.consultas.size)
    }
}
