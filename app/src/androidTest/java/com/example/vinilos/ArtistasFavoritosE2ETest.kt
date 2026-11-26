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
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.example.vinilos.data.model.Artist
import com.example.vinilos.data.model.ArtistType
import com.example.vinilos.data.remote.ServiceAdapter
import com.example.vinilos.data.repository.ArtistRepositoryImpl
import com.example.vinilos.data.repository.FavoriteRepository
import com.example.vinilos.data.repository.FavoriteRepositoryImpl
import com.example.vinilos.e2e.EvidenciaE2E
import com.example.vinilos.ui.artistas.TagBotonFavorito
import com.example.vinilos.ui.artistas.TagConfirmacionFavorito
import com.example.vinilos.ui.artistas.TagContadorArtistas
import com.example.vinilos.ui.artistas.TagArtistDetail
import com.example.vinilos.ui.artistas.TagErrorFavorito
import com.example.vinilos.ui.artistas.TagListaArtistas
import com.example.vinilos.ui.artistas.TagNombreArtistDetail
import com.example.vinilos.ui.artistas.TagNotAvailableFavorito
import com.example.vinilos.ui.artistas.TagReintentarFavorito
import com.example.vinilos.ui.artistas.tagArtista
import com.example.vinilos.ui.ingreso.TagAtras
import com.example.vinilos.ui.ingreso.TagEntrarColeccionista
import com.example.vinilos.ui.ingreso.TagEntrarVisitante
import com.example.vinilos.ui.ingreso.TagListaColeccionistas
import com.example.vinilos.ui.ingreso.tagColeccionista
import com.example.vinilos.ui.inicio.TagInicio
import com.example.vinilos.ui.inicio.TagVerArtistas
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.mockwebserver.SocketPolicy
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * E2E for HU10 (add favorite artists), with the scenarios from T10-5.
 *
 * FN10-ESC01 through ESC05 run against the real backend. Since they add favorites, they are run
 * against local BackVynils (`-PbaseUrl=http://10.0.2.2:3000/` on the emulator; on a phone,
 * `http://localhost:3000/` with `adb reverse tcp:3000 tcp:3000`). Each scenario that adds favorites
 * creates its own collector with `POST /collectors` and deletes it in [borrarColeccionistasDePrueba]
 * with `DELETE /collectors/{id}`, which also deletes its favorites, even if the test fails.
 * `DELETE /collectors/{id}/bands/{id}` and `.../musicians/{id}` are not used: in BackVynils they
 * respond 204 without removing the favorite. Artists are chosen from the real data; if there is no
 * band or musician, the scenario is skipped.
 *
 * FN10-ESC06 through ESC08 (errors that the real backend does not trigger on demand) use
 * `MockWebServer`, like FN03-ESC05 and FN07-ESC07.
 *
 * Each scenario saves a screenshot per step, `informe.md`, and a PDF in
 * `.../files/e2e-evidencia/FN10_ESCxx/` (on the device; the PDFs are versioned by hand in
 * `docs/pruebas/sprint-2/HU10/e2e/`).
 */
class ArtistasFavoritosE2ETest {

    @get:Rule
    val rule = createEmptyComposeRule()

    /** The app's real repository, which also counts queries and submissions (design.md, D1). */
    private class FavoritosReales(
        private val real: FavoriteRepository = FavoriteRepositoryImpl(ServiceAdapter(BuildConfig.BASE_URL))
    ) : FavoriteRepository by real {
        @Volatile var consultas = 0
        @Volatile var envios = 0

        override suspend fun esFavorito(collectorId: Int, artistId: Int): Result<Boolean> {
            consultas++
            return real.esFavorito(collectorId, artistId)
        }

        override suspend fun addFavorite(collectorId: Int, artistId: Int, tipo: ArtistType): Result<Unit> {
            envios++
            return real.addFavorite(collectorId, artistId, tipo)
        }
    }

    private val favoritos = FavoritosReales()

    private val http = OkHttpClient()

