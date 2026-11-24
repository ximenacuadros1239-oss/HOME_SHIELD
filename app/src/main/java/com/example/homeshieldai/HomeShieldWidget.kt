package com.example.homeshieldai

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.glance.unit.ColorProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Row
import androidx.glance.layout.Column
import androidx.glance.layout.Alignment
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding

/** estado: 0 = protegida, 1 = sin conexión, 2 = emergencia */
data class DatosWidget(
    val estado: Int = 0,
    val potencia: Float = 0f,
    val gastoHoy: Float = 0f,
    val tempTablero: Float = 0f,
    val hogar: String = "",
    val ts: Long = 0L
)

private const val ARCHIVO = "widget"

fun guardarDatosWidget(context: Context, d: DatosWidget) {
    context.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE).edit()
        .putInt("estado", d.estado)
        .putFloat("potencia", d.potencia)
        .putFloat("gasto", d.gastoHoy)
        .putFloat("tablero", d.tempTablero)
        .putString("hogar", d.hogar)
        .putLong("ts", d.ts)
        .apply()
}

fun leerDatosWidget(context: Context): DatosWidget {
    val p = context.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)
    return DatosWidget(
        estado = p.getInt("estado", 0),
        potencia = p.getFloat("potencia", 0f),
        gastoHoy = p.getFloat("gasto", 0f),
        tempTablero = p.getFloat("tablero", 0f),
        hogar = p.getString("hogar", "") ?: "",
        ts = p.getLong("ts", 0L)
    )
}

class HomeShieldWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(
        setOf(
            androidx.compose.ui.unit.DpSize(110.dp, 110.dp),
            androidx.compose.ui.unit.DpSize(250.dp, 110.dp)
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val datos = leerDatosWidget(context)
        provideContent { ContenidoWidget(datos) }
    }
}

class HomeShieldWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HomeShieldWidget()
}

@androidx.compose.runtime.Composable
private fun ContenidoWidget(d: DatosWidget) {
    val grande = LocalSize.current.width >= 250.dp
    val hora = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(d.ts))

    val (titulo, subtitulo, fondo) = when {
        d.ts == 0L -> Triple("Home Shield AI", "Abre la app para ver tu casa", Color(0xFF12283A))
        d.estado == 2 -> Triple("PELIGRO", "Corte de emergencia", Color(0xFFB3261E))
        d.estado == 1 -> Triple("Sin conexión", "Revisa el Wi-Fi del módulo", Color(0xFF8A5A00))
        else -> Triple("Casa protegida", "Todo en orden · $hora", Color(0xFF0E6B62))
    }
    val blanco = ColorProvider(day = Color.White, night = Color.White)
    val suave = ColorProvider(day = Color(0xCCFFFFFF), night = Color(0xCCFFFFFF))

    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(fondo)
            .cornerRadius(22.dp)
            .padding(14.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = GlanceModifier.defaultWeight()) {
            Text(
                titulo,
                style = TextStyle(color = blanco, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            )
            Text(subtitulo, style = TextStyle(color = suave, fontSize = 12.sp))
            if (d.hogar.isNotBlank() && d.ts != 0L) {
                Spacer(GlanceModifier.height(4.dp))
                Text(d.hogar, style = TextStyle(color = suave, fontSize = 11.sp))
            }
        }
        if (d.ts != 0L) {
            Dato("Consumo", "%.0f W".format(Locale.US, d.potencia))
            if (grande) {
                Dato("Hoy", "S/ %.2f".format(Locale.US, d.gastoHoy))
                Dato("Tablero", "%.1f °C".format(Locale.US, d.tempTablero))
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun Dato(etiqueta: String, valor: String) {
    Column(
        modifier = GlanceModifier.padding(start = 14.dp),
        horizontalAlignment = Alignment.End
    ) {
        Text(
            valor,
            style = TextStyle(
                color = ColorProvider(day = Color.White, night = Color.White),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Text(
            etiqueta,
            style = TextStyle(
                color = ColorProvider(day = Color(0xCCFFFFFF), night = Color(0xCCFFFFFF)),
                fontSize = 11.sp
            )
        )
    }
}