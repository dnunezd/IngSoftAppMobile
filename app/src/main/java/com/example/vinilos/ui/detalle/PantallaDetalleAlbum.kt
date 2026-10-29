package com.example.vinilos.ui.detalle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.vinilos.R
import com.example.vinilos.ui.ingreso.EncabezadoVinilo
import com.example.vinilos.viewmodel.AlbumUiState

@Composable
fun PantallaDetalleAlbum(
    estado: AlbumUiState,
    onAtras: () -> Unit,
    onReintentar: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (estado is AlbumUiState.Exito) {
        val presentacion = remember(estado.album) { presentarAlbum(estado.album) }
        ContenidoAlbum(presentacion, onAtras, modifier)
        return
    }
    Column(
        modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        EncabezadoVinilo(onAtras = onAtras)
        if (estado == AlbumUiState.Cargando) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                val descripcion = stringResource(R.string.detalle_cargando)
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(56.dp)
                        .semantics { contentDescription = descripcion },
                    strokeWidth = 5.dp
                )
            }
        }
        if (estado == AlbumUiState.NoDisponible || estado == AlbumUiState.Error) {
            Column(
                Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
            ) {
                Text(
                    stringResource(
                        if (estado == AlbumUiState.NoDisponible) R.string.detalle_no_disponible
                        else R.string.detalle_error
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
                if (estado == AlbumUiState.NoDisponible) {
                    Button(onClick = onAtras) { Text(stringResource(R.string.detalle_volver_catalogo)) }
                } else {
                    Button(onClick = onReintentar) { Text(stringResource(R.string.detalle_reintentar)) }
                }
            }
        }
    }
}
