package com.example.vinilos.ui.ingreso

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.vinilos.R
import com.example.vinilos.ui.idiomaDeLaApp

const val TagAtras = "atras"

/**
 * Top bar with an optional back arrow, the logo, and the app name. [accion] is shown at the
 * end of the bar, for example the refresh indicator.
 */
@Composable
fun EncabezadoVinilo(
    modifier: Modifier = Modifier,
    marca: String = " ♪",
    onAtras: (() -> Unit)? = null,
    accion: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (onAtras != null) {
            IconButton(onClick = onAtras, modifier = Modifier.testTag(TagAtras)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.atras_descripcion)
                )
            }
        }
        Image(
            painter = painterResource(R.drawable.vinilo_logo),
            contentDescription = stringResource(R.string.logo_descripcion),
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
        )
        Text(
            text = buildAnnotatedString {
                append(stringResource(R.string.marca))
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.secondary)) { append(marca) }
            },
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (accion != null) {
            Spacer(Modifier.weight(1f))
            accion()
        }
    }
}

/**
 * Small indicator that the saved data already shown on screen is being refreshed
 * (add-cache-consultas, D6). It's the same one used by the catalog next to its header.
 */
@Composable
fun IndicadorActualizando(descripcion: String, tag: String, modifier: Modifier = Modifier) {
    CircularProgressIndicator(
        modifier = modifier
            .size(18.dp)
            .semantics { contentDescription = descripcion }
            .testTag(tag),
        strokeWidth = 2.dp
    )
}

/**
 * Shows [mensaje] in [avisos] once when [mostrar] turns `true`. Calls [onMostrado] in a
 * `finally`: if the screen exits before it finishes, the notice is marked as shown and doesn't
 * repeat when returning, the same as in the catalog.
 */
@Composable
fun AvisoUnaVez(mostrar: Boolean, mensaje: String, avisos: SnackbarHostState, onMostrado: () -> Unit) {
    LaunchedEffect(mostrar) {
        if (mostrar) {
            try {
                avisos.showSnackbar(mensaje)
            } finally {
                onMostrado()
            }
        }
    }
}

/** Small uppercase label on a soft background (for example "FREE ACCESS"). */
@Composable
fun ChipEtiqueta(
    texto: String,
    modifier: Modifier = Modifier,
    fondo: Color = MaterialTheme.colorScheme.primaryContainer,
    contenido: Color = MaterialTheme.colorScheme.primary
) {
    Text(
        text = texto.uppercase(idiomaDeLaApp()),
        style = MaterialTheme.typography.labelSmall,
        color = contenido,
        modifier = modifier
            .clip(CircleShape)
            .background(fondo)
            .padding(horizontal = 12.dp, vertical = 4.dp)
    )
}

/**
 * Marks a purely decorative adornment (labels, strips, and ambience headlines): it exposes no
 * text to screen readers. `clearAndSetSemantics` is used instead of `invisibleToUser` because
 * some readers (Samsung's Voice Assistant) still read nodes marked as invisible. It must go at
 * the end of the modifier chain so it doesn't erase a `testTag`.
 */
fun Modifier.decorativo(): Modifier = clearAndSetSemantics { }

/**
 * Marks a screen or section title as a header for TalkBack. Merges descendants so that a
 * headline split across several texts ("New" + "album") is read as a single header.
 */
fun Modifier.encabezado(): Modifier = semantics(mergeDescendants = true) { heading() }
