package com.example.vinilos.viewmodel

import com.example.vinilos.MainDispatcherRule
import com.example.vinilos.data.model.Collector
import com.example.vinilos.data.remote.LoadError
import com.example.vinilos.data.repository.CollectorRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Fake repository: each call returns the result that is manually completed. */
private class RepositorioFalso : CollectorRepository {
    val llamadas = mutableListOf<CompletableDeferred<Result<List<Collector>>>>()

    /** List saved on the device (add-cache-consultas). */
    var guardados: List<Collector> = emptyList()

    override suspend fun getCachedCollectors(): List<Collector> = guardados

    override suspend fun getCollectors(): Result<List<Collector>> {
        val respuesta = CompletableDeferred<Result<List<Collector>>>()
        llamadas += respuesta
        return respuesta.await()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class CollectorsViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private val manolo = Collector(100, "Manolo Bellon", "manollo@caracol.com.co")
    private val jaime = Collector(101, "Jaime Monsalve", "jmonsalve@rtvc.com.co")
    private val jose = Collector(102, "José Pérez", "jose@correo.com")
    private val todos = listOf(manolo, jaime, jose)

    private val error = Result.failure<List<Collector>>(LoadError())

    // ---- 4.1: loading states ----

    @Test
    fun alCrearseEmpiezaLoadingYPideLosColeccionistasUnaVez() = runTest {
        val repo = RepositorioFalso()

        val vm = CollectorsViewModel(repo)
        advanceUntilIdle()

        assertEquals(CollectorsUiState.Loading, vm.uiState.value)
        assertEquals(1, repo.llamadas.size)
    }

    @Test
    fun cargandoPasaASuccessConLosColeccionistas() = runTest {
        val repo = RepositorioFalso()
        val vm = CollectorsViewModel(repo)
        advanceUntilIdle()

        repo.llamadas[0].complete(Result.success(todos))
        advanceUntilIdle()

        assertEquals(CollectorsUiState.Success(todos), vm.uiState.value)
    }

    @Test
    fun cargandoPasaAEmptySiNoHayColeccionistas() = runTest {
        val repo = RepositorioFalso()
        val vm = CollectorsViewModel(repo)
        advanceUntilIdle()

        repo.llamadas[0].complete(Result.success(emptyList()))
        advanceUntilIdle()

        assertEquals(CollectorsUiState.Empty, vm.uiState.value)
    }

    @Test
    fun cargandoPasaAErrorSiFallaLaConsulta() = runTest {
        val repo = RepositorioFalso()
        val vm = CollectorsViewModel(repo)
        advanceUntilIdle()

        repo.llamadas[0].complete(error)
        advanceUntilIdle()

        assertEquals(CollectorsUiState.Error, vm.uiState.value)
    }

    @Test
    fun retryTrasUnErrorVuelveALoadingYLuegoASuccess() = runTest {
        val repo = RepositorioFalso()
        val vm = CollectorsViewModel(repo)
        advanceUntilIdle()
        repo.llamadas[0].complete(error)
        advanceUntilIdle()

        vm.retry()
        advanceUntilIdle()
        assertEquals(CollectorsUiState.Loading, vm.uiState.value)

        repo.llamadas[1].complete(Result.success(todos))
        advanceUntilIdle()
        assertEquals(CollectorsUiState.Success(todos), vm.uiState.value)
    }

    @Test
    fun reintentoFallidoVuelveAError() = runTest {
        val repo = RepositorioFalso()
        val vm = CollectorsViewModel(repo)
        advanceUntilIdle()
        repo.llamadas[0].complete(error)
        advanceUntilIdle()

        vm.retry()
        advanceUntilIdle()
        assertEquals(CollectorsUiState.Loading, vm.uiState.value)

        repo.llamadas[1].complete(error)
        advanceUntilIdle()
        assertEquals(CollectorsUiState.Error, vm.uiState.value)
    }

    @Test
    fun retryCancelaLaCargaAnteriorYUnaRespuestaTardiaNoPisaLaNueva() = runTest {
        val repo = RepositorioFalso()
        val vm = CollectorsViewModel(repo)
        advanceUntilIdle()

        vm.retry()
        advanceUntilIdle()
        repo.llamadas[1].complete(error)
        advanceUntilIdle()
        // The first query responds late with data: it should not change the state.
        repo.llamadas[0].complete(Result.success(todos))
        advanceUntilIdle()

        assertEquals(CollectorsUiState.Error, vm.uiState.value)
    }

    // ---- 4.2: search ----

    private fun kotlinx.coroutines.test.TestScope.vmConDatos(): CollectorsViewModel {
        val repo = RepositorioFalso()
        val vm = CollectorsViewModel(repo)
        advanceUntilIdle()
        repo.llamadas[0].complete(Result.success(todos))
        advanceUntilIdle()
        return vm
    }

    private fun CollectorsViewModel.visibles(): List<Collector> =
        (uiState.value as CollectorsUiState.Success).visibles

    @Test
    fun sinBusquedaSeVenTodosLosColeccionistas() = runTest {
        val vm = vmConDatos()

        assertEquals(todos, vm.visibles())
        assertEquals("", (vm.uiState.value as CollectorsUiState.Success).consulta)
    }

    @Test
    fun filtraPorNombre() = runTest {
        val vm = vmConDatos()

        vm.search("bel")

        assertEquals(listOf(manolo), vm.visibles())
    }

    @Test
    fun filtraPorCorreo() = runTest {
        val vm = vmConDatos()

        vm.search("rtvc")

        assertEquals(listOf(jaime), vm.visibles())
    }

    @Test
    fun noDistingueMayusculasDeMinusculas() = runTest {
        val vm = vmConDatos()

        vm.search("JAIME")

        assertEquals(listOf(jaime), vm.visibles())
    }

    @Test
    fun noDistingueTildesEnLaBusquedaNiEnLosDatos() = runTest {
        val vm = vmConDatos()

        vm.search("monsálve")
        assertEquals(listOf(jaime), vm.visibles())

        vm.search("jose perez")
        assertEquals(listOf(jose), vm.visibles())
    }

    @Test
    fun textoEmptyODeEspaciosMuestraTodos() = runTest {
        val vm = vmConDatos()
        vm.search("bel")

        vm.search("")
        assertEquals(todos, vm.visibles())

        vm.search("   ")
        assertEquals(todos, vm.visibles())
    }

    @Test
    fun ignoraLosEspaciosAlrededorDelTexto() = runTest {
        val vm = vmConDatos()

        vm.search("  bel ")

        assertEquals(listOf(manolo), vm.visibles())
    }

    @Test
    fun sinCoincidenciasLaListaVisibleQuedaVaciaPeroSigueEnSuccess() = runTest {
        val vm = vmConDatos()

        vm.search("zzz")

        assertTrue(vm.uiState.value is CollectorsUiState.Success)
        assertEquals(emptyList<Collector>(), vm.visibles())
    }

    @Test
    fun elTextoDeBusquedaSeConservaEnElEstado() = runTest {
        val vm = vmConDatos()

        vm.search("bel")

        // What a recreated screen (rotation) sees: the same ViewModel with the same state.
        val estado = vm.uiState.value as CollectorsUiState.Success
        assertEquals("bel", estado.consulta)
        assertEquals(listOf(manolo), estado.visibles)
    }

    @Test
    fun searchMientrasCargaNoHaceNada() = runTest {
        val repo = RepositorioFalso()
        val vm = CollectorsViewModel(repo)
        advanceUntilIdle()

        vm.search("bel")

        assertEquals(CollectorsUiState.Loading, vm.uiState.value)
    }

    @Test
    fun searchNoVuelveAConsultarAlBackend() = runTest {
        val repo = RepositorioFalso()
        val vm = CollectorsViewModel(repo)
        advanceUntilIdle()
        repo.llamadas[0].complete(Result.success(todos))
        advanceUntilIdle()

        vm.search("bel")
        vm.search("jaime")
        advanceUntilIdle()

        assertEquals(1, repo.llamadas.size)
    }

    // ---------- Saved list (add-cache-consultas, D5) ----------

    private fun repoConGuardados() = RepositorioFalso().apply { guardados = listOf(manolo, jaime) }

    @Test
    fun conListadoGuardadoSeMuestraDeInmediatoYLuegoLaRespuesta() = runTest {
        val repo = repoConGuardados()
        val vm = CollectorsViewModel(repo)
        advanceUntilIdle()

        assertEquals(CollectorsUiState.Success(listOf(manolo, jaime), actualizando = true), vm.uiState.value)

        repo.llamadas.single().complete(Result.success(todos))
        advanceUntilIdle()

        assertEquals(CollectorsUiState.Success(todos), vm.uiState.value)
    }

    @Test
    fun conListadoGuardadoYRedMalSeConservaConElAviso() = runTest {
        val repo = repoConGuardados()
        val vm = CollectorsViewModel(repo)
        advanceUntilIdle()

        repo.llamadas.single().complete(error)
        advanceUntilIdle()

        assertEquals(CollectorsUiState.Success(listOf(manolo, jaime), errorAlActualizar = true), vm.uiState.value)
    }

    @Test
    fun conListadoGuardadoYRespuestaVaciaTerminaEnEmpty() = runTest {
        val repo = repoConGuardados()
        val vm = CollectorsViewModel(repo)
        advanceUntilIdle()

        repo.llamadas.single().complete(Result.success(emptyList()))
        advanceUntilIdle()

        assertEquals(CollectorsUiState.Empty, vm.uiState.value)
    }

    @Test
    fun laBusquedaFuncionaSobreElListadoGuardadoSinPerderElAviso() = runTest {
        val repo = repoConGuardados()
        val vm = CollectorsViewModel(repo)
        advanceUntilIdle()
        repo.llamadas.single().complete(error)
        advanceUntilIdle()

        vm.search("jaime")

        val estado = vm.uiState.value as CollectorsUiState.Success
        assertEquals(listOf(jaime), estado.visibles)
        assertTrue(estado.errorAlActualizar)
    }

    @Test
    fun avisoMostradoApagaElAvisoSinQuitarElListadoNiLaBusqueda() = runTest {
        val repo = repoConGuardados()
        val vm = CollectorsViewModel(repo)
        advanceUntilIdle()
        repo.llamadas.single().complete(error)
        advanceUntilIdle()
        vm.search("jaime")

        vm.avisoMostrado()

        assertEquals(CollectorsUiState.Success(listOf(manolo, jaime), consulta = "jaime"), vm.uiState.value)
    }
}
