package com.example.vinilos

import android.os.SystemClock
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
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
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import com.example.vinilos.data.model.AlbumSummary
import com.example.vinilos.data.remote.ServiceAdapter
import com.example.vinilos.data.repository.AlbumRepository
import com.example.vinilos.data.repository.AlbumRepositoryImpl
import com.example.vinilos.e2e.EvidenciaE2E
import com.example.vinilos.ui.catalog.TagActualizandoCatalogo
import com.example.vinilos.ui.catalog.TagContadorCatalogo
import com.example.vinilos.ui.catalog.TagCuadriculaCatalogo
import com.example.vinilos.ui.catalog.TagErrorCatalogo
import com.example.vinilos.ui.catalog.TagProgresoCatalogo
import com.example.vinilos.ui.catalog.TagReintentarCatalogo
import com.example.vinilos.ui.catalog.TagRolActivo
import com.example.vinilos.ui.catalog.tagAlbum
import com.example.vinilos.ui.catalog.tagsDeAlbumesRecorriendo
import com.example.vinilos.ui.entry.TagAtras
import com.example.vinilos.ui.entry.TagEntrarColeccionista
import com.example.vinilos.ui.entry.TagEntrarVisitante
import com.example.vinilos.ui.entry.tagColeccionista
import com.example.vinilos.ui.home.TagInicio
import com.example.vinilos.ui.home.TagVerAlbumes
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * E2E for HU01 (album catalog) against the real BackVynils backend.
 *
 * It does not assume a fixed dataset (like the 4 albums in `sql/data.sql`): it wraps the real
 * repository to capture the catalog that the backend actually returns and computes the assertions
 * (count, order, name/genre/year of each album) from that captured data. This way the test is
 * consistent with both the local backend (Docker) and the one deployed by the tutors, regardless
 * of how many albums each one has.
 *
 * FN01-ESC03 (backend down) still uses `MockWebServer` with data fabricated by the test, because
 * shutting down the real backend within the test is not reliable and that case does not depend on
 * the real dataset; it did not need to change.
 *
 * On a phone it requires `adb reverse tcp:3000 tcp:3000` and the app installed with
 * `-PbaseUrl=http://localhost:3000/`. Scenarios FN01-ESC01 through FN01-ESC05 save a screenshot
 * per step, a row in the report, and a PDF in their own folder,
 * `.../files/e2e-evidencia/FN01_ESCxx/` (device only, not in the repo).
 */
class CatalogE2ETest {

    @get:Rule
    val rule = createEmptyComposeRule()

    /** The app's real repository: counts the queries and stores the last catalog received. */
    private class AlbumesRealesContados(
        private val real: AlbumRepository = AlbumRepositoryImpl(ServiceAdapter(BuildConfig.BASE_URL))
    ) : AlbumRepository by real {
        @Volatile var consultasCatalogo = 0
        @Volatile var catalogo: List<AlbumSummary>? = null

        override suspend fun getAlbums(): Result<List<AlbumSummary>> {
            consultasCatalogo++
            return real.getAlbums().also { resultado -> resultado.onSuccess { catalogo = it } }
        }
    }

    private val albumes = AlbumesRealesContados()

    @Before
    fun usarElBackendReal() {
        ApplicationProvider.getApplicationContext<VinilosApplication>().container =
            AppContainer(albumesRepositorio = albumes)
    }

    private fun esperarA(tag: String, milis: Long = 15_000) =
        rule.waitUntil(milis) { rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }

    /** Gives Coil time to render the covers before the screenshot. */
    private fun capturar(evidencia: String, paso: String) {
        SystemClock.sleep(2_000)
        EvidenciaE2E.capturar(rule, evidencia, paso)
    }

    private fun esperarElCatalogoReal(): List<AlbumSummary> {
        rule.waitUntil(15_000) { albumes.catalogo != null }
        return requireNotNull(albumes.catalogo)
    }

    /** From the home screen (recorded in the evidence as [pasoInicio]) opens the real catalog. */
    private fun verAlbumes(evidencia: String, pasoInicio: String): List<AlbumSummary> {
        esperarA(TagInicio)
        EvidenciaE2E.capturar(rule, evidencia, pasoInicio)
        rule.onNodeWithTag(TagVerAlbumes).performScrollTo().performClick()
        val catalogo = esperarElCatalogoReal()
        esperarA(tagAlbum(catalogo.first().id))
        return catalogo
    }

    private fun verAlbum(id: Int) =
        rule.onNodeWithTag(TagCuadriculaCatalogo).performScrollToNode(hasTestTag(tagAlbum(id)))

