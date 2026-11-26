package com.example.vinilos

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasContentDescription
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
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import com.example.vinilos.data.model.Artist
import com.example.vinilos.data.model.ArtistType
import com.example.vinilos.data.model.ArtistDetail
import com.example.vinilos.data.model.formatearReleaseDate
import com.example.vinilos.data.remote.ServiceAdapter
import com.example.vinilos.data.repository.ArtistRepository
import com.example.vinilos.data.repository.ArtistRepositoryImpl
import com.example.vinilos.e2e.EvidenciaE2E
import com.example.vinilos.ui.artistas.TagArtistas
import com.example.vinilos.ui.artistas.TagContadorAlbumes
import com.example.vinilos.ui.artistas.TagContadorArtistas
import com.example.vinilos.ui.artistas.TagContadorIntegrantes
import com.example.vinilos.ui.artistas.TagContadorPrizes
import com.example.vinilos.ui.artistas.TagDescripcionArtistDetail
import com.example.vinilos.ui.artistas.TagArtistDetail
import com.example.vinilos.ui.artistas.TagErrorArtistas
import com.example.vinilos.ui.artistas.TagFechaArtistDetail
import com.example.vinilos.ui.artistas.TagListaArtistas
import com.example.vinilos.ui.artistas.TagListaArtistDetail
import com.example.vinilos.ui.artistas.TagNombreArtistDetail
import com.example.vinilos.ui.artistas.TagReintentarArtistas
import com.example.vinilos.ui.artistas.TagTipoArtistDetail
import com.example.vinilos.ui.artistas.tagAlbumArtista
import com.example.vinilos.ui.artistas.tagArtista
import com.example.vinilos.ui.artistas.tagIntegrante
import com.example.vinilos.ui.ingreso.TagEntrarVisitante
import com.example.vinilos.ui.inicio.TagInicio
import com.example.vinilos.ui.inicio.TagVerArtistas
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * E2E for HU04 (artist detail) as a Visitor, starting from the artist list (HU03).
 *
 * FN04-ESC01 through ESC04 and ESC07 run against the real backend and do not assume a fixed
 * dataset: before opening the app they query the real list and details and pick the artist that
 * meets the scenario's condition; if there is none, the scenario is skipped. This way they pass
 * against both the local backend (Docker) and the one deployed by the tutors. On the emulator the
 * app reaches the local backend via `10.0.2.2`; on a phone, with `adb reverse tcp:3000 tcp:3000`
 * and `-PbaseUrl=http://localhost:3000/`.
 *
 * FN04-ESC05 (loading error) and ESC06 (band with members, which doesn't exist on either backend)
 * use `MockWebServer`, like [RecuperacionTest].
 *
 * Each scenario saves a screenshot per step, `informe.md`, and a PDF in
 * `.../files/e2e-evidencia/FN04_ESCxx/` (on the device; the PDFs are versioned by hand in
 * `docs/pruebas/sprint-2/HU04/e2e/`).
 */
class ArtistDetailE2ETest {

    @get:Rule
    val rule = createEmptyComposeRule()

    /** The app's real repository, which also counts how many times each detail is queried. */
    private class ArtistasReales(
        baseUrl: String = BuildConfig.BASE_URL,
        private val real: ArtistRepository = ArtistRepositoryImpl(ServiceAdapter(baseUrl))
    ) : ArtistRepository by real {
        val consultasDetalle = mutableListOf<Pair<ArtistType, Int>>()

        override suspend fun getDetail(tipo: ArtistType, id: Int): Result<ArtistDetail> {
            synchronized(consultasDetalle) { consultasDetalle += tipo to id }
            return real.getDetail(tipo, id)
        }

        fun consultasDe(artista: Artist) =
            synchronized(consultasDetalle) { consultasDetalle.count { it == artista.type to artista.id } }
    }

    private val artistas = ArtistasReales()

    private var servidor: MockWebServer? = null

    @Before
    fun usarElBackendReal() {
        ApplicationProvider.getApplicationContext<VinilosApplication>().container =
            AppContainer(artistasRepositorio = artistas)
    }

    @After
    fun detenerServidor() {
        servidor?.shutdown()
    }

    // ---------- Real data ----------

    /** An artist from the real list along with its real detail. */
    private data class Candidato(val artista: Artist, val detalle: ArtistDetail)

    /** Queries, with a separate repository (not counted), the list and the detail of each real artist. */
    private fun candidatosReales(): List<Candidato> = runBlocking {
        val consulta = ArtistRepositoryImpl(ServiceAdapter(BuildConfig.BASE_URL))
        consulta.getArtists().getOrThrow().mapNotNull { artista ->
            consulta.getDetail(artista.type, artista.id).getOrNull()?.let { Candidato(artista, it) }
        }
    }

    /** The first one that meets [condicion], or the scenario is skipped explaining what was missing. */
    private fun elegir(motivo: String, condicion: (Candidato) -> Boolean): Candidato {
        val elegido = candidatosReales().firstOrNull(condicion)
        Assume.assumeTrue("El backend conectado no tiene $motivo", elegido != null)
        return requireNotNull(elegido)
    }

    /** The one with prizes and, among those, the most albums: shows the largest number of sections with data. */
    private fun masCompleto(tipo: ArtistType, motivo: String): Candidato {
        val elegido = candidatosReales()
            .filter { it.artista.type == tipo }
            .maxWithOrNull(compareBy({ it.detalle.premios.size }, { it.detalle.albumes.size }))
        Assume.assumeTrue("El backend conectado no tiene $motivo", elegido != null)
        return requireNotNull(elegido)
    }

    // ---------- Navigation ----------

    private fun esperarA(tag: String, milis: Long = 15_000) =
        rule.waitUntil(milis) { rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }

    private fun esperarTexto(texto: String, milis: Long = 15_000) =
        rule.waitUntil(milis) { rule.onAllNodesWithText(texto).fetchSemanticsNodes().isNotEmpty() }

    private fun esperarQueDesaparezca(tag: String, milis: Long = 5_000) =
        rule.waitUntil(milis) { rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isEmpty() }

    /** Visitor -> home -> "Ver artistas" -> [artista]'s row -> detail loaded. */
    private fun abrirDetalle(artista: Artist, evidencia: String) {
        esperarA(TagEntrarVisitante)
        rule.onNodeWithTag(TagEntrarVisitante).performScrollTo().performClick()
        esperarA(TagInicio)
        EvidenciaE2E.capturar(rule, evidencia, "01_inicio_visitante")
        rule.onNodeWithTag(TagVerArtistas).performScrollTo().performClick()
        // The counter appears once the list has loaded; the row may be further down, not yet composed.
        esperarA(TagContadorArtistas)
        rule.onNodeWithTag(TagListaArtistas).performScrollToNode(hasTestTag(tagArtista(artista)))
        EvidenciaE2E.capturar(rule, evidencia, "02_listado_artistas")
        rule.onNodeWithTag(tagArtista(artista)).performClick()
        esperarA(TagArtistDetail)
        esperarQueDesaparezca(TagArtistas)
    }

    private fun esperarDetalle(nombre: String) {
        esperarA(TagNombreArtistDetail)
        rule.onNodeWithTag(TagNombreArtistDetail).assertTextEquals(nombre)
    }

    /** Scrolls the detail to the node (on small screens it may not be composed yet). */
    private fun desplazarA(matcher: SemanticsMatcher) {
        rule.onNodeWithTag(TagListaArtistDetail).performScrollToNode(matcher)
    }

    private fun verTexto(texto: String) {
        desplazarA(hasText(texto))
        rule.onAllNodesWithText(texto)[0].assertIsDisplayed()
    }

    private fun contador(cantidad: Int, singular: String, plural: String) =
        if (cantidad == 1) "1 $singular" else "$cantidad $plural"

    private fun etiquetaDe(tipo: ArtistType) = if (tipo == ArtistType.BANDA) "Banda" else "Músico"

    // ---------- Detail verifications ----------

    /** Image (real or default), name, label, date with its label, and description. */
    private fun verificarTarjeta(detalle: ArtistDetail) {
        esperarDetalle(detalle.nombre)
        rule.onNodeWithTag(TagTipoArtistDetail).assertTextEquals(etiquetaDe(detalle.tipo))
        rule.onNode(hasContentDescription("Imagen de ${detalle.nombre}")).assertExists()
        detalle.fecha?.let { fecha ->
            val rotulo = if (detalle.tipo == ArtistType.BANDA) "FECHA DE CREACIÓN" else "FECHA DE NACIMIENTO"
            rule.onNodeWithText(rotulo).assertIsDisplayed()
            rule.onNodeWithTag(TagFechaArtistDetail).assertTextEquals(formatearReleaseDate(fecha))
        }
        detalle.descripcion?.let { rule.onNodeWithTag(TagDescripcionArtistDetail).assertTextEquals(it) }
    }

    private fun verificarIntegrantes(detalle: ArtistDetail) {
        if (detalle.integrantes.isEmpty()) {
            verTexto("Sin integrantes")
            return
        }
        desplazarA(hasTestTag(TagContadorIntegrantes))
        rule.onNodeWithTag(TagContadorIntegrantes)
            .assertTextEquals(contador(detalle.integrantes.size, "integrante", "integrantes"))
        detalle.integrantes.forEach { integrante ->
            desplazarA(hasTestTag(tagIntegrante(integrante.id)))
            rule.onNode(
                hasTestTag(tagIntegrante(integrante.id)) and hasAnyDescendant(hasText(integrante.nombre)),
                useUnmergedTree = true
            ).assertExists()
            rule.onNode(hasContentDescription("Imagen de ${integrante.nombre}"), useUnmergedTree = true).assertExists()
        }
    }

    /** Albums with a cover (or fallback), name, and year, in the order used by the app. */
    private fun verificarAlbumes(detalle: ArtistDetail) {
        if (detalle.albumes.isEmpty()) {
            verTexto("Sin álbumes")
            return
        }
        desplazarA(hasTestTag(TagContadorAlbumes))
        rule.onNodeWithTag(TagContadorAlbumes)
            .assertTextEquals(contador(detalle.albumes.size, "álbum", "álbumes"))
        detalle.albumes.forEach { album ->
            desplazarA(hasTestTag(tagAlbumArtista(album.id)))
            // The row is clickable: it merges the name, year, and description of the cover (or the fallback).
            rule.onNode(hasTestTag(tagAlbumArtista(album.id)) and hasText(album.nombre)).assertIsDisplayed()
            album.anio?.let { rule.onNode(hasTestTag(tagAlbumArtista(album.id)) and hasText(it)).assertExists() }
        }
    }

    private fun verificarPrizes(detalle: ArtistDetail) {
        if (detalle.premios.isEmpty()) {
            verTexto("Sin premios")
            return
        }
        desplazarA(hasTestTag(TagContadorPrizes))
        rule.onNodeWithTag(TagContadorPrizes)
            .assertTextEquals(contador(detalle.premios.size, "premio", "premios"))
        detalle.premios.forEach { premio ->
            verTexto(premio.nombre)
            verTexto(premio.organizacion)
            premio.fecha?.let { verTexto(formatearReleaseDate(it)) }
        }
    }

    // ---------- Scenarios against the real backend ----------

    /** FN04-ESC01: a musician's detail shows their data, birth date, albums, and prizes. */
    @Test
    fun elDetalleDeUnMusicoMuestraSusDatosAlbumesYPrizes() {
        val evidencia = "FN04_ESC01"
        val (musico, detalle) = masCompleto(ArtistType.MUSICO, "músicos")

        ActivityScenario.launch(MainActivity::class.java).use {
            abrirDetalle(musico, evidencia)
            verificarTarjeta(detalle)
            EvidenciaE2E.capturar(rule, evidencia, "03_tarjeta_musico")

            verificarAlbumes(detalle)
            EvidenciaE2E.capturar(rule, evidencia, "04_albumes")

            verificarPrizes(detalle)
            EvidenciaE2E.capturar(rule, evidencia, "05_premios")
            rule.onAllNodesWithText("Integrantes").assertCountEquals(0)
        }
    }

    /** FN04-ESC02: a band's detail shows its creation date, members, albums, and prizes. */
    @Test
    fun elDetalleDeUnaBandaMuestraSuFechaDeCreacionIntegrantesAlbumesYPrizes() {
        val evidencia = "FN04_ESC02"
        val (banda, detalle) = masCompleto(ArtistType.BANDA, "bandas")

        ActivityScenario.launch(MainActivity::class.java).use {
            abrirDetalle(banda, evidencia)
            verificarTarjeta(detalle)
            EvidenciaE2E.capturar(rule, evidencia, "03_tarjeta_banda")

            verificarIntegrantes(detalle)
            EvidenciaE2E.capturar(rule, evidencia, "04_integrantes")

            verificarAlbumes(detalle)
            EvidenciaE2E.capturar(rule, evidencia, "05_albumes")

            verificarPrizes(detalle)
            EvidenciaE2E.capturar(rule, evidencia, "06_premios")
        }
    }

    /** FN04-ESC03: tapping an album opens its detail (HU02) and "Atrás" returns to the artist without repeating the query. */
    @Test
    fun tocarUnAlbumAbreSuDetalleYAtrasVuelveAlArtistaSinRepetirLaConsulta() {
        val evidencia = "FN04_ESC03"
        val (artista, detalle) = elegir("artistas con álbumes") { it.detalle.albumes.isNotEmpty() }
        val album = detalle.albumes.first()

        ActivityScenario.launch(MainActivity::class.java).use {
            abrirDetalle(artista, evidencia)
            esperarDetalle(detalle.nombre)
            desplazarA(hasTestTag(tagAlbumArtista(album.id)))
            EvidenciaE2E.capturar(rule, evidencia, "03_album_en_el_detalle")

            rule.onNodeWithTag(tagAlbumArtista(album.id)).performClick()
            esperarQueDesaparezca(TagArtistDetail)
            esperarTexto(album.nombre)
            rule.onAllNodesWithText(album.nombre)[0].assertIsDisplayed()
            EvidenciaE2E.capturar(rule, evidencia, "04_detalle_del_album")

            Espresso.pressBack()
            esperarDetalle(detalle.nombre)
            assertEquals(1, artistas.consultasDe(artista))
            EvidenciaE2E.capturar(rule, evidencia, "05_regreso_al_artista")
        }
    }

    /** FN04-ESC04: an artist with no albums or prizes shows "Sin álbumes" and "Sin premios". */
    @Test
    fun unArtistaSinAlbumesNiPrizesMuestraLosMensajesDeEmpty() {
        val evidencia = "FN04_ESC04"
        val (artista, detalle) = elegir("artistas sin álbumes ni premios") {
            it.detalle.albumes.isEmpty() && it.detalle.premios.isEmpty()
        }

        ActivityScenario.launch(MainActivity::class.java).use {
            abrirDetalle(artista, evidencia)
            verificarTarjeta(detalle)
            verTexto("Sin álbumes")
            verTexto("Sin premios")
            rule.onAllNodesWithTag(TagContadorAlbumes).assertCountEquals(0)
            rule.onAllNodesWithTag(TagContadorPrizes).assertCountEquals(0)
            EvidenciaE2E.capturar(rule, evidencia, "03_sin_albumes_ni_premios")
        }
    }

    /** FN04-ESC07: when rotating the screen, the detail is preserved without querying the backend again. */
    @Test
    fun alGirarLaPantallaElDetalleSeConservaSinVolverAConsultar() {
        val evidencia = "FN04_ESC07"
        val (artista, detalle) = masCompleto(ArtistType.MUSICO, "músicos")

        ActivityScenario.launch(MainActivity::class.java).use { escenario ->
            abrirDetalle(artista, evidencia)
            verificarTarjeta(detalle)
            EvidenciaE2E.capturar(rule, evidencia, "03_antes_de_girar")

            // Actually rotate (not just recreate the Activity) so the evidence is shown in landscape.
            escenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
            rule.waitUntil(5_000) {
                var horizontal = false
                escenario.onActivity {
                    horizontal = it.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                }
                horizontal
            }
            rule.waitForIdle()

            esperarDetalle(detalle.nombre)
            rule.onNodeWithTag(TagTipoArtistDetail).assertTextEquals(etiquetaDe(detalle.tipo))
            assertEquals(1, artistas.consultasDe(artista))
            EvidenciaE2E.capturar(rule, evidencia, "04_despues_de_girar")
        }
    }

    // ---------- Scenarios with MockWebServer ----------

    /**
     * Local server with a list containing a single artist and their detail. [caido] makes the detail
     * drop the connection, like a server that is down; the list always responds so the detail can be reached.
     */
    private fun iniciarServidor(
        artista: Artist,
        detalleJson: String,
        caido: () -> Boolean = { false }
    ) {
        val fila = """[{"id": ${artista.id}, "name": "${artista.name}", "image": null}]"""
        val rutaDetalle = if (artista.type == ArtistType.BANDA) "/bands/${artista.id}" else "/musicians/${artista.id}"
        val nuevo = MockWebServer().apply {
            dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse = when (request.path) {
                    "/musicians" -> MockResponse().setBody(if (artista.type == ArtistType.MUSICO) fila else "[]")
                    "/bands" -> MockResponse().setBody(if (artista.type == ArtistType.BANDA) fila else "[]")
                    rutaDetalle ->
                        if (caido()) MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START)
                        else MockResponse().setBody(detalleJson)
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

    /** FN04-ESC05: if loading fails, "No se pudo cargar el artista" shows up and "Reintentar" recovers it. */
    @Test
    fun siFallaLaCargaSeVeElErrorYReintentarRecuperaElDetalle() {
        val evidencia = "FN04_ESC05"
        val ruben = Artist(100, "Rubén Blades Bellido de Luna", null, ArtistType.MUSICO)
        var caido = true
        iniciarServidor(
            ruben,
            """
            {"id": 100, "name": "Rubén Blades Bellido de Luna", "image": null,
             "description": "Cantante, compositor y actor panameño.",
             "birthDate": "1948-07-16T00:00:00.000Z",
             "albums": [], "performerPrizes": []}
            """.trimIndent(),
            caido = { caido }
        )

        ActivityScenario.launch(MainActivity::class.java).use {
            abrirDetalle(ruben, evidencia)
            esperarA(TagErrorArtistas)
            rule.onNodeWithText("No se pudo cargar el artista").assertIsDisplayed()
            rule.onNodeWithTag(TagReintentarArtistas).assertIsDisplayed()
            EvidenciaE2E.capturar(rule, evidencia, "03_error_de_carga")

            // The backend "comes back" and the person taps Reintentar.
            caido = false
            rule.onNodeWithTag(TagReintentarArtistas).performClick()

            esperarDetalle("Rubén Blades Bellido de Luna")
            rule.onNodeWithTag(TagFechaArtistDetail).assertTextEquals("16 de julio de 1948")
            rule.onAllNodesWithTag(TagErrorArtistas).assertCountEquals(0)
            EvidenciaE2E.capturar(rule, evidencia, "04_recuperado")
        }
    }

    /** FN04-ESC06: a band with members shows them with an image (or the default one) and name. */
    @Test
    fun unaBandaConIntegrantesLosMuestraConImagenYNombre() {
        val evidencia = "FN04_ESC06"
        val queen = Artist(101, "Queen", null, ArtistType.BANDA)
        iniciarServidor(
            queen,
            """
            {"id": 101, "name": "Queen", "image": null,
             "description": "Banda británica de rock formada en Londres en 1970.",
             "creationDate": "1970-01-01T00:00:00.000Z",
             "musicians": [
               {"id": 1, "name": "Freddie Mercury", "image": null},
               {"id": 2, "name": "Brian May", "image": "http://localhost:1/no-existe.png"}
             ],
             "albums": [], "performerPrizes": []}
            """.trimIndent()
        )

        ActivityScenario.launch(MainActivity::class.java).use {
            abrirDetalle(queen, evidencia)
            esperarDetalle("Queen")
            rule.onNodeWithTag(TagTipoArtistDetail).assertTextEquals("Banda")
            rule.onNodeWithText("FECHA DE CREACIÓN").assertIsDisplayed()
            rule.onNodeWithTag(TagFechaArtistDetail).assertTextEquals("1 de enero de 1970")

            desplazarA(hasTestTag(TagContadorIntegrantes))
            rule.onNodeWithTag(TagContadorIntegrantes).assertTextEquals("2 integrantes")
            listOf(1 to "Freddie Mercury", 2 to "Brian May").forEach { (id, nombre) ->
                desplazarA(hasTestTag(tagIntegrante(id)))
                rule.onNode(hasTestTag(tagIntegrante(id)) and hasAnyDescendant(hasText(nombre)), useUnmergedTree = true)
                    .assertExists()
                // Without an image, or with a URL that fails to load, the default image is shown with its description.
                rule.onNode(hasContentDescription("Imagen de $nombre"), useUnmergedTree = true).assertExists()
            }
            EvidenciaE2E.capturar(rule, evidencia, "03_integrantes")
        }
    }
}
