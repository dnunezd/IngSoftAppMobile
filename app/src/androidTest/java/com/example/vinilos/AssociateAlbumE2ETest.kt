package com.example.vinilos

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import com.example.vinilos.data.model.Artist
import com.example.vinilos.data.model.ArtistType
import com.example.vinilos.data.remote.ServiceAdapter
import com.example.vinilos.data.repository.ArtistRepository
import com.example.vinilos.data.repository.ArtistRepositoryImpl
import com.example.vinilos.data.repository.CollectorRepositoryImpl
import com.example.vinilos.e2e.AlbumDePrueba
import com.example.vinilos.e2e.AlbumesDePrueba
import com.example.vinilos.e2e.EvidenciaE2E
import com.example.vinilos.ui.artistas.TagAsociandoAsociar
import com.example.vinilos.ui.artistas.TagBotonAgregarAlbum
import com.example.vinilos.ui.artistas.TagBotonAsociar
import com.example.vinilos.ui.artistas.TagBusquedaAsociar
import com.example.vinilos.ui.artistas.TagCancelarAsociar
import com.example.vinilos.ui.artistas.TagContadorArtistas
import com.example.vinilos.ui.artistas.TagArtistDetail
import com.example.vinilos.ui.artistas.TagErrorAsociar
import com.example.vinilos.ui.artistas.TagListaArtistas
import com.example.vinilos.ui.artistas.TagListaAsociar
import com.example.vinilos.ui.artistas.TagListaArtistDetail
import com.example.vinilos.ui.artistas.TagNombreArtistDetail
import com.example.vinilos.ui.artistas.TagSinResultadosAsociar
import com.example.vinilos.ui.artistas.TagTituloAsociar
import com.example.vinilos.ui.artistas.TagVentanaAsociar
import com.example.vinilos.ui.artistas.tagAlbumArtista
import com.example.vinilos.ui.artistas.tagAlbumAsociable
import com.example.vinilos.ui.artistas.tagArtista
import com.example.vinilos.ui.artistas.tagYaAsociado
import com.example.vinilos.ui.entry.TagEntrarColeccionista
import com.example.vinilos.ui.entry.TagListaColeccionistas
import com.example.vinilos.ui.entry.tagColeccionista
import com.example.vinilos.ui.home.TagInicio
import com.example.vinilos.ui.home.TagVerArtistas
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * E2E for HU15 (adding an album to an artist).
 *
 * FN15-ESC01 and ESC02 run against the real backend. Since they associate albums, they run against a
 * local BackVynils (on a phone, `-PbaseUrl=http://localhost:3000/` with `adb reverse tcp:3000 tcp:3000`).
 * What gets associated is always its own album created with `POST /albums`: [AlbumesDePrueba] deletes it
 * at the end, along with its relationship to the artist, even if the test fails; the artist's real
 * albums are not touched. The artists are the first musician and the first band from the backend; if
 * there are none, the scenario is skipped. The Collector is the first real collector.
 *
 * FN15-ESC03 (association failure and retry) uses `MockWebServer`, like FN07-ESC07 and FN10-ESC07.
 *
 * Each scenario saves a screenshot per step, `informe.md` and a PDF in
 * `.../files/e2e-evidencia/FN15_ESCxx/` (on the device; the PDFs are versioned by hand in
 * `docs/pruebas/sprint-3/HU15/e2e/`).
 */
class AssociateAlbumE2ETest {

    @get:Rule
    val rule = createEmptyComposeRule()

    /** The app's real repository, which also counts the association submissions. */
    private class ArtistasReales(
        private val real: ArtistRepository = ArtistRepositoryImpl(ServiceAdapter(BuildConfig.BASE_URL))
    ) : ArtistRepository by real {
        @Volatile var envios = 0

        override suspend fun associateAlbum(tipo: ArtistType, idArtista: Int, idAlbum: Int): Result<Unit> {
            envios++
            return real.associateAlbum(tipo, idArtista, idAlbum)
        }
    }

    private val artistas = ArtistasReales()
    private val albumesDePrueba = AlbumesDePrueba()
    private var servidor: MockWebServer? = null

    @Before
    fun usarElBackendReal() {
        ApplicationProvider.getApplicationContext<VinilosApplication>().container =
            AppContainer(artistasRepositorio = artistas)
    }

