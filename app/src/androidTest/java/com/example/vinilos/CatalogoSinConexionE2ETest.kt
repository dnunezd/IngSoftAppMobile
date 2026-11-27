package com.example.vinilos

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.example.vinilos.data.local.VinilosDb
import com.example.vinilos.ui.catalog.TagErrorCatalogo
import com.example.vinilos.ui.catalog.tagAlbum
import com.example.vinilos.ui.entry.TagAtras
import com.example.vinilos.ui.entry.TagEntrarVisitante
import com.example.vinilos.ui.home.TagInicio
import com.example.vinilos.ui.home.TagVerAlbumes
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Catalog saved on the device (add-cache-catalogo), with the real network stack against
 * `MockWebServer` and an in-memory Room database. Leaving the catalog discards its ViewModel, so
 * entering again repeats the initial load without restarting the app.
 */
class CatalogoSinConexionE2ETest {

    @get:Rule
    val rule = createEmptyComposeRule()

    private lateinit var servidor: MockWebServer
    private lateinit var db: VinilosDb

    @Before
    fun preparar() {
        servidor = MockWebServer().apply { start() }
        val contexto: Context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(contexto, VinilosDb::class.java).build()
        // "localhost" is one of the hosts allowed over HTTP in network_security_config.xml.
        ApplicationProvider.getApplicationContext<VinilosApplication>().container =
            AppContainer(baseUrl = "http://localhost:${servidor.port}/", albumDao = db.albumDao())
    }

    @After
    fun terminar() {
        servidor.shutdown()
        db.close()
    }

    private val tresAlbumes = """
        [
          {"id": 100, "name": "Buscando América", "genre": "Salsa", "releaseDate": "1984-08-01T00:00:00.000Z",
           "performers": [{"id": 1, "name": "Rubén Blades Bellido de Luna"}]},
          {"id": 101, "name": "Poeta del pueblo", "genre": "Salsa"},
          {"id": 102, "name": "A Night at the Opera", "genre": "Rock",
           "performers": [{"id": 2, "name": "Queen"}]}
        ]
    """.trimIndent()

    private val sinPoeta = """
        [
          {"id": 100, "name": "Buscando América", "genre": "Salsa"},
          {"id": 102, "name": "A Night at the Opera", "genre": "Rock"}
        ]
    """.trimIndent()

    private fun responder(cuerpo: String) = servidor.enqueue(MockResponse().setBody(cuerpo))

    /** The server drops the connection: this is what the app sees without network or with the backend down. */
    private fun servidorCaido() =
        servidor.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

    private fun esperarA(tag: String, milis: Long = 15_000) =
        rule.waitUntil(milis) { rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }

    private fun esperarAvisoDeActualizacion() = rule.waitUntil(15_000) {
        rule.onAllNodesWithText("No pudimos actualizar el catálogo").fetchSemanticsNodes().isNotEmpty()
    }

    private fun esperarPeticiones(cantidad: Int) =
        rule.waitUntil(15_000) { servidor.requestCount >= cantidad }

    private fun abrirCatalogo() {
        esperarA(TagInicio)
        rule.onNodeWithTag(TagVerAlbumes).performScrollTo().performClick()
    }

    private fun volverAlInicio() {
        rule.onNodeWithTag(TagAtras).performClick()
        esperarA(TagInicio)
    }

    private fun idsVisibles(ids: List<Int>) = ids.filter {
        rule.onAllNodesWithTag(tagAlbum(it)).fetchSemanticsNodes().isNotEmpty()
    }

    @Test
    fun sinConexionSeVenLosAlbumesGuardadosConElAvisoYSinError() {
        ActivityScenario.launch(MainActivity::class.java).use {
            responder(tresAlbumes)
            rule.onNodeWithTag(TagEntrarVisitante).performScrollTo().performClick()
            abrirCatalogo()
            esperarA(tagAlbum(100))
            volverAlInicio()

            servidorCaido()
            abrirCatalogo()
            esperarPeticiones(2)
            esperarAvisoDeActualizacion()

            rule.onNodeWithText("Buscando América").assertIsDisplayed()
            assertEquals(listOf(100, 101, 102), idsVisibles(listOf(100, 101, 102)))
            assertTrue(
                "Con catálogo guardado no debe verse la pantalla de error",
                rule.onAllNodesWithTag(TagErrorCatalogo).fetchSemanticsNodes().isEmpty()
            )
        }
    }

    @Test
    fun unAlbumQueYaNoVieneDesapareceYNoVuelveSinConexion() {
        ActivityScenario.launch(MainActivity::class.java).use {
            responder(tresAlbumes)
            rule.onNodeWithTag(TagEntrarVisitante).performScrollTo().performClick()
            abrirCatalogo()
            esperarA(tagAlbum(101))
            volverAlInicio()

            responder(sinPoeta)
            abrirCatalogo()
            esperarPeticiones(2)
            rule.waitUntil(15_000) { idsVisibles(listOf(101)).isEmpty() }
            assertEquals(listOf(100, 102), idsVisibles(listOf(100, 101, 102)))
            volverAlInicio()

            servidorCaido()
            abrirCatalogo()
            esperarPeticiones(3)
            esperarAvisoDeActualizacion()
            assertEquals(listOf(100, 102), idsVisibles(listOf(100, 101, 102)))
        }
    }
}
