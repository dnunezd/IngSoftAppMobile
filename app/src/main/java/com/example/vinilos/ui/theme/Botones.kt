package com.example.vinilos.ui.theme

import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * Colors for a form's main button. Disabled state uses a palette pair with sufficient
 * contrast; while submitting ([enviando]) it keeps the full orange with white text (4.86:1), because
 * the dimmed orange dropped to 3.08:1.
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