    /** Collectors created by the test: deleted at the end, along with their favorites (D2). */
    private val coleccionistasDePrueba = mutableListOf<Int>()

    private var servidor: MockWebServer? = null

    @Before
    fun usarElBackendReal() {
        ApplicationProvider.getApplicationContext<VinilosApplication>().container =
            AppContainer(favoritosRepositorio = favoritos)
    }

    @After
    fun borrarColeccionistasDePrueba() {
        servidor?.shutdown()
        coleccionistasDePrueba.forEach { id ->
            http.newCall(Request.Builder().url("${BuildConfig.BASE_URL}collectors/$id").delete().build()).execute().close()
        }
    }

    // ---------- Real data (design.md, D3) ----------

    private fun artistasReales(): List<Artist> = runBlocking {
        ArtistRepositoryImpl(ServiceAdapter(BuildConfig.BASE_URL)).getArtists().getOrThrow()
    }

    /** The first band or the first musician from the backend; if there is none, the scenario is skipped. */
    private fun primerArtista(tipo: ArtistType): Artist {
        val artista = artistasReales().firstOrNull { it.type == tipo }
        Assume.assumeTrue("Sin datos en el backend: no hay ningún artista de tipo $tipo", artista != null)
        return artista!!
    }

    /** Creates a collector with no favorites via `POST /collectors` and registers it for deletion (D2). */
    private fun createColeccionista(): Int {
        val marca = System.currentTimeMillis()
        val cuerpo = JSONObject()
            .put("name", "E2E FN10 $marca")
            .put("telephone", "3000000000")
            .put("email", "e2e.fn10.$marca@example.com")
            .toString()
            .toRequestBody("application/json".toMediaType())
        val respuesta = http.newCall(Request.Builder().url("${BuildConfig.BASE_URL}collectors").post(cuerpo).build()).execute()
        val id = respuesta.use { JSONObject(requireNotNull(it.body).string()).getInt("id") }
        coleccionistasDePrueba += id
        return id
    }

    /** Adds the favorite directly in the backend, without going through the app (setup for ESC05). */
    private fun agregarEnElBackend(coleccionista: Int, artista: Artist) {
        val ruta = if (artista.type == ArtistType.BANDA) "bands" else "musicians"
        val vacio = "".toRequestBody(null)
        http.newCall(
            Request.Builder().url("${BuildConfig.BASE_URL}collectors/$coleccionista/$ruta/${artista.id}").post(vacio).build()
        ).execute().use { assertTrue("No se pudo preparar el favorito", it.isSuccessful) }
    }

    /** Ids of the collector's favorites according to `GET /collectors/{id}/performers`. */
    private fun favoritosEnElBackend(coleccionista: Int): Set<Int> {
        val respuesta = http.newCall(
            Request.Builder().url("${BuildConfig.BASE_URL}collectors/$coleccionista/performers").build()
        ).execute()
        val lista = respuesta.use { JSONArray(requireNotNull(it.body).string()) }
        return (0 until lista.length()).map { lista.getJSONObject(it).getInt("id") }.toSet()
    }

    // ---------- Navigation ----------

    private fun esperarA(tag: String, milis: Long = 15_000) =
        rule.waitUntil(milis) { rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }

    /** The favorite confirmation hides itself after 2.5s; it's given up to 10s. */
    private fun esperarQueSeOculteLaConfirmacion() =
        rule.waitUntil(10_000) { rule.onAllNodesWithTag(TagConfirmacionFavorito).fetchSemanticsNodes().isEmpty() }

    /** Waits for the favorite button to show [texto]. */
    private fun esperarBoton(texto: String, milis: Long = 15_000) = rule.waitUntil(milis) {
        rule.onAllNodesWithTag(TagBotonFavorito).fetchSemanticsNodes().isNotEmpty() &&
            rule.onAllNodes(hasTestTag(TagBotonFavorito) and hasText(texto)).fetchSemanticsNodes().isNotEmpty()
    }

