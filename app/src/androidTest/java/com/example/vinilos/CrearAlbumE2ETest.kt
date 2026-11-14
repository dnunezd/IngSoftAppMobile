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
import com.example.vinilos.data.model.AlbumNuevo
import com.example.vinilos.data.remote.ServiceAdapter
import com.example.vinilos.data.repository.AlbumRepository
import com.example.vinilos.data.repository.AlbumRepositoryImpl
import com.example.vinilos.data.repository.CollectorRepository
import com.example.vinilos.data.repository.CollectorRepositoryImpl
import com.example.vinilos.e2e.EvidenciaE2E
import com.example.vinilos.ui.catalogo.TagContadorCatalogo
import com.example.vinilos.ui.catalogo.TagCrearAlbumCatalogo
import com.example.vinilos.ui.catalogo.TagRolActivo
import com.example.vinilos.ui.catalogo.tagAlbumNuevo
import com.example.vinilos.ui.crear.TagAvisoCampos
import com.example.vinilos.ui.crear.TagCancelarCrear
import com.example.vinilos.ui.crear.TagCrearAlbum
import com.example.vinilos.ui.crear.TagDescripcionAlbum
import com.example.vinilos.ui.crear.TagDisqueraAlbum
import com.example.vinilos.ui.crear.TagErrorEnvio
import com.example.vinilos.ui.crear.TagFechaAlbum
import com.example.vinilos.ui.crear.TagFormularioCrear
import com.example.vinilos.ui.crear.TagGeneroAlbum
import com.example.vinilos.ui.crear.TagNombreAlbum
import com.example.vinilos.ui.crear.TagPortadaAlbum
import com.example.vinilos.ui.crear.tagErrorCampo
import com.example.vinilos.ui.ingreso.TagEntrarColeccionista
import com.example.vinilos.ui.ingreso.TagEntrarVisitante
import com.example.vinilos.ui.ingreso.tagColeccionista
import com.example.vinilos.ui.inicio.TagInicio
import com.example.vinilos.ui.inicio.TagVerAlbumes
import com.example.vinilos.viewmodel.CampoAlbum
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
 * E2E de la HU07 (crear un álbum), con los escenarios de la T07-5.
 *
 * FN07-ESC01 a ESC06 van contra el backend real. Como crean datos, se corren contra BackVynils local
 * (`-PbaseUrl=http://10.0.2.2:3000/` en el emulador; en un teléfono, `http://localhost:3000/` con
 * `adb reverse tcp:3000 tcp:3000`). Todo álbum creado se borra en [borrarAlbumesCreados] con
 * `DELETE /albums/{id}`, aunque la prueba falle. El coleccionista se elige de los datos reales.
 *
 * FN07-ESC07 (backend caído y reintento) usa `MockWebServer`, como FN01-ESC03 y FN03-ESC05.
 *
 * Cada escenario guarda un pantallazo por paso, `informe.md` y un PDF en
 * `.../files/e2e-evidencia/FN07_ESCxx/` (en el dispositivo; los PDF se versionan a mano en
 * `docs/pruebas/sprint-2/HU07/e2e/`).
 */
class CrearAlbumE2ETest {

    @get:Rule
    val rule = createEmptyComposeRule()

