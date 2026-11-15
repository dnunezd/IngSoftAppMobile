package com.example.vinilos.viewmodel

import com.example.vinilos.MainDispatcherRule
import com.example.vinilos.data.model.Album
import com.example.vinilos.data.model.AlbumNuevo
import com.example.vinilos.data.model.AlbumResumen
import com.example.vinilos.data.remote.ErrorDeCarga
import com.example.vinilos.data.repository.AlbumRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** La lectura de lo guardado y cada consulta quedan pendientes hasta que la prueba las completa. */
private class CatalogoConCacheFalso : AlbumRepository {
    val lecturas = mutableListOf<CompletableDeferred<List<AlbumResumen>>>()
    val consultas = mutableListOf<CompletableDeferred<Result<List<AlbumResumen>>>>()

    override suspend fun obtenerAlbumesGuardados(): List<AlbumResumen> =
        CompletableDeferred<List<AlbumResumen>>().also { lecturas += it }.await()
    override suspend fun obtenerAlbumGuardado(id: Int): Album? = error("No se usa en esta prueba")

    override suspend fun obtenerAlbumes(): Result<List<AlbumResumen>> =
        CompletableDeferred<Result<List<AlbumResumen>>>().also { consultas += it }.await()

    fun leer(guardados: List<AlbumResumen>) = lecturas.last().complete(guardados)

    fun responder(resultado: Result<List<AlbumResumen>>) = consultas.last().complete(resultado)

    override suspend fun obtenerAlbum(id: Int): Result<Album> = error("No se usa en esta prueba")
    override suspend fun crearAlbum(album: AlbumNuevo): Result<Int> = error("No se usa en esta prueba")
}

