package com.example.vinilos.viewmodel

import com.example.vinilos.MainDispatcherRule
import com.example.vinilos.data.model.AlbumArtista
import com.example.vinilos.data.model.AlbumResumen
import com.example.vinilos.data.model.Artist
import com.example.vinilos.data.model.ArtistType
import com.example.vinilos.data.model.ArtistaDetalle
import com.example.vinilos.data.remote.ErrorDeCarga
import com.example.vinilos.data.remote.RecursoNoEncontrado
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
    val consultas = mutableListOf<Triple<ArtistType, Int, CompletableDeferred<Result<ArtistaDetalle>>>>()

    override suspend fun obtenerDetalle(tipo: ArtistType, id: Int): Result<ArtistaDetalle> {
        val respuesta = CompletableDeferred<Result<ArtistaDetalle>>()
        consultas += Triple(tipo, id, respuesta)
        return respuesta.await()
    }

    /** Detalles guardados en el dispositivo (add-cache-consultas). */
    val guardados = mutableMapOf<Pair<ArtistType, Int>, ArtistaDetalle>()

    override suspend fun obtenerDetalleGuardado(tipo: ArtistType, id: Int): ArtistaDetalle? = guardados[tipo to id]

    override suspend fun obtenerArtistas(): Result<List<Artist>> = error("No se usa en esta prueba")
    override suspend fun obtenerArtistasGuardados(): List<Artist> = error("No se usa en esta prueba")
    override suspend fun asociarAlbum(tipo: ArtistType, idArtista: Int, idAlbum: Int): Result<Unit> =
        error("No se usa en esta prueba")
}

