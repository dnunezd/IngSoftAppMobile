package com.example.vinilos.ui.crear

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.os.ConfigurationCompat
import com.example.vinilos.R
import com.example.vinilos.data.model.Collector
import com.example.vinilos.data.model.DisquerasAlbum
import com.example.vinilos.data.model.GenerosAlbum
import com.example.vinilos.data.model.Session
import com.example.vinilos.data.model.anioUtc
import com.example.vinilos.data.model.fechaSeleccionable
import com.example.vinilos.data.model.fechaVisible
import com.example.vinilos.data.model.hoyEnUtc
import com.example.vinilos.ui.catalogo.EncabezadoConRol
import com.example.vinilos.ui.detalle.PortadaAlbum
import com.example.vinilos.ui.theme.VinilosTheme
import com.example.vinilos.viewmodel.CampoAlbum
import com.example.vinilos.viewmodel.CrearAlbumUiState
import com.example.vinilos.viewmodel.EnvioAlbum
import com.example.vinilos.viewmodel.ErrorCampo
import com.example.vinilos.viewmodel.LimiteDescripcion
import com.example.vinilos.viewmodel.portadaValida
import java.util.Locale
import java.util.TimeZone

const val TagFormularioCrear = "crear_formulario"
const val TagNombreAlbum = "crear_nombre"
const val TagPortadaAlbum = "crear_portada"
const val TagFechaAlbum = "crear_fecha"
const val TagGeneroAlbum = "crear_genero"
const val TagDisqueraAlbum = "crear_disquera"
const val TagDescripcionAlbum = "crear_descripcion"
const val TagContadorDescripcion = "crear_contador"
const val TagAvisoCampos = "crear_aviso_campos"
const val TagErrorEnvio = "crear_error_envio"
const val TagCrearAlbum = "crear_enviar"
const val TagCancelarCrear = "crear_cancelar"

fun tagErrorCampo(campo: CampoAlbum) = "crear_error_${campo.name}"

private const val PrimerAnioDelSelector = 1900

/** Acciones del formulario, agrupadas para no pasar nueve callbacks por separado. */
data class AccionesCrearAlbum(
    val onNombre: (String) -> Unit = {},
    val onPortada: (String) -> Unit = {},
    val onFecha: (Long) -> Unit = {},
    val onGenero: (String) -> Unit = {},
    val onDisquera: (String) -> Unit = {},
    val onDescripcion: (String) -> Unit = {},
    val onCrear: () -> Unit = {},
    val onCancelar: () -> Unit = {}
)

/** Formulario para crear un álbum (HU07). No tiene estado propio salvo qué menú está abierto. */
@Composable
fun PantallaCrearAlbum(
    estado: CrearAlbumUiState,
    session: Session,
    acciones: AccionesCrearAlbum,
    modifier: Modifier = Modifier
) {
    // Salir durante el envío cancelaría una petición que el backend quizá ya guardó.
    BackHandler(enabled = estado.enviando) {}

    val desplazamiento = rememberScrollState()
    // Guardado para que al girar no vuelva a subir por un intento anterior.
    var ultimoIntento by rememberSaveable { mutableIntStateOf(estado.intentosFallidos) }
    LaunchedEffect(estado.intentosFallidos) {
        if (estado.intentosFallidos != ultimoIntento) {
            ultimoIntento = estado.intentosFallidos
            desplazamiento.animateScrollTo(0)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag(TagFormularioCrear)
    ) {
        EncabezadoConRol(
            session = session,
            titulo = stringResource(R.string.crear_encabezado),
            onAtras = acciones.onCancelar,
            atrasHabilitado = !estado.enviando
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(desplazamiento),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Titular()
                if (estado.errores.isNotEmpty()) AvisoCampos()
                TarjetaCampos(estado, acciones)
                if (estado.envio == EnvioAlbum.Fallido) ErrorEnvio()
                Botones(estado, acciones)
            }
        }
    }
}

