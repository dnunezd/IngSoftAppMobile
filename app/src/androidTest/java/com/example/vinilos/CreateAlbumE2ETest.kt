package com.example.vinilos

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import com.example.vinilos.data.model.NewAlbum
import com.example.vinilos.data.remote.ServiceAdapter
import com.example.vinilos.data.repository.AlbumRepository
import com.example.vinilos.data.repository.AlbumRepositoryImpl
import com.example.vinilos.data.repository.CollectorRepositoryImpl
import com.example.vinilos.e2e.EvidenciaE2E
import com.example.vinilos.ui.catalogo.TagContadorCatalogo
import com.example.vinilos.ui.catalogo.TagCreateAlbumCatalogo
import com.example.vinilos.ui.catalogo.TagRolActivo
import com.example.vinilos.ui.catalogo.tagNewAlbum
import com.example.vinilos.ui.create.TagAvisoCampos
import com.example.vinilos.ui.create.TagCancelarCrear
import com.example.vinilos.ui.create.TagCreateAlbum
import com.example.vinilos.ui.create.TagDescripcionAlbum
import com.example.vinilos.ui.create.TagDisqueraAlbum
import com.example.vinilos.ui.create.TagErrorEnvio
import com.example.vinilos.ui.create.TagFechaAlbum
import com.example.vinilos.ui.create.TagFormularioCrear
import com.example.vinilos.ui.create.TagGeneroAlbum
import com.example.vinilos.ui.create.TagNombreAlbum
import com.example.vinilos.ui.create.TagAlbumCover
import com.example.vinilos.ui.create.tagFieldError
import com.example.vinilos.ui.ingreso.TagEntrarColeccionista
import com.example.vinilos.ui.ingreso.TagEntrarVisitante
import com.example.vinilos.ui.ingreso.tagColeccionista
import com.example.vinilos.ui.inicio.TagInicio
import com.example.vinilos.ui.inicio.TagVerAlbumes
import com.example.vinilos.viewmodel.AlbumField
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.mockwebserver.SocketPolicy
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * E2E for HU07 (create an album), with the scenarios from T07-5.
 *
 * FN07-ESC01 through ESC06 run against the real backend. Since they create data, they run against the local BackVynils
 * (`-PbaseUrl=http://10.0.2.2:3000/` on the emulator; on a phone, `http://localhost:3000/` with
 * `adb reverse tcp:3000 tcp:3000`). Every album created is deleted in [borrarAlbumesCreados] with
 * `DELETE /albums/{id}`, even if the test fails. The collector is picked from the real data.
 *
 * FN07-ESC07 (backend down and retry) uses `MockWebServer`, like FN01-ESC03 and FN03-ESC05.
 *
 * Each scenario saves a screenshot per step, an `informe.md`, and a PDF in
 * `.../files/e2e-evidencia/FN07_ESCxx/` (on the device; the PDFs are versioned by hand in
 * `docs/pruebas/sprint-2/HU07/e2e/`).
 */
class CreateAlbumE2ETest {

    @get:Rule
    val rule = createEmptyComposeRule()

    /** The app's real repository, which also records how many times a create happened and what ids it returned. */
    private class AlbumesReales(
        private val real: AlbumRepository = AlbumRepositoryImpl(ServiceAdapter(BuildConfig.BASE_URL))
    ) : AlbumRepository by real {
        @Volatile var envios = 0
        val creados = mutableListOf<Int>()

        override suspend fun createAlbum(album: NewAlbum): Result<Int> {
            envios++
            return real.createAlbum(album).also { resultado ->
                resultado.onSuccess { id -> synchronized(creados) { creados += id } }
            }
        }
    }

    private val albumes = AlbumesReales()

    private var servidor: MockWebServer? = null

    @Before
    fun usarElBackendReal() {
        ApplicationProvider.getApplicationContext<VinilosApplication>().container =
            AppContainer(albumesRepositorio = albumes)
    }

    @After
    fun borrarAlbumesCreados() {
        servidor?.shutdown()
        val http = OkHttpClient()
        synchronized(albumes.creados) { albumes.creados.toList() }.forEach { id ->
            http.newCall(Request.Builder().url("${BuildConfig.BASE_URL}albums/$id").delete().build())
                .execute()
                .close()
        }
    }

