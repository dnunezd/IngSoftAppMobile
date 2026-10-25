package com.example.vinilos.ui.detalle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

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
        presentacion.mensajeCanciones?.let { mensaje ->
            item { Text(stringResource(mensaje), style = MaterialTheme.typography.bodyLarge) }
        }
    }
}
