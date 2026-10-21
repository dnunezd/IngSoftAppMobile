package com.example.vinilos.ui.inicio

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vinilos.R
import com.example.vinilos.data.model.Session
import com.example.vinilos.ui.ingreso.ChipEtiqueta
import com.example.vinilos.ui.ingreso.EncabezadoVinilo
import com.example.vinilos.ui.theme.VinilosTheme

const val TagInicio = "inicio"
const val TagVerAlbumes = "ver_albumes"
const val TagVerArtistas = "ver_artistas"

/**
 * Pantalla de inicio (secciones): a ella llegan ambos roles tras ingresar y desde aquí se elige
 * entre el catálogo de álbumes y el listado de artistas. No tiene estado ni consulta datos.
 */
@Composable
fun PantallaInicio(
    session: Session,
    onVerAlbumes: () -> Unit,
    onVerArtistas: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .testTag(TagInicio)
    ) {
        EncabezadoVinilo(modifier = Modifier.padding(horizontal = 20.dp), marca = "")
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            TitularInicio(session)
            TarjetaSeccion(
                etiqueta = stringResource(R.string.inicio_albumes_etiqueta),
                titulo = stringResource(R.string.inicio_albumes_titulo),
                descripcion = stringResource(R.string.inicio_albumes_descripcion),
                boton = stringResource(R.string.inicio_albumes_boton),
                tagBoton = TagVerAlbumes,
                brillo = Color(0x4DFFDBD2),
                icono = R.drawable.inicio_icono_albumes,
                ilustracion = R.drawable.inicio_albumes,
                onClick = onVerAlbumes
            )
            TarjetaSeccion(
                etiqueta = stringResource(R.string.inicio_artistas_etiqueta),
                titulo = stringResource(R.string.inicio_artistas_titulo),
                descripcion = stringResource(R.string.inicio_artistas_descripcion),
                boton = stringResource(R.string.inicio_artistas_boton),
                tagBoton = TagVerArtistas,
                brillo = Color(0x66DAE1FF),
                icono = R.drawable.inicio_icono_artistas,
                ilustracion = R.drawable.inicio_artistas,
                onClick = onVerArtistas
            )
        }
    }
}

@Composable
private fun TitularInicio(session: Session) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_ini_estrella),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(R.string.inicio_chip).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Image(
                    painter = painterResource(R.drawable.ic_ini_nota1),
                    contentDescription = null,
                    modifier = Modifier.size(width = 12.dp, height = 18.dp)
                )
                Image(
                    painter = painterResource(R.drawable.ic_ini_nota2),
                    contentDescription = null,
                    modifier = Modifier.size(width = 12.dp, height = 18.dp)
                )
            }
        }
        Text(
            text = when (session) {
                is Session.Collector -> stringResource(R.string.inicio_saludo_nombre, session.collector.name)
                else -> stringResource(R.string.inicio_saludo)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.inicio_titular),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = stringResource(R.string.inicio_subtitulo),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TarjetaSeccion(
    etiqueta: String,
    titulo: String,
    descripcion: String,
    boton: String,
    tagBoton: String,
    brillo: Color,
    @DrawableRes icono: Int,
    @DrawableRes ilustracion: Int,
    onClick: () -> Unit
) {
    val forma = RoundedCornerShape(40.dp)
    ElevatedCard(
        shape = forma,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.clip(forma)) {
            // Resplandor suave en la esquina superior derecha, como en el mockup.
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 40.dp, y = (-40).dp)
                    .size(160.dp)
                    .background(Brush.radialGradient(listOf(brillo, Color.Transparent)), CircleShape)
            )
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ChipEtiqueta(
                        texto = etiqueta,
                        fondo = Color(0x1AAF2900),
                        contenido = MaterialTheme.colorScheme.secondary
                    )
                    Image(
                        painter = painterResource(icono),
                        contentDescription = null,
                        // El PNG exportado trae esquinas blancas: se recorta en círculo.
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = titulo,
                        style = MaterialTheme.typography.titleLarge.copy(letterSpacing = (-0.2).sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = descripcion,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Image(
                    painter = painterResource(ilustracion),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                )
                BotonSeccion(texto = boton, tag = tagBoton, onClick = onClick)
            }
        }
    }
}

@Composable
private fun BotonSeccion(texto: String, tag: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        contentPadding = PaddingValues(start = 24.dp, end = 11.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = texto, style = MaterialTheme.typography.labelLarge)
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 1100)
@Composable
private fun PantallaInicioPreview() {
    VinilosTheme {
        PantallaInicio(session = Session.Visitor, onVerAlbumes = {}, onVerArtistas = {})
    }
}
