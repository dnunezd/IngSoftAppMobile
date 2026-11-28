package com.example.vinilos.viewmodel

import com.example.vinilos.MainDispatcherRule
import com.example.vinilos.data.model.Album
import com.example.vinilos.data.model.NewAlbum
import com.example.vinilos.data.model.AlbumSummary
import com.example.vinilos.data.remote.LoadError
import com.example.vinilos.data.repository.AlbumRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Reading the cached data and each query remain pending until the test completes them. */
private class CatalogoConCacheFalso : AlbumRepository {
    val lecturas = mutableListOf<CompletableDeferred<List<AlbumSummary>>>()
    val consultas = mutableListOf<CompletableDeferred<Result<List<AlbumSummary>>>>()

    override suspend fun getCachedAlbums(): List<AlbumSummary> =
        CompletableDeferred<List<AlbumSummary>>().also { lecturas += it }.await()
    override suspend fun getCachedAlbum(id: Int): Album = error("No se usa en esta prueba")

    override suspend fun getAlbums(): Result<List<AlbumSummary>> =
        CompletableDeferred<Result<List<AlbumSummary>>>().also { consultas += it }.await()

    fun leer(guardados: List<AlbumSummary>) = lecturas.last().complete(guardados)

    fun responder(resultado: Result<List<AlbumSummary>>) = consultas.last().complete(resultado)

    override suspend fun getAlbum(id: Int): Result<Album> = error("No se usa en esta prueba")
    override suspend fun createAlbum(album: NewAlbum): Result<Int> = error("No se usa en esta prueba")
    override suspend fun addTrack(
        albumId: Int,
        cancion: com.example.vinilos.data.model.Track
    ): Result<Unit> = error("No se usa en esta prueba")
}

