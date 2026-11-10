package com.example.vinilos.viewmodel

import com.example.vinilos.MainDispatcherRule
import com.example.vinilos.data.model.ArtistType
import com.example.vinilos.data.remote.ErrorDeCarga
import com.example.vinilos.data.remote.ServiceAdapter
import com.example.vinilos.data.repository.ArtistaNoDisponible
import com.example.vinilos.data.repository.FavoriteRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

private class FavoriteRepositoryFalso : FavoriteRepository(ServiceAdapter("http://localhost/")) {
    val comprobaciones = mutableListOf<CompletableDeferred<Result<Boolean>>>()
    val guardados = mutableListOf<CompletableDeferred<Result<Unit>>>()

    override suspend fun esFavorito(collectorId: Int, artistId: Int): Result<Boolean> {
        val respuesta = CompletableDeferred<Result<Boolean>>()
        comprobaciones += respuesta
        return respuesta.await()
    }

    override suspend fun agregarFavorito(collectorId: Int, artistId: Int, tipo: ArtistType): Result<Unit> {
        val respuesta = CompletableDeferred<Result<Unit>>()
        guardados += respuesta
        return respuesta.await()
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class FavoritoViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    @Test
    fun comprobandoPasaAFavoritoCuandoElArtistaYaEsFavorito() = runTest {
        val repositorio = FavoriteRepositoryFalso()
        val viewModel = FavoritoViewModel(100, 8, ArtistType.BANDA, repositorio)

        assertEquals(FavoritoUiState.Comprobando, viewModel.uiState.value)
        advanceUntilIdle()
        repositorio.comprobaciones.single().complete(Result.success(true))
        advanceUntilIdle()

        assertEquals(FavoritoUiState.Favorito(mostrarConfirmacion = false), viewModel.uiState.value)
    }

    @Test
    fun comprobandoPasaANoFavoritoCuandoElArtistaNoEsFavorito() = runTest {
        val repositorio = FavoriteRepositoryFalso()
        val viewModel = FavoritoViewModel(100, 8, ArtistType.BANDA, repositorio)

        advanceUntilIdle()
        repositorio.comprobaciones.single().complete(Result.success(false))
        advanceUntilIdle()

        assertEquals(FavoritoUiState.NoFavorito, viewModel.uiState.value)
    }

    @Test
    fun comprobandoPasaAErrorComprobacionCuandoFallaLaConsulta() = runTest {
        val repositorio = FavoriteRepositoryFalso()
        val viewModel = FavoritoViewModel(100, 8, ArtistType.BANDA, repositorio)

        advanceUntilIdle()
        repositorio.comprobaciones.single().complete(Result.failure(ErrorDeCarga()))
        advanceUntilIdle()

        assertEquals(FavoritoUiState.ErrorComprobacion, viewModel.uiState.value)
    }

    @Test
    fun reintentarComprobacionPasaDeErrorComprobacionAFavoritoOtraVez() = runTest {
        val repositorio = FavoriteRepositoryFalso()
        val viewModel = FavoritoViewModel(100, 8, ArtistType.BANDA, repositorio)
        advanceUntilIdle()
        repositorio.comprobaciones.single().complete(Result.failure(ErrorDeCarga()))
        advanceUntilIdle()
        assertEquals(FavoritoUiState.ErrorComprobacion, viewModel.uiState.value)

        viewModel.reintentarComprobacion()
        assertEquals(FavoritoUiState.Comprobando, viewModel.uiState.value)
        advanceUntilIdle()
        assertEquals(2, repositorio.comprobaciones.size)
        repositorio.comprobaciones.last().complete(Result.success(true))
        advanceUntilIdle()

        assertEquals(FavoritoUiState.Favorito(mostrarConfirmacion = false), viewModel.uiState.value)
    }

    @Test
    fun noFavoritoPasaAGuardandoYLuegoAFavoritoConConfirmacion() = runTest {
        val repositorio = FavoriteRepositoryFalso()
        val viewModel = FavoritoViewModel(100, 8, ArtistType.BANDA, repositorio)
        advanceUntilIdle()
        repositorio.comprobaciones.single().complete(Result.success(false))
        advanceUntilIdle()
        assertEquals(FavoritoUiState.NoFavorito, viewModel.uiState.value)

        viewModel.agregarFavorito()
        assertEquals(FavoritoUiState.Guardando, viewModel.uiState.value)
        advanceUntilIdle()
        repositorio.guardados.single().complete(Result.success(Unit))
        advanceUntilIdle()

        assertEquals(FavoritoUiState.Favorito(mostrarConfirmacion = true), viewModel.uiState.value)
    }

    @Test
    fun segundoToqueMientrasGuardandoSeIgnora() = runTest {
        val repositorio = FavoriteRepositoryFalso()
        val viewModel = FavoritoViewModel(100, 8, ArtistType.BANDA, repositorio)
        advanceUntilIdle()
        repositorio.comprobaciones.single().complete(Result.success(false))
        advanceUntilIdle()

        viewModel.agregarFavorito()
        viewModel.agregarFavorito()
        advanceUntilIdle()

        assertEquals(1, repositorio.guardados.size)
    }

    @Test
    fun guardarFavoritoPasaANoDisponibleCuandoElArtistaYaNoExiste() = runTest {
        val repositorio = FavoriteRepositoryFalso()
        val viewModel = FavoritoViewModel(100, 8, ArtistType.BANDA, repositorio)
        advanceUntilIdle()
        repositorio.comprobaciones.single().complete(Result.success(false))
        advanceUntilIdle()

        viewModel.agregarFavorito()
        advanceUntilIdle()
        repositorio.guardados.single().complete(Result.failure(ArtistaNoDisponible()))
        advanceUntilIdle()

        assertEquals(FavoritoUiState.NoDisponible, viewModel.uiState.value)
    }

    @Test
    fun guardarFavoritoPasaAErrorGuardadoYPermiteReintentar() = runTest {
        val repositorio = FavoriteRepositoryFalso()
        val viewModel = FavoritoViewModel(100, 8, ArtistType.BANDA, repositorio)
        advanceUntilIdle()
        repositorio.comprobaciones.single().complete(Result.success(false))
        advanceUntilIdle()

        viewModel.agregarFavorito()
        advanceUntilIdle()
        repositorio.guardados[0].complete(Result.failure(ErrorDeCarga()))
        advanceUntilIdle()
        assertEquals(FavoritoUiState.ErrorGuardado, viewModel.uiState.value)

        viewModel.agregarFavorito()
        assertEquals(FavoritoUiState.Guardando, viewModel.uiState.value)
        advanceUntilIdle()
        assertEquals(2, repositorio.guardados.size)
        repositorio.guardados[1].complete(Result.success(Unit))
        advanceUntilIdle()

        assertEquals(FavoritoUiState.Favorito(mostrarConfirmacion = true), viewModel.uiState.value)
    }

    @Test
    fun respuestaTardiaDeComprobacionNoReemplazaLaConsultaVigente() = runTest {
        val repositorio = FavoriteRepositoryFalso()
        val viewModel = FavoritoViewModel(100, 8, ArtistType.BANDA, repositorio)
        advanceUntilIdle()
        repositorio.comprobaciones.single().complete(Result.failure(ErrorDeCarga()))
        advanceUntilIdle()
        assertEquals(FavoritoUiState.ErrorComprobacion, viewModel.uiState.value)

        viewModel.reintentarComprobacion()
        advanceUntilIdle()
        val tardia = repositorio.comprobaciones[0]
        val vigente = repositorio.comprobaciones[1]
        vigente.complete(Result.success(false))
        advanceUntilIdle()
        tardia.complete(Result.success(true))
        advanceUntilIdle()

        assertEquals(FavoritoUiState.NoFavorito, viewModel.uiState.value)
    }

    @Test
    fun confirmacionMostradaOcultaElMensajeDeFavorito() = runTest {
        val repositorio = FavoriteRepositoryFalso()
        val viewModel = FavoritoViewModel(100, 8, ArtistType.BANDA, repositorio)
        advanceUntilIdle()
        repositorio.comprobaciones.single().complete(Result.success(false))
        advanceUntilIdle()
        viewModel.agregarFavorito()
        advanceUntilIdle()
        repositorio.guardados.single().complete(Result.success(Unit))
        advanceUntilIdle()
        assertEquals(FavoritoUiState.Favorito(mostrarConfirmacion = true), viewModel.uiState.value)

        viewModel.confirmacionMostrada()

        assertEquals(FavoritoUiState.Favorito(mostrarConfirmacion = false), viewModel.uiState.value)
    }
}
