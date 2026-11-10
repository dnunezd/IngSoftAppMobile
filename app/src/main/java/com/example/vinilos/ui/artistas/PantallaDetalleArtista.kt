package com.example.vinilos.ui.artistas

import androidx.annotation.DrawableRes
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.vinilos.R
import com.example.vinilos.data.model.AlbumArtista
import com.example.vinilos.data.model.ArtistType
import com.example.vinilos.data.model.ArtistaDetalle
import com.example.vinilos.data.model.IntegranteArtista
import com.example.vinilos.data.model.PremioArtista
import com.example.vinilos.ui.theme.VinilosTheme
import com.example.vinilos.viewmodel.ArtistaDetalleUiState
import com.example.vinilos.viewmodel.FavoritoUiState
import kotlinx.coroutines.delay

const val TagDetalleArtista = "detalle_artista"
const val TagListaDetalleArtista = "detalle_artista_lista"
const val TagProgresoDetalleArtista = "detalle_artista_progreso"
const val TagNombreDetalleArtista = "detalle_artista_nombre"
const val TagTipoDetalleArtista = "detalle_artista_tipo"
const val TagFechaDetalleArtista = "detalle_artista_fecha"
const val TagDescripcionDetalleArtista = "detalle_artista_descripcion"
const val TagContadorIntegrantes = "detalle_artista_contador_integrantes"
const val TagContadorAlbumes = "detalle_artista_contador_albumes"
const val TagContadorPremios = "detalle_artista_contador_premios"
const val TagBotonFavorito = "detalle_artista_boton_favorito"
const val TagReintentarFavorito = "detalle_artista_reintentar_favorito"
const val TagErrorFavorito = "detalle_artista_error_favorito"
const val TagConfirmacionFavorito = "detalle_artista_confirmacion_favorito"
const val TagNoDisponibleFavorito = "detalle_artista_no_disponible_favorito"

/** Milisegundos que permanece visible la confirmación de favorito antes de ocultarse sola. */
private const val DuracionConfirmacionFavoritoMs = 2500L

fun tagAlbumArtista(id: Int) = "detalle_artista_album_$id"
fun tagRespaldoAlbum(id: Int) = "detalle_artista_album_respaldo_$id"
fun tagIntegrante(id: Int) = "detalle_artista_integrante_$id"

private val EsquinaTarjeta = RoundedCornerShape(24.dp)
private val EsquinaFila = RoundedCornerShape(20.dp)
private val EsquinaMiniatura = RoundedCornerShape(12.dp)

/**
 * Detalle de un músico o una banda con sus integrantes, álbumes y premios (HU04). [estadoFavorito]
 * nulo oculta por completo la sección de favorito (HU10): así se mantiene oculta para el Visitante
 * sin que esta pantalla necesite conocer la sesión.
 */
@Composable
fun PantallaDetalleArtista(
    estado: ArtistaDetalleUiState,
    onSeleccionarAlbum: (Int) -> Unit,
    onReintentar: () -> Unit,
    onAtras: () -> Unit,
    modifier: Modifier = Modifier,
    estadoFavorito: FavoritoUiState? = null,
    onAgregarFavorito: () -> Unit = {},
    onReintentarFavorito: () -> Unit = {},
    onConfirmacionFavoritoMostrada: () -> Unit = {},
    // Volver al listado tras "artista ya no disponible": por defecto es igual a onAtras, pero
    // VinilosNavHost pasa uno propio para marcar el listado y forzar una nueva consulta (sin
    // eso, popBackStack por sí solo no recarga el listado; ver bug corregido en HU10).
    onVolverAlListado: () -> Unit = onAtras
) {
    // Se maneja aquí arriba (fuera del LazyColumn) y no dentro de la tarjeta: esa tarjeta se
    // compone en un "item" sin key explícita, y un LaunchedEffect allí puede reiniciarse si el
    // item se recompone/recicla, lo que impedía que la espera de 2.5s llegara a completarse.
    if (estadoFavorito is FavoritoUiState.Favorito && estadoFavorito.mostrarConfirmacion) {
        LaunchedEffect(estadoFavorito) {
            delay(DuracionConfirmacionFavoritoMs)
            onConfirmacionFavoritoMostrada()
        }
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag(TagDetalleArtista)
    ) {
        EncabezadoArtistas(stringResource(R.string.artista_titulo), onAtras)
        // Una sola columna perezosa: un artista puede tener muchos álbumes y premios.
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag(TagListaDetalleArtista),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (estado) {
                ArtistaDetalleUiState.Cargando -> {
                    item { Cargando() }
                    item { TarjetaEsqueleto() }
                    items(FILAS_DE_RELLENO) { FilaEsqueleto() }
                }
                ArtistaDetalleUiState.Error -> item {
                    ErrorArtistas(stringResource(R.string.artista_error), onReintentar)
                }
                is ArtistaDetalleUiState.Exito -> contenido(
                    detalle = estado.detalle,
                    onSeleccionarAlbum = onSeleccionarAlbum,
                    estadoFavorito = estadoFavorito,
                    onAgregarFavorito = onAgregarFavorito,
                    onReintentarFavorito = onReintentarFavorito,
                    onVolverAlListado = onVolverAlListado
                )
            }
        }
    }
}

