package com.example.vinilos

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasContentDescription
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
import androidx.test.espresso.Espresso
import com.example.vinilos.data.model.Artist
import com.example.vinilos.data.model.ArtistType
import com.example.vinilos.data.model.ArtistaDetalle
import com.example.vinilos.data.remote.ServiceAdapter
import com.example.vinilos.data.repository.ArtistRepository
import com.example.vinilos.e2e.EvidenciaE2E
import com.example.vinilos.ui.artistas.TagArtistas
import com.example.vinilos.ui.artistas.TagContadorAlbumes
import com.example.vinilos.ui.artistas.TagContadorArtistas
import com.example.vinilos.ui.artistas.TagContadorIntegrantes
import com.example.vinilos.ui.artistas.TagContadorPremios
import com.example.vinilos.ui.artistas.TagDescripcionDetalleArtista
import com.example.vinilos.ui.artistas.TagDetalleArtista
import com.example.vinilos.ui.artistas.TagErrorArtistas
import com.example.vinilos.ui.artistas.TagFechaDetalleArtista
import com.example.vinilos.ui.artistas.TagListaArtistas
import com.example.vinilos.ui.artistas.TagListaDetalleArtista
import com.example.vinilos.ui.artistas.TagNombreDetalleArtista
import com.example.vinilos.ui.artistas.TagReintentarArtistas
import com.example.vinilos.ui.artistas.TagTipoDetalleArtista
import com.example.vinilos.ui.artistas.tagAlbumArtista
import com.example.vinilos.ui.artistas.tagArtista
import com.example.vinilos.ui.artistas.tagIntegrante
import com.example.vinilos.ui.ingreso.TagEntrarVisitante
import com.example.vinilos.ui.inicio.TagInicio
import com.example.vinilos.ui.inicio.TagVerArtistas
import kotlinx.coroutines.runBlocking
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
 * E2E de la HU04 (detalle del artista) como Visitante, desde el listado de artistas (HU03).
 *
 * FN04-ESC01 a ESC04 y ESC07 van contra el backend real y no asumen un dataset fijo: antes de abrir
 * la app consultan el listado y los detalles reales y eligen el artista que cumple la condición del
 * escenario; si no hay ninguno, el escenario se omite. Así pasan contra el backend local (Docker) y
 * contra el desplegado por los tutores. En el emulador la app llega al backend local por
 * `10.0.2.2`; en un teléfono, con `adb reverse tcp:3000 tcp:3000` y `-PbaseUrl=http://localhost:3000/`.
 *
 * FN04-ESC05 (error de carga) y ESC06 (banda con integrantes, que no existe en ninguno de los dos
 * backends) usan `MockWebServer`, como [RecuperacionTest].
 *
 * Cada escenario guarda un pantallazo por paso, `informe.md` y un PDF en
 * `.../files/e2e-evidencia/FN04_ESCxx/` (en el dispositivo; los PDF se versionan a mano en
 * `docs/pruebas/sprint-2/HU04/e2e/`).
 */
class ArtistaDetalleE2ETest {

    @get:Rule
    val rule = createEmptyComposeRule()

    /** El repositorio real de la app, que además cuenta cuántas veces se consulta cada detalle. */
    private class ArtistasReales(baseUrl: String = BuildConfig.BASE_URL) : ArtistRepository(ServiceAdapter(baseUrl)) {
        val consultasDetalle = mutableListOf<Pair<ArtistType, Int>>()

        override suspend fun obtenerDetalle(tipo: ArtistType, id: Int): Result<ArtistaDetalle> {
            synchronized(consultasDetalle) { consultasDetalle += tipo to id }
            return super.obtenerDetalle(tipo, id)
        }

        fun consultasDe(artista: Artist) =
            synchronized(consultasDetalle) { consultasDetalle.count { it == artista.type to artista.id } }
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

    // ---------- Datos reales ----------

    /** Un artista del listado real junto con su detalle real. */
    private data class Candidato(val artista: Artist, val detalle: ArtistaDetalle)

    /** Consulta, con un repositorio aparte (no cuenta), el listado y el detalle de cada artista real. */
    private fun candidatosReales(): List<Candidato> = runBlocking {
        val consulta = ArtistRepository(ServiceAdapter(BuildConfig.BASE_URL))
        consulta.obtenerArtistas().getOrThrow().mapNotNull { artista ->
            consulta.obtenerDetalle(artista.type, artista.id).getOrNull()?.let { Candidato(artista, it) }
        }
    }

