package com.example.vinilos

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import com.example.vinilos.data.model.Artist
import com.example.vinilos.data.model.ArtistType
import com.example.vinilos.data.remote.ServiceAdapter
import com.example.vinilos.data.repository.ArtistRepository
import com.example.vinilos.e2e.EvidenciaE2E
import com.example.vinilos.ui.artistas.TagArtistas
import com.example.vinilos.ui.artistas.TagBuscadorArtistas
import com.example.vinilos.ui.artistas.TagContadorArtistas
import com.example.vinilos.ui.artistas.TagDetalleArtista
import com.example.vinilos.ui.artistas.TagErrorArtistas
import com.example.vinilos.ui.artistas.TagListaArtistas
import com.example.vinilos.ui.artistas.TagNombreDetalleArtista
import com.example.vinilos.ui.artistas.TagReintentarArtistas
import com.example.vinilos.ui.artistas.TagTipoDetalleArtista
import com.example.vinilos.ui.artistas.TagVacioArtistas
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
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * E2E de la HU03 (listado de artistas) como Visitante.
 *
 * FN03-ESC01 a ESC04 y ESC07 van contra el backend real BackVynils, con sus datos de ejemplo: la
 * banda Queen (101) y el músico Rubén Blades Bellido de Luna (100), cuyas URL de imagen están rotas.
 * En el emulador la app llega al backend por `10.0.2.2`; en un teléfono, con
 * `adb reverse tcp:3000 tcp:3000` e instalando la app con `-PbaseUrl=http://localhost:3000/`.
 *
 * FN03-ESC05 (backend caído) y ESC06 (sin artistas) no se pueden provocar contra BackVynils sin
 * detenerlo o borrar datos, así que usan `MockWebServer`, igual que [RecuperacionTest].
 *
 * Cada escenario guarda un pantallazo por paso, `informe.md` y un PDF en
 * `.../files/e2e-evidencia/FN03_ESCxx/` (solo en el dispositivo, no en el repo).
 */
class ArtistasE2ETest {

    @get:Rule
    val rule = createEmptyComposeRule()

    private val queen = Artist(101, "Queen", null, ArtistType.BANDA)
    private val ruben = Artist(100, "Rubén Blades Bellido de Luna", null, ArtistType.MUSICO)

    private var servidor: MockWebServer? = null

    @Before
    fun usarElBackendReal() {
        ApplicationProvider.getApplicationContext<VinilosApplication>().container = AppContainer()
    }

    @After
    fun detenerServidor() {
        servidor?.shutdown()
    }

    private fun esperarA(tag: String, milis: Long = 15_000) =
        rule.waitUntil(milis) { rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }

    private fun esperarQueDesaparezca(tag: String, milis: Long = 5_000) =
        rule.waitUntil(milis) { rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isEmpty() }

    /** Visitante -> inicio -> "Ver artistas". */
    private fun abrirArtistasComoVisitante(evidencia: String) {
        esperarA(TagEntrarVisitante)
        rule.onNodeWithTag(TagEntrarVisitante).performScrollTo().performClick()
        esperarA(TagInicio)
        EvidenciaE2E.capturar(rule, evidencia, "01_inicio_visitante")
        rule.onNodeWithTag(TagVerArtistas).performScrollTo().performClick()
        esperarA(TagArtistas)
    }

    private fun esperarListado() = esperarA(tagArtista(queen))

    /** Desplaza la lista hasta la fila (en pantallas pequeñas puede no estar compuesta aún). */
    private fun fila(artista: Artist): SemanticsNodeInteraction {
        rule.onNodeWithTag(TagListaArtistas).performScrollToNode(hasTestTag(tagArtista(artista)))
        return rule.onNodeWithTag(tagArtista(artista))
    }

    private fun buscar(texto: String) {
        rule.onNodeWithTag(TagListaArtistas).performScrollToNode(hasTestTag(TagBuscadorArtistas))
        rule.onNodeWithTag(TagBuscadorArtistas).performTextInput(texto)
        rule.waitForIdle()
    }

    private fun borrarBusqueda() {
        rule.onNodeWithTag(TagBuscadorArtistas).performTextClearance()
        rule.waitForIdle()
    }

    private fun contadorDice(texto: String) {
        rule.onNodeWithTag(TagListaArtistas).performScrollToNode(hasTestTag(TagContadorArtistas))
        rule.onNodeWithTag(TagContadorArtistas).assertTextEquals(texto)
    }

    private fun noAparece(artista: Artist) =
        rule.onAllNodesWithTag(tagArtista(artista)).assertCountEquals(0)

