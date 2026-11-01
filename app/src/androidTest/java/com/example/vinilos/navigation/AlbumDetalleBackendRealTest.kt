package com.example.vinilos.navigation

import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import com.example.vinilos.AppContainer
import com.example.vinilos.RepositorioArtistasFalso
import com.example.vinilos.RepositorioFalso
import com.example.vinilos.data.model.Collector
import com.example.vinilos.e2e.EvidenciaE2E
import com.example.vinilos.ui.theme.VinilosTheme
import com.example.vinilos.viewmodel.SessionViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

/**
 * E2E de la HU02 (detalle del álbum) contra el backend real BackVynils, para dos casos que se
 * prueban mejor navegando directo a la ruta por id (un id inexistente no aparece en el catálogo):
 *  - un álbum sin canciones ni comentarios, y
 *  - un álbum inexistente (404 real del backend).
 *
 * El primer caso no asume un id ni un nombre fijo (como el álbum 101/"Poeta del pueblo" de
 * `sql/data.sql`): consulta el catálogo real y busca, entre los primeros álbumes, uno cuyo
 * detalle real no tenga canciones ni comentarios. Si el backend actual no tiene ningún álbum así
 * (por ejemplo, el desplegado por los tutores), la prueba se omite en vez de fallar, porque el
 * escenario que busca cubrir (mensajes de vacío) no se puede ejercitar con esos datos.
 *
 * Por eso se arma el mismo `VinilosNavHost` de la app (con el [AlbumRepository][com.example.vinilos.data.repository.AlbumRepository]
 * real de [AppContainer]) y se navega directo a la ruta del detalle por id, igual que hace el
 * catálogo al pulsar un álbum. El resto del árbol de navegación no participa en esta HU, así que
 * los repositorios de coleccionistas/artistas quedan con datos controlados.
 *
 * Requiere el backend en el PC y el teléfono con `adb reverse tcp:3000 tcp:3000`, instalando la
 * app con `-PbaseUrl=http://localhost:3000/`.
 *
 * Cada paso guarda un pantallazo y una fila en el informe de
 * `.../files/e2e-evidencia/<carpeta>/informe.md` (solo en el dispositivo, no en el repo). Cada
 * escenario usa su propia carpeta (FN02_ESC02 y FN02_ESC03) para obtener un PDF por escenario.
 */
class AlbumDetalleBackendRealTest {

    @get:Rule
    val rule = createComposeRule()

    private lateinit var nav: TestNavHostController

    private val contenedor = AppContainer()

    private fun mostrarDesde(ruta: String) {
        rule.setContent {
            val contexto = LocalContext.current
            nav = remember {
                TestNavHostController(contexto).apply { navigatorProvider.addNavigator(ComposeNavigator()) }
            }
            VinilosTheme {
                VinilosNavHost(
                    navController = nav,
                    sessionViewModel = SessionViewModel().apply { enterAsVisitor() },
                    collectorRepository = RepositorioFalso(Result.success(emptyList<Collector>())),
                    artistRepository = RepositorioArtistasFalso(Result.success(emptyList())),
                    albumRepository = contenedor.albumRepository
                )
            }
        }
        rule.runOnIdle { nav.navigate(ruta) }
        rule.waitForIdle()
    }

    @Test
    fun unAlbumSinCancionesNiComentariosMuestraLosMensajesDeVacioConDatosReales() {
        // Busca, entre los primeros 30 álbumes del catálogo real, uno sin canciones ni
        // comentarios. No se asume cuál id cumple esa condición: depende del backend conectado.
        val vacio = runBlocking {
            val catalogo = contenedor.albumRepository.obtenerAlbumes().getOrThrow()
            catalogo.asSequence().take(30).firstNotNullOfOrNull { resumen ->
                contenedor.albumRepository.obtenerAlbum(resumen.id).getOrNull()
                    ?.takeIf { it.canciones.isEmpty() && it.comentarios.isEmpty() }
            }
        }
        assumeTrue(
            "El backend conectado no tiene ningún álbum sin canciones ni comentarios " +
                "entre los primeros 30 del catálogo",
            vacio != null
        )
        requireNotNull(vacio)

        mostrarDesde(Rutas.detalleAlbum(vacio.id))

        rule.waitUntil(15_000) {
            rule.onAllNodesWithText(vacio.nombre).fetchSemanticsNodes().isNotEmpty()
        }

        // performScrollToNode se usa porque en una lista larga en un dispositivo real los
        // elementos mas abajo (artista, mensajes de vacio) pueden no estar compuestos aun.
        val lista = rule.onNode(hasScrollToNodeAction())
        rule.onNodeWithText(vacio.nombre).assertIsDisplayed()
        vacio.artistas.forEach { artista ->
            lista.performScrollToNode(hasText(artista.nombre))
            rule.onNodeWithText(artista.nombre).assertIsDisplayed()
        }
        lista.performScrollToNode(hasText("Este álbum aún no tiene canciones registradas"))
        rule.onNodeWithText("Este álbum aún no tiene canciones registradas").assertIsDisplayed()
        lista.performScrollToNode(hasText("Aún no hay comentarios para este álbum"))
        rule.onNodeWithText("Aún no hay comentarios para este álbum").assertIsDisplayed()
        lista.performScrollToNode(hasText("Sin calificaciones"))
        rule.onNodeWithText("Sin calificaciones").assertIsDisplayed()
        EvidenciaE2E.capturar(rule, "FN02_ESC02", "07_album_sin_contenido")
    }

    @Test
    fun unAlbumInexistenteMuestraElMensaje404RealDelBackend() {
        mostrarDesde(Rutas.detalleAlbum(999))

        rule.waitUntil(15_000) {
            rule.onAllNodesWithText("El álbum ya no está disponible").fetchSemanticsNodes().isNotEmpty()
        }

        rule.onNodeWithText("El álbum ya no está disponible").assertIsDisplayed()
        rule.onNodeWithText("Volver al catálogo").assertIsDisplayed()
        EvidenciaE2E.capturar(rule, "FN02_ESC03", "08_album_404_real")
    }
}
