package com.example.homeshieldai

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import kotlinx.coroutines.delay
import androidx.compose.runtime.compositionLocalOf

/** false si la persona activó «Quitar animaciones» en el sistema. */
@Composable
fun animacionesActivas(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(
            context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f
        ) > 0f
    }
}

fun Modifier.aparecer(indice: Int): Modifier = composed {
    val activas = animacionesActivas()
    val progreso = remember { Animatable(if (activas) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (activas) {
            delay(indice * 70L)
            progreso.animateTo(1f, tween(450))
        }
    }
    graphicsLayer {
        alpha = progreso.value
        translationY = (1f - progreso.value) * 36f
    }
}

@Composable
fun NumeroAnimado(
    valor: Float,
    formato: (Float) -> String,
    estilo: TextStyle,
    color: Color = Color.Unspecified,
    modifier: Modifier = Modifier
) {
    val activas = animacionesActivas()
    val mostrado by animateFloatAsState(valor, tween(if (activas) 600 else 0), label = "numero")
    Text(formato(mostrado), style = estilo, color = color, modifier = modifier)
}

class ContadorAparicion {
    private var n = 0
    fun siguiente(): Int = n++
}

val LocalContadorAparicion = compositionLocalOf<ContadorAparicion?> { null }

/** Cada tarjeta aparece sola, una tras otra, según su orden en la pantalla. */
fun Modifier.aparecerAuto(): Modifier = composed {
    val contador = LocalContadorAparicion.current
    val indice = remember { minOf(contador?.siguiente() ?: 0, 6) }
    val activas = animacionesActivas() && contador != null
    val progreso = remember { Animatable(if (activas) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (activas) {
            delay(indice * 70L)
            progreso.animateTo(1f, tween(450))
        }
    }
    graphicsLayer {
        alpha = progreso.value
        translationY = (1f - progreso.value) * 36f
    }
}