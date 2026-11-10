package com.example.vinilos

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
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
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.example.vinilos.data.model.Artist
import com.example.vinilos.data.model.ArtistType
import com.example.vinilos.data.remote.ServiceAdapter
import com.example.vinilos.data.repository.ArtistRepository
import com.example.vinilos.data.repository.FavoriteRepository
import com.example.vinilos.e2e.EvidenciaE2E
import com.example.vinilos.ui.artistas.TagBotonFavorito
import com.example.vinilos.ui.artistas.TagConfirmacionFavorito
import com.example.vinilos.ui.artistas.TagContadorArtistas
import com.example.vinilos.ui.artistas.TagDetalleArtista
import com.example.vinilos.ui.artistas.TagErrorFavorito
import com.example.vinilos.ui.artistas.TagListaArtistas
import com.example.vinilos.ui.artistas.TagNombreDetalleArtista
import com.example.vinilos.ui.artistas.TagNoDisponibleFavorito
import com.example.vinilos.ui.artistas.TagReintentarFavorito
import com.example.vinilos.ui.artistas.tagArtista
import com.example.vinilos.ui.ingreso.TagAtras
import com.example.vinilos.ui.ingreso.TagEntrarColeccionista
import com.example.vinilos.ui.ingreso.TagEntrarVisitante
import com.example.vinilos.ui.ingreso.TagListaColeccionistas
import com.example.vinilos.ui.ingreso.tagColeccionista
import com.example.vinilos.ui.inicio.TagInicio
import com.example.vinilos.ui.inicio.TagVerArtistas
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.mockwebserver.SocketPolicy
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * E2E de la HU10 (agregar artistas favoritos), con los escenarios de la T10-5.
 *
 * FN10-ESC01 a ESC05 van contra el backend real. Como agregan favoritos, se corren contra BackVynils
 * local (`-PbaseUrl=http://10.0.2.2:3000/` en el emulador; en un teléfono, `http://localhost:3000/` con
 * `adb reverse tcp:3000 tcp:3000`). Cada escenario que agrega favoritos crea su propio coleccionista con
 * `POST /collectors` y lo borra en [borrarColeccionistasDePrueba] con `DELETE /collectors/{id}`, que
 * también borra sus favoritos, aunque la prueba falle. No se usa `DELETE /collectors/{id}/bands/{id}`
 * ni `.../musicians/{id}`: en BackVynils responden 204 sin quitar el favorito. Los artistas se eligen de
 * los datos reales; si no hay una banda o un músico, el escenario se omite.
 *
 * FN10-ESC06 a ESC08 (errores que el backend real no provoca a voluntad) usan `MockWebServer`, como
 * FN03-ESC05 y FN07-ESC07.
 *
 * Cada escenario guarda un pantallazo por paso, `informe.md` y un PDF en
 * `.../files/e2e-evidencia/FN10_ESCxx/` (en el dispositivo; los PDF se versionan a mano en
 * `docs/pruebas/sprint-2/HU10/e2e/`).
 */
class ArtistasFavoritosE2ETest {

    @get:Rule
    val rule = createEmptyComposeRule()

    /** El repositorio real de la app, que además cuenta las consultas y los envíos (design.md, D1). */
    private class FavoritosReales : FavoriteRepository(ServiceAdapter(BuildConfig.BASE_URL)) {
        @Volatile var consultas = 0
        @Volatile var envios = 0

        override suspend fun esFavorito(collectorId: Int, artistId: Int): Result<Boolean> {
            consultas++
            return super.esFavorito(collectorId, artistId)
        }

        override suspend fun agregarFavorito(collectorId: Int, artistId: Int, tipo: ArtistType): Result<Unit> {
            envios++
            return super.agregarFavorito(collectorId, artistId, tipo)
        }
    }

    private val favoritos = FavoritosReales()

    private val http = OkHttpClient()

    /** Coleccionistas creados por la prueba: se borran al terminar, con sus favoritos (D2). */
    private val coleccionistasDePrueba = mutableListOf<Int>()

    private var servidor: MockWebServer? = null

