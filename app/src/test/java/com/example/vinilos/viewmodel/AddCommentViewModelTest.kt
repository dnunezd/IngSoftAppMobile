package com.example.vinilos.viewmodel

import com.example.vinilos.MainDispatcherRule
import com.example.vinilos.data.model.Album
import com.example.vinilos.data.model.NewAlbum
import com.example.vinilos.data.model.AlbumSummary
import com.example.vinilos.data.model.Track
import com.example.vinilos.data.model.NewComment
import com.example.vinilos.data.remote.LoadError
import com.example.vinilos.data.repository.AlbumNotAvailable
import com.example.vinilos.data.repository.AlbumRepository
import com.example.vinilos.data.repository.CommentRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private val album = Album(
    id = 100, nombre = "Buscando América", portada = null, descripcion = "Salsa urbana", genero = "Salsa",
    disquera = "Elektra", fechaLanzamiento = "1984-08-01T00:00:00.000Z", canciones = emptyList(),
    artistas = emptyList(), comentarios = emptyList()
)

/** Albums: the saved one responds immediately and each network query stays pending until the test completes it. */
private class AlbumesFalsos : AlbumRepository {
    val guardados = mutableMapOf<Int, Album>()
    val consultas = mutableListOf<Pair<Int, CompletableDeferred<Result<Album>>>>()

    override suspend fun getCachedAlbum(id: Int): Album? = guardados[id]

    override suspend fun getAlbum(id: Int): Result<Album> {
        val respuesta = CompletableDeferred<Result<Album>>()
        consultas += id to respuesta
        return respuesta.await()
    }

    fun responder(resultado: Result<Album>) = consultas.last().second.complete(resultado)

    override suspend fun getAlbums(): Result<List<AlbumSummary>> = error("No se usa en esta prueba")
    override suspend fun getCachedAlbums(): List<AlbumSummary> = error("No se usa en esta prueba")
    override suspend fun createAlbum(album: NewAlbum): Result<Int> = error("No se usa en esta prueba")
    override suspend fun addTrack(albumId: Int, cancion: Track): Result<Unit> = error("No se usa en esta prueba")
}

/** Each post stays pending until the test completes it. */
private class CommentsFalsos : CommentRepository {
    val envios = mutableListOf<Pair<Int, NewComment>>()
    private val respuestas = mutableListOf<CompletableDeferred<Result<Unit>>>()

    override suspend fun postComment(albumId: Int, comentario: NewComment): Result<Unit> {
        envios += albumId to comentario
        val respuesta = CompletableDeferred<Result<Unit>>()
        respuestas += respuesta
        return respuesta.await()
    }

    fun responder(resultado: Result<Unit>) = respuestas.last().complete(resultado)
}

