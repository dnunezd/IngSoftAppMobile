package com.example.vinilos

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.example.vinilos.data.local.VinilosDb
import com.example.vinilos.ui.catalogo.tagAlbum
import com.example.vinilos.ui.ingreso.TagEntrarVisitante
import com.example.vinilos.ui.inicio.TagInicio
import com.example.vinilos.ui.inicio.TagVerAlbumes
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Detalle de álbum guardado (add-cache-consultas, HU02), con la pila de red real contra `MockWebServer`
 * y la base de Room en memoria. El detalle se guarda con la consulta del catálogo: el usuario nunca abrió
 * el álbum con conexión.
 */
class DetalleAlbumSinConexionE2ETest {

    @get:Rule
    val rule = createEmptyComposeRule()

    private lateinit var servidor: MockWebServer
    private lateinit var db: VinilosDb

    @Before
    fun preparar() {
        servidor = MockWebServer().apply { start() }
        val contexto: Context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(contexto, VinilosDb::class.java).build()
        // "localhost" es uno de los hosts con HTTP permitido en network_security_config.xml.
        ApplicationProvider.getApplicationContext<VinilosApplication>().container =
            AppContainer(baseUrl = "http://localhost:${servidor.port}/", albumDao = db.albumDao())
    }

    @After
    fun terminar() {
        servidor.shutdown()
        db.close()
    }

    private val catalogo = """
        [
          {"id": 100, "name": "Buscando América", "cover": null, "description": "Salsa urbana",
           "genre": "Salsa", "recordLabel": "Elektra", "releaseDate": "1984-08-01T00:00:00.000Z",
           "tracks": [{"id": 1, "name": "Decisiones", "duration": "5:05"}],
           "performers": [{"id": 1, "name": "Rubén Blades"}],
           "comments": [{"id": 1, "description": "Un clásico de la salsa", "rating": 5}]},
          {"id": 101, "name": "Poeta del pueblo", "genre": "Salsa"}
        ]
    """.trimIndent()

    private fun responder(cuerpo: String) = servidor.enqueue(MockResponse().setBody(cuerpo))

    /** El servidor corta la conexión: es lo que ve la app sin red o con el backend caído. */
    private fun servidorCaido() =
        servidor.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

    private fun esperarA(tag: String, milis: Long = 15_000) =
        rule.waitUntil(milis) { rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }

    private fun esperarTexto(texto: String, milis: Long = 15_000) =
        rule.waitUntil(milis) { rule.onAllNodesWithText(texto).fetchSemanticsNodes().isNotEmpty() }

    private fun esperarPeticiones(cantidad: Int) =
        rule.waitUntil(15_000) { servidor.requestCount >= cantidad }

    private fun noSeVe(texto: String) = rule.onAllNodesWithText(texto).fetchSemanticsNodes().isEmpty()

    /** Entra como Visitante y abre el catálogo con el servidor sano, sin abrir ningún detalle. */
    private fun verCatalogoConConexion() {
        responder(catalogo)
        rule.onNodeWithTag(TagEntrarVisitante).performScrollTo().performClick()
        esperarA(TagInicio)
        rule.onNodeWithTag(TagVerAlbumes).performScrollTo().performClick()
        esperarA(tagAlbum(100))
    }

    private fun abrirAlbum(id: Int) = rule.onNodeWithTag(tagAlbum(id)).performScrollTo().performClick()

    @Test
    fun sinConexionSeVeElDetalleGuardadoConElCatalogoYElAviso() {
        ActivityScenario.launch(MainActivity::class.java).use {
            verCatalogoConConexion()

            servidorCaido()
            abrirAlbum(100)
            esperarPeticiones(2)
            esperarTexto("No pudimos actualizar el álbum")

            rule.onNodeWithText("Buscando América").assertIsDisplayed()
            val detalle = rule.onNode(hasScrollAction())
            detalle.performScrollToNode(hasText("Decisiones"))
            rule.onNodeWithText("Decisiones").assertIsDisplayed()
            detalle.performScrollToNode(hasText("Un clásico de la salsa"))
            rule.onNodeWithText("Un clásico de la salsa").assertIsDisplayed()
            assertTrue("Con detalle guardado no debe verse el error", noSeVe("No pudimos cargar el álbum"))
        }
    }

    @Test
    fun unAlbumSinDetalleValidoMuestraElErrorSinConexion() {
        ActivityScenario.launch(MainActivity::class.java).use {
            verCatalogoConConexion()

            // El 101 está en el catálogo, pero le faltan descripción, disquera y fecha: no se guardó su detalle.
            servidorCaido()
            abrirAlbum(101)
            esperarPeticiones(2)
            esperarTexto("No pudimos cargar el álbum")
            assertTrue(noSeVe("No pudimos actualizar el álbum"))
        }
    }

    @Test
    fun un404BorraElDetalleGuardadoYLaSiguienteAperturaSinConexionNoLoMuestra() {
        ActivityScenario.launch(MainActivity::class.java).use {
            verCatalogoConConexion()

            servidor.enqueue(MockResponse().setResponseCode(404))
            abrirAlbum(100)
            esperarPeticiones(2)
            esperarTexto("El álbum ya no está disponible")
            rule.onNodeWithText("Volver al catálogo").performClick()
            esperarA(tagAlbum(100))

            servidorCaido()
            abrirAlbum(100)
            esperarPeticiones(3)
            esperarTexto("No pudimos cargar el álbum")
            assertTrue(noSeVe("Buscando América"))
        }
    }
}