    /** El primero que cumple [condicion], o el escenario se omite explicando qué faltó. */
    private fun elegir(motivo: String, condicion: (Candidato) -> Boolean): Candidato {
        val elegido = candidatosReales().firstOrNull(condicion)
        Assume.assumeTrue("El backend conectado no tiene $motivo", elegido != null)
        return requireNotNull(elegido)
    }

    /** El que tenga premios y, entre ellos, más álbumes: muestra el mayor número de secciones con datos. */
    private fun masCompleto(tipo: ArtistType, motivo: String): Candidato {
        val elegido = candidatosReales()
            .filter { it.artista.type == tipo }
            .maxWithOrNull(compareBy<Candidato>({ it.detalle.premios.size }, { it.detalle.albumes.size }))
        Assume.assumeTrue("El backend conectado no tiene $motivo", elegido != null)
        return requireNotNull(elegido)
    }

    // ---------- Navegación ----------

    private fun esperarA(tag: String, milis: Long = 15_000) =
        rule.waitUntil(milis) { rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }

    private fun esperarTexto(texto: String, milis: Long = 15_000) =
        rule.waitUntil(milis) { rule.onAllNodesWithText(texto).fetchSemanticsNodes().isNotEmpty() }

    private fun esperarQueDesaparezca(tag: String, milis: Long = 5_000) =
        rule.waitUntil(milis) { rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isEmpty() }

    /** Visitante -> inicio -> "Ver artistas" -> fila del [artista] -> detalle cargado. */
    private fun abrirDetalle(artista: Artist, evidencia: String) {
        esperarA(TagEntrarVisitante)
        rule.onNodeWithTag(TagEntrarVisitante).performScrollTo().performClick()
        esperarA(TagInicio)
        EvidenciaE2E.capturar(rule, evidencia, "01_inicio_visitante")
        rule.onNodeWithTag(TagVerArtistas).performScrollTo().performClick()
        // El contador aparece cuando el listado cargó; la fila puede estar más abajo, aún sin componer.
        esperarA(TagContadorArtistas)
        rule.onNodeWithTag(TagListaArtistas).performScrollToNode(hasTestTag(tagArtista(artista)))
        EvidenciaE2E.capturar(rule, evidencia, "02_listado_artistas")
        rule.onNodeWithTag(tagArtista(artista)).performClick()
        esperarA(TagDetalleArtista)
        esperarQueDesaparezca(TagArtistas)
    }

    private fun esperarDetalle(nombre: String) {
        esperarA(TagNombreDetalleArtista)
        rule.onNodeWithTag(TagNombreDetalleArtista).assertTextEquals(nombre)
    }

    /** Desplaza el detalle hasta el nodo (en pantallas pequeñas puede no estar compuesto aún). */
    private fun desplazarA(matcher: SemanticsMatcher) {
        rule.onNodeWithTag(TagListaDetalleArtista).performScrollToNode(matcher)
    }

    private fun verTexto(texto: String) {
        desplazarA(hasText(texto))
        rule.onAllNodesWithText(texto)[0].assertIsDisplayed()
    }

    private fun contador(cantidad: Int, singular: String, plural: String) =
        if (cantidad == 1) "1 $singular" else "$cantidad $plural"

    private fun etiquetaDe(tipo: ArtistType) = if (tipo == ArtistType.BANDA) "Banda" else "Músico"

    // ---------- Verificaciones del detalle ----------

    /** Imagen (real o por defecto), nombre, etiqueta, fecha con su rótulo y descripción. */
    private fun verificarTarjeta(detalle: ArtistaDetalle) {
        esperarDetalle(detalle.nombre)
        rule.onNodeWithTag(TagTipoDetalleArtista).assertTextEquals(etiquetaDe(detalle.tipo))
        rule.onNode(hasContentDescription("Imagen de ${detalle.nombre}")).assertExists()
        detalle.fecha?.let { fecha ->
            val rotulo = if (detalle.tipo == ArtistType.BANDA) "FECHA DE CREACIÓN" else "FECHA DE NACIMIENTO"
            rule.onNodeWithText(rotulo).assertIsDisplayed()
            rule.onNodeWithTag(TagFechaDetalleArtista).assertTextEquals(fecha)
        }
        detalle.descripcion?.let { rule.onNodeWithTag(TagDescripcionDetalleArtista).assertTextEquals(it) }
    }

