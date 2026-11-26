package com.example.vinilos.viewmodel

import com.example.vinilos.MainDispatcherRule
import com.example.vinilos.data.model.Album
import com.example.vinilos.data.model.NewAlbum
import com.example.vinilos.data.model.AlbumSummary
import com.example.vinilos.data.remote.LoadError
import com.example.vinilos.data.repository.AlbumRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Cada consulta queda pendiente hasta que la prueba la completa. */
private class CatalogoFalso(
    private val ignoraCancelacion: Boolean = false
) : AlbumRepository {
    val consultas = mutableListOf<CompletableDeferred<Result<List<AlbumSummary>>>>()

    override suspend fun getAlbums(): Result<List<AlbumSummary>> {
        val respuesta = CompletableDeferred<Result<List<AlbumSummary>>>()
        consultas += respuesta
        return if (ignoraCancelacion) withContext(NonCancellable) { respuesta.await() }
        else respuesta.await()
    }

    fun responder(resultado: Result<List<AlbumSummary>>) = consultas.last().complete(resultado)

    override suspend fun getAlbum(id: Int): Result<Album> = error("No se usa en esta prueba")
    override suspend fun getCachedAlbums(): List<AlbumSummary> = emptyList()
    override suspend fun getCachedAlbum(id: Int): Album = error("No se usa en esta prueba")
    override suspend fun createAlbum(album: NewAlbum): Result<Int> = error("No se usa en esta prueba")
    override suspend fun addTrack(
        albumId: Int,
        cancion: com.example.vinilos.data.model.Track
    ): Result<Unit> = error("No se usa en esta prueba")
}

