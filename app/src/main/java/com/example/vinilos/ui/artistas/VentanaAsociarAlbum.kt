package com.example.vinilos.ui.artistas

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.vinilos.R
import com.example.vinilos.data.model.AlbumResumen
import com.example.vinilos.ui.detalle.PortadaAlbum
import com.example.vinilos.ui.theme.VinilosTheme
import com.example.vinilos.viewmodel.AsociarAlbumUiState
import com.example.vinilos.viewmodel.ListadoAsociable
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.ui.platform.LocalDensity
import com.example.vinilos.ui.theme.coloresBotonPrincipal

const val TagBotonAgregarAlbum = "asociar_boton_agregar"
const val TagVentanaAsociar = "asociar_ventana"
const val TagTituloAsociar = "asociar_titulo"
const val TagBusquedaAsociar = "asociar_busqueda"
const val TagBotonAsociar = "asociar_boton_asociar"
const val TagAsociandoAsociar = "asociar_asociando"
const val TagCancelarAsociar = "asociar_cancelar"
const val TagCerrarAsociar = "asociar_cerrar"
const val TagErrorAsociar = "asociar_error"
const val TagVacioAsociar = "asociar_vacio"
const val TagSinResultadosAsociar = "asociar_sin_resultados"
const val TagCargandoAsociar = "asociar_cargando"
const val TagErrorListadoAsociar = "asociar_error_listado"
const val TagListaAsociar = "asociar_lista"

fun tagAlbumAsociable(id: Int) = "asociar_album_$id"
fun tagPortadaAsociable(id: Int) = "asociar_portada_$id"
fun tagYaAsociado(id: Int) = "asociar_ya_asociado_$id"

private val EsquinaFila = RoundedCornerShape(16.dp)
private val EsquinaPortada = RoundedCornerShape(12.dp)
private val Pildora = RoundedCornerShape(50)

/** Acciones de la ventana para agregar un álbum a un artista (HU15). */
data class AccionesAsociarAlbum(
    val onBuscar: (String) -> Unit = {},
    val onSeleccionar: (Int) -> Unit = {},
    val onAsociar: () -> Unit = {},
    /** "Cancelar", el botón de cierre, deslizar la ventana hacia abajo y el botón atrás. */
    val onCancelar: () -> Unit = {},
    val onReintentarCarga: () -> Unit = {}
)

