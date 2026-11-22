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
import com.example.vinilos.data.model.ComentarioNuevo
import com.example.vinilos.data.remote.ServiceAdapter
import com.example.vinilos.data.repository.CollectorRepositoryImpl
import com.example.vinilos.data.repository.ComentarioRepository
import com.example.vinilos.data.repository.ComentarioRepositoryImpl
import com.example.vinilos.e2e.AlbumDePrueba
import com.example.vinilos.e2e.AlbumesDePrueba
import com.example.vinilos.e2e.EvidenciaE2E
import com.example.vinilos.ui.catalogo.TagContadorCatalogo
import com.example.vinilos.ui.catalogo.TagCuadriculaCatalogo
import com.example.vinilos.ui.catalogo.tagAlbum
import com.example.vinilos.ui.comentar.TagAceptarPublicado
import com.example.vinilos.ui.comentar.TagAlbumComentado
import com.example.vinilos.ui.comentar.TagComentarioPublicado
import com.example.vinilos.ui.comentar.TagContadorOpinion
import com.example.vinilos.ui.comentar.TagContinuarEscribiendo
import com.example.vinilos.ui.comentar.TagDescartarComentario
import com.example.vinilos.ui.comentar.TagDialogoDescartar
import com.example.vinilos.ui.comentar.TagErrorPublicar
import com.example.vinilos.ui.comentar.TagNivelCalificacion
import com.example.vinilos.ui.comentar.TagOpinion
import com.example.vinilos.ui.comentar.TagPantallaComentar
import com.example.vinilos.ui.comentar.TagPublicarComentario
import com.example.vinilos.ui.comentar.tagEstrella
import com.example.vinilos.ui.detalle.TagComentarAlbum
import com.example.vinilos.ui.detalle.TagListaDetalleAlbum
import com.example.vinilos.ui.ingreso.TagEntrarColeccionista
import com.example.vinilos.ui.ingreso.TagListaColeccionistas
import com.example.vinilos.ui.ingreso.tagColeccionista
import com.example.vinilos.ui.inicio.TagInicio
import com.example.vinilos.ui.inicio.TagVerAlbumes
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
 * E2E de la HU09 (comentar un álbum).
 *
 * FN09-ESC01 y ESC02 van contra el backend real. Como publican comentarios, se corren contra BackVynils
 * local (en un teléfono, `-PbaseUrl=http://localhost:3000/` con `adb reverse tcp:3000 tcp:3000`). Los
 * escenarios comentan un álbum propio creado con `POST /albums`, que [AlbumesDePrueba] borra al terminar
 * con sus comentarios, aunque la prueba falle. El Coleccionista es el primer coleccionista real.
 *
 * FN09-ESC03 (falla al publicar y reintento) usa `MockWebServer`, como FN07-ESC07 y FN10-ESC07.
 *
 * Cada escenario guarda un pantallazo por paso, `informe.md` y un PDF en
 * `.../files/e2e-evidencia/FN09_ESCxx/` (en el dispositivo; los PDF se versionan a mano en
 * `docs/pruebas/sprint-3/HU09/e2e/`).
 */
class ComentarAlbumE2ETest {

    @get:Rule
    val rule = createEmptyComposeRule()

    /** El repositorio real de la app, que además cuenta los comentarios enviados. */
    private class ComentariosReales(
        private val real: ComentarioRepository = ComentarioRepositoryImpl(ServiceAdapter(BuildConfig.BASE_URL))
    ) : ComentarioRepository by real {
        @Volatile var envios = 0

        override suspend fun publicarComentario(albumId: Int, comentario: ComentarioNuevo): Result<Unit> {
            envios++
            return real.publicarComentario(albumId, comentario)
        }
    }

    private val comentarios = ComentariosReales()
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

    // ---------- Datos reales ----------

    private fun primerColeccionistaReal(): Collector = runBlocking {
        CollectorRepositoryImpl(ServiceAdapter(BuildConfig.BASE_URL)).obtenerColeccionistas().getOrThrow().first()
    }

    /** Comentarios del álbum según el backend, como pares opinión-calificación. */
    private fun comentariosEnElBackend(album: AlbumDePrueba): List<Pair<String, Int>> {
        val lista = albumesDePrueba.album(album.id).getJSONArray("comments")
        return (0 until lista.length()).map {
            lista.getJSONObject(it).let { comentario -> comentario.getString("description") to comentario.getInt("rating") }
        }
    }