@OptIn(ExperimentalCoroutinesApi::class)
class AlbumsViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private val opera = AlbumSummary(102, "A Night at the Opera", null, "Rock", "Queen", "1975")
    private val buscando = AlbumSummary(100, "Buscando América", null, "Salsa", "Rubén Blades", "1984")

    private fun kotlinx.coroutines.test.TestScope.cargado(
        resultado: Result<List<AlbumSummary>>,
        repositorio: CatalogoFalso = CatalogoFalso()
    ): Pair<AlbumsViewModel, CatalogoFalso> {
        val viewModel = AlbumsViewModel(repositorio)
        advanceUntilIdle()
        repositorio.responder(resultado)
        advanceUntilIdle()
        return viewModel to repositorio
    }

    @Test
    fun cargandoPasaASuccessConLosAlbumes() = runTest {
        val repositorio = CatalogoFalso()
        val viewModel = AlbumsViewModel(repositorio)
        assertEquals(AlbumsUiState.Loading, viewModel.uiState.value)
        advanceUntilIdle()

        repositorio.responder(Result.success(listOf(opera, buscando)))
        advanceUntilIdle()

        assertEquals(AlbumsUiState.Success(listOf(opera, buscando)), viewModel.uiState.value)
    }

    @Test
    fun cargandoPasaAEmptySinAlbumes() = runTest {
        val (viewModel, _) = cargado(Result.success(emptyList()))

        assertEquals(AlbumsUiState.Empty(), viewModel.uiState.value)
    }

    @Test
    fun cargandoPasaAErrorCuandoFallaLaConsulta() = runTest {
        val (viewModel, _) = cargado(Result.failure(LoadError()))

        assertEquals(AlbumsUiState.Error, viewModel.uiState.value)
    }

    @Test
    fun laConsultaSeHaceUnaSolaVezAlCrearse() = runTest {
        val (_, repositorio) = cargado(Result.success(listOf(opera)))

        assertEquals(1, repositorio.consultas.size)
    }

    @Test
    fun retryDesdeErrorPasaPorLoadingYLlegaASuccess() = runTest {
        val (viewModel, repositorio) = cargado(Result.failure(LoadError()))

        viewModel.retry()
        assertEquals(AlbumsUiState.Loading, viewModel.uiState.value)
        advanceUntilIdle()
        repositorio.responder(Result.success(listOf(opera)))
        advanceUntilIdle()

        assertEquals(AlbumsUiState.Success(listOf(opera)), viewModel.uiState.value)
        assertEquals(2, repositorio.consultas.size)
    }

    @Test
    fun retryNoHaceNadaFueraDelError() = runTest {
        val (viewModel, repositorio) = cargado(Result.success(listOf(opera)))

        viewModel.retry()
        advanceUntilIdle()

        assertEquals(AlbumsUiState.Success(listOf(opera)), viewModel.uiState.value)
        assertEquals(1, repositorio.consultas.size)
    }

    @Test
    fun actualizarMantieneLaListaVisibleYPublicaLaNueva() = runTest {
        val (viewModel, repositorio) = cargado(Result.success(listOf(opera)))

        viewModel.actualizar()
        assertEquals(AlbumsUiState.Success(listOf(opera), actualizando = true), viewModel.uiState.value)
        advanceUntilIdle()
        repositorio.responder(Result.success(listOf(opera, buscando)))
        advanceUntilIdle()

        assertEquals(AlbumsUiState.Success(listOf(opera, buscando)), viewModel.uiState.value)
    }

    @Test
    fun actualizarDesdeEmptyPuedeMostrarAlbumes() = runTest {
        val (viewModel, repositorio) = cargado(Result.success(emptyList()))

        viewModel.actualizar()
        assertEquals(AlbumsUiState.Empty(actualizando = true), viewModel.uiState.value)
        advanceUntilIdle()
        repositorio.responder(Result.success(listOf(buscando)))
        advanceUntilIdle()

        assertEquals(AlbumsUiState.Success(listOf(buscando)), viewModel.uiState.value)
    }

    @Test
    fun siFallaLaActualizacionConservaLaListaYAvisa() = runTest {
        val (viewModel, repositorio) = cargado(Result.success(listOf(opera)))

        viewModel.actualizar()
        advanceUntilIdle()
        repositorio.responder(Result.failure(LoadError()))
        advanceUntilIdle()

        assertEquals(
            AlbumsUiState.Success(listOf(opera), errorAlActualizar = true),
            viewModel.uiState.value
        )
    }

    @Test
    fun avisoMostradoApagaElAviso() = runTest {
        val (viewModel, repositorio) = cargado(Result.success(listOf(opera)))
        viewModel.actualizar()
        advanceUntilIdle()
        repositorio.responder(Result.failure(LoadError()))
        advanceUntilIdle()

        viewModel.avisoMostrado()

        assertEquals(AlbumsUiState.Success(listOf(opera)), viewModel.uiState.value)
    }

    @Test
    fun siFallaLaActualizacionDelEmptySigueEmptyYAvisa() = runTest {
        val (viewModel, repositorio) = cargado(Result.success(emptyList()))

        viewModel.actualizar()
        advanceUntilIdle()
        repositorio.responder(Result.failure(LoadError()))
        advanceUntilIdle()

        assertEquals(AlbumsUiState.Empty(errorAlActualizar = true), viewModel.uiState.value)
    }

    @Test
    fun actualizarNoHaceNadaMientrasCargaNiEnError() = runTest {
        val repositorio = CatalogoFalso()
        val viewModel = AlbumsViewModel(repositorio)
        advanceUntilIdle()

        viewModel.actualizar()
        advanceUntilIdle()
        assertEquals(1, repositorio.consultas.size)

        repositorio.responder(Result.failure(LoadError()))
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

            assertEquals(AlbumsUiState.Success(listOf(opera, buscando)), viewModel.uiState.value)
        } finally {
            repositorio.consultas.forEach { it.complete(Result.success(emptyList())) }
            advanceUntilIdle()
        }
    }

    @Test
    fun unErrorInesperadoNoSeDisfrazaDeLoadError() {
        val esperado = IllegalStateException("Error de programación")
        val repositorio = object : AlbumRepository {
            override suspend fun getAlbums(): Result<List<AlbumSummary>> = Result.failure(esperado)
            override suspend fun getAlbum(id: Int): Result<Album> = error("No se usa en esta prueba")
            override suspend fun getCachedAlbums(): List<AlbumSummary> = emptyList()
            override suspend fun getCachedAlbum(id: Int): Album = error("No se usa en esta prueba")
            override suspend fun createAlbum(album: NewAlbum): Result<Int> = error("No se usa en esta prueba")
            override suspend fun addTrack(
                albumId: Int,
                cancion: com.example.vinilos.data.model.Track
            ): Result<Unit> = error("No se usa en esta prueba")
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

    private val abbey = AlbumSummary(205, "Abbey Road", null, "Rock", "", "1969")

    @Test
    fun albumCreadoConElCatalogoCargadoActualizaSinQuitarLaLista() = runTest {
        val (viewModel, repositorio) = cargado(Result.success(listOf(opera)))

        viewModel.albumCreado(abbey.id)
        advanceUntilIdle()

        assertEquals(AlbumsUiState.Success(listOf(opera), actualizando = true), viewModel.uiState.value)
        assertEquals(2, repositorio.consultas.size)
        repositorio.responder(Result.success(listOf(abbey, opera)))
        advanceUntilIdle()
        assertEquals(AlbumsUiState.Success(listOf(abbey, opera)), viewModel.uiState.value)
    }

    @Test
    fun albumCreadoConElCatalogoEmptyActualiza() = runTest {
        val (viewModel, repositorio) = cargado(Result.success(emptyList()))

        viewModel.albumCreado(abbey.id)
        advanceUntilIdle()
        repositorio.responder(Result.success(listOf(abbey)))
        advanceUntilIdle()

        assertEquals(AlbumsUiState.Success(listOf(abbey)), viewModel.uiState.value)
    }

    @Test
    fun albumCreadoConElCatalogoEnErrorVuelveACargar() = runTest {
        val (viewModel, repositorio) = cargado(Result.failure(LoadError()))

        viewModel.albumCreado(abbey.id)
        assertEquals(AlbumsUiState.Loading, viewModel.uiState.value)
        advanceUntilIdle()
        repositorio.responder(Result.success(listOf(abbey)))
        advanceUntilIdle()

        assertEquals(AlbumsUiState.Success(listOf(abbey)), viewModel.uiState.value)
    }

    @Test
    fun albumCreadoMientrasCargaRepiteLaConsultaParaIncluirlo() = runTest {
        val repositorio = CatalogoFalso()
        val viewModel = AlbumsViewModel(repositorio)
        advanceUntilIdle()

        viewModel.albumCreado(abbey.id)
        advanceUntilIdle()
        repositorio.responder(Result.success(listOf(abbey)))
        advanceUntilIdle()

        assertEquals(2, repositorio.consultas.size)
        assertEquals(AlbumsUiState.Success(listOf(abbey)), viewModel.uiState.value)
    }

    @Test
    fun albumCreadoActivaElAvisoYElDesplazamientoHastaQueSeMarcanHechos() = runTest {
        val (viewModel, _) = cargado(Result.success(listOf(opera)))
        assertNull(viewModel.albumNuevo.value)

        viewModel.albumCreado(abbey.id)

        assertEquals(NewAlbumCatalogo(abbey.id), viewModel.albumNuevo.value)
        viewModel.avisoCreadoMostrado()
        assertEquals(
            NewAlbumCatalogo(abbey.id, avisoPendiente = false),
            viewModel.albumNuevo.value
        )
        viewModel.desplazadoAlNuevo()
        assertEquals(
            NewAlbumCatalogo(abbey.id, avisoPendiente = false, desplazamientoPendiente = false),
            viewModel.albumNuevo.value
        )
    }

    @Test
    fun laMarcaDelNewAlbumSigueTrasDeslizarParaActualizar() = runTest {
        val (viewModel, repositorio) = cargado(Result.success(listOf(opera)))
        viewModel.albumCreado(abbey.id)
        advanceUntilIdle()
        repositorio.responder(Result.success(listOf(abbey, opera)))
        advanceUntilIdle()

        viewModel.actualizar()
        advanceUntilIdle()
        repositorio.responder(Result.success(listOf(abbey, opera)))
        advanceUntilIdle()

        assertEquals(abbey.id, viewModel.albumNuevo.value?.id)
    }

    @Test
    fun unSegundoAlbumReemplazaLaMarca() = runTest {
        val (viewModel, _) = cargado(Result.success(listOf(opera)))
        viewModel.albumCreado(abbey.id)
        viewModel.avisoCreadoMostrado()

        viewModel.albumCreado(300)

        assertEquals(NewAlbumCatalogo(300), viewModel.albumNuevo.value)
    }

    @Test
    fun siFallaLaConsultaPosteriorSeConservaElAvisoDeCreadoYElDeActualizacion() = runTest {
        val (viewModel, repositorio) = cargado(Result.success(listOf(opera)))

        viewModel.albumCreado(abbey.id)
        advanceUntilIdle()
        repositorio.responder(Result.failure(LoadError()))
        advanceUntilIdle()

        assertEquals(
            AlbumsUiState.Success(listOf(opera), errorAlActualizar = true),
            viewModel.uiState.value
        )
        assertEquals(NewAlbumCatalogo(abbey.id), viewModel.albumNuevo.value)
    }
}
