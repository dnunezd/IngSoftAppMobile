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

    /** Resultado para la carga (mejor esfuerzo) de la tarjeta de contexto del álbum destino. */
    var album: Result<Album> = Result.failure(ErrorDeCarga())

    override suspend fun agregarCancion(albumId: Int, cancion: Cancion): Result<Unit> {
        envios += albumId to cancion
        return CompletableDeferred<Result<Unit>>().also(respuestas::add).await()
    }

    fun responder(resultado: Result<Unit>) {
        respuestas.last().complete(resultado)
    }

    override suspend fun obtenerAlbum(id: Int): Result<Album> = album
    override suspend fun obtenerAlbumes(): Result<List<AlbumResumen>> = error("No se usa en esta prueba")
    override suspend fun obtenerAlbumesGuardados(): List<AlbumResumen> = error("No se usa en esta prueba")
    override suspend fun obtenerAlbumGuardado(id: Int): Album? = error("No se usa en esta prueba")
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
    fun cargaLaInformacionDelAlbumParaMostrarlaComoContextoDelFormulario() = runTest {
        val repositorio = RepositorioAgregarCancionFalso()
        repositorio.album = Result.success(
            Album(
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
        )

        val viewModel = AgregarCancionViewModel(101, repositorio)
        advanceUntilIdle()

        assertEquals(
            InfoAlbumDestino("A Day at the Races", "https://ejemplo.com/portada.jpg", "Queen"),
            viewModel.uiState.value.albumInfo
        )
    }

    @Test
    fun siFallaLaCargaDelAlbumElFormularioSigueUsableSinTarjetaDeContexto() = runTest {
        val repositorio = RepositorioAgregarCancionFalso()
        repositorio.album = Result.failure(AlbumNoDisponible())

        val viewModel = AgregarCancionViewModel(101, repositorio)
        advanceUntilIdle()

        assertEquals(null, viewModel.uiState.value.albumInfo)
        assertEquals(EnvioCancion.Editando, viewModel.uiState.value.envio)
    }
}
