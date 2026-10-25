package com.example.vinilos.ui.detalle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
    Column(
        modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        EncabezadoVinilo(onAtras = onAtras)
        if (estado == AlbumUiState.Cargando) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                val descripcion = stringResource(R.string.detalle_cargando)
                CircularProgressIndicator(Modifier.semantics { contentDescription = descripcion })
            }
        }
    }
}
