package com.example.vinilos.ui.catalogo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import androidx.compose.ui.unit.sp
import com.example.vinilos.R
import com.example.vinilos.data.model.AlbumResumen
import com.example.vinilos.data.model.Session
import com.example.vinilos.ui.detalle.PortadaAlbum
import com.example.vinilos.ui.ingreso.ChipEtiqueta
import com.example.vinilos.ui.ingreso.TagAtras
import com.example.vinilos.ui.theme.VinilosTheme
import com.example.vinilos.viewmodel.AlbumsUiState

/** `catalogo` y `catalogo_rol` son los tags que ya usaban las pruebas de ingreso y rotación (HU00). */
const val TagCatalogo = "catalogo"
const val TagRolActivo = "catalogo_rol"
const val TagCuadriculaCatalogo = "catalogo_cuadricula"
const val TagProgresoCatalogo = "catalogo_progreso"
const val TagActualizandoCatalogo = "catalogo_actualizando"
const val TagContadorCatalogo = "catalogo_contador"
const val TagVacioCatalogo = "catalogo_vacio"
const val TagErrorCatalogo = "catalogo_error"
const val TagReintentarCatalogo = "catalogo_reintentar"

fun tagAlbum(id: Int) = "album_$id"

/**
 * Catálogo de álbumes (HU01): cuadrícula con portada, nombre y género, deslizar hacia abajo para
 * actualizar y estados de carga, vacío y error. Es igual para el Visitante y el Coleccionista; solo
 * cambia la etiqueta del rol.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaCatalogo(
    estado: AlbumsUiState,
    session: Session,
    onActualizar: () -> Unit,
    onReintentar: () -> Unit,
    onAvisoMostrado: () -> Unit,
    onSeleccionar: (Int) -> Unit,
    onAtras: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cargado = estado is AlbumsUiState.Exito || estado is AlbumsUiState.Vacio
    val deslizar = rememberPullToRefreshState(enabled = { cargado })
    if (deslizar.isRefreshing) {
        LaunchedEffect(Unit) {
            onActualizar()
            // El progreso de la actualización se muestra junto al encabezado de la lista.
            deslizar.endRefresh()
        }
    }

    val avisos = remember { SnackbarHostState() }
    val mensajeError = stringResource(R.string.catalogo_error_actualizar)
    val conAviso = (estado as? AlbumsUiState.Exito)?.errorAlActualizar == true ||
        (estado as? AlbumsUiState.Vacio)?.errorAlActualizar == true
    LaunchedEffect(conAviso) {
        if (conAviso) {
            avisos.showSnackbar(mensajeError)
            onAvisoMostrado()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag(TagCatalogo)
    ) {
        Encabezado(session, onAtras)
        // clipToBounds: el indicador de deslizar espera oculto sobre la lista y no debe verse
        // encima del encabezado.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .nestedScroll(deslizar.nestedScrollConnection)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 156.dp),
                state = rememberLazyGridState(),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(TagCuadriculaCatalogo),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                anchoCompleto("titular") { Titular() }
                when (estado) {
                    AlbumsUiState.Cargando -> {
                        anchoCompleto("cargando") { Cargando() }
                        items(4) { TarjetaEsqueleto() }
                    }
                    AlbumsUiState.Error -> anchoCompleto("error") { Error(onReintentar) }
                    is AlbumsUiState.Vacio -> {
                        anchoCompleto("encabezado") { EncabezadoLista(0, estado.actualizando) }
                        anchoCompleto("vacio") { Vacio() }
                    }
                    is AlbumsUiState.Exito -> {
                        anchoCompleto("encabezado") { EncabezadoLista(estado.albumes.size, estado.actualizando) }
                        items(estado.albumes, key = { it.id }, contentType = { "album" }) { album ->
                            TarjetaAlbum(album, onSeleccionar)
                        }
                    }
                }
            }
            PullToRefreshContainer(
                state = deslizar,
                modifier = Modifier.align(Alignment.TopCenter),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            )
            SnackbarHost(hostState = avisos, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}

private fun LazyGridScope.anchoCompleto(clave: String, contenido: @Composable () -> Unit) =
    item(key = clave, span = { GridItemSpan(maxLineSpan) }) { contenido() }

@Composable
private fun Encabezado(session: Session, onAtras: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconButton(
            onClick = onAtras,
            modifier = Modifier
                .testTag(TagAtras)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.atras_descripcion),
                tint = MaterialTheme.colorScheme.secondary
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            val rol = when (session) {
                Session.Visitor -> stringResource(R.string.catalogo_etiqueta_visitante)
                is Session.Collector -> stringResource(R.string.catalogo_etiqueta_coleccionista)
                Session.None -> null
            }
            if (rol != null) {
                ChipEtiqueta(
                    texto = rol,
                    fondo = MaterialTheme.colorScheme.onBackground,
                    contenido = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.testTag(TagRolActivo)
                )
            }
            Text(
                text = stringResource(R.string.catalogo_encabezado),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
private fun Titular() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ChipEtiqueta(
                texto = stringResource(R.string.catalogo_chip),
                fondo = MaterialTheme.colorScheme.primary,
                contenido = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(Modifier.weight(1f))
            repeat(2) {
                Icon(
                    painter = painterResource(R.drawable.ic_cat_music_note),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.catalogo_titular_1),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = stringResource(R.string.catalogo_titular_2),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 12.dp, vertical = 2.dp)
            )
        }
        Text(
            text = stringResource(R.string.catalogo_subtitulo),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EncabezadoLista(cantidad: Int, actualizando: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.catalogo_lista),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (actualizando) {
            val descripcion = stringResource(R.string.catalogo_actualizando)
            CircularProgressIndicator(
                modifier = Modifier
                    .size(18.dp)
                    .semantics { contentDescription = descripcion }
                    .testTag(TagActualizandoCatalogo),
                strokeWidth = 2.dp
            )
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = pluralStringResource(R.plurals.catalogo_contador, cantidad, cantidad),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag(TagContadorCatalogo)
        )
    }
}

@Composable
private fun TarjetaAlbum(album: AlbumResumen, onSeleccionar: (Int) -> Unit) {
    val descripcion = stringResource(R.string.catalogo_seleccionar_descripcion, album.nombre)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .testTag(tagAlbum(album.id))
            .semantics { contentDescription = descripcion }
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onSeleccionar(album.id) }
    ) {
        Box {
            // Si la URL falta, no responde o no es una imagen, se ve la portada genérica.
            PortadaAlbum(
                url = album.portada,
                contentDescription = stringResource(R.string.catalogo_portada_descripcion, album.nombre),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            if (album.anio != null) {
                Text(
                    text = album.anio,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f))
                        .padding(horizontal = 10.dp, vertical = 2.dp)
                )
            }
        }
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = album.nombre,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium, fontSize = 18.sp),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (album.artista.isNotEmpty()) {
                Text(
                    text = album.artista,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(
                    painter = painterResource(R.drawable.ic_cat_album),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = if (album.anio != null) {
                        stringResource(R.string.catalogo_genero_anio, album.genero, album.anio)
                    } else {
                        album.genero
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun Cargando() {
    val descripcion = stringResource(R.string.catalogo_cargando)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 24.dp, horizontal = 16.dp)
            .semantics(mergeDescendants = true) { contentDescription = descripcion }
            .testTag(TagProgresoCatalogo),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(modifier = Modifier.size(56.dp), strokeWidth = 4.dp)
            Icon(
                painter = painterResource(R.drawable.ic_cat_graphic_eq),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = descripcion,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
    }
}

/** Tarjeta de relleno mientras carga, con la misma forma que una tarjeta de álbum. */
@Composable
private fun TarjetaEsqueleto() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Box(
            modifier = Modifier
                .width(110.dp)
                .height(14.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Box(
            modifier = Modifier
                .width(64.dp)
                .height(12.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
    }
}

@Composable
private fun Vacio() {
    TarjetaEstado {
        CirculoIcono(R.drawable.ic_cat_album, MaterialTheme.colorScheme.primary)
        Text(
            text = stringResource(R.string.catalogo_vacio),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag(TagVacioCatalogo)
        )
    }
}

@Composable
private fun Error(onReintentar: () -> Unit) {
    TarjetaEstado {
        Box {
            CirculoIcono(R.drawable.ic_cat_music_off, MaterialTheme.colorScheme.secondary)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_cat_priority_high),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Text(
            text = stringResource(R.string.catalogo_error),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag(TagErrorCatalogo)
        )
        Button(onClick = onReintentar, modifier = Modifier.testTag(TagReintentarCatalogo)) {
            Icon(imageVector = Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.catalogo_reintentar), style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** Tarjeta centrada de los estados vacío y error, con la forma de las tarjetas de álbum. */
@Composable
private fun TarjetaEstado(contenido: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 40.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        content = contenido
    )
}

/** Ícono grande sobre un círculo de fondo suave, como en los estados de artistas. */
@Composable
private fun CirculoIcono(icono: Int, tinte: Color) {
    Box(
        modifier = Modifier
            .size(96.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(painter = painterResource(icono), contentDescription = null, tint = tinte, modifier = Modifier.size(44.dp))
    }
}

private val AlbumesDeEjemplo = listOf(
    AlbumResumen(103, "A Day at the Races", null, "Rock", "Queen", "1976"),
    AlbumResumen(102, "A Night at the Opera", null, "Rock", "Queen", "1975"),
    AlbumResumen(100, "Buscando América", null, "Salsa", "Rubén Blades Bellido de Luna", "1984"),
    AlbumResumen(101, "Poeta del pueblo", null, "Salsa", "Rubén Blades Bellido de Luna", "1984")
)

@Preview(showBackground = true, heightDp = 800, name = "Con datos")
@Composable
private fun PreviewExito() = VinilosTheme {
    PantallaCatalogo(AlbumsUiState.Exito(AlbumesDeEjemplo), Session.Visitor, {}, {}, {}, {}, {})
}

@Preview(showBackground = true, heightDp = 800, name = "Actualizando")
@Composable
private fun PreviewActualizando() = VinilosTheme {
    PantallaCatalogo(AlbumsUiState.Exito(AlbumesDeEjemplo, actualizando = true), Session.Visitor, {}, {}, {}, {}, {})
}

@Preview(showBackground = true, heightDp = 800, name = "Cargando")
@Composable
private fun PreviewCargando() = VinilosTheme {
    PantallaCatalogo(AlbumsUiState.Cargando, Session.Visitor, {}, {}, {}, {}, {})
}

@Preview(showBackground = true, heightDp = 800, name = "Vacío")
@Composable
private fun PreviewVacio() = VinilosTheme {
    PantallaCatalogo(AlbumsUiState.Vacio(), Session.Visitor, {}, {}, {}, {}, {})
}

@Preview(showBackground = true, heightDp = 800, name = "Error")
@Composable
private fun PreviewError() = VinilosTheme {
    PantallaCatalogo(AlbumsUiState.Error, Session.Visitor, {}, {}, {}, {}, {})
}
