package com.example.vinilos.ui.artistas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.vinilos.R
import com.example.vinilos.data.model.Artist
import com.example.vinilos.data.model.ArtistType
import com.example.vinilos.ui.ingreso.ChipEtiqueta
import com.example.vinilos.ui.ingreso.TagAtras
import com.example.vinilos.ui.theme.EtiquetaBandaFondo
import com.example.vinilos.ui.theme.EtiquetaBandaTexto
import com.example.vinilos.ui.theme.VinilosTheme
import com.example.vinilos.viewmodel.ArtistsUiState

/** Se conserva el tag del destino provisional: las pruebas de navegación llegan aquí por él. */
const val TagArtistas = "artistas"
const val TagProgresoArtistas = "artistas_progreso"
const val TagBuscadorArtistas = "artistas_buscador"
const val TagContadorArtistas = "artistas_contador"
const val TagVacioArtistas = "artistas_vacio"
const val TagErrorArtistas = "artistas_error"
const val TagReintentarArtistas = "artistas_reintentar"
const val TagListaArtistas = "artistas_lista"

fun tagArtista(artista: Artist) = "artista_${artista.type.name.lowercase()}_${artista.id}"

/** Listado de músicos y bandas con búsqueda y sus estados de carga, vacío y error (HU03). */
@Composable
fun PantallaArtistas(
    estado: ArtistsUiState,
    onBuscar: (String) -> Unit,
    onReintentar: () -> Unit,
    onSeleccionar: (Artist) -> Unit,
    onAtras: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag(TagArtistas)
    ) {
        Encabezado(onAtras)
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag(TagListaArtistas),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Titular() }
            item {
                Buscador(
                    consultaInicial = (estado as? ArtistsUiState.Exito)?.consulta.orEmpty(),
                    habilitado = estado is ArtistsUiState.Exito,
                    onBuscar = onBuscar
                )
            }
            when (estado) {
                ArtistsUiState.Cargando -> {
                    item { Cargando() }
                    items(5) { FilaEsqueleto() }
                }
                ArtistsUiState.Error -> item { Error(onReintentar) }
                ArtistsUiState.Vacio -> item { Vacio(conBusqueda = false) }
                is ArtistsUiState.Exito -> {
                    if (estado.visibles.isEmpty()) {
                        item { Vacio(conBusqueda = true) }
                    } else {
                        item { EncabezadoLista(estado.visibles.size) }
                        items(estado.visibles, key = { tagArtista(it) }) { artista ->
                            FilaArtista(artista, onSeleccionar)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Encabezado(onAtras: () -> Unit) {
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
        Text(
            text = stringResource(R.string.artistas_titulo),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun Titular() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ChipEtiqueta(
            texto = stringResource(R.string.artistas_chip),
            fondo = MaterialTheme.colorScheme.primary,
            contenido = MaterialTheme.colorScheme.onPrimary
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.artistas_titular_1),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = stringResource(R.string.artistas_titular_2),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 12.dp, vertical = 2.dp)
            )
        }
        Text(
            text = stringResource(R.string.artistas_subtitulo),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun Buscador(consultaInicial: String, habilitado: Boolean, onBuscar: (String) -> Unit) {
    // Texto local para que escribir no dependa del viaje por el ViewModel; el ViewModel conserva
    // el valor y rememberSaveable lo restaura al girar la pantalla.
    var texto by rememberSaveable { mutableStateOf(consultaInicial) }
    BasicTextField(
        value = texto,
        onValueChange = {
            texto = it
            onBuscar(it)
        },
        enabled = habilitado,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(TagBuscadorArtistas),
        decorationBox = { campo ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.onSurfaceVariant, CircleShape)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (texto.isEmpty()) {
                        Text(
                            text = stringResource(R.string.artistas_buscar),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    campo()
                }
            }
        }
    )
}

@Composable
private fun EncabezadoLista(cantidad: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.artistas_lista),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = pluralStringResource(R.plurals.artistas_contador, cantidad, cantidad),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag(TagContadorArtistas)
        )
    }
}

