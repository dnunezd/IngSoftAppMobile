package com.example.vinilos

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import com.example.vinilos.data.model.Collector
import com.example.vinilos.data.model.NewComment
import com.example.vinilos.data.remote.ServiceAdapter
import com.example.vinilos.data.repository.CollectorRepositoryImpl
import com.example.vinilos.data.repository.CommentRepository
import com.example.vinilos.data.repository.CommentRepositoryImpl
import com.example.vinilos.e2e.AlbumDePrueba
import com.example.vinilos.e2e.AlbumesDePrueba
import com.example.vinilos.e2e.EvidenciaE2E
import com.example.vinilos.ui.catalog.TagContadorCatalogo
import com.example.vinilos.ui.catalog.TagCuadriculaCatalogo
import com.example.vinilos.ui.catalog.tagAlbum
import com.example.vinilos.ui.comentar.TagAceptarPublicado
import com.example.vinilos.ui.comentar.TagAlbumComentado
import com.example.vinilos.ui.comentar.TagCommentPublicado
import com.example.vinilos.ui.comentar.TagContadorOpinion
import com.example.vinilos.ui.comentar.TagContinuarEscribiendo
import com.example.vinilos.ui.comentar.TagDescartarComment
import com.example.vinilos.ui.comentar.TagDialogoDescartar
import com.example.vinilos.ui.comentar.TagErrorPublicar
import com.example.vinilos.ui.comentar.TagRatingLevel
import com.example.vinilos.ui.comentar.TagOpinion
import com.example.vinilos.ui.comentar.TagPantallaComentar
import com.example.vinilos.ui.comentar.TagPublicarComment
import com.example.vinilos.ui.comentar.tagEstrella
import com.example.vinilos.ui.detail.TagAddComment
import com.example.vinilos.ui.detail.TagListaDetalleAlbum
import com.example.vinilos.ui.entry.TagEntrarColeccionista
import com.example.vinilos.ui.entry.TagListaColeccionistas
import com.example.vinilos.ui.entry.tagColeccionista
import com.example.vinilos.ui.home.TagInicio
import com.example.vinilos.ui.home.TagVerAlbumes
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * E2E for HU09 (commenting on an album).
 *
 * FN09-ESC01 and ESC02 run against the real backend. Since they publish comments, they run against a
 * local BackVynils (on a phone, `-PbaseUrl=http://localhost:3000/` with `adb reverse tcp:3000 tcp:3000`).
 * The scenarios comment on their own album created with `POST /albums`, which [AlbumesDePrueba] deletes
 * at the end along with its comments, even if the test fails. The Collector is the first real collector.
 *
 * FN09-ESC03 (publish failure and retry) uses `MockWebServer`, like FN07-ESC07 and FN10-ESC07.
 *
 * Each scenario saves a screenshot per step, `informe.md` and a PDF in
 * `.../files/e2e-evidencia/FN09_ESCxx/` (on the device; the PDFs are versioned by hand in
 * `docs/pruebas/sprint-3/HU09/e2e/`).
 */
class AddCommentE2ETest {

    @get:Rule
    val rule = createEmptyComposeRule()

    /** The app's real repository, which also counts the comments sent. */
    private class CommentsReales(
        private val real: CommentRepository = CommentRepositoryImpl(ServiceAdapter(BuildConfig.BASE_URL))
    ) : CommentRepository by real {
        @Volatile var envios = 0

        override suspend fun postComment(albumId: Int, comentario: NewComment): Result<Unit> {
            envios++
            return real.postComment(albumId, comentario)
        }
    }

    private val comentarios = CommentsReales()
    private val albumesDePrueba = AlbumesDePrueba()
    private var servidor: MockWebServer? = null

    @Before
    fun usarElBackendReal() {
        ApplicationProvider.getApplicationContext<VinilosApplication>().container =
            AppContainer(comentariosRepositorio = comentarios)
    }

    @After
    fun borrarAlbumesDePrueba() {
        servidor?.shutdown()
        albumesDePrueba.borrarTodos()
    }

    // ---------- Real data ----------

    private fun primerColeccionistaReal(): Collector = runBlocking {
        CollectorRepositoryImpl(ServiceAdapter(BuildConfig.BASE_URL)).getCollectors().getOrThrow().first()
    }

