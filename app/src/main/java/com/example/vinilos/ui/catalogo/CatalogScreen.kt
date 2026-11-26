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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
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
import com.example.vinilos.data.model.AlbumSummary
import com.example.vinilos.data.model.Session
import com.example.vinilos.ui.detalle.AlbumCover
import com.example.vinilos.ui.ingreso.ChipEtiqueta
import com.example.vinilos.ui.theme.VinilosTheme
import com.example.vinilos.viewmodel.NewAlbumCatalogo
import com.example.vinilos.viewmodel.AlbumsUiState
import com.example.vinilos.ui.ingreso.encabezado

/** `catalogo` and `catalogo_rol` are the tags already used by the login and rotation tests (HU00). */
const val TagCatalogo = "catalogo"
const val TagRolActivo = "catalogo_rol"
const val TagCuadriculaCatalogo = "catalogo_cuadricula"
const val TagProgresoCatalogo = "catalogo_progreso"
const val TagActualizandoCatalogo = "catalogo_actualizando"
const val TagContadorCatalogo = "catalogo_contador"
const val TagEmptyCatalogo = "catalogo_vacio"
const val TagErrorCatalogo = "catalogo_error"
const val TagReintentarCatalogo = "catalogo_reintentar"
const val TagCreateAlbumCatalogo = "catalogo_crear_album"

fun tagAlbum(id: Int) = "album_$id"

fun tagNewAlbum(id: Int) = "album_nuevo_$id"

/** Grid items before the first album: the title and the list header. */
private const val ElementosAntesDeAlbumes = 2

/**
 * Album catalog (HU01): grid with cover, name and genre, pull down to
 * refresh, and loading, empty and error states. The albums are the same for the Visitor and the
 * Collector; the latter also sees the "Create album" button (HU07), and when returning from creating one
 * it is notified, scrolled to it, and marked as new.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogScreen(
    estado: AlbumsUiState,
    session: Session,
    onActualizar: () -> Unit,
    onReintentar: () -> Unit,
    onAvisoMostrado: () -> Unit,
    onSeleccionar: (Int) -> Unit,
    onAtras: () -> Unit,
    modifier: Modifier = Modifier,
    albumNuevo: NewAlbumCatalogo? = null,
    onCreateAlbum: () -> Unit = {},
    onAvisoCreadoMostrado: () -> Unit = {},
    onDesplazadoAlNuevo: () -> Unit = {}
) {
    val cargado = estado is AlbumsUiState.Success || estado is AlbumsUiState.Empty
    // The refresh progress is shown next to the list header, so the
    // pull-to-refresh indicator never keeps spinning (isRefreshing = false) and hides itself again.
    val deslizar = rememberPullToRefreshState()

    val avisos = remember { SnackbarHostState() }
    val mensajeAviso = stringResource(R.string.catalogo_error_actualizar)
    val conAviso = (estado as? AlbumsUiState.Success)?.errorAlActualizar == true ||
        (estado as? AlbumsUiState.Empty)?.errorAlActualizar == true
    LaunchedEffect(conAviso) {
        if (conAviso) {
            // finally: if the screen exits before it finishes (tapping an album, rotating), the notice
            // stays marked as shown and doesn't repeat when coming back.
            try {
                avisos.showSnackbar(mensajeAviso)
            } finally {
                onAvisoMostrado()
            }
        }
    }

    val mensajeCreado = stringResource(R.string.catalogo_album_creado)
    val avisoCreadoPendiente = albumNuevo?.avisoPendiente == true
    LaunchedEffect(avisoCreadoPendiente) {
        if (avisoCreadoPendiente) {
            try {
                avisos.showSnackbar(mensajeCreado)
            } finally {
                onAvisoCreadoMostrado()
            }
        }
    }

    val cuadricula = rememberLazyGridState()
    // The new album may end up far away due to alphabetical order. It waits for the list to contain it.
    val indiceNuevo = (estado as? AlbumsUiState.Success)?.albumes
        ?.indexOfFirst { it.id == albumNuevo?.id }
        ?.takeIf { it >= 0 && albumNuevo?.desplazamientoPendiente == true }
    LaunchedEffect(indiceNuevo) {
        if (indiceNuevo != null) {
            try {
                cuadricula.animateScrollToItem(indiceNuevo + ElementosAntesDeAlbumes)
            } finally {
                onDesplazadoAlNuevo()
            }
        }
    }

    val puedeCrear = session.collectorActionsEnabled
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag(TagCatalogo)
    ) {
        HeaderWithRole(session, stringResource(R.string.catalogo_encabezado), onAtras)
        // clipToBounds: the pull-to-refresh indicator waits hidden over the list and must not show
        // above the header.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .pullToRefresh(
                    isRefreshing = false,
                    state = deslizar,
                    enabled = cargado,
                    onRefresh = onActualizar
                )
        ) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 156.dp),
                state = cuadricula,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(TagCuadriculaCatalogo),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    top = 8.dp,
                    end = 20.dp,
                    bottom = if (puedeCrear) EspacioBotonCrear else 8.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                anchoCompleto("titular") { Titular() }
                when (estado) {
                    AlbumsUiState.Loading -> {
                        anchoCompleto("cargando") { Loading() }
                        items(4) { TarjetaEsqueleto() }
                    }
                    AlbumsUiState.Error -> anchoCompleto("error") { Error(onReintentar) }
                    is AlbumsUiState.Empty -> {
                        anchoCompleto("encabezado") { EncabezadoLista(0, estado.actualizando) }
                        anchoCompleto("vacio") { Empty() }
                    }
                    is AlbumsUiState.Success -> {
                        anchoCompleto("encabezado") { EncabezadoLista(estado.albumes.size, estado.actualizando) }
                        items(estado.albumes, key = { it.id }, contentType = { "album" }) { album ->
                            TarjetaAlbum(album, album.id == albumNuevo?.id, onSeleccionar)
                        }
                    }
                }
            }
            PullToRefreshDefaults.Indicator(
                state = deslizar,
                isRefreshing = false,
                modifier = Modifier.align(Alignment.TopCenter),
                containerColor = MaterialTheme.colorScheme.surface,
                color = MaterialTheme.colorScheme.primary
            )
            if (puedeCrear) {
                ExtendedFloatingActionButton(
                    onClick = onCreateAlbum,
                    // Material hides the text from accessibility, so the icon provides the name.
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = stringResource(R.string.crear_album)
                        )
                    },
                    text = {
                        Text(stringResource(R.string.crear_album), style = MaterialTheme.typography.labelLarge)
                    },
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .testTag(TagCreateAlbumCatalogo)
                )
            }
            SnackbarHost(
                hostState = avisos,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (puedeCrear) EspacioBotonCrear else 0.dp)
            )
        }
    }
}

/** Space left free by the "Create album" button: its 56 dp height plus margins. */
private val EspacioBotonCrear = 88.dp

