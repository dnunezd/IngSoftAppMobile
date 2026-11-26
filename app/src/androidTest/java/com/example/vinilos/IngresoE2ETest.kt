package com.example.vinilos

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import com.example.vinilos.data.model.AlbumSummary
import com.example.vinilos.data.model.Collector
import com.example.vinilos.data.remote.ServiceAdapter
import com.example.vinilos.data.repository.AlbumRepository
import com.example.vinilos.data.repository.AlbumRepositoryImpl
import com.example.vinilos.data.repository.CollectorRepository
import com.example.vinilos.data.repository.CollectorRepositoryImpl
import com.example.vinilos.e2e.EvidenciaE2E
import com.example.vinilos.ui.artistas.TagArtistas
import com.example.vinilos.ui.catalogo.TagCatalogo
import com.example.vinilos.ui.catalogo.TagRolActivo
import com.example.vinilos.ui.catalogo.tagAlbum
import com.example.vinilos.ui.ingreso.TagAtras
import com.example.vinilos.ui.ingreso.TagBuscador
import com.example.vinilos.ui.ingreso.TagEntrarColeccionista
import com.example.vinilos.ui.ingreso.TagEntrarVisitante
import com.example.vinilos.ui.ingreso.iniciales
import com.example.vinilos.ui.ingreso.tagColeccionista
import com.example.vinilos.ui.inicio.TagInicio
import com.example.vinilos.ui.inicio.TagVerAlbumes
import com.example.vinilos.ui.inicio.TagVerArtistas
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.text.Normalizer
import java.util.Locale

/**
 * E2E for HU00 (sign-in) against the real BackVynils backend.
 *
 * It does not assume a fixed dataset (like collectors 100 and 101 from `sql/data.sql`): it wraps
 * the real collector and album repositories to capture what the connected backend actually
 * returns, and computes the assertions (name, email, initials, discriminating search term) from
 * that data. This way the test is consistent both with the local backend (Docker) and the one
 * deployed by the instructors, regardless of which collectors each one has.
 *
 * On a phone it requires `adb reverse tcp:3000 tcp:3000` and the app installed with
 * `-PbaseUrl=http://localhost:3000/`. Recovery from backend failures is tested, automatically and
 * without a backend, in [RecuperacionTest].
 *
 * Scenarios FN00-ESC01 through FN00-ESC03 save a screenshot per step and a row in the report at
 * `.../files/e2e-evidencia/FN00_ESCxx/informe.md` (device-only, not in the repo).
 */
class IngresoE2ETest {

    @get:Rule
    val rule = createEmptyComposeRule()

    /** The real collector repository: stores the last listing received from the backend. */
    private class ColeccionistasRealesContados(
        private val real: CollectorRepository = CollectorRepositoryImpl(ServiceAdapter(BuildConfig.BASE_URL))
    ) : CollectorRepository by real {
        @Volatile var coleccionistas: List<Collector>? = null

        override suspend fun getCollectors(): Result<List<Collector>> =
            real.getCollectors().also { resultado -> resultado.onSuccess { coleccionistas = it } }
    }

    /** The real album repository: only used to know which is the first real album in the catalog. */
    private class AlbumesRealesContados(
        private val real: AlbumRepository = AlbumRepositoryImpl(ServiceAdapter(BuildConfig.BASE_URL))
    ) : AlbumRepository by real {
        @Volatile var catalogo: List<AlbumSummary>? = null

        override suspend fun getAlbums(): Result<List<AlbumSummary>> =
            real.getAlbums().also { resultado -> resultado.onSuccess { catalogo = it } }
    }

    private val coleccionistasRepo = ColeccionistasRealesContados()
    private val albumesRepo = AlbumesRealesContados()

    @Before
    fun usarElBackendReal() {
        ApplicationProvider.getApplicationContext<VinilosApplication>().container =
            AppContainer(colectoresRepositorio = coleccionistasRepo, albumesRepositorio = albumesRepo)
    }

    private fun esperarA(tag: String, milis: Long = 15_000) =
        rule.waitUntil(milis) { rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }

    private fun entrarComoColeccionista() {
        rule.onNodeWithTag(TagEntrarColeccionista).performScrollTo().performClick()
    }

    private fun esperarLosColeccionistasReales(): List<Collector> {
        rule.waitUntil(15_000) { coleccionistasRepo.coleccionistas != null }
        return requireNotNull(coleccionistasRepo.coleccionistas)
    }

