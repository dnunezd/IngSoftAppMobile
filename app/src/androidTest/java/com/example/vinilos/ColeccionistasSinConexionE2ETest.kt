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
 * Selector de coleccionistas guardado (add-cache-consultas, HU00), con la pila de red real contra
 * `MockWebServer` y la base de Room en memoria. Volver al ingreso descarta el ViewModel del selector, así
 * que entrar de nuevo repite la carga sin reiniciar la app.
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
        // "localhost" es uno de los hosts con HTTP permitido en network_security_config.xml.
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

    /** El servidor corta la conexión: es lo que ve la app sin red o con el backend caído. */
    private fun servidorCaido() =
        servidor.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

    private fun esperarA(tag: String, milis: Long = 15_000) =
        rule.waitUntil(milis) { rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }

    private fun esperarTexto(texto: String, milis: Long = 15_000) =
        rule.waitUntil(milis) { rule.onAllNodesWithText(texto).fetchSemanticsNodes().isNotEmpty() }

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
            esperarTexto("No pudimos actualizar los coleccionistas")
            assertTrue(
                "Con listado guardado no debe verse el error",
                rule.onAllNodesWithTag(TagMensajeError).fetchSemanticsNodes().isEmpty()
            )

            rule.onNodeWithTag(TagBuscador).performTextInput("monsalve")
            rule.onNodeWithTag(tagColeccionista(101)).assertIsDisplayed()
            rule.onNodeWithTag(tagColeccionista(100)).assertDoesNotExist()
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
            esperarTexto("No pudimos actualizar los coleccionistas")
            rule.onNodeWithTag(tagColeccionista(100)).assertIsDisplayed()
            rule.onNodeWithTag(tagColeccionista(101)).assertDoesNotExist()
        }
    }
}