    /** Valid form data; the name carries the timestamp so it isn't confused with another album. */
    private data class Formulario(
        val nombre: String = "E2E FN07 ${System.currentTimeMillis()}",
        val portada: String = "https://upload.wikimedia.org/wikipedia/en/4/42/Beatles_-_Abbey_Road.jpg",
        val genero: String = "Rock",
        val disquera: String = "EMI",
        val descripcion: String = "Álbum creado por la prueba E2E de la HU07."
    )

    // ---------- Date ----------

    /** Day 1 of the current month (based on the local day), which the picker always allows choosing. */
    private val primerDiaDelMes: Calendar = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }

    private val fechaVisibleEsperada = String.format(
        Locale.ROOT, "01/%02d/%d", primerDiaDelMes.get(Calendar.MONTH) + 1, primerDiaDelMes.get(Calendar.YEAR)
    )

    private val fechaIsoEsperada = String.format(
        Locale.ROOT, "%d-%02d-01T00:00:00.000Z", primerDiaDelMes.get(Calendar.YEAR), primerDiaDelMes.get(Calendar.MONTH) + 1
    )

    // ---------- Navigation ----------

    private fun esperarA(tag: String, milis: Long = 15_000) =
        rule.waitUntil(milis) { rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }

    /** The NEW badge is inside the card (which merges its content): it is searched for without merging. */
    private fun esperarInsigniaNuevo(id: Int, milis: Long = 15_000) = rule.waitUntil(milis) {
        rule.onAllNodesWithTag(tagNewAlbum(id), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    }

    private fun esperarTexto(texto: String, milis: Long = 15_000) =
        rule.waitUntil(milis) { rule.onAllNodesWithText(texto).fetchSemanticsNodes().isNotEmpty() }

    private fun esperarQueSeCierreElFormulario() =
        rule.waitUntil(15_000) { rule.onAllNodesWithTag(TagFormularioCrear).fetchSemanticsNodes().isEmpty() }

    /** Home -> "Ver álbumes" -> catalog loaded. */
    private fun verCatalogo(evidencia: String, paso: String) {
        esperarA(TagInicio)
        rule.onNodeWithTag(TagVerAlbumes).performScrollTo().performClick()
        esperarA(TagContadorCatalogo)
        EvidenciaE2E.capturar(rule, evidencia, paso)
    }

    /** Login -> Collector [idColeccionista] -> catalog. */
    private fun entrarComoColeccionista(idColeccionista: Int, evidencia: String) {
        esperarA(TagEntrarColeccionista)
        rule.onNodeWithTag(TagEntrarColeccionista).performScrollTo().performClick()
        esperarA(tagColeccionista(idColeccionista))
        rule.onNodeWithTag(tagColeccionista(idColeccionista)).performClick()
        verCatalogo(evidencia, "01_catalogo_coleccionista")
        rule.onNodeWithTag(TagRolActivo).assertTextEquals("COLECCIONISTA")
    }

    private fun primerColeccionistaReal(): Int = runBlocking {
        CollectorRepositoryImpl(ServiceAdapter(BuildConfig.BASE_URL)).getCollectors().getOrThrow().first().id
    }

    private fun abrirFormulario(evidencia: String) {
        rule.onNodeWithTag(TagCreateAlbumCatalogo).performClick()
        esperarA(TagFormularioCrear)
        EvidenciaE2E.capturar(rule, evidencia, "02_formulario_vacio")
    }

    // ---------- Form ----------

    private fun escribir(tag: String, texto: String) {
        rule.onNodeWithTag(tag).performScrollTo().performTextInput(texto)
        // On API 23, the open keyboard can cover the next field or button and the tap gets lost.
        Espresso.closeSoftKeyboard()
    }

    private fun elegirOpcion(tag: String, opcion: String) {
        rule.onNodeWithTag(tag).performScrollTo().performClick()
        esperarTexto(opcion)
        rule.onNodeWithText(opcion).performClick()
        rule.waitForIdle()
    }

    /** Opens the picker, taps day 1 of the shown month (the current one) and presses "Aceptar". */
    private fun elegirPrimerDiaDelMes() {
        rule.onNodeWithTag(TagFechaAlbum).performScrollTo().performClick()
        esperarTexto("Aceptar")
        // Each day is exposed with its full date ("domingo, 1 de noviembre de 2026"), not just the number.
        rule.onNode(hasText(", 1 de ", substring = true) and hasClickAction()).performClick()
        rule.onNodeWithText("Aceptar").performClick()
        rule.waitForIdle()
    }

    private fun llenar(formulario: Formulario, portada: String = formulario.portada) {
        escribir(TagNombreAlbum, formulario.nombre)
        escribir(TagAlbumCover, portada)
        elegirPrimerDiaDelMes()
        elegirOpcion(TagGeneroAlbum, formulario.genero)
        elegirOpcion(TagDisqueraAlbum, formulario.disquera)
        escribir(TagDescripcionAlbum, formulario.descripcion)
    }

    /** A selection field (date, genre, or label) shows [valor]. */
    private fun seleccionMuestra(tag: String, valor: String) {
        val campo = hasTestTag(tag) and hasAnyDescendant(hasText(valor))
        rule.onNode(campo, useUnmergedTree = true).performScrollTo().assertExists()
    }

    /** All fields keep what was typed or chosen. */
    private fun formularioConserva(formulario: Formulario) {
        rule.onNodeWithTag(TagNombreAlbum).performScrollTo().assertTextEquals(formulario.nombre)
        rule.onNodeWithTag(TagAlbumCover).performScrollTo().assertTextEquals(formulario.portada)
        seleccionMuestra(TagFechaAlbum, fechaVisibleEsperada)
        seleccionMuestra(TagGeneroAlbum, formulario.genero)
        seleccionMuestra(TagDisqueraAlbum, formulario.disquera)
        rule.onNodeWithTag(TagDescripcionAlbum).performScrollTo().assertTextEquals(formulario.descripcion)
    }

    /** The field shows [mensaje] below it. */
    private fun campoMarcado(campo: AlbumField, mensaje: String) {
        val error = hasTestTag(tagFieldError(campo)) and hasAnyDescendant(hasText(mensaje))
        rule.onNode(error, useUnmergedTree = true).performScrollTo().assertIsDisplayed()
    }

    private fun pulsar(tag: String) {
        rule.onNodeWithTag(tag).performScrollTo().performClick()
        rule.waitForIdle()
    }

    private fun desplazarA(matcher: SemanticsMatcher) {
        rule.onNode(matcher, useUnmergedTree = true).performScrollTo()
    }

    private fun albumesEnElBackend(): Int = runBlocking {
        AlbumRepositoryImpl(ServiceAdapter(BuildConfig.BASE_URL)).getAlbums().getOrThrow().size
    }

    private fun albumDelBackend(id: Int): JSONObject {
        val respuesta = OkHttpClient().newCall(Request.Builder().url("${BuildConfig.BASE_URL}albums/$id").build()).execute()
        return respuesta.use { JSONObject(requireNotNull(it.body).string()) }
    }

    // ---------- Scenarios against the real backend ----------

    /** FN07-ESC01: the Visitor does not see "Crear álbum"; the Collector does. */
    @Test
    fun elVisitanteNoVeLaOpcionDeCrearYElColeccionistaSi() {
        val evidencia = "FN07_ESC01"
        val coleccionista = primerColeccionistaReal()

        ActivityScenario.launch(MainActivity::class.java).use {
            esperarA(TagEntrarVisitante)
            rule.onNodeWithTag(TagEntrarVisitante).performScrollTo().performClick()
            verCatalogo(evidencia, "01_catalogo_visitante")
            rule.onNodeWithTag(TagRolActivo).assertTextEquals("VISITANTE")
            rule.onAllNodesWithTag(TagCreateAlbumCatalogo).assertCountEquals(0)

            // Go back to login (catalog -> home -> login) with the back button and enter as a Collector.
            Espresso.pressBack()
            esperarA(TagInicio)
            rule.waitForIdle()
            Espresso.pressBack()
            esperarA(TagEntrarColeccionista)
            rule.onNodeWithTag(TagEntrarColeccionista).performScrollTo().performClick()
            esperarA(tagColeccionista(coleccionista))
            rule.onNodeWithTag(tagColeccionista(coleccionista)).performClick()
            verCatalogo(evidencia, "02_catalogo_coleccionista")
            rule.onNodeWithTag(TagCreateAlbumCatalogo).assertIsDisplayed()
        }
    }

    /** FN07-ESC02: a valid album is saved, "Álbum creado" appears, and the album shows up in the catalog as NEW. */
    @Test
    fun createUnAlbumValidoLoGuardaYLoMuestraEnElCatalogo() {
        val evidencia = "FN07_ESC02"
        val formulario = Formulario()
        val antes = albumesEnElBackend()

        ActivityScenario.launch(MainActivity::class.java).use {
            entrarComoColeccionista(primerColeccionistaReal(), evidencia)
            abrirFormulario(evidencia)
            llenar(formulario)
            seleccionMuestra(TagFechaAlbum, fechaVisibleEsperada)
            EvidenciaE2E.capturar(rule, evidencia, "03_formulario_lleno")

            pulsar(TagCreateAlbum)
            esperarQueSeCierreElFormulario()
            esperarTexto("Álbum creado", milis = 10_000)
            EvidenciaE2E.capturar(rule, evidencia, "04_album_creado")

            val id = synchronized(albumes.creados) { albumes.creados.single() }
            esperarInsigniaNuevo(id)
            rule.onNodeWithTag(tagNewAlbum(id), useUnmergedTree = true).assertIsDisplayed()
            rule.onAllNodesWithText(formulario.nombre)[0].assertIsDisplayed()
            EvidenciaE2E.capturar(rule, evidencia, "05_album_nuevo_en_el_catalogo")

            // The backend saved the album exactly once, with the chosen day (not shifted by timezone).
            assertEquals(1, albumes.envios)
            assertEquals(antes + 1, albumesEnElBackend())
            val guardado = albumDelBackend(id)
            assertEquals(formulario.nombre, guardado.getString("name"))
            assertEquals(fechaIsoEsperada, guardado.getString("releaseDate"))
            assertEquals(formulario.genero, guardado.getString("genre"))
            assertEquals(formulario.disquera, guardado.getString("recordLabel"))
        }
    }

    /** FN07-ESC03: "Cancelar" with the form filled in closes it without saving or sending anything. */
    @Test
    fun cancelarUnFormularioConDatosNoGuardaNada() {
        val evidencia = "FN07_ESC03"
        val formulario = Formulario()
        val antes = albumesEnElBackend()

        ActivityScenario.launch(MainActivity::class.java).use {
            entrarComoColeccionista(primerColeccionistaReal(), evidencia)
            abrirFormulario(evidencia)
            llenar(formulario)
            EvidenciaE2E.capturar(rule, evidencia, "03_formulario_lleno")

            pulsar(TagCancelarCrear)
            esperarQueSeCierreElFormulario()
            esperarA(TagContadorCatalogo)
            rule.onAllNodesWithText(formulario.nombre).assertCountEquals(0)
            EvidenciaE2E.capturar(rule, evidencia, "04_catalogo_sin_cambios")

            assertEquals(0, albumes.envios)
            assertEquals(antes, albumesEnElBackend())
        }
    }

    /** FN07-ESC04: with an empty form, every field is marked "Campo obligatorio" and nothing is sent. */
    @Test
    fun conCamposEmptysSeMarcaCadaUnoYNoSeEnviaNada() {
        val evidencia = "FN07_ESC04"
        val antes = albumesEnElBackend()

        ActivityScenario.launch(MainActivity::class.java).use {
            entrarComoColeccionista(primerColeccionistaReal(), evidencia)
            abrirFormulario(evidencia)

            pulsar(TagCreateAlbum)
            esperarA(TagAvisoCampos)
            desplazarA(hasTestTag(TagAvisoCampos))
            rule.onNodeWithText("Atención requerida").assertIsDisplayed()
            EvidenciaE2E.capturar(rule, evidencia, "03_aviso_y_primeros_campos")

            AlbumField.entries.forEach { campoMarcado(it, "Campo obligatorio") }
            EvidenciaE2E.capturar(rule, evidencia, "04_ultimos_campos_marcados")

            rule.onNodeWithTag(TagFormularioCrear).assertExists()
            assertEquals(0, albumes.envios)
            assertEquals(antes, albumesEnElBackend())
        }
    }

    /** FN07-ESC05: a cover that doesn't start with http:// or https:// shows "Ingresa una URL válida". */
    @Test
    fun unaPortadaInvalidaMuestraElMensajeYNoSeEnviaNada() {
        val evidencia = "FN07_ESC05"
        val formulario = Formulario()
        val antes = albumesEnElBackend()

        ActivityScenario.launch(MainActivity::class.java).use {
            entrarComoColeccionista(primerColeccionistaReal(), evidencia)
            abrirFormulario(evidencia)
            llenar(formulario, portada = "ftp://vinilos.example/portada.jpg")

            pulsar(TagCreateAlbum)
            campoMarcado(AlbumField.Portada, "Ingresa una URL válida")
            EvidenciaE2E.capturar(rule, evidencia, "03_url_invalida")

            // Only the cover ends up marked: the other fields are valid.
            AlbumField.entries.filter { it != AlbumField.Portada }.forEach {
                rule.onAllNodesWithTag(tagFieldError(it), useUnmergedTree = true).assertCountEquals(0)
            }
            assertEquals(0, albumes.envios)
            assertEquals(antes, albumesEnElBackend())
        }
    }

    /** FN07-ESC06: when rotating the screen, the filled form keeps everything typed and chosen. */
    @Test
    fun alGirarLaPantallaElFormularioConservaLoEscritoYElegido() {
        val evidencia = "FN07_ESC06"
        val formulario = Formulario()

        ActivityScenario.launch(MainActivity::class.java).use { escenario ->
            entrarComoColeccionista(primerColeccionistaReal(), evidencia)
            abrirFormulario(evidencia)
            llenar(formulario)
            EvidenciaE2E.capturar(rule, evidencia, "03_antes_de_girar")

            // Actually rotate (not just recreate the Activity) so the evidence shows up in landscape.
            escenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
            rule.waitUntil(5_000) {
                var horizontal = false
                escenario.onActivity {
                    horizontal = it.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                }
                horizontal
            }
            esperarA(TagFormularioCrear)
            rule.waitForIdle()

            formularioConserva(formulario)
            rule.onNodeWithTag(TagNombreAlbum).performScrollTo()
            EvidenciaE2E.capturar(rule, evidencia, "04_despues_de_girar")
            assertEquals(0, albumes.envios)
        }
    }

    // ---------- Scenario with MockWebServer ----------

    /** FN07-ESC07: with the backend down, the error is shown, the data is kept, and retrying creates the album. */
    @Test
    fun conElBackendCaidoSeConservanLosDatosYReintentarCreaElAlbum() {
        val evidencia = "FN07_ESC07"
        val formulario = Formulario()
        val idCreado = 500
        var caido = true
        var creado = false
        var creaciones = 0
        val existente = """{"id": 100, "name": "Buscando América", "cover": null, "genre": "Salsa",
            "releaseDate": "1984-08-01T00:00:00.000Z", "performers": []}"""
        val nuevo = MockWebServer().apply {
            dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse = when {
                    request.path == "/collectors" -> MockResponse().setBody(
                        """[{"id": 100, "name": "Manolo Bellon", "email": "manollo@caracol.com.co"}]"""
                    )
                    request.path == "/albums" && request.method == "GET" -> MockResponse().setBody(
                        if (creado) {
                            """[$existente, {"id": $idCreado, "name": "${formulario.nombre}", "cover": null,
                                "genre": "${formulario.genero}", "releaseDate": "$fechaIsoEsperada", "performers": []}]"""
                        } else {
                            "[$existente]"
                        }
                    )
                    request.path == "/albums" && request.method == "POST" ->
                        if (caido) {
                            MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START)
                        } else {
                            creaciones++
                            creado = true
                            MockResponse().setResponseCode(201).setBody("""{"id": $idCreado, "name": "${formulario.nombre}"}""")
                        }
                    else -> MockResponse().setResponseCode(404)
                }
            }
            start()
        }
        servidor = nuevo
        // "localhost" is one of the hosts allowed over HTTP in network_security_config.xml.
        ApplicationProvider.getApplicationContext<VinilosApplication>().container =
            AppContainer(baseUrl = "http://localhost:${nuevo.port}/")

        ActivityScenario.launch(MainActivity::class.java).use {
            entrarComoColeccionista(100, evidencia)
            abrirFormulario(evidencia)
            llenar(formulario)

            pulsar(TagCreateAlbum)
            esperarA(TagErrorEnvio)
            desplazarA(hasTestTag(TagErrorEnvio))
            rule.onNodeWithText("No se pudo crear el álbum, intenta de nuevo").assertIsDisplayed()
            rule.onNodeWithTag(TagCreateAlbum).performScrollTo()
            rule.onNodeWithText("Reintentar creación").assertIsDisplayed()
            EvidenciaE2E.capturar(rule, evidencia, "03_error_al_crear")

            formularioConserva(formulario)
            EvidenciaE2E.capturar(rule, evidencia, "04_datos_conservados")

            // The backend "comes back" and the user retries.
            caido = false
            pulsar(TagCreateAlbum)
            esperarQueSeCierreElFormulario()
            esperarTexto("Álbum creado", milis = 10_000)
            esperarInsigniaNuevo(idCreado)
            EvidenciaE2E.capturar(rule, evidencia, "05_album_creado_al_reintentar")
            assertEquals(1, creaciones)
        }
    }
}
