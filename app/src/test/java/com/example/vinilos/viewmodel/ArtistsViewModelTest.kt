package com.example.vinilos.viewmodel

import com.example.vinilos.MainDispatcherRule
import com.example.vinilos.data.model.Artist
import com.example.vinilos.data.model.ArtistType
import com.example.vinilos.data.model.ArtistDetail
import com.example.vinilos.data.remote.LoadError
import com.example.vinilos.data.repository.ArtistRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Fake repository: each call returns whatever result is manually completed. */
private class RepositorioArtistasFalso : ArtistRepository {
    val llamadas = mutableListOf<CompletableDeferred<Result<List<Artist>>>>()

    override suspend fun getArtists(): Result<List<Artist>> {
        val respuesta = CompletableDeferred<Result<List<Artist>>>()
        llamadas += respuesta
        return respuesta.await()
    }

    /** List saved on the device (add-cache-consultas). */
    var guardados: List<Artist> = emptyList()

    override suspend fun getCachedArtists(): List<Artist> = guardados

    override suspend fun getDetail(tipo: ArtistType, id: Int): Result<ArtistDetail> = error("No se usa en esta prueba")
    override suspend fun getCachedDetail(tipo: ArtistType, id: Int): ArtistDetail = error("No se usa en esta prueba")
    override suspend fun associateAlbum(tipo: ArtistType, idArtista: Int, idAlbum: Int): Result<Unit> =
        error("No se usa en esta prueba")
}

