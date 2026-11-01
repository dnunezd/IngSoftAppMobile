package com.example.vinilos

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import com.example.vinilos.data.model.Artist
import com.example.vinilos.data.model.ArtistType
import com.example.vinilos.data.remote.ServiceAdapter
import com.example.vinilos.data.repository.ArtistRepository
import com.example.vinilos.e2e.EvidenciaE2E
import com.example.vinilos.ui.artistas.TagArtistas
import com.example.vinilos.ui.artistas.TagBuscadorArtistas
import com.example.vinilos.ui.artistas.TagContadorArtistas
import com.example.vinilos.ui.artistas.TagDetalleArtista
import com.example.vinilos.ui.artistas.TagErrorArtistas
import com.example.vinilos.ui.artistas.TagListaArtistas
import com.example.vinilos.ui.artistas.TagNombreDetalleArtista
import com.example.vinilos.ui.artistas.TagReintentarArtistas
import com.example.vinilos.ui.artistas.TagTipoDetalleArtista
import com.example.vinilos.ui.artistas.TagVacioArtistas
import com.example.vinilos.ui.artistas.tagArtista
import com.example.vinilos.ui.ingreso.TagAtras
import com.example.vinilos.ui.ingreso.TagEntrarVisitante
import com.example.vinilos.ui.inicio.TagInicio
import com.example.vinilos.ui.inicio.TagVerArtistas
import java.text.Normalizer
import java.util.Locale
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assume
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * E2E de la HU03 (listado de artistas) como Visitante.
 *
 * FN03-ESC01 a ESC04 y ESC07 van contra el backend real BackVynils, pero no asumen un dataset fijo
 * (como los 2 artistas de ejemplo de `sql/data.sql`): envuelven el repositorio real para capturar
 * el listado que el backend realmente devuelve y calculan los términos de búsqueda y las
 * aseriones a partir de esos datos. Así las pruebas son coherentes tanto con el backend local
 * (Docker) como con el desplegado por los tutores, sin importar cuántos artistas tenga cada uno.
 * En el emulador la app llega al backend por `10.0.2.2`; en un teléfono, con
 * `adb reverse tcp:3000 tcp:3000` e instalando la app con `-PbaseUrl=http://localhost:3000/`.
 *
 * FN03-ESC05 (backend caído) y ESC06 (sin artistas) no se pueden provocar contra BackVynils sin
 * detenerlo o borrar datos, así que siguen usando `MockWebServer` con datos fabricados; no
 * dependen del dataset real y no necesitaban cambiar.
 *
 * Cada escenario guarda un pantallazo por paso, `informe.md` y un PDF en
 * `.../files/e2e-evidencia/FN03_ESCxx/` (solo en el dispositivo, no en el repo).
 */
class ArtistasE2ETest {

    @get:Rule
    val rule = createEmptyComposeRule()

    /** El repositorio real de la app: cuenta las consultas y guarda el último listado recibido. */
    private class ArtistasReales : ArtistRepository(ServiceAdapter(BuildConfig.BASE_URL)) {
        @Volatile var consultas = 0
        @Volatile var lista: List<Artist>? = null

        override suspend fun obtenerArtistas(): Result<List<Artist>> {
            consultas++
            return super.obtenerArtistas().also { resultado -> resultado.onSuccess { lista = it } }
        }
    }

    private val artistas = ArtistasReales()

    private var servidor: MockWebServer? = null

    @Before
    fun usarElBackendReal() {
        ApplicationProvider.getApplicationContext<VinilosApplication>().container =
            AppContainer(artistasRepositorio = artistas)
    }

    @After
    fun detenerServidor() {
        servidor?.shutdown()
    }

    private fun esperarA(tag: String, milis: Long = 15_000) =
        rule.waitUntil(milis) { rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }

    private fun esperarQueDesaparezca(tag: String, milis: Long = 5_000) =
        rule.waitUntil(milis) { rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isEmpty() }

