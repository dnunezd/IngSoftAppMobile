package com.example.vinilos.ui.comentar

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.vinilos.R
import com.example.vinilos.data.model.Album
import com.example.vinilos.data.model.Artista
import com.example.vinilos.data.model.Collector
import com.example.vinilos.data.model.Session
import com.example.vinilos.ui.catalogo.HeaderWithRole
import com.example.vinilos.ui.detalle.AlbumCover
import com.example.vinilos.ui.ingreso.ChipEtiqueta
import com.example.vinilos.ui.ingreso.iniciales
import com.example.vinilos.ui.theme.coloresBotonPrincipal
import com.example.vinilos.ui.ingreso.encabezado
import com.example.vinilos.ui.theme.VinilosTheme
import com.example.vinilos.viewmodel.CargaAlbum
import com.example.vinilos.viewmodel.AddCommentUiState
import com.example.vinilos.viewmodel.EnvioComment
import com.example.vinilos.viewmodel.LimiteOpinion
import com.example.vinilos.viewmodel.RatingLevel
import com.example.vinilos.viewmodel.nivelCalificacion
import com.example.vinilos.ui.idiomaDeLaApp

const val TagPantallaComentar = "comentar_pantalla"
const val TagAlbumComentado = "comentar_album"
const val TagRatingLevel = "comentar_nivel"
const val TagOpinion = "comentar_opinion"
const val TagContadorOpinion = "comentar_contador"
const val TagPublicarComment = "comentar_publicar"
const val TagErrorPublicar = "comentar_error"
const val TagReintentarAlbum = "comentar_reintentar_album"
const val TagDialogoDescartar = "comentar_dialogo_descartar"
const val TagContinuarEscribiendo = "comentar_continuar"
const val TagDescartarComment = "comentar_descartar"
const val TagCommentPublicado = "comentar_publicado"
const val TagAceptarPublicado = "comentar_aceptar"

fun tagEstrella(calificacion: Int) = "comentar_estrella_$calificacion"

/** Lo que el usuario puede hacer en la pantalla; lo resuelve `ComentarAlbumViewModel`. */
data class AddCommentActions(
    val onCalificar: (Int) -> Unit,
    val onOpinion: (String) -> Unit,
    val onPublicar: () -> Unit,
    /** Flecha, botón atrás del sistema y "Aceptar" (add-comentar-album, D5 y D6). */
    val onSalir: () -> Unit,
    val onContinuarEscribiendo: () -> Unit,
    val onDescartar: () -> Unit,
    val onReintentarAlbum: () -> Unit
)

/**
 * Comentar un álbum (HU09): solo la ve el Coleccionista. Con el comentario publicado, la misma pantalla
 * muestra el resumen de éxito (add-comentar-album, D6).
 */
@Composable
fun AddCommentScreen(
    estado: AddCommentUiState,
    session: Session,
    acciones: AddCommentActions,
    modifier: Modifier = Modifier
) {
    // El ViewModel decide: con borrador pregunta y durante el envío no hace nada.
    BackHandler(onBack = acciones.onSalir)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag(TagPantallaComentar)
    ) {
        HeaderWithRole(
            session = session,
            titulo = stringResource(R.string.comentar_encabezado),
            onAtras = acciones.onSalir,
            atrasHabilitado = !estado.enviando
        )
        when (val album = estado.album) {
            CargaAlbum.Loading -> LoadingAlbum()
            CargaAlbum.Error -> ErrorAlbum(acciones.onReintentarAlbum)
            is CargaAlbum.Listo -> {
                val nombre = (session as? Session.Collector)?.collector?.name.orEmpty()
                Desplazable {
                    if (estado.publicado) {
                        ContenidoPublicado(album.album, nombre, estado, acciones.onSalir)
                    } else {
                        Formulario(album.album, nombre, estado, acciones)
                    }
                }
            }
        }
    }

    if (estado.confirmarDescarte) DialogoDescartar(acciones)
}

@Composable
private fun Desplazable(contenido: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            contenido()
        }
    }
}

@Composable
private fun LoadingAlbum() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val descripcion = stringResource(R.string.comentar_cargando)
        CircularProgressIndicator(
            modifier = Modifier
                .size(56.dp)
                .semantics { contentDescription = descripcion },
            strokeWidth = 5.dp
        )
    }
}

@Composable
private fun ErrorAlbum(onReintentar: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
    ) {
        Text(
            stringResource(R.string.comentar_error_album),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Button(onClick = onReintentar, modifier = Modifier.testTag(TagReintentarAlbum)) {
            Text(stringResource(R.string.comentar_reintentar))
        }
    }
}

// ---------- Formulario ----------

