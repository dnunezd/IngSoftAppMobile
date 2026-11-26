package com.example.vinilos.viewmodel

import com.example.vinilos.MainDispatcherRule
import com.example.vinilos.data.model.Album
import com.example.vinilos.data.model.NewAlbum
import com.example.vinilos.data.model.AlbumSummary
import com.example.vinilos.data.remote.LoadError
import com.example.vinilos.data.repository.AlbumRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Cada envío queda pendiente hasta que la prueba lo completa. */
private class CreacionFalsa : AlbumRepository {
    val envios = mutableListOf<NewAlbum>()
    private val respuestas = mutableListOf<CompletableDeferred<Result<Int>>>()

    override suspend fun createAlbum(album: NewAlbum): Result<Int> {
        envios += album
        val respuesta = CompletableDeferred<Result<Int>>()
        respuestas += respuesta
        return respuesta.await()
    }

    fun responder(resultado: Result<Int>) = respuestas.last().complete(resultado)

    override suspend fun getAlbum(id: Int): Result<Album> = error("No se usa en esta prueba")
    override suspend fun getAlbums(): Result<List<AlbumSummary>> = error("No se usa en esta prueba")
    override suspend fun getCachedAlbums(): List<AlbumSummary> = error("No se usa en esta prueba")
    override suspend fun getCachedAlbum(id: Int): Album = error("No se usa en esta prueba")
    override suspend fun addTrack(
        albumId: Int,
        cancion: com.example.vinilos.data.model.Track
    ): Result<Unit> = error("No se usa en esta prueba")
}

