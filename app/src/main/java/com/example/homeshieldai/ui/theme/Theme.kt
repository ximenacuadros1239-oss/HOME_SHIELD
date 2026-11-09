package com.example.homeshieldai.ui.theme

import android.app.Activity
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class ModoTema(val etiqueta: String) {
    SISTEMA("Automático"),
    CLARO("Claro"),
    OSCURO("Oscuro")
}

private fun esquema(p: Paleta): ColorScheme {
    val base = if (p.oscura) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = p.menta,
        onPrimary = p.fondo,
        primaryContainer = p.mentaOscuro,
        onPrimaryContainer = p.textoPrincipal,
        secondary = p.info,
        tertiary = p.ambar,
        error = p.peligro,
        background = p.fondo,
        onBackground = p.textoPrincipal,
        surface = p.superficie,
        onSurface = p.textoPrincipal,
        surfaceVariant = p.superficieAlta,
        onSurfaceVariant = p.textoSecundario,
        outline = p.borde
    )
}

@Composable
fun HomeShieldAITheme(
    modo: ModoTema = ModoTema.SISTEMA,
    content: @Composable () -> Unit
) {
    val oscuro = when (modo) {
        ModoTema.SISTEMA -> isSystemInDarkTheme()
        ModoTema.CLARO -> false
        ModoTema.OSCURO -> true
    }
    val paleta = if (oscuro) PaletaOscura else PaletaClara

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            var c = view.context
            while (c is ContextWrapper && c !is Activity) c = c.baseContext
            (c as? Activity)?.window?.let { ventana ->
                val control = WindowCompat.getInsetsController(ventana, view)
                control.isAppearanceLightStatusBars = !oscuro
                control.isAppearanceLightNavigationBars = !oscuro
            }
        }
    }

    CompositionLocalProvider(LocalPaleta provides paleta) {
        MaterialTheme(
            colorScheme = esquema(paleta),
            typography = Typography,
            shapes = Shapes
        ) {
            CompositionLocalProvider(
                LocalContentColor provides paleta.textoPrincipal,
                content = content
            )
        }
    }
}