    /** Visitante -> inicio -> "Ver artistas" (sin esperar datos; también sirve con MockWebServer). */
    private fun abrirArtistasComoVisitante(evidencia: String) {
        esperarA(TagEntrarVisitante)
        rule.onNodeWithTag(TagEntrarVisitante).performScrollTo().performClick()
        esperarA(TagInicio)
        EvidenciaE2E.capturar(rule, evidencia, "01_inicio_visitante")
        rule.onNodeWithTag(TagVerArtistas).performScrollTo().performClick()
        esperarA(TagArtistas)
    }

    private fun esperarElListadoReal(): List<Artist> {
        rule.waitUntil(15_000) { artistas.lista != null }
        return requireNotNull(artistas.lista)
    }

    /** Para los escenarios contra el backend real: abre artistas y devuelve el listado real capturado. */
    private fun verArtistasReales(evidencia: String): List<Artist> {
        abrirArtistasComoVisitante(evidencia)
        val lista = esperarElListadoReal()
        esperarA(tagArtista(lista.first()))
        return lista
    }

    /** Desplaza la lista hasta la fila (en pantallas pequeñas puede no estar compuesta aún). */
    private fun fila(artista: Artist): SemanticsNodeInteraction {
        rule.onNodeWithTag(TagListaArtistas).performScrollToNode(hasTestTag(tagArtista(artista)))
        return rule.onNodeWithTag(tagArtista(artista))
    }

    private fun buscar(texto: String) {
        rule.onNodeWithTag(TagListaArtistas).performScrollToNode(hasTestTag(TagBuscadorArtistas))
        rule.onNodeWithTag(TagBuscadorArtistas).performTextInput(texto)
        rule.waitForIdle()
    }

    private fun borrarBusqueda() {
        rule.onNodeWithTag(TagBuscadorArtistas).performTextClearance()
        rule.waitForIdle()
    }

    private fun contadorDice(texto: String) {
        rule.onNodeWithTag(TagListaArtistas).performScrollToNode(hasTestTag(TagContadorArtistas))
        rule.onNodeWithTag(TagContadorArtistas).assertTextEquals(texto)
    }

    private fun textoContador(cantidad: Int) = if (cantidad == 1) "1 artista" else "$cantidad artistas"

    private fun noAparece(artista: Artist) =
        rule.onAllNodesWithTag(tagArtista(artista)).assertCountEquals(0)

    /** Igual que la normalización de búsqueda de producción: sin tildes y en minúsculas. */
    private val marcasDiacriticas = Regex("\\p{InCombiningDiacriticalMarks}+")
    private fun normalizado(texto: String) =
        Normalizer.normalize(texto, Normalizer.Form.NFD).replace(marcasDiacriticas, "").lowercase(Locale.ROOT)

    /** La subcadena más corta del nombre de [objetivo] que no aparece en ningún otro artista de [lista]. */
    private fun substringDiscriminante(objetivo: Artist, lista: List<Artist>): String {
        val nombre = normalizado(objetivo.name)
        val otros = lista.filterNot { it.id == objetivo.id && it.type == objetivo.type }
            .map { normalizado(it.name) }
        for (longitud in 2..nombre.length) {
            for (inicio in 0..(nombre.length - longitud)) {
                val candidato = nombre.substring(inicio, inicio + longitud)
                if (otros.none { candidato in it }) return candidato
            }
        }
        return nombre
    }

    /** Un término que no coincide (ni como subcadena) con ningún artista real de [lista]. */
    private fun terminoSinCoincidencias(lista: List<Artist>): String {
        val normalizados = lista.map { normalizado(it.name) }
        var sufijo = 0
        var candidato = "zzzqx"
        while (normalizados.any { candidato in it }) {
            sufijo++
            candidato = "zzzqx$sufijo"
        }
        return candidato
    }

    private fun etiquetaDe(artista: Artist) = if (artista.type == ArtistType.BANDA) "Banda" else "Músico"