    @After
    fun borrarAlbumesDePrueba() {
        servidor?.shutdown()
        albumesDePrueba.borrarTodos()
    }

    // ---------- Real data ----------

    /** The first band or the first musician from the backend; if there is none, the scenario is skipped. */
    private fun primerArtista(tipo: ArtistType): Artist {
        val artista = runBlocking {
            ArtistRepositoryImpl(ServiceAdapter(BuildConfig.BASE_URL)).getArtists().getOrThrow()
        }.firstOrNull { it.type == tipo }
        Assume.assumeTrue("Sin datos en el backend: no hay ningún artista de tipo $tipo", artista != null)
        return artista!!
    }

    private fun primerColeccionistaReal(): Int = runBlocking {
        CollectorRepositoryImpl(ServiceAdapter(BuildConfig.BASE_URL)).getCollectors().getOrThrow().first().id
    }

    private fun ruta(artista: Artist) = if (artista.type == ArtistType.BANDA) "bands" else "musicians"

    private fun albumesEnElBackend(artista: Artist): Set<Int> = albumesDePrueba.albumesDe(ruta(artista), artista.id)

    // ---------- Navigation ----------

    private fun esperarA(tag: String, milis: Long = 15_000) =
        rule.waitUntil(milis) { rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }

    private fun esperarTexto(texto: String, milis: Long = 15_000) =
        rule.waitUntil(milis) { rule.onAllNodesWithText(texto).fetchSemanticsNodes().isNotEmpty() }

    private fun esperarQueSeCierreLaVentana() =
        rule.waitUntil(15_000) { rule.onAllNodesWithTag(TagVentanaAsociar).fetchSemanticsNodes().isEmpty() }

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

    /** Home -> "Ver artistas" -> detail of [artista], loaded and scrolled to the albums section. */
    private fun abrirDetalle(artista: Artist, evidencia: String, paso: String) {
        rule.onNodeWithTag(TagVerArtistas).performScrollTo().performClick()
        esperarA(TagContadorArtistas)
        rule.onNodeWithTag(TagListaArtistas).performScrollToNode(hasTestTag(tagArtista(artista)))
        rule.onNodeWithTag(tagArtista(artista)).performClick()
        esperarA(TagArtistDetail)
        esperarA(TagNombreArtistDetail)
        rule.onNodeWithTag(TagNombreArtistDetail).assertTextEquals(artista.name)
        rule.onNodeWithTag(TagListaArtistDetail).performScrollToNode(hasText("Álbumes"))
        EvidenciaE2E.capturar(rule, evidencia, paso)
    }

    /** "Agregar álbum" -> window opened with the catalog loaded. */
    private fun abrirVentana() {
        rule.onNodeWithTag(TagListaArtistDetail).performScrollToNode(hasTestTag(TagBotonAgregarAlbum))
        rule.onNodeWithTag(TagBotonAgregarAlbum).performClick()
        esperarA(TagVentanaAsociar)
        esperarA(TagListaAsociar)
    }

    private fun search(texto: String) {
        rule.onNodeWithTag(TagBusquedaAsociar).performTextReplacement(texto)
        Espresso.closeSoftKeyboard()
    }

    private fun filaDe(id: Int) = rule.onNodeWithTag(tagAlbumAsociable(id))

    private fun select(id: Int) {
        rule.onNodeWithTag(TagListaAsociar).performScrollToNode(hasTestTag(tagAlbumAsociable(id)))
        filaDe(id).performClick()
    }

    private fun associate() = rule.onNodeWithTag(TagBotonAsociar)

    /** The album [id] appears in the artist detail's album list. */
    private fun verAlbumEnElDetalle(id: Int) {
        rule.waitUntil(15_000) {
            runCatching {
                rule.onNodeWithTag(TagListaArtistDetail).performScrollToNode(hasTestTag(tagAlbumArtista(id)))
            }.isSuccess
        }
        rule.onNodeWithTag(tagAlbumArtista(id)).assertIsDisplayed()
    }

