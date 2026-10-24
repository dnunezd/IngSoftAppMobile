package com.example.vinilos.ui.artistas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.vinilos.R
import com.example.vinilos.data.model.ArtistType
import com.example.vinilos.ui.ingreso.EncabezadoVinilo

const val TagDetalleArtista = "detalle_artista"
const val TagNombreDetalleArtista = "detalle_artista_nombre"
const val TagTipoDetalleArtista = "detalle_artista_tipo"

/** Destino provisional del detalle del artista: HU04 lo reemplaza por el detalle real. */
@Composable
fun PantallaArtistaProvisional(
    nombre: String,
    tipo: ArtistType,
    onAtras: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag(TagDetalleArtista)
    ) {
        EncabezadoVinilo(modifier = Modifier.padding(horizontal = 12.dp), onAtras = onAtras)
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(
                text = stringResource(R.string.artista_detalle_titulo),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = nombre,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.testTag(TagNombreDetalleArtista)
            )
            Text(
                text = stringResource(
                    if (tipo == ArtistType.BANDA) R.string.artistas_tipo_banda else R.string.artistas_tipo_musico
                ),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.testTag(TagTipoDetalleArtista)
            )
        }
    }
}