private fun LazyListScope.contenido(
    detalle: ArtistaDetalle,
    onSeleccionarAlbum: (Int) -> Unit,
    estadoFavorito: FavoritoUiState?,
    onAgregarFavorito: () -> Unit,
    onReintentarFavorito: () -> Unit,
    onVolverAlListado: () -> Unit
) {
    item(key = "tarjeta_principal") {
        TarjetaPrincipal(
            detalle = detalle,
            estadoFavorito = estadoFavorito,
            onAgregarFavorito = onAgregarFavorito,
            onReintentarFavorito = onReintentarFavorito,
            onVolverAlListado = onVolverAlListado
        )
    }
    if (detalle.tipo == ArtistType.BANDA) {
        item {
            TituloSeccion(
                titulo = R.string.artista_integrantes,
                contador = R.plurals.artista_integrantes_contador,
                cantidad = detalle.integrantes.size,
                tagContador = TagContadorIntegrantes
            )
        }
        item {
            if (detalle.integrantes.isEmpty()) {
                BloqueVacio(R.string.artista_sin_integrantes, rememberVectorPainter(Icons.Outlined.Person))
            } else {
                FilaIntegrantes(detalle.integrantes)
            }
        }
    }
    item {
        TituloSeccion(
            titulo = R.string.artista_albumes,
            contador = R.plurals.artista_albumes_contador,
            cantidad = detalle.albumes.size,
            tagContador = TagContadorAlbumes
        )
    }
    if (detalle.albumes.isEmpty()) {
        item { BloqueVacio(R.string.artista_sin_albumes, painterResource(R.drawable.ic_cat_album)) }
    } else {
        items(detalle.albumes, key = { "album_${it.id}" }) { FilaAlbum(it, onSeleccionarAlbum) }
    }
    item {
        TituloSeccion(
            titulo = R.string.artista_premios,
            contador = R.plurals.artista_premios_contador,
            cantidad = detalle.premios.size,
            tagContador = TagContadorPremios
        )
    }
    if (detalle.premios.isEmpty()) {
        item { BloqueVacio(R.string.artista_sin_premios, painterResource(R.drawable.ic_det_trofeo)) }
    } else {
        items(detalle.premios, key = { "premio_${it.id}" }) { FilaPremio(it) }
    }
}