    /** The album's comments according to the backend, as opinion-rating pairs. */
    private fun comentariosEnElBackend(album: AlbumDePrueba): List<Pair<String, Int>> {
        val lista = albumesDePrueba.album(album.id).getJSONArray("comments")
        return (0 until lista.length()).map {
            lista.getJSONObject(it).let { comentario -> comentario.getString("description") to comentario.getInt("rating") }
        }
    }

    // ---------- Navigation ----------

    private fun esperarA(tag: String, milis: Long = 15_000) =
        rule.waitUntil(milis) { rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }

    private fun esperarQueSeCierreLaPantalla() =
        rule.waitUntil(15_000) { rule.onAllNodesWithTag(TagPantallaComentar).fetchSemanticsNodes().isEmpty() }

    private fun entrarComoColeccionista(coleccionista: Int) {
        esperarA(TagEntrarColeccionista)
        rule.onNodeWithTag(TagEntrarColeccionista).performScrollTo().performClick()
        rule.waitUntil(15_000) {
            runCatching {
                rule.onNodeWithTag(TagListaColeccionistas).performScrollToNode(hasTestTag(tagColeccionista(coleccionista)))
            }.isSuccess
        }
        rule.onNodeWithTag(tagColeccionista(coleccionista)).performClick()
        esperarA(TagInicio)
    }

    /** Home -> "Ver álbumes" -> detail of album [id], loaded and scrolled to the comments section. */
    private fun abrirDetalle(id: Int, evidencia: String, paso: String) {
        rule.onNodeWithTag(TagVerAlbumes).performScrollTo().performClick()
        esperarA(TagContadorCatalogo)
        rule.onNodeWithTag(TagCuadriculaCatalogo).performScrollToNode(hasTestTag(tagAlbum(id)))
        rule.onNodeWithTag(tagAlbum(id)).performClick()
        // The section may be further down, still not composed: wait for the detail to load and scroll to it.
        rule.waitUntil(15_000) {
            runCatching { rule.onNodeWithTag(TagListaDetalleAlbum).performScrollToNode(hasText("Comentarios")) }.isSuccess
        }
        EvidenciaE2E.capturar(rule, evidencia, paso)
    }

    private fun abrirComentar() {
        rule.onNodeWithTag(TagAddComment).performClick()
        esperarA(TagPublicarComment)
    }

    private fun calificar(estrellas: Int) = rule.onNodeWithTag(tagEstrella(estrellas)).performScrollTo().performClick()

    private fun opinar(texto: String) {
        rule.onNodeWithTag(TagOpinion).performScrollTo().performTextReplacement(texto)
        Espresso.closeSoftKeyboard()
    }

    private fun post() = rule.onNodeWithTag(TagPublicarComment).performScrollTo()

    /** The comment [opinion] appears in the detail's list. */
    private fun verCommentEnElDetalle(opinion: String) {
        rule.waitUntil(15_000) {
            runCatching { rule.onNodeWithTag(TagListaDetalleAlbum).performScrollToNode(hasText(opinion)) }.isSuccess
        }
        rule.onNodeWithText(opinion).assertIsDisplayed()
    }

    // ---------- Scenarios against the real backend ----------

