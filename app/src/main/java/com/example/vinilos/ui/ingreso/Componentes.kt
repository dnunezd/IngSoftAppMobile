package com.example.vinilos.ui.ingreso

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.vinilos.R

const val TagAtras = "atras"

/** Barra superior con la flecha de atrás (opcional), el logo y el nombre de la app. */
@Composable
fun EncabezadoVinilo(
    modifier: Modifier = Modifier,
    marca: String = " ♪",
    onAtras: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp),
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
        text = texto.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = contenido,
        modifier = modifier
            .clip(CircleShape)
            .background(fondo)
            .padding(horizontal = 12.dp, vertical = 4.dp)
    )
}
