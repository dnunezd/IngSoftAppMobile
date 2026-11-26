package com.example.vinilos.viewmodel

import com.example.vinilos.MainDispatcherRule
import com.example.vinilos.data.model.AlbumArtista
import com.example.vinilos.data.model.AlbumSummary
import com.example.vinilos.data.model.Artist
import com.example.vinilos.data.model.ArtistType
import com.example.vinilos.data.model.ArtistDetail
import com.example.vinilos.data.remote.LoadError
import com.example.vinilos.data.remote.ResourceNotFound
import com.example.vinilos.data.repository.ArtistRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private class ArtistRepositoryFalso : ArtistRepository {
    val consultas = mutableListOf<Triple<ArtistType, Int, CompletableDeferred<Result<ArtistDetail>>>>()

    override suspend fun getDetail(tipo: ArtistType, id: Int): Result<ArtistDetail> {
        val respuesta = CompletableDeferred<Result<ArtistDetail>>()
        consultas += Triple(tipo, id, respuesta)
        return respuesta.await()
    }

    /** Detalles guardados en el dispositivo (add-cache-consultas). */
    val guardados = mutableMapOf<Pair<ArtistType, Int>, ArtistDetail>()

    override suspend fun getCachedDetail(tipo: ArtistType, id: Int): ArtistDetail? = guardados[tipo to id]

    override suspend fun getArtists(): Result<List<Artist>> = error("No se usa en esta prueba")
    override suspend fun getCachedArtists(): List<Artist> = error("No se usa en esta prueba")
    override suspend fun associateAlbum(tipo: ArtistType, idArtista: Int, idAlbum: Int): Result<Unit> =
        error("No se usa en esta prueba")
}

