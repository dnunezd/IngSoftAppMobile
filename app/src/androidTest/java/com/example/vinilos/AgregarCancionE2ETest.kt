package com.example.vinilos

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
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import com.example.vinilos.data.model.Cancion
import com.example.vinilos.data.remote.ServiceAdapter
import com.example.vinilos.data.repository.AlbumRepository
import com.example.vinilos.data.repository.AlbumRepositoryImpl
import com.example.vinilos.data.repository.CollectorRepositoryImpl
import com.example.vinilos.e2e.AlbumDePrueba
import com.example.vinilos.e2e.AlbumesDePrueba
import com.example.vinilos.e2e.EvidenciaE2E
import com.example.vinilos.ui.catalogo.TagContadorCatalogo
import com.example.vinilos.ui.catalogo.TagCuadriculaCatalogo
import com.example.vinilos.ui.catalogo.tagAlbum
import com.example.vinilos.ui.crear.TagCancelarAgregarCancion
import com.example.vinilos.ui.crear.TagDuracionCancion
import com.example.vinilos.ui.crear.TagFormularioAgregarCancion
import com.example.vinilos.ui.crear.TagGuardarCancion
import com.example.vinilos.ui.crear.TagNombreCancion
import com.example.vinilos.ui.detalle.TagAgregarCancionDetalle
import com.example.vinilos.ui.detalle.TagListaDetalleAlbum
import com.example.vinilos.ui.ingreso.TagEntrarColeccionista
import com.example.vinilos.ui.ingreso.TagEntrarVisitante
import com.example.vinilos.ui.ingreso.TagListaColeccionistas
import com.example.vinilos.ui.ingreso.tagColeccionista
import com.example.vinilos.ui.inicio.TagInicio
import com.example.vinilos.ui.inicio.TagVerAlbumes
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
 * E2E de la HU08 (asociar canciones a un álbum).
 *
 * FN08-ESC01 a ESC04 van contra el backend real. Como agregan canciones, se corren contra BackVynils
 * local (en un teléfono, `-PbaseUrl=http://localhost:3000/` con `adb reverse tcp:3000 tcp:3000`). Los
 * escenarios del Coleccionista trabajan sobre un álbum propio creado con `POST /albums`, que
 * [AlbumesDePrueba] borra al terminar con sus canciones, aunque la prueba falle. El Visitante usa el
 * primer álbum real y el Coleccionista, el primer coleccionista real.
 *
 * FN08-ESC05 y ESC06 (errores que el backend real no provoca a voluntad) usan `MockWebServer`, como
 * FN07-ESC07 y FN10-ESC06 a ESC08.
 *
 * Cada escenario guarda un pantallazo por paso, `informe.md` y un PDF en
 * `.../files/e2e-evidencia/FN08_ESCxx/` (en el dispositivo; los PDF se versionan a mano en
 * `docs/pruebas/sprint-3/HU08/e2e/`).
 */
class AgregarCancionE2ETest {

    @get:Rule
    val rule = createEmptyComposeRule()

