package com.example.vinilos.ui.detail

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

/** Fraction of the container occupied by the fallback icon: used for both the thumbnail and the detail. */
private const val FRACCION_ICONO_RESPALDO = 0.4f

/**
 * Album cover (optimizar-cache-e-imagenes, D1): a single [AsyncImage] over a neutral background. With no
 * URL, while loading, or on failure, Coil draws the centered, tinted vinyl icon; once the image
 * loads, it stops drawing it. No fallback bitmap is decoded, and no separate node is composed
 * for the icon.
 *
 * Used in the catalog, the album detail, the create-album preview, and the artist's albums.
 */
@Composable
fun AlbumCover(
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
 * Draws [icono] centered, with a side equal to [fraccion] of the space's shorter side, and tinted with [tinte]. It
 * has no intrinsic size, so Coil gives it the full image space regardless of `ContentScale`.
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