@Composable
private fun TarjetaPrincipal(
    detalle: ArtistaDetalle,
    estadoFavorito: FavoritoUiState?,
    onAgregarFavorito: () -> Unit,
    onReintentarFavorito: () -> Unit,
    onVolverAlListado: () -> Unit
) {
    val porDefecto = painterResource(porDefectoDe(detalle.tipo))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(EsquinaTarjeta)
            .background(MaterialTheme.colorScheme.surface)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Si la URL no responde, no es una imagen o no existe, se muestra la imagen por defecto.
        AsyncImage(
            model = detalle.imagen?.takeIf { it.isNotBlank() },
            contentDescription = stringResource(R.string.artistas_imagen_descripcion, detalle.nombre),
            placeholder = porDefecto,
            error = porDefecto,
            fallback = porDefecto,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(112.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Text(
            text = detalle.nombre,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag(TagNombreDetalleArtista)
        )
        EtiquetaTipo(detalle.tipo, Modifier.testTag(TagTipoDetalleArtista))
        detalle.fecha?.let { BloqueFecha(detalle.tipo, it) }
        detalle.descripcion?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TagDescripcionDetalleArtista)
            )
        }
        if (estadoFavorito != null) {
            SeccionFavorito(
                nombre = detalle.nombre,
                estado = estadoFavorito,
                onAgregar = onAgregarFavorito,
                onReintentar = onReintentarFavorito,
                onVolverAlListado = onVolverAlListado
            )
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
    onVolverAlListado: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        when (estado) {
            FavoritoUiState.NoDisponible -> TarjetaFavoritoNoDisponible(onVolverAlListado)
            // El guardado reintenta con la misma acción de agregar (onAgregar); la tarjeta de
            // error reemplaza al botón principal en vez de mostrarse junto a él.
            FavoritoUiState.ErrorGuardado -> TarjetaErrorGuardado(
                onReintentar = onAgregar,
                modifier = Modifier.testTag(TagErrorFavorito)
            )
            else -> {
                val esFavorito = estado is FavoritoUiState.Favorito
                val botonActivo = estado == FavoritoUiState.NoFavorito
                val textoBoton = when (estado) {
                    FavoritoUiState.Guardando -> stringResource(R.string.favoritos_guardando)
                    is FavoritoUiState.Favorito -> stringResource(R.string.favoritos_favorito)
                    else -> stringResource(R.string.favoritos_agregar)
                }
                Button(
                    onClick = onAgregar,
                    enabled = botonActivo,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(TagBotonFavorito)
                ) {
                    Icon(
                        imageVector = if (esFavorito) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = null,
                        tint = if (esFavorito) MaterialTheme.colorScheme.error else LocalContentColor.current
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(textoBoton)
                }

                if (esFavorito) {
                    NotaBloqueoTemporalFavorito()
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

                if (estado is FavoritoUiState.Favorito && estado.mostrarConfirmacion) {
                    Text(
                        text = stringResource(R.string.favoritos_confirmacion, nombre),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag(TagConfirmacionFavorito)
                    )
                }
            }
        }
    }
}

/** Nota bajo el botón ya marcado como favorito: confirma que ya es favorito (HU10). */
@Composable
private fun NotaBloqueoTemporalFavorito() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = stringResource(R.string.favoritos_bloqueado_nota),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Tarjeta de error al guardar el favorito (HU10), con el mismo lenguaje visual que [TarjetaError]
 * en la creación de álbum (HU07): fondo errorContainer, ícono de advertencia y botón de reintento.
 */
@Composable
private fun TarjetaErrorGuardado(onReintentar: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                text = stringResource(R.string.favoritos_error_guardado),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
        Button(
            onClick = onReintentar,
            shape = RoundedCornerShape(50),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(imageVector = Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.favoritos_reintentar_guardado))
        }
    }
}

/** Tarjeta cuando el artista ya no existe en el backend (HU10): mensaje y regreso al listado. */
@Composable
private fun TarjetaFavoritoNoDisponible(onVolverAlListado: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error
        )
        Text(
            text = stringResource(R.string.favoritos_no_disponible),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag(TagNoDisponibleFavorito)
        )
        Text(
            text = stringResource(R.string.favoritos_no_disponible_descripcion),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Button(
            onClick = onVolverAlListado,
            shape = RoundedCornerShape(50),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.favoritos_volver_artistas))
        }
    }
}

@Composable
private fun BloqueFecha(tipo: ArtistType, fecha: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(Icons.Outlined.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column {
            Text(
                text = stringResource(
                    if (tipo == ArtistType.BANDA) R.string.artista_fecha_creacion else R.string.artista_fecha_nacimiento
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = fecha,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag(TagFechaDetalleArtista)
            )
        }
    }
}

/** Título de la sección, con el contador a la derecha solo cuando hay elementos. */
@Composable
private fun TituloSeccion(@StringRes titulo: Int, @PluralsRes contador: Int, cantidad: Int, tagContador: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(titulo),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        if (cantidad > 0) {
            Text(
                text = pluralStringResource(contador, cantidad, cantidad),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag(tagContador)
            )
        }
    }
}

/** Sección sin elementos: bloque gris con un ícono y el mensaje, como en el marco de Figma. */
@Composable
private fun BloqueVacio(@StringRes mensaje: Int, icono: Painter) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(EsquinaFila)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    ) {
        Icon(
            painter = icono,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = stringResource(mensaje),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Los integrantes no son seleccionables: ningún criterio lo pide y el mockup no muestra indicador. */
@Composable
private fun FilaIntegrantes(integrantes: List<IntegranteArtista>) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(integrantes, key = { it.id }) { Integrante(it) }
    }
}