    @Before
    fun usarElBackendReal() {
        ApplicationProvider.getApplicationContext<VinilosApplication>().container =
            AppContainer(favoritosRepositorio = favoritos)
    }

    @After
    fun borrarColeccionistasDePrueba() {
        servidor?.shutdown()
        coleccionistasDePrueba.forEach { id ->
            http.newCall(Request.Builder().url("${BuildConfig.BASE_URL}collectors/$id").delete().build()).execute().close()
        }
    }

    // ---------- Datos reales (design.md, D3) ----------

    private fun artistasReales(): List<Artist> = runBlocking {
        ArtistRepository(ServiceAdapter(BuildConfig.BASE_URL)).obtenerArtistas().getOrThrow()
    }

    /** La primera banda o el primer músico del backend; si no hay, el escenario se omite. */
    private fun primerArtista(tipo: ArtistType): Artist {
        val artista = artistasReales().firstOrNull { it.type == tipo }
        Assume.assumeTrue("Sin datos en el backend: no hay ningún artista de tipo $tipo", artista != null)
        return artista!!
    }

    /** Crea un coleccionista sin favoritos con `POST /collectors` y lo registra para borrarlo (D2). */
    private fun crearColeccionista(): Int {
        val marca = System.currentTimeMillis()
        val cuerpo = JSONObject()
            .put("name", "E2E FN10 $marca")
            .put("telephone", "3000000000")
            .put("email", "e2e.fn10.$marca@example.com")
            .toString()
            .toRequestBody("application/json".toMediaType())
        val respuesta = http.newCall(Request.Builder().url("${BuildConfig.BASE_URL}collectors").post(cuerpo).build()).execute()
        val id = respuesta.use { JSONObject(requireNotNull(it.body).string()).getInt("id") }
        coleccionistasDePrueba += id
        return id
    }

    /** Agrega el favorito directo en el backend, sin pasar por la app (preparación de ESC05). */
    private fun agregarEnElBackend(coleccionista: Int, artista: Artist) {
        val ruta = if (artista.type == ArtistType.BANDA) "bands" else "musicians"
        val vacio = "".toRequestBody(null)
        http.newCall(
            Request.Builder().url("${BuildConfig.BASE_URL}collectors/$coleccionista/$ruta/${artista.id}").post(vacio).build()
        ).execute().use { assertTrue("No se pudo preparar el favorito", it.isSuccessful) }
    }

    /** Ids de los favoritos del coleccionista según `GET /collectors/{id}/performers`. */
    private fun favoritosEnElBackend(coleccionista: Int): Set<Int> {
        val respuesta = http.newCall(
            Request.Builder().url("${BuildConfig.BASE_URL}collectors/$coleccionista/performers").build()
        ).execute()
        val lista = respuesta.use { JSONArray(requireNotNull(it.body).string()) }
        return (0 until lista.length()).map { lista.getJSONObject(it).getInt("id") }.toSet()
    }

    // ---------- Navegación ----------

    private fun esperarA(tag: String, milis: Long = 15_000) =
        rule.waitUntil(milis) { rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }

    private fun esperarTexto(texto: String, milis: Long = 15_000) =
        rule.waitUntil(milis) { rule.onAllNodesWithText(texto).fetchSemanticsNodes().isNotEmpty() }

    private fun esperarQueDesaparezca(tag: String, milis: Long = 15_000) =
        rule.waitUntil(milis) { rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isEmpty() }

    /** Espera a que el botón de favorito muestre [texto]. */
    private fun esperarBoton(texto: String, milis: Long = 15_000) = rule.waitUntil(milis) {
        rule.onAllNodesWithTag(TagBotonFavorito).fetchSemanticsNodes().isNotEmpty() &&
            rule.onAllNodes(hasTestTag(TagBotonFavorito) and hasText(texto)).fetchSemanticsNodes().isNotEmpty()
    }

    private fun entrarComoVisitante() {
        esperarA(TagEntrarVisitante)
        rule.onNodeWithTag(TagEntrarVisitante).performScrollTo().performClick()
        esperarA(TagInicio)
    }

