package com.example.vinilos.ui.detalle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import coil.compose.AsyncImage
import com.example.vinilos.R

/** Ícono de disco que se ve mientras la portada carga, si falta o si no se puede descargar. */
const val TagRespaldoPortada = "portada_respaldo"

/** Parte del contenedor que ocupa el ícono de respaldo: proporcionado en la miniatura y en el detalle. */
private const val FRACCION_ICONO_RESPALDO = 0.4f

/**
 * Portada del álbum con el patrón de las miniaturas de HU04 (optimizar-cache-e-imagenes, D1): el ícono
 * vectorial de disco queda debajo, sobre fondo neutro, y [AsyncImage] lo tapa cuando la imagen carga. Sin
 * URL, mientras carga o si falla, `AsyncImage` no dibuja nada y se ve el ícono. Así no se decodifica ningún
 * mapa de bits de respaldo por cada portada.
 */
@Composable
fun PortadaAlbum(
    url: String?,
    modifier: Modifier = Modifier,
    contentDescription: String = stringResource(R.string.detalle_portada),
    contentScale: ContentScale = ContentScale.Fit
) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_cat_album),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxSize(FRACCION_ICONO_RESPALDO).testTag(TagRespaldoPortada)
        )
        AsyncImage(
            model = url?.takeIf { it.isNotBlank() },
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = Modifier.matchParentSize()
        )
    }
}
