package com.example.vinilos

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
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
import com.example.vinilos.data.repository.ArtistRepositoryImpl
import com.example.vinilos.e2e.EvidenciaE2E
import com.example.vinilos.ui.artistas.TagArtistas
import com.example.vinilos.ui.artistas.TagBuscadorArtistas
import com.example.vinilos.ui.artistas.TagContadorArtistas
import com.example.vinilos.ui.artistas.TagArtistDetail
import com.example.vinilos.ui.artistas.TagErrorArtistas
import com.example.vinilos.ui.artistas.TagListaArtistas
import com.example.vinilos.ui.artistas.TagNombreArtistDetail
import com.example.vinilos.ui.artistas.TagReintentarArtistas
import com.example.vinilos.ui.artistas.TagTipoArtistDetail
import com.example.vinilos.ui.artistas.TagEmptyArtistas
import com.example.vinilos.ui.artistas.tagArtista
import com.example.vinilos.ui.ingreso.TagAtras
import com.example.vinilos.ui.ingreso.TagEntrarVisitante
import com.example.vinilos.ui.inicio.TagInicio
import com.example.vinilos.ui.inicio.TagVerArtistas
import java.text.Normalizer
import java.util.Locale
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * E2E for HU03 (artist listing) as Visitor.
 *
 * FN03-ESC01 through ESC04 and ESC07 run against the real BackVynils backend, but don't assume a fixed
 * dataset (like the 2 sample artists from `sql/data.sql`): they wrap the real repository to capture the
 * listing the backend actually returns and compute the search terms and assertions from that data. This
 * way the tests are consistent both with the local backend (Docker) and with the one deployed by the
 * tutors, regardless of how many artists each has. On the emulator, the app reaches the backend via
 * `10.0.2.2`; on a phone, with `adb reverse tcp:3000 tcp:3000` and installing the app with
 * `-PbaseUrl=http://localhost:3000/`.
 *
 * FN03-ESC05 (backend down) and ESC06 (no artists) can't be triggered against BackVynils without
 * stopping it or deleting data, so they still use `MockWebServer` with fabricated data; they don't
 * depend on the real dataset and didn't need to change.
 *
 * Each scenario saves a screenshot per step, `informe.md` and a PDF in
 * `.../files/e2e-evidencia/FN03_ESCxx/` (only on the device, not in the repo).
 */
class ArtistasE2ETest {

    @get:Rule
    val rule = createEmptyComposeRule()

    /** The app's real repository: counts the queries and stores the last listing received. */
    private class ArtistasReales(
        private val real: ArtistRepository = ArtistRepositoryImpl(ServiceAdapter(BuildConfig.BASE_URL))
    ) : ArtistRepository by real {
        @Volatile var consultas = 0
        @Volatile var lista: List<Artist>? = null

        override suspend fun getArtists(): Result<List<Artist>> {
            consultas++
            return real.getArtists().also { resultado -> resultado.onSuccess { lista = it } }
        }
    }

    private val artistas = ArtistasReales()

    private var servidor: MockWebServer? = null

    @Before
    fun usarElBackendReal() {
        ApplicationProvider.getApplicationContext<VinilosApplication>().container =
            AppContainer(artistasRepositorio = artistas)
    }

    @After
    fun detenerServidor() {
        servidor?.shutdown()
    }

    private fun esperarA(tag: String, milis: Long = 15_000) =
        rule.waitUntil(milis) { rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }

    private fun esperarQueDesaparezca(tag: String, milis: Long = 5_000) =
        rule.waitUntil(milis) { rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isEmpty() }

    /** Visitor -> home -> "Ver artistas" (without waiting for data; also works with MockWebServer). */
    private fun abrirArtistasComoVisitante(evidencia: String) {
        esperarA(TagEntrarVisitante)
        rule.onNodeWithTag(TagEntrarVisitante).performScrollTo().performClick()
        esperarA(TagInicio)
        EvidenciaE2E.capturar(rule, evidencia, "01_inicio_visitante")
        rule.onNodeWithTag(TagVerArtistas).performScrollTo().performClick()
        esperarA(TagArtistas)
    }

    private fun esperarElListadoReal(): List<Artist> {
        rule.waitUntil(15_000) { artistas.lista != null }
        return requireNotNull(artistas.lista)
    }

