package com.example.vinilos

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.example.vinilos.data.model.Album
import com.example.vinilos.data.model.AlbumSummary
import com.example.vinilos.data.model.formatearReleaseDate
import com.example.vinilos.data.remote.ServiceAdapter
import com.example.vinilos.data.repository.AlbumRepository
import com.example.vinilos.data.repository.AlbumRepositoryImpl
import com.example.vinilos.e2e.EvidenciaE2E
import com.example.vinilos.ui.catalogo.TagCatalogo
import com.example.vinilos.ui.catalogo.TagCuadriculaCatalogo
import com.example.vinilos.ui.catalogo.tagAlbum
import com.example.vinilos.ui.ingreso.TagAtras
import com.example.vinilos.ui.ingreso.TagEntrarVisitante
import com.example.vinilos.ui.inicio.TagInicio
import com.example.vinilos.ui.inicio.TagVerAlbumes
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * E2E for HU02 (album detail) against the real BackVynils backend: logs in as a visitor, opens the
 * catalog, taps the first available album, and checks that its detail shows exactly the data the
 * backend returned for that album.
 *
 * Unlike an earlier version, this test doesn't assume a fixed album or dataset (like album 100 from
 * `sql/data.sql`): it wraps the real repository to capture the backend's real response (catalog and
 * detail) and computes the assertions from that captured data, reusing `formatearFechaLanzamiento`
 * (the same production formatter) for the date. This way the test is consistent both with the local
 * backend (Docker, `sql/data.sql`) and with the one deployed by the tutors, regardless of which
 * albums each has.
 *
 * Requires the backend on the PC and the phone with `adb reverse tcp:3000 tcp:3000`, installing the
 * app with `-PbaseUrl=http://localhost:3000/` (or without flags, if using the deployed backend).
 *
 * Each step saves a screenshot and a row in the report at
 * `.../files/e2e-evidencia/FN02_ESC01/informe.md` (only on the device, not in the repo). The
 * resulting PDF is the evidence for scenario FN02_ESC01 (full successful flow of HU02).
 */
class AlbumDetailE2ETest {

    private val carpetaEvidencia = "FN02_ESC01"

    /** Real repository that also stores the catalog and detail the backend returned. */
    private class AlbumRepositorioQueCaptura(
        private val real: AlbumRepository = AlbumRepositoryImpl(ServiceAdapter(BuildConfig.BASE_URL))
    ) : AlbumRepository by real {
        @Volatile var catalogo: List<AlbumSummary>? = null
        @Volatile var detalle: Album? = null

        override suspend fun getAlbums(): Result<List<AlbumSummary>> =
            real.getAlbums().also { resultado -> resultado.onSuccess { catalogo = it } }

        override suspend fun getAlbum(id: Int): Result<Album> =
            real.getAlbum(id).also { resultado -> resultado.onSuccess { detalle = it } }
    }

    private val repositorio = AlbumRepositorioQueCaptura()

    @get:Rule
    val rule = createEmptyComposeRule()

    @Before
    fun usarElBackendReal() {
        ApplicationProvider.getApplicationContext<VinilosApplication>().container =
            AppContainer(albumesRepositorio = repositorio)
    }

    private fun esperarA(tag: String, milis: Long = 15_000) =
        rule.waitUntil(milis) { rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }

    /** Scrolls the detail list until it finds [texto] and confirms it is displayed. */
    private fun scrollYVerificar(texto: String) {
        rule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(texto))
        rule.onNodeWithText(texto).assertIsDisplayed()
    }

    @Test
    fun elVisitanteVeElDetalleCompletoDelAlbumConSusDatosRealesDelBackend() {
        ActivityScenario.launch(MainActivity::class.java).use {
            rule.onNodeWithTag(TagEntrarVisitante).performScrollTo().performClick()
            esperarA(TagInicio)
            EvidenciaE2E.capturar(rule, carpetaEvidencia, "01_inicio")

            rule.onNodeWithTag(TagVerAlbumes).performScrollTo().performClick()
            esperarA(TagCatalogo)
            EvidenciaE2E.capturar(rule, carpetaEvidencia, "02_catalogo")

            // Waits for the real backend to respond with the catalog (without assuming how many or
            // which albums it has) and takes the first one to open its detail.
            rule.waitUntil(15_000) { repositorio.catalogo != null }
            val primerAlbum = requireNotNull(repositorio.catalogo).first()

            rule.onNodeWithTag(TagCuadriculaCatalogo)
                .performScrollToNode(hasTestTag(tagAlbum(primerAlbum.id)))
            rule.onNodeWithTag(tagAlbum(primerAlbum.id)).performClick()

            // Waits for the real backend query for that particular album's detail.
            rule.waitUntil(15_000) { repositorio.detalle?.id == primerAlbum.id }
            val detalle = requireNotNull(repositorio.detalle)

            // Real metadata of the queried album: whatever the backend returned.
            scrollYVerificar(detalle.nombre)
            scrollYVerificar(detalle.genero)
            scrollYVerificar(detalle.disquera)
            scrollYVerificar(formatearReleaseDate(detalle.fechaLanzamiento))
            EvidenciaE2E.capturar(rule, carpetaEvidencia, "03_detalle_metadatos")

            // Real artists of the album, if it has any, each with its image.
            detalle.artistas.forEach { artista ->
                scrollYVerificar(artista.nombre)
                rule.onNodeWithContentDescription("Imagen de ${artista.nombre}").assertIsDisplayed()
            }

            // Real tracks, or the empty-state message if the album has none registered.
            if (detalle.canciones.isEmpty()) {
                scrollYVerificar("Este álbum aún no tiene canciones registradas")
            } else {
                detalle.canciones.forEach { cancion -> scrollYVerificar(cancion.nombre) }
            }
            EvidenciaE2E.capturar(rule, carpetaEvidencia, "04_detalle_artista_canciones")

            // Real comments (without author), or the empty-state message if the album has none.
            if (detalle.comentarios.isEmpty()) {
                scrollYVerificar("Aún no hay comentarios para este álbum")
                scrollYVerificar("Sin calificaciones")
            } else {
                detalle.comentarios.forEach { comentario -> scrollYVerificar(comentario.descripcion) }
            }
            rule.onNodeWithText("Autor").assertDoesNotExist()
            EvidenciaE2E.capturar(rule, carpetaEvidencia, "05_detalle_comentario")

            // The back arrow (header) may have scrolled out of the composed area from the previous
            // scroll; it is searched for again before tapping it.
            rule.onNode(hasScrollToNodeAction()).performScrollToNode(hasTestTag(TagAtras))
            rule.onNodeWithTag(TagAtras).performClick()
            esperarA(TagCatalogo)
            EvidenciaE2E.capturar(rule, carpetaEvidencia, "06_regreso_catalogo")
        }
    }
}