/** Initial load with a cached catalog (add-cache-catalogo, D5). */
@OptIn(ExperimentalCoroutinesApi::class)
class AlbumsViewModelCacheTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private val opera = AlbumSummary(102, "A Night at the Opera", null, "Rock", "Queen", "1975")
    private val buscando = AlbumSummary(100, "Buscando América", null, "Salsa", "Rubén Blades", "1984")
    private val poeta = AlbumSummary(101, "Poeta del pueblo", null, "Salsa", "Rubén Blades", "1979")

    private fun TestScope.abrir(guardados: List<AlbumSummary>): Pair<AlbumsViewModel, CatalogoConCacheFalso> {
        val repositorio = CatalogoConCacheFalso()
        val viewModel = AlbumsViewModel(repositorio)
        advanceUntilIdle()
        repositorio.leer(guardados)
        advanceUntilIdle()
        return viewModel to repositorio
    }

    @Test
    fun sinGuardadosYConRedPasaDeLoadingASuccess() = runTest {
        val repositorio = CatalogoConCacheFalso()
        val viewModel = AlbumsViewModel(repositorio)
        assertEquals(AlbumsUiState.Loading, viewModel.uiState.value)
        advanceUntilIdle()
        repositorio.leer(emptyList())
        advanceUntilIdle()
        assertEquals(AlbumsUiState.Loading, viewModel.uiState.value)

        repositorio.responder(Result.success(listOf(opera, buscando)))
        advanceUntilIdle()

        assertEquals(AlbumsUiState.Success(listOf(opera, buscando)), viewModel.uiState.value)
    }

    @Test
    fun sinGuardadosYSinRedTerminaEnError() = runTest {
        val (viewModel, repositorio) = abrir(emptyList())

        repositorio.responder(Result.failure(LoadError()))
        advanceUntilIdle()

        assertEquals(AlbumsUiState.Error, viewModel.uiState.value)
    }

    @Test
    fun conGuardadosLosMuestraDeInmediatoMientrasConsulta() = runTest {
        val (viewModel, repositorio) = abrir(listOf(opera, buscando))

        assertEquals(AlbumsUiState.Success(listOf(opera, buscando), actualizando = true), viewModel.uiState.value)
        assertEquals(1, repositorio.consultas.size)

        repositorio.responder(Result.success(listOf(opera, buscando, poeta)))
        advanceUntilIdle()

        assertEquals(AlbumsUiState.Success(listOf(opera, buscando, poeta)), viewModel.uiState.value)
    }

    @Test
    fun conGuardadosYSinRedConservaLosGuardadosYAvisa() = runTest {
        val (viewModel, repositorio) = abrir(listOf(opera, buscando))

        repositorio.responder(Result.failure(LoadError()))
        advanceUntilIdle()

        assertEquals(
            AlbumsUiState.Success(listOf(opera, buscando), errorAlActualizar = true),
            viewModel.uiState.value
        )

        viewModel.avisoMostrado()

        assertEquals(AlbumsUiState.Success(listOf(opera, buscando)), viewModel.uiState.value)
    }

    @Test
    fun conGuardadosYRespuestaVaciaTerminaEnEmpty() = runTest {
        val (viewModel, repositorio) = abrir(listOf(opera))

        repositorio.responder(Result.success(emptyList()))
        advanceUntilIdle()

        assertEquals(AlbumsUiState.Empty(), viewModel.uiState.value)
    }

    @Test
    fun unAlbumCreadoDuranteLaLecturaDeLoGuardadoGana() = runTest {
        val repositorio = CatalogoConCacheFalso()
        val viewModel = AlbumsViewModel(repositorio)
        advanceUntilIdle()

        // While the cached data is being read the state is Loading, so albumCreado reloads it.
        viewModel.albumCreado(103)
        advanceUntilIdle()
        repositorio.lecturas.first().complete(listOf(opera))
        repositorio.leer(emptyList())
        advanceUntilIdle()
        repositorio.responder(Result.success(listOf(opera, buscando)))
        advanceUntilIdle()

        assertEquals(2, repositorio.lecturas.size)
        assertEquals(1, repositorio.consultas.size)
        assertEquals(AlbumsUiState.Success(listOf(opera, buscando)), viewModel.uiState.value)
    }

    @Test
    fun actualizarMientrasSeConsultaConGuardadosGanaLaUltimaConsulta() = runTest {
        val (viewModel, repositorio) = abrir(listOf(opera))

        viewModel.actualizar()
        advanceUntilIdle()
        repositorio.consultas.first().complete(Result.success(listOf(opera)))
        repositorio.responder(Result.success(listOf(opera, buscando)))
        advanceUntilIdle()

        assertEquals(2, repositorio.consultas.size)
        assertEquals(AlbumsUiState.Success(listOf(opera, buscando)), viewModel.uiState.value)
    }

    @Test
    fun retryDesdeErrorVuelveALeerLoGuardado() = runTest {
        val (viewModel, repositorio) = abrir(emptyList())
        repositorio.responder(Result.failure(LoadError()))
        advanceUntilIdle()

        viewModel.retry()
        advanceUntilIdle()

        assertEquals(2, repositorio.lecturas.size)
        assertEquals(AlbumsUiState.Loading, viewModel.uiState.value)
    }

    // ---------- Album created without waiting for the query (optimizar-cache-e-imagenes, D7) ----------

    private val abbey = AlbumSummary(205, "Abbey Road", null, "Rock", "", "1969")

    /** Catalog loaded with [opera] and no pending queries. */
    private fun TestScope.catalogoCargado(): Pair<AlbumsViewModel, CatalogoConCacheFalso> {
        val (viewModel, repositorio) = abrir(listOf(opera))
        repositorio.responder(Result.success(listOf(opera)))
        advanceUntilIdle()
        return viewModel to repositorio
    }

    @Test
    fun elAlbumCreadoSeVeAntesDeQueRespondaLaConsulta() = runTest {
        val (viewModel, repositorio) = catalogoCargado()

        viewModel.albumCreado(abbey.id)

        assertEquals(AlbumsUiState.Success(listOf(opera), actualizando = true), viewModel.uiState.value)
        advanceUntilIdle()
        repositorio.leer(listOf(abbey, opera))
        advanceUntilIdle()
        assertEquals(AlbumsUiState.Success(listOf(abbey, opera), actualizando = true), viewModel.uiState.value)
        assertEquals(2, repositorio.consultas.size)

        // Meanwhile, another device added another album: it appears once the query responds.
        repositorio.responder(Result.success(listOf(abbey, opera, poeta)))
        advanceUntilIdle()

        assertEquals(AlbumsUiState.Success(listOf(abbey, opera, poeta)), viewModel.uiState.value)
    }

    @Test
    fun siFallaLaConsultaTrasCrearSeConservaElAlbumCreadoYSeAvisa() = runTest {
        val (viewModel, repositorio) = catalogoCargado()
        viewModel.albumCreado(abbey.id)
        advanceUntilIdle()
        repositorio.leer(listOf(abbey, opera))
        advanceUntilIdle()

        repositorio.responder(Result.failure(LoadError()))
        advanceUntilIdle()

        assertEquals(AlbumsUiState.Success(listOf(abbey, opera), errorAlActualizar = true), viewModel.uiState.value)
        assertEquals(NewAlbumCatalogo(abbey.id), viewModel.albumNuevo.value)
    }

    @Test
    fun sinElAlbumCreadoGuardadoSeActualizaComoAntes() = runTest {
        val (viewModel, repositorio) = catalogoCargado()
        viewModel.albumCreado(abbey.id)
        advanceUntilIdle()

        repositorio.leer(listOf(opera))
        advanceUntilIdle()

        assertEquals(AlbumsUiState.Success(listOf(opera), actualizando = true), viewModel.uiState.value)
        repositorio.responder(Result.failure(LoadError()))
        advanceUntilIdle()
        assertEquals(AlbumsUiState.Success(listOf(opera), errorAlActualizar = true), viewModel.uiState.value)
    }

    @Test
    fun conElCatalogoEmptyElAlbumCreadoGuardadoTambienSeVeDeInmediato() = runTest {
        val (viewModel, repositorio) = abrir(emptyList())
        repositorio.responder(Result.success(emptyList()))
        advanceUntilIdle()

        viewModel.albumCreado(abbey.id)
        advanceUntilIdle()
        repositorio.leer(listOf(abbey))
        advanceUntilIdle()

        assertEquals(AlbumsUiState.Success(listOf(abbey), actualizando = true), viewModel.uiState.value)
    }
}
