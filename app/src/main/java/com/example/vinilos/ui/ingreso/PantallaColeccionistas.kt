package com.example.vinilos.ui.ingreso

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.vinilos.R
import com.example.vinilos.data.model.Collector
import com.example.vinilos.ui.theme.AvatarAzul
import com.example.vinilos.ui.theme.AvatarAzulClaro
import com.example.vinilos.ui.theme.AvatarGris
import com.example.vinilos.ui.theme.AvatarGrisClaro
import com.example.vinilos.ui.theme.VinilosTheme
import com.example.vinilos.viewmodel.CollectorsUiState

const val TagProgreso = "progreso"
const val TagMensajeError = "mensaje_error"
const val TagReintentar = "boton_reintentar"
const val TagVisitanteError = "boton_visitante_error"
const val TagMensajeVacio = "mensaje_vacio"
const val TagVolver = "boton_volver"
const val TagBuscador = "campo_busqueda"
const val TagSinResultados = "sin_resultados"
const val TagListaColeccionistas = "lista_coleccionistas"

fun tagColeccionista(id: Int) = "coleccionista_$id"

/** Selector de coleccionista: lista, busca y muestra los estados de carga, error y vacío. */
@Composable
fun PantallaColeccionistas(
    estado: CollectorsUiState,
    onBuscar: (String) -> Unit,
    onReintentar: () -> Unit,
    onSeleccionar: (Collector) -> Unit,
    onVisitante: () -> Unit,
    onAtras: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        EncabezadoVinilo(modifier = Modifier.padding(horizontal = 12.dp), onAtras = onAtras)
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag(TagListaColeccionistas),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { TitularColeccionistas() }
            when (estado) {
                CollectorsUiState.Cargando -> item { Cargando() }
                CollectorsUiState.Error -> item { Error(onReintentar, onVisitante) }
                CollectorsUiState.Vacio -> item { Vacio(onAtras) }
                is CollectorsUiState.Exito -> {
                    item {
                        Box {
                            Buscador(consultaInicial = estado.consulta, onBuscar = onBuscar)
                            NotasDelBuscador(modifier = Modifier.align(Alignment.TopEnd))
                        }
                    }
                    if (estado.visibles.isEmpty()) {
                        item { SinResultados() }
                    } else {
                        items(estado.visibles, key = { it.id }) { coleccionista ->
                            FilaColeccionista(coleccionista, onSeleccionar)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Buscador(consultaInicial: String, onBuscar: (String) -> Unit) {
    // Texto local para que escribir no dependa del viaje por el ViewModel; el ViewModel conserva
    // el valor y rememberSaveable lo restaura al girar la pantalla.
    var texto by rememberSaveable { mutableStateOf(consultaInicial) }
    BasicTextField(
        value = texto,
        onValueChange = {
            texto = it
            onBuscar(it)
        },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(TagBuscador),
        decorationBox = { campo ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
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
                            text = stringResource(R.string.coleccionistas_buscar),
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
private fun FilaColeccionista(coleccionista: Collector, onSeleccionar: (Collector) -> Unit) {
    val descripcion = stringResource(R.string.coleccionistas_seleccionar_descripcion, coleccionista.name)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onSeleccionar(coleccionista) }
            .padding(14.dp)
            .semantics { contentDescription = descripcion }
            .testTag(tagColeccionista(coleccionista.id)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Avatar(coleccionista)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = coleccionista.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = coleccionista.email,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun Avatar(coleccionista: Collector) {
    val (claro, oscuro) =
        if (coleccionista.id % 2 == 0) AvatarAzulClaro to AvatarAzul else AvatarGrisClaro to AvatarGris
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(claro, oscuro))),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = iniciales(coleccionista.name),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimary
        )
    }
}

@Composable
private fun Cargando() {
    Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
        val descripcion = stringResource(R.string.coleccionistas_cargando_descripcion)
        CircularProgressIndicator(
            modifier = Modifier
                .semantics { contentDescription = descripcion }
                .testTag(TagProgreso)
        )
    }
}

@Composable
private fun Error(onReintentar: () -> Unit, onVisitante: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.coleccionistas_error),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag(TagMensajeError)
        )
        Button(onClick = onReintentar, modifier = Modifier.testTag(TagReintentar)) {
            Text(stringResource(R.string.coleccionistas_reintentar), style = MaterialTheme.typography.labelLarge)
        }
        TextButton(onClick = onVisitante, modifier = Modifier.testTag(TagVisitanteError)) {
            Text(stringResource(R.string.coleccionistas_ingresar_visitante), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun Vacio(onVolver: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.coleccionistas_vacio),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag(TagMensajeVacio)
        )
        Button(onClick = onVolver, modifier = Modifier.testTag(TagVolver)) {
            Text(stringResource(R.string.coleccionistas_volver), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun SinResultados() {
    Text(
        text = stringResource(R.string.coleccionistas_sin_resultados),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp).testTag(TagSinResultados)
    )
}

private val ColeccionistasDeEjemplo = listOf(
    Collector(100, "Manolo Bellon", "manollo@caracol.com.co"),
    Collector(101, "Jaime Monsalve", "jmonsalve@rtvc.com.co")
)

@Preview(showBackground = true, heightDp = 800, name = "Con datos")
@Composable
private fun PreviewExito() = VinilosTheme {
    PantallaColeccionistas(CollectorsUiState.Exito(ColeccionistasDeEjemplo), {}, {}, {}, {}, {})
}

@Preview(showBackground = true, heightDp = 800, name = "Cargando")
@Composable
private fun PreviewCargando() = VinilosTheme {
    PantallaColeccionistas(CollectorsUiState.Cargando, {}, {}, {}, {}, {})
}

@Preview(showBackground = true, heightDp = 800, name = "Error")
@Composable
private fun PreviewError() = VinilosTheme {
    PantallaColeccionistas(CollectorsUiState.Error, {}, {}, {}, {}, {})
}

@Preview(showBackground = true, heightDp = 800, name = "Vacío")
@Composable
private fun PreviewVacio() = VinilosTheme {
    PantallaColeccionistas(CollectorsUiState.Vacio, {}, {}, {}, {}, {})
}

@Preview(showBackground = true, heightDp = 800, name = "Sin coincidencias")
@Composable
private fun PreviewSinResultados() = VinilosTheme {
    PantallaColeccionistas(CollectorsUiState.Exito(ColeccionistasDeEjemplo, consulta = "zzz"), {}, {}, {}, {}, {})
}
