package com.example.vinilos.ui.theme

import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * Colores del botón principal de un formulario. Deshabilitado usa un par de la paleta con contraste
 * suficiente; mientras se envía ([enviando]) conserva el naranja pleno con el texto blanco (4,86:1), porque
 * el naranja atenuado bajaba a 3,08:1.
 */
@Composable
fun coloresBotonPrincipal(enviando: Boolean = false): ButtonColors =
    if (enviando) {
        ButtonDefaults.buttonColors(
            disabledContainerColor = MaterialTheme.colorScheme.primary,
            disabledContentColor = MaterialTheme.colorScheme.onPrimary
        )
    } else {
        ButtonDefaults.buttonColors(
            disabledContainerColor = BotonDesactivadoFondo,
            disabledContentColor = BotonDesactivadoTexto
        )
    }