    /**
     * Artistas cuyo nombre no se repite en ningún otro de [lista]. El backend real puede listar el
     * mismo nombre como músico y como banda (comparten identidad por nombre, no por id): la búsqueda
     * por nombre no puede distinguir esos casos, así que los escenarios de búsqueda solo usan
     * artistas con nombre único.
     */
    private fun conNombreUnico(lista: List<Artist>): List<Artist> {
        val conteoPorNombre = lista.groupingBy { normalizado(it.name) }.eachCount()
        return lista.filter { conteoPorNombre.getValue(normalizado(it.name)) == 1 }
    }

    /** FN03-ESC01: ver el listado real de artistas desde el menú principal. */
    @Test
    fun elVisitanteVeMusicosYBandasEnOrdenConImagenNombreYEtiqueta() {
        val evidencia = "FN03_ESC01"
        ActivityScenario.launch(MainActivity::class.java).use {
            val lista = verArtistasReales(evidencia)

            rule.onNodeWithTag(TagBuscadorArtistas).assertIsDisplayed()
            contadorDice(textoContador(lista.size))

            // Los dos primeros del listado real (ya viene ordenado alfabéticamente) deben verse en ese orden.
            if (lista.size >= 2) {
                val primeros = lista.take(2)
                rule.onNodeWithTag(TagListaArtistas).performScrollToNode(hasTestTag(tagArtista(primeros[0])))
                val filas = rule.onAllNodes(hasTestTag(tagArtista(primeros[0])) or hasTestTag(tagArtista(primeros[1])))
                filas.assertCountEquals(2)
                filas[0].assert(hasTestTag(tagArtista(primeros[0])))
                filas[1].assert(hasTestTag(tagArtista(primeros[1])))
            }

            lista.forEach { artista ->
                // Se ancla por el tag de esa fila (tipo+id), no por nombre/descripción: el backend real
                // puede repetir el mismo nombre en un músico y una banda (comparten identidad por nombre).
                val filaDelArtista = hasTestTag(tagArtista(artista))
                fila(artista).assert(hasText(artista.name)).assert(hasText(etiquetaDe(artista)))
                rule.onNode(filaDelArtista and hasContentDescription("Imagen de ${artista.name}")).assertExists()
            }
            EvidenciaE2E.capturar(rule, evidencia, "02_listado_artistas")
        }
    }

    /** FN03-ESC02: filtrar por nombre a medida que se escribe, sin distinguir mayúsculas ni tildes. */
    @Test
    fun elBuscadorFiltraPorNombreMientrasSeEscribe() {
        val evidencia = "FN03_ESC02"
        ActivityScenario.launch(MainActivity::class.java).use {
            val lista = verArtistasReales(evidencia)
            EvidenciaE2E.capturar(rule, evidencia, "02_listado_completo")

            // Solo artistas con nombre único: el backend real puede repetir el mismo nombre en un
            // músico y una banda, y ahí la búsqueda por nombre no puede distinguir a cuál se refiere.
            val candidatos = conNombreUnico(lista)
            Assume.assumeTrue(
                "El backend conectado no tiene artistas con nombre único para probar la búsqueda",
                candidatos.isNotEmpty()
            )

            val primerObjetivo = candidatos.first()
            val terminoUno = substringDiscriminante(primerObjetivo, lista)
            buscar(terminoUno)
            contadorDice(textoContador(1))
            fila(primerObjetivo).assertIsDisplayed()
            (lista - primerObjetivo).forEach { noAparece(it) }
            EvidenciaE2E.capturar(rule, evidencia, "03_busqueda_discriminante")
            borrarBusqueda()

            // En mayúsculas también debe encontrar a otro artista real único, sin distinguir tildes.
            if (candidatos.size > 1) {
                val segundoObjetivo = candidatos[1]
                val terminoDos = substringDiscriminante(segundoObjetivo, lista)
                buscar(terminoDos.uppercase(Locale.ROOT))
                contadorDice(textoContador(1))
                fila(segundoObjetivo).assertIsDisplayed()
                (lista - segundoObjetivo).forEach { noAparece(it) }
                Espresso.closeSoftKeyboard()
                EvidenciaE2E.capturar(rule, evidencia, "04_busqueda_mayusculas")
                borrarBusqueda()
            }

            contadorDice(textoContador(lista.size))
            EvidenciaE2E.capturar(rule, evidencia, "05_busqueda_borrada")
        }
    }

