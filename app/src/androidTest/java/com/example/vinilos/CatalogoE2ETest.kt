package com.example.vinilos

import android.os.SystemClock
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
 * E2E de la HU01 (catálogo de álbumes) contra el backend real BackVynils, con los 4 álbumes de
 * `sql/data.sql` (ids 100 a 103). E2E-01-03 (backend caído) usa `MockWebServer`, porque apagar el
 * backend dentro de la prueba no es confiable; el caso real se verifica a mano (M-01-07).
 *
 * En un teléfono requiere `adb reverse tcp:3000 tcp:3000` y la app instalada con
 * `-PbaseUrl=http://localhost:3000/`. Cada paso guarda un pantallazo y una fila en
 * `.../files/e2e-evidencia/catalogo de albumes/informe.md`.
 */
class CatalogoE2ETest {

    private val carpetaEvidencia = "catalogo de albumes"

    @get:Rule
    val rule = createEmptyComposeRule()

    /** El repositorio real de la app, que además cuenta cuántas veces se consulta el catálogo. */
    private class AlbumesRealesContados : AlbumRepository(ServiceAdapter(BuildConfig.BASE_URL)) {
        @Volatile var consultasCatalogo = 0

        override suspend fun obtenerAlbumes(): Result<List<AlbumResumen>> {
            consultasCatalogo++
            return super.obtenerAlbumes()
        }
    }

    private val albumes = AlbumesRealesContados()

    /** Orden alfabético esperado de los álbumes de ejemplo. */
    private val enOrden = listOf(
        103 to "A Day at the Races",
        102 to "A Night at the Opera",
        100 to "Buscando América",
        101 to "Poeta del pueblo"
    )

    @Before
    fun usarElBackendReal() {
        ApplicationProvider.getApplicationContext<VinilosApplication>().container =
            AppContainer(albumesRepositorio = albumes)
    }

    private fun esperarA(tag: String, milis: Long = 15_000) =
        rule.waitUntil(milis) { rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }

    /** Da tiempo a que Coil pinte las portadas antes del pantallazo. */
    private fun capturar(paso: String) {
        SystemClock.sleep(2_000)
        EvidenciaE2E.capturar(rule, carpetaEvidencia, paso)
    }

    private fun verAlbumes() {
        esperarA(TagInicio)
        rule.onNodeWithTag(TagVerAlbumes).performScrollTo().performClick()
        esperarA(tagAlbum(enOrden.first().first))
    }

    private fun verAlbum(id: Int) =
        rule.onNodeWithTag(TagCuadriculaCatalogo).performScrollToNode(hasTestTag(tagAlbum(id)))

    private fun comprobarLosCuatroAlbumesEnOrden() {
        rule.onNodeWithTag(TagContadorCatalogo).assertTextEquals("4 lanzamientos")
        enOrden.forEach { (id, nombre) ->
            verAlbum(id)
            rule.onNode(hasTestTag(tagAlbum(id)) and hasText(nombre) and hasContentDescription("Portada de $nombre"))
                .assertIsDisplayed()
        }
        val esperados = enOrden.map { tagAlbum(it.first) }
        assertEquals(esperados, rule.tagsDeAlbumesRecorriendo(esperados).filter { it in esperados })
    }

    /** E2E-01-01: Visitante y Coleccionista ven los mismos 4 álbumes, en orden, con portada, nombre y género. */
    @Test
    fun visitanteYColeccionistaVenLosCuatroAlbumesEnOrden() {
        ActivityScenario.launch(MainActivity::class.java).use {
            rule.onNodeWithTag(TagEntrarVisitante).performScrollTo().performClick()
            verAlbumes()
            rule.onNodeWithTag(TagRolActivo).assertTextEquals("VISITANTE")
            rule.onNode(hasTestTag(tagAlbum(103)) and hasText("Rock · 1976")).assertIsDisplayed()
            capturar("01_01_catalogo_visitante")
            comprobarLosCuatroAlbumesEnOrden()

            // Volver al ingreso y entrar como coleccionista.
            rule.onNodeWithTag(TagAtras).performClick()
            esperarA(TagInicio)
            Espresso.pressBack()
            esperarA(TagEntrarColeccionista)
            rule.onNodeWithTag(TagEntrarColeccionista).performScrollTo().performClick()
            esperarA(tagColeccionista(100))
            rule.onNodeWithTag(tagColeccionista(100)).performClick()
            verAlbumes()
            rule.onNodeWithTag(TagRolActivo).assertTextEquals("COLECCIONISTA")
            capturar("01_02_catalogo_coleccionista")
            comprobarLosCuatroAlbumesEnOrden()
        }
    }