@Composable
private fun FilaArtista(artista: Artist, onSeleccionar: (Artist) -> Unit) {
    val descripcion = stringResource(R.string.artistas_seleccionar_descripcion, artista.name)
    val porDefecto = painterResource(
        if (artista.type == ArtistType.BANDA) R.drawable.ic_banda_por_defecto else R.drawable.ic_artista_por_defecto
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .testTag(tagArtista(artista))
            .semantics { contentDescription = descripcion }
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onSeleccionar(artista) }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Si la URL no responde, no es una imagen o no existe, se muestra la imagen por defecto.
        AsyncImage(
            model = artista.image?.takeIf { it.isNotBlank() },
            contentDescription = stringResource(R.string.artistas_imagen_descripcion, artista.name),
            placeholder = porDefecto,
            error = porDefecto,
            fallback = porDefecto,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = artista.name,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium, fontSize = 18.sp),
                color = MaterialTheme.colorScheme.onSurface
            )
            EtiquetaTipo(artista.type)
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EtiquetaTipo(tipo: ArtistType) {
    val (texto, fondo, contenido) = when (tipo) {
        ArtistType.MUSICO -> Triple(
            stringResource(R.string.artistas_tipo_musico),
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        ArtistType.BANDA -> Triple(stringResource(R.string.artistas_tipo_banda), EtiquetaBandaFondo, EtiquetaBandaTexto)
    }
    Text(
        text = texto,
        style = MaterialTheme.typography.labelLarge,
        color = contenido,
        modifier = Modifier
            .clip(CircleShape)
            .background(fondo)
            .padding(horizontal = 12.dp, vertical = 2.dp)
    )
}

@Composable
private fun Cargando() {
    val descripcion = stringResource(R.string.artistas_cargando)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .semantics(mergeDescendants = true) { contentDescription = descripcion }
            .testTag(TagProgresoArtistas),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
        Text(
            text = descripcion,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Fila de relleno mientras carga, con la misma forma que una fila de artista. */
@Composable
private fun FilaEsqueleto() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .width(150.dp)
                    .height(14.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Box(
                modifier = Modifier
                    .width(64.dp)
                    .height(18.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        }
    }
}

@Composable
private fun Vacio(conBusqueda: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
        }
        Text(
            text = stringResource(R.string.artistas_vacio),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag(TagVacioArtistas)
        )
        if (conBusqueda) {
            Text(
                text = stringResource(R.string.artistas_sin_coincidencias),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun Error(onReintentar: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .border(2.dp, MaterialTheme.colorScheme.secondary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "!", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.secondary)
            }
        }
        Text(
            text = stringResource(R.string.artistas_error),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag(TagErrorArtistas)
        )
        Button(onClick = onReintentar, modifier = Modifier.testTag(TagReintentarArtistas)) {
            Icon(imageVector = Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.artistas_reintentar), style = MaterialTheme.typography.labelLarge)
        }
    }
}

private val ArtistasDeEjemplo = listOf(
    Artist(101, "Queen", null, ArtistType.BANDA),
    Artist(100, "Rubén Blades Bellido de Luna", null, ArtistType.MUSICO)
)

@Preview(showBackground = true, heightDp = 800, name = "Con datos")
@Composable
private fun PreviewExito() = VinilosTheme {
    PantallaArtistas(ArtistsUiState.Exito(ArtistasDeEjemplo), {}, {}, {}, {})
}

@Preview(showBackground = true, heightDp = 800, name = "Cargando")
@Composable
private fun PreviewCargando() = VinilosTheme {
    PantallaArtistas(ArtistsUiState.Cargando, {}, {}, {}, {})
}

@Preview(showBackground = true, heightDp = 800, name = "Error")
@Composable
private fun PreviewError() = VinilosTheme {
    PantallaArtistas(ArtistsUiState.Error, {}, {}, {}, {})
}

@Preview(showBackground = true, heightDp = 800, name = "Sin coincidencias")
@Composable
private fun PreviewSinCoincidencias() = VinilosTheme {
    PantallaArtistas(ArtistsUiState.Exito(ArtistasDeEjemplo, consulta = "zzz"), {}, {}, {}, {})
}