@OptIn(ExperimentalCoroutinesApi::class)
class ArtistDetailViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private val detalle = ArtistDetail(
        101, "Queen", null, "Banda británica", ArtistType.BANDA, "1970-01-01T00:00:00.000Z",
        emptyList(), emptyList(), emptyList()
    )

    @Test
    fun alCrearseConsultaElArtistaYPasaASuccess() = runTest {
        val repositorio = ArtistRepositoryFalso()
        val viewModel = ArtistDetailViewModel(ArtistType.BANDA, 101, repositorio)

        assertEquals(ArtistDetailUiState.Loading, viewModel.uiState.value)
        advanceUntilIdle()
        val consulta = repositorio.consultas.single()
        assertEquals(ArtistType.BANDA to 101, consulta.first to consulta.second)

        consulta.third.complete(Result.success(detalle))
        advanceUntilIdle()

        assertEquals(ArtistDetailUiState.Success(detalle), viewModel.uiState.value)
    }

    @Test
    fun pasaAErrorCuandoFallaLaConsulta() = runTest {
        val repositorio = ArtistRepositoryFalso()
        val viewModel = ArtistDetailViewModel(ArtistType.BANDA, 101, repositorio)
        advanceUntilIdle()

        repositorio.consultas.single().third.complete(Result.failure(LoadError()))
        advanceUntilIdle()

        assertEquals(ArtistDetailUiState.Error, viewModel.uiState.value)
    }

    @Test
    fun retryPasaDeErrorALoadingYSuccessConElMismoArtista() = runTest {
        val repositorio = ArtistRepositoryFalso()
        val viewModel = ArtistDetailViewModel(ArtistType.BANDA, 101, repositorio)
        advanceUntilIdle()
        repositorio.consultas.single().third.complete(Result.failure(LoadError()))
        advanceUntilIdle()

        viewModel.retry()
        assertEquals(ArtistDetailUiState.Loading, viewModel.uiState.value)
        advanceUntilIdle()
        assertEquals(
            listOf(ArtistType.BANDA to 101, ArtistType.BANDA to 101),
            repositorio.consultas.map { it.first to it.second }
        )
        repositorio.consultas.last().third.complete(Result.success(detalle))
        advanceUntilIdle()

        assertEquals(ArtistDetailUiState.Success(detalle), viewModel.uiState.value)
    }

    @Test
    fun retrySoloActuaDesdeError() = runTest {
        val repositorio = ArtistRepositoryFalso()
        val viewModel = ArtistDetailViewModel(ArtistType.BANDA, 101, repositorio)
        advanceUntilIdle()

        viewModel.retry()
        advanceUntilIdle()
        assertEquals(1, repositorio.consultas.size)

        repositorio.consultas.single().third.complete(Result.success(detalle))
        advanceUntilIdle()
        viewModel.retry()
        advanceUntilIdle()

        assertEquals(1, repositorio.consultas.size)
        assertEquals(ArtistDetailUiState.Success(detalle), viewModel.uiState.value)
    }

    /** Con una sola consulta en curso, ningún resultado tardío puede reemplazar a la vigente. */
    @Test
    fun retryDosVecesSeguidasNoLanzaUnaConsultaParalela() = runTest {
        val repositorio = ArtistRepositoryFalso()
        val viewModel = ArtistDetailViewModel(ArtistType.BANDA, 101, repositorio)
        advanceUntilIdle()
        repositorio.consultas.single().third.complete(Result.failure(LoadError()))
        advanceUntilIdle()

        viewModel.retry()
        viewModel.retry()
        advanceUntilIdle()

        assertEquals(2, repositorio.consultas.size)
    }

    // ---------- Detalle guardado (add-cache-consultas, D5) ----------

    private fun repoConGuardado() = ArtistRepositoryFalso().apply { guardados[ArtistType.BANDA to 101] = detalle }
    private val actualizado = detalle.copy(descripcion = "Banda británica de rock")

    @Test
    fun conDetalleGuardadoSeMuestraDeInmediatoYLuegoLaRespuesta() = runTest {
        val repositorio = repoConGuardado()
        val viewModel = ArtistDetailViewModel(ArtistType.BANDA, 101, repositorio)
        advanceUntilIdle()

        assertEquals(ArtistDetailUiState.Success(detalle, actualizando = true), viewModel.uiState.value)

        repositorio.consultas.single().third.complete(Result.success(actualizado))
        advanceUntilIdle()

        assertEquals(ArtistDetailUiState.Success(actualizado), viewModel.uiState.value)
    }

    @Test
    fun conDetalleGuardadoYRedMalSeConservaConElAviso() = runTest {
        val repositorio = repoConGuardado()
        val viewModel = ArtistDetailViewModel(ArtistType.BANDA, 101, repositorio)
        advanceUntilIdle()

        repositorio.consultas.single().third.complete(Result.failure(LoadError()))
        advanceUntilIdle()

        assertEquals(ArtistDetailUiState.Success(detalle, errorAlActualizar = true), viewModel.uiState.value)

        viewModel.avisoMostrado()
        assertEquals(ArtistDetailUiState.Success(detalle), viewModel.uiState.value)
    }

    @Test
    fun un404TerminaEnErrorAunqueHayaDetalleGuardado() = runTest {
        val repositorio = repoConGuardado()
        val viewModel = ArtistDetailViewModel(ArtistType.BANDA, 101, repositorio)
        advanceUntilIdle()

        repositorio.consultas.single().third.complete(Result.failure(ResourceNotFound()))
        advanceUntilIdle()

        assertEquals(ArtistDetailUiState.Error, viewModel.uiState.value)
    }

    @Test
    fun elDetalleGuardadoDeOtroTipoNoSeUsa() = runTest {
        val repositorio = repoConGuardado()
        val viewModel = ArtistDetailViewModel(ArtistType.MUSICO, 101, repositorio)
        advanceUntilIdle()

        assertEquals(ArtistDetailUiState.Loading, viewModel.uiState.value)
        repositorio.consultas.single().third.complete(Result.failure(LoadError()))
        advanceUntilIdle()
        assertEquals(ArtistDetailUiState.Error, viewModel.uiState.value)
    }

    // ---------- Álbum asociado (HU15, add-agregar-album-artista D5) ----------

    private val ruben = ArtistDetail(
        100, "Rubén Blades Bellido de Luna", null, null, ArtistType.MUSICO, null, emptyList(),
        listOf(AlbumArtista(100, "Buscando América", "p100", "1984"), AlbumArtista(101, "Poeta del pueblo", "p101", null)),
        emptyList()
    )
    private val noche = AlbumSummary(102, "A Night at the Opera", "p102", "Rock", "Queen", "1975")
    private val nocheEnElDetalle = AlbumArtista(102, "A Night at the Opera", "p102", "1975")

    private fun kotlinx.coroutines.test.TestScope.rubenCargado(repositorio: ArtistRepositoryFalso): ArtistDetailViewModel {
        val viewModel = ArtistDetailViewModel(ArtistType.MUSICO, 100, repositorio)
        advanceUntilIdle()
        repositorio.consultas.single().third.complete(Result.success(ruben))
        advanceUntilIdle()
        return viewModel
    }

    @Test
    fun albumAsociadoLoAgregaAlFinalDeLaDiscografiaYActivaElAviso() = runTest {
        // Revisión del PR #114: se agrega al final y el orden lo fija la nueva consulta (un solo criterio de orden).
        val repositorio = ArtistRepositoryFalso()
        val viewModel = rubenCargado(repositorio)

        viewModel.albumAsociado(noche)

        val estado = viewModel.uiState.value as ArtistDetailUiState.Success
        assertEquals(ruben.albumes + nocheEnElDetalle, estado.detalle.albumes)
        assertTrue(estado.mostrarAlbumAsociado)
    }

    @Test
    fun albumAsociadoVuelveAConsultarElDetalleYUsaLaRespuesta() = runTest {
        val repositorio = ArtistRepositoryFalso()
        val viewModel = rubenCargado(repositorio)
        val actualizado = ruben.copy(albumes = listOf(nocheEnElDetalle) + ruben.albumes)

        viewModel.albumAsociado(noche)
        advanceUntilIdle()
        assertEquals(2, repositorio.consultas.size)
        assertEquals(ArtistType.MUSICO to 100, repositorio.consultas.last().first to repositorio.consultas.last().second)
        repositorio.consultas.last().third.complete(Result.success(actualizado))
        advanceUntilIdle()

        val estado = viewModel.uiState.value as ArtistDetailUiState.Success
        assertEquals(actualizado, estado.detalle)
        assertTrue("El aviso sigue pendiente hasta que la pantalla lo muestre", estado.mostrarAlbumAsociado)
        assertFalse(estado.actualizando)
    }

    @Test
    fun siLaNuevaConsultaFallaConservaElAlbumAgregadoSinAvisoDeError() = runTest {
        val repositorio = ArtistRepositoryFalso()
        val viewModel = rubenCargado(repositorio)

        viewModel.albumAsociado(noche)
        advanceUntilIdle()
        repositorio.consultas.last().third.complete(Result.failure(LoadError()))
        advanceUntilIdle()

        val estado = viewModel.uiState.value as ArtistDetailUiState.Success
        assertTrue(nocheEnElDetalle in estado.detalle.albumes)
        assertFalse(estado.errorAlActualizar)
        assertFalse(estado.actualizando)
    }

    @Test
    fun albumAsociadoMostradoApagaElAviso() = runTest {
        val repositorio = ArtistRepositoryFalso()
        val viewModel = rubenCargado(repositorio)
        viewModel.albumAsociado(noche)

        viewModel.albumAsociadoMostrado()

        assertFalse((viewModel.uiState.value as ArtistDetailUiState.Success).mostrarAlbumAsociado)
    }

    @Test
    fun unAlbumQueYaEstabaNoSeRepite() = runTest {
        val repositorio = ArtistRepositoryFalso()
        val viewModel = rubenCargado(repositorio)

        viewModel.albumAsociado(AlbumSummary(100, "Buscando América", "p100", "Salsa", "Rubén Blades", "1984"))

        assertEquals(ruben.albumes, (viewModel.uiState.value as ArtistDetailUiState.Success).detalle.albumes)
    }
}
