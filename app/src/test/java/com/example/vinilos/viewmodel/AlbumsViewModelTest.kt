package com.example.vinilos.viewmodel

import com.example.vinilos.MainDispatcherRule
import com.example.vinilos.data.model.AlbumResumen
import com.example.vinilos.data.remote.ErrorDeCarga
import com.example.vinilos.data.remote.ServiceAdapter
import com.example.vinilos.data.repository.AlbumRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Cada consulta queda pendiente hasta que la prueba la completa. */
private class CatalogoFalso(
    private val ignoraCancelacion: Boolean = false
) : AlbumRepository(ServiceAdapter("http://localhost/")) {
    val consultas = mutableListOf<CompletableDeferred<Result<List<AlbumResumen>>>>()

    override suspend fun obtenerAlbumes(): Result<List<AlbumResumen>> {
        val respuesta = CompletableDeferred<Result<List<AlbumResumen>>>()
        consultas += respuesta
        return if (ignoraCancelacion) withContext(NonCancellable) { respuesta.await() }
        else respuesta.await()
    }

    fun responder(resultado: Result<List<AlbumResumen>>) = consultas.last().complete(resultado)
}

@OptIn(ExperimentalCoroutinesApi::class)
class AlbumsViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private val opera = AlbumResumen(102, "A Night at the Opera", null, "Rock", "Queen", "1975")
    private val buscando = AlbumResumen(100, "Buscando América", null, "Salsa", "Rubén Blades", "1984")

    private fun kotlinx.coroutines.test.TestScope.cargado(
        resultado: Result<List<AlbumResumen>>,
        repositorio: CatalogoFalso = CatalogoFalso()
    ): Pair<AlbumsViewModel, CatalogoFalso> {
        val viewModel = AlbumsViewModel(repositorio)
        advanceUntilIdle()
        repositorio.responder(resultado)
        advanceUntilIdle()
        return viewModel to repositorio
    }

    @Test
    fun cargandoPasaAExitoConLosAlbumes() = runTest {
        val repositorio = CatalogoFalso()
        val viewModel = AlbumsViewModel(repositorio)
        assertEquals(AlbumsUiState.Cargando, viewModel.uiState.value)
        advanceUntilIdle()

        repositorio.responder(Result.success(listOf(opera, buscando)))
        advanceUntilIdle()

        assertEquals(AlbumsUiState.Exito(listOf(opera, buscando)), viewModel.uiState.value)
    }

    @Test
    fun cargandoPasaAVacioSinAlbumes() = runTest {
        val (viewModel, _) = cargado(Result.success(emptyList()))

        assertEquals(AlbumsUiState.Vacio(), viewModel.uiState.value)
    }

    @Test
    fun cargandoPasaAErrorCuandoFallaLaConsulta() = runTest {
        val (viewModel, _) = cargado(Result.failure(ErrorDeCarga()))

        assertEquals(AlbumsUiState.Error, viewModel.uiState.value)
    }

    @Test
    fun laConsultaSeHaceUnaSolaVezAlCrearse() = runTest {
        val (_, repositorio) = cargado(Result.success(listOf(opera)))

        assertEquals(1, repositorio.consultas.size)
    }

    @Test
    fun reintentarDesdeErrorPasaPorCargandoYLlegaAExito() = runTest {
        val (viewModel, repositorio) = cargado(Result.failure(ErrorDeCarga()))

        viewModel.reintentar()
        assertEquals(AlbumsUiState.Cargando, viewModel.uiState.value)
        advanceUntilIdle()
        repositorio.responder(Result.success(listOf(opera)))
        advanceUntilIdle()

        assertEquals(AlbumsUiState.Exito(listOf(opera)), viewModel.uiState.value)
        assertEquals(2, repositorio.consultas.size)
    }

    @Test
    fun reintentarNoHaceNadaFueraDelError() = runTest {
        val (viewModel, repositorio) = cargado(Result.success(listOf(opera)))

        viewModel.reintentar()
        advanceUntilIdle()

        assertEquals(AlbumsUiState.Exito(listOf(opera)), viewModel.uiState.value)
        assertEquals(1, repositorio.consultas.size)
    }

    @Test
    fun actualizarMantieneLaListaVisibleYPublicaLaNueva() = runTest {
        val (viewModel, repositorio) = cargado(Result.success(listOf(opera)))

        viewModel.actualizar()
        assertEquals(AlbumsUiState.Exito(listOf(opera), actualizando = true), viewModel.uiState.value)
        advanceUntilIdle()
        repositorio.responder(Result.success(listOf(opera, buscando)))
        advanceUntilIdle()

        assertEquals(AlbumsUiState.Exito(listOf(opera, buscando)), viewModel.uiState.value)
    }

    @Test
    fun actualizarDesdeVacioPuedeMostrarAlbumes() = runTest {
        val (viewModel, repositorio) = cargado(Result.success(emptyList()))

        viewModel.actualizar()
        assertEquals(AlbumsUiState.Vacio(actualizando = true), viewModel.uiState.value)
        advanceUntilIdle()
        repositorio.responder(Result.success(listOf(buscando)))
        advanceUntilIdle()

        assertEquals(AlbumsUiState.Exito(listOf(buscando)), viewModel.uiState.value)
    }

    @Test
    fun siFallaLaActualizacionConservaLaListaYAvisa() = runTest {
        val (viewModel, repositorio) = cargado(Result.success(listOf(opera)))

        viewModel.actualizar()
        advanceUntilIdle()
        repositorio.responder(Result.failure(ErrorDeCarga()))
        advanceUntilIdle()

        assertEquals(
            AlbumsUiState.Exito(listOf(opera), errorAlActualizar = true),
            viewModel.uiState.value
        )
    }

    @Test
    fun avisoMostradoApagaElAviso() = runTest {
        val (viewModel, repositorio) = cargado(Result.success(listOf(opera)))
        viewModel.actualizar()
        advanceUntilIdle()
        repositorio.responder(Result.failure(ErrorDeCarga()))
        advanceUntilIdle()

        viewModel.avisoMostrado()

        assertEquals(AlbumsUiState.Exito(listOf(opera)), viewModel.uiState.value)
    }

    @Test
    fun siFallaLaActualizacionDelVacioSigueVacioYAvisa() = runTest {
        val (viewModel, repositorio) = cargado(Result.success(emptyList()))

        viewModel.actualizar()
        advanceUntilIdle()
        repositorio.responder(Result.failure(ErrorDeCarga()))
        advanceUntilIdle()

        assertEquals(AlbumsUiState.Vacio(errorAlActualizar = true), viewModel.uiState.value)
    }

    @Test
    fun actualizarNoHaceNadaMientrasCargaNiEnError() = runTest {
        val repositorio = CatalogoFalso()
        val viewModel = AlbumsViewModel(repositorio)
        advanceUntilIdle()

        viewModel.actualizar()
        advanceUntilIdle()
        assertEquals(1, repositorio.consultas.size)

        repositorio.responder(Result.failure(ErrorDeCarga()))
        advanceUntilIdle()
        viewModel.actualizar()
        advanceUntilIdle()

        assertEquals(AlbumsUiState.Error, viewModel.uiState.value)
        assertEquals(1, repositorio.consultas.size)
    }

    @Test
    fun respuestaTardiaNoReemplazaLaConsultaVigente() = runTest {
        val repositorio = CatalogoFalso(ignoraCancelacion = true)
        val (viewModel, _) = cargado(Result.success(listOf(opera)), repositorio)

        viewModel.actualizar()
        advanceUntilIdle()
        viewModel.actualizar()
        advanceUntilIdle()
        try {
            assertEquals(3, repositorio.consultas.size)
            repositorio.consultas[2].complete(Result.success(listOf(opera, buscando)))
            advanceUntilIdle()
            repositorio.consultas[1].complete(Result.success(emptyList()))
            advanceUntilIdle()

            assertEquals(AlbumsUiState.Exito(listOf(opera, buscando)), viewModel.uiState.value)
        } finally {
            repositorio.consultas.forEach { it.complete(Result.success(emptyList())) }
            advanceUntilIdle()
        }
    }

    @Test
    fun unErrorInesperadoNoSeDisfrazaDeErrorDeCarga() {
        val esperado = IllegalStateException("Error de programación")
        val repositorio = object : AlbumRepository(ServiceAdapter("http://localhost/")) {
            override suspend fun obtenerAlbumes(): Result<List<AlbumResumen>> = Result.failure(esperado)
        }
        var estadoFinal: AlbumsUiState? = null

        val error = runCatching {
            runTest {
                val viewModel = AlbumsViewModel(repositorio)
                advanceUntilIdle()
                estadoFinal = viewModel.uiState.value
            }
        }.exceptionOrNull()

        assertTrue("El error inesperado debe propagarse, no mostrarse como error de carga", error != null)
        assertTrue(generateSequence(error) { it.cause }.any { it === esperado })
        assertTrue(estadoFinal != AlbumsUiState.Error)
    }
}
