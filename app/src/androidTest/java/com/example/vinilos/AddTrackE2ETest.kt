package com.example.vinilos

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import com.example.vinilos.data.model.Track
import com.example.vinilos.data.remote.ServiceAdapter
import com.example.vinilos.data.repository.AlbumRepository
import com.example.vinilos.data.repository.AlbumRepositoryImpl
import com.example.vinilos.data.repository.CollectorRepositoryImpl
import com.example.vinilos.e2e.AlbumDePrueba
import com.example.vinilos.e2e.AlbumesDePrueba
import com.example.vinilos.e2e.EvidenciaE2E
import com.example.vinilos.ui.catalog.TagContadorCatalogo
import com.example.vinilos.ui.catalog.TagCuadriculaCatalogo
import com.example.vinilos.ui.catalog.tagAlbum
import com.example.vinilos.ui.create.TagCancelarAddTrack
import com.example.vinilos.ui.create.TagDuracionTrack
import com.example.vinilos.ui.create.TagFormularioAddTrack
import com.example.vinilos.ui.create.TagGuardarTrack
import com.example.vinilos.ui.create.TagNombreTrack
import com.example.vinilos.ui.detail.TagAddTrackDetalle
import com.example.vinilos.ui.detail.TagListaDetalleAlbum
import com.example.vinilos.ui.entry.TagEntrarColeccionista
import com.example.vinilos.ui.entry.TagEntrarVisitante
import com.example.vinilos.ui.entry.TagListaColeccionistas
import com.example.vinilos.ui.entry.tagColeccionista
import com.example.vinilos.ui.home.TagInicio
import com.example.vinilos.ui.home.TagVerAlbumes
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * E2E for HU08 (associating tracks with an album).
 *
 * FN08-ESC01, ESC02, and ESC04 through ESC07 run against the real backend. Since they add tracks, they
 * run against a local BackVynils (on a phone, `-PbaseUrl=http://localhost:3000/` with `adb reverse
 * tcp:3000 tcp:3000`). The Collector scenarios work on their own album created with `POST /albums`,
 * which [AlbumesDePrueba] deletes at the end along with its tracks, even if the test fails; ESC07
 * deletes it midway through the test to attempt a save against an already-deleted album. The Visitor
 * uses the first real album, and the Collector uses the first real collector.
 *
 * FN08-ESC03 (save failure and retry) uses `MockWebServer`, like FN07-ESC07 and FN10-ESC07.
 *
 * Each scenario saves a screenshot per step, `informe.md` and a PDF in
 * `.../files/e2e-evidencia/FN08_ESCxx/` (on the device; the PDFs are versioned by hand in
 * `docs/pruebas/sprint-3/HU08/e2e/`).
 */
class AddTrackE2ETest {

    @get:Rule
    val rule = createEmptyComposeRule()

    /** The app's real repository, which also counts the track submissions. */
    private class AlbumesReales(
        private val real: AlbumRepository = AlbumRepositoryImpl(ServiceAdapter(BuildConfig.BASE_URL))
    ) : AlbumRepository by real {
        @Volatile var envios = 0

        override suspend fun addTrack(albumId: Int, cancion: Track): Result<Unit> {
            envios++
            return real.addTrack(albumId, cancion)
        }
    }

    private val albumes = AlbumesReales()
    private val albumesDePrueba = AlbumesDePrueba()
    private var servidor: MockWebServer? = null

    @Before
    fun usarElBackendReal() {
        ApplicationProvider.getApplicationContext<VinilosApplication>().container =
            AppContainer(albumesRepositorio = albumes)
    }

    @After
    fun borrarAlbumesDePrueba() {
        servidor?.shutdown()
        albumesDePrueba.borrarTodos()
    }

    // ---------- Real data ----------

