package com.example.vinilos.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// El mockup de Vinilos solo existe en claro, así que el tema es siempre claro y de marca
// (sin color dinámico ni esquema oscuro).
private val EsquemaClaro = lightColorScheme(
    primary = Naranja,
    onPrimary = Superficie,
    primaryContainer = NaranjaSuave,
    onPrimaryContainer = NaranjaOscuro,
    secondary = NaranjaOscuro,
    onSecondary = Superficie,
    tertiary = NaranjaOscuro,
    background = Fondo,
    onBackground = TextoPrincipal,
    surface = Superficie,
    surfaceTint = Superficie,
    onSurface = TextoPrincipal,
    surfaceVariant = SuperficieVariante,
    onSurfaceVariant = TextoSecundario,
    outline = Contorno
)

@Composable
fun VinilosTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EsquemaClaro,
        typography = Typography,
        content = content
    )
}