@OptIn(ExperimentalCoroutinesApi::class)
class ArtistaDetalleViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private val detalle = ArtistaDetalle(
        101, "Queen", null, "Banda británica", ArtistType.BANDA, "1 de enero de 1970",
        emptyList(), emptyList(), emptyList()
    )

    @Test
    fun alCrearseConsultaElArtistaYPasaAExito() = runTest {
        val repositorio = ArtistRepositoryFalso()
        val viewModel = ArtistaDetalleViewModel(ArtistType.BANDA, 101, repositorio)

        assertEquals(ArtistaDetalleUiState.Cargando, viewModel.uiState.value)
        advanceUntilIdle()
        val consulta = repositorio.consultas.single()
        assertEquals(ArtistType.BANDA to 101, consulta.first to consulta.second)

        consulta.third.complete(Result.success(detalle))
        advanceUntilIdle()

        assertEquals(ArtistaDetalleUiState.Exito(detalle), viewModel.uiState.value)
    }

    @Test
    fun pasaAErrorCuandoFallaLaConsulta() = runTest {
        val repositorio = ArtistRepositoryFalso()
        val viewModel = ArtistaDetalleViewModel(ArtistType.BANDA, 101, repositorio)
        advanceUntilIdle()

        repositorio.consultas.single().third.complete(Result.failure(ErrorDeCarga()))
        advanceUntilIdle()

        assertEquals(ArtistaDetalleUiState.Error, viewModel.uiState.value)
    }

    @Test
    fun reintentarPasaDeErrorACargandoYExitoConElMismoArtista() = runTest {
        val repositorio = ArtistRepositoryFalso()
        val viewModel = ArtistaDetalleViewModel(ArtistType.BANDA, 101, repositorio)
        advanceUntilIdle()
        repositorio.consultas.single().third.complete(Result.failure(ErrorDeCarga()))
        advanceUntilIdle()

        viewModel.reintentar()
        assertEquals(ArtistaDetalleUiState.Cargando, viewModel.uiState.value)
        advanceUntilIdle()
        assertEquals(
            listOf(ArtistType.BANDA to 101, ArtistType.BANDA to 101),
            repositorio.consultas.map { it.first to it.second }
        )
        repositorio.consultas.last().third.complete(Result.success(detalle))
        advanceUntilIdle()

        assertEquals(ArtistaDetalleUiState.Exito(detalle), viewModel.uiState.value)
    }

    @Test
    fun reintentarSoloActuaDesdeError() = runTest {
        val repositorio = ArtistRepositoryFalso()
        val viewModel = ArtistaDetalleViewModel(ArtistType.BANDA, 101, repositorio)
        advanceUntilIdle()

        viewModel.reintentar()
        advanceUntilIdle()
        assertEquals(1, repositorio.consultas.size)

        repositorio.consultas.single().third.complete(Result.success(detalle))
        advanceUntilIdle()
        viewModel.reintentar()
        advanceUntilIdle()

        assertEquals(1, repositorio.consultas.size)
        assertEquals(ArtistaDetalleUiState.Exito(detalle), viewModel.uiState.value)
    }

    /** Con una sola consulta en curso, ningún resultado tardío puede reemplazar a la vigente. */
    @Test
    fun reintentarDosVecesSeguidasNoLanzaUnaConsultaParalela() = runTest {
        val repositorio = ArtistRepositoryFalso()
        val viewModel = ArtistaDetalleViewModel(ArtistType.BANDA, 101, repositorio)
        advanceUntilIdle()
        repositorio.consultas.single().third.complete(Result.failure(ErrorDeCarga()))
        advanceUntilIdle()

        viewModel.reintentar()
        viewModel.reintentar()
        advanceUntilIdle()

        assertEquals(2, repositorio.consultas.size)
    }

    // ---------- Detalle guardado (add-cache-consultas, D5) ----------

    private fun repoConGuardado() = ArtistRepositoryFalso().apply { guardados[ArtistType.BANDA to 101] = detalle }
    private val actualizado = detalle.copy(descripcion = "Banda británica de rock")

    @Test
    fun conDetalleGuardadoSeMuestraDeInmediatoYLuegoLaRespuesta() = runTest {
        val repositorio = repoConGuardado()
        val viewModel = ArtistaDetalleViewModel(ArtistType.BANDA, 101, repositorio)
        advanceUntilIdle()

        assertEquals(ArtistaDetalleUiState.Exito(detalle, actualizando = true), viewModel.uiState.value)

        repositorio.consultas.single().third.complete(Result.success(actualizado))
        advanceUntilIdle()

        assertEquals(ArtistaDetalleUiState.Exito(actualizado), viewModel.uiState.value)
    }

    @Test
    fun conDetalleGuardadoYRedMalSeConservaConElAviso() = runTest {
        val repositorio = repoConGuardado()
        val viewModel = ArtistaDetalleViewModel(ArtistType.BANDA, 101, repositorio)
        advanceUntilIdle()

        repositorio.consultas.single().third.complete(Result.failure(ErrorDeCarga()))
        advanceUntilIdle()

        assertEquals(ArtistaDetalleUiState.Exito(detalle, errorAlActualizar = true), viewModel.uiState.value)

        viewModel.avisoMostrado()
        assertEquals(ArtistaDetalleUiState.Exito(detalle), viewModel.uiState.value)
    }

    @Test
    fun un404TerminaEnErrorAunqueHayaDetalleGuardado() = runTest {
        val repositorio = repoConGuardado()
        val viewModel = ArtistaDetalleViewModel(ArtistType.BANDA, 101, repositorio)
        advanceUntilIdle()

        repositorio.consultas.single().third.complete(Result.failure(RecursoNoEncontrado()))
        advanceUntilIdle()

        assertEquals(ArtistaDetalleUiState.Error, viewModel.uiState.value)
    }

    @Test
    fun elDetalleGuardadoDeOtroTipoNoSeUsa() = runTest {
        val repositorio = repoConGuardado()
        val viewModel = ArtistaDetalleViewModel(ArtistType.MUSICO, 101, repositorio)
        advanceUntilIdle()

        assertEquals(ArtistaDetalleUiState.Cargando, viewModel.uiState.value)
        repositorio.consultas.single().third.complete(Result.failure(ErrorDeCarga()))
        advanceUntilIdle()
        assertEquals(ArtistaDetalleUiState.Error, viewModel.uiState.value)
    }

    // ---------- Álbum asociado (HU15, add-agregar-album-artista D5) ----------

    private val ruben = ArtistaDetalle(
        100, "Rubén Blades Bellido de Luna", null, null, ArtistType.MUSICO, null, emptyList(),
        listOf(AlbumArtista(100, "Buscando América", "p100", "1984"), AlbumArtista(101, "Poeta del pueblo", "p101", null)),
        emptyList()
    )
    private val noche = AlbumResumen(102, "A Night at the Opera", "p102", "Rock", "Queen", "1975")
    private val nocheEnElDetalle = AlbumArtista(102, "A Night at the Opera", "p102", "1975")

    private fun kotlinx.coroutines.test.TestScope.rubenCargado(repositorio: ArtistRepositoryFalso): ArtistaDetalleViewModel {
        val viewModel = ArtistaDetalleViewModel(ArtistType.MUSICO, 100, repositorio)
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

        val estado = viewModel.uiState.value as ArtistaDetalleUiState.Exito
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

        val estado = viewModel.uiState.value as ArtistaDetalleUiState.Exito
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
        repositorio.consultas.last().third.complete(Result.failure(ErrorDeCarga()))
        advanceUntilIdle()

        val estado = viewModel.uiState.value as ArtistaDetalleUiState.Exito
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

        assertFalse((viewModel.uiState.value as ArtistaDetalleUiState.Exito).mostrarAlbumAsociado)
    }

    @Test
    fun unAlbumQueYaEstabaNoSeRepite() = runTest {
        val repositorio = ArtistRepositoryFalso()
        val viewModel = rubenCargado(repositorio)

        viewModel.albumAsociado(AlbumResumen(100, "Buscando América", "p100", "Salsa", "Rubén Blades", "1984"))

        assertEquals(ruben.albumes, (viewModel.uiState.value as ArtistaDetalleUiState.Exito).detalle.albumes)
    }
}
