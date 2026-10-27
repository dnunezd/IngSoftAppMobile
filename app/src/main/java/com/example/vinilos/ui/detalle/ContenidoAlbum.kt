package com.example.vinilos.ui.detalle

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.vinilos.R
import com.example.vinilos.ui.ingreso.EncabezadoVinilo

@Composable
fun ContenidoAlbum(
    presentacion: AlbumPresentacion,
    onAtras: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { EncabezadoVinilo(onAtras = onAtras) }
        item {
            PortadaAlbum(
                url = presentacion.album.portada,
                estado = EstadoPortada.Provisional,
                modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(24.dp))
            )
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(presentacion.album.nombre, style = MaterialTheme.typography.headlineMedium)
                    Text(presentacion.album.descripcion, style = MaterialTheme.typography.bodyLarge)
                    Text(presentacion.album.genero, style = MaterialTheme.typography.labelLarge)
                    Text(presentacion.album.disquera, style = MaterialTheme.typography.bodyMedium)
                    Text(presentacion.fechaLanzamiento, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        if (presentacion.album.artistas.isNotEmpty()) {
            item {
                Text(
                    stringResource(R.string.detalle_artistas),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            items(presentacion.album.artistas) { artista ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.inicio_artistas),
                        contentDescription = stringResource(R.string.detalle_artista_provisional, artista.nombre),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(64.dp).clip(CircleShape)
                    )
                    Text(artista.nombre, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        item {
            Text(
                stringResource(R.string.detalle_canciones),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        presentacion.mensajeCanciones?.let { mensaje ->
            item { Text(stringResource(mensaje), style = MaterialTheme.typography.bodyLarge) }
        }
        items(presentacion.album.canciones) { cancion ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(cancion.nombre, modifier = Modifier.weight(1f))
                Text(cancion.duracion, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Text(
                stringResource(R.string.detalle_comentarios),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
        presentacion.mensajeComentarios?.let { mensaje ->
            item { Text(stringResource(mensaje), style = MaterialTheme.typography.bodyLarge) }
        }
        items(presentacion.album.comentarios) { comentario ->
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(comentario.descripcion)
                    Text(
                        comentario.calificacion?.let { stringResource(R.string.detalle_calificacion, it) }
                            ?: stringResource(R.string.detalle_sin_calificaciones),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        item {
            Text(stringResource(R.string.detalle_promedio), style = MaterialTheme.typography.titleMedium)
            Text(
                presentacion.promedio ?: stringResource(R.string.detalle_sin_calificaciones),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
