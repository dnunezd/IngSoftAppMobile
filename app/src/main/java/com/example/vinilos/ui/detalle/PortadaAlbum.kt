package com.example.vinilos.ui.detalle

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.vinilos.R

sealed interface EstadoPortada {
    data object Provisional : EstadoPortada
    data object Cargando : EstadoPortada
    data object Error : EstadoPortada
    data class Disponible(val imagen: Painter) : EstadoPortada
}

@Composable
fun PortadaAlbum(
    url: String?,
    estado: EstadoPortada = EstadoPortada.Provisional,
    modifier: Modifier = Modifier
) {
    if (!url.isNullOrBlank() && estado == EstadoPortada.Cargando) {
        Box(modifier, contentAlignment = Alignment.Center) {
            val descripcion = stringResource(R.string.detalle_portada_cargando)
            CircularProgressIndicator(Modifier.semantics { contentDescription = descripcion })
        }
        return
    }
    val imagenDisponible = if (!url.isNullOrBlank() && estado is EstadoPortada.Disponible) {
        estado.imagen
    } else {
        null
    }
    Image(
        painter = imagenDisponible ?: painterResource(R.drawable.inicio_albumes),
        contentDescription = stringResource(
            if (imagenDisponible != null) R.string.detalle_portada
            else R.string.detalle_imagen_provisional
        ),
        contentScale = ContentScale.Fit,
        modifier = modifier
    )
}