    /**
     * Opens the [artista]'s window, checks the title, "Asociar" disabled, and a real album already
     * associated (if it has one), searches for and picks the test [album], associates it, and sees it in
     * the detail.
     */
    private fun associateDesdeElDetalle(artista: Artist, album: AlbumDePrueba, evidencia: String) {
        val yaAsociado = albumesEnElBackend(artista).firstOrNull()

        ActivityScenario.launch(MainActivity::class.java).use {
            entrarComoColeccionista(primerColeccionistaReal())
            abrirDetalle(artista, evidencia, "01_detalle_con_agregar_album")
            rule.onNodeWithTag(TagBotonAgregarAlbum).assertIsDisplayed()

            abrirVentana()
            rule.onNodeWithTag(TagTituloAsociar).assertTextEquals("Asociar álbum a ${artista.name}")
            associate().assertIsNotEnabled()
            if (yaAsociado != null) {
                rule.onNodeWithTag(TagListaAsociar).performScrollToNode(hasTestTag(tagAlbumAsociable(yaAsociado)))
                rule.onNodeWithTag(tagYaAsociado(yaAsociado), useUnmergedTree = true).assertIsDisplayed()
                filaDe(yaAsociado).assertIsNotEnabled()
            }
            EvidenciaE2E.capturar(rule, evidencia, "02_ventana_asociar")

            search(album.nombre)
            esperarA(tagAlbumAsociable(album.id))
            if (yaAsociado != null) rule.onAllNodesWithTag(tagAlbumAsociable(yaAsociado)).assertCountEquals(0)
            select(album.id)
            filaDe(album.id).assertIsSelected()
            associate().assertIsEnabled()
            EvidenciaE2E.capturar(rule, evidencia, "03_album_seleccionado")

            associate().performClick()
            esperarQueSeCierreLaVentana()
            esperarTexto("Álbum asociado", milis = 10_000)
            EvidenciaE2E.capturar(rule, evidencia, "04_album_asociado")
            verAlbumEnElDetalle(album.id)
            EvidenciaE2E.capturar(rule, evidencia, "05_detalle_con_el_album")
        }
    }

    // ---------- Scenarios against the real backend ----------

    /**
     * FN15-ESC01: a search with no matches warns about it and clears the selection; "Cancelar" closes
     * without associating and the window reopens without selection or search.
     */
    @Test
    fun cancelarCierraSinAsociarYLaBusquedaSinResultadosQuitaLaSeleccion() {
        val evidencia = "FN15_ESC01"
        val musico = primerArtista(ArtistType.MUSICO)
        val album = albumesDePrueba.create("E2E FN15")
        val antes = albumesEnElBackend(musico)

        ActivityScenario.launch(MainActivity::class.java).use {
            entrarComoColeccionista(primerColeccionistaReal())
            abrirDetalle(musico, evidencia, "01_detalle_con_agregar_album")
            abrirVentana()

            search(album.nombre)
            esperarA(tagAlbumAsociable(album.id))
            select(album.id)
            associate().assertIsEnabled()
            EvidenciaE2E.capturar(rule, evidencia, "02_album_seleccionado")

            search("zzzz sin coincidencias")
            esperarA(TagSinResultadosAsociar)
            rule.onNodeWithText("No encontramos álbumes con esa búsqueda").assertIsDisplayed()
            associate().assertIsNotEnabled()
            EvidenciaE2E.capturar(rule, evidencia, "03_busqueda_sin_resultados")

            search(album.nombre)
            esperarA(tagAlbumAsociable(album.id))
            filaDe(album.id).assertIsNotSelected()
            select(album.id)
            rule.onNodeWithTag(TagCancelarAsociar).performClick()
            esperarQueSeCierreLaVentana()
            rule.onAllNodesWithText("Álbum asociado").assertCountEquals(0)
            rule.onAllNodesWithTag(tagAlbumArtista(album.id)).assertCountEquals(0)
            EvidenciaE2E.capturar(rule, evidencia, "04_cancelado_sin_cambios")

            abrirVentana()
            rule.onNodeWithTag(TagBusquedaAsociar).assert(hasText(album.nombre).not())
            associate().assertIsNotEnabled()
            EvidenciaE2E.capturar(rule, evidencia, "05_ventana_otra_vez_vacia")
        }
        assertEquals(0, artistas.envios)
        assertEquals(antes, albumesEnElBackend(musico))
    }

    /** FN15-ESC02: the Collector adds an album to a band, which keeps the albums it already had. */
    @Test
    fun elColeccionistaAgregaUnAlbumAUnaBanda() {
        val banda = primerArtista(ArtistType.BANDA)
        val album = albumesDePrueba.create("E2E FN15")
        val antes = albumesEnElBackend(banda)

        associateDesdeElDetalle(banda, album, "FN15_ESC02")

        assertEquals(1, artistas.envios)
        assertEquals(antes + album.id, albumesEnElBackend(banda))
    }