@OptIn(ExperimentalCoroutinesApi::class)
class CreateAlbumViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private val fecha = -8_380_800_000L

    private fun lleno(repositorio: CreacionFalsa = CreacionFalsa()) =
        CreateAlbumViewModel(repositorio).apply {
            cambiarNombre("  Abbey Road ")
            cambiarPortada(" https://example.com/abbey.jpg ")
            cambiarFecha(fecha)
            cambiarGenero("Rock")
            cambiarDisquera("EMI")
            cambiarDescripcion(" Undécimo álbum ")
        }

    @Test
    fun conCamposFaltantesNoEnviaYMarcaLosErrores() = runTest {
        val repositorio = CreacionFalsa()
        val viewModel = CreateAlbumViewModel(repositorio)

        viewModel.create()
        advanceUntilIdle()

        assertTrue(repositorio.envios.isEmpty())
        assertEquals(6, viewModel.uiState.value.errores.size)
        assertEquals(1, viewModel.uiState.value.intentosFallidos)
        assertEquals(EnvioAlbum.Editando, viewModel.uiState.value.envio)
    }

    @Test
    fun conPortadaInvalidaNoEnvia() = runTest {
        val repositorio = CreacionFalsa()
        val viewModel = lleno(repositorio)
        viewModel.cambiarPortada("ftp://servidor/portada.png")

        viewModel.create()
        advanceUntilIdle()

        assertTrue(repositorio.envios.isEmpty())
        assertEquals(
            mapOf(AlbumField.Portada to FieldError.UrlInvalida),
            viewModel.uiState.value.errores
        )
    }

    @Test
    fun editarUnCampoBorraSoloSuError() = runTest {
        val viewModel = CreateAlbumViewModel(CreacionFalsa())
        viewModel.create()

        viewModel.cambiarNombre("A")
        viewModel.cambiarGenero("Rock")

        assertEquals(
            setOf(AlbumField.Portada, AlbumField.Fecha, AlbumField.Disquera, AlbumField.Descripcion),
            viewModel.uiState.value.errores.keys
        )
    }

    @Test
    fun laDescripcionSeLimitaA500Caracteres() = runTest {
        val viewModel = CreateAlbumViewModel(CreacionFalsa())

        viewModel.cambiarDescripcion("a".repeat(501))

        assertEquals(500, viewModel.uiState.value.descripcion.length)
    }

    @Test
    fun formularioValidoPasaPorEnviandoHastaCreado() = runTest {
        val repositorio = CreacionFalsa()
        val viewModel = lleno(repositorio)

        viewModel.create()
        assertEquals(EnvioAlbum.Enviando, viewModel.uiState.value.envio)
        advanceUntilIdle()
        repositorio.responder(Result.success(205))
        advanceUntilIdle()

        assertEquals(EnvioAlbum.Creado(205), viewModel.uiState.value.envio)
    }

    @Test
    fun enviaLosTextosRecortadosYLaFechaElegida() = runTest {
        val repositorio = CreacionFalsa()
        val viewModel = lleno(repositorio)

        viewModel.create()
        advanceUntilIdle()

        assertEquals(
            NewAlbum(
                nombre = "Abbey Road",
                portada = "https://example.com/abbey.jpg",
                fechaLanzamientoMillis = fecha,
                descripcion = "Undécimo álbum",
                genero = "Rock",
                disquera = "EMI"
            ),
            repositorio.envios.single()
        )
    }

    @Test
    fun dosToquesSeguidosHacenUnaSolaPeticion() = runTest {
        val repositorio = CreacionFalsa()
        val viewModel = lleno(repositorio)

        viewModel.create()
        viewModel.create()
        advanceUntilIdle()
        viewModel.create()
        advanceUntilIdle()

        assertEquals(1, repositorio.envios.size)
    }

    @Test
    fun trasCrearNoSePuedeEnviarOtraVez() = runTest {
        val repositorio = CreacionFalsa()
        val viewModel = lleno(repositorio)
        viewModel.create()
        advanceUntilIdle()
        repositorio.responder(Result.success(205))
        advanceUntilIdle()

        viewModel.create()
        advanceUntilIdle()

        assertEquals(1, repositorio.envios.size)
    }

    @Test
    fun mientrasEnviaNoCambianLosCampos() = runTest {
        val viewModel = lleno()
        viewModel.create()
        advanceUntilIdle()

        viewModel.cambiarNombre("Otro")

        assertEquals("  Abbey Road ", viewModel.uiState.value.nombre)
    }

    @Test
    fun siFallaElEnvioConservaLosDatosYQuedaFallido() = runTest {
        val repositorio = CreacionFalsa()
        val viewModel = lleno(repositorio)
        viewModel.create()
        advanceUntilIdle()

        repositorio.responder(Result.failure(LoadError()))
        advanceUntilIdle()

        val estado = viewModel.uiState.value
        assertEquals(EnvioAlbum.Fallido, estado.envio)
        assertEquals("  Abbey Road ", estado.nombre)
        assertEquals(fecha, estado.fechaMillis)
        assertEquals("Rock", estado.genero)
        assertEquals("EMI", estado.disquera)
    }

    @Test
    fun trasFallarSePuedeEditarSinPerderElEstado() = runTest {
        val repositorio = CreacionFalsa()
        val viewModel = lleno(repositorio)
        viewModel.create()
        advanceUntilIdle()
        repositorio.responder(Result.failure(LoadError()))
        advanceUntilIdle()

        viewModel.cambiarNombre("Let It Be")

        assertEquals("Let It Be", viewModel.uiState.value.nombre)
        assertEquals(EnvioAlbum.Fallido, viewModel.uiState.value.envio)
    }

    @Test
    fun retryTrasFallarEnviaDeNuevoYPuedeCrear() = runTest {
        val repositorio = CreacionFalsa()
        val viewModel = lleno(repositorio)
        viewModel.create()
        advanceUntilIdle()
        repositorio.responder(Result.failure(LoadError()))
        advanceUntilIdle()

        viewModel.create()
        assertEquals(EnvioAlbum.Enviando, viewModel.uiState.value.envio)
        advanceUntilIdle()
        repositorio.responder(Result.success(205))
        advanceUntilIdle()

        assertEquals(2, repositorio.envios.size)
        assertEquals(EnvioAlbum.Creado(205), viewModel.uiState.value.envio)
    }

    @Test
    fun siTrasFallarQuedaUnCampoInvalidoVuelveAEditando() = runTest {
        val repositorio = CreacionFalsa()
        val viewModel = lleno(repositorio)
        viewModel.create()
        advanceUntilIdle()
        repositorio.responder(Result.failure(LoadError()))
        advanceUntilIdle()

        viewModel.cambiarNombre("")
        viewModel.create()
        advanceUntilIdle()

        assertEquals(EnvioAlbum.Editando, viewModel.uiState.value.envio)
        assertEquals(1, repositorio.envios.size)
    }
}
