package com.example.vinilos

import android.os.SystemClock
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
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
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import com.example.vinilos.data.model.AlbumResumen
import com.example.vinilos.data.remote.ServiceAdapter
import com.example.vinilos.data.repository.AlbumRepository
import com.example.vinilos.e2e.EvidenciaE2E
import com.example.vinilos.ui.catalogo.TagActualizandoCatalogo
import com.example.vinilos.ui.catalogo.TagContadorCatalogo
import com.example.vinilos.ui.catalogo.TagCuadriculaCatalogo
import com.example.vinilos.ui.catalogo.TagErrorCatalogo
import com.example.vinilos.ui.catalogo.TagProgresoCatalogo
import com.example.vinilos.ui.catalogo.TagReintentarCatalogo
import com.example.vinilos.ui.catalogo.TagRolActivo
import com.example.vinilos.ui.catalogo.tagAlbum
import com.example.vinilos.ui.catalogo.tagsDeAlbumesRecorriendo
import com.example.vinilos.ui.ingreso.TagAtras
import com.example.vinilos.ui.ingreso.TagEntrarColeccionista
import com.example.vinilos.ui.ingreso.TagEntrarVisitante
import com.example.vinilos.ui.ingreso.tagColeccionista
import com.example.vinilos.ui.inicio.TagInicio
import com.example.vinilos.ui.inicio.TagVerAlbumes
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * E2E de la HU01 (catálogo de álbumes) contra el backend real BackVynils.
 *
 * No asume un dataset fijo (como los 4 álbumes de `sql/data.sql`): envuelve el repositorio real
 * para capturar el catálogo que el backend realmente devuelve y calcula las aserciones (cantidad,
 * orden, nombre/género/año de cada álbum) a partir de esos datos capturados. Así la prueba es
 * coherente tanto con el backend local (Docker) como con el desplegado por los tutores, sin
 * importar cuántos álbumes tenga cada uno.
 *
 * FN01-ESC03 (backend caído) sigue usando `MockWebServer` con datos fabricados por la prueba,
 * porque apagar el backend real dentro de la prueba no es confiable y ese caso no depende del
 * dataset real; no necesitaba cambiar.
 *
 * En un teléfono requiere `adb reverse tcp:3000 tcp:3000` y la app instalada con
 * `-PbaseUrl=http://localhost:3000/`. Los escenarios FN01-ESC01 a FN01-ESC05 guardan un pantallazo
 * por paso, una fila en el informe y un PDF en su propia carpeta,
 * `.../files/e2e-evidencia/FN01_ESCxx/` (solo en el dispositivo, no en el repo).
 */
class CatalogoE2ETest {

    @get:Rule
    val rule = createEmptyComposeRule()

    /** El repositorio real de la app: cuenta las consultas y guarda el último catálogo recibido. */
    private class AlbumesRealesContados : AlbumRepository(ServiceAdapter(BuildConfig.BASE_URL)) {
        @Volatile var consultasCatalogo = 0
        @Volatile var catalogo: List<AlbumResumen>? = null

        override suspend fun obtenerAlbumes(): Result<List<AlbumResumen>> {
            consultasCatalogo++
            return super.obtenerAlbumes().also { resultado -> resultado.onSuccess { catalogo = it } }
        }
    }

    private val albumes = AlbumesRealesContados()

    @Before
    fun usarElBackendReal() {
        ApplicationProvider.getApplicationContext<VinilosApplication>().container =
            AppContainer(albumesRepositorio = albumes)
    }

    private fun esperarA(tag: String, milis: Long = 15_000) =
        rule.waitUntil(milis) { rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }

    /** Da tiempo a que Coil pinte las portadas antes del pantallazo. */
    private fun capturar(evidencia: String, paso: String) {
        SystemClock.sleep(2_000)
        EvidenciaE2E.capturar(rule, evidencia, paso)
    }

    private fun esperarElCatalogoReal(): List<AlbumResumen> {
        rule.waitUntil(15_000) { albumes.catalogo != null }
        return requireNotNull(albumes.catalogo)
    }

    /** Desde el inicio (que queda en la evidencia como [pasoInicio]) abre el catálogo real. */
    private fun verAlbumes(evidencia: String, pasoInicio: String): List<AlbumResumen> {
        esperarA(TagInicio)
        EvidenciaE2E.capturar(rule, evidencia, pasoInicio)
        rule.onNodeWithTag(TagVerAlbumes).performScrollTo().performClick()
        val catalogo = esperarElCatalogoReal()
        esperarA(tagAlbum(catalogo.first().id))
        return catalogo
    }

    private fun verAlbum(id: Int) =
        rule.onNodeWithTag(TagCuadriculaCatalogo).performScrollToNode(hasTestTag(tagAlbum(id)))

