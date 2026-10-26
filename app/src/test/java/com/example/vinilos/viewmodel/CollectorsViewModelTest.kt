package com.example.vinilos.viewmodel

import com.example.vinilos.MainDispatcherRule
import com.example.vinilos.data.model.Collector
import com.example.vinilos.data.remote.ErrorDeCarga
import com.example.vinilos.data.remote.ServiceAdapter
import com.example.vinilos.data.repository.CollectorRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Repositorio falso: cada llamada devuelve el resultado que se complete a mano. */
private class RepositorioFalso : CollectorRepository(ServiceAdapter("http://localhost/")) {
    val llamadas = mutableListOf<CompletableDeferred<Result<List<Collector>>>>()

    override suspend fun obtenerColeccionistas(): Result<List<Collector>> {
        val respuesta = CompletableDeferred<Result<List<Collector>>>()
        llamadas += respuesta
        return respuesta.await()
    }
}

class CollectorsViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private val manolo = Collector(100, "Manolo Bellon", "manollo@caracol.com.co")
    private val jaime = Collector(101, "Jaime Monsalve", "jmonsalve@rtvc.com.co")
    private val jose = Collector(102, "José Pérez", "jose@correo.com")
    private val todos = listOf(manolo, jaime, jose)

    private val error = Result.failure<List<Collector>>(ErrorDeCarga())

    // ---- 4.1: estados de la carga ----

    @Test
    fun alCrearseEmpiezaCargandoYPideLosColeccionistasUnaVez() = runTest {
        val repo = RepositorioFalso()

        val vm = CollectorsViewModel(repo)
        advanceUntilIdle()

        assertEquals(CollectorsUiState.Cargando, vm.uiState.value)
        assertEquals(1, repo.llamadas.size)
    }

    @Test
    fun cargandoPasaAExitoConLosColeccionistas() = runTest {
        val repo = RepositorioFalso()
        val vm = CollectorsViewModel(repo)
        advanceUntilIdle()

        repo.llamadas[0].complete(Result.success(todos))
        advanceUntilIdle()

        assertEquals(CollectorsUiState.Exito(todos), vm.uiState.value)
    }

    @Test
    fun cargandoPasaAVacioSiNoHayColeccionistas() = runTest {
        val repo = RepositorioFalso()
        val vm = CollectorsViewModel(repo)
        advanceUntilIdle()

        repo.llamadas[0].complete(Result.success(emptyList()))
        advanceUntilIdle()

        assertEquals(CollectorsUiState.Vacio, vm.uiState.value)
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
    fun reintentarTrasUnErrorVuelveACargandoYLuegoAExito() = runTest {
        val repo = RepositorioFalso()
        val vm = CollectorsViewModel(repo)
        advanceUntilIdle()
        repo.llamadas[0].complete(error)
        advanceUntilIdle()

        vm.reintentar()
        advanceUntilIdle()
        assertEquals(CollectorsUiState.Cargando, vm.uiState.value)

        repo.llamadas[1].complete(Result.success(todos))
        advanceUntilIdle()
        assertEquals(CollectorsUiState.Exito(todos), vm.uiState.value)
    }

    @Test
    fun reintentoFallidoVuelveAError() = runTest {
        val repo = RepositorioFalso()
        val vm = CollectorsViewModel(repo)
        advanceUntilIdle()
        repo.llamadas[0].complete(error)
        advanceUntilIdle()

        vm.reintentar()
        advanceUntilIdle()
        assertEquals(CollectorsUiState.Cargando, vm.uiState.value)

        repo.llamadas[1].complete(error)
        advanceUntilIdle()
        assertEquals(CollectorsUiState.Error, vm.uiState.value)
    }

    @Test
    fun reintentarCancelaLaCargaAnteriorYUnaRespuestaTardiaNoPisaLaNueva() = runTest {
        val repo = RepositorioFalso()
        val vm = CollectorsViewModel(repo)
        advanceUntilIdle()

        vm.reintentar()
        advanceUntilIdle()
        repo.llamadas[1].complete(error)
        advanceUntilIdle()
        // La primera consulta responde tarde con datos: no debe cambiar el estado.
        repo.llamadas[0].complete(Result.success(todos))
        advanceUntilIdle()

        assertEquals(CollectorsUiState.Error, vm.uiState.value)
    }

    // ---- 4.2: búsqueda ----

    private suspend fun kotlinx.coroutines.test.TestScope.vmConDatos(): CollectorsViewModel {
        val repo = RepositorioFalso()
        val vm = CollectorsViewModel(repo)
        advanceUntilIdle()
        repo.llamadas[0].complete(Result.success(todos))
        advanceUntilIdle()
        return vm
    }

    private fun CollectorsViewModel.visibles(): List<Collector> =
        (uiState.value as CollectorsUiState.Exito).visibles

    @Test
    fun sinBusquedaSeVenTodosLosColeccionistas() = runTest {
        val vm = vmConDatos()

        assertEquals(todos, vm.visibles())
        assertEquals("", (vm.uiState.value as CollectorsUiState.Exito).consulta)
    }

    @Test
    fun filtraPorNombre() = runTest {
        val vm = vmConDatos()

        vm.buscar("bel")

        assertEquals(listOf(manolo), vm.visibles())
    }

    @Test
    fun filtraPorCorreo() = runTest {
        val vm = vmConDatos()

        vm.buscar("rtvc")

        assertEquals(listOf(jaime), vm.visibles())
    }

    @Test
    fun noDistingueMayusculasDeMinusculas() = runTest {
        val vm = vmConDatos()

        vm.buscar("JAIME")

        assertEquals(listOf(jaime), vm.visibles())
    }

    @Test
    fun noDistingueTildesEnLaBusquedaNiEnLosDatos() = runTest {
        val vm = vmConDatos()

        vm.buscar("monsálve")
        assertEquals(listOf(jaime), vm.visibles())

        vm.buscar("jose perez")
        assertEquals(listOf(jose), vm.visibles())
    }

    @Test
    fun textoVacioODeEspaciosMuestraTodos() = runTest {
        val vm = vmConDatos()
        vm.buscar("bel")

        vm.buscar("")
        assertEquals(todos, vm.visibles())

        vm.buscar("   ")
        assertEquals(todos, vm.visibles())
    }

    @Test
    fun ignoraLosEspaciosAlrededorDelTexto() = runTest {
        val vm = vmConDatos()

        vm.buscar("  bel ")

        assertEquals(listOf(manolo), vm.visibles())
    }

    @Test
    fun sinCoincidenciasLaListaVisibleQuedaVaciaPeroSigueEnExito() = runTest {
        val vm = vmConDatos()

        vm.buscar("zzz")

        assertTrue(vm.uiState.value is CollectorsUiState.Exito)
        assertEquals(emptyList<Collector>(), vm.visibles())
    }

    @Test
    fun elTextoDeBusquedaSeConservaEnElEstado() = runTest {
        val vm = vmConDatos()

        vm.buscar("bel")

        // Lo que ve una pantalla recreada (rotación): el mismo ViewModel con el mismo estado.
        val estado = vm.uiState.value as CollectorsUiState.Exito
        assertEquals("bel", estado.consulta)
        assertEquals(listOf(manolo), estado.visibles)
    }

    @Test
    fun buscarMientrasCargaNoHaceNada() = runTest {
        val repo = RepositorioFalso()
        val vm = CollectorsViewModel(repo)
        advanceUntilIdle()

        vm.buscar("bel")

        assertEquals(CollectorsUiState.Cargando, vm.uiState.value)
    }

    @Test
    fun buscarNoVuelveAConsultarAlBackend() = runTest {
        val repo = RepositorioFalso()
        val vm = CollectorsViewModel(repo)
        advanceUntilIdle()
        repo.llamadas[0].complete(Result.success(todos))
        advanceUntilIdle()

        vm.buscar("bel")
        vm.buscar("jaime")
        advanceUntilIdle()

        assertEquals(1, repo.llamadas.size)
    }
}
