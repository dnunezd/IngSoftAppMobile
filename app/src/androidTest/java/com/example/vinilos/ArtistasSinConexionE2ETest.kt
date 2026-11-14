package com.example.vinilos

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.example.vinilos.data.local.VinilosDb
import com.example.vinilos.data.model.Artist
import com.example.vinilos.data.model.ArtistType
import com.example.vinilos.ui.artistas.TagErrorArtistas
import com.example.vinilos.ui.artistas.TagNombreDetalleArtista
import com.example.vinilos.ui.artistas.tagArtista
import com.example.vinilos.ui.ingreso.TagAtras
import com.example.vinilos.ui.ingreso.TagEntrarVisitante
import com.example.vinilos.ui.inicio.TagInicio
import com.example.vinilos.ui.inicio.TagVerArtistas
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Listado y detalle de artistas guardados (add-cache-consultas, HU03 y HU04), con la pila de red real
 * contra `MockWebServer` y la base de Room en memoria. El listado hace dos consultas en paralelo, así que
 * el servidor responde por ruta y [caido] simula la falta de conexión.
 */
class ArtistasSinConexionE2ETest {

    @get:Rule
    val rule = createEmptyComposeRule()

    private lateinit var servidor: MockWebServer
    private lateinit var db: VinilosDb

    @Volatile private var caido = false

    private val queen = Artist(101, "Queen", null, ArtistType.BANDA)
    private val ruben = Artist(100, "Rubén Blades", null, ArtistType.MUSICO)

    private val rutas = mapOf(
        "/musicians" to """[{"id": 100, "name": "Rubén Blades", "image": null}]""",
        "/bands" to """[{"id": 101, "name": "Queen", "image": null}]""",
        "/bands/101" to """{"id": 101, "name": "Queen", "image": null, "description": "Banda británica",
            "creationDate": "1970-01-01T00:00:00.000Z", "albums": [], "musicians": [], "performerPrizes": []}"""
    )

    @Before
    fun preparar() {
        servidor = MockWebServer()
        servidor.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when {
                caido -> MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START)
                else -> rutas[request.path]?.let { MockResponse().setBody(it) } ?: MockResponse().setResponseCode(404)
            }
        }
        servidor.start()
        val contexto: Context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(contexto, VinilosDb::class.java).build()
        // "localhost" es uno de los hosts con HTTP permitido en network_security_config.xml.
        ApplicationProvider.getApplicationContext<VinilosApplication>().container =
            AppContainer(baseUrl = "http://localhost:${servidor.port}/", artistaDao = db.artistaDao())
    }

    @After
    fun terminar() {
        servidor.shutdown()
        db.close()
    }

    private fun esperarA(tag: String, milis: Long = 15_000) =
        rule.waitUntil(milis) { rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }

    private fun esperarTexto(texto: String, milis: Long = 15_000) =
        rule.waitUntil(milis) { rule.onAllNodesWithText(texto).fetchSemanticsNodes().isNotEmpty() }

    private fun abrirListado() {
        esperarA(TagInicio)
        rule.onNodeWithTag(TagVerArtistas).performScrollTo().performClick()
    }

    private fun abrir(artista: Artist) {
        esperarA(tagArtista(artista))
        rule.onNodeWithTag(tagArtista(artista)).performScrollTo().performClick()
    }

    private fun atras() = rule.onNodeWithTag(TagAtras).performClick()

    /** Con el servidor sano: entra como Visitante, ve el listado y abre el detalle de Queen. */
    private fun verListadoYQueenConConexion() {
        rule.onNodeWithTag(TagEntrarVisitante).performScrollTo().performClick()
        abrirListado()
        abrir(queen)
        esperarA(TagNombreDetalleArtista)
        rule.onNodeWithTag(TagNombreDetalleArtista).assertTextEquals("Queen")
        atras()
        esperarA(tagArtista(queen))
        atras()
    }

    @Test
    fun sinConexionSeVenElListadoYElDetalleYaAbiertoConSusAvisos() {
        ActivityScenario.launch(MainActivity::class.java).use {
            verListadoYQueenConConexion()

            caido = true
            abrirListado()
            esperarTexto("No se pudo actualizar el listado de artistas")
            rule.onNodeWithTag(tagArtista(queen)).assertIsDisplayed()
            rule.onNodeWithTag(tagArtista(ruben)).assertIsDisplayed()
            assertTrue(rule.onAllNodesWithTag(TagErrorArtistas).fetchSemanticsNodes().isEmpty())

            abrir(queen)
            esperarTexto("No se pudo actualizar el artista")
            rule.onNodeWithTag(TagNombreDetalleArtista).assertTextEquals("Queen")
            assertTrue(rule.onAllNodesWithText("No se pudo cargar el artista").fetchSemanticsNodes().isEmpty())
        }
    }

    @Test
    fun sinConexionUnArtistaNuncaAbiertoMuestraElError() {
        ActivityScenario.launch(MainActivity::class.java).use {
            verListadoYQueenConConexion()

            caido = true
            abrirListado()
            esperarTexto("No se pudo actualizar el listado de artistas")
            abrir(ruben)
            esperarTexto("No se pudo cargar el artista")
            assertTrue(rule.onAllNodesWithText("No se pudo actualizar el artista").fetchSemanticsNodes().isEmpty())
        }
    }
}