@Composable
private fun Titular() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.crear_titular_1),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = stringResource(R.string.crear_titular_2),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(horizontal = 12.dp, vertical = 2.dp)
            )
        }
        Text(
            text = stringResource(R.string.crear_subtitulo),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AvisoCampos() {
    TarjetaError(
        titulo = stringResource(R.string.crear_aviso_titulo),
        detalle = stringResource(R.string.crear_aviso_detalle),
        modifier = Modifier.testTag(TagAvisoCampos)
    )
}

@Composable
private fun ErrorEnvio() {
    TarjetaError(
        titulo = stringResource(R.string.crear_error_envio),
        modifier = Modifier.testTag(TagErrorEnvio)
    )
}

@Composable
private fun TarjetaError(titulo: String, modifier: Modifier = Modifier, detalle: String? = null) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = titulo,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            if (detalle != null) {
                Text(
                    text = detalle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
private fun TarjetaCampos(estado: CrearAlbumUiState, acciones: AccionesCrearAlbum) {
    val habilitado = !estado.enviando
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Campo(stringResource(R.string.crear_nombre), estado, CampoAlbum.Nombre) {
            CampoTexto(
                valor = estado.nombre,
                onCambio = acciones.onNombre,
                indicacion = stringResource(R.string.crear_nombre_indicacion),
                icono = R.drawable.ic_cat_album,
                conError = CampoAlbum.Nombre in estado.errores,
                habilitado = habilitado,
                teclado = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.testTag(TagNombreAlbum)
            )
        }
        Campo(stringResource(R.string.crear_portada), estado, CampoAlbum.Portada) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CampoTexto(
                    valor = estado.portada,
                    onCambio = acciones.onPortada,
                    indicacion = stringResource(R.string.crear_portada_indicacion),
                    icono = R.drawable.ic_crear_enlace,
                    conError = CampoAlbum.Portada in estado.errores,
                    habilitado = habilitado,
                    // Sin autocorrección: el teclado cambiaba letras de la URL y dejaba otra dirección.
                    teclado = KeyboardOptions(
                        autoCorrect = false,
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag(TagPortadaAlbum)
                )
                if (portadaValida(estado.portada)) {
                    PortadaAlbum(
                        url = estado.portada.trim(),
                        contentDescription = stringResource(R.string.crear_portada_vista_previa),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                }
            }
        }
        Campo(stringResource(R.string.crear_fecha), estado, CampoAlbum.Fecha) {
            CampoFecha(estado.fechaMillis, CampoAlbum.Fecha in estado.errores, habilitado, acciones.onFecha)
        }
        Campo(stringResource(R.string.crear_genero), estado, CampoAlbum.Genero) {
            CampoOpciones(
                valor = estado.genero,
                opciones = GenerosAlbum,
                indicacion = stringResource(R.string.crear_genero_indicacion),
                icono = R.drawable.ic_crear_genero,
                conError = CampoAlbum.Genero in estado.errores,
                habilitado = habilitado,
                onElegir = acciones.onGenero,
                tag = TagGeneroAlbum
            )
        }
        Campo(stringResource(R.string.crear_disquera), estado, CampoAlbum.Disquera) {
            CampoOpciones(
                valor = estado.disquera,
                opciones = DisquerasAlbum,
                indicacion = stringResource(R.string.crear_disquera_indicacion),
                icono = R.drawable.ic_crear_disquera,
                conError = CampoAlbum.Disquera in estado.errores,
                habilitado = habilitado,
                onElegir = acciones.onDisquera,
                tag = TagDisqueraAlbum
            )
        }
        Campo(
            etiqueta = stringResource(R.string.crear_descripcion),
            estado = estado,
            campo = CampoAlbum.Descripcion,
            extra = {
                Text(
                    text = stringResource(R.string.crear_contador, estado.descripcion.length, LimiteDescripcion),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag(TagContadorDescripcion)
                )
            }
        ) {
            CampoDescripcion(
                valor = estado.descripcion,
                onCambio = acciones.onDescripcion,
                conError = CampoAlbum.Descripcion in estado.errores,
                habilitado = habilitado
            )
        }
    }
}

/** Etiqueta con asterisco, el control del campo y su mensaje de error. */
@Composable
private fun Campo(
    etiqueta: String,
    estado: CrearAlbumUiState,
    campo: CampoAlbum,
    extra: @Composable () -> Unit = {},
    control: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = buildAnnotatedString {
                    append(etiqueta)
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) { append(" *") }
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.weight(1f))
            extra()
        }
        control()
        estado.errores[campo]?.let { error ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.testTag(tagErrorCampo(campo))
            ) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = stringResource(
                        when (error) {
                            ErrorCampo.Obligatorio -> R.string.crear_campo_obligatorio
                            ErrorCampo.UrlInvalida -> R.string.crear_url_invalida
                        }
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun bordeDe(conError: Boolean): Color =
    if (conError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline

/** Píldora común de los campos, con el mismo aspecto que el buscador de artistas. */
@Composable
private fun Pildora(
    conError: Boolean,
    modifier: Modifier = Modifier,
    contenido: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, bordeDe(conError), CircleShape)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        contenido()
    }
}

@Composable
private fun IconoCampo(icono: Int) {
    Icon(
        painter = painterResource(icono),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(20.dp)
    )
}

@Composable
private fun TextoIndicacion(texto: String) {
    Text(
        text = texto,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1
    )
}

@Composable
private fun CampoTexto(
    valor: String,
    onCambio: (String) -> Unit,
    indicacion: String,
    icono: Int,
    conError: Boolean,
    habilitado: Boolean,
    teclado: KeyboardOptions,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = valor,
        onValueChange = onCambio,
        enabled = habilitado,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = teclado,
        modifier = modifier.fillMaxWidth(),
        decorationBox = { campo ->
            Pildora(conError) {
                IconoCampo(icono)
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (valor.isEmpty()) TextoIndicacion(indicacion)
                    campo()
                }
            }
        }
    )
}