    /** El repositorio real de la app, que además registra cuántas veces se crea y qué ids devolvió. */
    private class AlbumesReales(
        private val real: AlbumRepository = AlbumRepositoryImpl(ServiceAdapter(BuildConfig.BASE_URL))
    ) : AlbumRepository by real {
        @Volatile var envios = 0
        val creados = mutableListOf<Int>()

        override suspend fun crearAlbum(album: AlbumNuevo): Result<Int> {
            envios++
            return real.crearAlbum(album).also { resultado ->
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

    /** Datos válidos del formulario; el nombre lleva la hora para no confundirse con otro álbum. */
    private data class Formulario(
        val nombre: String = "E2E FN07 ${System.currentTimeMillis()}",
        val portada: String = "https://upload.wikimedia.org/wikipedia/en/4/42/Beatles_-_Abbey_Road.jpg",
        val genero: String = "Rock",
        val disquera: String = "EMI",
        val descripcion: String = "Álbum creado por la prueba E2E de la HU07."
    )

    // ---------- Fecha ----------

    /** El día 1 del mes actual (según el día local), que el selector siempre permite elegir. */
    private val primerDiaDelMes: Calendar = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }

    private val fechaVisibleEsperada = String.format(
        Locale.ROOT, "01/%02d/%d", primerDiaDelMes.get(Calendar.MONTH) + 1, primerDiaDelMes.get(Calendar.YEAR)
    )

    private val fechaIsoEsperada = String.format(
        Locale.ROOT, "%d-%02d-01T00:00:00.000Z", primerDiaDelMes.get(Calendar.YEAR), primerDiaDelMes.get(Calendar.MONTH) + 1
    )

    // ---------- Navegación ----------

    private fun esperarA(tag: String, milis: Long = 15_000) =
        rule.waitUntil(milis) { rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }

    /** La insignia NUEVO está dentro de la tarjeta (que fusiona su contenido): se busca sin fusionar. */
    private fun esperarInsigniaNuevo(id: Int, milis: Long = 15_000) = rule.waitUntil(milis) {
        rule.onAllNodesWithTag(tagAlbumNuevo(id), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
    }

    private fun esperarTexto(texto: String, milis: Long = 15_000) =
        rule.waitUntil(milis) { rule.onAllNodesWithText(texto).fetchSemanticsNodes().isNotEmpty() }

    private fun esperarQueDesaparezca(tag: String, milis: Long = 15_000) =
        rule.waitUntil(milis) { rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isEmpty() }

    /** Inicio -> "Ver álbumes" -> catálogo cargado. */
    private fun verCatalogo(evidencia: String, paso: String) {
        esperarA(TagInicio)
        rule.onNodeWithTag(TagVerAlbumes).performScrollTo().performClick()
        esperarA(TagContadorCatalogo)
        EvidenciaE2E.capturar(rule, evidencia, paso)
    }

    /** Ingreso -> Coleccionista [idColeccionista] -> catálogo. */
    private fun entrarComoColeccionista(idColeccionista: Int, evidencia: String) {
        esperarA(TagEntrarColeccionista)
        rule.onNodeWithTag(TagEntrarColeccionista).performScrollTo().performClick()
        esperarA(tagColeccionista(idColeccionista))
        rule.onNodeWithTag(tagColeccionista(idColeccionista)).performClick()
        verCatalogo(evidencia, "01_catalogo_coleccionista")
        rule.onNodeWithTag(TagRolActivo).assertTextEquals("COLECCIONISTA")
    }

    private fun primerColeccionistaReal(): Int = runBlocking {
        CollectorRepositoryImpl(ServiceAdapter(BuildConfig.BASE_URL)).obtenerColeccionistas().getOrThrow().first().id
    }

    private fun abrirFormulario(evidencia: String) {
        rule.onNodeWithTag(TagCrearAlbumCatalogo).performClick()
        esperarA(TagFormularioCrear)
        EvidenciaE2E.capturar(rule, evidencia, "02_formulario_vacio")
    }

    // ---------- Formulario ----------

    private fun escribir(tag: String, texto: String) {
        rule.onNodeWithTag(tag).performScrollTo().performTextInput(texto)
        // En API 23 el teclado abierto puede tapar el siguiente campo o botón y el toque se pierde.
        Espresso.closeSoftKeyboard()
    }

    private fun elegirOpcion(tag: String, opcion: String) {
        rule.onNodeWithTag(tag).performScrollTo().performClick()
        esperarTexto(opcion)
        rule.onNodeWithText(opcion).performClick()
        rule.waitForIdle()
    }

    /** Abre el selector, toca el día 1 del mes que muestra (el actual) y pulsa "Aceptar". */
    private fun elegirPrimerDiaDelMes() {
        rule.onNodeWithTag(TagFechaAlbum).performScrollTo().performClick()
        esperarTexto("Aceptar")
        // Cada día se expone con su fecha completa ("domingo, 1 de noviembre de 2026"), no solo con el número.
        rule.onNode(hasText(", 1 de ", substring = true) and hasClickAction()).performClick()
        rule.onNodeWithText("Aceptar").performClick()
        rule.waitForIdle()
    }

    private fun llenar(formulario: Formulario, portada: String = formulario.portada) {
        escribir(TagNombreAlbum, formulario.nombre)
        escribir(TagPortadaAlbum, portada)
        elegirPrimerDiaDelMes()
        elegirOpcion(TagGeneroAlbum, formulario.genero)
        elegirOpcion(TagDisqueraAlbum, formulario.disquera)
        escribir(TagDescripcionAlbum, formulario.descripcion)
    }

    /** Un campo de selección (fecha, género o disquera) muestra [valor]. */
    private fun seleccionMuestra(tag: String, valor: String) {
        val campo = hasTestTag(tag) and hasAnyDescendant(hasText(valor))
        rule.onNode(campo, useUnmergedTree = true).performScrollTo().assertExists()
    }

    /** Todos los campos conservan lo que se escribió o eligió. */
    private fun formularioConserva(formulario: Formulario) {
        rule.onNodeWithTag(TagNombreAlbum).performScrollTo().assertTextEquals(formulario.nombre)
        rule.onNodeWithTag(TagPortadaAlbum).performScrollTo().assertTextEquals(formulario.portada)
        seleccionMuestra(TagFechaAlbum, fechaVisibleEsperada)
        seleccionMuestra(TagGeneroAlbum, formulario.genero)
        seleccionMuestra(TagDisqueraAlbum, formulario.disquera)
        rule.onNodeWithTag(TagDescripcionAlbum).performScrollTo().assertTextEquals(formulario.descripcion)
    }

    /** El campo muestra [mensaje] debajo. */
    private fun campoMarcado(campo: CampoAlbum, mensaje: String) {
        val error = hasTestTag(tagErrorCampo(campo)) and hasAnyDescendant(hasText(mensaje))
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
        AlbumRepositoryImpl(ServiceAdapter(BuildConfig.BASE_URL)).obtenerAlbumes().getOrThrow().size
    }

    private fun albumDelBackend(id: Int): JSONObject {
        val respuesta = OkHttpClient().newCall(Request.Builder().url("${BuildConfig.BASE_URL}albums/$id").build()).execute()
        return respuesta.use { JSONObject(requireNotNull(it.body).string()) }
    }

    // ---------- Escenarios contra el backend real ----------

    /** FN07-ESC01: el Visitante no ve "Crear álbum"; el Coleccionista sí. */
    @Test
    fun elVisitanteNoVeLaOpcionDeCrearYElColeccionistaSi() {
        val evidencia = "FN07_ESC01"
        val coleccionista = primerColeccionistaReal()

        ActivityScenario.launch(MainActivity::class.java).use {
            esperarA(TagEntrarVisitante)
            rule.onNodeWithTag(TagEntrarVisitante).performScrollTo().performClick()
            verCatalogo(evidencia, "01_catalogo_visitante")
            rule.onNodeWithTag(TagRolActivo).assertTextEquals("VISITANTE")
            rule.onAllNodesWithTag(TagCrearAlbumCatalogo).assertCountEquals(0)

            // Volver al ingreso (catálogo -> inicio -> ingreso) con el botón atrás y entrar como Coleccionista.
            Espresso.pressBack()
            esperarA(TagInicio)
            rule.waitForIdle()
            Espresso.pressBack()
            esperarA(TagEntrarColeccionista)
            rule.onNodeWithTag(TagEntrarColeccionista).performScrollTo().performClick()
            esperarA(tagColeccionista(coleccionista))
            rule.onNodeWithTag(tagColeccionista(coleccionista)).performClick()
            verCatalogo(evidencia, "02_catalogo_coleccionista")
            rule.onNodeWithTag(TagCrearAlbumCatalogo).assertIsDisplayed()
        }
    }

    /** FN07-ESC02: un álbum válido se guarda, aparece "Álbum creado" y el álbum en el catálogo como NUEVO. */
    @Test
    fun crearUnAlbumValidoLoGuardaYLoMuestraEnElCatalogo() {
        val evidencia = "FN07_ESC02"
        val formulario = Formulario()
        val antes = albumesEnElBackend()

        ActivityScenario.launch(MainActivity::class.java).use {
            entrarComoColeccionista(primerColeccionistaReal(), evidencia)
            abrirFormulario(evidencia)
            llenar(formulario)
            seleccionMuestra(TagFechaAlbum, fechaVisibleEsperada)
            EvidenciaE2E.capturar(rule, evidencia, "03_formulario_lleno")

            pulsar(TagCrearAlbum)
            esperarQueDesaparezca(TagFormularioCrear)
            esperarTexto("Álbum creado", milis = 10_000)
            EvidenciaE2E.capturar(rule, evidencia, "04_album_creado")

            val id = synchronized(albumes.creados) { albumes.creados.single() }
            esperarInsigniaNuevo(id)
            rule.onNodeWithTag(tagAlbumNuevo(id), useUnmergedTree = true).assertIsDisplayed()
            rule.onAllNodesWithText(formulario.nombre)[0].assertIsDisplayed()
            EvidenciaE2E.capturar(rule, evidencia, "05_album_nuevo_en_el_catalogo")

            // El backend guardó el álbum una sola vez, con el día elegido (sin correrse por la zona horaria).
            assertEquals(1, albumes.envios)
            assertEquals(antes + 1, albumesEnElBackend())
            val guardado = albumDelBackend(id)
            assertEquals(formulario.nombre, guardado.getString("name"))
            assertEquals(fechaIsoEsperada, guardado.getString("releaseDate"))
            assertEquals(formulario.genero, guardado.getString("genre"))
            assertEquals(formulario.disquera, guardado.getString("recordLabel"))
        }
    }

    /** FN07-ESC03: "Cancelar" con el formulario lleno cierra sin guardar ni enviar nada. */
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
            esperarQueDesaparezca(TagFormularioCrear)
            esperarA(TagContadorCatalogo)
            rule.onAllNodesWithText(formulario.nombre).assertCountEquals(0)
            EvidenciaE2E.capturar(rule, evidencia, "04_catalogo_sin_cambios")

            assertEquals(0, albumes.envios)
            assertEquals(antes, albumesEnElBackend())
        }
    }

    /** FN07-ESC04: con el formulario vacío, cada campo se marca con "Campo obligatorio" y no se envía nada. */
    @Test
    fun conCamposVaciosSeMarcaCadaUnoYNoSeEnviaNada() {
        val evidencia = "FN07_ESC04"
        val antes = albumesEnElBackend()

        ActivityScenario.launch(MainActivity::class.java).use {
            entrarComoColeccionista(primerColeccionistaReal(), evidencia)
            abrirFormulario(evidencia)

            pulsar(TagCrearAlbum)
            esperarA(TagAvisoCampos)
            desplazarA(hasTestTag(TagAvisoCampos))
            rule.onNodeWithText("Atención requerida").assertIsDisplayed()
            EvidenciaE2E.capturar(rule, evidencia, "03_aviso_y_primeros_campos")

            CampoAlbum.entries.forEach { campoMarcado(it, "Campo obligatorio") }
            EvidenciaE2E.capturar(rule, evidencia, "04_ultimos_campos_marcados")

            rule.onNodeWithTag(TagFormularioCrear).assertExists()
            assertEquals(0, albumes.envios)
            assertEquals(antes, albumesEnElBackend())
        }
    }

    /** FN07-ESC05: una portada que no empieza con http:// o https:// muestra "Ingresa una URL válida". */
    @Test
    fun unaPortadaInvalidaMuestraElMensajeYNoSeEnviaNada() {
        val evidencia = "FN07_ESC05"
        val formulario = Formulario()
        val antes = albumesEnElBackend()

        ActivityScenario.launch(MainActivity::class.java).use {
            entrarComoColeccionista(primerColeccionistaReal(), evidencia)
            abrirFormulario(evidencia)
            llenar(formulario, portada = "ftp://vinilos.example/portada.jpg")

            pulsar(TagCrearAlbum)
            campoMarcado(CampoAlbum.Portada, "Ingresa una URL válida")
            EvidenciaE2E.capturar(rule, evidencia, "03_url_invalida")

            // Solo la portada queda marcada: los demás campos son válidos.
            CampoAlbum.entries.filter { it != CampoAlbum.Portada }.forEach {
                rule.onAllNodesWithTag(tagErrorCampo(it), useUnmergedTree = true).assertCountEquals(0)
            }
            assertEquals(0, albumes.envios)
            assertEquals(antes, albumesEnElBackend())
        }
    }

    /** FN07-ESC06: al girar la pantalla, el formulario lleno conserva todo lo escrito y elegido. */
    @Test
    fun alGirarLaPantallaElFormularioConservaLoEscritoYElegido() {
        val evidencia = "FN07_ESC06"
        val formulario = Formulario()

        ActivityScenario.launch(MainActivity::class.java).use { escenario ->
            entrarComoColeccionista(primerColeccionistaReal(), evidencia)
            abrirFormulario(evidencia)
            llenar(formulario)
            EvidenciaE2E.capturar(rule, evidencia, "03_antes_de_girar")

            // Girar de verdad (no solo recrear la Activity) para que la evidencia se vea en horizontal.
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

    // ---------- Escenario con MockWebServer ----------

    /** FN07-ESC07: con el backend caído se ve el error, se conservan los datos y el reintento crea el álbum. */
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
        // "localhost" es uno de los hosts con HTTP permitido en network_security_config.xml.
        ApplicationProvider.getApplicationContext<VinilosApplication>().container =
            AppContainer(baseUrl = "http://localhost:${nuevo.port}/")

        ActivityScenario.launch(MainActivity::class.java).use {
            entrarComoColeccionista(100, evidencia)
            abrirFormulario(evidencia)
            llenar(formulario)

            pulsar(TagCrearAlbum)
            esperarA(TagErrorEnvio)
            desplazarA(hasTestTag(TagErrorEnvio))
            rule.onNodeWithText("No se pudo crear el álbum, intenta de nuevo").assertIsDisplayed()
            rule.onNodeWithTag(TagCrearAlbum).performScrollTo()
            rule.onNodeWithText("Reintentar creación").assertIsDisplayed()
            EvidenciaE2E.capturar(rule, evidencia, "03_error_al_crear")

            formularioConserva(formulario)
            EvidenciaE2E.capturar(rule, evidencia, "04_datos_conservados")

            // El backend "vuelve" y la persona reintenta.
            caido = false
            pulsar(TagCrearAlbum)
            esperarQueDesaparezca(TagFormularioCrear)
            esperarTexto("Álbum creado", milis = 10_000)
            esperarInsigniaNuevo(idCreado)
            EvidenciaE2E.capturar(rule, evidencia, "05_album_creado_al_reintentar")
            assertEquals(1, creaciones)
        }
    }
}