    /**
     * FN09-ESC01: with nothing written, exiting happens without a prompt; with something written, it
     * asks "¿Descartar comentario?", "Continuar escribiendo" keeps what was written, and "Descartar y
     * salir" returns without publishing.
     */
    @Test
    fun salirConAlgoEscritoPreguntaSiDescartarElComment() {
        val evidencia = "FN09_ESC01"
        val album = albumesDePrueba.create("E2E FN09")
        val opinion = "Todavía me falta escucharlo completo."

        ActivityScenario.launch(MainActivity::class.java).use {
            entrarComoColeccionista(primerColeccionistaReal().id)
            abrirDetalle(album.id, evidencia, "01_detalle_sin_comentarios")

            // With nothing written, "Atrás" returns to the detail without asking.
            abrirComentar()
            Espresso.pressBack()
            esperarQueSeCierreLaPantalla()
            rule.onAllNodesWithTag(TagDialogoDescartar).assertCountEquals(0)
            esperarA(TagListaDetalleAlbum)

            abrirComentar()
            calificar(3)
            opinar(opinion)
            Espresso.pressBack()
            esperarA(TagDialogoDescartar)
            rule.onNodeWithText("¿Descartar comentario?").assertIsDisplayed()
            EvidenciaE2E.capturar(rule, evidencia, "02_dialogo_descartar")

            rule.onNodeWithTag(TagContinuarEscribiendo).performClick()
            rule.waitUntil(5_000) { rule.onAllNodesWithTag(TagDialogoDescartar).fetchSemanticsNodes().isEmpty() }
            rule.onNodeWithTag(TagRatingLevel).assertTextEquals("Regular (3/5)")
            rule.onNodeWithTag(TagOpinion).assert(hasText(opinion))
            EvidenciaE2E.capturar(rule, evidencia, "03_continuar_escribiendo")

            Espresso.pressBack()
            esperarA(TagDialogoDescartar)
            rule.onNodeWithTag(TagDescartarComment).performClick()
            esperarQueSeCierreLaPantalla()
            rule.onNodeWithTag(TagListaDetalleAlbum).performScrollToNode(hasText("Comentarios"))
            rule.onNodeWithText("Aún no hay comentarios para este álbum").assertIsDisplayed()
            EvidenciaE2E.capturar(rule, evidencia, "04_detalle_sin_publicar")
        }
        assertEquals(0, comentarios.envios)
        assertEquals(emptyList<Pair<String, Int>>(), comentariosEnElBackend(album))
    }

    /** FN09-ESC02: the Collector rates, writes an opinion, publishes it, and sees the comment in the detail. */
    @Test
    fun elColeccionistaPublicaUnComment() {
        val evidencia = "FN09_ESC02"
        val album = albumesDePrueba.create("E2E FN09")
        val coleccionista = primerColeccionistaReal()
        val opinion = "Prensado limpio y un máster con mucho detalle en los graves."

        ActivityScenario.launch(MainActivity::class.java).use {
            entrarComoColeccionista(coleccionista.id)
            abrirDetalle(album.id, evidencia, "01_detalle_sin_comentarios")
            rule.onNodeWithText("Aún no hay comentarios para este álbum").assertIsDisplayed()

            abrirComentar()
            rule.onNodeWithTag(TagAlbumComentado).assertTextEquals(album.nombre)
            // The collector's label is drawn in uppercase.
            rule.onNodeWithText("${coleccionista.name} (Coleccionista)", ignoreCase = true).performScrollTo().assertIsDisplayed()
            rule.onAllNodesWithTag(TagRatingLevel).assertCountEquals(0)
            post().assertIsNotEnabled()
            EvidenciaE2E.capturar(rule, evidencia, "02_formulario_vacio")

            calificar(4)
            rule.onNodeWithTag(TagRatingLevel).assertTextEquals("Bueno (4/5)")
            opinar(opinion)
            rule.onNodeWithTag(TagContadorOpinion).assertTextEquals("${opinion.length} / 400")
            post().assertIsEnabled()
            EvidenciaE2E.capturar(rule, evidencia, "03_formulario_lleno")

            post().performClick()
            esperarA(TagCommentPublicado)
            rule.onNodeWithText("Comentario publicado").assertIsDisplayed()
            rule.onNodeWithText(opinion, substring = true).assertIsDisplayed()
            EvidenciaE2E.capturar(rule, evidencia, "04_comentario_publicado")

            rule.onNodeWithTag(TagAceptarPublicado).performScrollTo().performClick()
            esperarQueSeCierreLaPantalla()
            verCommentEnElDetalle(opinion)
            EvidenciaE2E.capturar(rule, evidencia, "05_detalle_con_el_comentario")
        }
        assertEquals(1, comentarios.envios)
        assertEquals(listOf(opinion to 4), comentariosEnElBackend(album))
    }

    // ---------- Scenario with MockWebServer ----------

    /** State of the simulated server: the first submission fails and the following ones save the comment. */
    private class Simulado {
        @Volatile var envios = 0
        val comentarios = mutableListOf<String>()
    }