    /** FN03-ESC03: una búsqueda sin coincidencias muestra el mensaje y se puede corregir. */
    @Test
    fun unaBusquedaSinCoincidenciasMuestraElMensaje() {
        val evidencia = "FN03_ESC03"
        ActivityScenario.launch(MainActivity::class.java).use {
            val lista = verArtistasReales(evidencia)
            val termino = terminoSinCoincidencias(lista)

            buscar(termino)
            Espresso.closeSoftKeyboard()
            esperarA(TagVacioArtistas)
            rule.onNodeWithTag(TagVacioArtistas).assertTextEquals("No se encontraron artistas")
            rule.onNodeWithText("Prueba con otro nombre.").assertIsDisplayed()
            rule.onNodeWithTag(TagBuscadorArtistas).assertTextEquals(termino)
            lista.forEach { noAparece(it) }
            EvidenciaE2E.capturar(rule, evidencia, "02_sin_coincidencias")

            borrarBusqueda()
            esperarA(tagArtista(lista.first()))
            contadorDice(textoContador(lista.size))
            EvidenciaE2E.capturar(rule, evidencia, "03_listado_restaurado")
        }
    }

    /** FN03-ESC04: seleccionar un artista real lleva a su detalle y "Atrás" vuelve con la búsqueda. */
    @Test
    fun seleccionarUnArtistaLlevaASuDetalleYAtrasVuelveAlListado() {
        val evidencia = "FN03_ESC04"
        ActivityScenario.launch(MainActivity::class.java).use {
            val lista = verArtistasReales(evidencia)
            val candidatos = conNombreUnico(lista)
            Assume.assumeTrue(
                "El backend conectado no tiene artistas con nombre único para probar la búsqueda",
                candidatos.isNotEmpty()
            )
            val objetivo = candidatos.first()
            val termino = substringDiscriminante(objetivo, lista)

            buscar(termino)
            Espresso.closeSoftKeyboard()
            (lista - objetivo).forEach { noAparece(it) }
            EvidenciaE2E.capturar(rule, evidencia, "02_busqueda_objetivo")
            fila(objetivo).performClick()

            esperarA(TagDetalleArtista)
            esperarQueDesaparezca(TagArtistas)
            rule.onNodeWithText("Detalle del artista").assertIsDisplayed()
            rule.onNodeWithTag(TagNombreDetalleArtista).assertTextEquals(objetivo.name)
            rule.onNodeWithTag(TagTipoDetalleArtista).assertTextEquals(etiquetaDe(objetivo))
            EvidenciaE2E.capturar(rule, evidencia, "03_detalle_objetivo")

            rule.onNodeWithTag(TagAtras).performClick()
            esperarA(TagArtistas)
            esperarQueDesaparezca(TagDetalleArtista)
            rule.onNodeWithTag(TagBuscadorArtistas).assertTextEquals(termino)
            fila(objetivo).assertIsDisplayed()
            (lista - objetivo).forEach { noAparece(it) }
            EvidenciaE2E.capturar(rule, evidencia, "04_regreso_listado")
        }
    }

