package com.example.vinilos.viewmodel

import com.example.vinilos.MainDispatcherRule
import com.example.vinilos.data.model.Artist
import com.example.vinilos.data.model.ArtistType
import com.example.vinilos.data.remote.ArtistApi
import com.example.vinilos.data.remote.BandDto
import com.example.vinilos.data.remote.MusicianDto
import com.example.vinilos.data.repository.ArtistRepository
import com.example.vinilos.data.repository.ErrorDeCarga
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Repositorio falso: cada llamada devuelve el resultado que se complete a mano. */
private class RepositorioArtistasFalso : ArtistRepository(
    object : ArtistApi {
        override suspend fun getMusicians(): List<MusicianDto> = error("no se usa")
        override suspend fun getBands(): List<BandDto> = error("no se usa")
    }
) {
    val llamadas = mutableListOf<CompletableDeferred<Result<List<Artist>>>>()

    override suspend fun obtenerArtistas(): Result<List<Artist>> {
        val respuesta = CompletableDeferred<Result<List<Artist>>>()
        llamadas += respuesta
        return respuesta.await()
    }
}

class ArtistsViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private val queen = Artist(101, "Queen", "https://img/queen.jpg", ArtistType.BANDA)
    private val ruben = Artist(100, "Rubén Blades Bellido de Luna", "https://img/ruben.jpg", ArtistType.MUSICO)
    private val todos = listOf(queen, ruben)

    private val error = Result.failure<List<Artist>>(ErrorDeCarga())

    private fun cargado(repo: RepositorioArtistasFalso, lista: List<Artist> = todos) {
        repo.llamadas.last().complete(Result.success(lista))
    }

    // ---- 3.1: estados de la carga ----

    @Test
    fun alCrearseEmpiezaCargandoYPideLosArtistasUnaVez() = runTest {
        val repo = RepositorioArtistasFalso()

        val vm = ArtistsViewModel(repo)
        advanceUntilIdle()

        assertEquals(ArtistsUiState.Cargando, vm.uiState.value)
        assertEquals(1, repo.llamadas.size)
    }

    @Test
    fun cargandoPasaAExitoConLosArtistas() = runTest {
        val repo = RepositorioArtistasFalso()
        val vm = ArtistsViewModel(repo)
        advanceUntilIdle()

        cargado(repo)
        advanceUntilIdle()

        assertEquals(ArtistsUiState.Exito(todos), vm.uiState.value)
    }

    @Test
    fun cargandoPasaAVacioSiNoHayArtistas() = runTest {
        val repo = RepositorioArtistasFalso()
        val vm = ArtistsViewModel(repo)
        advanceUntilIdle()

        cargado(repo, emptyList())
        advanceUntilIdle()

        assertEquals(ArtistsUiState.Vacio, vm.uiState.value)
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
    fun reintentarTrasErrorVuelveACargandoYLuegoAExito() = runTest {
        val repo = RepositorioArtistasFalso()
        val vm = ArtistsViewModel(repo)
        advanceUntilIdle()
        repo.llamadas[0].complete(error)
        advanceUntilIdle()

        vm.reintentar()
        advanceUntilIdle()
        assertEquals(ArtistsUiState.Cargando, vm.uiState.value)
        assertEquals(2, repo.llamadas.size)

        repo.llamadas[1].complete(Result.success(todos))
        advanceUntilIdle()
        assertEquals(ArtistsUiState.Exito(todos), vm.uiState.value)
    }

    @Test
    fun reintentarQueVuelveAFallarTerminaEnError() = runTest {
        val repo = RepositorioArtistasFalso()
        val vm = ArtistsViewModel(repo)
        advanceUntilIdle()
        repo.llamadas[0].complete(error)
        advanceUntilIdle()

        vm.reintentar()
        advanceUntilIdle()
        repo.llamadas[1].complete(error)
        advanceUntilIdle()

        assertEquals(ArtistsUiState.Error, vm.uiState.value)
    }

    @Test
    fun reintentarCancelaLaCargaAnterior() = runTest {
        val repo = RepositorioArtistasFalso()
        val vm = ArtistsViewModel(repo)
        advanceUntilIdle()

        vm.reintentar()
        advanceUntilIdle()
        // La primera consulta responde tarde: su resultado no debe pisar a la segunda.
        repo.llamadas[1].complete(Result.success(listOf(queen)))
        repo.llamadas[0].complete(error)
        advanceUntilIdle()

        assertEquals(ArtistsUiState.Exito(listOf(queen)), vm.uiState.value)
    }

    // ---- 3.2: búsqueda por nombre ----

    private fun exito(vm: ArtistsViewModel) = vm.uiState.value as ArtistsUiState.Exito

    @Test
    fun buscarFiltraPorNombreEnCualquierParte() = runTest {
        val repo = RepositorioArtistasFalso()
        val vm = ArtistsViewModel(repo)
        advanceUntilIdle()
        cargado(repo)
        advanceUntilIdle()

        vm.buscar("que")

        assertEquals(listOf(queen), exito(vm).visibles)
        assertEquals("que", exito(vm).consulta)
        assertEquals(todos, exito(vm).artistas)
    }

    @Test
    fun buscarNoDistingueMayusculasNiTildes() = runTest {
        val repo = RepositorioArtistasFalso()
        val vm = ArtistsViewModel(repo)
        advanceUntilIdle()
        cargado(repo)
        advanceUntilIdle()

        vm.buscar("RUBEN")
        assertEquals(listOf(ruben), exito(vm).visibles)

        vm.buscar("bellído")
        assertEquals(listOf(ruben), exito(vm).visibles)
    }

    @Test
    fun textoVacioODeEspaciosMuestraTodos() = runTest {
        val repo = RepositorioArtistasFalso()
        val vm = ArtistsViewModel(repo)
        advanceUntilIdle()
        cargado(repo)
        advanceUntilIdle()

        vm.buscar("que")
        vm.buscar("   ")

        assertEquals(todos, exito(vm).visibles)
    }

    @Test
    fun busquedaSinCoincidenciasDejaLaListaVisibleVacia() = runTest {
        val repo = RepositorioArtistasFalso()
        val vm = ArtistsViewModel(repo)
        advanceUntilIdle()
        cargado(repo)
        advanceUntilIdle()

        vm.buscar("zzz")

        assertTrue(exito(vm).visibles.isEmpty())
        assertEquals("zzz", exito(vm).consulta)
    }

    @Test
    fun buscarNoTieneEfectoMientrasCargaNiEnError() = runTest {
        val repo = RepositorioArtistasFalso()
        val vm = ArtistsViewModel(repo)
        advanceUntilIdle()

        vm.buscar("que")
        assertEquals(ArtistsUiState.Cargando, vm.uiState.value)

        repo.llamadas[0].complete(error)
        advanceUntilIdle()
        vm.buscar("que")
        assertEquals(ArtistsUiState.Error, vm.uiState.value)
    }
}
