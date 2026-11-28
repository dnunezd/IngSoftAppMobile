package com.example.vinilos.ui.theme

import androidx.compose.ui.graphics.Color

// Palette taken from the Figma "Vinilos App" (Available Collectors frame).
val Naranja = Color(0xFFFF4D26)
// Orange with sufficient contrast (4.6:1 over `Fondo` and 4.9:1 with white text) for buttons, chips
// and brand text. `Naranja` is left for non-text elements (icons, waves, decoration).
val NaranjaContraste = Color(0xFFD13A12)
val NaranjaOscuro = Color(0xFFAF2900)
val NaranjaSuave = Color(0xFFFCE4DF)
val Fondo = Color(0xFFFCF9F8)
val Superficie = Color(0xFFFFFFFF)
val SuperficieVariante = Color(0xFFF0EDEC)
val Contorno = Color(0xFFEBE7E7)
val TextoPrincipal = Color(0xFF1C1B1B)
val TextoSecundario = Color(0xFF5C5B5E)

// Login screen (Html -> Body -> Main component from Figma).
val ChipFondo = Color(0xFFFFE8E2)
val ChipTexto = Color(0xFFA32F10)
val ChipColeccionistaFondo = Color(0xFFFFEDE9)
val TitularIngreso = Color(0xFF121316)
val TitularGris = Color(0xFF63636B)
val TextoTenue = Color(0xFF63636B)
val FondoImagen = Color(0xFFF4F4F6)

// Form errors (HU07), from `DESIGN.md`.
val ErrorFormulario = Color(0xFFBA1A1A)
val ErrorFormularioFondo = Color(0xFFFFDAD6)
val ErrorFormularioTexto = Color(0xFF93000A)

// Initials avatars: the first one uses blue and the rest use the mockup's gray. Both ends of the
// gradient are dark so the white initials keep 4.5:1 across the whole circle.
val AvatarAzulClaro = Color(0xFF0050CC)
val AvatarAzul = Color(0xFF003E9E)
val AvatarGrisClaro = Color(0xFF5C5B5E)
val AvatarGris = Color(0xFF45444A)

// Artist listing (HU03): "Músico" and "Banda" labels from the mockup.
val EtiquetaBandaFondo = Color(0xFFDDE4FF)
val EtiquetaBandaTexto = Color(0xFF0050CC)

// Disabled main button (completar-accesibilidad-hu08-hu09-hu15): Material's default colors
// (38% text over 12% background) don't reach 4.5:1. This pair gives around 5.1:1.
val BotonDesactivadoFondo = SuperficieVariante
val BotonDesactivadoTexto = TextoTenue
