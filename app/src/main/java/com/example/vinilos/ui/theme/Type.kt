package com.example.vinilos.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// El mockup usa Inter; hasta empaquetarla se usa la fuente sans-serif del sistema con los
// mismos pesos, tamaños e interlineados.
private val Familia = FontFamily.SansSerif

val Typography = Typography(
    headlineLarge = TextStyle(
        fontFamily = Familia, fontWeight = FontWeight.ExtraBold,
        fontSize = 34.sp, lineHeight = 40.sp, letterSpacing = (-0.85).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = Familia, fontWeight = FontWeight.ExtraBold,
        fontSize = 26.sp, lineHeight = 32.sp, letterSpacing = (-0.65).sp
    ),
    titleLarge = TextStyle(
        fontFamily = Familia, fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = (-0.5).sp
    ),
    titleMedium = TextStyle(
        fontFamily = Familia, fontWeight = FontWeight.Bold,
        fontSize = 15.sp, lineHeight = 21.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = Familia, fontWeight = FontWeight.Normal,
        fontSize = 15.sp, lineHeight = 21.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Familia, fontWeight = FontWeight.Normal,
        fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 0.13.sp
    ),
    labelLarge = TextStyle(
        fontFamily = Familia, fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp, lineHeight = 18.sp, letterSpacing = 0.14.sp
    ),
    labelSmall = TextStyle(
        fontFamily = Familia, fontWeight = FontWeight.Bold,
        fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.55.sp
    )
)