    // ---------- Navegación ----------

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

    /** Inicio -> "Ver álbumes" -> detalle del álbum [id], cargado y en la sección de comentarios. */
    private fun abrirDetalle(id: Int, evidencia: String, paso: String) {
        rule.onNodeWithTag(TagVerAlbumes).performScrollTo().performClick()
        esperarA(TagContadorCatalogo)
        rule.onNodeWithTag(TagCuadriculaCatalogo).performScrollToNode(hasTestTag(tagAlbum(id)))
        rule.onNodeWithTag(tagAlbum(id)).performClick()
        // La sección puede estar más abajo, aún sin componer: se espera a que el detalle cargue y se llegue a ella.
        rule.waitUntil(15_000) {
            runCatching { rule.onNodeWithTag(TagListaDetalleAlbum).performScrollToNode(hasText("Comentarios")) }.isSuccess
        }
        EvidenciaE2E.capturar(rule, evidencia, paso)
    }

    private fun abrirComentar() {
        rule.onNodeWithTag(TagComentarAlbum).performClick()
        esperarA(TagPublicarComentario)
    }

    private fun calificar(estrellas: Int) = rule.onNodeWithTag(tagEstrella(estrellas)).performScrollTo().performClick()

    private fun opinar(texto: String) {
        rule.onNodeWithTag(TagOpinion).performScrollTo().performTextReplacement(texto)
        Espresso.closeSoftKeyboard()
    }

    private fun publicar() = rule.onNodeWithTag(TagPublicarComentario).performScrollTo()

    /** El comentario [opinion] aparece en la lista del detalle. */
    private fun verComentarioEnElDetalle(opinion: String) {
        rule.waitUntil(15_000) {
            runCatching { rule.onNodeWithTag(TagListaDetalleAlbum).performScrollToNode(hasText(opinion)) }.isSuccess
        }
        rule.onNodeWithText(opinion).assertIsDisplayed()
    }

    // ---------- Escenarios contra el backend real ----------

    /**
     * FN09-ESC01: sin nada escrito se sale sin preguntar; con algo escrito se pregunta "¿Descartar
     * comentario?", "Continuar escribiendo" conserva lo escrito y "Descartar y salir" vuelve sin publicar.
     */
    @Test
    fun salirConAlgoEscritoPreguntaSiDescartarElComentario() {
        val evidencia = "FN09_ESC01"
        val album = albumesDePrueba.crear("E2E FN09")
        val opinion = "Todavía me falta escucharlo completo."

        ActivityScenario.launch(MainActivity::class.java).use {
            entrarComoColeccionista(primerColeccionistaReal().id)
            abrirDetalle(album.id, evidencia, "01_detalle_sin_comentarios")

            // Sin nada escrito, "Atrás" vuelve al detalle sin preguntar.
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
            rule.onNodeWithTag(TagNivelCalificacion).assertTextEquals("Regular (3/5)")
            rule.onNodeWithTag(TagOpinion).assert(hasText(opinion))
            EvidenciaE2E.capturar(rule, evidencia, "03_continuar_escribiendo")

            Espresso.pressBack()
            esperarA(TagDialogoDescartar)
            rule.onNodeWithTag(TagDescartarComentario).performClick()
            esperarQueSeCierreLaPantalla()
            rule.onNodeWithTag(TagListaDetalleAlbum).performScrollToNode(hasText("Comentarios"))
            rule.onNodeWithText("Aún no hay comentarios para este álbum").assertIsDisplayed()
            EvidenciaE2E.capturar(rule, evidencia, "04_detalle_sin_publicar")
        }
        assertEquals(0, comentarios.envios)
        assertEquals(emptyList<Pair<String, Int>>(), comentariosEnElBackend(album))
    }