@Composable
private fun CampoDescripcion(
    valor: String,
    onCambio: (String) -> Unit,
    conError: Boolean,
    habilitado: Boolean
) {
    val forma = RoundedCornerShape(24.dp)
    BasicTextField(
        value = valor,
        onValueChange = onCambio,
        enabled = habilitado,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(TagDescripcionAlbum),
        decorationBox = { campo ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 120.dp)
                    .clip(forma)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, bordeDe(conError), forma)
                    .padding(16.dp)
            ) {
                if (valor.isEmpty()) {
                    Text(
                        text = stringResource(R.string.crear_descripcion_indicacion),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                campo()
            }
        }
    )
}

/** Campo de solo lectura: la fecha únicamente se elige en el selector. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CampoFecha(
    fechaMillis: Long?,
    conError: Boolean,
    habilitado: Boolean,
    onFecha: (Long) -> Unit
) {
    var abierto by rememberSaveable { mutableStateOf(false) }
    Pildora(
        conError = conError,
        modifier = Modifier
            .clip(CircleShape)
            .clickable(enabled = habilitado, role = Role.Button) { abierto = true }
            .testTag(TagFechaAlbum)
    ) {
        Icon(
            imageVector = Icons.Filled.DateRange,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        if (fechaMillis != null) {
            Text(
                text = fechaVisible(fechaMillis),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        } else {
            TextoIndicacion(stringResource(R.string.crear_fecha_indicacion))
        }
    }
    if (abierto) {
        SelectorFecha(
            fechaMillis = fechaMillis,
            onAceptar = {
                onFecha(it)
                abierto = false
            },
            onCerrar = { abierto = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorFecha(fechaMillis: Long?, onAceptar: (Long) -> Unit, onCerrar: () -> Unit) {
    val hoy = remember { hoyEnUtc(System.currentTimeMillis(), TimeZone.getDefault()) }
    val selector = rememberDatePickerState(
        initialSelectedDateMillis = fechaMillis,
        yearRange = PrimerAnioDelSelector..anioUtc(hoy),
        selectableDates = remember(hoy) {
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = fechaSeleccionable(utcTimeMillis, hoy)
                override fun isSelectableYear(year: Int) = year <= anioUtc(hoy)
            }
        }
    )
    // No se usa DatePickerDialog: no limita su altura y en horizontal deja afuera sus botones. Aquí el
    // calendario se desplaza y los botones quedan siempre visibles.
    Dialog(onDismissRequest = onCerrar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .padding(8.dp)
                .widthIn(max = 360.dp)
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
                    EnEspanol {
                        // El modo por defecto permite escribir la fecha; el criterio exige elegirla.
                        DatePicker(
                            state = selector,
                            showModeToggle = false,
                            title = {
                                Text(
                                    text = stringResource(R.string.crear_fecha_titulo),
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp)
                                )
                            }
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onCerrar) {
                        Text(stringResource(R.string.crear_cancelar), style = MaterialTheme.typography.labelLarge)
                    }
                    TextButton(
                        onClick = { selector.selectedDateMillis?.let(onAceptar) },
                        enabled = selector.selectedDateMillis != null
                    ) {
                        Text(stringResource(R.string.crear_fecha_aceptar), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

/**
 * La app solo existe en español, pero el selector toma el idioma del dispositivo: en un teléfono en
 * inglés mezclaría "Selected date" con los textos propios. El aviso de lint se refiere a la descarga
 * de idiomas de los App Bundle; aquí se entrega un APK con un solo idioma.
 */