    /** From the home screen, "Ver álbumes" leads to the catalog. */
    private fun verAlbumes() {
        esperarA(TagInicio)
        rule.onNodeWithTag(TagVerAlbumes).performScrollTo().performClick()
        esperarA(TagCatalogo)
        // The real catalog loads from BackVynils: it waits for the first real album (whichever
        // backend is connected) so the evidence shows the list instead of the "Cargando" state.
        rule.waitUntil(15_000) { albumesRepo.catalogo != null }
        val primerAlbum = requireNotNull(albumesRepo.catalogo).first()
        esperarA(tagAlbum(primerAlbum.id))
    }

    private fun filaColeccionista(coleccionista: Collector): SemanticsMatcher =
        hasTestTag(tagColeccionista(coleccionista.id))

    /** Checks name, email, and initials of [coleccionista], anchored to its own row. */
    private fun comprobarFilaColeccionista(coleccionista: Collector) {
        val fila = filaColeccionista(coleccionista)
        rule.onNode(fila).assertIsDisplayed()
        rule.onNode(fila and hasText(coleccionista.name)).assertExists()
        rule.onNode(fila and hasText(coleccionista.email)).assertExists()
        rule.onNode(fila and hasText(iniciales(coleccionista.name))).assertExists()
    }

    /** Same as the production search normalization: no accents and lowercase. */
    private val marcasDiacriticas = Regex("\\p{InCombiningDiacriticalMarks}+")
    private fun normalizado(texto: String) =
        Normalizer.normalize(texto, Normalizer.Form.NFD).replace(marcasDiacriticas, "").lowercase(Locale.ROOT)

    /**
     * The shortest substring of the name or email of [objetivo] that does not appear in either the
     * name or the email of any other collector in [lista]. Production search filters by normalized
     * name or email, so the term must discriminate on both fields.
     *
     * Returns `null` if no substring discriminates (e.g. the full name of [objetivo] is itself a
     * substring of the name or email of another real collector): in that case there is no safe
     * search term for [objetivo] and one must not be made up.
     */
    private fun substringDiscriminante(objetivo: Collector, lista: List<Collector>): String? {
        val bases = listOf(normalizado(objetivo.name), normalizado(objetivo.email))
        val otros = lista.filterNot { it.id == objetivo.id }
            .flatMap { listOf(normalizado(it.name), normalizado(it.email)) }
        for (base in bases) {
            for (longitud in 2..base.length) {
                for (inicio in 0..(base.length - longitud)) {
                    val candidato = base.substring(inicio, inicio + longitud)
                    if (otros.none { candidato in it }) return candidato
                }
            }
        }
        return null
    }

    /**
     * Collectors whose name+email is not repeated in any other entry of [lista]. The real backend
     * can list the same person twice with a different id (same name and email): no substring
     * discriminates between two identical records, so the search only uses collectors with
     * unique data.
     */
    private fun conDatosUnicos(lista: List<Collector>): List<Collector> {
        fun clave(c: Collector) = normalizado(c.name) + "|" + normalizado(c.email)
        val conteo = lista.groupingBy(::clave).eachCount()
        return lista.filter { conteo.getValue(clave(it)) == 1 }
    }

    /** FN00-ESC01: sign in to the platform as a visitor. */
    @Test
    fun ingresarComoVisitanteLlevaAlInicioYDeAhiAlCatalogo() {
        val evidencia = "FN00_ESC01"
        ActivityScenario.launch(MainActivity::class.java).use {
            esperarA(TagEntrarVisitante)
            EvidenciaE2E.capturar(rule, evidencia, "01_ingreso")
            rule.onNodeWithTag(TagEntrarVisitante).performScrollTo().performClick()

            esperarA(TagInicio)
            rule.onNodeWithText("¡Hola, melómano!").assertIsDisplayed()
            EvidenciaE2E.capturar(rule, evidencia, "02_inicio_visitante")
            verAlbumes()
            rule.onNodeWithTag(TagRolActivo).assertTextEquals("VISITANTE")
            EvidenciaE2E.capturar(rule, evidencia, "03_catalogo_visitante")
        }
    }

    /** FN00-ESC02: sign in to the platform as a collector. */
    @Test
    fun ingresarComoColeccionistaMuestraLaListaRealYSeEligeUno() {
        val evidencia = "FN00_ESC02"
        ActivityScenario.launch(MainActivity::class.java).use {
            esperarA(TagEntrarColeccionista)
            EvidenciaE2E.capturar(rule, evidencia, "01_ingreso")
            entrarComoColeccionista()

            val coleccionistas = esperarLosColeccionistasReales()
            Assume.assumeTrue("El backend conectado no tiene coleccionistas", coleccionistas.isNotEmpty())
            esperarA(tagColeccionista(coleccionistas.first().id))
            coleccionistas.take(2).forEach { comprobarFilaColeccionista(it) }
            EvidenciaE2E.capturar(rule, evidencia, "02_listado_coleccionistas")

            val elegido = coleccionistas.first()
            rule.onNodeWithTag(tagColeccionista(elegido.id)).performClick()

            esperarA(TagInicio)
            rule.onNodeWithText("¡Hola, ${elegido.name}!").assertIsDisplayed()
            EvidenciaE2E.capturar(rule, evidencia, "03_inicio_coleccionista")
            verAlbumes()
            rule.onNodeWithTag(TagRolActivo).assertTextEquals("COLECCIONISTA")
            EvidenciaE2E.capturar(rule, evidencia, "04_catalogo_coleccionista")
        }
    }