    private fun entrarComoColeccionista(coleccionista: Int) {
        esperarA(TagEntrarColeccionista)
        rule.onNodeWithTag(TagEntrarColeccionista).performScrollTo().performClick()
        // El coleccionista de la prueba suele quedar al final: se espera a que la lista cargue y se llegue a él.
        rule.waitUntil(15_000) {
            runCatching {
                rule.onNodeWithTag(TagListaColeccionistas).performScrollToNode(hasTestTag(tagColeccionista(coleccionista)))
            }.isSuccess
        }
        rule.onNodeWithTag(tagColeccionista(coleccionista)).performClick()
        esperarA(TagInicio)
    }

    /** Inicio -> "Ver artistas" -> fila de [artista], visible en el listado. */
    private fun verListado(artista: Artist, evidencia: String, paso: String) {
        rule.onNodeWithTag(TagVerArtistas).performScrollTo().performClick()
        // El contador aparece cuando el listado cargó; la fila puede estar más abajo, aún sin componer.
        esperarA(TagContadorArtistas)
        rule.onNodeWithTag(TagListaArtistas).performScrollToNode(hasTestTag(tagArtista(artista)))
        EvidenciaE2E.capturar(rule, evidencia, paso)
    }

    /** Inicio -> listado -> detalle de [artista] cargado. */
    private fun abrirDetalle(artista: Artist, evidencia: String, pasoListado: String) {
        verListado(artista, evidencia, pasoListado)
        rule.onNodeWithTag(tagArtista(artista)).performClick()
        esperarA(TagDetalleArtista)
        esperarA(TagNombreDetalleArtista)
        rule.onNodeWithTag(TagNombreDetalleArtista).assertTextEquals(artista.name)
    }

    /** Detalle -> listado -> inicio, con el botón atrás de cada pantalla. */
    private fun volverAlInicio() {
        rule.onNodeWithTag(TagAtras).performClick()
        esperarA(TagContadorArtistas)
        rule.onNodeWithTag(TagAtras).performClick()
        esperarA(TagInicio)
    }

    private fun boton() = rule.onNodeWithTag(TagBotonFavorito).performScrollTo()

    /** Toca "Agregar Favorito" y comprueba que queda "Favorito" con la confirmación, que se oculta sola. */
    private fun agregarYVerConfirmacion(artista: Artist, evidencia: String, pasoAntes: String, pasoDespues: String) {
        esperarBoton("Agregar Favorito")
        boton().assertIsEnabled()
        EvidenciaE2E.capturar(rule, evidencia, pasoAntes)

        boton().performClick()
        esperarA(TagConfirmacionFavorito, milis = 10_000)
        rule.onNodeWithText("${artista.name} se agregó a tus favoritos").assertIsDisplayed()
        boton().assert(hasText("Favorito")).assertIsNotEnabled()
        EvidenciaE2E.capturar(rule, evidencia, pasoDespues)

        // El mensaje es breve: desaparece solo y el botón sigue como "Favorito".
        esperarQueDesaparezca(TagConfirmacionFavorito, milis = 10_000)
        boton().assert(hasText("Favorito")).assertIsNotEnabled()
    }

    // ---------- Escenarios contra el backend real ----------

    /** FN10-ESC01: el Visitante no ve el botón de favorito en el detalle de una banda ni de un músico. */
    @Test
    fun elVisitanteNoVeElBotonDeFavorito() {
        val evidencia = "FN10_ESC01"
        val banda = primerArtista(ArtistType.BANDA)
        val musico = primerArtista(ArtistType.MUSICO)

        ActivityScenario.launch(MainActivity::class.java).use {
            entrarComoVisitante()
            abrirDetalle(banda, evidencia, "01_listado_visitante")
            rule.onAllNodesWithTag(TagBotonFavorito).assertCountEquals(0)
            EvidenciaE2E.capturar(rule, evidencia, "02_detalle_banda_sin_boton")

            volverAlInicio()
            abrirDetalle(musico, evidencia, "03_listado_visitante")
            rule.onAllNodesWithTag(TagBotonFavorito).assertCountEquals(0)
            EvidenciaE2E.capturar(rule, evidencia, "04_detalle_musico_sin_boton")
            assertEquals(0, favoritos.consultas)
        }
    }