    private fun textoContador(cantidad: Int) =
        if (cantidad == 1) "1 lanzamiento" else "$cantidad lanzamientos"

    /**
     * Una muestra acotada del catálogo real: los primeros [tamano] y el último. El backend
     * desplegado puede tener cientos de álbumes, y recorrerlos todos en una prueba E2E (que ya de
     * por sí hace esperas e IO real) la haría impráctica; esta muestra alcanza para comprobar que
     * los datos reales llegan bien a la pantalla sin recorrer el catálogo completo.
     */
    private fun muestraDelCatalogo(catalogo: List<AlbumResumen>, tamano: Int = 5): List<AlbumResumen> {
        val primeros = catalogo.take(tamano)
        val ultimo = catalogo.lastOrNull()
        return if (ultimo != null && ultimo !in primeros) primeros + ultimo else primeros
    }

    /** Comprueba, sobre una muestra del catálogo real, que se vea con los mismos datos y en orden. */
    private fun comprobarLosAlbumesDelCatalogoReal(catalogo: List<AlbumResumen>) {
        rule.onNodeWithTag(TagContadorCatalogo).assertTextEquals(textoContador(catalogo.size))
        val muestra = muestraDelCatalogo(catalogo)
        muestra.forEach { album ->
            verAlbum(album.id)
            val generoYAnio = if (album.anio != null) "${album.genero} · ${album.anio}" else album.genero
            var matcher: SemanticsMatcher = hasTestTag(tagAlbum(album.id))
            matcher = matcher and hasText(album.nombre)
            matcher = matcher and hasText(generoYAnio)
            matcher = matcher and hasContentDescription("Portada de ${album.nombre}")
            rule.onNode(matcher).assertIsDisplayed()
        }
        val esperados = muestra.map { tagAlbum(it.id) }
        assertEquals(esperados, rule.tagsDeAlbumesRecorriendo(esperados).filter { it in esperados })
    }

    /** FN01-ESC01: Visitante y Coleccionista ven el mismo catálogo real, en orden, con portada, nombre y género. */
    @Test
    fun visitanteYColeccionistaVenLosCuatroAlbumesEnOrden() {
        val evidencia = "FN01_ESC01"
        ActivityScenario.launch(MainActivity::class.java).use {
            rule.onNodeWithTag(TagEntrarVisitante).performScrollTo().performClick()
            val catalogoVisitante = verAlbumes(evidencia, "01_inicio_visitante")
            rule.onNodeWithTag(TagRolActivo).assertTextEquals("VISITANTE")
            capturar(evidencia, "02_catalogo_visitante")
            comprobarLosAlbumesDelCatalogoReal(catalogoVisitante)

            // Volver al ingreso y entrar como coleccionista.
            rule.onNodeWithTag(TagAtras).performClick()
            esperarA(TagInicio)
            Espresso.pressBack()
            esperarA(TagEntrarColeccionista)
            rule.onNodeWithTag(TagEntrarColeccionista).performScrollTo().performClick()
            esperarA(tagColeccionista(100))
            rule.onNodeWithTag(tagColeccionista(100)).performClick()
            val catalogoColeccionista = verAlbumes(evidencia, "03_inicio_coleccionista")
            rule.onNodeWithTag(TagRolActivo).assertTextEquals("COLECCIONISTA")
            capturar(evidencia, "04_catalogo_coleccionista")
            comprobarLosAlbumesDelCatalogoReal(catalogoColeccionista)
        }
    }

    /** FN01-ESC02: recorrer el catálogo real hasta el final y actualizarlo deslizando hacia abajo. */
    @Test
    fun recorrerHastaElFinalYDeslizarParaActualizar() {
        val evidencia = "FN01_ESC02"
        ActivityScenario.launch(MainActivity::class.java).use {
            rule.onNodeWithTag(TagEntrarVisitante).performScrollTo().performClick()
            val catalogo = verAlbumes(evidencia, "01_inicio_visitante")
            val ultimo = catalogo.last()

            verAlbum(ultimo.id)
            rule.onNodeWithTag(tagAlbum(ultimo.id)).assertIsDisplayed()
            capturar(evidencia, "02_final_del_catalogo")

            // Al principio de la lista, para que el gesto actualice en vez de solo desplazar.
            rule.onNodeWithTag(TagCuadriculaCatalogo).performScrollToIndex(0)
            rule.onNodeWithTag(TagCuadriculaCatalogo).performTouchInput {
                swipeDown(startY = top + 10f, endY = bottom, durationMillis = 600)
            }
            rule.waitUntil(15_000) { albumes.consultasCatalogo == 2 }
            rule.waitUntil(15_000) {
                rule.onAllNodesWithTag(TagActualizandoCatalogo).fetchSemanticsNodes().isEmpty()
            }
            val catalogoActualizado = requireNotNull(albumes.catalogo)
            rule.onNodeWithTag(TagContadorCatalogo).assertTextEquals(textoContador(catalogoActualizado.size))
            rule.onNodeWithTag(tagAlbum(catalogo.first().id)).assertIsDisplayed()
            capturar(evidencia, "03_tras_actualizar")
        }
    }