    /** For scenarios against the real backend: opens artists and returns the captured real listing. */
    private fun verArtistasReales(evidencia: String): List<Artist> {
        abrirArtistasComoVisitante(evidencia)
        val lista = esperarElListadoReal()
        esperarA(tagArtista(lista.first()))
        return lista
    }

    /** Scrolls the list to the row (on small screens it may not be composed yet). */
    private fun fila(artista: Artist): SemanticsNodeInteraction {
        rule.onNodeWithTag(TagListaArtistas).performScrollToNode(hasTestTag(tagArtista(artista)))
        return rule.onNodeWithTag(tagArtista(artista))
    }

    private fun search(texto: String) {
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

    private fun textoContador(cantidad: Int) = if (cantidad == 1) "1 artista" else "$cantidad artistas"

    private fun noAparece(artista: Artist) =
        rule.onAllNodesWithTag(tagArtista(artista)).assertCountEquals(0)

    /** Same as production's search normalization: without accents and in lowercase. */
    private val marcasDiacriticas = Regex("\\p{InCombiningDiacriticalMarks}+")
    private fun normalizado(texto: String) =
        Normalizer.normalize(texto, Normalizer.Form.NFD).replace(marcasDiacriticas, "").lowercase(Locale.ROOT)

    /** The shortest substring of [objetivo]'s name that doesn't appear in any other artist in [lista]. */
    private fun substringDiscriminante(objetivo: Artist, lista: List<Artist>): String {
        val nombre = normalizado(objetivo.name)
        val otros = lista.filterNot { it.id == objetivo.id && it.type == objetivo.type }
            .map { normalizado(it.name) }
        for (longitud in 2..nombre.length) {
            for (inicio in 0..(nombre.length - longitud)) {
                val candidato = nombre.substring(inicio, inicio + longitud)
                if (otros.none { candidato in it }) return candidato
            }
        }
        return nombre
    }

    /** A term that doesn't match (even as a substring) any real artist in [lista]. */
    private fun terminoSinCoincidencias(lista: List<Artist>): String {
        val normalizados = lista.map { normalizado(it.name) }
        var sufijo = 0
        var candidato = "zzzqx"
        while (normalizados.any { candidato in it }) {
            sufijo++
            candidato = "zzzqx$sufijo"
        }
        return candidato
    }

    private fun etiquetaDe(artista: Artist) = if (artista.type == ArtistType.BANDA) "Banda" else "Músico"

    /**
     * Artists whose name is not repeated in any other in [lista]. The real backend can list the same
     * name as both a musician and a band (they share identity by name, not by id): searching by name
     * can't distinguish those cases, so the search scenarios only use artists with a unique name.
     */
    private fun conNombreUnico(lista: List<Artist>): List<Artist> {
        val conteoPorNombre = lista.groupingBy { normalizado(it.name) }.eachCount()
        return lista.filter { conteoPorNombre.getValue(normalizado(it.name)) == 1 }
    }

    /** FN03-ESC01: see the real artist listing from the main menu. */
    @Test
    fun elVisitanteVeMusicosYBandasEnOrdenConImagenNombreYEtiqueta() {
        val evidencia = "FN03_ESC01"
        ActivityScenario.launch(MainActivity::class.java).use {
            val lista = verArtistasReales(evidencia)

            rule.onNodeWithTag(TagBuscadorArtistas).assertIsDisplayed()
            contadorDice(textoContador(lista.size))

            // The first two in the real listing (already sorted alphabetically) must be seen in that order.
            if (lista.size >= 2) {
                val primeros = lista.take(2)
                rule.onNodeWithTag(TagListaArtistas).performScrollToNode(hasTestTag(tagArtista(primeros[0])))
                val filas = rule.onAllNodes(hasTestTag(tagArtista(primeros[0])) or hasTestTag(tagArtista(primeros[1])))
                filas.assertCountEquals(2)
                filas[0].assert(hasTestTag(tagArtista(primeros[0])))
                filas[1].assert(hasTestTag(tagArtista(primeros[1])))
            }

            lista.forEach { artista ->
                // Anchored by that row's tag (type+id), not by name/description: the real backend can
                // repeat the same name for a musician and a band (they share identity by name).
                val filaDelArtista = hasTestTag(tagArtista(artista))
                fila(artista).assert(hasText(artista.name)).assert(hasText(etiquetaDe(artista)))
                rule.onNode(filaDelArtista and hasContentDescription("Imagen de ${artista.name}")).assertExists()
            }
            EvidenciaE2E.capturar(rule, evidencia, "02_listado_artistas")
        }
    }

    /** FN03-ESC02: filter by name as it's typed, case- and accent-insensitive. */
    @Test
    fun elBuscadorFiltraPorNombreMientrasSeEscribe() {
        val evidencia = "FN03_ESC02"
        ActivityScenario.launch(MainActivity::class.java).use {
            val lista = verArtistasReales(evidencia)
            EvidenciaE2E.capturar(rule, evidencia, "02_listado_completo")

            // Only artists with a unique name: the real backend can repeat the same name for a
            // musician and a band, and there the search by name can't tell which one is meant.
            val candidatos = conNombreUnico(lista)
            Assume.assumeTrue(
                "El backend conectado no tiene artistas con nombre único para probar la búsqueda",
                candidatos.isNotEmpty()
            )

            val primerObjetivo = candidatos.first()
            val terminoUno = substringDiscriminante(primerObjetivo, lista)
            search(terminoUno)
            contadorDice(textoContador(1))
            fila(primerObjetivo).assertIsDisplayed()
            (lista - primerObjetivo).forEach { noAparece(it) }
            EvidenciaE2E.capturar(rule, evidencia, "03_busqueda_discriminante")
            borrarBusqueda()

            // In uppercase it must also find another real, unique artist, accent-insensitively.
            if (candidatos.size > 1) {
                val segundoObjetivo = candidatos[1]
                val terminoDos = substringDiscriminante(segundoObjetivo, lista)
                search(terminoDos.uppercase(Locale.ROOT))
                contadorDice(textoContador(1))
                fila(segundoObjetivo).assertIsDisplayed()
                (lista - segundoObjetivo).forEach { noAparece(it) }
                Espresso.closeSoftKeyboard()
                EvidenciaE2E.capturar(rule, evidencia, "04_busqueda_mayusculas")
                borrarBusqueda()
            }

            contadorDice(textoContador(lista.size))
            EvidenciaE2E.capturar(rule, evidencia, "05_busqueda_borrada")
        }
    }

    /** FN03-ESC03: a search with no matches shows the message and can be corrected. */
    @Test
    fun unaBusquedaSinCoincidenciasMuestraElMensaje() {
        val evidencia = "FN03_ESC03"
        ActivityScenario.launch(MainActivity::class.java).use {
            val lista = verArtistasReales(evidencia)
            val termino = terminoSinCoincidencias(lista)

            search(termino)
            Espresso.closeSoftKeyboard()
            esperarA(TagEmptyArtistas)
            rule.onNodeWithTag(TagEmptyArtistas).assertTextEquals("No se encontraron artistas")
            rule.onNodeWithText("Prueba con otro nombre.").assertIsDisplayed()
            rule.onNodeWithTag(TagBuscadorArtistas).assertTextEquals(termino)
            lista.forEach { noAparece(it) }
            EvidenciaE2E.capturar(rule, evidencia, "02_sin_coincidencias")

            borrarBusqueda()
            esperarA(tagArtista(lista.first()))
            contadorDice(textoContador(lista.size))
            EvidenciaE2E.capturar(rule, evidencia, "03_listado_restaurado")
        }
    }

    /** FN03-ESC04: selecting a real artist leads to their detail, and "Atrás" returns with the search. */
    @Test
    fun selectUnArtistaLlevaASuDetalleYAtrasVuelveAlListado() {
        val evidencia = "FN03_ESC04"
        ActivityScenario.launch(MainActivity::class.java).use {
            val lista = verArtistasReales(evidencia)
            val candidatos = conNombreUnico(lista)
            Assume.assumeTrue(
                "El backend conectado no tiene artistas con nombre único para probar la búsqueda",
                candidatos.isNotEmpty()
            )
            val objetivo = candidatos.first()
            val termino = substringDiscriminante(objetivo, lista)

            search(termino)
            Espresso.closeSoftKeyboard()
            (lista - objetivo).forEach { noAparece(it) }
            EvidenciaE2E.capturar(rule, evidencia, "02_busqueda_objetivo")
            fila(objetivo).performClick()

            esperarA(TagArtistDetail)
            esperarQueDesaparezca(TagArtistas)
            // The name no longer travels in the route: it appears when the detail query to the backend finishes.
            esperarA(TagNombreArtistDetail)
            rule.onNodeWithTag(TagNombreArtistDetail).assertTextEquals(objetivo.name)
            rule.onNodeWithTag(TagTipoArtistDetail).assertTextEquals(etiquetaDe(objetivo))
            EvidenciaE2E.capturar(rule, evidencia, "03_detalle_objetivo")

            rule.onNodeWithTag(TagAtras).performClick()
            esperarA(TagArtistas)
            esperarQueDesaparezca(TagArtistDetail)
            rule.onNodeWithTag(TagBuscadorArtistas).assertTextEquals(termino)
            fila(objetivo).assertIsDisplayed()
            (lista - objetivo).forEach { noAparece(it) }
            EvidenciaE2E.capturar(rule, evidencia, "04_regreso_listado")
        }
    }

    /** Local server with musicians and bands; [caido] makes it cut the connection, like a backend that's down. */
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
        // "localhost" is one of the hosts allowed over HTTP in network_security_config.xml.
        ApplicationProvider.getApplicationContext<VinilosApplication>().container =
            AppContainer(baseUrl = "http://localhost:${nuevo.port}/")
        return nuevo
    }