@OptIn(ExperimentalCoroutinesApi::class)
class ArtistsViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private val queen = Artist(101, "Queen", "https://img/queen.jpg", ArtistType.BANDA)
    private val ruben = Artist(100, "Rubén Blades Bellido de Luna", "https://img/ruben.jpg", ArtistType.MUSICO)
    private val todos = listOf(queen, ruben)

    private val error = Result.failure<List<Artist>>(LoadError())

    private fun cargado(repo: RepositorioArtistasFalso, lista: List<Artist> = todos) {
        repo.llamadas.last().complete(Result.success(lista))
    }

    // ---- 3.1: loading states ----

    @Test
    fun alCrearseEmpiezaLoadingYPideLosArtistasUnaVez() = runTest {
        val repo = RepositorioArtistasFalso()

        val vm = ArtistsViewModel(repo)
        advanceUntilIdle()

        assertEquals(ArtistsUiState.Loading, vm.uiState.value)
        assertEquals(1, repo.llamadas.size)
    }

    @Test
    fun cargandoPasaASuccessConLosArtistas() = runTest {
        val repo = RepositorioArtistasFalso()
        val vm = ArtistsViewModel(repo)
        advanceUntilIdle()

        cargado(repo)
        advanceUntilIdle()

        assertEquals(ArtistsUiState.Success(todos), vm.uiState.value)
    }

    @Test
    fun cargandoPasaAEmptySiNoHayArtistas() = runTest {
        val repo = RepositorioArtistasFalso()
        val vm = ArtistsViewModel(repo)
        advanceUntilIdle()

        cargado(repo, emptyList())
        advanceUntilIdle()

        assertEquals(ArtistsUiState.Empty, vm.uiState.value)
    }

    @Test
    fun cargandoPasaAErrorSiLaConsultaFalla() = runTest {
        val repo = RepositorioArtistasFalso()
        val vm = ArtistsViewModel(repo)
        advanceUntilIdle()

        repo.llamadas[0].complete(error)
        advanceUntilIdle()

        assertEquals(ArtistsUiState.Error, vm.uiState.value)
    }

    @Test
    fun retryTrasErrorVuelveALoadingYLuegoASuccess() = runTest {
        val repo = RepositorioArtistasFalso()
        val vm = ArtistsViewModel(repo)
        advanceUntilIdle()
        repo.llamadas[0].complete(error)
        advanceUntilIdle()

        vm.retry()
        advanceUntilIdle()
        assertEquals(ArtistsUiState.Loading, vm.uiState.value)
        assertEquals(2, repo.llamadas.size)

        repo.llamadas[1].complete(Result.success(todos))
        advanceUntilIdle()
        assertEquals(ArtistsUiState.Success(todos), vm.uiState.value)
    }

    @Test
    fun retryQueVuelveAFallarTerminaEnError() = runTest {
        val repo = RepositorioArtistasFalso()
        val vm = ArtistsViewModel(repo)
        advanceUntilIdle()
        repo.llamadas[0].complete(error)
        advanceUntilIdle()

        vm.retry()
        advanceUntilIdle()
        repo.llamadas[1].complete(error)
        advanceUntilIdle()

        assertEquals(ArtistsUiState.Error, vm.uiState.value)
    }

    @Test
    fun retryCancelaLaCargaAnterior() = runTest {
        val repo = RepositorioArtistasFalso()
        val vm = ArtistsViewModel(repo)
        advanceUntilIdle()

        vm.retry()
        advanceUntilIdle()
        // The first query responds late: its result must not override the second one.
        repo.llamadas[1].complete(Result.success(listOf(queen)))
        repo.llamadas[0].complete(error)
        advanceUntilIdle()

        assertEquals(ArtistsUiState.Success(listOf(queen)), vm.uiState.value)
    }

    // ---- 3.2: search by name ----

    private fun exito(vm: ArtistsViewModel) = vm.uiState.value as ArtistsUiState.Success

    @Test
    fun searchFiltraPorNombreEnCualquierParte() = runTest {
        val repo = RepositorioArtistasFalso()
        val vm = ArtistsViewModel(repo)
        advanceUntilIdle()
        cargado(repo)
        advanceUntilIdle()

        vm.search("que")

        assertEquals(listOf(queen), exito(vm).visibles)
        assertEquals("que", exito(vm).consulta)
        assertEquals(todos, exito(vm).artistas)
    }

    @Test
    fun searchNoDistingueMayusculasNiTildes() = runTest {
        val repo = RepositorioArtistasFalso()
        val vm = ArtistsViewModel(repo)
        advanceUntilIdle()
        cargado(repo)
        advanceUntilIdle()

        vm.search("RUBEN")
        assertEquals(listOf(ruben), exito(vm).visibles)

        vm.search("bellído")
        assertEquals(listOf(ruben), exito(vm).visibles)
    }

    @Test
    fun textoEmptyODeEspaciosMuestraTodos() = runTest {
        val repo = RepositorioArtistasFalso()
        val vm = ArtistsViewModel(repo)
        advanceUntilIdle()
        cargado(repo)
        advanceUntilIdle()

        vm.search("que")
        vm.search("   ")

        assertEquals(todos, exito(vm).visibles)
    }

    @Test
    fun busquedaSinCoincidenciasDejaLaListaVisibleVacia() = runTest {
        val repo = RepositorioArtistasFalso()
        val vm = ArtistsViewModel(repo)
        advanceUntilIdle()
        cargado(repo)
        advanceUntilIdle()

        vm.search("zzz")

        assertTrue(exito(vm).visibles.isEmpty())
        assertEquals("zzz", exito(vm).consulta)
    }

    @Test
    fun searchNoTieneEfectoMientrasCargaNiEnError() = runTest {
        val repo = RepositorioArtistasFalso()
        val vm = ArtistsViewModel(repo)
        advanceUntilIdle()

        vm.search("que")
        assertEquals(ArtistsUiState.Loading, vm.uiState.value)

        repo.llamadas[0].complete(error)
        advanceUntilIdle()
        vm.search("que")
        assertEquals(ArtistsUiState.Error, vm.uiState.value)
    }

    // ---------- Saved list (add-cache-consultas, D5) ----------

    private fun repoConGuardados() = RepositorioArtistasFalso().apply { guardados = listOf(queen) }

    @Test
    fun conListadoGuardadoSeMuestraDeInmediatoYLuegoLaRespuesta() = runTest {
        val repo = repoConGuardados()
        val vm = ArtistsViewModel(repo)
        advanceUntilIdle()

        assertEquals(ArtistsUiState.Success(listOf(queen), actualizando = true), vm.uiState.value)

        cargado(repo)
        advanceUntilIdle()
        assertEquals(ArtistsUiState.Success(todos), vm.uiState.value)
    }

    @Test
    fun conListadoGuardadoYRedMalSeConservaConElAviso() = runTest {
        val repo = repoConGuardados()
        val vm = ArtistsViewModel(repo)
        advanceUntilIdle()

        repo.llamadas.single().complete(error)
        advanceUntilIdle()

        assertEquals(ArtistsUiState.Success(listOf(queen), errorAlActualizar = true), vm.uiState.value)
    }

    @Test
    fun laBusquedaSobreElListadoGuardadoSeConservaAlLlegarLaRespuesta() = runTest {
        val repo = repoConGuardados()
        val vm = ArtistsViewModel(repo)
        advanceUntilIdle()

        vm.search("rub")
        cargado(repo)
        advanceUntilIdle()

        val estado = vm.uiState.value as ArtistsUiState.Success
        assertEquals("rub", estado.consulta)
        assertEquals(listOf(ruben), estado.visibles)
    }

    @Test
    fun avisoMostradoApagaElAvisoSinQuitarElListado() = runTest {
        val repo = repoConGuardados()
        val vm = ArtistsViewModel(repo)
        advanceUntilIdle()
        repo.llamadas.single().complete(error)
        advanceUntilIdle()

        vm.avisoMostrado()

        assertEquals(ArtistsUiState.Success(listOf(queen)), vm.uiState.value)
    }
}