@Composable
private fun Formulario(album: Album, coleccionista: String, estado: AddCommentUiState, acciones: AddCommentActions) {
    TarjetaAlbum(album, coleccionista)
    Tarjeta {
        Text(
            stringResource(R.string.comentar_pregunta),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().encabezado()
        )
        Text(
            stringResource(R.string.comentar_subtitulo),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Estrellas(estado.calificacion, habilitadas = !estado.enviando, onCalificar = acciones.onCalificar)
        TextoNivel(estado.calificacion)
        Opinion(estado, acciones.onOpinion)
        if (estado.envio == EnvioComment.Fallido) ErrorPublicar()
        BotonPublicar(estado, acciones.onPublicar)
    }
}

@Composable
private fun Tarjeta(contenido: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        contenido()
    }
}

@Composable
private fun TarjetaAlbum(album: Album, coleccionista: String) {
    Tarjeta {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            AlbumCover(
                url = album.portada,
                contentDescription = stringResource(R.string.detalle_portada_con_nombre, album.nombre),
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    stringResource(R.string.comentar_calificando).uppercase(idiomaDeLaApp()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    album.nombre,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.testTag(TagAlbumComentado)
                )
                Text(
                    datosDelAlbum(album),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(album.disquera, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
            }
        }
        if (coleccionista.isNotEmpty()) {
            ChipEtiqueta(
                texto = stringResource(R.string.comentar_coleccionista, coleccionista),
                fondo = MaterialTheme.colorScheme.surfaceVariant,
                contenido = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/** "Rubén Blades · 1984": artistas y año, sin la parte que falte. */
private fun datosDelAlbum(album: Album): String =
    listOf(album.artistas.joinToString(", ") { it.nombre }, album.fechaLanzamiento.take(4))
        .filter { it.isNotBlank() }
        .joinToString(" · ")

@Composable
private fun Estrellas(
    calificacion: Int?,
    habilitadas: Boolean,
    onCalificar: (Int) -> Unit,
    modifier: Modifier = Modifier,
    tamano: Int = 44
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        for (estrella in 1..5) {
            val marcada = calificacion != null && estrella <= calificacion
            val descripcion = stringResource(R.string.comentar_estrella, estrella)
            IconButton(
                onClick = { onCalificar(estrella) },
                enabled = habilitadas,
                modifier = Modifier
                    .size((tamano + 8).dp)
                    .testTag(tagEstrella(estrella))
                    .semantics {
                        contentDescription = descripcion
                        selected = marcada
                    }
            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = if (marcada) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    modifier = Modifier.size(tamano.dp)
                )
            }
        }
    }
}

/** Estrellas del resumen publicado: no son accionables y se anuncian como un solo elemento ("4 de 5 estrellas"). */
@Composable
private fun EstrellasSoloLectura(calificacion: Int?) {
    val valor = calificacion ?: 0
    val descripcion = stringResource(R.string.comentar_estrella, valor)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = descripcion },
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    ) {
        for (estrella in 1..5) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = if (estrella <= valor) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun TextoNivel(calificacion: Int?) {
    val nivel = nivelCalificacion(calificacion) ?: return
    Text(
        stringResource(R.string.comentar_nivel, stringResource(nivel.texto), requireNotNull(calificacion)),
        style = MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(TagRatingLevel)
    )
}

private val RatingLevel.texto: Int
    get() = when (this) {
        RatingLevel.MuyMalo -> R.string.comentar_nivel_1
        RatingLevel.Malo -> R.string.comentar_nivel_2
        RatingLevel.Regular -> R.string.comentar_nivel_3
        RatingLevel.Bueno -> R.string.comentar_nivel_4
        RatingLevel.Excelente -> R.string.comentar_nivel_5
    }

@Composable
private fun Opinion(estado: AddCommentUiState, onOpinion: (String) -> Unit) {
    OutlinedTextField(
        value = estado.opinion,
        onValueChange = onOpinion,
        enabled = !estado.enviando,
        placeholder = { Text(stringResource(R.string.comentar_opinion)) },
        minLines = 4,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp)
            .testTag(TagOpinion)
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            stringResource(R.string.comentar_publico),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        // Sin salto de línea: con la fuente al 200 % el contador quedaba apilado en vertical.
        Text(
            stringResource(R.string.comentar_contador, estado.opinion.length, LimiteOpinion),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier
                .padding(start = 8.dp)
                .testTag(TagContadorOpinion)
        )
    }
}

@Composable
private fun ErrorPublicar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(12.dp)
            .testTag(TagErrorPublicar),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
        Text(
            stringResource(R.string.comentar_error),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

@Composable
private fun BotonPublicar(estado: AddCommentUiState, onPublicar: () -> Unit) {
    Button(
        onClick = onPublicar,
        enabled = estado.puedePublicar,
        colors = coloresBotonPrincipal(enviando = estado.enviando),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag(TagPublicarComment)
    ) {
        if (estado.enviando) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp
            )
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.comentar_publicando), style = MaterialTheme.typography.labelLarge)
        } else {
            Text(stringResource(R.string.comentar_publicar), style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun DialogoDescartar(acciones: AddCommentActions) {
    AlertDialog(
        onDismissRequest = acciones.onContinuarEscribiendo,
        icon = { Icon(Icons.Filled.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text(stringResource(R.string.comentar_descartar_titulo), textAlign = TextAlign.Center) },
        text = { Text(stringResource(R.string.comentar_descartar_texto), textAlign = TextAlign.Center) },
        confirmButton = {
            Button(
                onClick = acciones.onContinuarEscribiendo,
                modifier = Modifier.testTag(TagContinuarEscribiendo)
            ) {
                Text(stringResource(R.string.comentar_continuar))
            }
        },
        dismissButton = {
            TextButton(onClick = acciones.onDescartar, modifier = Modifier.testTag(TagDescartarComment)) {
                Text(stringResource(R.string.comentar_descartar))
            }
        },
        modifier = Modifier.testTag(TagDialogoDescartar)
    )
}

// ---------- Éxito (D6) ----------

@Composable
private fun ContenidoPublicado(album: Album, coleccionista: String, estado: AddCommentUiState, onAceptar: () -> Unit) {
    Tarjeta {
        Row(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .testTag(TagCommentPublicado),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(
                stringResource(R.string.comentar_publicado),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        IlustracionPublicado(Modifier.align(Alignment.CenterHorizontally))
        Text(
            stringResource(R.string.comentar_publicado_titulo),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().encabezado()
        )
        Text(
            stringResource(R.string.comentar_publicado_texto),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AlbumCover(
                url = album.portada,
                contentDescription = stringResource(R.string.detalle_portada_con_nombre, album.nombre),
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
            Column {
                Text(
                    stringResource(R.string.comentar_album_resenado).uppercase(idiomaDeLaApp()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(album.nombre, style = MaterialTheme.typography.titleMedium)
                Text(datosDelAlbum(album), style = MaterialTheme.typography.bodySmall)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(iniciales(coleccionista), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Text(coleccionista, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            }
            EstrellasSoloLectura(estado.calificacion)
            Text(
                "«${estado.opinion.trim()}»",
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(12.dp)
            )
        }
    }
    Button(
        onClick = onAceptar,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag(TagAceptarPublicado)
    ) {
        Icon(Icons.Filled.ThumbUp, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.comentar_aceptar), style = MaterialTheme.typography.labelLarge)
    }
}

/** Vinilo con la marca de publicado, como en el mockup de éxito. */
@Composable
private fun IlustracionPublicado(modifier: Modifier = Modifier) {
    Box(modifier.size(152.dp)) {
        Image(
            painter = painterResource(R.drawable.vinilo_logo),
            contentDescription = stringResource(R.string.comentar_ilustracion),
            modifier = Modifier
                .size(140.dp)
                .align(Alignment.Center)
                .clip(CircleShape)
        )
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
        )
    }
}

// ---------- Vistas previas ----------

private val SesionDeEjemplo = Session.Collector(Collector(101, "Jaime Monsalve", "jmonsalve@rtvc.com.co"))

private val AlbumDeEjemplo = Album(
    id = 100, nombre = "Buscando América", portada = null, descripcion = "Salsa urbana", genero = "Salsa",
    disquera = "Elektra", fechaLanzamiento = "1984-08-01T00:00:00.000Z", canciones = emptyList(),
    artistas = listOf(Artista("Rubén Blades", null)), comentarios = emptyList()
)

private val AccionesDeEjemplo = AddCommentActions({}, {}, {}, {}, {}, {}, {})

@Preview(showBackground = true, heightDp = 900, name = "Formulario")
@Composable
private fun PreviewFormulario() = VinilosTheme {
    AddCommentScreen(
        estado = AddCommentUiState(
            album = CargaAlbum.Listo(AlbumDeEjemplo), calificacion = 5,
            opinion = "Prensado original con una profundidad de audio magistral."
        ),
        session = SesionDeEjemplo,
        acciones = AccionesDeEjemplo
    )
}

@Preview(showBackground = true, heightDp = 900, name = "Error")
@Composable
private fun PreviewError() = VinilosTheme {
    AddCommentScreen(
        estado = AddCommentUiState(
            album = CargaAlbum.Listo(AlbumDeEjemplo), calificacion = 3, opinion = "Buen sonido",
            envio = EnvioComment.Fallido
        ),
        session = SesionDeEjemplo,
        acciones = AccionesDeEjemplo
    )
}

@Preview(showBackground = true, heightDp = 900, name = "Publicado")
@Composable
private fun PreviewPublicado() = VinilosTheme {
    AddCommentScreen(
        estado = AddCommentUiState(
            album = CargaAlbum.Listo(AlbumDeEjemplo), calificacion = 5,
            opinion = "Prensado original con una profundidad de audio magistral.", envio = EnvioComment.Publicado
        ),
        session = SesionDeEjemplo,
        acciones = AccionesDeEjemplo
    )
}
