package com.example.vinilos.i18n

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * El inglés (values-en) debe tener los mismos textos y plurales que el español (values), con los
 * mismos marcadores de formato (internacionalizar-ingles). Así, un texto nuevo sin traducir, o
 * traducido con otros parámetros, falla aquí y no en tiempo de ejecución.
 */
class ParidadDeTraduccionesTest {

    private val marcador = Regex("""%(\d+\$)?[sdf]""")

    /** Clave (string o plurals/cantidad) → marcadores en orden. */
    private fun leer(carpeta: String): Map<String, List<String>> {
        val archivo = File("src/main/res/$carpeta/strings.xml")
        assertTrue("No existe ${archivo.absolutePath}", archivo.exists())
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(archivo)
        val textos = mutableMapOf<String, List<String>>()
        val strings = doc.getElementsByTagName("string")
        for (i in 0 until strings.length) {
            val e = strings.item(i) as Element
            if (e.getAttribute("translatable") == "false") continue
            textos[e.getAttribute("name")] = marcador.findAll(e.textContent).map { it.value }.toList()
        }
        val plurales = doc.getElementsByTagName("plurals")
        for (i in 0 until plurales.length) {
            val p = plurales.item(i) as Element
            val items = p.getElementsByTagName("item")
            for (j in 0 until items.length) {
                val item = items.item(j) as Element
                textos["${p.getAttribute("name")}/${item.getAttribute("quantity")}"] =
                    marcador.findAll(item.textContent).map { it.value }.toList()
            }
        }
        return textos
    }

    private val espanol = leer("values")
    private val ingles = leer("values-en")

    @Test
    fun elInglesTieneExactamenteLosMismosTextosQueElEspanol() {
        assertEquals("Faltan en values-en", emptySet<String>(), espanol.keys - ingles.keys)
        assertEquals("Sobran en values-en", emptySet<String>(), ingles.keys - espanol.keys)
    }

    @Test
    fun cadaTraduccionConservaSusMarcadoresDeFormato() {
        val distintos = espanol.filter { (clave, marcas) -> ingles[clave] != null && ingles[clave] != marcas }
        assertEquals("Marcadores distintos", emptyMap<String, List<String>>(), distintos)
    }

    @Test
    fun cadaIdiomaDeclaraSuCodigo() {
        assertTrue(File("src/main/res/values/strings.xml").readText().contains("<string name=\"idioma\">es</string>"))
        assertTrue(File("src/main/res/values-en/strings.xml").readText().contains("<string name=\"idioma\">en</string>"))
    }
}
