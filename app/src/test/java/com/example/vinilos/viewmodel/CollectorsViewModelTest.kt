package com.example.vinilos.viewmodel

import com.example.vinilos.MainDispatcherRule
import com.example.vinilos.data.model.Collector
import com.example.vinilos.data.remote.CollectorApi
import com.example.vinilos.data.remote.CollectorDto
import com.example.vinilos.data.repository.CollectorRepository
import com.example.vinilos.data.repository.ErrorDeCarga
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Repositorio falso: cada llamada devuelve el resultado que se complete a mano. */
private class RepositorioFalso : CollectorRepository(
    object : CollectorApi {
        override suspend fun getCollectors(): List<CollectorDto> = error("no se usa")
    }
) {
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
        // Given
        val repo = RepositorioFalso()

        // When
        val vm = CollectorsViewModel(repo)
        advanceUntilIdle()

        // Then
        assertEquals(CollectorsUiState.Cargando, vm.uiState.value)
        assertEquals(1, repo.llamadas.size)
    }

    @Test
    fun cargandoPasaAExitoConLosColeccionistas() = runTest {
        // Given
        val repo = RepositorioFalso()
        val vm = CollectorsViewModel(repo)
        advanceUntilIdle()

        // When
        repo.llamadas[0].complete(Result.success(todos))
        advanceUntilIdle()

        // Then
        assertEquals(CollectorsUiState.Exito(todos), vm.uiState.value)
    }

    @Test
    fun cargandoPasaAVacioSiNoHayColeccionistas() = runTest {
        // Given
        val repo = RepositorioFalso()
        val vm = CollectorsViewModel(repo)
        advanceUntilIdle()

        // When
        repo.llamadas[0].complete(Result.success(emptyList()))
        advanceUntilIdle()

        // Then
        assertEquals(CollectorsUiState.Vacio, vm.uiState.value)
    }

    @Test
    fun cargandoPasaAErrorSiFallaLaConsulta() = runTest {
        // Given
        val repo = RepositorioFalso()
        val vm = CollectorsViewModel(repo)
        advanceUntilIdle()

        // When
        repo.llamadas[0].complete(error)
        advanceUntilIdle()

        // Then
        assertEquals(CollectorsUiState.Error, vm.uiState.value)
    }

    @Test
    fun reintentarTrasUnErrorVuelveACargando() = runTest {
        // Given
        val repo = RepositorioFalso()
        val vm = CollectorsViewModel(repo)
        advanceUntilIdle()
        repo.llamadas[0].complete(error)
        advanceUntilIdle()

        // When
        vm.reintentar()
        advanceUntilIdle()

        // Then
        assertEquals(CollectorsUiState.Cargando, vm.uiState.value)
    }

    @Test
    fun reintentarTrasUnErrorYResponderConDatosPasaAExito() = runTest {
        // Given
        val repo = RepositorioFalso()
        val vm = CollectorsViewModel(repo)
        advanceUntilIdle()
        repo.llamadas[0].complete(error)
        advanceUntilIdle()
        vm.reintentar()
        advanceUntilIdle()

        // When
        repo.llamadas[1].complete(Result.success(todos))
        advanceUntilIdle()

        // Then
        assertEquals(CollectorsUiState.Exito(todos), vm.uiState.value)
    }

    @Test
    fun reintentoFallidoVuelveAError() = runTest {
        // Given
        val repo = RepositorioFalso()
        val vm = CollectorsViewModel(repo)
        advanceUntilIdle()
        repo.llamadas[0].complete(error)
        advanceUntilIdle()
        vm.reintentar()
        advanceUntilIdle()

        // When
        repo.llamadas[1].complete(error)
        advanceUntilIdle()

        // Then
        assertEquals(CollectorsUiState.Error, vm.uiState.value)
    }

    @Test
    fun reintentarCancelaLaCargaAnteriorYUnaRespuestaTardiaNoPisaLaNueva() = runTest {
        // Given
        val repo = RepositorioFalso()
        val vm = CollectorsViewModel(repo)
        advanceUntilIdle()
        vm.reintentar()
        advanceUntilIdle()
        repo.llamadas[1].complete(error)
        advanceUntilIdle()

        // When: la primera consulta responde tarde con datos
        repo.llamadas[0].complete(Result.success(todos))
        advanceUntilIdle()

        // Then
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
        // Given
        val vm = vmConDatos()

        // When
        val visibles = vm.visibles()

        // Then
        assertEquals(todos, visibles)
        assertEquals("", (vm.uiState.value as CollectorsUiState.Exito).consulta)
    }

    @Test
    fun filtraPorNombre() = runTest {
        // Given
        val vm = vmConDatos()

        // When
        vm.buscar("bel")

        // Then
        assertEquals(listOf(manolo), vm.visibles())
    }

    @Test
    fun filtraPorCorreo() = runTest {
        // Given
        val vm = vmConDatos()

        // When
        vm.buscar("rtvc")

        // Then
        assertEquals(listOf(jaime), vm.visibles())
    }

    @Test
    fun noDistingueMayusculasDeMinusculas() = runTest {
        // Given
        val vm = vmConDatos()

        // When
        vm.buscar("JAIME")

        // Then
        assertEquals(listOf(jaime), vm.visibles())
    }

    @Test
    fun noDistingueTildesEnLaBusqueda() = runTest {
        // Given
        val vm = vmConDatos()

        // When
        vm.buscar("monsálve")

        // Then
        assertEquals(listOf(jaime), vm.visibles())
    }

    @Test
    fun noDistingueTildesEnLosDatos() = runTest {
        // Given
        val vm = vmConDatos()

        // When
        vm.buscar("jose perez")

        // Then
        assertEquals(listOf(jose), vm.visibles())
    }

    @Test
    fun textoVacioMuestraTodos() = runTest {
        // Given
        val vm = vmConDatos()
        vm.buscar("bel")

        // When
        vm.buscar("")

        // Then
        assertEquals(todos, vm.visibles())
    }

    @Test
    fun textoDeEspaciosMuestraTodos() = runTest {
        // Given
        val vm = vmConDatos()
        vm.buscar("bel")

        // When
        vm.buscar("   ")

        // Then
        assertEquals(todos, vm.visibles())
    }

    @Test
    fun ignoraLosEspaciosAlrededorDelTexto() = runTest {
        // Given
        val vm = vmConDatos()

        // When
        vm.buscar("  bel ")

        // Then
        assertEquals(listOf(manolo), vm.visibles())
    }

    @Test
    fun sinCoincidenciasLaListaVisibleQuedaVaciaPeroSigueEnExito() = runTest {
        // Given
        val vm = vmConDatos()

        // When
        vm.buscar("zzz")

        // Then
        assertTrue(vm.uiState.value is CollectorsUiState.Exito)
        assertEquals(emptyList<Collector>(), vm.visibles())
    }

    @Test
    fun elTextoDeBusquedaSeConservaEnElEstado() = runTest {
        // Given
        val vm = vmConDatos()

        // When
        vm.buscar("bel")

        // Then: lo que ve una pantalla recreada (rotación) es el mismo ViewModel con el mismo estado
        val estado = vm.uiState.value as CollectorsUiState.Exito
        assertEquals("bel", estado.consulta)
        assertEquals(listOf(manolo), estado.visibles)
    }

    @Test
    fun buscarMientrasCargaNoHaceNada() = runTest {
        // Given
        val repo = RepositorioFalso()
        val vm = CollectorsViewModel(repo)
        advanceUntilIdle()

        // When
        vm.buscar("bel")

        // Then
        assertEquals(CollectorsUiState.Cargando, vm.uiState.value)
    }

    @Test
    fun buscarNoVuelveAConsultarAlBackend() = runTest {
        // Given
        val repo = RepositorioFalso()
        val vm = CollectorsViewModel(repo)
        advanceUntilIdle()
        repo.llamadas[0].complete(Result.success(todos))
        advanceUntilIdle()

        // When
        vm.buscar("bel")
        vm.buscar("jaime")
        advanceUntilIdle()

        // Then
        assertEquals(1, repo.llamadas.size)
    }
}