    private fun primerAlbumReal(): Int = runBlocking {
        val album = AlbumRepositoryImpl(ServiceAdapter(BuildConfig.BASE_URL)).getAlbums().getOrThrow().firstOrNull()
        Assume.assumeTrue("Sin datos en el backend: no hay álbumes", album != null)
        album!!.id
    }

    private fun primerColeccionistaReal(): Int = runBlocking {
        CollectorRepositoryImpl(ServiceAdapter(BuildConfig.BASE_URL)).getCollectors().getOrThrow().first().id
    }

    /** The album's tracks according to the backend, as name-duration pairs. */
    private fun cancionesEnElBackend(album: AlbumDePrueba): List<Pair<String, String>> {
        val canciones = albumesDePrueba.album(album.id).getJSONArray("tracks")
        return (0 until canciones.length()).map {
            canciones.getJSONObject(it).let { cancion -> cancion.getString("name") to cancion.getString("duration") }
        }
    }

    // ---------- Navigation ----------

    private fun esperarA(tag: String, milis: Long = 15_000) =
        rule.waitUntil(milis) { rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }

    private fun esperarTexto(texto: String, milis: Long = 15_000) =
        rule.waitUntil(milis) { rule.onAllNodesWithText(texto).fetchSemanticsNodes().isNotEmpty() }

    private fun esperarQueSeCierreElFormulario() =
        rule.waitUntil(15_000) { rule.onAllNodesWithTag(TagFormularioAddTrack).fetchSemanticsNodes().isEmpty() }

    private fun entrarComoVisitante() {
        esperarA(TagEntrarVisitante)
        rule.onNodeWithTag(TagEntrarVisitante).performScrollTo().performClick()
        esperarA(TagInicio)
    }

    private fun entrarComoColeccionista(coleccionista: Int) {
        esperarA(TagEntrarColeccionista)
        rule.onNodeWithTag(TagEntrarColeccionista).performScrollTo().performClick()
        rule.waitUntil(15_000) {
            runCatching {
                rule.onNodeWithTag(TagListaColeccionistas).performScrollToNode(hasTestTag(tagColeccionista(coleccionista)))
            }.isSuccess
        }
        rule.onNodeWithTag(tagColeccionista(coleccionista)).performClick()
        esperarA(TagInicio)
    }

    /** Home -> "Ver álbumes" -> detail of album [id], loaded. */
    private fun abrirDetalle(id: Int, evidencia: String, paso: String) {
        rule.onNodeWithTag(TagVerAlbumes).performScrollTo().performClick()
        esperarA(TagContadorCatalogo)
        rule.onNodeWithTag(TagCuadriculaCatalogo).performScrollToNode(hasTestTag(tagAlbum(id)))
        rule.onNodeWithTag(tagAlbum(id)).performClick()
        // The section may be further down, still not composed: wait for the detail to load and scroll to it.
        rule.waitUntil(15_000) {
            runCatching { rule.onNodeWithTag(TagListaDetalleAlbum).performScrollToNode(hasText("Canciones")) }.isSuccess
        }
        EvidenciaE2E.capturar(rule, evidencia, paso)
    }

    private fun abrirFormulario() {
        rule.onNodeWithTag(TagListaDetalleAlbum).performScrollToNode(hasTestTag(TagAddTrackDetalle))
        rule.onNodeWithTag(TagAddTrackDetalle).performClick()
        esperarA(TagFormularioAddTrack)
    }

    private fun escribir(tag: String, texto: String) {
        rule.onNodeWithTag(tag).performScrollTo().performTextReplacement(texto)
        Espresso.closeSoftKeyboard()
    }

    /** How many fields show [mensaje] below (the supporting text is inside the field). */
    private fun camposCon(mensaje: String) = rule.onAllNodesWithText(mensaje, useUnmergedTree = true)

    private fun save() = rule.onNodeWithTag(TagGuardarTrack).performScrollTo()