/**
 * Ventana inferior para agregar un álbum a [nombreArtista] (HU15, add-agregar-album-artista D7). Sin estado:
 * solo se muestra mientras [estado] diga que está abierta. Mientras se asocia no se puede cerrar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VentanaAsociarAlbum(estado: AsociarAlbumUiState, nombreArtista: String, acciones: AccionesAsociarAlbum) {
    if (!estado.abierta) return
    val asociando by rememberUpdatedState(estado.asociando)
    val hoja = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden || !asociando }
    )
    ModalBottomSheet(
        onDismissRequest = acciones.onCancelar,
        sheetState = hoja,
        containerColor = MaterialTheme.colorScheme.surface,
        // El asa visible sigue midiendo 32 dp; el margen lleva su área táctil a 48 × 48 dp.
        dragHandle = { BottomSheetDefaults.DragHandle(modifier = Modifier.padding(horizontal = 8.dp)) }
    ) {
        ContenidoAsociar(estado, nombreArtista, acciones)
    }
}

@Composable
private fun ContenidoAsociar(estado: AsociarAlbumUiState, nombreArtista: String, acciones: AccionesAsociarAlbum) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(TagVentanaAsociar)
            .padding(horizontal = 20.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Encabezado(nombreArtista, cerrarActivo = !estado.asociando, onCerrar = acciones.onCancelar)
        BarraBusqueda(estado.busqueda, acciones.onBuscar)
        Listado(estado, acciones)
        if (estado.errorAlAsociar) ErrorAlAsociar()
        Botones(estado, acciones)
        Spacer(Modifier.size(4.dp))
    }
}

@Composable
private fun Encabezado(nombreArtista: String, cerrarActivo: Boolean, onCerrar: () -> Unit) {
    // El foco inicial va al título: en Android 8 o anterior, la ventana enfocaba sola la búsqueda y abría el
    // teclado, que dejaba la lista sin espacio. Además, TalkBack anuncia primero a qué artista se agrega.
    val foco = remember { FocusRequester() }
    LaunchedEffect(Unit) { foco.requestFocus() }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.asociar_titulo, nombreArtista),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .focusRequester(foco)
                .focusable()
                .semantics { heading() }
                .testTag(TagTituloAsociar)
        )
        IconButton(
            onClick = onCerrar,
            enabled = cerrarActivo,
            modifier = Modifier.testTag(TagCerrarAsociar)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.asociar_cerrar),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun BarraBusqueda(texto: String, onBuscar: (String) -> Unit) {
    TextField(
        value = texto,
        onValueChange = onBuscar,
        singleLine = true,
        placeholder = { Text(stringResource(R.string.asociar_buscar)) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = if (texto.isEmpty()) {
            null
        } else {
            {
                IconButton(onClick = { onBuscar("") }) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.asociar_limpiar_busqueda))
                }
            }
        },
        shape = Pildora,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(TagBusquedaAsociar)
    )
}

@Composable
private fun ColumnScope.Listado(estado: AsociarAlbumUiState, acciones: AccionesAsociarAlbum) {
    when (estado.listado) {
        ListadoAsociable.Cargando -> Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp)
                .semantics(mergeDescendants = true) {}
                .testTag(TagCargandoAsociar),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
            Text(
                text = stringResource(R.string.asociar_cargando),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        ListadoAsociable.Error -> Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag(TagErrorListadoAsociar),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.asociar_error_listado),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            TextButton(onClick = acciones.onReintentarCarga) { Text(stringResource(R.string.asociar_reintentar)) }
        }
        is ListadoAsociable.Listo -> {
            if (estado.sinDisponibles) Mensaje(R.string.asociar_sin_albumes, TagVacioAsociar)
            if (estado.sinResultados) Mensaje(R.string.asociar_sin_resultados, TagSinResultadosAsociar)
            LazyColumn(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .heightIn(max = 420.dp)
                    .testTag(TagListaAsociar),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(estado.visibles, key = { it.id }) { album ->
                    FilaAsociable(
                        album = album,
                        yaAsociado = estado.yaAsociado(album.id),
                        elegido = estado.seleccionado == album.id,
                        activa = !estado.asociando,
                        onSeleccionar = acciones.onSeleccionar
                    )
                }
            }
        }
    }
}

/** Mensaje centrado con el ícono de disco, como los bloques vacíos del detalle. */
@Composable
private fun Mensaje(@StringRes texto: Int, tag: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(EsquinaFila)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(vertical = 16.dp, horizontal = 12.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_cat_album),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = stringResource(texto),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Fila seleccionable con rol de botón de opción. La ya asociada queda deshabilitada, en gris y con la
 * etiqueta "Ya asociado"; su texto conserva el color pleno para no perder contraste (D7).
 */
@Composable
private fun FilaAsociable(
    album: AlbumResumen,
    yaAsociado: Boolean,
    elegido: Boolean,
    activa: Boolean,
    onSeleccionar: (Int) -> Unit
) {
    val fondo = when {
        yaAsociado -> MaterialTheme.colorScheme.surfaceVariant
        elegido -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }
    // `primary` y no `Naranja`: sobre `primaryContainer` el radio y el borde necesitan 3:1 (revisión del PR #114).
    val fuenteAmpliada = LocalDensity.current.fontScale > FuenteAmpliadaAsociar
    val borde = if (elegido) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(EsquinaFila)
            .background(fondo)
            .border(1.dp, borde, EsquinaFila)
            .testTag(tagAlbumAsociable(album.id))
            .selectable(
                selected = elegido,
                enabled = activa && !yaAsociado,
                role = Role.RadioButton,
                onClick = { onSeleccionar(album.id) }
            )
            .heightIn(min = 72.dp)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // La portada es decorativa dentro de la fila: el nombre ya se anuncia, así TalkBack no lo lee dos veces.
        Box(modifier = Modifier.clearAndSetSemantics { testTag = tagPortadaAsociable(album.id) }) {
            PortadaAlbum(
                url = album.portada,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .clip(EsquinaPortada)
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = album.nombre,
                style = MaterialTheme.typography.titleMedium,
                color = if (yaAsociado) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
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
            // Con la fuente ampliada la etiqueta va debajo del año: al lado dejaba el título truncado.
            if (yaAsociado && fuenteAmpliada) EtiquetaYaAsociado(album.id)
        }
        if (yaAsociado && !fuenteAmpliada) EtiquetaYaAsociado(album.id)
        RadioButton(
            selected = elegido,
            onClick = null,
            enabled = activa && !yaAsociado,
            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
        )
    }
}

@Composable
private fun ErrorAlAsociar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(EsquinaFila)
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(14.dp)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }
            .testTag(TagErrorAsociar),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_cat_priority_high),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = stringResource(R.string.asociar_error),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