    // ---------- Scenario with MockWebServer ----------

    private val ruben = Artist(100, "Rubén Blades Bellido de Luna", null, ArtistType.MUSICO)

    /** State of the simulated server: the first association fails and the following ones are saved. */
    private class Simulado {
        @Volatile var envios = 0
        @Volatile var asociado = false
    }

    /** Collector 100, Rubén Blades (musician, no albums) and album 101 "A Night at the Opera". */
    private fun iniciarServidor(simulado: Simulado) {
        val opera = """{"id": 101, "name": "A Night at the Opera", "cover": null,
            "releaseDate": "1975-11-21T00:00:00.000Z", "description": "Rock", "genre": "Rock",
            "recordLabel": "EMI", "performers": []}"""
        fun detalleRuben() = """
            {"id": 100, "name": "${ruben.name}", "image": null,
             "description": "Cantante, compositor y actor panameño.",
             "birthDate": "1948-07-16T00:00:00.000Z",
             "albums": [${if (simulado.asociado) opera else ""}], "performerPrizes": []}
        """.trimIndent()
        val nuevo = MockWebServer().apply {
            dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse = when {
                    request.path == "/collectors" -> MockResponse().setBody(
                        """[{"id": 100, "name": "Manolo Bellon", "email": "manollo@caracol.com.co"}]"""
                    )
                    request.path == "/collectors/100/performers" -> MockResponse().setBody("[]")
                    request.path == "/musicians" -> MockResponse().setBody("""[{"id": 100, "name": "${ruben.name}", "image": null}]""")
                    request.path == "/bands" -> MockResponse().setBody("[]")
                    request.path == "/performerprizes" -> MockResponse().setBody("[]")
                    request.path == "/albums" -> MockResponse().setBody("[$opera]")
                    request.path == "/musicians/100/albums/101" && request.method == "POST" -> {
                        simulado.envios++
                        if (simulado.envios == 1) {
                            // The first association takes a while (to see the submission in progress) and ends in a server error.
                            MockResponse().setResponseCode(500).setHeadersDelay(3, TimeUnit.SECONDS)
                        } else {
                            simulado.asociado = true
                            MockResponse().setBody("{}")
                        }
                    }
                    request.path == "/musicians/100" -> MockResponse().setBody(detalleRuben())
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
     * FN15-ESC03: while associating, the button stays disabled and a second tap sends nothing; if the
     * association fails, it is reported with the window open and the chosen album, and retrying
     * associates it.
     */
    @Test
    fun siFallaLaAsociacionSeConservaLaSeleccionYReintentarLaGuarda() {
        val evidencia = "FN15_ESC03"
        val simulado = Simulado()
        iniciarServidor(simulado)

        ActivityScenario.launch(MainActivity::class.java).use {
            entrarComoColeccionista(100)
            abrirDetalle(ruben, evidencia, "01_detalle_sin_albumes")
            abrirVentana()
            select(101)
            EvidenciaE2E.capturar(rule, evidencia, "02_album_seleccionado")

            associate().performClick()
            esperarA(TagAsociandoAsociar, milis = 2_000)
            associate().assertIsNotEnabled()
            EvidenciaE2E.capturar(rule, evidencia, "03_asociando")
            associate().performClick()

            esperarA(TagErrorAsociar)
            rule.onNodeWithText("No se pudo asociar el álbum, intenta de nuevo").assertIsDisplayed()
            assertEquals("El toque durante el envío no debe enviar otra asociación", 1, simulado.envios)
            rule.onAllNodesWithText("Álbum asociado").assertCountEquals(0)
            filaDe(101).assertIsSelected()
            associate().assertIsEnabled()
            EvidenciaE2E.capturar(rule, evidencia, "04_error_al_asociar")

            associate().performClick()
            esperarQueSeCierreLaVentana()
            esperarTexto("Álbum asociado", milis = 10_000)
            verAlbumEnElDetalle(101)
            EvidenciaE2E.capturar(rule, evidencia, "05_asociado_al_reintentar")
        }
        assertEquals(2, simulado.envios)
        assertTrue(simulado.asociado)
    }
}
