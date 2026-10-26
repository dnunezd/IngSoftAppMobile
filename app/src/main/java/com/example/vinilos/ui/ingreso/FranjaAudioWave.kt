package com.example.vinilos.ui.ingreso

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vinilos.R
import com.example.vinilos.ui.theme.ChipTexto
import com.example.vinilos.ui.theme.TextoTenue
import com.example.vinilos.ui.theme.VinilosTheme

const val TagFranjaAudioWave = "franja_audio_wave"

/**
 * Franja decorativa del ingreso (marcador "Interactive beat wave marker doodle" del Figma): no tiene
 * datos reales ni responde a toques. La onda es el SVG del diseño, recortado a 200x24.
 */
@Composable
fun FranjaAudioWave(modifier: Modifier = Modifier) {
    val forma = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(forma)
            .background(Color.White.copy(alpha = 0.7f))
            .border(1.dp, Color.Black.copy(alpha = 0.04f), forma)
            .padding(horizontal = 9.dp, vertical = 5.dp)
            .testTag(TagFranjaAudioWave),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.ingreso_audio_wave).uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                lineHeight = 16.5.sp
            ),
            color = ChipTexto
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(24.dp)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Image(
                painter = painterResource(R.drawable.ic_ing_onda),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .widthIn(max = 200.dp)
                    .fillMaxWidth()
                    .height(24.dp)
            )
        }
        Text(
            text = stringResource(R.string.ingreso_bpm),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Normal,
                lineHeight = 16.5.sp,
                letterSpacing = 0.sp
            ),
            color = TextoTenue
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun FranjaAudioWavePreview() {
    VinilosTheme {
        FranjaAudioWave(modifier = Modifier.padding(20.dp))
    }
}