    private fun textoContador(cantidad: Int) =
        if (cantidad == 1) "1 lanzamiento" else "$cantidad lanzamientos"

    /**
     * A bounded sample of the real catalog: the first [tamano] and the last one. The deployed
     * backend can have hundreds of albums, and scrolling through all of them in an E2E test (which
     * already does real waits and IO) would make it impractical; this sample is enough to verify
     * that the real data reaches the screen correctly without scrolling through the entire catalog.
     */
    private fun muestraDelCatalogo(catalogo: List<AlbumSummary>, tamano: Int = 5): List<AlbumSummary> {
        val primeros = catalogo.take(tamano)
        val ultimo = catalogo.lastOrNull()
        return if (ultimo != null && ultimo !in primeros) primeros + ultimo else primeros
    }

    /** Checks, over a sample of the real catalog, that it is displayed with the same data and in order. */
    private fun comprobarLosAlbumesDelCatalogoReal(catalogo: List<AlbumSummary>) {
        rule.onNodeWithTag(TagContadorCatalogo).assertTextEquals(textoContador(catalogo.size))
        val muestra = muestraDelCatalogo(catalogo)
        muestra.forEach { album ->
            verAlbum(album.id)
            val generoYAnio = if (album.anio != null) "${album.genero} · ${album.anio}" else album.genero
            var matcher: SemanticsMatcher = hasTestTag(tagAlbum(album.id))
            matcher = matcher and hasText(album.nombre)
            matcher = matcher and hasText(generoYAnio)
            matcher = matcher and hasContentDescription("Portada de ${album.nombre}")
            rule.onNode(matcher).assertIsDisplayed()
        }
        val esperados = muestra.map { tagAlbum(it.id) }
        assertEquals(esperados, rule.tagsDeAlbumesRecorriendo(esperados).filter { it in esperados })
    }

    /** FN01-ESC01: Visitor and Collector see the same real catalog, in order, with cover, name and genre. */
    @Test
    fun visitanteYColeccionistaVenLosCuatroAlbumesEnOrden() {
        val evidencia = "FN01_ESC01"
        ActivityScenario.launch(MainActivity::class.java).use {
            rule.onNodeWithTag(TagEntrarVisitante).performScrollTo().performClick()
            val catalogoVisitante = verAlbumes(evidencia, "01_inicio_visitante")
            rule.onNodeWithTag(TagRolActivo).assertTextEquals("VISITANTE")
            capturar(evidencia, "02_catalogo_visitante")
            comprobarLosAlbumesDelCatalogoReal(catalogoVisitante)

            // Go back to the entry screen and log in as collector.
            rule.onNodeWithTag(TagAtras).performClick()
            esperarA(TagInicio)
            Espresso.pressBack()
            esperarA(TagEntrarColeccionista)
            rule.onNodeWithTag(TagEntrarColeccionista).performScrollTo().performClick()
            esperarA(tagColeccionista(100))
            rule.onNodeWithTag(tagColeccionista(100)).performClick()
            val catalogoColeccionista = verAlbumes(evidencia, "03_inicio_coleccionista")
            rule.onNodeWithTag(TagRolActivo).assertTextEquals("COLECCIONISTA")
            capturar(evidencia, "04_catalogo_coleccionista")
            comprobarLosAlbumesDelCatalogoReal(catalogoColeccionista)
        }
    }

    /** FN01-ESC02: scroll through the real catalog to the end and refresh it by swiping down. */
    @Test
    fun recorrerHastaElFinalYDeslizarParaActualizar() {
        val evidencia = "FN01_ESC02"
        ActivityScenario.launch(MainActivity::class.java).use {
            rule.onNodeWithTag(TagEntrarVisitante).performScrollTo().performClick()
            val catalogo = verAlbumes(evidencia, "01_inicio_visitante")
            val ultimo = catalogo.last()

            verAlbum(ultimo.id)
            rule.onNodeWithTag(tagAlbum(ultimo.id)).assertIsDisplayed()
            capturar(evidencia, "02_final_del_catalogo")

            // At the top of the list, so the gesture refreshes instead of just scrolling.
            rule.onNodeWithTag(TagCuadriculaCatalogo).performScrollToIndex(0)
            rule.onNodeWithTag(TagCuadriculaCatalogo).performTouchInput {
                swipeDown(startY = top + 10f, endY = bottom, durationMillis = 600)
            }
            rule.waitUntil(15_000) { albumes.consultasCatalogo == 2 }
            rule.waitUntil(15_000) {
                rule.onAllNodesWithTag(TagActualizandoCatalogo).fetchSemanticsNodes().isEmpty()
            }
            val catalogoActualizado = requireNotNull(albumes.catalogo)
            rule.onNodeWithTag(TagContadorCatalogo).assertTextEquals(textoContador(catalogoActualizado.size))
            rule.onNodeWithTag(tagAlbum(catalogo.first().id)).assertIsDisplayed()
            capturar(evidencia, "03_tras_actualizar")
        }
    }