    /** FN10-ESC02: el Coleccionista agrega una banda; el botón pasa a "Favorito" y el backend la guarda. */
    @Test
    fun elColeccionistaAgregaUnaBandaComoFavorita() {
        val evidencia = "FN10_ESC02"
        val banda = primerArtista(ArtistType.BANDA)
        val coleccionista = crearColeccionista()

        ActivityScenario.launch(MainActivity::class.java).use {
            entrarComoColeccionista(coleccionista)
            abrirDetalle(banda, evidencia, "01_listado_coleccionista")
            agregarYVerConfirmacion(banda, evidencia, "02_agregar_favorito", "03_favorito_con_confirmacion")
            EvidenciaE2E.capturar(rule, evidencia, "04_confirmacion_oculta")
        }
        assertEquals(1, favoritos.envios)
        assertTrue("El backend debe devolver la banda como favorita", banda.id in favoritosEnElBackend(coleccionista))
    }

    /** FN10-ESC03: el Coleccionista agrega un músico; el botón pasa a "Favorito" y el backend lo guarda. */
    @Test
    fun elColeccionistaAgregaUnMusicoComoFavorito() {
        val evidencia = "FN10_ESC03"
        val musico = primerArtista(ArtistType.MUSICO)
        val coleccionista = crearColeccionista()

        ActivityScenario.launch(MainActivity::class.java).use {
            entrarComoColeccionista(coleccionista)
            abrirDetalle(musico, evidencia, "01_listado_coleccionista")
            agregarYVerConfirmacion(musico, evidencia, "02_agregar_favorito", "03_favorito_con_confirmacion")
            EvidenciaE2E.capturar(rule, evidencia, "04_confirmacion_oculta")
        }
        assertEquals(1, favoritos.envios)
        assertTrue("El backend debe devolver el músico como favorito", musico.id in favoritosEnElBackend(coleccionista))
    }

    /** FN10-ESC04: tras agregar un favorito y volver a abrir la app, el botón sigue como "Favorito". */
    @Test
    fun alVolverAAbrirLaAppElBotonSigueComoFavorito() {
        val evidencia = "FN10_ESC04"
        val artista = primerArtista(ArtistType.BANDA)
        val coleccionista = crearColeccionista()

        ActivityScenario.launch(MainActivity::class.java).use {
            entrarComoColeccionista(coleccionista)
            abrirDetalle(artista, evidencia, "01_listado_coleccionista")
            agregarYVerConfirmacion(artista, evidencia, "02_agregar_favorito", "03_favorito_agregado")
        }
        val consultasAntes = favoritos.consultas

        // Se cierra la app y se vuelve a abrir: hay que ingresar de nuevo con el mismo coleccionista (D4).
        ActivityScenario.launch(MainActivity::class.java).use {
            entrarComoColeccionista(coleccionista)
            EvidenciaE2E.capturar(rule, evidencia, "04_app_abierta_de_nuevo")
            abrirDetalle(artista, evidencia, "05_listado_coleccionista")
            esperarBoton("Favorito")
            boton().assertIsNotEnabled()
            rule.onAllNodesWithTag(TagConfirmacionFavorito).assertCountEquals(0)
            EvidenciaE2E.capturar(rule, evidencia, "06_sigue_como_favorito")
        }
        assertTrue("Al volver a abrir el detalle se consulta de nuevo el backend", favoritos.consultas > consultasAntes)
        assertEquals(1, favoritos.envios)
    }

    /** FN10-ESC05: un artista que ya es favorito aparece como "Favorito" y desactivado, sin enviar nada. */
    @Test
    fun unArtistaQueYaEsFavoritoApareceDesactivado() {
        val evidencia = "FN10_ESC05"
        val artista = primerArtista(ArtistType.MUSICO)
        val coleccionista = crearColeccionista()
        agregarEnElBackend(coleccionista, artista)

        ActivityScenario.launch(MainActivity::class.java).use {
            entrarComoColeccionista(coleccionista)
            abrirDetalle(artista, evidencia, "01_listado_coleccionista")
            esperarBoton("Favorito")
            boton().assertIsNotEnabled()
            rule.onNodeWithText("Este artista ya está en tus favoritos.").assertIsDisplayed()
            EvidenciaE2E.capturar(rule, evidencia, "02_ya_es_favorito")
            boton().performClick()
            rule.waitForIdle()
        }
        assertEquals(0, favoritos.envios)
    }