    /** The track [nombre] appears in the detail's list with its [duracion]. */
    private fun verTrackEnElDetalle(nombre: String, duracion: String) {
        rule.waitUntil(15_000) {
            runCatching { rule.onNodeWithTag(TagListaDetalleAlbum).performScrollToNode(hasText(nombre)) }.isSuccess
        }
        rule.onNodeWithText(nombre).assertIsDisplayed()
        rule.onNodeWithText(duracion).assertIsDisplayed()
    }

    /** Actually rotates the screen (not just recreating the Activity), so the evidence appears rotated. */
    private fun girar(escenario: ActivityScenario<MainActivity>, horizontal: Boolean) {
        escenario.onActivity {
            it.requestedOrientation =
                if (horizontal) ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        val esperada = if (horizontal) Configuration.ORIENTATION_LANDSCAPE else Configuration.ORIENTATION_PORTRAIT
        rule.waitUntil(5_000) {
            var lista = false
            escenario.onActivity { lista = it.resources.configuration.orientation == esperada }
            lista
        }
        esperarA(TagFormularioAddTrack)
        rule.waitForIdle()
    }

    // ---------- Scenarios against the real backend ----------

    /** FN08-ESC01: a duration that doesn't follow m:ss shows the error and leaves "Guardar" disabled. */
    @Test
    fun unaDuracionConFormatoInvalidoNoSePuedeGuardar() {
        val evidencia = "FN08_ESC01"
        val album = albumesDePrueba.create("E2E FN08")
        val errorFormato = "Usa el formato m:ss, por ejemplo 5:05"

        ActivityScenario.launch(MainActivity::class.java).use {
            entrarComoColeccionista(primerColeccionistaReal())
            abrirDetalle(album.id, evidencia, "01_detalle_sin_canciones")
            abrirFormulario()
            escribir(TagNombreTrack, "Pista E2E")

            escribir(TagDuracionTrack, "3:75")
            camposCon(errorFormato).assertCountEquals(1)
            save().assertIsNotEnabled()
            EvidenciaE2E.capturar(rule, evidencia, "02_segundos_fuera_de_rango")

            escribir(TagDuracionTrack, "345")
            camposCon(errorFormato).assertCountEquals(1)
            save().assertIsNotEnabled()
            EvidenciaE2E.capturar(rule, evidencia, "03_sin_dos_puntos")

            escribir(TagDuracionTrack, "3:45")
            camposCon(errorFormato).assertCountEquals(0)
            save().assertIsEnabled()
            EvidenciaE2E.capturar(rule, evidencia, "04_duracion_corregida")
        }
        assertEquals(0, albumes.envios)
        assertTrue(cancionesEnElBackend(album).isEmpty())
    }

    /** FN08-ESC02: the Collector adds a valid track and sees it in the album's detail. */
    @Test
    fun elColeccionistaAgregaUnaTrackAlAlbum() {
        val evidencia = "FN08_ESC02"
        val album = albumesDePrueba.create("E2E FN08")

        ActivityScenario.launch(MainActivity::class.java).use {
            entrarComoColeccionista(primerColeccionistaReal())
            abrirDetalle(album.id, evidencia, "01_detalle_sin_canciones")
            rule.onNodeWithText("Este álbum aún no tiene canciones registradas").assertIsDisplayed()

            abrirFormulario()
            camposCon("Campo obligatorio").assertCountEquals(2)
            save().assertIsNotEnabled()
            EvidenciaE2E.capturar(rule, evidencia, "02_formulario_vacio")

            escribir(TagNombreTrack, "Pista E2E")
            camposCon("Campo obligatorio").assertCountEquals(1)
            escribir(TagDuracionTrack, "3:45")
            camposCon("Campo obligatorio").assertCountEquals(0)
            save().assertIsEnabled()
            EvidenciaE2E.capturar(rule, evidencia, "03_formulario_lleno")

            save().performClick()
            esperarQueSeCierreElFormulario()
            esperarTexto("Canción agregada", milis = 10_000)
            rule.onNodeWithText("Se asoció a ${album.nombre}").assertIsDisplayed()
            EvidenciaE2E.capturar(rule, evidencia, "04_cancion_agregada")

            verTrackEnElDetalle("Pista E2E", "3:45")
            EvidenciaE2E.capturar(rule, evidencia, "05_detalle_con_la_cancion")
        }
        assertEquals(1, albumes.envios)
        assertEquals(listOf("Pista E2E" to "3:45"), cancionesEnElBackend(album))
    }

    /** FN08-ESC04: the Visitor doesn't see the "Agregar canción" action in an album's detail. */
    @Test
    fun elVisitanteNoVeLaAccionDeAddTrack() {
        val evidencia = "FN08_ESC04"
        val album = primerAlbumReal()

        ActivityScenario.launch(MainActivity::class.java).use {
            entrarComoVisitante()
            abrirDetalle(album, evidencia, "01_detalle_seccion_canciones")
            rule.onAllNodesWithTag(TagAddTrackDetalle).assertCountEquals(0)
            rule.onNodeWithTag(TagListaDetalleAlbum).performScrollToNode(hasText("Comentarios"))
            rule.onAllNodesWithTag(TagAddTrackDetalle).assertCountEquals(0)
            EvidenciaE2E.capturar(rule, evidencia, "02_detalle_sin_agregar_cancion")
        }
        assertEquals(0, albumes.envios)
    }

    /** FN08-ESC05: "Cancelar" closes the form without saving, and reopening it shows it empty. */
    @Test
    fun cancelarCierraSinGuardarYElFormularioVuelveEmpty() {
        val evidencia = "FN08_ESC05"
        val album = albumesDePrueba.create("E2E FN08")

        ActivityScenario.launch(MainActivity::class.java).use {
            entrarComoColeccionista(primerColeccionistaReal())
            abrirDetalle(album.id, evidencia, "01_detalle_sin_canciones")
            abrirFormulario()
            escribir(TagNombreTrack, "Pista cancelada")
            escribir(TagDuracionTrack, "2:30")
            EvidenciaE2E.capturar(rule, evidencia, "02_formulario_lleno")

            rule.onNodeWithTag(TagCancelarAddTrack).performScrollTo().performClick()
            esperarQueSeCierreElFormulario()
            rule.onNodeWithTag(TagListaDetalleAlbum).performScrollToNode(hasText("Canciones"))
            rule.onNodeWithText("Este álbum aún no tiene canciones registradas").assertIsDisplayed()
            rule.onAllNodesWithText("Pista cancelada").assertCountEquals(0)
            EvidenciaE2E.capturar(rule, evidencia, "03_detalle_sin_cambios")

            abrirFormulario()
            rule.onNodeWithTag(TagNombreTrack).assert(hasText("Pista cancelada").not())
            camposCon("Campo obligatorio").assertCountEquals(2)
            save().assertIsNotEnabled()
            EvidenciaE2E.capturar(rule, evidencia, "04_formulario_vacio_otra_vez")
        }
        assertEquals(0, albumes.envios)
        assertTrue(cancionesEnElBackend(album).isEmpty())
    }

    /**
     * FN08-ESC06: when the screen is rotated, the form preserves what was written and the duration
     * error; after fixing it and rotating back, it saves only once.
     */
    @Test
    fun alGirarLaPantallaElFormularioConservaLoEscritoYSeGuardaUnaVez() {
        val evidencia = "FN08_ESC06"
        val album = albumesDePrueba.create("E2E FN08")
        val errorFormato = "Usa el formato m:ss, por ejemplo 5:05"

        ActivityScenario.launch(MainActivity::class.java).use { escenario ->
            entrarComoColeccionista(primerColeccionistaReal())
            abrirDetalle(album.id, evidencia, "01_detalle_sin_canciones")
            abrirFormulario()
            escribir(TagNombreTrack, "Pista girada")
            escribir(TagDuracionTrack, "3:75")
            camposCon(errorFormato).assertCountEquals(1)
            EvidenciaE2E.capturar(rule, evidencia, "02_antes_de_girar")

            girar(escenario, horizontal = true)
            rule.onNodeWithTag(TagNombreTrack).performScrollTo().assert(hasText("Pista girada"))
            rule.onNodeWithTag(TagDuracionTrack).performScrollTo().assert(hasText("3:75"))
            camposCon(errorFormato).assertCountEquals(1)
            save().assertIsNotEnabled()
            EvidenciaE2E.capturar(rule, evidencia, "03_horizontal_conserva_datos_y_error")

            escribir(TagDuracionTrack, "3:45")
            camposCon(errorFormato).assertCountEquals(0)
            girar(escenario, horizontal = false)
            rule.onNodeWithTag(TagNombreTrack).performScrollTo().assert(hasText("Pista girada"))
            rule.onNodeWithTag(TagDuracionTrack).performScrollTo().assert(hasText("3:45"))
            save().assertIsEnabled()
            EvidenciaE2E.capturar(rule, evidencia, "04_vertical_con_duracion_corregida")

            save().performClick()
            esperarQueSeCierreElFormulario()
            esperarTexto("Canción agregada", milis = 10_000)
            verTrackEnElDetalle("Pista girada", "3:45")
            EvidenciaE2E.capturar(rule, evidencia, "05_detalle_con_la_cancion")
        }
        assertEquals("Girar no debe enviar la canción ni repetir el envío", 1, albumes.envios)
        assertEquals(listOf("Pista girada" to "3:45"), cancionesEnElBackend(album))
    }

    /**
     * FN08-ESC07: if the album is deleted on the backend while the form is open, saving warns that it is
     * no longer available and, on returning, the detail also shows it as unavailable.
     */
    @Test
    fun saveSobreUnAlbumYaEliminadoAvisaQueNoEstaDisponible() {
        val evidencia = "FN08_ESC07"
        val album = albumesDePrueba.create("E2E FN08")

        ActivityScenario.launch(MainActivity::class.java).use {
            entrarComoColeccionista(primerColeccionistaReal())
            abrirDetalle(album.id, evidencia, "01_detalle_sin_canciones")
            abrirFormulario()
            escribir(TagNombreTrack, "Pista huérfana")
            escribir(TagDuracionTrack, "2:10")
            EvidenciaE2E.capturar(rule, evidencia, "02_formulario_lleno")

            // Someone else deletes the album on the backend before the track is saved.
            albumesDePrueba.borrar(album)
            save().performClick()

            esperarTexto("El álbum ya no está disponible")
            rule.onAllNodesWithText("Canción agregada").assertCountEquals(0)
            rule.onNodeWithText("Volver").assertIsDisplayed()
            EvidenciaE2E.capturar(rule, evidencia, "03_album_no_disponible")

            rule.onNodeWithText("Volver").performClick()
            esperarQueSeCierreElFormulario()
            // The detail is re-queried and switches to "unavailable" instead of showing the old album.
            esperarTexto("Volver al catálogo")
            rule.onNodeWithText("El álbum ya no está disponible").assertIsDisplayed()
            rule.onAllNodesWithTag(TagAddTrackDetalle).assertCountEquals(0)
            EvidenciaE2E.capturar(rule, evidencia, "04_detalle_no_disponible")
        }
        assertEquals(1, albumes.envios)
    }

    // ---------- Scenarios with MockWebServer ----------

    /** State of the simulated server, which each scenario changes midway through the test. */
    private class Simulado {
        @Volatile var respuestaGuardado: (envio: Int) -> MockResponse = { MockResponse().setBody("{}") }
        @Volatile var envios = 0
        val canciones = mutableListOf<String>()
    }

    /** Collector 100 and album 100 "Buscando América", with no tracks until one is saved. */
    private fun iniciarServidor(simulado: Simulado) {
        fun album() = """
            {"id": 100, "name": "Buscando América", "cover": null, "description": "Salsa urbana",
             "genre": "Salsa", "recordLabel": "Elektra", "releaseDate": "1984-08-01T00:00:00.000Z",
             "tracks": [${synchronized(simulado.canciones) { simulado.canciones.joinToString() }}],
             "performers": [], "comments": []}
        """.trimIndent()
        val nuevo = MockWebServer().apply {
            dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse = when {
                    request.path == "/collectors" -> MockResponse().setBody(
                        """[{"id": 100, "name": "Manolo Bellon", "email": "manollo@caracol.com.co"}]"""
                    )
                    request.path == "/albums" -> MockResponse().setBody("[${album()}]")
                    request.path == "/albums/100/tracks" && request.method == "POST" -> {
                        simulado.envios++
                        simulado.respuestaGuardado(simulado.envios).also { respuesta ->
                            if (respuesta.status.contains(" 200 ")) {
                                val cuerpo = request.body.readUtf8()
                                synchronized(simulado.canciones) { simulado.canciones += cuerpo.replaceFirst("{", """{"id": 1, """) }
                            }
                        }
                    }
                    request.path == "/albums/100" -> MockResponse().setBody(album())
                    else -> MockResponse().setResponseCode(404)
                }
            }
            start()
        }
        servidor = nuevo
        // "localhost" is one of the hosts allowed over HTTP in network_security_config.xml.
        ApplicationProvider.getApplicationContext<VinilosApplication>().container =
            AppContainer(baseUrl = "http://localhost:${nuevo.port}/")
    }