    /** FN03-ESC01: ver el listado de artistas desde el menú principal. */
    @Test
    fun elVisitanteVeMusicosYBandasEnOrdenConImagenNombreYEtiqueta() {
        val evidencia = "FN03_ESC01"
        ActivityScenario.launch(MainActivity::class.java).use {
            abrirArtistasComoVisitante(evidencia)
            esperarListado()

            rule.onNodeWithTag(TagBuscadorArtistas).assertIsDisplayed()
            contadorDice("2 artistas")

            // onAllNodes devuelve los nodos en el orden del árbol, que es el orden en pantalla.
            val filas = rule.onAllNodes(hasTestTag(tagArtista(queen)) or hasTestTag(tagArtista(ruben)))
            filas.assertCountEquals(2)
            filas[0].assert(hasTestTag(tagArtista(queen)))
            filas[1].assert(hasTestTag(tagArtista(ruben)))

            fila(queen).assert(hasText("Queen")).assert(hasText("Banda"))
            rule.onNodeWithContentDescription("Imagen de Queen", useUnmergedTree = true).assertExists()
            fila(ruben).assert(hasText("Rubén Blades Bellido de Luna")).assert(hasText("Músico"))
            rule.onNodeWithContentDescription("Imagen de Rubén Blades Bellido de Luna", useUnmergedTree = true)
                .assertExists()
            EvidenciaE2E.capturar(rule, evidencia, "02_listado_artistas")
        }
    }

    /** FN03-ESC02: filtrar por nombre a medida que se escribe, sin distinguir mayúsculas ni tildes. */
    @Test
    fun elBuscadorFiltraPorNombreMientrasSeEscribe() {
        val evidencia = "FN03_ESC02"
        ActivityScenario.launch(MainActivity::class.java).use {
            abrirArtistasComoVisitante(evidencia)
            esperarListado()
            EvidenciaE2E.capturar(rule, evidencia, "02_listado_completo")

            // "e" está en los dos nombres; "ee" solo en Queen.
            buscar("e")
            contadorDice("2 artistas")
            buscar("e")
            contadorDice("1 artista")
            fila(queen).assertIsDisplayed()
            noAparece(ruben)
            EvidenciaE2E.capturar(rule, evidencia, "03_busqueda_ee")

            // Sin tilde y en mayúsculas también encuentra a "Rubén".
            borrarBusqueda()
            buscar("RUBEN")
            contadorDice("1 artista")
            fila(ruben).assertIsDisplayed()
            noAparece(queen)
            Espresso.closeSoftKeyboard()
            EvidenciaE2E.capturar(rule, evidencia, "04_busqueda_ruben")

            borrarBusqueda()
            contadorDice("2 artistas")
            EvidenciaE2E.capturar(rule, evidencia, "05_busqueda_borrada")
        }
    }

    /** FN03-ESC03: una búsqueda sin coincidencias muestra el mensaje y se puede corregir. */
    @Test
    fun unaBusquedaSinCoincidenciasMuestraElMensaje() {
        val evidencia = "FN03_ESC03"
        ActivityScenario.launch(MainActivity::class.java).use {
            abrirArtistasComoVisitante(evidencia)
            esperarListado()

            buscar("zzz")
            Espresso.closeSoftKeyboard()
            esperarA(TagVacioArtistas)
            rule.onNodeWithTag(TagVacioArtistas).assertTextEquals("No se encontraron artistas")
            rule.onNodeWithText("Prueba con otro nombre.").assertIsDisplayed()
            rule.onNodeWithTag(TagBuscadorArtistas).assertTextEquals("zzz")
            noAparece(queen)
            noAparece(ruben)
            EvidenciaE2E.capturar(rule, evidencia, "02_sin_coincidencias")

            borrarBusqueda()
            esperarListado()
            contadorDice("2 artistas")
            EvidenciaE2E.capturar(rule, evidencia, "03_listado_restaurado")
        }
    }

    /** FN03-ESC04: seleccionar un artista lleva a su detalle y "Atrás" vuelve con la búsqueda. */
    @Test
    fun seleccionarUnArtistaLlevaASuDetalleYAtrasVuelveAlListado() {
        val evidencia = "FN03_ESC04"
        ActivityScenario.launch(MainActivity::class.java).use {
            abrirArtistasComoVisitante(evidencia)
            esperarListado()

            buscar("queen")
            Espresso.closeSoftKeyboard()
            noAparece(ruben)
            EvidenciaE2E.capturar(rule, evidencia, "02_busqueda_queen")
            fila(queen).performClick()

            esperarA(TagDetalleArtista)
            esperarQueDesaparezca(TagArtistas)
            rule.onNodeWithText("Detalle del artista").assertIsDisplayed()
            rule.onNodeWithTag(TagNombreDetalleArtista).assertTextEquals("Queen")
            rule.onNodeWithTag(TagTipoDetalleArtista).assertTextEquals("Banda")
            EvidenciaE2E.capturar(rule, evidencia, "03_detalle_queen")

            rule.onNodeWithTag(TagAtras).performClick()
            esperarA(TagArtistas)
            esperarQueDesaparezca(TagDetalleArtista)
            rule.onNodeWithTag(TagBuscadorArtistas).assertTextEquals("queen")
            fila(queen).assertIsDisplayed()
            noAparece(ruben)
            EvidenciaE2E.capturar(rule, evidencia, "04_regreso_listado")
        }
    }

