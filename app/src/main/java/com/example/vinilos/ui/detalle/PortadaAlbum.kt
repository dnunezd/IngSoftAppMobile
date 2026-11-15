package com.example.vinilos.ui.detalle

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import coil.compose.AsyncImage
import com.example.vinilos.R

/** Parte del contenedor que ocupa el ícono de respaldo: proporcionado en la miniatura y en el detalle. */
private const val FRACCION_ICONO_RESPALDO = 0.4f

/**
 * Portada del álbum (optimizar-cache-e-imagenes, D1): un solo [AsyncImage] sobre fondo neutro. Sin URL,
 * mientras carga o si falla, Coil dibuja el ícono vectorial de disco centrado y teñido; cuando la imagen
 * carga, deja de dibujarlo. No se decodifica ningún mapa de bits de respaldo ni se compone un nodo aparte
 * para el ícono.
 *
 * Se usa en el catálogo, el detalle del álbum, la vista previa de crear álbum y los álbumes del artista.
 */
@Composable
fun PortadaAlbum(
    url: String?,
    modifier: Modifier = Modifier,
    contentDescription: String = stringResource(R.string.detalle_portada),
    contentScale: ContentScale = ContentScale.Fit
) {
    val icono = painterResource(R.drawable.ic_cat_album)
    val tinte = MaterialTheme.colorScheme.onSurfaceVariant
    val respaldo = remember(icono, tinte) { IconoCentrado(icono, ColorFilter.tint(tinte), FRACCION_ICONO_RESPALDO) }
    AsyncImage(
        model = url?.takeIf { it.isNotBlank() },
        contentDescription = contentDescription,
        placeholder = respaldo,
        error = respaldo,
        fallback = respaldo,
        contentScale = contentScale,
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant)
    )
}

/**
 * Dibuja [icono] centrado, con lado igual a [fraccion] del lado menor del espacio, y teñido con [tinte]. No
 * tiene tamaño propio, así que Coil le da todo el espacio de la imagen sin importar el `ContentScale`.
 */
private class IconoCentrado(
    private val icono: Painter,
    private val tinte: ColorFilter,
    private val fraccion: Float
) : Painter() {
    override val intrinsicSize: Size get() = Size.Unspecified

    override fun DrawScope.onDraw() {
        val lado = size.minDimension * fraccion
        translate(left = (size.width - lado) / 2f, top = (size.height - lado) / 2f) {
            with(icono) { draw(Size(lado, lado), colorFilter = tinte) }
        }
    }
}
