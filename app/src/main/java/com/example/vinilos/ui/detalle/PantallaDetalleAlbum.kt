package com.example.vinilos.ui.detalle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.vinilos.R
import com.example.vinilos.data.model.Session
import com.example.vinilos.ui.ingreso.AvisoUnaVez
import com.example.vinilos.ui.ingreso.EncabezadoVinilo
import com.example.vinilos.viewmodel.AlbumUiState

const val TagActualizandoDetalle = "detalle_actualizando"

/** Verde del ícono de confirmación en el aviso "Canción agregada" (no forma parte de la paleta de marca). */
private val VerdeConfirmacion = Color(0xFF2ECC71)

@Composable
fun PantallaDetalleAlbum(
    estado: AlbumUiState,
    onAtras: () -> Unit,
    onReintentar: () -> Unit,
    modifier: Modifier = Modifier,
    onAvisoMostrado: () -> Unit = {},
    session: Session = Session.Visitor,
    onAgregarCancion: () -> Unit = {},
    mostrarAvisoCancionAgregada: Boolean = false,
    onAvisoCancionAgregadaMostrado: () -> Unit = {}
) {
    if (estado is AlbumUiState.Exito) {
        val presentacion = remember(estado.album) { presentarAlbum(estado.album) }
        val avisos = remember { SnackbarHostState() }
        AvisoUnaVez(
            mostrar = estado.errorAlActualizar,
            mensaje = stringResource(R.string.detalle_error_actualizar),
            avisos = avisos,
            onMostrado = onAvisoMostrado
        )
        val mensajeCancionAgregada = stringResource(R.string.agregar_cancion_exito)
        val detalleCancionAgregada = stringResource(R.string.agregar_cancion_exito_detalle, estado.album.nombre)
        LaunchedEffect(mostrarAvisoCancionAgregada) {
            if (mostrarAvisoCancionAgregada) {
                // El subtítulo ("Se asoció a <álbum>") viaja en actionLabel: no se usa como acción,
                // solo distingue este aviso del resto para pintarlo con el estilo de éxito.
                avisos.showSnackbar(
                    message = mensajeCancionAgregada,
                    actionLabel = detalleCancionAgregada,
                    withDismissAction = true,
                    duration = SnackbarDuration.Short
                )
                onAvisoCancionAgregadaMostrado()
            }
        }
        Box(modifier.fillMaxSize()) {
            ContenidoAlbum(
                presentacion,
                onAtras,
                actualizando = estado.actualizando,
                session = session,
                onAgregarCancion = onAgregarCancion
            )
            SnackbarHost(
                avisos,
                Modifier.align(Alignment.BottomCenter).windowInsetsPadding(WindowInsets.safeDrawing)
            ) { datos ->
                val subtitulo = datos.visuals.actionLabel
                if (subtitulo != null) {
                    AvisoExito(titulo = datos.visuals.message, subtitulo = subtitulo, onCerrar = datos::dismiss)
                } else {
                    Snackbar(snackbarData = datos)
                }
            }
        }
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

/** Aviso de éxito al estilo del mockup: píldora oscura, ícono de confirmación, título y subtítulo. */
@Composable
private fun AvisoExito(titulo: String, subtitulo: String, onCerrar: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(16.dp)
            .widthIn(max = 360.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.inverseSurface)
            .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(VerdeConfirmacion),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.inverseOnSurface
            )
            Text(
                text = subtitulo,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.7f)
            )
        }
        IconButton(onClick = onCerrar, modifier = Modifier.size(28.dp)) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.agregar_cancion_cerrar_aviso),
                tint = MaterialTheme.colorScheme.inverseOnSurface
            )
        }
    }
}
