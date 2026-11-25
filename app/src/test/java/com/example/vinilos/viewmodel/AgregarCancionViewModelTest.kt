package com.example.vinilos.viewmodel

import com.example.vinilos.MainDispatcherRule
import com.example.vinilos.data.model.Album
import com.example.vinilos.data.model.AlbumNuevo
import com.example.vinilos.data.model.AlbumResumen
import com.example.vinilos.data.model.Artista
import com.example.vinilos.data.model.Cancion
import com.example.vinilos.data.remote.ErrorDeCarga
import com.example.vinilos.data.repository.AlbumNoDisponible
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

private class RepositorioAgregarCancionFalso : AlbumRepository {
    val envios = mutableListOf<Pair<Int, Cancion>>()
    private val respuestas = mutableListOf<CompletableDeferred<Result<Unit>>>()

    /**
     * Álbum ya guardado en caché (Room) para la tarjeta de contexto del formulario. La carga es de la
     * caché local, no de la red: el álbum ya se consultó al abrir el detalle (add-cache-consultas),
     * así que no hace falta repetir el `GET /albums/{id}` (evita la consulta duplicada de HU02/HU10).
     */
    var albumGuardado: Album? = null

    override suspend fun agregarCancion(albumId: Int, cancion: Cancion): Result<Unit> {
        envios += albumId to cancion
        return CompletableDeferred<Result<Unit>>().also(respuestas::add).await()
    }

    fun responder(resultado: Result<Unit>) {
        respuestas.last().complete(resultado)
    }

    override suspend fun obtenerAlbum(id: Int): Result<Album> =
        error("La tarjeta de contexto no debe repetir el GET /albums/{id}; usar obtenerAlbumGuardado")
    override suspend fun obtenerAlbumes(): Result<List<AlbumResumen>> = error("No se usa en esta prueba")
    override suspend fun obtenerAlbumesGuardados(): List<AlbumResumen> = error("No se usa en esta prueba")
    override suspend fun obtenerAlbumGuardado(id: Int): Album? = albumGuardado
    override suspend fun crearAlbum(album: AlbumNuevo): Result<Int> = error("No se usa en esta prueba")
}

@OptIn(ExperimentalCoroutinesApi::class)
class AgregarCancionViewModelTest {
    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    @Test
    fun formularioVacioIniciaConAmbosCamposObligatoriosYGuardarDeshabilitado() {
        val viewModel = AgregarCancionViewModel(101, RepositorioAgregarCancionFalso())

        assertEquals(
            mapOf(
                CampoCancion.Nombre to ErrorCancion.Obligatorio,
                CampoCancion.Duracion to ErrorCancion.Obligatorio
            ),
            viewModel.uiState.value.errores
        )
        assertFalse(viewModel.uiState.value.puedeGuardar)
    }

    @Test
    fun editarCamposActualizaLosErroresSinOcultarElDelOtroCampo() {
        val viewModel = AgregarCancionViewModel(101, RepositorioAgregarCancionFalso())

        viewModel.cambiarNombre("Canción")
        assertEquals(
            mapOf(CampoCancion.Duracion to ErrorCancion.Obligatorio),
            viewModel.uiState.value.errores
        )
        viewModel.cambiarDuracion("5:60")
        assertEquals(
            mapOf(CampoCancion.Duracion to ErrorCancion.DuracionInvalida),
            viewModel.uiState.value.errores
        )
        viewModel.cambiarDuracion("5:05")
        assertTrue(viewModel.uiState.value.errores.isEmpty())
        assertTrue(viewModel.uiState.value.puedeGuardar)
    }

    @Test
    fun noGuardaCuandoLosValoresNoSonValidos() = runTest {
        val repositorio = RepositorioAgregarCancionFalso()
        val viewModel = AgregarCancionViewModel(101, repositorio)

        viewModel.guardar()

        assertTrue(repositorio.envios.isEmpty())
    }

