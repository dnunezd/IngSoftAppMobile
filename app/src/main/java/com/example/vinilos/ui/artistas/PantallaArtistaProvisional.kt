package com.example.vinilos.ui.artistas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.vinilos.R
import com.example.vinilos.data.model.ArtistType
import com.example.vinilos.ui.ingreso.EncabezadoVinilo
import com.example.vinilos.viewmodel.FavoritoUiState
import kotlinx.coroutines.delay

const val TagDetalleArtista = "detalle_artista"
const val TagNombreDetalleArtista = "detalle_artista_nombre"
const val TagTipoDetalleArtista = "detalle_artista_tipo"
const val TagBotonFavorito = "detalle_artista_boton_favorito"
const val TagReintentarFavorito = "detalle_artista_reintentar_favorito"
const val TagErrorFavorito = "detalle_artista_error_favorito"
const val TagConfirmacionFavorito = "detalle_artista_confirmacion_favorito"
const val TagNoDisponibleFavorito = "detalle_artista_no_disponible_favorito"

/** Milisegundos que permanece visible la confirmación de favorito antes de ocultarse sola. */
private const val DuracionConfirmacionFavoritoMs = 2500L

/** Destino provisional del detalle del artista: HU04 lo reemplaza por el detalle real. */
@Composable
fun PantallaArtistaProvisional(
    nombre: String,
    tipo: ArtistType,
    onAtras: () -> Unit,
    modifier: Modifier = Modifier,
    estadoFavorito: FavoritoUiState? = null,
    onAgregarFavorito: () -> Unit = {},
    onReintentarFavorito: () -> Unit = {},
    onConfirmacionFavoritoMostrada: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag(TagDetalleArtista)
    ) {
        EncabezadoVinilo(modifier = Modifier.padding(horizontal = 12.dp), onAtras = onAtras)
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(
                text = stringResource(R.string.artista_detalle_titulo),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = nombre,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.testTag(TagNombreDetalleArtista)
            )
            Text(
                text = stringResource(
                    if (tipo == ArtistType.BANDA) R.string.artistas_tipo_banda else R.string.artistas_tipo_musico
                ),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.testTag(TagTipoDetalleArtista)
            )
            if (estadoFavorito != null) {
                SeccionFavorito(
                    nombre = nombre,
                    estado = estadoFavorito,
                    onAgregar = onAgregarFavorito,
                    onReintentar = onReintentarFavorito,
                    onConfirmacionMostrada = onConfirmacionFavoritoMostrada,
                    onVolverAlListado = onAtras
                )
            }
        }
    }
}

/** Botón de favorito y sus mensajes asociados (HU10); oculto por completo para el Visitante. */
@Composable
private fun SeccionFavorito(
    nombre: String,
    estado: FavoritoUiState,
    onAgregar: () -> Unit,
    onReintentar: () -> Unit,
    onConfirmacionMostrada: () -> Unit,
    onVolverAlListado: () -> Unit
) {
    Column(
        modifier = Modifier.padding(top = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (estado == FavoritoUiState.NoDisponible) {
            Text(
                text = stringResource(R.string.favoritos_no_disponible),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag(TagNoDisponibleFavorito)
            )
            Button(onClick = onVolverAlListado) {
                Text(stringResource(R.string.favoritos_volver_artistas))
            }
            return@Column
        }

        val textoBoton = when (estado) {
            FavoritoUiState.Guardando -> stringResource(R.string.favoritos_guardando)
            is FavoritoUiState.Favorito -> stringResource(R.string.favoritos_favorito)
            else -> stringResource(R.string.favoritos_agregar)
        }
        val botonActivo = estado == FavoritoUiState.NoFavorito || estado == FavoritoUiState.ErrorGuardado
        Button(
            onClick = onAgregar,
            enabled = botonActivo,
            modifier = Modifier.testTag(TagBotonFavorito)
        ) {
            Text(textoBoton)
        }

        if (estado == FavoritoUiState.ErrorComprobacion) {
            Text(
                text = stringResource(R.string.favoritos_error_comprobacion),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag(TagErrorFavorito)
            )
            TextButton(onClick = onReintentar, modifier = Modifier.testTag(TagReintentarFavorito)) {
                Text(stringResource(R.string.favoritos_reintentar))
            }
        }

        if (estado == FavoritoUiState.ErrorGuardado) {
            Text(
                text = stringResource(R.string.favoritos_error_guardado),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag(TagErrorFavorito)
            )
        }

        if (estado is FavoritoUiState.Favorito && estado.mostrarConfirmacion) {
            Text(
                text = stringResource(R.string.favoritos_confirmacion, nombre),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag(TagConfirmacionFavorito)
            )
            LaunchedEffect(Unit) {
                delay(DuracionConfirmacionFavoritoMs)
                onConfirmacionMostrada()
            }
        }
    }
}