    /** Servidor local con músicos y bandas; [caido] hace que corte la conexión, como un backend apagado. */
    private fun iniciarServidor(musicos: String, bandas: String, caido: () -> Boolean): MockWebServer {
        val nuevo = MockWebServer().apply {
            dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse = when {
                    caido() -> MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START)
                    request.path == "/musicians" -> MockResponse().setBody(musicos)
                    request.path == "/bands" -> MockResponse().setBody(bandas)
                    else -> MockResponse().setResponseCode(404)
                }
            }
            start()
        }
        servidor = nuevo
        // "localhost" es uno de los hosts con HTTP permitido en network_security_config.xml.
        ApplicationProvider.getApplicationContext<VinilosApplication>().container =
            AppContainer(baseUrl = "http://localhost:${nuevo.port}/")
        return nuevo
    }

    /** FN03-ESC05: con el backend caído se ve el error y "Reintentar" recupera el listado. */
    @Test
    fun conElBackendCaidoSeVeElErrorYReintentarRecuperaElListado() {
        val evidencia = "FN03_ESC05"
        var caido = true
        iniciarServidor(
            musicos = """[{"id": 100, "name": "Rubén Blades Bellido de Luna", "image": null}]""",
            bandas = """[{"id": 101, "name": "Queen", "image": null}]""",
            caido = { caido }
        )

        ActivityScenario.launch(MainActivity::class.java).use {
            abrirArtistasComoVisitante(evidencia)

            esperarA(TagErrorArtistas)
            rule.onNodeWithText("No se pudo cargar el listado de artistas").assertIsDisplayed()
            rule.onNodeWithTag(TagReintentarArtistas).assertIsDisplayed()
            EvidenciaE2E.capturar(rule, evidencia, "02_backend_caido")

            // El backend "vuelve" y la persona pulsa Reintentar.
            caido = false
            rule.onNodeWithTag(TagReintentarArtistas).performClick()

            esperarListado()
            contadorDice("2 artistas")
            fila(queen).assertIsDisplayed()
            fila(ruben).assertIsDisplayed()
            EvidenciaE2E.capturar(rule, evidencia, "03_recuperado")
        }
    }

    /** FN03-ESC06: si no hay músicos ni bandas registrados se muestra el mensaje. */
    @Test
    fun sinArtistasRegistradosSeMuestraElMensaje() {
        val evidencia = "FN03_ESC06"
        iniciarServidor(musicos = "[]", bandas = "[]", caido = { false })

        ActivityScenario.launch(MainActivity::class.java).use {
            abrirArtistasComoVisitante(evidencia)

            esperarA(TagVacioArtistas)
            rule.onNodeWithTag(TagVacioArtistas).assertTextEquals("No se encontraron artistas")
            rule.onAllNodesWithTag(TagContadorArtistas).assertCountEquals(0)
            EvidenciaE2E.capturar(rule, evidencia, "02_sin_artistas")
        }
    }

    /** Repositorio real que cuenta las consultas, para comprobar que girar no vuelve a consultar. */
    private class ArtistasConContador : ArtistRepository(ServiceAdapter(BuildConfig.BASE_URL)) {
        @Volatile
        var consultas = 0

        override suspend fun obtenerArtistas(): Result<List<Artist>> {
            consultas++
            return super.obtenerArtistas()
        }
    }

    /** FN03-ESC07: al girar la pantalla se conservan el listado filtrado y la búsqueda. */
    @Test
    fun alGirarLaPantallaSeConservanLaBusquedaYElListadoSinVolverAConsultar() {
        val evidencia = "FN03_ESC07"
        val artistas = ArtistasConContador()
        ApplicationProvider.getApplicationContext<VinilosApplication>().container =
            AppContainer(artistasRepositorio = artistas)

        ActivityScenario.launch(MainActivity::class.java).use { escenario ->
            abrirArtistasComoVisitante(evidencia)
            esperarListado()

            buscar("rub")
            Espresso.closeSoftKeyboard()
            fila(ruben).assertIsDisplayed()
            noAparece(queen)
            EvidenciaE2E.capturar(rule, evidencia, "02_antes_de_girar")

            // Girar de verdad (no solo recrear la Activity) para que la evidencia se vea en horizontal.
            escenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
            rule.waitUntil(5_000) {
                var horizontal = false
                escenario.onActivity {
                    horizontal = it.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                }
                horizontal
            }
            esperarA(TagArtistas)
            rule.waitForIdle()

            rule.onNodeWithTag(TagBuscadorArtistas).assertTextEquals("rub")
            fila(ruben).assertIsDisplayed()
            noAparece(queen)
            contadorDice("1 artista")
            assertEquals(1, artistas.consultas)
            EvidenciaE2E.capturar(rule, evidencia, "03_despues_de_girar")
        }
    }
}