    private fun entrarComoVisitante() {
        esperarA(TagEntrarVisitante)
        rule.onNodeWithTag(TagEntrarVisitante).performScrollTo().performClick()
        esperarA(TagInicio)
    }

    private fun entrarComoColeccionista(coleccionista: Int) {
        esperarA(TagEntrarColeccionista)
        rule.onNodeWithTag(TagEntrarColeccionista).performScrollTo().performClick()
        // The test's collector usually ends up at the bottom: it waits for the list to load and reaches it.
        rule.waitUntil(15_000) {
            runCatching {
                rule.onNodeWithTag(TagListaColeccionistas).performScrollToNode(hasTestTag(tagColeccionista(coleccionista)))
            }.isSuccess
        }
        rule.onNodeWithTag(tagColeccionista(coleccionista)).performClick()
        esperarA(TagInicio)
    }

    /** Home -> "Ver artistas" -> row for [artista], visible in the list. */
    private fun verListado(artista: Artist, evidencia: String, paso: String) {
        rule.onNodeWithTag(TagVerArtistas).performScrollTo().performClick()
        // The counter appears once the list has loaded; the row may be further down, not yet composed.
        esperarA(TagContadorArtistas)
        rule.onNodeWithTag(TagListaArtistas).performScrollToNode(hasTestTag(tagArtista(artista)))
        EvidenciaE2E.capturar(rule, evidencia, paso)
    }

    /** Home -> list -> [artista]'s detail loaded. */
    private fun abrirDetalle(artista: Artist, evidencia: String, pasoListado: String) {
        verListado(artista, evidencia, pasoListado)
        rule.onNodeWithTag(tagArtista(artista)).performClick()
        esperarA(TagArtistDetail)
        esperarA(TagNombreArtistDetail)
        rule.onNodeWithTag(TagNombreArtistDetail).assertTextEquals(artista.name)
    }

    /** Detail -> list -> home, using each screen's back button. */
    private fun volverAlInicio() {
        rule.onNodeWithTag(TagAtras).performClick()
        esperarA(TagContadorArtistas)
        rule.onNodeWithTag(TagAtras).performClick()
        esperarA(TagInicio)
    }

    private fun boton() = rule.onNodeWithTag(TagBotonFavorito).performScrollTo()

    /** Taps "Agregar Favorito" and checks that it stays "Favorito" with the confirmation, which hides itself. */
    private fun agregarYVerConfirmacion(artista: Artist, evidencia: String, pasoDespues: String) {
        esperarBoton("Agregar Favorito")
        boton().assertIsEnabled()
        EvidenciaE2E.capturar(rule, evidencia, "02_agregar_favorito")

        boton().performClick()
        esperarA(TagConfirmacionFavorito, milis = 10_000)
        rule.onNodeWithText("${artista.name} se agregó a tus favoritos").assertIsDisplayed()
        boton().assert(hasText("Favorito")).assertIsNotEnabled()
        EvidenciaE2E.capturar(rule, evidencia, pasoDespues)

        // The message is brief: it disappears on its own and the button stays as "Favorito".
        esperarQueSeOculteLaConfirmacion()
        boton().assert(hasText("Favorito")).assertIsNotEnabled()
    }

    // ---------- Scenarios against the real backend ----------

    /** FN10-ESC01: the Visitor does not see the favorite button in the detail of a band or a musician. */
    @Test
    fun elVisitanteNoVeElBotonDeFavorito() {
        val evidencia = "FN10_ESC01"
        val banda = primerArtista(ArtistType.BANDA)
        val musico = primerArtista(ArtistType.MUSICO)

        ActivityScenario.launch(MainActivity::class.java).use {
            entrarComoVisitante()
            abrirDetalle(banda, evidencia, "01_listado_visitante")
            rule.onAllNodesWithTag(TagBotonFavorito).assertCountEquals(0)
            EvidenciaE2E.capturar(rule, evidencia, "02_detalle_banda_sin_boton")

            volverAlInicio()
            abrirDetalle(musico, evidencia, "03_listado_visitante")
            rule.onAllNodesWithTag(TagBotonFavorito).assertCountEquals(0)
            EvidenciaE2E.capturar(rule, evidencia, "04_detalle_musico_sin_boton")
            assertEquals(0, favoritos.consultas)
        }
    }

