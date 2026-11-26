package com.example.vinilos.ui.create

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.example.vinilos.R
import com.example.vinilos.data.model.Session
import com.example.vinilos.ui.catalogo.HeaderWithRole
import com.example.vinilos.ui.detalle.AlbumCover
import com.example.vinilos.viewmodel.AddTrackUiState
import com.example.vinilos.viewmodel.TrackField
import com.example.vinilos.viewmodel.TrackSubmission
import com.example.vinilos.viewmodel.TrackError
import com.example.vinilos.viewmodel.TargetAlbumInfo
import com.example.vinilos.ui.ingreso.encabezado
import com.example.vinilos.ui.theme.coloresBotonPrincipal
import com.example.vinilos.ui.idiomaDeLaApp

const val TagFormularioAddTrack = "agregar_cancion_formulario"
const val TagNombreTrack = "agregar_cancion_nombre"
const val TagDuracionTrack = "agregar_cancion_duracion"
const val TagGuardarTrack = "agregar_cancion_guardar"
const val TagCancelarAddTrack = "agregar_cancion_cancelar"

data class AddTrackActions(
    val onNombre: (String) -> Unit = {},
    val onDuracion: (String) -> Unit = {},
    val onGuardar: () -> Unit = {},
    val onCancelar: () -> Unit = {},
    val onAtras: () -> Unit = {},
    /** The album no longer exists in the backend: unlike [onAtras], it notifies the detail screen. */
    val onAlbumNotAvailable: () -> Unit = {}
)

@Composable
fun AddTrackScreen(
    estado: AddTrackUiState,
    session: Session,
    acciones: AddTrackActions,
    modifier: Modifier = Modifier
) {
    if (estado.envio == TrackSubmission.AlbumNotAvailable) {
        BackHandler { acciones.onAlbumNotAvailable() }
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            HeaderWithRole(
                session = session,
                titulo = stringResource(R.string.agregar_cancion_titulo),
                onAtras = acciones.onAlbumNotAvailable,
                atrasHabilitado = true
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    stringResource(R.string.agregar_cancion_album_no_disponible),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.error
                )
                Button(onClick = acciones.onAlbumNotAvailable, modifier = Modifier.padding(top = 16.dp)) {
                    Text(stringResource(R.string.agregar_cancion_volver))
                }
            }
        }
        return
    }

    BackHandler(enabled = estado.guardando) {}
    val camposBloqueados = estado.guardando
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag(TagFormularioAddTrack),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HeaderWithRole(
            session = session,
            titulo = stringResource(R.string.agregar_cancion_titulo),
            onAtras = acciones.onCancelar,
            atrasHabilitado = !camposBloqueados
        )
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (estado.albumInfo != null) {
                TarjetaAlbumDestino(estado.albumInfo)
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    stringResource(R.string.agregar_cancion_datos_cancion),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.encabezado()
                )
                CampoTextoTrack(
                    valor = estado.nombre,
                    etiqueta = stringResource(R.string.agregar_cancion_nombre),
                    tag = TagNombreTrack,
                    error = estado.errores[TrackField.Nombre],
                    habilitado = !camposBloqueados,
                    onValor = acciones.onNombre
                )
                CampoTextoTrack(
                    valor = estado.duracion,
                    etiqueta = stringResource(R.string.agregar_cancion_duracion),
                    tag = TagDuracionTrack,
                    error = estado.errores[TrackField.Duracion],
                    habilitado = !camposBloqueados,
                    onValor = acciones.onDuracion
                )
            }
            if (estado.envio == TrackSubmission.Fallida) {
                TarjetaErrorEnvio(stringResource(R.string.agregar_cancion_error_envio))
            }
            Button(
                onClick = acciones.onGuardar,
                enabled = estado.puedeGuardar,
                colors = coloresBotonPrincipal(enviando = estado.guardando),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag(TagGuardarTrack)
            ) {
                Text(
                    stringResource(
                        if (estado.guardando) R.string.agregar_cancion_guardando
                        else R.string.agregar_cancion_guardar
                    ),
                    style = MaterialTheme.typography.labelLarge
                )
            }
            Button(
                onClick = acciones.onCancelar,
                enabled = !camposBloqueados,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag(TagCancelarAddTrack)
            ) {
                Text(stringResource(R.string.agregar_cancion_cancelar))
            }
        }
    }

}

/** Context card for "target album" (best-effort cover art, see [InfoAlbumDestino]). */
@Composable
private fun TarjetaAlbumDestino(info: TargetAlbumInfo) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AlbumCover(
            url = info.portada,
            contentDescription = stringResource(R.string.detalle_portada_con_nombre, info.nombre),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(R.string.agregar_cancion_album_destino).uppercase(idiomaDeLaApp()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = info.nombre,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!info.artista.isNullOrBlank()) {
                Text(
                    text = info.artista,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Same error banner style as the create-album form, so both look consistent. */
@Composable
private fun TarjetaErrorEnvio(mensaje: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error
        )
        Text(
            text = mensaje,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

@Composable
private fun CampoTextoTrack(
    valor: String,
    etiqueta: String,
    tag: String,
    error: TrackError?,
    habilitado: Boolean,
    onValor: (String) -> Unit
) {
    val textoError = when (error) {
        TrackError.Obligatorio -> stringResource(R.string.agregar_cancion_campo_obligatorio)
        TrackError.DuracionInvalida -> stringResource(R.string.agregar_cancion_error_formato)
        null -> null
    }
    OutlinedTextField(
        value = valor,
        onValueChange = onValor,
        label = { Text(etiqueta) },
        singleLine = true,
        enabled = habilitado,
        isError = textoError != null,
        supportingText = textoError?.let { mensaje -> { Text(mensaje) } },
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Next
        ),
        modifier = Modifier.fillMaxWidth().testTag(tag)
    )
}