    @Test
    fun guardaUnaSolaVezYEnviaElAlbumYLosValoresValidos() = runTest {
        val repositorio = RepositorioAgregarCancionFalso()
        val viewModel = AgregarCancionViewModel(101, repositorio)
        viewModel.cambiarNombre("  Decisiones  ")
        viewModel.cambiarDuracion("5:05")

        viewModel.guardar()
        assertEquals(EnvioCancion.Guardando, viewModel.uiState.value.envio)
        viewModel.guardar()
        advanceUntilIdle()

        assertEquals(listOf(101 to Cancion("Decisiones", "5:05")), repositorio.envios)
        repositorio.responder(Result.success(Unit))
        advanceUntilIdle()
        assertEquals(EnvioCancion.Agregada, viewModel.uiState.value.envio)
    }

    @Test
    fun falloConservaLoEscritoYPermiteReintentar() = runTest {
        val repositorio = RepositorioAgregarCancionFalso()
        val viewModel = AgregarCancionViewModel(101, repositorio)
        viewModel.cambiarNombre("Decisiones")
        viewModel.cambiarDuracion("5:05")
        viewModel.guardar()
        advanceUntilIdle()
        repositorio.responder(Result.failure(ErrorDeCarga()))
        advanceUntilIdle()

        assertEquals(EnvioCancion.Fallida, viewModel.uiState.value.envio)
        assertEquals("Decisiones", viewModel.uiState.value.nombre)
        assertEquals("5:05", viewModel.uiState.value.duracion)
        assertTrue(viewModel.uiState.value.puedeGuardar)

        viewModel.guardar()
        advanceUntilIdle()
        assertEquals(2, repositorio.envios.size)
    }

    @Test
    fun albumNoDisponibleSeDistingueDelErrorReintentable() = runTest {
        val repositorio = RepositorioAgregarCancionFalso()
        val viewModel = AgregarCancionViewModel(101, repositorio)
        viewModel.cambiarNombre("Decisiones")
        viewModel.cambiarDuracion("5:05")
        viewModel.guardar()
        advanceUntilIdle()
        repositorio.responder(Result.failure(AlbumNoDisponible()))
        advanceUntilIdle()

        assertEquals(EnvioCancion.AlbumNoDisponible, viewModel.uiState.value.envio)
        assertFalse(viewModel.uiState.value.puedeGuardar)
        viewModel.guardar()
        advanceUntilIdle()
        assertEquals(1, repositorio.envios.size)
    }

    @Test
    fun cargaLaInformacionDelAlbumGuardadoParaMostrarlaComoContextoDelFormularioSinRepetirLaConsultaDeRed() = runTest {
        val repositorio = RepositorioAgregarCancionFalso()
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

        val viewModel = AgregarCancionViewModel(101, repositorio)
        advanceUntilIdle()

        assertEquals(
            InfoAlbumDestino("A Day at the Races", "https://ejemplo.com/portada.jpg", "Queen"),
            viewModel.uiState.value.albumInfo
        )
    }

    @Test
    fun editarDespuesDeUnFalloOcultaElBannerDeErrorAntesDeVolverAGuardar() = runTest {
        val repositorio = RepositorioAgregarCancionFalso()
        val viewModel = AgregarCancionViewModel(101, repositorio)
        viewModel.cambiarNombre("Decisiones")
        viewModel.cambiarDuracion("5:05")
        viewModel.guardar()
        advanceUntilIdle()
        repositorio.responder(Result.failure(ErrorDeCarga()))
        advanceUntilIdle()
        assertEquals(EnvioCancion.Fallida, viewModel.uiState.value.envio)

        viewModel.cambiarNombre("Decisiones (editada)")

        assertEquals(EnvioCancion.Editando, viewModel.uiState.value.envio)
    }

    @Test
    fun unFalloNoReconocidoNoTumbaLaAppYQuedaComoFallidaReintentable() = runTest {
        val repositorio = RepositorioAgregarCancionFalso()
        val viewModel = AgregarCancionViewModel(101, repositorio)
        viewModel.cambiarNombre("Decisiones")
        viewModel.cambiarDuracion("5:05")
        viewModel.guardar()
        advanceUntilIdle()

        repositorio.responder(Result.failure(IllegalStateException("Fallo inesperado del backend")))
        advanceUntilIdle()

        assertEquals(EnvioCancion.Fallida, viewModel.uiState.value.envio)
        assertTrue(viewModel.uiState.value.puedeGuardar)
    }
}
