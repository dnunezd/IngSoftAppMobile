package com.example.vinilos

import android.content.Context
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.example.vinilos.data.local.VinilosDb
import com.example.vinilos.data.model.Artist
import com.example.vinilos.data.model.ArtistType
import com.example.vinilos.ui.artistas.TagBotonFavorito
import com.example.vinilos.ui.artistas.TagErrorFavorito
import com.example.vinilos.ui.artistas.TagNombreDetalleArtista
import com.example.vinilos.ui.artistas.TagReintentarFavorito
import com.example.vinilos.ui.artistas.tagArtista
import com.example.vinilos.ui.ingreso.TagAtras
import com.example.vinilos.ui.ingreso.TagEntrarColeccionista
import com.example.vinilos.ui.ingreso.tagColeccionista
import com.example.vinilos.ui.inicio.TagInicio
import com.example.vinilos.ui.inicio.TagVerArtistas
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.Collections

/**
 * Favoritos confirmados guardados (add-cache-consultas, HU10), con la pila de red real contra un
 * `MockWebServer` que recuerda los favoritos agregados, y la base de Room en memoria. El detalle del
 * artista también se guarda, porque el botón de favorito solo aparece con el detalle en pantalla.
 */
class FavoritosSinConexionE2ETest {

    @get:Rule
    val rule = createEmptyComposeRule()

    private lateinit var servidor: MockWebServer
    private lateinit var db: VinilosDb

    @Volatile private var caido = false
    private val favoritosDelBackend: MutableSet<Int> = Collections.synchronizedSet(mutableSetOf())

    private val queen = Artist(101, "Queen", null, ArtistType.BANDA)
    private val ruben = Artist(100, "Rubén Blades", null, ArtistType.MUSICO)

    private fun detalle(id: Int, nombre: String, fecha: String) = """{"id": $id, "name": "$nombre", "image": null,
        "description": "Descripción", "$fecha": "1970-01-01T00:00:00.000Z", "albums": [], "musicians": [],
        "performerPrizes": []}"""

    private fun responder(request: RecordedRequest): MockResponse {
        if (caido) return MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START)
        val cuerpo = when (request.method to request.path) {
            "GET" to "/collectors" -> """[{"id": 100, "name": "Manolo Bellon", "email": "m@a.com"}]"""
            "GET" to "/musicians" -> """[{"id": 100, "name": "Rubén Blades", "image": null}]"""
            "GET" to "/bands" -> """[{"id": 101, "name": "Queen", "image": null}]"""
            "GET" to "/bands/101" -> detalle(101, "Queen", "creationDate")
            "GET" to "/musicians/100" -> detalle(100, "Rubén Blades", "birthDate")
            "GET" to "/collectors/100/performers" -> favoritosDelBackend.joinToString(",", "[", "]") { """{"id": $it}""" }
            "POST" to "/collectors/100/bands/101" -> {
                favoritosDelBackend += 101
                """{"id": 101}"""
            }
            else -> return MockResponse().setResponseCode(404)
        }
        return MockResponse().setBody(cuerpo)
    }

    @Before
    fun preparar() {
        servidor = MockWebServer()
        servidor.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest) = responder(request)
        }
        servidor.start()
        val contexto: Context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(contexto, VinilosDb::class.java).build()
        // "localhost" es uno de los hosts con HTTP permitido en network_security_config.xml.
        ApplicationProvider.getApplicationContext<VinilosApplication>().container = AppContainer(
            baseUrl = "http://localhost:${servidor.port}/",
            artistaDao = db.artistaDao(),
            favoritoDao = db.favoritoDao()
        )
    }

    @After
    fun terminar() {
        servidor.shutdown()
        db.close()
    }

    private fun esperarA(tag: String, milis: Long = 15_000) =
        rule.waitUntil(milis) { rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }

    /** Espera a que el botón de favorito muestre [texto]. */
    private fun esperarBoton(texto: String, milis: Long = 15_000) = rule.waitUntil(milis) {
        rule.onAllNodes(hasTestTag(TagBotonFavorito) and hasText(texto)).fetchSemanticsNodes().isNotEmpty()
    }

    private fun boton() = rule.onNodeWithTag(TagBotonFavorito).performScrollTo()

    private fun entrarComoColeccionista() {
        esperarA(TagEntrarColeccionista)
        rule.onNodeWithTag(TagEntrarColeccionista).performScrollTo().performClick()
        esperarA(tagColeccionista(100))
        rule.onNodeWithTag(tagColeccionista(100)).performClick()
        esperarA(TagInicio)
        rule.onNodeWithTag(TagVerArtistas).performScrollTo().performClick()
    }

    private fun abrir(artista: Artist) {
        esperarA(tagArtista(artista))
        rule.onNodeWithTag(tagArtista(artista)).performScrollTo().performClick()
        esperarA(TagNombreDetalleArtista)
    }

    private fun alListado(artista: Artist) {
        rule.onNodeWithTag(TagAtras).performClick()
        esperarA(tagArtista(artista))
    }

    @Test
    fun sinConexionUnFavoritoConfirmadoSeVeComoFavoritoYOtroArtistaOfreceReintentar() {
        ActivityScenario.launch(MainActivity::class.java).use {
            // Con conexión: agregar Queen y abrir Rubén, que no es favorito.
            entrarComoColeccionista()
            abrir(queen)
            esperarBoton("Agregar Favorito")
            boton().assertIsEnabled().performClick()
            esperarBoton("Favorito")
            alListado(ruben)
            abrir(ruben)
            esperarBoton("Agregar Favorito")
            alListado(queen)

            // Sin conexión: el listado y los detalles salen de lo guardado.
            caido = true
            abrir(queen)
            esperarBoton("Favorito")
            boton().assert(hasText("Favorito")).assertIsNotEnabled()
            assertEquals(0, rule.onAllNodesWithTag(TagErrorFavorito).fetchSemanticsNodes().size)

            alListado(ruben)
            abrir(ruben)
            esperarA(TagReintentarFavorito)
            boton().assertIsNotEnabled()
            rule.onNodeWithTag(TagErrorFavorito).assertIsDisplayed()
        }
    }
}