@OptIn(ExperimentalCoroutinesApi::class)
class AddCommentViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private val albumes = AlbumesFalsos().apply { guardados[100] = album }
    private val comentarios = CommentsFalsos()

    private fun viewModel() = AddCommentViewModel(
        albumId = 100, coleccionistaId = 101, albumRepository = albumes, comentarioRepository = comentarios
    )

    private fun AddCommentViewModel.lleno() = apply {
        calificar(5)
        cambiarOpinion(" Gran prensado ")
    }

    // ---------- Album (D3) ----------

    @Test
    fun muestraElAlbumGuardadoSinConsultarElBackend() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        assertEquals(CargaAlbum.Listo(album), viewModel.uiState.value.album)
        assertTrue("No debe consultar el backend", albumes.consultas.isEmpty())
    }

    @Test
    fun sinAlbumGuardadoLoConsulta() = runTest {
        albumes.guardados.clear()
        val viewModel = viewModel()
        advanceUntilIdle()
        assertEquals(CargaAlbum.Loading, viewModel.uiState.value.album)
        assertEquals(100, albumes.consultas.single().first)

        albumes.responder(Result.success(album))
        advanceUntilIdle()

        assertEquals(CargaAlbum.Listo(album), viewModel.uiState.value.album)
    }

    @Test
    fun siLaConsultaFallaMuestraErrorYSePuedeReintentar() = runTest {
        albumes.guardados.clear()
        val viewModel = viewModel()
        advanceUntilIdle()
        albumes.responder(Result.failure(LoadError()))
        advanceUntilIdle()
        assertEquals(CargaAlbum.Error, viewModel.uiState.value.album)

        viewModel.retry()
        advanceUntilIdle()
        assertEquals(CargaAlbum.Loading, viewModel.uiState.value.album)
        albumes.responder(Result.success(album))
        advanceUntilIdle()

        assertEquals(CargaAlbum.Listo(album), viewModel.uiState.value.album)
        assertEquals(2, albumes.consultas.size)
    }

    @Test
    fun siElBackendYaNoTieneElAlbumMuestraError() = runTest {
        albumes.guardados.clear()
        val viewModel = viewModel()
        advanceUntilIdle()

        albumes.responder(Result.failure(AlbumNotAvailable()))
        advanceUntilIdle()

        assertEquals(CargaAlbum.Error, viewModel.uiState.value.album)
    }

    @Test
    fun retrySinErrorNoVuelveAConsultar() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.retry()
        advanceUntilIdle()

        assertTrue(albumes.consultas.isEmpty())
    }

    // ---------- Editing ----------

    @Test
    fun calificarGuardaLaEstrellaElegida() = runTest {
        val viewModel = viewModel()

        viewModel.calificar(4)

        assertEquals(4, viewModel.uiState.value.calificacion)
    }

    @Test
    fun unaCalificacionFueraDeRangoSeIgnora() = runTest {
        val viewModel = viewModel()
        viewModel.calificar(3)

        viewModel.calificar(0)
        viewModel.calificar(6)

        assertEquals(3, viewModel.uiState.value.calificacion)
    }

    @Test
    fun laOpinionConservaSoloLosPrimeros400Caracteres() = runTest {
        val viewModel = viewModel()

        viewModel.cambiarOpinion("a".repeat(450))

        assertEquals("a".repeat(400), viewModel.uiState.value.opinion)
    }

    // ---------- Post (D4) ----------

    @Test
    fun publicaLaOpinionSinEspaciosLaCalificacionYElColeccionista() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.lleno()

        viewModel.post()
        advanceUntilIdle()

        assertEquals(EnvioComment.Enviando, viewModel.uiState.value.envio)
        assertEquals(listOf(100 to NewComment(101, 5, "Gran prensado")), comentarios.envios)
    }

    @Test
    fun alPublicarseQuedaPublicado() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.lleno().post()
        advanceUntilIdle()

        comentarios.responder(Result.success(Unit))
        advanceUntilIdle()

        assertEquals(EnvioComment.Publicado, viewModel.uiState.value.envio)
        assertEquals(5, viewModel.uiState.value.calificacion)
        assertEquals(" Gran prensado ", viewModel.uiState.value.opinion)
    }

    @Test
    fun siFallaQuedaFallidoYConservaLoEscrito() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.lleno().post()
        advanceUntilIdle()

        comentarios.responder(Result.failure(LoadError()))
        advanceUntilIdle()

        val estado = viewModel.uiState.value
        assertEquals(EnvioComment.Fallido, estado.envio)
        assertEquals(5, estado.calificacion)
        assertEquals(" Gran prensado ", estado.opinion)
        assertTrue("Se puede reintentar", estado.puedePublicar)
    }

    @Test
    fun despuesDelErrorSePuedeReintentar() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.lleno().post()
        advanceUntilIdle()
        comentarios.responder(Result.failure(LoadError()))
        advanceUntilIdle()

        viewModel.post()
        advanceUntilIdle()
        comentarios.responder(Result.success(Unit))
        advanceUntilIdle()

        assertEquals(2, comentarios.envios.size)
        assertEquals(EnvioComment.Publicado, viewModel.uiState.value.envio)
    }

    @Test
    fun editarDespuesDelErrorOcultaElError() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.lleno().post()
        advanceUntilIdle()
        comentarios.responder(Result.failure(LoadError()))
        advanceUntilIdle()

        viewModel.cambiarOpinion("Gran prensado, mejor que el CD")

        assertEquals(EnvioComment.Editando, viewModel.uiState.value.envio)
    }

    @Test
    fun dosToquesSeguidosEnvianUnSoloComment() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.lleno()

        viewModel.post()
        viewModel.post()
        advanceUntilIdle()

        assertEquals(1, comentarios.envios.size)
    }

    @Test
    fun sinCamposCompletosNoEnviaNada() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.calificar(5)

        viewModel.post()
        advanceUntilIdle()

        assertTrue(comentarios.envios.isEmpty())
        assertEquals(EnvioComment.Editando, viewModel.uiState.value.envio)
    }

    @Test
    fun duranteElEnvioNoSePuedeEditar() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.lleno().post()
        advanceUntilIdle()

        viewModel.calificar(1)
        viewModel.cambiarOpinion("Otra cosa")

        assertEquals(5, viewModel.uiState.value.calificacion)
        assertEquals(" Gran prensado ", viewModel.uiState.value.opinion)
    }

    @Test
    fun despuesDePublicarNoSePuedeEditarNiVolverAPublicar() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.lleno().post()
        advanceUntilIdle()
        comentarios.responder(Result.success(Unit))
        advanceUntilIdle()

        viewModel.calificar(1)
        viewModel.post()
        advanceUntilIdle()

        assertEquals(5, viewModel.uiState.value.calificacion)
        assertEquals(1, comentarios.envios.size)
    }

    // ---------- Exit (D5) ----------

    @Test
    fun sinNadaEscritoSaleSinPreguntar() = runTest {
        val viewModel = viewModel()

        viewModel.pedirSalir()

        assertTrue(viewModel.uiState.value.salir)
        assertFalse(viewModel.uiState.value.confirmarDescarte)
    }

    @Test
    fun conAlgoEscritoPreguntaAntesDeSalir() = runTest {
        val viewModel = viewModel()
        viewModel.calificar(3)

        viewModel.pedirSalir()

        assertTrue(viewModel.uiState.value.confirmarDescarte)
        assertFalse(viewModel.uiState.value.salir)
    }

    @Test
    fun continuarEscribiendoCierraElDialogoYConservaLoEscrito() = runTest {
        val viewModel = viewModel()
        viewModel.lleno().pedirSalir()

        viewModel.continuarEscribiendo()

        val estado = viewModel.uiState.value
        assertFalse(estado.confirmarDescarte)
        assertFalse(estado.salir)
        assertEquals(5, estado.calificacion)
        assertEquals(" Gran prensado ", estado.opinion)
    }

    @Test
    fun descartarSale() = runTest {
        val viewModel = viewModel()
        viewModel.lleno().pedirSalir()

        viewModel.descartar()

        assertTrue(viewModel.uiState.value.salir)
        assertFalse(viewModel.uiState.value.confirmarDescarte)
    }

    @Test
    fun duranteElEnvioNoSePuedeSalir() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.lleno().post()
        advanceUntilIdle()

        viewModel.pedirSalir()

        assertFalse(viewModel.uiState.value.salir)
        assertFalse(viewModel.uiState.value.confirmarDescarte)
    }

    @Test
    fun despuesDePublicarSaleSinPreguntar() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.lleno().post()
        advanceUntilIdle()
        comentarios.responder(Result.success(Unit))
        advanceUntilIdle()

        viewModel.pedirSalir()

        assertTrue(viewModel.uiState.value.salir)
        assertFalse(viewModel.uiState.value.confirmarDescarte)
    }

    @Test
    fun alEmpezarNoHayCalificacionNiOpinion() = runTest {
        val estado = viewModel().uiState.value

        assertNull(estado.calificacion)
        assertEquals("", estado.opinion)
        assertEquals(EnvioComment.Editando, estado.envio)
    }
}