    /**
     * FN08-ESC03: while saving, "Guardando…" is shown and a second tap sends nothing; if the save fails,
     * the error is shown with the data preserved, and retrying adds the track.
     */
    @Test
    fun siFallaElGuardadoSeConservanLosDatosYReintentarLaAgrega() {
        val evidencia = "FN08_ESC03"
        // The first submission takes a while and ends in a server error; the next one succeeds.
        val simulado = Simulado().apply {
            respuestaGuardado = { envio ->
                if (envio == 1) MockResponse().setResponseCode(500).setHeadersDelay(3, TimeUnit.SECONDS)
                else MockResponse().setBody("{}")
            }
        }
        iniciarServidor(simulado)

        ActivityScenario.launch(MainActivity::class.java).use {
            entrarComoColeccionista(100)
            abrirDetalle(100, evidencia, "01_detalle_sin_canciones")
            abrirFormulario()
            escribir(TagNombreTrack, "Decisiones")
            escribir(TagDuracionTrack, "5:05")
            EvidenciaE2E.capturar(rule, evidencia, "02_formulario_lleno")

            save().performClick()
            rule.waitUntil(2_000) {
                rule.onAllNodes(hasTestTag(TagGuardarTrack) and hasText("Guardando…")).fetchSemanticsNodes().isNotEmpty()
            }
            save().assertIsNotEnabled()
            EvidenciaE2E.capturar(rule, evidencia, "03_guardando")
            save().performClick()

            esperarTexto("No se pudo agregar la canción, intenta de nuevo")
            assertEquals("El toque durante \"Guardando…\" no debe enviar otra petición", 1, simulado.envios)
            rule.onNodeWithTag(TagNombreTrack).assert(hasText("Decisiones"))
            rule.onNodeWithTag(TagDuracionTrack).assert(hasText("5:05"))
            save().assertIsEnabled()
            EvidenciaE2E.capturar(rule, evidencia, "04_error_al_guardar")

            save().performClick()
            esperarQueSeCierreElFormulario()
            esperarTexto("Canción agregada", milis = 10_000)
            verTrackEnElDetalle("Decisiones", "5:05")
            EvidenciaE2E.capturar(rule, evidencia, "05_agregada_al_reintentar")
        }
        assertEquals(2, simulado.envios)
    }
}
