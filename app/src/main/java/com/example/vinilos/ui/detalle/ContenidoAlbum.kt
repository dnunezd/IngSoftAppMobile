package com.example.vinilos.ui.detalle

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
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
                modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(24.dp))
            )
        }
        item { TarjetaMetadatos(presentacion) }
        if (presentacion.album.artistas.isNotEmpty()) {
            item {
                Text(
                    stringResource(R.string.detalle_artistas),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            items(presentacion.album.artistas) { artista -> FilaArtista(artista) }
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
        itemsIndexed(presentacion.album.canciones) { indice, cancion -> FilaCancion(indice + 1, cancion) }
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
        items(presentacion.album.comentarios) { comentario -> TarjetaComentario(comentario) }
    }
}

@Composable
private fun TarjetaMetadatos(presentacion: AlbumPresentacion) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                presentacion.album.genero,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                presentacion.album.disquera,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(presentacion.album.nombre, style = MaterialTheme.typography.headlineMedium)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                PromedioConEstrellas(presentacion.promedio)
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        stringResource(R.string.detalle_estreno),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(presentacion.fechaLanzamiento, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Text(
                presentacion.album.descripcion,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun PromedioConEstrellas(promedio: String?) {
    val descripcion = stringResource(R.string.detalle_promedio)
    Column(modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = descripcion }) {
        if (promedio == null) {
            Text(
                stringResource(R.string.detalle_sin_calificaciones),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(promedio, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                TextoEstrellas(estrellasLlenas(promedio))
            }
        }
    }
}

/** Dibuja de 0 a 5 estrellas llenas con texto (sin depender de iconos adicionales). */
@Composable
private fun TextoEstrellas(llenas: Int, modifier: Modifier = Modifier) {
    Text(
        text = "★".repeat(llenas) + "☆".repeat(5 - llenas),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
    )
}

@Composable
private fun FilaArtista(artista: com.example.vinilos.data.model.Artista) {
    val porDefecto = painterResource(R.drawable.inicio_artistas)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AsyncImage(
            model = artista.imagen?.takeIf { it.isNotBlank() },
            contentDescription = stringResource(R.string.detalle_artista_imagen, artista.nombre),
            placeholder = porDefecto,
            error = porDefecto,
            fallback = porDefecto,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(64.dp).clip(CircleShape)
        )
        Text(artista.nombre, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun FilaCancion(numero: Int, cancion: com.example.vinilos.data.model.Cancion) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "%02d".format(numero),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(28.dp)
            )
            Text(cancion.nombre, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
            Text(cancion.duracion, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TarjetaComentario(comentario: com.example.vinilos.data.model.Comentario) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(comentario.descripcion)
            comentario.calificacion?.let { calificacion ->
                val descripcionCalificacion = stringResource(R.string.detalle_calificacion, calificacion)
                TextoEstrellas(
                    llenas = calificacion.coerceIn(0, 5),
                    modifier = Modifier.semantics { contentDescription = descripcionCalificacion }
                )
            } ?: Text(
                stringResource(R.string.detalle_sin_calificaciones),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