    /** E2E-01-02: recorrer el catálogo hasta el final y actualizarlo deslizando hacia abajo. */
    @Test
    fun recorrerHastaElFinalYDeslizarParaActualizar() {
        ActivityScenario.launch(MainActivity::class.java).use {
            rule.onNodeWithTag(TagEntrarVisitante).performScrollTo().performClick()
            verAlbumes()

            verAlbum(101)
            rule.onNodeWithTag(tagAlbum(101)).assertIsDisplayed()
            capturar("02_01_final_del_catalogo")

            // Al principio de la lista, para que el gesto actualice en vez de solo desplazar.
            rule.onNodeWithTag(TagCuadriculaCatalogo).performScrollToIndex(0)
            rule.onNodeWithTag(TagCuadriculaCatalogo).performTouchInput {
                swipeDown(startY = top + 10f, endY = bottom, durationMillis = 600)
            }
            rule.waitUntil(15_000) { albumes.consultasCatalogo == 2 }
            rule.waitUntil(15_000) {
                rule.onAllNodesWithTag(TagActualizandoCatalogo).fetchSemanticsNodes().isEmpty()
            }
            rule.onNodeWithTag(TagContadorCatalogo).assertTextEquals("4 lanzamientos")
            rule.onNodeWithTag(tagAlbum(103)).assertIsDisplayed()
            capturar("02_02_tras_actualizar")
        }
    }

    /** E2E-01-03: con el backend caído se ve el error y "Reintentar" recupera el catálogo (MockWebServer). */
    @Test
    fun conElBackendCaidoSeVeElErrorYReintentarRecuperaElCatalogo() {
        val servidor = MockWebServer().apply { start() }
        try {
            ApplicationProvider.getApplicationContext<VinilosApplication>().container =
                AppContainer(baseUrl = "http://localhost:${servidor.port}/")
            servidor.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))

            ActivityScenario.launch(MainActivity::class.java).use {
                rule.onNodeWithTag(TagEntrarVisitante).performScrollTo().performClick()
                esperarA(TagInicio)
                rule.onNodeWithTag(TagVerAlbumes).performScrollTo().performClick()
                esperarA(TagErrorCatalogo)
                rule.onNodeWithTag(TagErrorCatalogo).assertTextEquals("No pudimos cargar el catálogo")
                rule.onNodeWithText("Reintentar").performScrollTo().assertIsDisplayed()
                capturar("03_01_backend_caido")

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
                capturar("03_02_recuperado")
            }
        } finally {
            servidor.shutdown()
        }
    }

    /** E2E-01-04: al girar, el catálogo conserva su posición y no vuelve a consultar el backend. */
    @Test
    fun alGirarConservaLaPosicionSinVolverAConsultar() {
        ActivityScenario.launch(MainActivity::class.java).use { escenario ->
            rule.onNodeWithTag(TagEntrarVisitante).performScrollTo().performClick()
            verAlbumes()
            verAlbum(101)
            rule.onNodeWithTag(tagAlbum(101)).assertIsDisplayed()
            capturar("04_01_antes_de_girar")

            escenario.recreate()
            rule.waitForIdle()

            rule.onNodeWithTag(tagAlbum(101)).assertIsDisplayed()
            rule.onNodeWithTag(TagProgresoCatalogo).assertDoesNotExist()
            assertEquals(1, albumes.consultasCatalogo)
            capturar("04_02_despues_de_girar")
        }
    }

    /** E2E-01-05: tocar un álbum abre su detalle real y "Atrás" vuelve al catálogo sin recargarlo. */
    @Test
    fun tocarUnAlbumAbreSuDetalleYAtrasVuelveAlCatalogo() {
        ActivityScenario.launch(MainActivity::class.java).use {
            rule.onNodeWithTag(TagEntrarVisitante).performScrollTo().performClick()
            verAlbumes()

            verAlbum(100)
            rule.onNodeWithTag(tagAlbum(100)).performClick()
            rule.waitUntil(15_000) {
                rule.onAllNodesWithText("1 de agosto de 1984").fetchSemanticsNodes().isNotEmpty()
            }
            rule.onNodeWithText("Buscando América").assertIsDisplayed()
            capturar("05_01_detalle")

            Espresso.pressBack()
            esperarA(tagAlbum(100))
            rule.onNodeWithTag(TagRolActivo).assertTextEquals("VISITANTE")
            assertEquals(1, albumes.consultasCatalogo)
            capturar("05_02_regreso_al_catalogo")
        }
    }
}
