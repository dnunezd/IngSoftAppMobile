package com.example.vinilos.ui.ingreso

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
import com.example.vinilos.ui.theme.ChipColeccionistaFondo
import com.example.vinilos.ui.theme.ChipFondo
import com.example.vinilos.ui.theme.ChipTexto
import com.example.vinilos.ui.theme.FondoImagen
import com.example.vinilos.ui.theme.Naranja
import com.example.vinilos.ui.theme.NaranjaOscuro
import com.example.vinilos.ui.theme.TextoTenue
import com.example.vinilos.ui.theme.TitularGris
import com.example.vinilos.ui.theme.TitularIngreso
import com.example.vinilos.ui.theme.VinilosTheme

const val TagEntrarVisitante = "entrar_visitante"
const val TagEntrarColeccionista = "entrar_coleccionista"

private val BordeChip = Color(0xFFFFB5A3)

/**
 * Pantalla de ingreso (componente `Html → Body → Main` del Figma): el usuario elige entrar como
 * Visitante o como Coleccionista. [ilustracionVisitante] e [ilustracionColeccionista] son las
 * imágenes de las tarjetas.
 */
@Composable
fun PantallaIngreso(
    onVisitante: () -> Unit,
    onColeccionista: () -> Unit,
    modifier: Modifier = Modifier,
    @DrawableRes ilustracionVisitante: Int = R.drawable.ingreso_visitante,
    @DrawableRes ilustracionColeccionista: Int = R.drawable.ingreso_coleccionista
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        EncabezadoIngreso()
        SeccionTitular()
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            TarjetaPerfil(
                nombre = stringResource(R.string.ingreso_visitante),
                etiqueta = stringResource(R.string.ingreso_visitante_etiqueta),
                etiquetaFondo = FondoImagen,
                etiquetaTexto = TextoTenue,
                etiquetaPeso = FontWeight.Normal,
                chip = stringResource(R.string.ingreso_visitante_chip),
                chipIcono = R.drawable.ic_ing_audifonos,
                chipFondo = Color.White.copy(alpha = 0.9f),
                chipBorde = Color.Black.copy(alpha = 0.05f),
                chipTexto = TextoTenue,
                bordeTarjeta = Naranja.copy(alpha = 0.15f),
                fondoImagen = FondoImagen,
                alturaImagen = 176.dp,
                ilustracion = ilustracionVisitante,
                alineacionIlustracion = BiasAlignment(horizontalBias = 0f, verticalBias = -0.5f),
                tagEntrar = TagEntrarVisitante,
                onEntrar = onVisitante
            )
            Box {
                TarjetaPerfil(
                    nombre = stringResource(R.string.ingreso_coleccionista),
                    etiqueta = stringResource(R.string.ingreso_coleccionista_etiqueta),
                    etiquetaFondo = ChipColeccionistaFondo,
                    etiquetaTexto = ChipTexto,
                    etiquetaPeso = FontWeight.Bold,
                    chip = stringResource(R.string.ingreso_coleccionista_chip),
                    chipIcono = R.drawable.ic_ing_target,
                    chipFondo = Color(0xF2FFF0EC),
                    chipBorde = BordeChip.copy(alpha = 0.4f),
                    chipTexto = ChipTexto,
                    bordeTarjeta = Naranja.copy(alpha = 0.2f),
                    fondoImagen = ChipColeccionistaFondo,
                    alturaImagen = 192.dp,
                    ilustracion = ilustracionColeccionista,
                    // La figura está de pie: se recorta desde arriba para que se vea la cara y el disco.
                    alineacionIlustracion = Alignment.TopCenter,
                    tagEntrar = TagEntrarColeccionista,
                    onEntrar = onColeccionista
                )
                // Estrella "sticker" que sobresale de la esquina inferior derecha de la tarjeta.
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 11.dp, y = 15.dp)
                        .size(36.dp)
                        .rotate(12f),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_ing_estrella_sticker),
                        contentDescription = null,
                        modifier = Modifier.size(width = 28.5.dp, height = 28.2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EncabezadoIngreso() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .shadow(1.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White)
                .padding(4.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.ic_ing_logo),
                contentDescription = stringResource(R.string.logo_descripcion),
                modifier = Modifier.fillMaxSize()
            )
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = stringResource(R.string.marca),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 26.sp,
                    lineHeight = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.65).sp
                ),
                color = TitularIngreso
            )
            Box(
                modifier = Modifier
                    .padding(start = 2.dp, bottom = 8.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(NaranjaOscuro)
            )
        }
    }
}

