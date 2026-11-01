package com.example.vinilos.ui.detalle

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import coil.compose.AsyncImage
import com.example.vinilos.R

/**
 * Portada del álbum con el mismo patrón de HU03: [AsyncImage] de Coil con la ilustración local
 * como respaldo mientras no hay URL, carga o falla la descarga.
 */
@Composable
fun PortadaAlbum(
    url: String?,
    modifier: Modifier = Modifier,
    contentDescription: String = stringResource(R.string.detalle_portada),
    contentScale: ContentScale = ContentScale.Fit
) {
    val porDefecto = painterResource(R.drawable.inicio_albumes)
    AsyncImage(
        model = url?.takeIf { it.isNotBlank() },
        contentDescription = contentDescription,
        placeholder = porDefecto,
        error = porDefecto,
        fallback = porDefecto,
        contentScale = contentScale,
        modifier = modifier
    )
}