@SuppressLint("AppBundleLocaleChanges")
@Composable
private fun EnEspanol(contenido: @Composable () -> Unit) {
    val configuracion = LocalConfiguration.current
    val contexto = LocalContext.current
    if (ConfigurationCompat.getLocales(configuracion)[0]?.language == "es") {
        contenido()
    } else {
        val espanol = remember(configuracion) {
            Configuration(configuracion).apply { setLocale(Locale("es")) }
        }
        CompositionLocalProvider(
            LocalConfiguration provides espanol,
            LocalContext provides contexto.createConfigurationContext(espanol),
            content = contenido
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CampoOpciones(
    valor: String?,
    opciones: List<String>,
    indicacion: String,
    icono: Int,
    conError: Boolean,
    habilitado: Boolean,
    onElegir: (String) -> Unit,
    tag: String
) {
    var abierto by rememberSaveable { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = abierto,
        onExpandedChange = { if (habilitado) abierto = it }
    ) {
        Pildora(
            conError = conError,
            modifier = Modifier
                .menuAnchor()
                .testTag(tag)
        ) {
            IconoCampo(icono)
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (valor != null) {
                    Text(
                        text = valor,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    TextoIndicacion(indicacion)
                }
            }
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        ExposedDropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            opciones.forEach { opcion ->
                DropdownMenuItem(
                    text = { Text(opcion, style = MaterialTheme.typography.bodyLarge) },
                    onClick = {
                        onElegir(opcion)
                        abierto = false
                    }
                )
            }
        }
    }
}

@Composable
private fun Botones(estado: CrearAlbumUiState, acciones: AccionesCrearAlbum) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            onClick = acciones.onCrear,
            enabled = !estado.enviando,
            colors = ButtonDefaults.buttonColors(
                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                disabledContentColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag(TagCrearAlbum)
        ) {
            when (estado.envio) {
                EnvioAlbum.Enviando -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.crear_enviando), style = MaterialTheme.typography.labelLarge)
                }
                EnvioAlbum.Fallido -> {
                    Icon(imageVector = Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.crear_reintentar), style = MaterialTheme.typography.labelLarge)
                }
                else -> {
                    Icon(
                        painter = painterResource(R.drawable.ic_cat_album),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.crear_album), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        Button(
            onClick = acciones.onCancelar,
            enabled = !estado.enviando,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag(TagCancelarCrear)
        ) {
            Text(stringResource(R.string.crear_cancelar), style = MaterialTheme.typography.labelLarge)
        }
    }
}

private val SesionDeEjemplo = Session.Collector(Collector(100, "Manolo Bellon", "manollo@caracol.com.co"))

private val FormularioDeEjemplo = CrearAlbumUiState(
    nombre = "Abbey Road",
    portada = "https://example.com/abbey.jpg",
    descripcion = "Undécimo álbum de estudio de The Beatles.",
    fechaMillis = -8_380_800_000L,
    genero = "Rock",
    disquera = "EMI"
)

@Preview(showBackground = true, heightDp = 1000, name = "Vacío")
@Composable
private fun PreviewVacio() = VinilosTheme {
    PantallaCrearAlbum(CrearAlbumUiState(), SesionDeEjemplo, AccionesCrearAlbum())
}

@Preview(showBackground = true, heightDp = 1200, name = "Con errores")
@Composable
private fun PreviewErrores() = VinilosTheme {
    PantallaCrearAlbum(
        CrearAlbumUiState(
            portada = "ftp://servidor/portada.png",
            errores = CampoAlbum.entries.associateWith {
                if (it == CampoAlbum.Portada) ErrorCampo.UrlInvalida else ErrorCampo.Obligatorio
            }
        ),
        SesionDeEjemplo,
        AccionesCrearAlbum()
    )
}

@Preview(showBackground = true, heightDp = 1000, name = "Enviando")
@Composable
private fun PreviewEnviando() = VinilosTheme {
    PantallaCrearAlbum(FormularioDeEjemplo.copy(envio = EnvioAlbum.Enviando), SesionDeEjemplo, AccionesCrearAlbum())
}

@Preview(showBackground = true, heightDp = 1100, name = "Fallido")
@Composable
private fun PreviewFallido() = VinilosTheme {
    PantallaCrearAlbum(FormularioDeEjemplo.copy(envio = EnvioAlbum.Fallido), SesionDeEjemplo, AccionesCrearAlbum())
}