private fun LazyGridScope.anchoCompleto(clave: String, contenido: @Composable () -> Unit) =
    item(key = clave, span = { GridItemSpan(maxLineSpan) }) { contenido() }

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
        Row(
            modifier = Modifier.encabezado(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.encabezado()
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
private fun TarjetaAlbum(album: AlbumSummary, esNuevo: Boolean, onSeleccionar: (Int) -> Unit) {
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
            // If the URL is missing, doesn't respond, or isn't an image, the generic cover is shown.
            AlbumCover(
                url = album.portada,
                contentDescription = stringResource(R.string.catalogo_portada_descripcion, album.nombre),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            )
            // The year remains visible next to the genre, so the badge takes its place.
            if (esNuevo) {
                ChipEtiqueta(
                    texto = stringResource(R.string.catalogo_nuevo),
                    fondo = MaterialTheme.colorScheme.primary,
                    contenido = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .testTag(tagNewAlbum(album.id))
                )
            } else if (album.anio != null) {
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
private fun Loading() {
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

/** Placeholder card while loading, with the same shape as an album card. */
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
private fun Empty() {
    TarjetaEstado {
        CirculoIcono(R.drawable.ic_cat_album, MaterialTheme.colorScheme.primary)
        Text(
            text = stringResource(R.string.catalogo_vacio),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag(TagEmptyCatalogo)
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

/** Centered card for the empty and error states, with the shape of the album cards. */
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

/** Large icon over a soft background circle, like in the artist states. */
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
    AlbumSummary(103, "A Day at the Races", null, "Rock", "Queen", "1976"),
    AlbumSummary(102, "A Night at the Opera", null, "Rock", "Queen", "1975"),
    AlbumSummary(100, "Buscando América", null, "Salsa", "Rubén Blades Bellido de Luna", "1984"),
    AlbumSummary(101, "Poeta del pueblo", null, "Salsa", "Rubén Blades Bellido de Luna", "1984")
)

@Preview(showBackground = true, heightDp = 800, name = "Con datos")
@Composable
private fun PreviewSuccess() = VinilosTheme {
    CatalogScreen(AlbumsUiState.Success(AlbumesDeEjemplo), Session.Visitor, {}, {}, {}, {}, {})
}

@Preview(showBackground = true, heightDp = 800, name = "Actualizando")
@Composable
private fun PreviewActualizando() = VinilosTheme {
    CatalogScreen(AlbumsUiState.Success(AlbumesDeEjemplo, actualizando = true), Session.Visitor, {}, {}, {}, {}, {})
}

@Preview(showBackground = true, heightDp = 800, name = "Cargando")
@Composable
private fun PreviewLoading() = VinilosTheme {
    CatalogScreen(AlbumsUiState.Loading, Session.Visitor, {}, {}, {}, {}, {})
}

@Preview(showBackground = true, heightDp = 800, name = "Vacío")
@Composable
private fun PreviewEmpty() = VinilosTheme {
    CatalogScreen(AlbumsUiState.Empty(), Session.Visitor, {}, {}, {}, {}, {})
}

@Preview(showBackground = true, heightDp = 800, name = "Error")
@Composable
private fun PreviewError() = VinilosTheme {
    CatalogScreen(AlbumsUiState.Error, Session.Visitor, {}, {}, {}, {}, {})
}