/** Carga inicial con catálogo guardado (add-cache-catalogo, D5). */
@OptIn(ExperimentalCoroutinesApi::class)
class AlbumsViewModelCacheTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private val opera = AlbumResumen(102, "A Night at the Opera", null, "Rock", "Queen", "1975")
    private val buscando = AlbumResumen(100, "Buscando América", null, "Salsa", "Rubén Blades", "1984")
    private val poeta = AlbumResumen(101, "Poeta del pueblo", null, "Salsa", "Rubén Blades", "1979")

    private fun TestScope.abrir(guardados: List<AlbumResumen>): Pair<AlbumsViewModel, CatalogoConCacheFalso> {
        val repositorio = CatalogoConCacheFalso()
        val viewModel = AlbumsViewModel(repositorio)
        advanceUntilIdle()
        repositorio.leer(guardados)
        advanceUntilIdle()
        return viewModel to repositorio
    }

    @Test
    fun sinGuardadosYConRedPasaDeCargandoAExito() = runTest {
        val repositorio = CatalogoConCacheFalso()
        val viewModel = AlbumsViewModel(repositorio)
        assertEquals(AlbumsUiState.Cargando, viewModel.uiState.value)
        advanceUntilIdle()
        repositorio.leer(emptyList())
        advanceUntilIdle()
        assertEquals(AlbumsUiState.Cargando, viewModel.uiState.value)

        repositorio.responder(Result.success(listOf(opera, buscando)))
        advanceUntilIdle()

        assertEquals(AlbumsUiState.Exito(listOf(opera, buscando)), viewModel.uiState.value)
    }

    @Test
    fun sinGuardadosYSinRedTerminaEnError() = runTest {
        val (viewModel, repositorio) = abrir(emptyList())

        repositorio.responder(Result.failure(ErrorDeCarga()))
        advanceUntilIdle()

        assertEquals(AlbumsUiState.Error, viewModel.uiState.value)
    }

    @Test
    fun conGuardadosLosMuestraDeInmediatoMientrasConsulta() = runTest {
        val (viewModel, repositorio) = abrir(listOf(opera, buscando))

        assertEquals(AlbumsUiState.Exito(listOf(opera, buscando), actualizando = true), viewModel.uiState.value)
        assertEquals(1, repositorio.consultas.size)

        repositorio.responder(Result.success(listOf(opera, buscando, poeta)))
        advanceUntilIdle()

        assertEquals(AlbumsUiState.Exito(listOf(opera, buscando, poeta)), viewModel.uiState.value)
    }

    @Test
    fun conGuardadosYSinRedConservaLosGuardadosYAvisa() = runTest {
        val (viewModel, repositorio) = abrir(listOf(opera, buscando))

        repositorio.responder(Result.failure(ErrorDeCarga()))
        advanceUntilIdle()

        assertEquals(
            AlbumsUiState.Exito(listOf(opera, buscando), errorAlActualizar = true),
            viewModel.uiState.value
        )

        viewModel.avisoMostrado()

        assertEquals(AlbumsUiState.Exito(listOf(opera, buscando)), viewModel.uiState.value)
    }

    @Test
    fun conGuardadosYRespuestaVaciaTerminaEnVacio() = runTest {
        val (viewModel, repositorio) = abrir(listOf(opera))

        repositorio.responder(Result.success(emptyList()))
        advanceUntilIdle()

        assertEquals(AlbumsUiState.Vacio(), viewModel.uiState.value)
    }

    @Test
    fun unAlbumCreadoDuranteLaLecturaDeLoGuardadoGana() = runTest {
        val repositorio = CatalogoConCacheFalso()
        val viewModel = AlbumsViewModel(repositorio)
        advanceUntilIdle()

        // Mientras se lee lo guardado el estado es Cargando, así que albumCreado vuelve a cargar.
        viewModel.albumCreado(103)
        advanceUntilIdle()
        repositorio.lecturas.first().complete(listOf(opera))
        repositorio.leer(emptyList())
        advanceUntilIdle()
        repositorio.responder(Result.success(listOf(opera, buscando)))
        advanceUntilIdle()

        assertEquals(2, repositorio.lecturas.size)
        assertEquals(1, repositorio.consultas.size)
        assertEquals(AlbumsUiState.Exito(listOf(opera, buscando)), viewModel.uiState.value)
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
        assertEquals(AlbumsUiState.Exito(listOf(opera, buscando)), viewModel.uiState.value)
    }

    @Test
    fun reintentarDesdeErrorVuelveALeerLoGuardado() = runTest {
        val (viewModel, repositorio) = abrir(emptyList())
        repositorio.responder(Result.failure(ErrorDeCarga()))
        advanceUntilIdle()

        viewModel.reintentar()
        advanceUntilIdle()

        assertEquals(2, repositorio.lecturas.size)
        assertEquals(AlbumsUiState.Cargando, viewModel.uiState.value)
    }

    // ---------- Álbum creado sin esperar la consulta (optimizar-cache-e-imagenes, D7) ----------

    private val abbey = AlbumResumen(205, "Abbey Road", null, "Rock", "", "1969")

    /** Catálogo cargado con [opera] y sin consultas pendientes. */
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

        assertEquals(AlbumsUiState.Exito(listOf(opera), actualizando = true), viewModel.uiState.value)
        advanceUntilIdle()
        repositorio.leer(listOf(abbey, opera))
        advanceUntilIdle()
        assertEquals(AlbumsUiState.Exito(listOf(abbey, opera), actualizando = true), viewModel.uiState.value)
        assertEquals(2, repositorio.consultas.size)

        // Mientras tanto, otro dispositivo agregó otro álbum: aparece cuando responde la consulta.
        repositorio.responder(Result.success(listOf(abbey, opera, poeta)))
        advanceUntilIdle()

        assertEquals(AlbumsUiState.Exito(listOf(abbey, opera, poeta)), viewModel.uiState.value)
    }

    @Test
    fun siFallaLaConsultaTrasCrearSeConservaElAlbumCreadoYSeAvisa() = runTest {
        val (viewModel, repositorio) = catalogoCargado()
        viewModel.albumCreado(abbey.id)
        advanceUntilIdle()
        repositorio.leer(listOf(abbey, opera))
        advanceUntilIdle()

        repositorio.responder(Result.failure(ErrorDeCarga()))
        advanceUntilIdle()

        assertEquals(AlbumsUiState.Exito(listOf(abbey, opera), errorAlActualizar = true), viewModel.uiState.value)
        assertEquals(AlbumNuevoCatalogo(abbey.id), viewModel.albumNuevo.value)
    }

    @Test
    fun sinElAlbumCreadoGuardadoSeActualizaComoAntes() = runTest {
        val (viewModel, repositorio) = catalogoCargado()
        viewModel.albumCreado(abbey.id)
        advanceUntilIdle()

        repositorio.leer(listOf(opera))
        advanceUntilIdle()

        assertEquals(AlbumsUiState.Exito(listOf(opera), actualizando = true), viewModel.uiState.value)
        repositorio.responder(Result.failure(ErrorDeCarga()))
        advanceUntilIdle()
        assertEquals(AlbumsUiState.Exito(listOf(opera), errorAlActualizar = true), viewModel.uiState.value)
    }

    @Test
    fun conElCatalogoVacioElAlbumCreadoGuardadoTambienSeVeDeInmediato() = runTest {
        val (viewModel, repositorio) = abrir(emptyList())
        repositorio.responder(Result.success(emptyList()))
        advanceUntilIdle()

        viewModel.albumCreado(abbey.id)
        advanceUntilIdle()
        repositorio.leer(listOf(abbey))
        advanceUntilIdle()

        assertEquals(AlbumsUiState.Exito(listOf(abbey), actualizando = true), viewModel.uiState.value)
    }
}
