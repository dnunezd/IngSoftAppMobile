package com.example.vinilos.ui.catalogo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.vinilos.R
import com.example.vinilos.data.model.Session
import com.example.vinilos.ui.ingreso.ChipEtiqueta
import com.example.vinilos.ui.ingreso.TagAtras

/** Encabezado del catálogo y del formulario: flecha de atrás, etiqueta del rol y título. */
@Composable
fun EncabezadoConRol(
    session: Session,
    titulo: String,
    onAtras: () -> Unit,
    modifier: Modifier = Modifier,
    atrasHabilitado: Boolean = true
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconButton(
            onClick = onAtras,
            enabled = atrasHabilitado,
            modifier = Modifier
                .testTag(TagAtras)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.atras_descripcion),
                tint = MaterialTheme.colorScheme.secondary
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            val rol = when (session) {
                Session.Visitor -> stringResource(R.string.catalogo_etiqueta_visitante)
                is Session.Collector -> stringResource(R.string.catalogo_etiqueta_coleccionista)
                Session.None -> null
            }
            if (rol != null) {
                ChipEtiqueta(
                    texto = rol,
                    fondo = MaterialTheme.colorScheme.onBackground,
                    contenido = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.testTag(TagRolActivo)
                )
            }
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}
