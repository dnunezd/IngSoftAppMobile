package com.example.vinilos.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

/**
 * Contraste WCAG de los pares texto/fondo de la paleta (corregir-accesibilidad-scanner).
 * 4,5:1 para texto normal; 3:1 solo se acepta en texto grande y no se usa aquí.
 */
class ContrasteDePaletaTest {

    private fun canal(valor: Float): Double {
        val v = valor.toDouble()
        return if (v <= 0.03928) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
    }

    private fun luminancia(color: Color): Double =
        0.2126 * canal(color.red) + 0.7152 * canal(color.green) + 0.0722 * canal(color.blue)

    private fun razon(a: Color, b: Color): Double {
        val clara = maxOf(luminancia(a), luminancia(b))
        val oscura = minOf(luminancia(a), luminancia(b))
        return (clara + 0.05) / (oscura + 0.05)
    }

    private fun exigir(nombre: String, texto: Color, fondo: Color, minimo: Double = 4.5) {
        val obtenida = razon(texto, fondo)
        assertTrue("$nombre: contraste %.2f:1, mínimo $minimo:1".format(obtenida), obtenida >= minimo)
    }

    @Test
    fun elTextoBlancoSobreElNaranjaDeContrasteCumple() {
        exigir("blanco sobre NaranjaContraste", Superficie, NaranjaContraste)
    }

    @Test
    fun elNaranjaDeContrasteComoTextoSobreElFondoCumple() {
        exigir("NaranjaContraste sobre Fondo", NaranjaContraste, Fondo)
        exigir("NaranjaContraste sobre Superficie", NaranjaContraste, Superficie)
    }

    @Test
    fun losChipsYEtiquetasSobreFondosTenuesCumplen() {
        exigir("ChipTexto sobre ChipFondo", ChipTexto, ChipFondo)
        exigir("ChipTexto sobre ChipColeccionistaFondo", ChipTexto, ChipColeccionistaFondo)
        exigir("NaranjaOscuro sobre NaranjaSuave", NaranjaOscuro, NaranjaSuave)
        exigir("TextoTenue sobre FondoImagen", TextoTenue, FondoImagen)
        exigir("TextoTenue sobre Fondo", TextoTenue, Fondo)
    }

    @Test
    fun elBotonDeshabilitadoCumple() {
        exigir("BotonDesactivadoTexto sobre BotonDesactivadoFondo", BotonDesactivadoTexto, BotonDesactivadoFondo)
    }

    @Test
    fun lasAccionesDeDialogosSobreLosContenedoresDeSuperficieCumplen() {
        // surfaceContainerHigh (diálogos) es Superficie; surfaceContainer (menús) es Fondo.
        exigir("NaranjaContraste sobre Superficie", NaranjaContraste, Superficie)
        exigir("NaranjaContraste sobre Fondo", NaranjaContraste, Fondo)
        exigir("NaranjaOscuro sobre NaranjaSuave (chip y avatar del comentario publicado)", NaranjaOscuro, NaranjaSuave)
    }

    @Test
    fun elTitularGrisSobreElFondoCumple() {
        exigir("TitularGris sobre Fondo", TitularGris, Fondo)
    }

    @Test
    fun lasInitialsBlancasSobreLosDosExtremosDeCadaAvatarCumplen() {
        listOf(AvatarAzulClaro, AvatarAzul, AvatarGrisClaro, AvatarGris).forEach {
            exigir("blanco sobre avatar $it", Superficie, it)
        }
    }
}