    /** FN01-ESC03: con el backend caído se ve el error y "Reintentar" recupera el catálogo (MockWebServer). */
    @Test
    fun conElBackendCaidoSeVeElErrorYReintentarRecuperaElCatalogo() {
        val evidencia = "FN01_ESC03"
        val servidor = MockWebServer().apply { start() }
        try {
            ApplicationProvider.getApplicationContext<VinilosApplication>().container =
                AppContainer(baseUrl = "http://localhost:${servidor.port}/")
            servidor.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

            ActivityScenario.launch(MainActivity::class.java).use {
                rule.onNodeWithTag(TagEntrarVisitante).performScrollTo().performClick()
                esperarA(TagInicio)
                EvidenciaE2E.capturar(rule, evidencia, "01_inicio_visitante")
                rule.onNodeWithTag(TagVerAlbumes).performScrollTo().performClick()
                esperarA(TagErrorCatalogo)
                rule.onNodeWithTag(TagErrorCatalogo).assertTextEquals("No pudimos cargar el catálogo")
                rule.onNodeWithText("Reintentar").performScrollTo().assertIsDisplayed()
                capturar(evidencia, "02_backend_caido")

                servidor.enqueue(
                    MockResponse().setBody(
                        """[{"id": 100, "name": "Buscando América", "genre": "Salsa",
                            "releaseDate": "1984-08-01T00:00:00.000Z",
                            "performers": [{"id": 1, "name": "Rubén Blades Bellido de Luna"}]}]"""
                    )
                )
                rule.onNodeWithTag(TagReintentarCatalogo).performScrollTo().performClick()
                esperarA(tagAlbum(100))
                rule.onNodeWithText("Buscando América").assertIsDisplayed()
                capturar(evidencia, "03_recuperado")
            }
        } finally {
            servidor.shutdown()
        }
    }

    /** FN01-ESC04: al girar, el catálogo real conserva su posición y no vuelve a consultar el backend. */
    @Test
    fun alGirarConservaLaPosicionSinVolverAConsultar() {
        val evidencia = "FN01_ESC04"
        ActivityScenario.launch(MainActivity::class.java).use { escenario ->
            rule.onNodeWithTag(TagEntrarVisitante).performScrollTo().performClick()
            val catalogo = verAlbumes(evidencia, "01_inicio_visitante")
            val ultimo = catalogo.last()
            verAlbum(ultimo.id)
            rule.onNodeWithTag(tagAlbum(ultimo.id)).assertIsDisplayed()
            capturar(evidencia, "02_antes_de_girar")

            escenario.recreate()
            rule.waitForIdle()

            rule.onNodeWithTag(tagAlbum(ultimo.id)).assertIsDisplayed()
            rule.onNodeWithTag(TagProgresoCatalogo).assertDoesNotExist()
            assertEquals(1, albumes.consultasCatalogo)
            capturar(evidencia, "03_despues_de_girar")
        }
    }

    /** FN01-ESC05: tocar un álbum real abre su detalle y "Atrás" vuelve al catálogo sin recargarlo. */
    @Test
    fun tocarUnAlbumAbreSuDetalleYAtrasVuelveAlCatalogo() {
        val evidencia = "FN01_ESC05"
        ActivityScenario.launch(MainActivity::class.java).use {
            rule.onNodeWithTag(TagEntrarVisitante).performScrollTo().performClick()
            val catalogo = verAlbumes(evidencia, "01_inicio_visitante")
            val primero = catalogo.first()

            verAlbum(primero.id)
            rule.onNodeWithTag(tagAlbum(primero.id)).performClick()
            rule.waitUntil(15_000) {
                rule.onAllNodesWithText(primero.nombre).fetchSemanticsNodes().isNotEmpty()
            }
            rule.onNodeWithText(primero.nombre).assertIsDisplayed()
            capturar(evidencia, "02_detalle")

            Espresso.pressBack()
            esperarA(tagAlbum(primero.id))
            rule.onNodeWithTag(TagRolActivo).assertTextEquals("VISITANTE")
            assertEquals(1, albumes.consultasCatalogo)
            capturar(evidencia, "03_regreso_al_catalogo")
        }
    }
}
