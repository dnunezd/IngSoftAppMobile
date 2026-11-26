package com.example.vinilos.viewmodel

import com.example.vinilos.MainDispatcherRule
import com.example.vinilos.data.model.ArtistType
import com.example.vinilos.data.remote.LoadError
import com.example.vinilos.data.repository.ArtistNotAvailable
import com.example.vinilos.data.repository.FavoriteRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

private class FavoriteRepositoryFalso : FavoriteRepository {
    val comprobaciones = mutableListOf<CompletableDeferred<Result<Boolean>>>()
    val guardados = mutableListOf<CompletableDeferred<Result<Unit>>>()

    /** Favoritos confirmados guardados en el dispositivo (add-cache-consultas). */
    var favoritosGuardados: Set<Pair<Int, Int>> = emptySet()
    var lecturasGuardadas = 0

    override suspend fun esFavoritoGuardado(collectorId: Int, artistId: Int): Boolean {
        lecturasGuardadas++
        return (collectorId to artistId) in favoritosGuardados
    }

    override suspend fun esFavorito(collectorId: Int, artistId: Int): Result<Boolean> {
        val respuesta = CompletableDeferred<Result<Boolean>>()
        comprobaciones += respuesta
        return respuesta.await()
    }

    override suspend fun addFavorite(collectorId: Int, artistId: Int, tipo: ArtistType): Result<Unit> {
        val respuesta = CompletableDeferred<Result<Unit>>()
        guardados += respuesta
        return respuesta.await()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class FavoriteViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    @Test
    fun comprobandoPasaAFavoritoCuandoElArtistaYaEsFavorito() = runTest {
        val repositorio = FavoriteRepositoryFalso()
        val viewModel = FavoriteViewModel(100, 8, ArtistType.BANDA, repositorio)

        assertEquals(FavoriteUiState.Comprobando, viewModel.uiState.value)
        advanceUntilIdle()
        repositorio.comprobaciones.single().complete(Result.success(true))
        advanceUntilIdle()

        assertEquals(FavoriteUiState.Favorito(mostrarConfirmacion = false), viewModel.uiState.value)
    }

    @Test
    fun comprobandoPasaANoFavoritoCuandoElArtistaNoEsFavorito() = runTest {
        val repositorio = FavoriteRepositoryFalso()
        val viewModel = FavoriteViewModel(100, 8, ArtistType.BANDA, repositorio)

        advanceUntilIdle()
        repositorio.comprobaciones.single().complete(Result.success(false))
        advanceUntilIdle()

        assertEquals(FavoriteUiState.NoFavorito, viewModel.uiState.value)
    }

    @Test
    fun comprobandoPasaAErrorComprobacionCuandoFallaLaConsulta() = runTest {
        val repositorio = FavoriteRepositoryFalso()
        val viewModel = FavoriteViewModel(100, 8, ArtistType.BANDA, repositorio)

        advanceUntilIdle()
        repositorio.comprobaciones.single().complete(Result.failure(LoadError()))
        advanceUntilIdle()

        assertEquals(FavoriteUiState.ErrorComprobacion, viewModel.uiState.value)
    }

    @Test
    fun retryCheckPasaDeErrorComprobacionAFavoritoOtraVez() = runTest {
        val repositorio = FavoriteRepositoryFalso()
        val viewModel = FavoriteViewModel(100, 8, ArtistType.BANDA, repositorio)
        advanceUntilIdle()
        repositorio.comprobaciones.single().complete(Result.failure(LoadError()))
        advanceUntilIdle()
        assertEquals(FavoriteUiState.ErrorComprobacion, viewModel.uiState.value)

        viewModel.retryCheck()
        assertEquals(FavoriteUiState.Comprobando, viewModel.uiState.value)
        advanceUntilIdle()
        assertEquals(2, repositorio.comprobaciones.size)
        repositorio.comprobaciones.last().complete(Result.success(true))
        advanceUntilIdle()

        assertEquals(FavoriteUiState.Favorito(mostrarConfirmacion = false), viewModel.uiState.value)
    }

    @Test
    fun noFavoritoPasaAGuardandoYLuegoAFavoritoConConfirmacion() = runTest {
        val repositorio = FavoriteRepositoryFalso()
        val viewModel = FavoriteViewModel(100, 8, ArtistType.BANDA, repositorio)
        advanceUntilIdle()
        repositorio.comprobaciones.single().complete(Result.success(false))
        advanceUntilIdle()
        assertEquals(FavoriteUiState.NoFavorito, viewModel.uiState.value)

        viewModel.addFavorite()
        assertEquals(FavoriteUiState.Guardando, viewModel.uiState.value)
        advanceUntilIdle()
        repositorio.guardados.single().complete(Result.success(Unit))
        advanceUntilIdle()

        assertEquals(FavoriteUiState.Favorito(mostrarConfirmacion = true), viewModel.uiState.value)
    }

    @Test
    fun segundoToqueMientrasGuardandoSeIgnora() = runTest {
        val repositorio = FavoriteRepositoryFalso()
        val viewModel = FavoriteViewModel(100, 8, ArtistType.BANDA, repositorio)
        advanceUntilIdle()
        repositorio.comprobaciones.single().complete(Result.success(false))
        advanceUntilIdle()

        viewModel.addFavorite()
        viewModel.addFavorite()
        advanceUntilIdle()

        assertEquals(1, repositorio.guardados.size)
    }

    @Test
    fun saveFavoritoPasaANotAvailableCuandoElArtistaYaNoExiste() = runTest {
        val repositorio = FavoriteRepositoryFalso()
        val viewModel = FavoriteViewModel(100, 8, ArtistType.BANDA, repositorio)
        advanceUntilIdle()
        repositorio.comprobaciones.single().complete(Result.success(false))
        advanceUntilIdle()

        viewModel.addFavorite()
        advanceUntilIdle()
        repositorio.guardados.single().complete(Result.failure(ArtistNotAvailable()))
        advanceUntilIdle()

        assertEquals(FavoriteUiState.NotAvailable, viewModel.uiState.value)
    }

    @Test
    fun saveFavoritoPasaAErrorGuardadoYPermiteReintentar() = runTest {
        val repositorio = FavoriteRepositoryFalso()
        val viewModel = FavoriteViewModel(100, 8, ArtistType.BANDA, repositorio)
        advanceUntilIdle()
        repositorio.comprobaciones.single().complete(Result.success(false))
        advanceUntilIdle()

        viewModel.addFavorite()
        advanceUntilIdle()
        repositorio.guardados[0].complete(Result.failure(LoadError()))
        advanceUntilIdle()
        assertEquals(FavoriteUiState.ErrorGuardado, viewModel.uiState.value)

        viewModel.addFavorite()
        assertEquals(FavoriteUiState.Guardando, viewModel.uiState.value)
        advanceUntilIdle()
        assertEquals(2, repositorio.guardados.size)
        repositorio.guardados[1].complete(Result.success(Unit))
        advanceUntilIdle()

        assertEquals(FavoriteUiState.Favorito(mostrarConfirmacion = true), viewModel.uiState.value)
    }

    @Test
    fun respuestaTardiaDeComprobacionNoReemplazaLaConsultaVigente() = runTest {
        val repositorio = FavoriteRepositoryFalso()
        val viewModel = FavoriteViewModel(100, 8, ArtistType.BANDA, repositorio)
        advanceUntilIdle()
        repositorio.comprobaciones.single().complete(Result.failure(LoadError()))
        advanceUntilIdle()
        assertEquals(FavoriteUiState.ErrorComprobacion, viewModel.uiState.value)

        viewModel.retryCheck()
        advanceUntilIdle()
        val tardia = repositorio.comprobaciones[0]
        val vigente = repositorio.comprobaciones[1]
        vigente.complete(Result.success(false))
        advanceUntilIdle()
        tardia.complete(Result.success(true))
        advanceUntilIdle()

        assertEquals(FavoriteUiState.NoFavorito, viewModel.uiState.value)
    }

    @Test
    fun confirmacionMostradaOcultaElMensajeDeFavorito() = runTest {
        val repositorio = FavoriteRepositoryFalso()
        val viewModel = FavoriteViewModel(100, 8, ArtistType.BANDA, repositorio)
        advanceUntilIdle()
        repositorio.comprobaciones.single().complete(Result.success(false))
        advanceUntilIdle()
        viewModel.addFavorite()
        advanceUntilIdle()
        repositorio.guardados.single().complete(Result.success(Unit))
        advanceUntilIdle()
        assertEquals(FavoriteUiState.Favorito(mostrarConfirmacion = true), viewModel.uiState.value)

        viewModel.confirmacionMostrada()

        assertEquals(FavoriteUiState.Favorito(mostrarConfirmacion = false), viewModel.uiState.value)
    }

    // ---------- Favorito confirmado guardado (add-cache-consultas, D5) ----------

    @Test
    fun comprobacionFallidaConFavoritoGuardadoMuestraFavoritoSinError() = runTest {
        val repositorio = FavoriteRepositoryFalso().apply { favoritosGuardados = setOf(100 to 8) }
        val viewModel = FavoriteViewModel(100, 8, ArtistType.BANDA, repositorio)
        advanceUntilIdle()

        repositorio.comprobaciones.single().complete(Result.failure(LoadError()))
        advanceUntilIdle()

        assertEquals(FavoriteUiState.Favorito(mostrarConfirmacion = false), viewModel.uiState.value)
    }

    @Test
    fun comprobacionFallidaSinFavoritoGuardadoSigueEnErrorComprobacion() = runTest {
        // El 8 es favorito guardado de otro coleccionista, no del 100.
        val repositorio = FavoriteRepositoryFalso().apply { favoritosGuardados = setOf(101 to 8) }
        val viewModel = FavoriteViewModel(100, 8, ArtistType.BANDA, repositorio)
        advanceUntilIdle()

        repositorio.comprobaciones.single().complete(Result.failure(LoadError()))
        advanceUntilIdle()

        assertEquals(FavoriteUiState.ErrorComprobacion, viewModel.uiState.value)
    }

    @Test
    fun conComprobacionSuccesssaNoSeLeeLoGuardado() = runTest {
        val repositorio = FavoriteRepositoryFalso().apply { favoritosGuardados = setOf(100 to 8) }
        val viewModel = FavoriteViewModel(100, 8, ArtistType.BANDA, repositorio)
        advanceUntilIdle()

        repositorio.comprobaciones.single().complete(Result.success(false))
        advanceUntilIdle()

        assertEquals(FavoriteUiState.NoFavorito, viewModel.uiState.value)
        assertEquals(0, repositorio.lecturasGuardadas)
    }
}
