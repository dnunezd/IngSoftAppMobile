package com.example.vinilos.ui.ingreso

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vinilos.R
import com.example.vinilos.ui.theme.Naranja
import com.example.vinilos.ui.theme.TextoPrincipal
import com.example.vinilos.ui.theme.VinilosTheme

const val TagFranjaAudioFeed = "franja_audio_feed"

/** Altura (dp) y opacidad de cada barra de las ondas del Figma. */
private val BarrasIzquierda = listOf(
    8 to 1f, 16 to 1f, 12 to 0.8f, 4 to 0.45f, 14 to 1f, 8 to 0.7f, 16 to 1f,
    10 to 0.8f, 6 to 0.6f, 12 to 1f, 8 to 0.7f, 16 to 1f, 8 to 0.8f, 4 to 0.45f
)
private val BarrasDerecha = listOf(
    4 to 0.45f, 10 to 0.8f, 16 to 1f, 8 to 0.7f, 14 to 1f, 8 to 1f, 16 to 1f, 6 to 0.6f, 12 to 1f, 4 to 0.45f
)

/** Cabecera del selector: etiquetas, titular "¿Quién eres?", franja AUDIO FEED y subtítulo. */
@Composable
fun TitularColeccionistas() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.fillMaxWidth().height(106.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FilaEtiquetas()
                FilaTitular()
            }
            DoodlesColeccionistas(modifier = Modifier.align(Alignment.TopEnd))
            FranjaAudioFeed(modifier = Modifier.align(Alignment.BottomStart))
        }
        Text(
            text = stringResource(R.string.coleccionistas_subtitulo),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FilaEtiquetas() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(Naranja.copy(alpha = 0.12f))
                .border(1.dp, Naranja.copy(alpha = 0.25f), CircleShape)
                .padding(horizontal = 13.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.ic_sel_chip_nota),
                contentDescription = null,
                modifier = Modifier.size(width = 7.dp, height = 10.5.dp)
            )
            Text(
                text = stringResource(R.string.coleccionistas_chip_curado).uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.275.sp),
                color = Naranja
            )
        }
        Row(
            modifier = Modifier
                .rotate(-3f)
                .shadow(3.dp, CircleShape, spotColor = Naranja)
                .clip(CircleShape)
                .background(Naranja)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.ic_sel_explore),
                contentDescription = null,
                modifier = Modifier.size(width = 10.5.dp, height = 11.7.dp)
            )
            Text(
                text = stringResource(R.string.coleccionistas_chip_explora),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp),
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
private fun FilaTitular() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.coleccionistas_titulo_1),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = stringResource(R.string.coleccionistas_titulo_2),
            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 30.sp, lineHeight = 37.5.sp, letterSpacing = (-0.75).sp),
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .rotate(-2f)
                .shadow(4.dp, RoundedCornerShape(16.dp), spotColor = Naranja)
                .clip(RoundedCornerShape(16.dp))
                .background(Naranja)
                .padding(horizontal = 12.dp, vertical = 2.dp)
        )
    }
}

/** Franja decorativa "AUDIO FEED" entre dos ondas de barras; no responde a toques. */
@Composable
fun FranjaAudioFeed(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(4.dp)
            .testTag(TagFranjaAudioFeed),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OndaDeBarras(barras = BarrasIzquierda, alFinal = false, modifier = Modifier.weight(1f))
        Text(
            text = stringResource(R.string.coleccionistas_audio_feed).uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(lineHeight = 16.5.sp),
            color = Naranja
        )
        OndaDeBarras(barras = BarrasDerecha, alFinal = true, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun OndaDeBarras(barras: List<Pair<Int, Float>>, alFinal: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(16.dp)
            .clipToBounds(),
        horizontalArrangement = Arrangement.spacedBy(4.dp, if (alFinal) Alignment.End else Alignment.Start),
        verticalAlignment = Alignment.CenterVertically
    ) {
        barras.forEach { (altura, opacidad) ->
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(altura.dp)
                    .alpha(opacidad)
                    .clip(CircleShape)
                    .background(Naranja)
            )
        }
    }
}

private val Serif = FontFamily.Serif

/** Racimo de notas y estrella en la esquina superior derecha: SVG del Figma y glifos de texto. */
@Composable
private fun DoodlesColeccionistas(modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(width = 112.dp, height = 64.dp)) {
        Text(
            text = "♪",
            style = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 22.sp),
            color = TextoPrincipal,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-5).dp)
                .rotate(12f)
        )
        Text(
            text = "♫",
            style = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 18.sp),
            color = Naranja,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-37).dp, y = 15.dp)
                .rotate(-12f)
        )
        Image(
            painter = painterResource(R.drawable.ic_sel_nota_pequena),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-12).dp, y = (-4).dp)
                .size(width = 9.7.dp, height = 11.7.dp)
        )
        Text(
            text = "𝄞",
            style = TextStyle(fontFamily = Serif, fontSize = 14.sp),
            color = TextoPrincipal,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-48).dp, y = (-8).dp)
                .alpha(0.75f)
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-23).dp, y = (-9).dp)
                .size(34.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .rotate(45f)
                    .shadow(2.dp, RoundedCornerShape(6.dp), spotColor = Naranja)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Naranja),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_sel_estrella),
                    contentDescription = null,
                    modifier = Modifier
                        .rotate(-45f)
                        .size(width = 10.5.dp, height = 10.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun TitularColeccionistasPreview() {
    VinilosTheme {
        Box(modifier = Modifier.padding(20.dp)) { TitularColeccionistas() }
    }
}

/** Dos notas de texto que asoman sobre el extremo derecho del campo de búsqueda. */
@Composable
fun NotasDelBuscador(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.offset(x = (-24).dp, y = (-12).dp),
        horizontalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        Text(
            text = "♪",
            style = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 15.sp),
            color = Naranja,
            modifier = Modifier.rotate(-12f)
        )
        Text(
            text = "♫",
            style = TextStyle(fontFamily = Serif, fontWeight = FontWeight.Bold, fontSize = 13.sp),
            color = TextoPrincipal,
            modifier = Modifier.rotate(6f)
        )
    }
}
