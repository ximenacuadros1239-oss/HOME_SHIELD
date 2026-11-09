package com.example.homeshieldai.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class Paleta(
    val fondo: Color,
    val fondoAlto: Color,
    val superficie: Color,
    val superficieAlta: Color,
    val menta: Color,
    val mentaOscuro: Color,
    val ambar: Color,
    val peligro: Color,
    val info: Color,
    val textoPrincipal: Color,
    val textoSecundario: Color,
    val borde: Color,
    val oscura: Boolean
)

val PaletaOscura = Paleta(
    fondo = Color(0xFF0A1624),
    fondoAlto = Color(0xFF0E2236),
    superficie = Color(0xFF12283A),
    superficieAlta = Color(0xFF173246),
    menta = Color(0xFF2DE0C0),
    mentaOscuro = Color(0xFF0E6B62),
    ambar = Color(0xFFF2B33D),
    peligro = Color(0xFFE04A55),
    info = Color(0xFF3C8DDE),
    textoPrincipal = Color(0xFFEAF4F6),
    textoSecundario = Color(0xFF8FA6B2),
    borde = Color(0x33FFFFFF),
    oscura = true
)

val PaletaClara = Paleta(
    fondo = Color(0xFFF3F8F8),
    fondoAlto = Color(0xFFDDEEEC),
    superficie = Color(0xFFFFFFFF),
    superficieAlta = Color(0xFFE9F2F3),
    menta = Color(0xFF0B7F70),
    mentaOscuro = Color(0xFFC6EAE3),
    ambar = Color(0xFFB26A00),
    peligro = Color(0xFFC62828),
    info = Color(0xFF1565C0),
    textoPrincipal = Color(0xFF10222B),
    textoSecundario = Color(0xFF51656F),
    borde = Color(0x2210222B),
    oscura = false
)

val LocalPaleta = staticCompositionLocalOf { PaletaOscura }

val Fondo: Color @Composable get() = LocalPaleta.current.fondo
val FondoAlto: Color @Composable get() = LocalPaleta.current.fondoAlto
val Superficie: Color @Composable get() = LocalPaleta.current.superficie
val SuperficieAlta: Color @Composable get() = LocalPaleta.current.superficieAlta
val Menta: Color @Composable get() = LocalPaleta.current.menta
val MentaOscuro: Color @Composable get() = LocalPaleta.current.mentaOscuro
val Ambar: Color @Composable get() = LocalPaleta.current.ambar
val Peligro: Color @Composable get() = LocalPaleta.current.peligro
val Info: Color @Composable get() = LocalPaleta.current.info
val TextoPrincipal: Color @Composable get() = LocalPaleta.current.textoPrincipal
val TextoSecundario: Color @Composable get() = LocalPaleta.current.textoSecundario
val Borde: Color @Composable get() = LocalPaleta.current.borde