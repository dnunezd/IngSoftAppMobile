package com.example.vinilos

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.example.vinilos.ui.catalogo.TagErrorCatalogo
import com.example.vinilos.ui.catalogo.TagReintentarCatalogo
import com.example.vinilos.ui.catalogo.tagAlbum
import com.example.vinilos.ui.ingreso.TagEntrarColeccionista
import com.example.vinilos.ui.ingreso.TagEntrarVisitante
import com.example.vinilos.ui.ingreso.TagMensajeError
import com.example.vinilos.ui.ingreso.TagReintentar
import com.example.vinilos.ui.ingreso.TagVisitanteError
import com.example.vinilos.ui.ingreso.tagColeccionista
import com.example.vinilos.ui.inicio.TagInicio
import com.example.vinilos.ui.inicio.TagVerAlbumes
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Recuperación ante fallos del backend, sin pasos manuales ni infraestructura: la app usa su pila
 * de red real (Retrofit y OkHttp) contra un servidor local que la prueba hace caer y volver con
 * `MockWebServer`. Corre solo con el dispositivo, sin Docker ni `adb reverse`.
 */
class RecuperacionTest {

    @get:Rule
    val rule = createEmptyComposeRule()

    private lateinit var servidor: MockWebServer

    private val coleccionistas = """
        [
          {"id": 100, "name": "Manolo Bellon", "email": "manollo@caracol.com.co"},
          {"id": 101, "name": "Jaime Monsalve", "email": "jmonsalve@rtvc.com.co"}
        ]
    """.trimIndent()

    @Before
    fun iniciarServidor() {
        servidor = MockWebServer().apply { start() }
        // "localhost" es uno de los hosts con HTTP permitido en network_security_config.xml.
        ApplicationProvider.getApplicationContext<VinilosApplication>().container =
            AppContainer(baseUrl = "http://localhost:${servidor.port}/")
    }

    @After
    fun detenerServidor() {
        servidor.shutdown()
    }

    /** El servidor corta la conexión: es lo que ve la app cuando el backend está caído. */
    private fun servidorCaido() =
        servidor.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

    private fun servidorConErrorInterno() = servidor.enqueue(MockResponse().setResponseCode(500))

    private fun servidorSano() = servidor.enqueue(MockResponse().setBody(coleccionistas))

    private fun esperarA(tag: String, milis: Long = 15_000) =
        rule.waitUntil(milis) { rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }

    private fun esperarPeticiones(cantidad: Int) =
        rule.waitUntil(15_000) { servidor.requestCount >= cantidad }

    private fun entrarComoColeccionista() {
        rule.onNodeWithTag(TagEntrarColeccionista).performScrollTo().performClick()
    }

    private fun reintentar() {
        esperarA(TagReintentar)
        rule.onNodeWithTag(TagReintentar).performClick()
    }

    @Test
    fun conElServidorCaidoSeVeElErrorYReintentarRecuperaLaLista() {
        servidorCaido()

        ActivityScenario.launch(MainActivity::class.java).use {
            entrarComoColeccionista()

            esperarA(TagMensajeError)
            rule.onNodeWithText("No pudimos cargar los coleccionistas").assertIsDisplayed()
            rule.onNodeWithTag(TagVisitanteError).assertIsDisplayed()

            // El servidor "vuelve" y la persona pulsa Reintentar.
            servidorSano()
            reintentar()

            esperarA(tagColeccionista(100))
            rule.onNodeWithText("Manolo Bellon").assertIsDisplayed()
            rule.onNodeWithText("Jaime Monsalve").assertIsDisplayed()
            assertEquals(2, servidor.requestCount)
        }
    }

    @Test
    fun unErrorDelServidorSeRecuperaConReintentar() {
        servidorConErrorInterno()

        ActivityScenario.launch(MainActivity::class.java).use {
            entrarComoColeccionista()
            esperarA(TagMensajeError)
            rule.onNodeWithText("No pudimos cargar los coleccionistas").assertIsDisplayed()

            servidorSano()
            reintentar()

            esperarA(tagColeccionista(101))
            rule.onNodeWithText("Jaime Monsalve").assertIsDisplayed()
        }
    }

    @Test
    fun variosReintentosFallidosSeguidosDeUnoExitosoMuestranLaLista() {
        servidorCaido()
        servidorConErrorInterno()
        servidorCaido()
        servidorSano()

        ActivityScenario.launch(MainActivity::class.java).use {
            entrarComoColeccionista()
            esperarPeticiones(1)
            esperarA(TagMensajeError)

            // Cada reintento fallido vuelve a mostrar el mensaje de error con "Reintentar".
            repeat(2) { vez ->
                reintentar()
                esperarPeticiones(vez + 2)
                esperarA(TagMensajeError)
                rule.onNodeWithText("No pudimos cargar los coleccionistas").assertIsDisplayed()
                rule.onNodeWithTag(TagReintentar).assertIsDisplayed()
            }

            reintentar()

            esperarA(tagColeccionista(100))
            rule.onNodeWithText("Manolo Bellon").assertIsDisplayed()
            assertEquals(4, servidor.requestCount)
        }
    }

    @Test
    fun ingresarComoVisitanteDesdeElErrorEntraAlInicio() {
        servidorCaido()

        ActivityScenario.launch(MainActivity::class.java).use {
            entrarComoColeccionista()
            esperarA(TagMensajeError)

            rule.onNodeWithTag(TagVisitanteError).performClick()

            esperarA(TagInicio)
            rule.onNodeWithText("¡Hola, melómano!").assertIsDisplayed()
        }
    }

    private val albumes = """
        [
          {"id": 100, "name": "Buscando América", "genre": "Salsa", "releaseDate": "1984-08-01T00:00:00.000Z",
           "performers": [{"id": 1, "name": "Rubén Blades Bellido de Luna"}]}
        ]
    """.trimIndent()

    @Test
    fun elCatalogoConElBackendCaidoMuestraElErrorYReintentarLoRecupera() {
        ActivityScenario.launch(MainActivity::class.java).use {
            rule.onNodeWithTag(TagEntrarVisitante).performScrollTo().performClick()
            esperarA(TagInicio)
            servidorCaido()

            rule.onNodeWithTag(TagVerAlbumes).performScrollTo().performClick()
            esperarA(TagErrorCatalogo)
            rule.onNodeWithTag(TagErrorCatalogo).assertTextEquals("No pudimos cargar el catálogo")

            servidor.enqueue(MockResponse().setBody(albumes))
            rule.onNodeWithTag(TagReintentarCatalogo).performClick()

            esperarA(tagAlbum(100))
            rule.onNodeWithText("Buscando América").assertIsDisplayed()
        }
    }
}
