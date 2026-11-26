package com.example.vinilos.viewmodel

import com.example.vinilos.MainDispatcherRule
import com.example.vinilos.data.model.Album
import com.example.vinilos.data.model.NewAlbum
import com.example.vinilos.data.model.AlbumSummary
import com.example.vinilos.data.model.Artista
import com.example.vinilos.data.model.Track
import com.example.vinilos.data.remote.LoadError
import com.example.vinilos.data.repository.AlbumNotAvailable
import com.example.vinilos.data.repository.AlbumRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private class RepositorioAddTrackFalso : AlbumRepository {
    val envios = mutableListOf<Pair<Int, Track>>()
    private val respuestas = mutableListOf<CompletableDeferred<Result<Unit>>>()

    /**
     * Album already saved in cache (Room) for the form's context card. The load comes from the
     * local cache, not the network: the album was already queried when opening the detail screen
     * (add-cache-consultas), so there's no need to repeat the `GET /albums/{id}` (avoids the
     * duplicate query from HU02/HU10).
     */
    var albumGuardado: Album? = null

    override suspend fun addTrack(albumId: Int, cancion: Track): Result<Unit> {
        envios += albumId to cancion
        return CompletableDeferred<Result<Unit>>().also(respuestas::add).await()
    }

    fun responder(resultado: Result<Unit>) {
        respuestas.last().complete(resultado)
    }

    override suspend fun getAlbum(id: Int): Result<Album> =
        error("La tarjeta de contexto no debe repetir el GET /albums/{id}; usar obtenerAlbumGuardado")
    override suspend fun getAlbums(): Result<List<AlbumSummary>> = error("No se usa en esta prueba")
    override suspend fun getCachedAlbums(): List<AlbumSummary> = error("No se usa en esta prueba")
    override suspend fun getCachedAlbum(id: Int): Album? = albumGuardado
    override suspend fun createAlbum(album: NewAlbum): Result<Int> = error("No se usa en esta prueba")
}

@OptIn(ExperimentalCoroutinesApi::class)
class AddTrackViewModelTest {
    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    @Test
    fun formularioEmptyIniciaConAmbosCamposObligatoriosYGuardarDeshabilitado() {
        val viewModel = AddTrackViewModel(101, RepositorioAddTrackFalso())

        assertEquals(
            mapOf(
                TrackField.Nombre to TrackError.Obligatorio,
                TrackField.Duracion to TrackError.Obligatorio
            ),
            viewModel.uiState.value.errores
        )
        assertFalse(viewModel.uiState.value.puedeGuardar)
    }

    @Test
    fun editarCamposActualizaLosErroresSinOcultarElDelOtroCampo() {
        val viewModel = AddTrackViewModel(101, RepositorioAddTrackFalso())

        viewModel.cambiarNombre("Canción")
        assertEquals(
            mapOf(TrackField.Duracion to TrackError.Obligatorio),
            viewModel.uiState.value.errores
        )
        viewModel.cambiarDuracion("5:60")
        assertEquals(
            mapOf(TrackField.Duracion to TrackError.DuracionInvalida),
            viewModel.uiState.value.errores
        )
        viewModel.cambiarDuracion("5:05")
        assertTrue(viewModel.uiState.value.errores.isEmpty())
        assertTrue(viewModel.uiState.value.puedeGuardar)
    }

    @Test
    fun noGuardaCuandoLosValoresNoSonValidos() = runTest {
        val repositorio = RepositorioAddTrackFalso()
        val viewModel = AddTrackViewModel(101, repositorio)

        viewModel.save()

        assertTrue(repositorio.envios.isEmpty())
    }

    @Test
    fun guardaUnaSolaVezYEnviaElAlbumYLosValoresValidos() = runTest {
        val repositorio = RepositorioAddTrackFalso()
        val viewModel = AddTrackViewModel(101, repositorio)
        viewModel.cambiarNombre("  Decisiones  ")
        viewModel.cambiarDuracion("5:05")

        viewModel.save()
        assertEquals(TrackSubmission.Guardando, viewModel.uiState.value.envio)
        viewModel.save()
        advanceUntilIdle()

        assertEquals(listOf(101 to Track("Decisiones", "5:05")), repositorio.envios)
        repositorio.responder(Result.success(Unit))
        advanceUntilIdle()
        assertEquals(TrackSubmission.Agregada, viewModel.uiState.value.envio)
    }