    private fun verificarIntegrantes(detalle: ArtistaDetalle) {
        if (detalle.integrantes.isEmpty()) {
            verTexto("Sin integrantes")
            return
        }
        desplazarA(hasTestTag(TagContadorIntegrantes))
        rule.onNodeWithTag(TagContadorIntegrantes)
            .assertTextEquals(contador(detalle.integrantes.size, "integrante", "integrantes"))
        detalle.integrantes.forEach { integrante ->
            desplazarA(hasTestTag(tagIntegrante(integrante.id)))
            rule.onNode(
                hasTestTag(tagIntegrante(integrante.id)) and hasAnyDescendant(hasText(integrante.nombre)),
                useUnmergedTree = true
            ).assertExists()
            rule.onNode(hasContentDescription("Imagen de ${integrante.nombre}"), useUnmergedTree = true).assertExists()
        }
    }

    /** Álbumes con portada (o respaldo), nombre y año, en el orden de la app. */
    private fun verificarAlbumes(detalle: ArtistaDetalle) {
        if (detalle.albumes.isEmpty()) {
            verTexto("Sin álbumes")
            return
        }
        desplazarA(hasTestTag(TagContadorAlbumes))
        rule.onNodeWithTag(TagContadorAlbumes)
            .assertTextEquals(contador(detalle.albumes.size, "álbum", "álbumes"))
        detalle.albumes.forEach { album ->
            desplazarA(hasTestTag(tagAlbumArtista(album.id)))
            // La fila es clicable: fusiona el nombre, el año y la descripción de la portada (o del respaldo).
            rule.onNode(hasTestTag(tagAlbumArtista(album.id)) and hasText(album.nombre)).assertIsDisplayed()
            album.anio?.let { rule.onNode(hasTestTag(tagAlbumArtista(album.id)) and hasText(it)).assertExists() }
        }
    }

    private fun verificarPremios(detalle: ArtistaDetalle) {
        if (detalle.premios.isEmpty()) {
            verTexto("Sin premios")
            return
        }
        desplazarA(hasTestTag(TagContadorPremios))
        rule.onNodeWithTag(TagContadorPremios)
            .assertTextEquals(contador(detalle.premios.size, "premio", "premios"))
        detalle.premios.forEach { premio ->
            verTexto(premio.nombre)
            verTexto(premio.organizacion)
            premio.fecha?.let { verTexto(it) }
        }
    }

    // ---------- Escenarios contra el backend real ----------

    /** FN04-ESC01: el detalle de un músico muestra sus datos, su fecha de nacimiento, álbumes y premios. */
    @Test
    fun elDetalleDeUnMusicoMuestraSusDatosAlbumesYPremios() {
        val evidencia = "FN04_ESC01"
        val (musico, detalle) = masCompleto(ArtistType.MUSICO, "músicos")

        ActivityScenario.launch(MainActivity::class.java).use {
            abrirDetalle(musico, evidencia)
            verificarTarjeta(detalle)
            EvidenciaE2E.capturar(rule, evidencia, "03_tarjeta_musico")

            verificarAlbumes(detalle)
            EvidenciaE2E.capturar(rule, evidencia, "04_albumes")

            verificarPremios(detalle)
            EvidenciaE2E.capturar(rule, evidencia, "05_premios")
            rule.onAllNodesWithText("Integrantes").assertCountEquals(0)
        }
    }

    /** FN04-ESC02: el detalle de una banda muestra su fecha de creación, integrantes, álbumes y premios. */
    @Test
    fun elDetalleDeUnaBandaMuestraSuFechaDeCreacionIntegrantesAlbumesYPremios() {
        val evidencia = "FN04_ESC02"
        val (banda, detalle) = masCompleto(ArtistType.BANDA, "bandas")

        ActivityScenario.launch(MainActivity::class.java).use {
            abrirDetalle(banda, evidencia)
            verificarTarjeta(detalle)
            EvidenciaE2E.capturar(rule, evidencia, "03_tarjeta_banda")

            verificarIntegrantes(detalle)
            EvidenciaE2E.capturar(rule, evidencia, "04_integrantes")

            verificarAlbumes(detalle)
            EvidenciaE2E.capturar(rule, evidencia, "05_albumes")

            verificarPremios(detalle)
            EvidenciaE2E.capturar(rule, evidencia, "06_premios")
        }
    }

