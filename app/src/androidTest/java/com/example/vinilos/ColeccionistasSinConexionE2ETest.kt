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
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import com.example.vinilos.data.local.VinilosDb
import com.example.vinilos.ui.ingreso.TagAtras
import com.example.vinilos.ui.ingreso.TagBuscador
import com.example.vinilos.ui.ingreso.TagEntrarColeccionista
import com.example.vinilos.ui.ingreso.TagMensajeError
import com.example.vinilos.ui.ingreso.tagColeccionista
import com.example.vinilos.ui.inicio.TagInicio
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Saved collector selector (add-cache-consultas, HU00), using the real network stack against
 * `MockWebServer` and the in-memory Room database. Going back to login discards the selector's ViewModel, so
 * entering again repeats the load without restarting the app.
 */
class ColeccionistasSinConexionE2ETest {

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
            AppContainer(baseUrl = "http://localhost:${servidor.port}/", coleccionistaDao = db.coleccionistaDao())
    }

    @After
    fun terminar() {
        servidor.shutdown()
        db.close()
    }

    private val coleccionistas = """
        [
          {"id": 100, "name": "Manolo Bellon", "email": "manollo@caracol.com.co"},
          {"id": 101, "name": "Jaime Monsalve", "email": "jmonsalve@rtvc.com.co"}
        ]
    """.trimIndent()

    private fun responder(cuerpo: String) = servidor.enqueue(MockResponse().setBody(cuerpo))

    /** The server cuts the connection: this is what the app sees with no network or a down backend. */
    private fun servidorCaido() =
        servidor.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

    private fun esperarA(tag: String, milis: Long = 15_000) =
        rule.waitUntil(milis) { rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }

    private fun esperarAvisoDeActualizacion() = rule.waitUntil(15_000) {
        rule.onAllNodesWithText("No pudimos actualizar los coleccionistas").fetchSemanticsNodes().isNotEmpty()
    }

    private fun esperarQueSeOculteElAviso() = rule.waitUntil(15_000) {
        rule.onAllNodesWithText("No pudimos actualizar los coleccionistas").fetchSemanticsNodes().isEmpty()
    }

    private fun esperarPeticiones(cantidad: Int) =
        rule.waitUntil(15_000) { servidor.requestCount >= cantidad }

    private fun abrirSelector() {
        esperarA(TagEntrarColeccionista)
        rule.onNodeWithTag(TagEntrarColeccionista).performScrollTo().performClick()
    }

    @Test
    fun sinConexionSeVeElListadoGuardadoYSePuedeEntrarComoColeccionista() {
        ActivityScenario.launch(MainActivity::class.java).use {
            responder(coleccionistas)
            abrirSelector()
            esperarA(tagColeccionista(100))
            rule.onNodeWithTag(TagAtras).performClick()

            servidorCaido()
            abrirSelector()
            esperarPeticiones(2)
            esperarAvisoDeActualizacion()
            assertTrue(
                "Con listado guardado no debe verse el error",
                rule.onAllNodesWithTag(TagMensajeError).fetchSemanticsNodes().isEmpty()
            )

            rule.onNodeWithTag(TagBuscador).performTextInput("monsalve")
            rule.onNodeWithTag(tagColeccionista(101)).assertIsDisplayed()
            rule.onNodeWithTag(tagColeccionista(100)).assertDoesNotExist()
            // On small screens (API 23), with the keyboard open the notice covers the card and intercepts the tap.
            Espresso.closeSoftKeyboard()
            esperarQueSeOculteElAviso()
            rule.onNodeWithTag(tagColeccionista(101)).performClick()

            esperarA(TagInicio)
            rule.onNodeWithText("¡Hola, Jaime Monsalve!").assertIsDisplayed()
        }
    }

    @Test
    fun unColeccionistaQueYaNoVieneNoApareceSinConexion() {
        ActivityScenario.launch(MainActivity::class.java).use {
            responder(coleccionistas)
            abrirSelector()
            esperarA(tagColeccionista(101))
            rule.onNodeWithTag(TagAtras).performClick()

            responder("""[{"id": 100, "name": "Manolo Bellon", "email": "manollo@caracol.com.co"}]""")
            abrirSelector()
            esperarPeticiones(2)
            rule.waitUntil(15_000) { rule.onAllNodesWithTag(tagColeccionista(101)).fetchSemanticsNodes().isEmpty() }
            rule.onNodeWithTag(TagAtras).performClick()

            servidorCaido()
            abrirSelector()
            esperarPeticiones(3)
            esperarAvisoDeActualizacion()
            rule.onNodeWithTag(tagColeccionista(100)).assertIsDisplayed()
            rule.onNodeWithTag(tagColeccionista(101)).assertDoesNotExist()
        }
    }
}
