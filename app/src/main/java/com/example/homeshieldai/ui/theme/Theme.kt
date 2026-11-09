package com.example.homeshieldai.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = Menta,
    onPrimary = Fondo,
    primaryContainer = MentaOscuro,
    onPrimaryContainer = TextoPrincipal,
    secondary = Info,
    tertiary = Ambar,
    error = Peligro,
    background = Fondo,
    onBackground = TextoPrincipal,
    surface = Superficie,
    onSurface = TextoPrincipal,
    surfaceVariant = SuperficieAlta,
    onSurfaceVariant = TextoSecundario,
    outline = Borde
)

@Composable
fun HomeShieldAITheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}