    /** Collector 100 and album 100 "Buscando América", with no comments until one is published. */
    private fun iniciarServidor(simulado: Simulado) {
        fun album() = """
            {"id": 100, "name": "Buscando América", "cover": null, "description": "Salsa urbana",
             "genre": "Salsa", "recordLabel": "Elektra", "releaseDate": "1984-08-01T00:00:00.000Z",
             "tracks": [], "performers": [],
             "comments": [${synchronized(simulado.comentarios) { simulado.comentarios.joinToString() }}]}
        """.trimIndent()
        val nuevo = MockWebServer().apply {
            dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse = when {
                    request.path == "/collectors" -> MockResponse().setBody(
                        """[{"id": 100, "name": "Manolo Bellon", "email": "manollo@caracol.com.co"}]"""
                    )
                    request.path == "/albums" -> MockResponse().setBody("[${album()}]")
                    request.path == "/albums/100/comments" && request.method == "POST" -> {
                        simulado.envios++
                        if (simulado.envios == 1) {
                            // The first submission takes a while (to see "Publicando...") and ends in a server error.
                            MockResponse().setResponseCode(500).setHeadersDelay(3, TimeUnit.SECONDS)
                        } else {
                            val enviado = JSONObject(request.body.readUtf8())
                            val guardado = JSONObject()
                                .put("id", simulado.envios)
                                .put("description", enviado.getString("description"))
                                .put("rating", enviado.getInt("rating"))
                                .toString()
                            synchronized(simulado.comentarios) { simulado.comentarios += guardado }
                            MockResponse().setBody(guardado)
                        }
                    }
                    request.path == "/albums/100" -> MockResponse().setBody(album())
                    else -> MockResponse().setResponseCode(404)
                }
            }
            start()
        }
        servidor = nuevo
        // "localhost" is one of the hosts allowed over HTTP in network_security_config.xml.
        ApplicationProvider.getApplicationContext<VinilosApplication>().container =
            AppContainer(baseUrl = "http://localhost:${nuevo.port}/")
    }

    /**
     * FN09-ESC03: while publishing, "Publicando..." is shown and a second tap sends nothing; if it fails,
     * the error is shown with the rating and opinion preserved, and retrying publishes the comment.
     */
    @Test
    fun siFallaLaPublicacionSeConservaLoEscritoYReintentarLaPublica() {
        val evidencia = "FN09_ESC03"
        val simulado = Simulado()
        iniciarServidor(simulado)
        val opinion = "Un clásico de la salsa urbana."

        ActivityScenario.launch(MainActivity::class.java).use {
            entrarComoColeccionista(100)
            abrirDetalle(100, evidencia, "01_detalle_sin_comentarios")
            abrirComentar()
            calificar(5)
            opinar(opinion)
            EvidenciaE2E.capturar(rule, evidencia, "02_formulario_lleno")

            post().performClick()
            rule.waitUntil(2_000) {
                rule.onAllNodes(hasTestTag(TagPublicarComment) and hasText("Publicando...")).fetchSemanticsNodes().isNotEmpty()
            }
            post().assertIsNotEnabled()
            EvidenciaE2E.capturar(rule, evidencia, "03_publicando")
            post().performClick()

            esperarA(TagErrorPublicar)
            rule.onNodeWithText("No se pudo publicar, intenta de nuevo").assertIsDisplayed()
            assertEquals("El toque durante \"Publicando...\" no debe enviar otro comentario", 1, simulado.envios)
            rule.onNodeWithTag(TagRatingLevel).assertTextEquals("Excelente (5/5)")
            rule.onNodeWithTag(TagOpinion).assert(hasText(opinion))
            post().assertIsEnabled()
            EvidenciaE2E.capturar(rule, evidencia, "04_error_al_publicar")

            post().performClick()
            esperarA(TagCommentPublicado)
            EvidenciaE2E.capturar(rule, evidencia, "05_publicado_al_reintentar")
            rule.onNodeWithTag(TagAceptarPublicado).performScrollTo().performClick()
            esperarQueSeCierreLaPantalla()
            verCommentEnElDetalle(opinion)
            EvidenciaE2E.capturar(rule, evidencia, "06_detalle_con_el_comentario")
        }
        assertEquals(2, simulado.envios)
    }
}