    /** FN10-ESC02: the Collector adds a band; the button becomes "Favorito" and the backend saves it. */
    @Test
    fun elColeccionistaAgregaUnaBandaComoFavorita() {
        val evidencia = "FN10_ESC02"
        val banda = primerArtista(ArtistType.BANDA)
        val coleccionista = createColeccionista()

        ActivityScenario.launch(MainActivity::class.java).use {
            entrarComoColeccionista(coleccionista)
            abrirDetalle(banda, evidencia, "01_listado_coleccionista")
            agregarYVerConfirmacion(banda, evidencia, "03_favorito_con_confirmacion")
            EvidenciaE2E.capturar(rule, evidencia, "04_confirmacion_oculta")
        }
        assertEquals(1, favoritos.envios)
        assertTrue("El backend debe devolver la banda como favorita", banda.id in favoritosEnElBackend(coleccionista))
    }

    /** FN10-ESC03: the Collector adds a musician; the button becomes "Favorito" and the backend saves it. */
    @Test
    fun elColeccionistaAgregaUnMusicoComoFavorito() {
        val evidencia = "FN10_ESC03"
        val musico = primerArtista(ArtistType.MUSICO)
        val coleccionista = createColeccionista()

        ActivityScenario.launch(MainActivity::class.java).use {
            entrarComoColeccionista(coleccionista)
            abrirDetalle(musico, evidencia, "01_listado_coleccionista")
            agregarYVerConfirmacion(musico, evidencia, "03_favorito_con_confirmacion")
            EvidenciaE2E.capturar(rule, evidencia, "04_confirmacion_oculta")
        }
        assertEquals(1, favoritos.envios)
        assertTrue("El backend debe devolver el músico como favorito", musico.id in favoritosEnElBackend(coleccionista))
    }

    /** FN10-ESC04: after adding a favorite and reopening the app, the button still shows "Favorito". */
    @Test
    fun alVolverAAbrirLaAppElBotonSigueComoFavorito() {
        val evidencia = "FN10_ESC04"
        val artista = primerArtista(ArtistType.BANDA)
        val coleccionista = createColeccionista()

        ActivityScenario.launch(MainActivity::class.java).use {
            entrarComoColeccionista(coleccionista)
            abrirDetalle(artista, evidencia, "01_listado_coleccionista")
            agregarYVerConfirmacion(artista, evidencia, "03_favorito_agregado")
        }
        val consultasAntes = favoritos.consultas

        // The app is closed and reopened: you have to log in again with the same collector (D4).
        ActivityScenario.launch(MainActivity::class.java).use {
            entrarComoColeccionista(coleccionista)
            EvidenciaE2E.capturar(rule, evidencia, "04_app_abierta_de_nuevo")
            abrirDetalle(artista, evidencia, "05_listado_coleccionista")
            esperarBoton("Favorito")
            boton().assertIsNotEnabled()
            rule.onAllNodesWithTag(TagConfirmacionFavorito).assertCountEquals(0)
            EvidenciaE2E.capturar(rule, evidencia, "06_sigue_como_favorito")
        }
        assertTrue("Al volver a abrir el detalle se consulta de nuevo el backend", favoritos.consultas > consultasAntes)
        assertEquals(1, favoritos.envios)
    }

    /** FN10-ESC05: an artist that is already a favorite shows up as "Favorito" and disabled, without sending anything. */
    @Test
    fun unArtistaQueYaEsFavoritoApareceDesactivado() {
        val evidencia = "FN10_ESC05"
        val artista = primerArtista(ArtistType.MUSICO)
        val coleccionista = createColeccionista()
        agregarEnElBackend(coleccionista, artista)

        ActivityScenario.launch(MainActivity::class.java).use {
            entrarComoColeccionista(coleccionista)
            abrirDetalle(artista, evidencia, "01_listado_coleccionista")
            esperarBoton("Favorito")
            boton().assertIsNotEnabled()
            rule.onNodeWithText("Este artista ya está en tus favoritos.").assertIsDisplayed()
            EvidenciaE2E.capturar(rule, evidencia, "02_ya_es_favorito")
            boton().performClick()
            rule.waitForIdle()
        }
        assertEquals(0, favoritos.envios)
    }

