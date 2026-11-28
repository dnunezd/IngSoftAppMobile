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
import com.example.vinilos.e2e.EvidenciaE2E
import com.example.vinilos.ui.theme.VinilosTheme
import com.example.vinilos.viewmodel.SessionViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test

/**
 * E2E for HU02 (album detail) against the real BackVynils backend, for two cases that are best
 * tested by navigating directly to the route by id (a nonexistent id doesn't appear in the catalog):
 *  - an album with no tracks or comments, and
 *  - a nonexistent album (a real 404 from the backend).
 *
 * The first case doesn't assume a fixed id or name (like album 101/"Poeta del pueblo" from
 * `sql/data.sql`): it queries the real catalog and looks, among the first albums, for one whose
 * real detail has no tracks or comments. If the currently connected backend has no such album
 * (e.g. the one deployed by the tutors), the test is skipped instead of failing, because the
 * scenario it aims to cover (empty-state messages) can't be exercised with that data.
 *
 * That's why the same `VinilosNavHost` from the app is assembled (with the real [AlbumRepository][com.example.vinilos.data.repository.AlbumRepository]
 * from [AppContainer]) and navigation goes directly to the detail route by id, the same way the
 * catalog does when an album is tapped. The rest of the navigation tree doesn't take part in this HU, so
 * the collector/artist repositories are left with controlled data.
 *
 * Requires the backend on the PC and the phone with `adb reverse tcp:3000 tcp:3000`, installing the
 * app with `-PbaseUrl=http://localhost:3000/`.
 *
 * Each step saves a screenshot and a row in the report at
 * `.../files/e2e-evidencia/<folder>/informe.md` (only on the device, not in the repo). Each
 * scenario uses its own folder (FN02_ESC02 and FN02_ESC03) to get one PDF per scenario.
 */
class AlbumDetailBackendRealTest {

    @get:Rule
    val rule = createComposeRule()

    private lateinit var nav: TestNavHostController

    private val contenedor = AppContainer()

    private fun mostrarDesde(ruta: String) {
        // The ViewModel is created outside the composable so it isn't recreated on every recomposition.
        val sesion = SessionViewModel().apply { enterAsVisitor() }
        rule.setContent {
            val contexto = LocalContext.current
            nav = remember {
                TestNavHostController(contexto).apply { navigatorProvider.addNavigator(ComposeNavigator()) }
            }
            VinilosTheme {
                VinilosNavHost(
                    navController = nav,
                    sessionViewModel = sesion,
                    collectorRepository = RepositorioFalso(Result.success(emptyList())),
                    artistRepository = RepositorioArtistasFalso(Result.success(emptyList())),
                    albumRepository = contenedor.albumRepository,
                    favoriteRepository = contenedor.favoriteRepository,
                    comentarioRepository = contenedor.comentarioRepository
                )
            }
        }
        rule.runOnIdle { nav.navigate(ruta) }
        rule.waitForIdle()
    }

    @Test
    fun unAlbumSinTrackesNiCommentsMuestraLosMensajesDeEmptyConDatosReales() {
        // Looks, among the first 30 albums in the real catalog, for one with no tracks or
        // comments. It doesn't assume which id meets that condition: it depends on the connected backend.
        val vacio = runBlocking {
            val catalogo = contenedor.albumRepository.getAlbums().getOrThrow()
            catalogo.asSequence().take(30).firstNotNullOfOrNull { resumen ->
                contenedor.albumRepository.getAlbum(resumen.id).getOrNull()
                    ?.takeIf { it.canciones.isEmpty() && it.comentarios.isEmpty() }
            }
        }
        assumeTrue(
            "El backend conectado no tiene ningún álbum sin canciones ni comentarios " +
                "entre los primeros 30 del catálogo",
            vacio != null
        )
        requireNotNull(vacio)

        mostrarDesde(Routes.detalleAlbum(vacio.id))

        rule.waitUntil(15_000) {
            rule.onAllNodesWithText(vacio.nombre).fetchSemanticsNodes().isNotEmpty()
        }

        // performScrollToNode is used because in a long list on a real device the
        // elements further down (artist, empty messages) may not be composed yet.
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
        mostrarDesde(Routes.detalleAlbum(999))

        rule.waitUntil(15_000) {
            rule.onAllNodesWithText("El álbum ya no está disponible").fetchSemanticsNodes().isNotEmpty()
        }

        rule.onNodeWithText("El álbum ya no está disponible").assertIsDisplayed()
        rule.onNodeWithText("Volver al catálogo").assertIsDisplayed()
        EvidenciaE2E.capturar(rule, "FN02_ESC03", "08_album_404_real")
    }
}