    /** El repositorio real de la app, que además cuenta los envíos de canciones. */
    private class AlbumesReales(
        private val real: AlbumRepository = AlbumRepositoryImpl(ServiceAdapter(BuildConfig.BASE_URL))
    ) : AlbumRepository by real {
        @Volatile var envios = 0

        override suspend fun agregarCancion(albumId: Int, cancion: Cancion): Result<Unit> {
            envios++
            return real.agregarCancion(albumId, cancion)
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

    // ---------- Datos reales ----------

    private fun primerAlbumReal(): Int = runBlocking {
        val album = AlbumRepositoryImpl(ServiceAdapter(BuildConfig.BASE_URL)).obtenerAlbumes().getOrThrow().firstOrNull()
        Assume.assumeTrue("Sin datos en el backend: no hay álbumes", album != null)
        album!!.id
    }

    private fun primerColeccionistaReal(): Int = runBlocking {
        CollectorRepositoryImpl(ServiceAdapter(BuildConfig.BASE_URL)).obtenerColeccionistas().getOrThrow().first().id
    }

    /** Canciones del álbum según el backend, como pares nombre-duración. */
    private fun cancionesEnElBackend(album: AlbumDePrueba): List<Pair<String, String>> {
        val canciones = albumesDePrueba.album(album.id).getJSONArray("tracks")
        return (0 until canciones.length()).map {
            canciones.getJSONObject(it).let { cancion -> cancion.getString("name") to cancion.getString("duration") }
        }
    }

    // ---------- Navegación ----------

    private fun esperarA(tag: String, milis: Long = 15_000) =
        rule.waitUntil(milis) { rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }

    private fun esperarTexto(texto: String, milis: Long = 15_000) =
        rule.waitUntil(milis) { rule.onAllNodesWithText(texto).fetchSemanticsNodes().isNotEmpty() }

    private fun esperarQueSeCierreElFormulario() =
        rule.waitUntil(15_000) { rule.onAllNodesWithTag(TagFormularioAgregarCancion).fetchSemanticsNodes().isEmpty() }

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

    /** Inicio -> "Ver álbumes" -> detalle del álbum [id], cargado. */
    private fun abrirDetalle(id: Int, evidencia: String, paso: String) {
        rule.onNodeWithTag(TagVerAlbumes).performScrollTo().performClick()
        esperarA(TagContadorCatalogo)
        rule.onNodeWithTag(TagCuadriculaCatalogo).performScrollToNode(hasTestTag(tagAlbum(id)))
        rule.onNodeWithTag(tagAlbum(id)).performClick()
        // La sección puede estar más abajo, aún sin componer: se espera a que el detalle cargue y se llegue a ella.
        rule.waitUntil(15_000) {
            runCatching { rule.onNodeWithTag(TagListaDetalleAlbum).performScrollToNode(hasText("Canciones")) }.isSuccess
        }
        EvidenciaE2E.capturar(rule, evidencia, paso)
    }

    private fun abrirFormulario() {
        rule.onNodeWithTag(TagListaDetalleAlbum).performScrollToNode(hasTestTag(TagAgregarCancionDetalle))
        rule.onNodeWithTag(TagAgregarCancionDetalle).performClick()
        esperarA(TagFormularioAgregarCancion)
    }

    private fun escribir(tag: String, texto: String) {
        rule.onNodeWithTag(tag).performScrollTo().performTextReplacement(texto)
        Espresso.closeSoftKeyboard()
    }

    /** Cuántos campos muestran [mensaje] debajo (el texto de apoyo está dentro del campo). */
    private fun camposCon(mensaje: String) = rule.onAllNodesWithText(mensaje, useUnmergedTree = true)

    private fun guardar() = rule.onNodeWithTag(TagGuardarCancion).performScrollTo()

    /** La canción [nombre] aparece en la lista del detalle con su [duracion]. */
    private fun verCancionEnElDetalle(nombre: String, duracion: String) {
        rule.waitUntil(15_000) {
            runCatching { rule.onNodeWithTag(TagListaDetalleAlbum).performScrollToNode(hasText(nombre)) }.isSuccess
        }
        rule.onNodeWithText(nombre).assertIsDisplayed()
        rule.onNodeWithText(duracion).assertIsDisplayed()
    }

    // ---------- Escenarios contra el backend real ----------

    /** FN08-ESC01: el Visitante no ve la acción "Agregar canción" en el detalle de un álbum. */
    @Test
    fun elVisitanteNoVeLaAccionDeAgregarCancion() {
        val evidencia = "FN08_ESC01"
        val album = primerAlbumReal()

        ActivityScenario.launch(MainActivity::class.java).use {
            entrarComoVisitante()
            abrirDetalle(album, evidencia, "01_detalle_seccion_canciones")
            rule.onAllNodesWithTag(TagAgregarCancionDetalle).assertCountEquals(0)
            rule.onNodeWithTag(TagListaDetalleAlbum).performScrollToNode(hasText("Comentarios"))
            rule.onAllNodesWithTag(TagAgregarCancionDetalle).assertCountEquals(0)
            EvidenciaE2E.capturar(rule, evidencia, "02_detalle_sin_agregar_cancion")
        }
        assertEquals(0, albumes.envios)
    }

    /** FN08-ESC02: el Coleccionista agrega una canción válida y la ve en el detalle del álbum. */
    @Test
    fun elColeccionistaAgregaUnaCancionAlAlbum() {
        val evidencia = "FN08_ESC02"
        val album = albumesDePrueba.crear("E2E FN08")

        ActivityScenario.launch(MainActivity::class.java).use {
            entrarComoColeccionista(primerColeccionistaReal())
            abrirDetalle(album.id, evidencia, "01_detalle_sin_canciones")
            rule.onNodeWithText("Este álbum aún no tiene canciones registradas").assertIsDisplayed()

            abrirFormulario()
            camposCon("Campo obligatorio").assertCountEquals(2)
            guardar().assertIsNotEnabled()
            EvidenciaE2E.capturar(rule, evidencia, "02_formulario_vacio")

            escribir(TagNombreCancion, "Pista E2E")
            camposCon("Campo obligatorio").assertCountEquals(1)
            escribir(TagDuracionCancion, "3:45")
            camposCon("Campo obligatorio").assertCountEquals(0)
            guardar().assertIsEnabled()
            EvidenciaE2E.capturar(rule, evidencia, "03_formulario_lleno")

            guardar().performClick()
            esperarQueSeCierreElFormulario()
            esperarTexto("Canción agregada", milis = 10_000)
            rule.onNodeWithText("Se asoció a ${album.nombre}").assertIsDisplayed()
            EvidenciaE2E.capturar(rule, evidencia, "04_cancion_agregada")

            verCancionEnElDetalle("Pista E2E", "3:45")
            EvidenciaE2E.capturar(rule, evidencia, "05_detalle_con_la_cancion")
        }
        assertEquals(1, albumes.envios)
        assertEquals(listOf("Pista E2E" to "3:45"), cancionesEnElBackend(album))
    }

    /** FN08-ESC03: una duración que no cumple m:ss muestra el error y deja "Guardar" deshabilitado. */
    @Test
    fun unaDuracionConFormatoInvalidoNoSePuedeGuardar() {
        val evidencia = "FN08_ESC03"
        val album = albumesDePrueba.crear("E2E FN08")
        val errorFormato = "Usa el formato m:ss, por ejemplo 5:05"

        ActivityScenario.launch(MainActivity::class.java).use {
            entrarComoColeccionista(primerColeccionistaReal())
            abrirDetalle(album.id, evidencia, "01_detalle_sin_canciones")
            abrirFormulario()
            escribir(TagNombreCancion, "Pista E2E")

            escribir(TagDuracionCancion, "3:75")
            camposCon(errorFormato).assertCountEquals(1)
            guardar().assertIsNotEnabled()
            EvidenciaE2E.capturar(rule, evidencia, "02_segundos_fuera_de_rango")

            escribir(TagDuracionCancion, "345")
            camposCon(errorFormato).assertCountEquals(1)
            guardar().assertIsNotEnabled()
            EvidenciaE2E.capturar(rule, evidencia, "03_sin_dos_puntos")

            escribir(TagDuracionCancion, "3:45")
            camposCon(errorFormato).assertCountEquals(0)
            guardar().assertIsEnabled()
            EvidenciaE2E.capturar(rule, evidencia, "04_duracion_corregida")
        }
        assertEquals(0, albumes.envios)
        assertTrue(cancionesEnElBackend(album).isEmpty())
    }

    /** FN08-ESC04: "Cancelar" cierra el formulario sin guardar y al volver a abrirlo aparece vacío. */
    @Test
    fun cancelarCierraSinGuardarYElFormularioVuelveVacio() {
        val evidencia = "FN08_ESC04"
        val album = albumesDePrueba.crear("E2E FN08")

        ActivityScenario.launch(MainActivity::class.java).use {
            entrarComoColeccionista(primerColeccionistaReal())
            abrirDetalle(album.id, evidencia, "01_detalle_sin_canciones")
            abrirFormulario()
            escribir(TagNombreCancion, "Pista cancelada")
            escribir(TagDuracionCancion, "2:30")
            EvidenciaE2E.capturar(rule, evidencia, "02_formulario_lleno")

            rule.onNodeWithTag(TagCancelarAgregarCancion).performScrollTo().performClick()
            esperarQueSeCierreElFormulario()
            rule.onNodeWithTag(TagListaDetalleAlbum).performScrollToNode(hasText("Canciones"))
            rule.onNodeWithText("Este álbum aún no tiene canciones registradas").assertIsDisplayed()
            rule.onAllNodesWithText("Pista cancelada").assertCountEquals(0)
            EvidenciaE2E.capturar(rule, evidencia, "03_detalle_sin_cambios")

            abrirFormulario()
            rule.onNodeWithTag(TagNombreCancion).assert(hasText("Pista cancelada").not())
            camposCon("Campo obligatorio").assertCountEquals(2)
            guardar().assertIsNotEnabled()
            EvidenciaE2E.capturar(rule, evidencia, "04_formulario_vacio_otra_vez")
        }
        assertEquals(0, albumes.envios)
        assertTrue(cancionesEnElBackend(album).isEmpty())
    }

    // ---------- Escenarios con MockWebServer ----------

    /** Estado del servidor simulado, que cada escenario cambia a mitad de la prueba. */
    private class Simulado {
        @Volatile var respuestaGuardado: (envio: Int) -> MockResponse = { MockResponse().setBody("{}") }
        @Volatile var envios = 0
        val canciones = mutableListOf<String>()
    }

    /** Coleccionista 100 y el álbum 100 "Buscando América", sin canciones hasta que se guarda una. */
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
        // "localhost" es uno de los hosts con HTTP permitido en network_security_config.xml.
        ApplicationProvider.getApplicationContext<VinilosApplication>().container =
            AppContainer(baseUrl = "http://localhost:${nuevo.port}/")
    }

    /**
     * FN08-ESC05: mientras se guarda se ve "Guardando..." y un segundo toque no envía nada; si el guardado
     * falla se ve el error con los datos conservados, y el reintento agrega la canción.
     */
    @Test
    fun siFallaElGuardadoSeConservanLosDatosYReintentarLaAgrega() {
        val evidencia = "FN08_ESC05"
        // El primer envío tarda y termina en error del servicio; el siguiente sale bien.
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
            escribir(TagNombreCancion, "Decisiones")
            escribir(TagDuracionCancion, "5:05")
            EvidenciaE2E.capturar(rule, evidencia, "02_formulario_lleno")

            guardar().performClick()
            rule.waitUntil(2_000) {
                rule.onAllNodes(hasTestTag(TagGuardarCancion) and hasText("Guardando...")).fetchSemanticsNodes().isNotEmpty()
            }
            guardar().assertIsNotEnabled()
            EvidenciaE2E.capturar(rule, evidencia, "03_guardando")
            guardar().performClick()

            esperarTexto("No se pudo agregar la canción, intenta de nuevo")
            assertEquals("El toque durante \"Guardando...\" no debe enviar otra petición", 1, simulado.envios)
            rule.onNodeWithTag(TagNombreCancion).assert(hasText("Decisiones"))
            rule.onNodeWithTag(TagDuracionCancion).assert(hasText("5:05"))
            guardar().assertIsEnabled()
            EvidenciaE2E.capturar(rule, evidencia, "04_error_al_guardar")

            guardar().performClick()
            esperarQueSeCierreElFormulario()
            esperarTexto("Canción agregada", milis = 10_000)
            verCancionEnElDetalle("Decisiones", "5:05")
            EvidenciaE2E.capturar(rule, evidencia, "05_agregada_al_reintentar")
        }
        assertEquals(2, simulado.envios)
    }