    /** Servidor local con músicos y bandas; [caido] hace que corte la conexión, como un backend apagado. */
    private fun iniciarServidor(musicos: String, bandas: String, caido: () -> Boolean): MockWebServer {
        val nuevo = MockWebServer().apply {
            dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse = when {
                    caido() -> MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START)
                    request.path == "/musicians" -> MockResponse().setBody(musicos)
                    request.path == "/bands" -> MockResponse().setBody(bandas)
                    else -> MockResponse().setResponseCode(404)
                }
            }
            start()
        }
        servidor = nuevo
        // "localhost" es uno de los hosts con HTTP permitido en network_security_config.xml.
        ApplicationProvider.getApplicationContext<VinilosApplication>().container =
            AppContainer(baseUrl = "http://localhost:${nuevo.port}/")
        return nuevo
    }

    /** FN03-ESC05: con el backend caído se ve el error y "Reintentar" recupera el listado. */
    @Test
    fun conElBackendCaidoSeVeElErrorYReintentarRecuperaElListado() {
        val evidencia = "FN03_ESC05"
        var caido = true
        iniciarServidor(
            musicos = """[{"id": 100, "name": "Rubén Blades Bellido de Luna", "image": null}]""",
            bandas = """[{"id": 101, "name": "Queen", "image": null}]""",
            caido = { caido }
        )

        ActivityScenario.launch(MainActivity::class.java).use {
            abrirArtistasComoVisitante(evidencia)

            esperarA(TagErrorArtistas)
            rule.onNodeWithText("No se pudo cargar el listado de artistas").assertIsDisplayed()
            rule.onNodeWithTag(TagReintentarArtistas).assertIsDisplayed()
            EvidenciaE2E.capturar(rule, evidencia, "02_backend_caido")

            // El backend "vuelve" y la persona pulsa Reintentar.
            caido = false
            rule.onNodeWithTag(TagReintentarArtistas).performClick()

            esperarA(TagContadorArtistas)
            contadorDice("2 artistas")
            EvidenciaE2E.capturar(rule, evidencia, "03_recuperado")
        }
    }

    /** FN03-ESC06: si no hay músicos ni bandas registrados se muestra el mensaje. */
    @Test
    fun sinArtistasRegistradosSeMuestraElMensaje() {
        val evidencia = "FN03_ESC06"
        iniciarServidor(musicos = "[]", bandas = "[]", caido = { false })

        ActivityScenario.launch(MainActivity::class.java).use {
            abrirArtistasComoVisitante(evidencia)

            esperarA(TagVacioArtistas)
            rule.onNodeWithTag(TagVacioArtistas).assertTextEquals("No se encontraron artistas")
            rule.onAllNodesWithTag(TagContadorArtistas).assertCountEquals(0)
            EvidenciaE2E.capturar(rule, evidencia, "02_sin_artistas")
        }
    }

    /** FN03-ESC07: al girar la pantalla se conservan la búsqueda y el listado real, sin volver a consultar. */
    @Test
    fun alGirarLaPantallaSeConservanLaBusquedaYElListadoSinVolverAConsultar() {
        val evidencia = "FN03_ESC07"
        ActivityScenario.launch(MainActivity::class.java).use { escenario ->
            val lista = verArtistasReales(evidencia)
            val candidatos = conNombreUnico(lista)
            Assume.assumeTrue(
                "El backend conectado no tiene artistas con nombre único para probar la búsqueda",
                candidatos.isNotEmpty()
            )
            val objetivo = candidatos.last()
            val termino = substringDiscriminante(objetivo, lista)

            buscar(termino)
            Espresso.closeSoftKeyboard()
            fila(objetivo).assertIsDisplayed()
            (lista - objetivo).forEach { noAparece(it) }
            EvidenciaE2E.capturar(rule, evidencia, "02_antes_de_girar")

            // Girar de verdad (no solo recrear la Activity) para que la evidencia se vea en horizontal.
            escenario.onActivity { it.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE }
            rule.waitUntil(5_000) {
                var horizontal = false
                escenario.onActivity {
                    horizontal = it.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                }
                horizontal
            }
            esperarA(TagArtistas)
            rule.waitForIdle()

            rule.onNodeWithTag(TagBuscadorArtistas).assertTextEquals(termino)
            fila(objetivo).assertIsDisplayed()
            (lista - objetivo).forEach { noAparece(it) }
            contadorDice(textoContador(1))
            assertEquals(1, artistas.consultas)
            EvidenciaE2E.capturar(rule, evidencia, "03_despues_de_girar")
        }
    }
}