    /** FN01-ESC03: with the backend down the error shows up and "Reintentar" recovers the catalog (MockWebServer). */
    @Test
    fun conElBackendCaidoSeVeElErrorYReintentarRecuperaElCatalogo() {
        val evidencia = "FN01_ESC03"
        val servidor = MockWebServer().apply { start() }
        try {
            ApplicationProvider.getApplicationContext<VinilosApplication>().container =
                AppContainer(baseUrl = "http://localhost:${servidor.port}/")
            servidor.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

            ActivityScenario.launch(MainActivity::class.java).use {
                rule.onNodeWithTag(TagEntrarVisitante).performScrollTo().performClick()
                esperarA(TagInicio)
                EvidenciaE2E.capturar(rule, evidencia, "01_inicio_visitante")
                rule.onNodeWithTag(TagVerAlbumes).performScrollTo().performClick()
                esperarA(TagErrorCatalogo)
                rule.onNodeWithTag(TagErrorCatalogo).assertTextEquals("No pudimos cargar el catálogo")
                rule.onNodeWithText("Reintentar").performScrollTo().assertIsDisplayed()
                capturar(evidencia, "02_backend_caido")

                servidor.enqueue(
                    MockResponse().setBody(
                        """[{"id": 100, "name": "Buscando América", "genre": "Salsa",
                            "releaseDate": "1984-08-01T00:00:00.000Z",
                            "performers": [{"id": 1, "name": "Rubén Blades Bellido de Luna"}]}]"""
                    )
                )
                rule.onNodeWithTag(TagReintentarCatalogo).performScrollTo().performClick()
                esperarA(tagAlbum(100))
                rule.onNodeWithText("Buscando América").assertIsDisplayed()
                capturar(evidencia, "03_recuperado")
            }
        } finally {
            servidor.shutdown()
        }
    }

    /** FN01-ESC04: when rotating, the real catalog keeps its position and does not query the backend again. */
    @Test
    fun alGirarConservaLaPosicionSinVolverAConsultar() {
        val evidencia = "FN01_ESC04"
        ActivityScenario.launch(MainActivity::class.java).use { escenario ->
            rule.onNodeWithTag(TagEntrarVisitante).performScrollTo().performClick()
            val catalogo = verAlbumes(evidencia, "01_inicio_visitante")
            val ultimo = catalogo.last()
            verAlbum(ultimo.id)
            rule.onNodeWithTag(tagAlbum(ultimo.id)).assertIsDisplayed()
            capturar(evidencia, "02_antes_de_girar")

            escenario.recreate()
            rule.waitForIdle()

            rule.onNodeWithTag(tagAlbum(ultimo.id)).assertIsDisplayed()
            rule.onNodeWithTag(TagProgresoCatalogo).assertDoesNotExist()
            assertEquals(1, albumes.consultasCatalogo)
            capturar(evidencia, "03_despues_de_girar")
        }
    }

    /** FN01-ESC05: tapping a real album opens its detail and "Atrás" returns to the catalog without reloading it. */
    @Test
    fun tocarUnAlbumAbreSuDetalleYAtrasVuelveAlCatalogo() {
        val evidencia = "FN01_ESC05"
        ActivityScenario.launch(MainActivity::class.java).use {
            rule.onNodeWithTag(TagEntrarVisitante).performScrollTo().performClick()
            val catalogo = verAlbumes(evidencia, "01_inicio_visitante")
            val primero = catalogo.first()

            verAlbum(primero.id)
            rule.onNodeWithTag(tagAlbum(primero.id)).performClick()
            rule.waitUntil(15_000) {
                rule.onAllNodesWithText(primero.nombre).fetchSemanticsNodes().isNotEmpty()
            }
            rule.onNodeWithText(primero.nombre).assertIsDisplayed()
            capturar(evidencia, "02_detalle")

            Espresso.pressBack()
            esperarA(tagAlbum(primero.id))
            rule.onNodeWithTag(TagRolActivo).assertTextEquals("VISITANTE")
            assertEquals(1, albumes.consultasCatalogo)
            capturar(evidencia, "03_regreso_al_catalogo")
        }
    }
}