    /** FN08-ESC06: si el álbum ya no existe al guardar, se avisa y se puede volver sin agregar la canción. */
    @Test
    fun siElAlbumYaNoExisteSeAvisaYSePuedeVolver() {
        val evidencia = "FN08_ESC06"
        val simulado = Simulado().apply { respuestaGuardado = { MockResponse().setResponseCode(404) } }
        iniciarServidor(simulado)

        ActivityScenario.launch(MainActivity::class.java).use {
            entrarComoColeccionista(100)
            abrirDetalle(100, evidencia, "01_detalle_sin_canciones")
            abrirFormulario()
            escribir(TagNombreCancion, "Decisiones")
            escribir(TagDuracionCancion, "5:05")
            guardar().performClick()

            esperarTexto("El álbum ya no está disponible")
            rule.onAllNodesWithText("Canción agregada").assertCountEquals(0)
            rule.onNodeWithText("Volver al álbum").assertIsDisplayed()
            EvidenciaE2E.capturar(rule, evidencia, "02_album_no_disponible")

            rule.onNodeWithText("Volver al álbum").performClick()
            esperarQueSeCierreElFormulario()
            esperarA(TagListaDetalleAlbum)
            rule.onNodeWithTag(TagListaDetalleAlbum).performScrollToNode(hasText("Canciones"))
            rule.onNodeWithText("Este álbum aún no tiene canciones registradas").assertIsDisplayed()
            EvidenciaE2E.capturar(rule, evidencia, "03_vuelve_sin_la_cancion")
        }
        assertEquals(1, simulado.envios)
    }
}