@Composable
private fun Integrante(integrante: IntegranteArtista) {
    val porDefecto = painterResource(R.drawable.ic_artista_por_defecto)
    Column(
        modifier = Modifier
            .width(84.dp)
            .testTag(tagIntegrante(integrante.id)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AsyncImage(
            model = integrante.imagen?.takeIf { it.isNotBlank() },
            contentDescription = stringResource(R.string.artistas_imagen_descripcion, integrante.nombre),
            placeholder = porDefecto,
            error = porDefecto,
            fallback = porDefecto,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Text(
            text = integrante.nombre,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun FilaAlbum(album: AlbumArtista, onSeleccionar: (Int) -> Unit) {
    val descripcion = stringResource(R.string.artistas_seleccionar_descripcion, album.nombre)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(EsquinaFila)
            .testTag(tagAlbumArtista(album.id))
            .semantics { contentDescription = descripcion }
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onSeleccionar(album.id) }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // El ícono de disco queda debajo: se ve mientras carga y si la portada falta o no carga.
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(EsquinaMiniatura)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_cat_album),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag(tagRespaldoAlbum(album.id))
            )
            AsyncImage(
                model = album.portada?.takeIf { it.isNotBlank() },
                contentDescription = stringResource(R.string.artistas_imagen_descripcion, album.nombre),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = album.nombre,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            album.anio?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FilaPremio(premio: PremioArtista) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(EsquinaFila)
            .background(MaterialTheme.colorScheme.surface)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_det_trofeo),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = premio.nombre,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = premio.organizacion,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            premio.fecha?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun Cargando() {
    val descripcion = stringResource(R.string.artista_cargando)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .semantics(mergeDescendants = true) { contentDescription = descripcion }
            .testTag(TagProgresoDetalleArtista),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
    ) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
        Text(
            text = descripcion,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Tarjeta de relleno mientras carga, con la misma forma que la tarjeta principal. */
@Composable
private fun TarjetaEsqueleto() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(EsquinaTarjeta)
            .background(MaterialTheme.colorScheme.surface)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Relleno(Modifier.size(112.dp), CircleShape)
        Relleno(Modifier.width(180.dp).height(24.dp), CircleShape)
        Relleno(Modifier.width(70.dp).height(22.dp), CircleShape)
        Relleno(Modifier.fillMaxWidth().height(52.dp), RoundedCornerShape(16.dp))
    }
}

/** Fila de relleno mientras carga, con la misma forma que una fila de álbum. */
@Composable
private fun FilaEsqueleto() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(EsquinaFila)
            .background(MaterialTheme.colorScheme.surface)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Relleno(Modifier.size(56.dp), EsquinaMiniatura)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Relleno(Modifier.width(150.dp).height(14.dp), CircleShape)
            Relleno(Modifier.width(60.dp).height(12.dp), CircleShape)
        }
    }
}

@Composable
private fun Relleno(modifier: Modifier, forma: Shape) {
    Box(modifier.clip(forma).background(MaterialTheme.colorScheme.surfaceVariant))
}

@DrawableRes
private fun porDefectoDe(tipo: ArtistType): Int =
    if (tipo == ArtistType.BANDA) R.drawable.ic_banda_por_defecto else R.drawable.ic_artista_por_defecto

private const val FILAS_DE_RELLENO = 3

private val BandaDeEjemplo = ArtistaDetalle(
    id = 101,
    nombre = "Queen",
    imagen = null,
    descripcion = "Banda británica de rock formada en Londres en 1970.",
    tipo = ArtistType.BANDA,
    fecha = "1 de enero de 1970",
    integrantes = listOf(IntegranteArtista(1, "Brian May", null), IntegranteArtista(2, "Roger Taylor", null)),
    albumes = listOf(
        AlbumArtista(103, "A Day at the Races", null, "1976"),
        AlbumArtista(102, "A Night at the Opera", null, "1975")
    ),
    premios = listOf(PremioArtista(1, "Grammy Award", "NARAS", "10 de diciembre de 1978"))
)

@Preview(showBackground = true, heightDp = 1100, name = "Banda con datos")
@Composable
private fun PreviewBanda() = VinilosTheme {
    PantallaDetalleArtista(ArtistaDetalleUiState.Exito(BandaDeEjemplo), {}, {}, {})
}

@Preview(showBackground = true, heightDp = 800, name = "Músico sin álbumes ni premios")
@Composable
private fun PreviewSinDatos() = VinilosTheme {
    val musico = BandaDeEjemplo.copy(
        nombre = "Rubén Blades",
        tipo = ArtistType.MUSICO,
        fecha = "16 de julio de 1948",
        integrantes = emptyList(),
        albumes = emptyList(),
        premios = emptyList()
    )
    PantallaDetalleArtista(ArtistaDetalleUiState.Exito(musico), {}, {}, {})
}

@Preview(showBackground = true, heightDp = 800, name = "Banda sin integrantes")
@Composable
private fun PreviewSinIntegrantes() = VinilosTheme {
    PantallaDetalleArtista(ArtistaDetalleUiState.Exito(BandaDeEjemplo.copy(integrantes = emptyList())), {}, {}, {})
}

@Preview(showBackground = true, heightDp = 800, name = "Cargando")
@Composable
private fun PreviewCargando() = VinilosTheme {
    PantallaDetalleArtista(ArtistaDetalleUiState.Cargando, {}, {}, {})
}

@Preview(showBackground = true, heightDp = 800, name = "Error")
@Composable
private fun PreviewError() = VinilosTheme {
    PantallaDetalleArtista(ArtistaDetalleUiState.Error, {}, {}, {})
}