    /** FN03-ESC05: with the backend down, the error is shown and "Reintentar" recovers the listing. */
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

            // The backend "comes back" and the person taps Reintentar.
            caido = false
            rule.onNodeWithTag(TagReintentarArtistas).performClick()

            esperarA(TagContadorArtistas)
            contadorDice("2 artistas")
            EvidenciaE2E.capturar(rule, evidencia, "03_recuperado")
        }
    }

    /** FN03-ESC06: if there are no musicians or bands registered, the message is shown. */
    @Test
    fun sinArtistasRegistradosSeMuestraElMensaje() {
        val evidencia = "FN03_ESC06"
        iniciarServidor(musicos = "[]", bandas = "[]", caido = { false })

        ActivityScenario.launch(MainActivity::class.java).use {
            abrirArtistasComoVisitante(evidencia)

            esperarA(TagEmptyArtistas)
            rule.onNodeWithTag(TagEmptyArtistas).assertTextEquals("No se encontraron artistas")
            rule.onAllNodesWithTag(TagContadorArtistas).assertCountEquals(0)
            EvidenciaE2E.capturar(rule, evidencia, "02_sin_artistas")
        }
    }

    /** FN03-ESC07: rotating the screen preserves the search and the real listing, without re-querying. */
    @Test
    fun alGirarLaPantallaSeConservanLaBusquedaYElListadoSinVolverAConsultar() {
        val evidencia = "FN03_ESC07"
        ActivityScenario.launch(MainActivity::class.java).use { escenario ->
            val lista = verArtistasReales(evidencia)
            val candidatos = conNombreUnico(lista)
            Assume.assumeTrue(
                "El backend conectado no tiene artistas con nombre único para probar la búsqueda",
                candidatos.isNotEmpty()
            )
            val objetivo = candidatos.last()
            val termino = substringDiscriminante(objetivo, lista)

            search(termino)
            Espresso.closeSoftKeyboard()
            fila(objetivo).assertIsDisplayed()
            (lista - objetivo).forEach { noAparece(it) }
            EvidenciaE2E.capturar(rule, evidencia, "02_antes_de_girar")
            // On API 23, if the search field stays focused while rotating, the keyboard reconnects and
            // resends the text the test typed ("ru" -> "ruru"). Focus is cleared before rotating.
            escenario.onActivity { it.currentFocus?.clearFocus() }
            rule.waitForIdle()

            // Actually rotate (not just recreate the Activity) so the evidence shows the landscape view.
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

            rule.onNodeWithTag(TagBuscadorArtistas).assertTextEquals(termino)
            fila(objetivo).assertIsDisplayed()
            (lista - objetivo).forEach { noAparece(it) }
            contadorDice(textoContador(1))
            assertEquals(1, artistas.consultas)
            EvidenciaE2E.capturar(rule, evidencia, "03_despues_de_girar")
        }
    }
}
