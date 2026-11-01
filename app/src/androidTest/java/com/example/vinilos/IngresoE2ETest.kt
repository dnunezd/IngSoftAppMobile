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
import com.example.vinilos.data.model.AlbumResumen
import com.example.vinilos.data.model.Collector
import com.example.vinilos.data.remote.ServiceAdapter
import com.example.vinilos.data.repository.AlbumRepository
import com.example.vinilos.data.repository.CollectorRepository
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
 * E2E de la HU00 (ingreso) contra el backend real BackVynils.
 *
 * No asume un dataset fijo (como los coleccionistas 100 y 101 de `sql/data.sql`): envuelve los
 * repositorios reales de coleccionistas y álbumes para capturar lo que el backend conectado
 * realmente devuelve, y calcula las aserciones (nombre, correo, iniciales, término de búsqueda
 * discriminante) a partir de esos datos. Así la prueba es coherente tanto con el backend local
 * (Docker) como con el desplegado por los tutores, sin importar qué coleccionistas tenga cada uno.
 *
 * En un teléfono requiere `adb reverse tcp:3000 tcp:3000` y la app instalada con
 * `-PbaseUrl=http://localhost:3000/`. La recuperación ante fallos del backend se prueba, de forma
 * automática y sin backend, en [RecuperacionTest].
 *
 * Los escenarios FN00-ESC01 a FN00-ESC03 guardan un pantallazo por paso y una fila en el informe de
 * `.../files/e2e-evidencia/FN00_ESCxx/informe.md` (solo en el dispositivo, no en el repo).
 */
class IngresoE2ETest {

    @get:Rule
    val rule = createEmptyComposeRule()

    /** El repositorio real de coleccionistas: guarda el último listado recibido del backend. */
    private class ColeccionistasRealesContados : CollectorRepository(ServiceAdapter(BuildConfig.BASE_URL)) {
        @Volatile var coleccionistas: List<Collector>? = null

        override suspend fun obtenerColeccionistas(): Result<List<Collector>> =
            super.obtenerColeccionistas().also { resultado -> resultado.onSuccess { coleccionistas = it } }
    }

    /** El repositorio real de álbumes: solo para saber cuál es el primer álbum real del catálogo. */
    private class AlbumesRealesContados : AlbumRepository(ServiceAdapter(BuildConfig.BASE_URL)) {
        @Volatile var catalogo: List<AlbumResumen>? = null

        override suspend fun obtenerAlbumes(): Result<List<AlbumResumen>> =
            super.obtenerAlbumes().also { resultado -> resultado.onSuccess { catalogo = it } }
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

    /** Desde la pantalla de inicio, "Ver álbumes" lleva al catálogo. */
    private fun verAlbumes() {
        esperarA(TagInicio)
        rule.onNodeWithTag(TagVerAlbumes).performScrollTo().performClick()
        esperarA(TagCatalogo)
        // El catálogo real carga desde BackVynils: se espera al primer álbum real (sea cual sea
        // el backend conectado) para que la evidencia muestre la lista y no el estado "Cargando".
        rule.waitUntil(15_000) { albumesRepo.catalogo != null }
        val primerAlbum = requireNotNull(albumesRepo.catalogo).first()
        esperarA(tagAlbum(primerAlbum.id))
    }

    private fun filaColeccionista(coleccionista: Collector): SemanticsMatcher =
        hasTestTag(tagColeccionista(coleccionista.id))

    /** Comprueba nombre, correo e iniciales de [coleccionista], anclado a su propia fila. */
    private fun comprobarFilaColeccionista(coleccionista: Collector) {
        val fila = filaColeccionista(coleccionista)
        rule.onNode(fila).assertIsDisplayed()
        rule.onNode(fila and hasText(coleccionista.name)).assertExists()
        rule.onNode(fila and hasText(coleccionista.email)).assertExists()
        rule.onNode(fila and hasText(iniciales(coleccionista.name))).assertExists()
    }

    /** Igual que la normalización de búsqueda de producción: sin tildes y en minúsculas. */
    private val marcasDiacriticas = Regex("\\p{InCombiningDiacriticalMarks}+")
    private fun normalizado(texto: String) =
        Normalizer.normalize(texto, Normalizer.Form.NFD).replace(marcasDiacriticas, "").lowercase(Locale.ROOT)

    /**
     * La subcadena más corta del nombre o correo de [objetivo] que no aparece ni en el nombre ni
     * en el correo de ningún otro coleccionista de [lista]. La búsqueda de producción filtra por
     * nombre o correo normalizados, así que el término debe discriminar en ambos campos.
     *
     * Devuelve `null` si ninguna subcadena discrimina (p.ej. el nombre completo de [objetivo] es,
     * a su vez, subcadena del nombre o correo de otro coleccionista real): en ese caso no hay
     * ningún término de búsqueda seguro para [objetivo] y no se debe inventar uno.
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
     * Coleccionistas cuyo nombre+correo no se repite en ningún otro de [lista]. El backend real
     * puede listar la misma persona dos veces con distinto id (mismo nombre y correo): ninguna
     * subcadena discrimina entre dos registros idénticos, así que la búsqueda solo usa
     * coleccionistas con datos únicos.
     */
    private fun conDatosUnicos(lista: List<Collector>): List<Collector> {
        fun clave(c: Collector) = normalizado(c.name) + "|" + normalizado(c.email)
        val conteo = lista.groupingBy(::clave).eachCount()
        return lista.filter { conteo.getValue(clave(it)) == 1 }
    }

    /** FN00-ESC01: ingresar a la plataforma como visitante. */
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

    /** FN00-ESC02: ingresar a la plataforma como coleccionista. */
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

    /** FN00-ESC03: ingresar como coleccionista usando la búsqueda. */
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

            // Sin cerrar el teclado, la captura del inicio sale cortada mientras la ventana recupera su alto.
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

            // Entrar como coleccionista lleva al inicio, y "Atrás" del sistema al ingreso, no al selector.
            entrarComoColeccionista()
            esperarA(tagColeccionista(segundo.id))
            rule.onNodeWithTag(tagColeccionista(segundo.id)).performClick()
            esperarA(TagInicio)

            // Del inicio al listado de artistas y de vuelta con la flecha, conservando la sesión.
            rule.onNodeWithTag(TagVerArtistas).performScrollTo().performClick()
            esperarA(TagArtistas)
            rule.onNodeWithTag(TagAtras).performClick()
            esperarA(TagInicio)
            verAlbumes()
            rule.onNodeWithTag(TagRolActivo).assertTextEquals("COLECCIONISTA")

            // Catálogo -> inicio -> ingreso.
            rule.onNodeWithTag(TagAtras).performClick()
            esperarA(TagInicio)
            Espresso.pressBack()
            esperarA(TagEntrarVisitante)
            rule.onAllNodesWithTag(TagBuscador).assertCountEquals(0)

            // Cambiar de perfil: ahora como visitante.
            rule.onNodeWithTag(TagEntrarVisitante).performScrollTo().performClick()
            verAlbumes()
            rule.onNodeWithTag(TagRolActivo).assertTextEquals("VISITANTE")
        }
    }
}