    // ---------- Scenarios with MockWebServer (design.md, D5) ----------

    private val ruben = Artist(100, "Rubén Blades Bellido de Luna", null, ArtistType.MUSICO)
    private val queen = Artist(101, "Queen", null, ArtistType.BANDA)

    /** State of the simulated server, which each scenario changes halfway through the test. */
    private class Simulado {
        @Volatile var comprobacionCaida = false
        @Volatile var respuestaGuardado: () -> MockResponse = { MockResponse().setBody("""{"id": 100}""") }
        @Volatile var rubenExiste = true
        @Volatile var envios = 0
    }

    /** Collector 100 with no favorites, Rubén Blades (musician), and Queen (band). */
    private fun iniciarServidor(simulado: Simulado) {
        val musicos = { if (simulado.rubenExiste) """[{"id": 100, "name": "${ruben.name}", "image": null}]""" else "[]" }
        val detalleRuben = """
            {"id": 100, "name": "${ruben.name}", "image": null,
             "description": "Cantante, compositor y actor panameño.",
             "birthDate": "1948-07-16T00:00:00.000Z", "albums": [], "performerPrizes": []}
        """.trimIndent()
        val nuevo = MockWebServer().apply {
            dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse = when {
                    request.path == "/collectors" -> MockResponse().setBody(
                        """[{"id": 100, "name": "Manolo Bellon", "email": "manollo@caracol.com.co"}]"""
                    )
                    request.path == "/collectors/100/performers" ->
                        if (simulado.comprobacionCaida) MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START)
                        else MockResponse().setBody("[]")
                    request.path == "/collectors/100/musicians/100" && request.method == "POST" -> {
                        simulado.envios++
                        simulado.respuestaGuardado()
                    }
                    request.path == "/musicians" -> MockResponse().setBody(musicos())
                    request.path == "/bands" -> MockResponse().setBody("""[{"id": 101, "name": "Queen", "image": null}]""")
                    request.path == "/musicians/100" -> MockResponse().setBody(detalleRuben)
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

    /** FN10-ESC06: if the favorite cannot be checked, the button stays disabled and "Reintentar" recovers it. */
    @Test
    fun siNoSePuedeComprobarElBotonQuedaDesactivadoYReintentarLoRecupera() {
        val evidencia = "FN10_ESC06"
        val simulado = Simulado().apply { comprobacionCaida = true }
        iniciarServidor(simulado)

        ActivityScenario.launch(MainActivity::class.java).use {
            entrarComoColeccionista(100)
            abrirDetalle(ruben, evidencia, "01_listado_coleccionista")
            esperarA(TagReintentarFavorito)
            rule.onNodeWithText("No pudimos comprobar si es tu favorito").assertIsDisplayed()
            boton().assertIsNotEnabled()
            rule.onNodeWithTag(TagReintentarFavorito).performScrollTo().assertIsDisplayed()
            EvidenciaE2E.capturar(rule, evidencia, "02_no_se_pudo_comprobar")

            // The backend "comes back" and the person retries.
            simulado.comprobacionCaida = false
            rule.onNodeWithTag(TagReintentarFavorito).performClick()
            esperarBoton("Agregar Favorito")
            boton().assertIsEnabled()
            rule.onAllNodesWithTag(TagReintentarFavorito).assertCountEquals(0)
            EvidenciaE2E.capturar(rule, evidencia, "03_comprobado_al_reintentar")
        }
        assertEquals(0, simulado.envios)
    }

    /**
     * FN10-ESC07: while saving, "Guardando..." is shown and a second tap doesn't send anything; if saving
     * fails the error appears without confirmation, and retrying saves it.
     */
    @Test
    fun siFallaElGuardadoSeVeElErrorYReintentarLoGuarda() {
        val evidencia = "FN10_ESC07"
        // The first submission takes time (D6) and ends in a service error; the following ones succeed.
        val simulado = Simulado().apply {
            respuestaGuardado = {
                if (envios == 1) MockResponse().setResponseCode(500).setHeadersDelay(3, TimeUnit.SECONDS)
                else MockResponse().setBody("""{"id": 100}""")
            }
        }
        iniciarServidor(simulado)

        ActivityScenario.launch(MainActivity::class.java).use {
            entrarComoColeccionista(100)
            abrirDetalle(ruben, evidencia, "01_listado_coleccionista")
            esperarBoton("Agregar Favorito")
            EvidenciaE2E.capturar(rule, evidencia, "02_agregar_favorito")

            boton().performClick()
            esperarBoton("Guardando...", milis = 2_000)
            boton().assertIsNotEnabled()
            EvidenciaE2E.capturar(rule, evidencia, "03_guardando")
            boton().performClick()

            esperarA(TagErrorFavorito)
            rule.onNodeWithText("No se pudo guardar el favorito, intenta de nuevo").assertIsDisplayed()
            rule.onAllNodesWithTag(TagConfirmacionFavorito).assertCountEquals(0)
            assertEquals("El toque durante \"Guardando...\" no debe enviar otra petición", 1, simulado.envios)
            rule.onNodeWithText("Reintentar guardar").performScrollTo().assertIsEnabled()
            EvidenciaE2E.capturar(rule, evidencia, "04_error_al_guardar")

            rule.onNodeWithText("Reintentar guardar").performClick()
            esperarA(TagConfirmacionFavorito, milis = 10_000)
            rule.onNodeWithText("${ruben.name} se agregó a tus favoritos").assertIsDisplayed()
            boton().assert(hasText("Favorito")).assertIsNotEnabled()
            EvidenciaE2E.capturar(rule, evidencia, "05_guardado_al_reintentar")
        }
        assertEquals(2, simulado.envios)
    }

    /** FN10-ESC08: if the artist no longer exists, it warns and "Volver al listado" shows it without that artist. */
    @Test
    fun siElArtistaYaNoExisteSeAvisaYElListadoSeActualiza() {
        val evidencia = "FN10_ESC08"
        val simulado = Simulado()
        // When saving, the backend no longer has the artist: it responds 404 and stops listing it.
        simulado.respuestaGuardado = {
            simulado.rubenExiste = false
            MockResponse().setResponseCode(404)
        }
        iniciarServidor(simulado)

        ActivityScenario.launch(MainActivity::class.java).use {
            entrarComoColeccionista(100)
            abrirDetalle(ruben, evidencia, "01_listado_con_el_artista")
            esperarBoton("Agregar Favorito")
            boton().performClick()

            esperarA(TagNotAvailableFavorito)
            rule.onNodeWithText("El artista ya no está disponible").assertIsDisplayed()
            rule.onAllNodesWithTag(TagConfirmacionFavorito).assertCountEquals(0)
            rule.onNodeWithText("Volver al listado de artistas").performScrollTo().assertIsDisplayed()
            EvidenciaE2E.capturar(rule, evidencia, "02_artista_no_disponible")

            rule.onNodeWithText("Volver al listado de artistas").performClick()
            esperarA(TagContadorArtistas)
            rule.waitUntil(15_000) { rule.onAllNodesWithTag(tagArtista(ruben)).fetchSemanticsNodes().isEmpty() }
            rule.onAllNodesWithTag(tagArtista(ruben)).assertCountEquals(0)
            rule.onNodeWithTag(TagListaArtistas).performScrollToNode(hasTestTag(tagArtista(queen)))
            rule.onNodeWithTag(tagArtista(queen)).assertIsDisplayed()
            EvidenciaE2E.capturar(rule, evidencia, "03_listado_sin_el_artista")
        }
        assertEquals(1, simulado.envios)
    }
}