    // ---------- Escenarios con MockWebServer (design.md, D5) ----------

    private val ruben = Artist(100, "Rubén Blades Bellido de Luna", null, ArtistType.MUSICO)
    private val queen = Artist(101, "Queen", null, ArtistType.BANDA)

    /** Estado del servidor simulado, que cada escenario cambia a mitad de la prueba. */
    private class Simulado {
        @Volatile var comprobacionCaida = false
        @Volatile var respuestaGuardado: () -> MockResponse = { MockResponse().setBody("""{"id": 100}""") }
        @Volatile var rubenExiste = true
        @Volatile var envios = 0
    }

    /** Coleccionista 100 sin favoritos, Rubén Blades (músico) y Queen (banda). */
    private fun iniciarServidor(simulado: Simulado) {
        val musicos = { if (simulado.rubenExiste) """[{"id": 100, "name": "${ruben.name}", "image": null}]""" else "[]" }
        val detalleRuben = """
            {"id": 100, "name": "${ruben.name}", "image": null,
             "description": "Cantante, compositor y actor panameño.",
             "birthDate": "1948-07-16T00:00:00.000Z", "albums": [], "performerPrizes": []}
        """.trimIndent()
        val nuevo = MockWebServer().apply {
            dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse = when {
                    request.path == "/collectors" -> MockResponse().setBody(
                        """[{"id": 100, "name": "Manolo Bellon", "email": "manollo@caracol.com.co"}]"""
                    )
                    request.path == "/collectors/100/performers" ->
                        if (simulado.comprobacionCaida) MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START)
                        else MockResponse().setBody("[]")
                    request.path == "/collectors/100/musicians/100" && request.method == "POST" -> {
                        simulado.envios++
                        simulado.respuestaGuardado()
                    }
                    request.path == "/musicians" -> MockResponse().setBody(musicos())
                    request.path == "/bands" -> MockResponse().setBody("""[{"id": 101, "name": "Queen", "image": null}]""")
                    request.path == "/musicians/100" -> MockResponse().setBody(detalleRuben)
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

    /** FN10-ESC06: si no se puede comprobar el favorito, el botón queda desactivado y "Reintentar" lo recupera. */
    @Test
    fun siNoSePuedeComprobarElBotonQuedaDesactivadoYReintentarLoRecupera() {
        val evidencia = "FN10_ESC06"
        val simulado = Simulado().apply { comprobacionCaida = true }
        iniciarServidor(simulado)

        ActivityScenario.launch(MainActivity::class.java).use {
            entrarComoColeccionista(100)
            abrirDetalle(ruben, evidencia, "01_listado_coleccionista")
            esperarA(TagReintentarFavorito)
            rule.onNodeWithText("No pudimos comprobar si es tu favorito").assertIsDisplayed()
            boton().assertIsNotEnabled()
            rule.onNodeWithTag(TagReintentarFavorito).performScrollTo().assertIsDisplayed()
            EvidenciaE2E.capturar(rule, evidencia, "02_no_se_pudo_comprobar")

            // El backend "vuelve" y la persona reintenta.
            simulado.comprobacionCaida = false
            rule.onNodeWithTag(TagReintentarFavorito).performClick()
            esperarBoton("Agregar Favorito")
            boton().assertIsEnabled()
            rule.onAllNodesWithTag(TagReintentarFavorito).assertCountEquals(0)
            EvidenciaE2E.capturar(rule, evidencia, "03_comprobado_al_reintentar")
        }
        assertEquals(0, simulado.envios)
    }

    /**
     * FN10-ESC07: mientras se guarda se ve "Guardando..." y un segundo toque no envía nada; si el guardado
     * falla aparece el error sin confirmación, y el reintento lo guarda.
     */
    @Test
    fun siFallaElGuardadoSeVeElErrorYReintentarLoGuarda() {
        val evidencia = "FN10_ESC07"
        // El primer envío tarda (D6) y termina en error del servicio; los siguientes salen bien.
        val simulado = Simulado().apply {
            respuestaGuardado = {
                if (envios == 1) MockResponse().setResponseCode(500).setHeadersDelay(3, TimeUnit.SECONDS)
                else MockResponse().setBody("""{"id": 100}""")
            }
        }
        iniciarServidor(simulado)

        ActivityScenario.launch(MainActivity::class.java).use {
            entrarComoColeccionista(100)
            abrirDetalle(ruben, evidencia, "01_listado_coleccionista")
            esperarBoton("Agregar Favorito")
            EvidenciaE2E.capturar(rule, evidencia, "02_agregar_favorito")

            boton().performClick()
            esperarBoton("Guardando...", milis = 2_000)
            boton().assertIsNotEnabled()
            EvidenciaE2E.capturar(rule, evidencia, "03_guardando")
            boton().performClick()

            esperarA(TagErrorFavorito)
            rule.onNodeWithText("No se pudo guardar el favorito, intenta de nuevo").assertIsDisplayed()
            rule.onAllNodesWithTag(TagConfirmacionFavorito).assertCountEquals(0)
            assertEquals("El toque durante \"Guardando...\" no debe enviar otra petición", 1, simulado.envios)
            rule.onNodeWithText("Reintentar guardar").performScrollTo().assertIsEnabled()
            EvidenciaE2E.capturar(rule, evidencia, "04_error_al_guardar")

            rule.onNodeWithText("Reintentar guardar").performClick()
            esperarA(TagConfirmacionFavorito, milis = 10_000)
            rule.onNodeWithText("${ruben.name} se agregó a tus favoritos").assertIsDisplayed()
            boton().assert(hasText("Favorito")).assertIsNotEnabled()
            EvidenciaE2E.capturar(rule, evidencia, "05_guardado_al_reintentar")
        }
        assertEquals(2, simulado.envios)
    }

    /** FN10-ESC08: si el artista ya no existe, se avisa y "Volver al listado" lo muestra sin ese artista. */
    @Test
    fun siElArtistaYaNoExisteSeAvisaYElListadoSeActualiza() {
        val evidencia = "FN10_ESC08"
        val simulado = Simulado()
        // Al guardar, el backend ya no tiene al artista: responde 404 y deja de listarlo.
        simulado.respuestaGuardado = {
            simulado.rubenExiste = false
            MockResponse().setResponseCode(404)
        }
        iniciarServidor(simulado)

        ActivityScenario.launch(MainActivity::class.java).use {
            entrarComoColeccionista(100)
            abrirDetalle(ruben, evidencia, "01_listado_con_el_artista")
            esperarBoton("Agregar Favorito")
            boton().performClick()

            esperarA(TagNoDisponibleFavorito)
            rule.onNodeWithText("El artista ya no está disponible").assertIsDisplayed()
            rule.onAllNodesWithTag(TagConfirmacionFavorito).assertCountEquals(0)
            rule.onNodeWithText("Volver al listado de artistas").performScrollTo().assertIsDisplayed()
            EvidenciaE2E.capturar(rule, evidencia, "02_artista_no_disponible")

            rule.onNodeWithText("Volver al listado de artistas").performClick()
            esperarA(TagContadorArtistas)
            rule.waitUntil(15_000) { rule.onAllNodesWithTag(tagArtista(ruben)).fetchSemanticsNodes().isEmpty() }
            rule.onAllNodesWithTag(tagArtista(ruben)).assertCountEquals(0)
            rule.onNodeWithTag(TagListaArtistas).performScrollToNode(hasTestTag(tagArtista(queen)))
            rule.onNodeWithTag(tagArtista(queen)).assertIsDisplayed()
            EvidenciaE2E.capturar(rule, evidencia, "03_listado_sin_el_artista")
        }
        assertEquals(1, simulado.envios)
    }
}