    /** FN04-ESC03: tocar un álbum abre su detalle (HU02) y "Atrás" vuelve al artista sin repetir la consulta. */
    @Test
    fun tocarUnAlbumAbreSuDetalleYAtrasVuelveAlArtistaSinRepetirLaConsulta() {
        val evidencia = "FN04_ESC03"
        val (artista, detalle) = elegir("artistas con álbumes") { it.detalle.albumes.isNotEmpty() }
        val album = detalle.albumes.first()

        ActivityScenario.launch(MainActivity::class.java).use {
            abrirDetalle(artista, evidencia)
            esperarDetalle(detalle.nombre)
            desplazarA(hasTestTag(tagAlbumArtista(album.id)))
            EvidenciaE2E.capturar(rule, evidencia, "03_album_en_el_detalle")

            rule.onNodeWithTag(tagAlbumArtista(album.id)).performClick()
            esperarQueDesaparezca(TagDetalleArtista)
            esperarTexto(album.nombre)
            rule.onAllNodesWithText(album.nombre)[0].assertIsDisplayed()
            EvidenciaE2E.capturar(rule, evidencia, "04_detalle_del_album")

            Espresso.pressBack()
            esperarDetalle(detalle.nombre)
            assertEquals(1, artistas.consultasDe(artista))
            EvidenciaE2E.capturar(rule, evidencia, "05_regreso_al_artista")
        }
    }

    /** FN04-ESC04: un artista sin álbumes ni premios muestra "Sin álbumes" y "Sin premios". */
    @Test
    fun unArtistaSinAlbumesNiPremiosMuestraLosMensajesDeVacio() {
        val evidencia = "FN04_ESC04"
        val (artista, detalle) = elegir("artistas sin álbumes ni premios") {
            it.detalle.albumes.isEmpty() && it.detalle.premios.isEmpty()
        }

        ActivityScenario.launch(MainActivity::class.java).use {
            abrirDetalle(artista, evidencia)
            verificarTarjeta(detalle)
            verTexto("Sin álbumes")
            verTexto("Sin premios")
            rule.onAllNodesWithTag(TagContadorAlbumes).assertCountEquals(0)
            rule.onAllNodesWithTag(TagContadorPremios).assertCountEquals(0)
            EvidenciaE2E.capturar(rule, evidencia, "03_sin_albumes_ni_premios")
        }
    }

    /** FN04-ESC07: al girar la pantalla el detalle se conserva sin volver a consultar el backend. */
    @Test
    fun alGirarLaPantallaElDetalleSeConservaSinVolverAConsultar() {
        val evidencia = "FN04_ESC07"
        val (artista, detalle) = masCompleto(ArtistType.MUSICO, "músicos")

        ActivityScenario.launch(MainActivity::class.java).use { escenario ->
            abrirDetalle(artista, evidencia)
            verificarTarjeta(detalle)
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
            rule.waitForIdle()

            esperarDetalle(detalle.nombre)
            rule.onNodeWithTag(TagTipoDetalleArtista).assertTextEquals(etiquetaDe(detalle.tipo))
            assertEquals(1, artistas.consultasDe(artista))
            EvidenciaE2E.capturar(rule, evidencia, "04_despues_de_girar")
        }
    }

    // ---------- Escenarios con MockWebServer ----------