/** Titular "Make Endless Beats, Sound Amazing" con sus decoraciones (todas SVG del Figma). */
@Composable
private fun SeccionTitular() {
    val titular = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 38.88.sp,
        letterSpacing = (-1.08).sp
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(233.dp)
    ) {
        Column(modifier = Modifier.offset(y = 8.dp)) {
            Text(text = stringResource(R.string.ingreso_hero_1), style = titular, color = TitularIngreso)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = stringResource(R.string.ingreso_hero_2), style = titular, color = TitularGris)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.ingreso_hero_pill),
                    style = titular.copy(fontSize = 28.sp, lineHeight = 30.24.sp),
                    color = ChipTexto,
                    modifier = Modifier
                        .rotate(-2f)
                        .clip(RoundedCornerShape(32.dp))
                        .background(ChipFondo)
                        .border(1.dp, BordeChip.copy(alpha = 0.3f), RoundedCornerShape(32.dp))
                        .padding(start = 11.dp, end = 11.dp, top = 2.dp, bottom = 3.dp)
                )
            }
        }
        Text(
            text = stringResource(R.string.ingreso_hero_3),
            style = titular.copy(fontSize = 30.sp, lineHeight = 33.sp, letterSpacing = (-0.6).sp),
            color = TitularIngreso,
            modifier = Modifier.offset(y = 96.5.dp)
        )
        Text(
            text = stringResource(R.string.ingreso_subtitulo),
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp, lineHeight = 22.75.sp),
            color = TextoTenue,
            modifier = Modifier
                .offset(y = 135.5.dp)
                .padding(end = 42.dp)
        )
        FranjaAudioWave(modifier = Modifier.offset(y = 199.dp))

        // Decoraciones (SVG del Figma): chispas sobre "Make", corcheas y la etiqueta "Beats & Sound".
        Image(
            painter = painterResource(R.drawable.ic_ing_doodle_make),
            contentDescription = null,
            modifier = Modifier
                .offset(x = 24.dp, y = (-8).dp)
                .size(32.dp)
        )
        Image(
            painter = painterResource(R.drawable.ic_ing_nota_beats),
            contentDescription = null,
            modifier = Modifier
                .offset(x = (-14).dp, y = 38.dp)
                .rotate(-12f)
                .size(24.dp)
        )
        Image(
            painter = painterResource(R.drawable.ic_ing_notas_grandes),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-45).dp, y = 53.dp)
                .rotate(12f)
                .size(28.dp)
        )
        Image(
            painter = painterResource(R.drawable.ic_ing_nota_chica),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-15).dp, y = 95.dp)
                .rotate(6f)
                .size(20.dp)
        )
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 5.dp, y = (-23).dp)
                .rotate(6f)
                .shadow(1.dp, CircleShape)
                .clip(CircleShape)
                .background(ChipFondo)
                .border(1.dp, BordeChip.copy(alpha = 0.4f), CircleShape)
                .padding(horizontal = 15.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.ic_ing_chip_nota),
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = stringResource(R.string.ingreso_chip_superior),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.3.sp
                ),
                color = ChipTexto
            )
        }
    }
}

@Composable
private fun TarjetaPerfil(
    nombre: String,
    etiqueta: String,
    etiquetaFondo: Color,
    etiquetaTexto: Color,
    etiquetaPeso: FontWeight,
    chip: String,
    @DrawableRes chipIcono: Int,
    chipFondo: Color,
    chipBorde: Color,
    chipTexto: Color,
    bordeTarjeta: Color,
    fondoImagen: Color,
    alturaImagen: androidx.compose.ui.unit.Dp,
    @DrawableRes ilustracion: Int,
    alineacionIlustracion: Alignment,
    tagEntrar: String,
    onEntrar: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, bordeTarjeta),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(21.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(alturaImagen)
                    .clip(RoundedCornerShape(16.dp))
                    .background(fondoImagen)
            ) {
                Image(
                    painter = painterResource(ilustracion),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alignment = alineacionIlustracion,
                    modifier = Modifier.fillMaxSize()
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .clip(CircleShape)
                        .background(chipFondo)
                        .border(1.dp, chipBorde, CircleShape)
                        .padding(horizontal = 11.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Image(painter = painterResource(chipIcono), contentDescription = null, modifier = Modifier.size(10.dp))
                    Text(
                        text = chip.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 16.5.sp,
                            letterSpacing = 0.275.sp
                        ),
                        color = chipTexto
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = nombre,
                        style = TextStyle(
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            lineHeight = 30.sp,
                            letterSpacing = (-0.5).sp
                        ),
                        color = TitularIngreso
                    )
                    Text(
                        text = etiqueta,
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = etiquetaPeso,
                            fontSize = 11.sp,
                            lineHeight = 16.5.sp
                        ),
                        color = etiquetaTexto,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(etiquetaFondo)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = onEntrar,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(containerColor = Naranja),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                    modifier = Modifier.testTag(tagEntrar)
                ) {
                    Text(
                        text = stringResource(R.string.ingreso_entrar),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            lineHeight = 21.sp,
                            letterSpacing = 0.35.sp
                        )
                    )
                    Spacer(Modifier.width(8.dp))
                    Image(
                        painter = painterResource(R.drawable.ic_ing_flecha),
                        contentDescription = null,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 1100)
@Composable
private fun PantallaIngresoPreview() {
    VinilosTheme {
        PantallaIngreso(onVisitante = {}, onColeccionista = {})
    }
}