    @Test
    fun falloConservaLoEscritoYPermiteReintentar() = runTest {
        val repositorio = RepositorioAddTrackFalso()
        val viewModel = AddTrackViewModel(101, repositorio)
        viewModel.cambiarNombre("Decisiones")
        viewModel.cambiarDuracion("5:05")
        viewModel.save()
        advanceUntilIdle()
        repositorio.responder(Result.failure(LoadError()))
        advanceUntilIdle()

        assertEquals(TrackSubmission.Fallida, viewModel.uiState.value.envio)
        assertEquals("Decisiones", viewModel.uiState.value.nombre)
        assertEquals("5:05", viewModel.uiState.value.duracion)
        assertTrue(viewModel.uiState.value.puedeGuardar)

        viewModel.save()
        advanceUntilIdle()
        assertEquals(2, repositorio.envios.size)
    }

    @Test
    fun albumNotAvailableSeDistingueDelErrorReintentable() = runTest {
        val repositorio = RepositorioAddTrackFalso()
        val viewModel = AddTrackViewModel(101, repositorio)
        viewModel.cambiarNombre("Decisiones")
        viewModel.cambiarDuracion("5:05")
        viewModel.save()
        advanceUntilIdle()
        repositorio.responder(Result.failure(AlbumNotAvailable()))
        advanceUntilIdle()

        assertEquals(TrackSubmission.AlbumNotAvailable, viewModel.uiState.value.envio)
        assertFalse(viewModel.uiState.value.puedeGuardar)
        viewModel.save()
        advanceUntilIdle()
        assertEquals(1, repositorio.envios.size)
    }

    @Test
    fun cargaLaInformacionDelAlbumGuardadoParaMostrarlaComoContextoDelFormularioSinRepetirLaConsultaDeRed() = runTest {
        val repositorio = RepositorioAddTrackFalso()
        repositorio.albumGuardado = Album(
            id = 101,
            nombre = "A Day at the Races",
            portada = "https://ejemplo.com/portada.jpg",
            descripcion = "",
            genero = "Rock",
            disquera = "EMI",
            fechaLanzamiento = "1976-12-10T00:00:00.000Z",
            canciones = emptyList(),
            artistas = listOf(Artista("Queen", null)),
            comentarios = emptyList()
        )

        val viewModel = AddTrackViewModel(101, repositorio)
        advanceUntilIdle()

        assertEquals(
            TargetAlbumInfo("A Day at the Races", "https://ejemplo.com/portada.jpg", "Queen"),
            viewModel.uiState.value.albumInfo
        )
    }

    @Test
    fun editarDespuesDeUnFalloOcultaElBannerDeErrorAntesDeVolverAGuardar() = runTest {
        val repositorio = RepositorioAddTrackFalso()
        val viewModel = AddTrackViewModel(101, repositorio)
        viewModel.cambiarNombre("Decisiones")
        viewModel.cambiarDuracion("5:05")
        viewModel.save()
        advanceUntilIdle()
        repositorio.responder(Result.failure(LoadError()))
        advanceUntilIdle()
        assertEquals(TrackSubmission.Fallida, viewModel.uiState.value.envio)

        viewModel.cambiarNombre("Decisiones (editada)")

        assertEquals(TrackSubmission.Editando, viewModel.uiState.value.envio)
    }

    @Test
    fun unFalloNoReconocidoNoTumbaLaAppYQuedaComoFallidaReintentable() = runTest {
        val repositorio = RepositorioAddTrackFalso()
        val viewModel = AddTrackViewModel(101, repositorio)
        viewModel.cambiarNombre("Decisiones")
        viewModel.cambiarDuracion("5:05")
        viewModel.save()
        advanceUntilIdle()

        repositorio.responder(Result.failure(IllegalStateException("Fallo inesperado del backend")))
        advanceUntilIdle()

        assertEquals(TrackSubmission.Fallida, viewModel.uiState.value.envio)
        assertTrue(viewModel.uiState.value.puedeGuardar)
    }
}
