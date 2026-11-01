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
import com.example.vinilos.data.model.AlbumResumen
import com.example.vinilos.data.remote.ServiceAdapter
import com.example.vinilos.data.repository.AlbumRepository
import com.example.vinilos.e2e.EvidenciaE2E
import com.example.vinilos.ui.catalogo.TagCatalogo
import com.example.vinilos.ui.catalogo.TagCuadriculaCatalogo
import com.example.vinilos.ui.catalogo.tagAlbum
import com.example.vinilos.ui.detalle.formatearFechaLanzamiento
import com.example.vinilos.ui.ingreso.TagAtras
import com.example.vinilos.ui.ingreso.TagEntrarVisitante
import com.example.vinilos.ui.inicio.TagInicio
import com.example.vinilos.ui.inicio.TagVerAlbumes
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * E2E de la HU02 (detalle del álbum) contra el backend real BackVynils: entra como visitante,
 * abre el catálogo, toca el primer álbum disponible y revisa que su detalle muestre exactamente
 * los datos que el backend devolvió para ese álbum.
 *
 * A diferencia de una versión anterior, esta prueba no asume un álbum ni un dataset fijo (como el
 * álbum 100 de `sql/data.sql`): envuelve el repositorio real para capturar la respuesta real del
 * backend (catálogo y detalle) y calcula las aserciones a partir de esos datos capturados,
 * reutilizando `formatearFechaLanzamiento` (el mismo formateador de producción) para la fecha. Así
 * la prueba es coherente tanto con el backend local (Docker, `sql/data.sql`) como con el
 * desplegado por los tutores, sin importar qué álbumes tenga cada uno.
 *
 * Requiere el backend en el PC y el teléfono con `adb reverse tcp:3000 tcp:3000`, instalando la
 * app con `-PbaseUrl=http://localhost:3000/` (o sin flags, si se usa el backend desplegado).
 *
 * Cada paso guarda un pantallazo y una fila en el informe de
 * `.../files/e2e-evidencia/FN02_ESC01/informe.md` (solo en el dispositivo, no en el repo). El PDF
 * resultante es la evidencia del escenario FN02_ESC01 (flujo completo exitoso de la HU02).
 */
class AlbumDetalleE2ETest {

    private val carpetaEvidencia = "FN02_ESC01"

    /** Repositorio real que además guarda el catálogo y el detalle que el backend devolvió. */
    private class AlbumRepositorioQueCaptura : AlbumRepository(ServiceAdapter(BuildConfig.BASE_URL)) {
        @Volatile var catalogo: List<AlbumResumen>? = null
        @Volatile var detalle: Album? = null

        override suspend fun obtenerAlbumes(): Result<List<AlbumResumen>> =
            super.obtenerAlbumes().also { resultado -> resultado.onSuccess { catalogo = it } }

        override suspend fun obtenerAlbum(id: Int): Result<Album> =
            super.obtenerAlbum(id).also { resultado -> resultado.onSuccess { detalle = it } }
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

    /** Desplaza la lista del detalle hasta encontrar [texto] y confirma que se muestra. */
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

            // Se espera a que el backend real responda con el catálogo (sin asumir cuántos ni
            // cuáles álbumes trae) y se toma el primero para abrir su detalle.
            rule.waitUntil(15_000) { repositorio.catalogo != null }
            val primerAlbum = requireNotNull(repositorio.catalogo).first()

            rule.onNodeWithTag(TagCuadriculaCatalogo)
                .performScrollToNode(hasTestTag(tagAlbum(primerAlbum.id)))
            rule.onNodeWithTag(tagAlbum(primerAlbum.id)).performClick()

            // Espera la consulta real al backend del detalle de ese álbum en particular.
            rule.waitUntil(15_000) { repositorio.detalle?.id == primerAlbum.id }
            val detalle = requireNotNull(repositorio.detalle)

            // Metadatos reales del álbum consultado: lo que el backend haya devuelto.
            scrollYVerificar(detalle.nombre)
            scrollYVerificar(detalle.genero)
            scrollYVerificar(detalle.disquera)
            scrollYVerificar(formatearFechaLanzamiento(detalle.fechaLanzamiento))
            EvidenciaE2E.capturar(rule, carpetaEvidencia, "03_detalle_metadatos")

            // Artistas reales del álbum, si tiene alguno, cada uno con su imagen.
            detalle.artistas.forEach { artista ->
                scrollYVerificar(artista.nombre)
                rule.onNodeWithContentDescription("Imagen de ${artista.nombre}").assertIsDisplayed()
            }

            // Canciones reales, o el mensaje de vacío si el álbum no tiene ninguna registrada.
            if (detalle.canciones.isEmpty()) {
                scrollYVerificar("Este álbum aún no tiene canciones registradas")
            } else {
                detalle.canciones.forEach { cancion -> scrollYVerificar(cancion.nombre) }
            }
            EvidenciaE2E.capturar(rule, carpetaEvidencia, "04_detalle_artista_canciones")

            // Comentarios reales (sin autor), o el mensaje de vacío si el álbum no tiene ninguno.
            if (detalle.comentarios.isEmpty()) {
                scrollYVerificar("Aún no hay comentarios para este álbum")
                scrollYVerificar("Sin calificaciones")
            } else {
                detalle.comentarios.forEach { comentario -> scrollYVerificar(comentario.descripcion) }
            }
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