    /**
     * Servidor local con un listado de un solo artista y su detalle. [caido] hace que el detalle corte
     * la conexión, como un backend apagado; el listado responde siempre para poder llegar al detalle.
     */
    private fun iniciarServidor(
        artista: Artist,
        detalleJson: String,
        caido: () -> Boolean = { false }
    ) {
        val fila = """[{"id": ${artista.id}, "name": "${artista.name}", "image": null}]"""
        val rutaDetalle = if (artista.type == ArtistType.BANDA) "/bands/${artista.id}" else "/musicians/${artista.id}"
        val nuevo = MockWebServer().apply {
            dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse = when (request.path) {
                    "/musicians" -> MockResponse().setBody(if (artista.type == ArtistType.MUSICO) fila else "[]")
                    "/bands" -> MockResponse().setBody(if (artista.type == ArtistType.BANDA) fila else "[]")
                    rutaDetalle ->
                        if (caido()) MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START)
                        else MockResponse().setBody(detalleJson)
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

    /** FN04-ESC05: si falla la carga se ve "No se pudo cargar el artista" y "Reintentar" lo recupera. */
    @Test
    fun siFallaLaCargaSeVeElErrorYReintentarRecuperaElDetalle() {
        val evidencia = "FN04_ESC05"
        val ruben = Artist(100, "Rubén Blades Bellido de Luna", null, ArtistType.MUSICO)
        var caido = true
        iniciarServidor(
            ruben,
            """
            {"id": 100, "name": "Rubén Blades Bellido de Luna", "image": null,
             "description": "Cantante, compositor y actor panameño.",
             "birthDate": "1948-07-16T00:00:00.000Z",
             "albums": [], "performerPrizes": []}
            """.trimIndent(),
            caido = { caido }
        )

        ActivityScenario.launch(MainActivity::class.java).use {
            abrirDetalle(ruben, evidencia)
            esperarA(TagErrorArtistas)
            rule.onNodeWithText("No se pudo cargar el artista").assertIsDisplayed()
            rule.onNodeWithTag(TagReintentarArtistas).assertIsDisplayed()
            EvidenciaE2E.capturar(rule, evidencia, "03_error_de_carga")

            // El backend "vuelve" y la persona pulsa Reintentar.
            caido = false
            rule.onNodeWithTag(TagReintentarArtistas).performClick()

            esperarDetalle("Rubén Blades Bellido de Luna")
            rule.onNodeWithTag(TagFechaDetalleArtista).assertTextEquals("16 de julio de 1948")
            rule.onAllNodesWithTag(TagErrorArtistas).assertCountEquals(0)
            EvidenciaE2E.capturar(rule, evidencia, "04_recuperado")
        }
    }

    /** FN04-ESC06: una banda con integrantes los muestra con imagen (o la de por defecto) y nombre. */
    @Test
    fun unaBandaConIntegrantesLosMuestraConImagenYNombre() {
        val evidencia = "FN04_ESC06"
        val queen = Artist(101, "Queen", null, ArtistType.BANDA)
        iniciarServidor(
            queen,
            """
            {"id": 101, "name": "Queen", "image": null,
             "description": "Banda británica de rock formada en Londres en 1970.",
             "creationDate": "1970-01-01T00:00:00.000Z",
             "musicians": [
               {"id": 1, "name": "Freddie Mercury", "image": null},
               {"id": 2, "name": "Brian May", "image": "http://localhost:1/no-existe.png"}
             ],
             "albums": [], "performerPrizes": []}
            """.trimIndent()
        )

        ActivityScenario.launch(MainActivity::class.java).use {
            abrirDetalle(queen, evidencia)
            esperarDetalle("Queen")
            rule.onNodeWithTag(TagTipoDetalleArtista).assertTextEquals("Banda")
            rule.onNodeWithText("FECHA DE CREACIÓN").assertIsDisplayed()
            rule.onNodeWithTag(TagFechaDetalleArtista).assertTextEquals("1 de enero de 1970")

            desplazarA(hasTestTag(TagContadorIntegrantes))
            rule.onNodeWithTag(TagContadorIntegrantes).assertTextEquals("2 integrantes")
            listOf(1 to "Freddie Mercury", 2 to "Brian May").forEach { (id, nombre) ->
                desplazarA(hasTestTag(tagIntegrante(id)))
                rule.onNode(hasTestTag(tagIntegrante(id)) and hasAnyDescendant(hasText(nombre)), useUnmergedTree = true)
                    .assertExists()
                // Sin imagen, o con una URL que no carga, se ve la imagen por defecto con su descripción.
                rule.onNode(hasContentDescription("Imagen de $nombre"), useUnmergedTree = true).assertExists()
            }
            EvidenciaE2E.capturar(rule, evidencia, "03_integrantes")
        }
    }
}
