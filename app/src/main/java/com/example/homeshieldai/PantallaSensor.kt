package com.example.homeshieldai

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.homeshieldai.ui.theme.*
import java.util.Locale

@Composable
fun PantallaSensor(estado: EstadoSistema, id: String, onVolver: () -> Unit) {
    val titulo = when (id) {
        "tablero" -> "Tablero eléctrico"
        "consumo" -> "Consumo en vivo"
        else -> "Sala"
    }
    val unidad = if (id == "consumo") "W" else "°C"
    val formato = if (id == "consumo") "%.0f" else "%.1f"
    val valor = when (id) {
        "tablero" -> estado.tempTablero
        "consumo" -> estado.potencia
        else -> estado.tempSala
    }
    val datos = when (id) {
        "tablero" -> estado.histTablero.toList()
        "consumo" -> estado.histPotencia.toList()
        else -> estado.histSala.toList()
    }
    val limite: Float? = if (id == "tablero") estado.tempAlerta else null
    val (textoEstado, colorEstado) = when (id) {
        "consumo" ->
            if (estado.emergencia) "Relé apagado" to Peligro else "Normal" to Menta
        else -> estadoTemp(valor, estado.tempAlerta)
    }

    val minimo = datos.minOrNull() ?: 0f
    val maximo = datos.maxOrNull() ?: 0f
    val promedio = if (datos.isNotEmpty()) datos.sum() / datos.size else 0f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onVolver) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = TextoPrincipal)
            }
            Text(titulo, style = MaterialTheme.typography.titleLarge)
        }

        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    formato.format(Locale.US, valor),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = androidx.compose.ui.unit.TextUnit(56f, androidx.compose.ui.unit.TextUnitType.Sp)
                    )
                )
                Text(
                    " $unidad",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextoSecundario,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }
            EstadoPunto(textoEstado, colorEstado)
        }

        TarjetaVidrio(modifier = Modifier.fillMaxWidth()) {
            Text("Últimos 2 minutos", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            GraficoLinea(datos, limite, colorEstado)
            Spacer(Modifier.height(8.dp))
            Text(
                "Se actualiza cada 2 segundos.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextoSecundario
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DatoEstadistico("Mínimo", formato.format(Locale.US, minimo), unidad, Modifier.weight(1f))
            DatoEstadistico("Promedio", formato.format(Locale.US, promedio), unidad, Modifier.weight(1f))
            DatoEstadistico("Máximo", formato.format(Locale.US, maximo), unidad, Modifier.weight(1f))
        }

        if (limite != null) {
            TarjetaVidrio(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Temperatura de alerta",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "%.0f °C".format(Locale.US, limite),
                        style = MaterialTheme.typography.titleMedium,
                        color = Ambar
                    )
                }
                Text(
                    "Puedes cambiarla en Ajustes. La línea punteada del gráfico marca este límite.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextoSecundario
                )
            }
        }
    }
}

@Composable
fun DatoEstadistico(titulo: String, valor: String, unidad: String, modifier: Modifier = Modifier) {
    TarjetaVidrio(modifier = modifier) {
        Text(titulo, style = MaterialTheme.typography.bodyMedium, color = TextoSecundario)
        Spacer(Modifier.height(4.dp))
        Text("$valor $unidad", style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
fun GraficoLinea(puntos: List<Float>, limite: Float?, color: Color) {
    val colorLimite = Ambar
    val colorGuia = Borde

    Canvas(modifier = Modifier.fillMaxWidth().height(160.dp)) {
        if (puntos.size < 2) return@Canvas

        var minimo = puntos.min()
        var maximo = puntos.max()
        if (limite != null) {
            minimo = minOf(minimo, limite)
            maximo = maxOf(maximo, limite)
        }
        val rango = (maximo - minimo).coerceAtLeast(0.5f)
        minimo -= rango * 0.1f
        maximo += rango * 0.1f
        val total = maximo - minimo

        fun y(v: Float) = size.height - (v - minimo) / total * size.height
        val paso = size.width / (puntos.size - 1)

        for (i in 0..3) {
            val yy = size.height * i / 3f
            drawLine(colorGuia, Offset(0f, yy), Offset(size.width, yy), strokeWidth = 1f)
        }

        val linea = Path().apply {
            moveTo(0f, y(puntos[0]))
            for (i in 1 until puntos.size) lineTo(i * paso, y(puntos[i]))
        }
        val relleno = Path().apply {
            addPath(linea)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(
            relleno,
            Brush.verticalGradient(listOf(color.copy(alpha = 0.35f), Color.Transparent))
        )
        drawPath(
            linea, color,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        if (limite != null) {
            drawLine(
                colorLimite, Offset(0f, y(limite)), Offset(size.width, y(limite)),
                strokeWidth = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))
            )
        }
    }
}