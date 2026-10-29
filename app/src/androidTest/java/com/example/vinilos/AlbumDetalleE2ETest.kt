package com.example.vinilos

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
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
 * E2E de la HU02 (detalle del álbum) contra el backend real BackVynils: entra como visitante,
 * abre el catálogo, toca el álbum 100 ("Buscando América" en
 * `sql/data.sql`, con canciones, un artista y un comentario) y revisa su detalle completo.
 *
 * Requiere el backend en el PC y el teléfono con `adb reverse tcp:3000 tcp:3000`, instalando la
 * app con `-PbaseUrl=http://localhost:3000/`. El álbum sin canciones ni comentarios y el 404 se
 * verifican aparte en `AlbumDetalleBackendRealTest` (navegación directa por id).
 *
 * Cada paso guarda un pantallazo y una fila en el informe de
 * `.../files/e2e-evidencia/detalle de album/informe.md` (solo en el dispositivo, no en el repo).
 */
class AlbumDetalleE2ETest {

    private val carpetaEvidencia = "detalle de album"

    @get:Rule
    val rule = createEmptyComposeRule()

    @Before
    fun usarElBackendReal() {
        ApplicationProvider.getApplicationContext<VinilosApplication>().container = AppContainer()
    }

    private fun esperarA(tag: String, milis: Long = 15_000) =
        rule.waitUntil(milis) { rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }

    @Test
    fun elVisitanteVeElDetalleCompletoDelAlbumConCancionesArtistaYComentario() {
        ActivityScenario.launch(MainActivity::class.java).use {
            rule.onNodeWithTag(TagEntrarVisitante).performScrollTo().performClick()
            esperarA(TagInicio)
            EvidenciaE2E.capturar(rule, carpetaEvidencia, "01_inicio")

            rule.onNodeWithTag(TagVerAlbumes).performScrollTo().performClick()
            esperarA(TagCatalogo)
            EvidenciaE2E.capturar(rule, carpetaEvidencia, "02_catalogo")

            // Se toca el álbum en el catálogo real (HU01).
            esperarA(tagAlbum(100))
            rule.onNodeWithTag(TagCuadriculaCatalogo).performScrollToNode(hasTestTag(tagAlbum(100)))
            rule.onNodeWithTag(tagAlbum(100)).performClick()
            // Espera la consulta real al backend (y no solo el cambio de pantalla).
            rule.waitUntil(15_000) {
                rule.onAllNodesWithText("Buscando América").fetchSemanticsNodes().isNotEmpty()
            }

            // Metadatos del álbum real (sql/data.sql, id 100).
            rule.onNodeWithText("Buscando América").assertIsDisplayed()
            rule.onNodeWithText("Salsa").assertIsDisplayed()
            rule.onNodeWithText("Elektra").assertIsDisplayed()
            rule.onNodeWithText("1 de agosto de 1984").assertIsDisplayed()
            EvidenciaE2E.capturar(rule, carpetaEvidencia, "03_detalle_metadatos")

            // Artista real, con su imagen (o el reemplazo si la URL de ejemplo falla).
            rule.onNode(hasScrollToNodeAction())
                .performScrollToNode(hasText("Rubén Blades Bellido de Luna"))
            rule.onNodeWithText("Rubén Blades Bellido de Luna").assertIsDisplayed()
            rule.onNodeWithContentDescription("Imagen de Rubén Blades Bellido de Luna").assertIsDisplayed()

            // Canciones reales.
            rule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Decisiones"))
            rule.onNodeWithText("Decisiones").assertIsDisplayed()
            rule.onNodeWithText("5:05").assertIsDisplayed()
            rule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Desapariciones"))
            rule.onNodeWithText("Desapariciones").assertIsDisplayed()
            rule.onNodeWithText("6:29").assertIsDisplayed()
            EvidenciaE2E.capturar(rule, carpetaEvidencia, "04_detalle_artista_canciones")

            // Comentario real, sin autor, con su calificación.
            rule.onNode(hasScrollToNodeAction())
                .performScrollToNode(hasText("The most relevant album of Ruben Blades"))
            rule.onNodeWithText("The most relevant album of Ruben Blades").assertIsDisplayed()
            rule.onNodeWithText("Autor").assertDoesNotExist()
            EvidenciaE2E.capturar(rule, carpetaEvidencia, "05_detalle_comentario")

            // La flecha de regreso (encabezado) puede haber salido de la zona compuesta por el
            // scroll anterior; se vuelve a buscar antes de pulsarla.
            rule.onNode(hasScrollToNodeAction()).performScrollToNode(hasTestTag(TagAtras))
            rule.onNodeWithTag(TagAtras).performClick()
            esperarA(TagCatalogo)
            EvidenciaE2E.capturar(rule, carpetaEvidencia, "06_regreso_catalogo")
        }
    }
}