@Composable
private fun Botones(estado: AsociarAlbumUiState, acciones: AccionesAsociarAlbum) {
    val descripcionAsociando = stringResource(R.string.asociar_asociando)
    Button(
        onClick = acciones.onAsociar,
        enabled = estado.puedeAsociar,
        colors = coloresBotonPrincipal(enviando = estado.asociando),
        shape = Pildora,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .testTag(TagBotonAsociar)
    ) {
        if (estado.asociando) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(18.dp)
                    .semantics { contentDescription = descripcionAsociando }
                    .testTag(TagAsociandoAsociar),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            Icon(
                painter = painterResource(R.drawable.ic_crear_enlace),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.asociar_asociar), style = MaterialTheme.typography.titleSmall)
    }
    Button(
        onClick = acciones.onCancelar,
        enabled = !estado.asociando,
        shape = Pildora,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .testTag(TagCancelarAsociar)
    ) {
        Text(stringResource(R.string.asociar_cancelar), style = MaterialTheme.typography.titleSmall)
    }
}

// ---------- Previsualizaciones ----------

private val previa = listOf(
    AlbumResumen(103, "A Day at the Races", null, "Rock", "Queen", "1976"),
    AlbumResumen(102, "A Night at the Opera", null, "Rock", "Queen", "1975"),
    AlbumResumen(100, "Buscando América", null, "Salsa", "Rubén Blades", "1984")
)

@Composable
private fun Previa(estado: AsociarAlbumUiState) = VinilosTheme {
    Column(Modifier.background(MaterialTheme.colorScheme.surface)) {
        ContenidoAsociar(estado, "Rubén Blades", AccionesAsociarAlbum())
    }
}

@Preview(showBackground = true, heightDp = 760)
@Composable
private fun PreviewSeleccion() =
    Previa(AsociarAlbumUiState(abierta = true, seleccionado = 102, asociados = setOf(100), listado = ListadoAsociable.Listo(previa)))

@Preview(showBackground = true, heightDp = 760)
@Composable
private fun PreviewErrorAlAsociar() = Previa(
    AsociarAlbumUiState(
        abierta = true, seleccionado = 102, errorAlAsociar = true, asociados = setOf(100),
        listado = ListadoAsociable.Listo(previa)
    )
)

@Preview(showBackground = true)
@Composable
private fun PreviewSinDisponibles() =
    Previa(AsociarAlbumUiState(abierta = true, asociados = setOf(100, 102, 103), listado = ListadoAsociable.Listo(previa)))

@Preview(showBackground = true)
@Composable
private fun PreviewSinResultados() =
    Previa(AsociarAlbumUiState(abierta = true, busqueda = "zzz", listado = ListadoAsociable.Listo(previa)))

@Preview(showBackground = true)
@Composable
private fun PreviewCargando() = Previa(AsociarAlbumUiState(abierta = true))

@Preview(showBackground = true)
@Composable
private fun PreviewErrorListado() = Previa(AsociarAlbumUiState(abierta = true, listado = ListadoAsociable.Error))

/** Escala de fuente a partir de la cual la etiqueta "Ya asociado" pasa debajo del año. */
private const val FuenteAmpliadaAsociar = 1.15f

@Composable
private fun EtiquetaYaAsociado(id: Int) {
    Text(
        text = stringResource(R.string.asociar_ya_asociado),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .testTag(tagYaAsociado(id))
    )
}