    /** FN00-ESC03: sign in as a collector using the search box. */
    @Test
    fun elBuscadorFiltraLosColeccionistasReales() {
        val evidencia = "FN00_ESC03"
        ActivityScenario.launch(MainActivity::class.java).use {
            esperarA(TagEntrarColeccionista)
            EvidenciaE2E.capturar(rule, evidencia, "01_ingreso")
            entrarComoColeccionista()
            val coleccionistas = esperarLosColeccionistasReales()
            Assume.assumeTrue("Se necesitan al menos 2 coleccionistas reales para probar la búsqueda", coleccionistas.size >= 2)
            esperarA(tagColeccionista(coleccionistas.first().id))
            EvidenciaE2E.capturar(rule, evidencia, "02_listado_coleccionistas")

            val objetivoYTermino = conDatosUnicos(coleccionistas).firstNotNullOfOrNull { candidato ->
                substringDiscriminante(candidato, coleccionistas)?.let { candidato to it }
            }
            Assume.assumeTrue(
                "Ningún coleccionista real tiene un término de búsqueda que lo discrimine de los demás",
                objetivoYTermino != null,
            )
            val (objetivo, termino) = requireNotNull(objetivoYTermino)
            rule.onNodeWithTag(TagBuscador).performTextInput(termino)

            rule.onNodeWithTag(tagColeccionista(objetivo.id)).assertIsDisplayed()
            (coleccionistas - objetivo).forEach { otro ->
                rule.onNodeWithTag(tagColeccionista(otro.id)).assertDoesNotExist()
            }
            EvidenciaE2E.capturar(rule, evidencia, "03_busqueda_filtrada")

            // Without closing the keyboard, the home screenshot comes out cropped while the window regains its height.
            Espresso.closeSoftKeyboard()
            rule.onNodeWithTag(tagColeccionista(objetivo.id)).performClick()

            esperarA(TagInicio)
            rule.onNodeWithText("¡Hola, ${objetivo.name}!").assertIsDisplayed()
            EvidenciaE2E.capturar(rule, evidencia, "04_inicio_coleccionista")
            verAlbumes()
            rule.onNodeWithTag(TagRolActivo).assertTextEquals("COLECCIONISTA")
            EvidenciaE2E.capturar(rule, evidencia, "05_catalogo_coleccionista")
        }
    }

    @Test
    fun navegaEntreIngresoInicioCatalogoYArtistasConAtras() {
        ActivityScenario.launch(MainActivity::class.java).use {
            entrarComoColeccionista()
            val coleccionistas = esperarLosColeccionistasReales()
            Assume.assumeTrue("Se necesitan al menos 2 coleccionistas reales para esta navegación", coleccionistas.size >= 2)
            val (primero, segundo) = coleccionistas.take(2)
            esperarA(tagColeccionista(primero.id))

            rule.onNodeWithTag(TagAtras).performClick()
            esperarA(TagEntrarVisitante)

            // Signing in as a collector leads to home, and the system "Back" goes to sign-in, not to the selector.
            entrarComoColeccionista()
            esperarA(tagColeccionista(segundo.id))
            rule.onNodeWithTag(tagColeccionista(segundo.id)).performClick()
            esperarA(TagInicio)

            // From home to the artist list and back with the arrow, keeping the session.
            rule.onNodeWithTag(TagVerArtistas).performScrollTo().performClick()
            esperarA(TagArtistas)
            rule.onNodeWithTag(TagAtras).performClick()
            esperarA(TagInicio)
            verAlbumes()
            rule.onNodeWithTag(TagRolActivo).assertTextEquals("COLECCIONISTA")

            // Catalog -> home -> sign-in.
            rule.onNodeWithTag(TagAtras).performClick()
            esperarA(TagInicio)
            Espresso.pressBack()
            esperarA(TagEntrarVisitante)
            rule.onAllNodesWithTag(TagBuscador).assertCountEquals(0)

            // Switching profile: now as visitor.
            rule.onNodeWithTag(TagEntrarVisitante).performScrollTo().performClick()
            verAlbumes()
            rule.onNodeWithTag(TagRolActivo).assertTextEquals("VISITANTE")
        }
    }
}