    /** FN09-ESC02: el Coleccionista califica, escribe su opinión, publica y ve el comentario en el detalle. */
    @Test
    fun elColeccionistaPublicaUnComentario() {
        val evidencia = "FN09_ESC02"
        val album = albumesDePrueba.crear("E2E FN09")
        val coleccionista = primerColeccionistaReal()
        val opinion = "Prensado limpio y un máster con mucho detalle en los graves."

        ActivityScenario.launch(MainActivity::class.java).use {
            entrarComoColeccionista(coleccionista.id)
            abrirDetalle(album.id, evidencia, "01_detalle_sin_comentarios")
            rule.onNodeWithText("Aún no hay comentarios para este álbum").assertIsDisplayed()

            abrirComentar()
            rule.onNodeWithTag(TagAlbumComentado).assertTextEquals(album.nombre)
            // La etiqueta del coleccionista se dibuja en mayúsculas.
            rule.onNodeWithText("${coleccionista.name} (Coleccionista)", ignoreCase = true).performScrollTo().assertIsDisplayed()
            rule.onAllNodesWithTag(TagNivelCalificacion).assertCountEquals(0)
            publicar().assertIsNotEnabled()
            EvidenciaE2E.capturar(rule, evidencia, "02_formulario_vacio")

            calificar(4)
            rule.onNodeWithTag(TagNivelCalificacion).assertTextEquals("Bueno (4/5)")
            opinar(opinion)
            rule.onNodeWithTag(TagContadorOpinion).assertTextEquals("${opinion.length} / 400")
            publicar().assertIsEnabled()
            EvidenciaE2E.capturar(rule, evidencia, "03_formulario_lleno")

            publicar().performClick()
            esperarA(TagComentarioPublicado)
            rule.onNodeWithText("Comentario publicado").assertIsDisplayed()
            rule.onNodeWithText(opinion, substring = true).assertIsDisplayed()
            EvidenciaE2E.capturar(rule, evidencia, "04_comentario_publicado")

            rule.onNodeWithTag(TagAceptarPublicado).performScrollTo().performClick()
            esperarQueSeCierreLaPantalla()
            verComentarioEnElDetalle(opinion)
            EvidenciaE2E.capturar(rule, evidencia, "05_detalle_con_el_comentario")
        }
        assertEquals(1, comentarios.envios)
        assertEquals(listOf(opinion to 4), comentariosEnElBackend(album))
    }

    // ---------- Escenario con MockWebServer ----------

    /** Estado del servidor simulado: el primer envío falla y los siguientes guardan el comentario. */
    private class Simulado {
        @Volatile var envios = 0
        val comentarios = mutableListOf<String>()
    }

    /** Coleccionista 100 y el álbum 100 "Buscando América", sin comentarios hasta que se publica uno. */
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
                            // El primer envío tarda (para ver "Publicando...") y termina en error del servicio.
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
        // "localhost" es uno de los hosts con HTTP permitido en network_security_config.xml.
        ApplicationProvider.getApplicationContext<VinilosApplication>().container =
            AppContainer(baseUrl = "http://localhost:${nuevo.port}/")
    }

    /**
     * FN09-ESC03: mientras se publica se ve "Publicando..." y un segundo toque no envía nada; si falla se
     * ve el error con la calificación y la opinión conservadas, y el reintento publica el comentario.
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

            publicar().performClick()
            rule.waitUntil(2_000) {
                rule.onAllNodes(hasTestTag(TagPublicarComentario) and hasText("Publicando...")).fetchSemanticsNodes().isNotEmpty()
            }
            publicar().assertIsNotEnabled()
            EvidenciaE2E.capturar(rule, evidencia, "03_publicando")
            publicar().performClick()

            esperarA(TagErrorPublicar)
            rule.onNodeWithText("No se pudo publicar, intenta de nuevo").assertIsDisplayed()
            assertEquals("El toque durante \"Publicando...\" no debe enviar otro comentario", 1, simulado.envios)
            rule.onNodeWithTag(TagNivelCalificacion).assertTextEquals("Excelente (5/5)")
            rule.onNodeWithTag(TagOpinion).assert(hasText(opinion))
            publicar().assertIsEnabled()
            EvidenciaE2E.capturar(rule, evidencia, "04_error_al_publicar")

            publicar().performClick()
            esperarA(TagComentarioPublicado)
            EvidenciaE2E.capturar(rule, evidencia, "05_publicado_al_reintentar")
            rule.onNodeWithTag(TagAceptarPublicado).performScrollTo().performClick()
            esperarQueSeCierreLaPantalla()
            verComentarioEnElDetalle(opinion)
            EvidenciaE2E.capturar(rule, evidencia, "06_detalle_con_el_comentario")
        }
        assertEquals(2, simulado.envios)
    }
}
