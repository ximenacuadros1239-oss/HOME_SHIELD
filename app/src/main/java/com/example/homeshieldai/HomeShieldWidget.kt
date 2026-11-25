package com.example.homeshieldai

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** estado: 0 = protegida, 1 = sin conexión, 2 = emergencia */
data class DatosWidget(
    val estado: Int = 0,
    val potencia: Float = 0f,
    val gastoHoy: Float = 0f,
    val tempTablero: Float = 0f,
    val hogar: String = "",
    val ts: Long = 0L,
    val gastoMes: Float = 0f,
    val presupuesto: Float = 0f
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
        .putFloat("gastoMes", d.gastoMes)
        .putFloat("presupuesto", d.presupuesto)
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
        ts = p.getLong("ts", 0L),
        gastoMes = p.getFloat("gastoMes", 0f),
        presupuesto = p.getFloat("presupuesto", 0f)
    )
}

class HomeShieldWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(
        setOf(
            DpSize(110.dp, 110.dp),
            DpSize(250.dp, 110.dp),
            DpSize(250.dp, 200.dp)
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

private fun cp(c: Color) = ColorProvider(day = c, night = c)

@Composable
private fun ContenidoWidget(d: DatosWidget) {
    val context = LocalContext.current
    val tam = LocalSize.current
    val ancho = tam.width >= 250.dp
    val alto = tam.height >= 180.dp
    val hora = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(d.ts))
    val viejo = d.ts != 0L && System.currentTimeMillis() - d.ts > 15 * 60_000L

    val (titulo, subtitulo, fondo) = when {
        d.ts == 0L -> Triple("Home Shield AI", "Abre la app para ver tu casa", Color(0xFF12283A))
        d.estado == 2 -> Triple("PELIGRO", "Corte de emergencia", Color(0xFFB3261E))
        d.estado == 1 -> Triple("Sin conexión", "Revisa el Wi-Fi del módulo", Color(0xFF8A5A00))
        viejo -> Triple("Sin actualizar", "Última lectura a las $hora", Color(0xFF8A5A00))
        else -> Triple("Casa protegida", "Todo en orden · $hora", Color(0xFF0E6B62))
    }
    val blanco = cp(Color.White)
    val suave = cp(Color(0xCCFFFFFF))
    val razon = if (d.presupuesto > 0f) d.gastoMes / d.presupuesto else 0f

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(cp(fondo))
            .cornerRadius(22.dp)
            .padding(14.dp)
            .clickable(actionStartActivity(Intent(context, MainActivity::class.java)))
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    titulo,
                    style = TextStyle(color = blanco, fontSize = 17.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1
                )
                Text(subtitulo, style = TextStyle(color = suave, fontSize = 12.sp), maxLines = 2)
                if (d.hogar.isNotBlank() && d.ts != 0L) {
                    Spacer(GlanceModifier.height(4.dp))
                    Text(d.hogar, style = TextStyle(color = suave, fontSize = 11.sp), maxLines = 1)
                }
            }
            if (d.ts != 0L) {
                Dato("Consumo", "%.0f W".format(Locale.US, d.potencia))
                if (ancho) {
                    Dato("Hoy", "S/ %.2f".format(Locale.US, d.gastoHoy))
                    Dato("Tablero", "%.1f °C".format(Locale.US, d.tempTablero))
                }
            }
        }

        if (alto && d.ts != 0L && d.presupuesto > 0f) {
            Spacer(GlanceModifier.height(14.dp))
            Text(
                "Presupuesto del mes · %.0f %%".format(Locale.US, razon * 100),
                style = TextStyle(color = blanco, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            )
            Spacer(GlanceModifier.height(6.dp))
            LinearProgressIndicator(
                progress = razon.coerceIn(0f, 1f),
                modifier = GlanceModifier.fillMaxWidth(),
                color = cp(Color.White),
                backgroundColor = cp(Color(0x33FFFFFF))
            )
            Spacer(GlanceModifier.height(6.dp))
            Text(
                if (razon >= 1f)
                    "Te pasaste S/ %.2f".format(Locale.US, d.gastoMes - d.presupuesto)
                else
                    "Te quedan S/ %.2f de S/ %.0f".format(
                        Locale.US, d.presupuesto - d.gastoMes, d.presupuesto
                    ),
                style = TextStyle(color = suave, fontSize = 12.sp)
            )
        }
    }
}

@Composable
private fun Dato(etiqueta: String, valor: String) {
    Column(
        modifier = GlanceModifier.padding(start = 14.dp),
        horizontalAlignment = Alignment.End
    ) {
        Text(
            valor,
            style = TextStyle(color = cp(Color.White), fontSize = 16.sp, fontWeight = FontWeight.Bold)
        )
        Text(
            etiqueta,
            style = TextStyle(color = cp(Color(0xCCFFFFFF)), fontSize = 11.sp)
        )
    }
}