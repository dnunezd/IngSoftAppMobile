package com.example.vinilos.navigation

import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import com.example.vinilos.AppContainer
import com.example.vinilos.RepositorioArtistasFalso
import com.example.vinilos.RepositorioFalso
import com.example.vinilos.data.model.Collector
import com.example.vinilos.e2e.EvidenciaE2E
import com.example.vinilos.ui.theme.VinilosTheme
import com.example.vinilos.viewmodel.SessionViewModel
import org.junit.Rule
import org.junit.Test

/**
 * E2E de la HU02 (detalle del álbum) contra el backend real BackVynils, para los dos casos que el
 * enlace provisional del catálogo (fijo al álbum 100, ver `AlbumIdProvisional`) no permite
 * alcanzar desde la interfaz mientras HU01 no tenga un catálogo real:
 *  - un álbum sin canciones ni comentarios (id 101, "Poeta del pueblo" en `sql/data.sql`), y
 *  - un álbum inexistente (404 real del backend).
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
 * `.../files/e2e-evidencia/detalle de album/informe.md` (solo en el dispositivo, no en el repo).
 */
class AlbumDetalleBackendRealTest {

    private val carpetaEvidencia = "detalle de album"

    @get:Rule
    val rule = createComposeRule()

    private lateinit var nav: TestNavHostController

    private fun mostrarDesde(ruta: String) {
        val contenedor = AppContainer()
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
        mostrarDesde(Rutas.detalleAlbum(101))

        rule.waitUntil(15_000) {
            rule.onAllNodesWithText("Poeta del pueblo").fetchSemanticsNodes().isNotEmpty()
        }

        rule.onNodeWithText("Poeta del pueblo").assertIsDisplayed()
        rule.onNodeWithText("Salsa").assertIsDisplayed()
        rule.onNodeWithText("Rubén Blades Bellido de Luna").assertIsDisplayed()
        rule.onNodeWithText("Este álbum aún no tiene canciones registradas").assertIsDisplayed()
        rule.onNodeWithText("Aún no hay comentarios para este álbum").assertIsDisplayed()
        rule.onNodeWithText("Sin calificaciones").assertIsDisplayed()
        EvidenciaE2E.capturar(rule, carpetaEvidencia, "07_album_sin_contenido")
    }

    @Test
    fun unAlbumInexistenteMuestraElMensaje404RealDelBackend() {
        mostrarDesde(Rutas.detalleAlbum(999))

        rule.waitUntil(15_000) {
            rule.onAllNodesWithText("El álbum ya no está disponible").fetchSemanticsNodes().isNotEmpty()
        }

        rule.onNodeWithText("El álbum ya no está disponible").assertIsDisplayed()
        rule.onNodeWithText("Volver al catálogo").assertIsDisplayed()
        EvidenciaE2E.capturar(rule, carpetaEvidencia, "08_album_404_real")
    }
}
