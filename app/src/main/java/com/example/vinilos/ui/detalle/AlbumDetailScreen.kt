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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
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
import com.example.vinilos.ui.idiomaDeLaApp

const val TagActualizandoDetalle = "detalle_actualizando"
const val TagAddComment = "detalle_comentar"

/** Green of the confirmation icon in the "Track added" snackbar (not part of the brand palette). */
private val VerdeConfirmacion = Color(0xFF2ECC71)

/**
 * Custom visuals for the "Track added" snackbar, so we don't depend on [SnackbarVisuals.actionLabel]
 * (meant for a real action, not for carrying a subtitle) when deciding in [SnackbarHost]
 * whether to draw it with the success style.
 */
private data class AvisoTrackAgregadaVisuals(
    val mensaje: String,
    val subtitulo: String
) : SnackbarVisuals {
    override val message: String get() = mensaje
    override val actionLabel: String? = null
    override val withDismissAction: Boolean = true
    override val duration: SnackbarDuration = SnackbarDuration.Short
}

@Composable
fun AlbumDetailScreen(
    estado: AlbumUiState,
    onAtras: () -> Unit,
    onReintentar: () -> Unit,
    modifier: Modifier = Modifier,
    onAvisoMostrado: () -> Unit = {},
    session: Session = Session.Visitor,
    onAddTrack: () -> Unit = {},
    mostrarAvisoTrackAgregada: Boolean = false,
    onAvisoTrackAgregadaMostrado: () -> Unit = {},
    /** Only the Collector can comment (HU09); without this action the button isn't drawn. */
    onComentar: (() -> Unit)? = null
) {
    if (estado is AlbumUiState.Success) {
        val idioma = idiomaDeLaApp()
        val presentacion = remember(estado.album, idioma) { presentarAlbum(estado.album, idioma) }
        val avisos = remember { SnackbarHostState() }
        AvisoUnaVez(
            mostrar = estado.errorAlActualizar,
            mensaje = stringResource(R.string.detalle_error_actualizar),
            avisos = avisos,
            onMostrado = onAvisoMostrado
        )
        val mensajeTrackAgregada = stringResource(R.string.agregar_cancion_exito)
        val detalleTrackAgregada = stringResource(R.string.agregar_cancion_exito_detalle, estado.album.nombre)
        LaunchedEffect(mostrarAvisoTrackAgregada) {
            if (mostrarAvisoTrackAgregada) {
                try {
                    avisos.showSnackbar(
                        AvisoTrackAgregadaVisuals(mensaje = mensajeTrackAgregada, subtitulo = detalleTrackAgregada)
                    )
                } finally {
                    // Same as AvisoUnaVez: if the screen exits before finishing, it stays marked as shown
                    // and won't appear again on return (nor reset on rotation).
                    onAvisoTrackAgregadaMostrado()
                }
            }
        }
        Box(modifier.fillMaxSize()) {
            AlbumContent(
                presentacion,
                onAtras,
                actualizando = estado.actualizando,
                session = session,
                onAddTrack = onAddTrack,
                margenInferior = if (onComentar != null) 72.dp else 0.dp
            )
            if (onComentar != null) {
                ExtendedFloatingActionButton(
                    onClick = onComentar,
                    // Material hides the text from accessibility, so the icon provides the name (as in HU07).
                    icon = { Icon(Icons.Filled.Edit, contentDescription = stringResource(R.string.detalle_comentar)) },
                    text = { Text(stringResource(R.string.detalle_comentar)) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .padding(20.dp)
                        .testTag(TagAddComment)
                )
            }
            SnackbarHost(
                avisos,
                Modifier.align(Alignment.BottomCenter).windowInsetsPadding(WindowInsets.safeDrawing)
            ) { datos ->
                val visuals = datos.visuals
                if (visuals is AvisoTrackAgregadaVisuals) {
                    AvisoSuccess(titulo = visuals.message, subtitulo = visuals.subtitulo, onCerrar = datos::dismiss)
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
        if (estado == AlbumUiState.Loading) {
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
        if (estado == AlbumUiState.NotAvailable || estado == AlbumUiState.Error) {
            Column(
                Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
            ) {
                Text(
                    stringResource(
                        if (estado == AlbumUiState.NotAvailable) R.string.detalle_no_disponible
                        else R.string.detalle_error
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
                if (estado == AlbumUiState.NotAvailable) {
                    Button(onClick = onAtras) { Text(stringResource(R.string.detalle_volver_catalogo)) }
                } else {
                    Button(onClick = onReintentar) { Text(stringResource(R.string.detalle_reintentar)) }
                }
            }
        }
    }
}

/** Success snackbar styled after the mockup: dark pill, confirmation icon, title, and subtitle. */
@Composable
private fun AvisoSuccess(titulo: String, subtitulo: String, onCerrar: () -> Unit) {
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
