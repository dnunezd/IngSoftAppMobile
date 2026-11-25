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
 * Barra superior con la flecha de atrás (opcional), el logo y el nombre de la app. [accion] se muestra al
 * final de la barra, por ejemplo el indicador de actualización.
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
 * Indicador pequeño de que se están actualizando los datos guardados que ya se ven en pantalla
 * (add-cache-consultas, D6). Es el mismo que usa el catálogo junto a su encabezado.
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
 * Muestra [mensaje] en [avisos] una vez cuando [mostrar] pasa a `true`. Llama a [onMostrado] en un
 * `finally`: si la pantalla sale antes de que termine, el aviso queda como mostrado y no se repite al
 * volver, igual que en el catálogo.
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

/** Etiqueta pequeña en mayúsculas sobre fondo suave (por ejemplo "ACCESO LIBRE"). */
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
 * Marca un adorno puramente decorativo (etiquetas, franjas y titulares de ambientación): no expone
 * ningún texto a los lectores de pantalla. Se usa `clearAndSetSemantics` y no `invisibleToUser`
 * porque algunos lectores (Voice Assistant de Samsung) siguen leyendo los nodos marcados como
 * invisibles. Debe ir al final de la cadena de modificadores para no borrar un `testTag`.
 */
fun Modifier.decorativo(): Modifier = clearAndSetSemantics { }

/**
 * Marca un título de pantalla o de sección como encabezado para TalkBack. Une los descendientes para
 * que un titular partido en varios textos ("Nuevo" + "álbum") se lea como un solo encabezado.
 */
fun Modifier.encabezado(): Modifier = semantics(mergeDescendants = true) { heading() }
