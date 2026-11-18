package com.example.homeshieldai

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.homeshieldai.ui.theme.*
import java.util.Locale

data class Periodo(
    val nombre: String,
    val tituloGrafico: String,
    val etiquetas: List<String>,
    val valores: List<Double>
)

val periodos = listOf(
    Periodo("Día", "Gasto por horas (S/)",
        listOf("0h", "4h", "8h", "12h", "16h", "20h"),
        listOf(0.1, 0.2, 0.4, 0.6, 0.5, 0.3)),
    Periodo("Semana", "Gasto diario (S/)",
        listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"),
        listOf(1.8, 2.0, 2.4, 2.1, 1.7, 2.9, 2.3)),
    Periodo("Mes", "Gasto semanal (S/)",
        listOf("Sem 1", "Sem 2", "Sem 3", "Sem 4"),
        listOf(15.9, 16.4, 16.1, 16.6))
)

private fun soles(v: Float) = "S/ " + "%.2f".format(Locale.US, v)

@Composable
fun PantallaConsumo(estado: EstadoSistema) {
    var seleccionado by remember { mutableIntStateOf(2) }
    val periodo = periodos[seleccionado]

    val gasto = when (seleccionado) {
        0 -> estado.gastoHoy.toDouble()
        1 -> estado.gastoMes * 7.0 / 30.0
        else -> estado.gastoMes.toDouble()
    }
    val limite = when (seleccionado) {
        0 -> estado.presupuesto / 30.0
        1 -> estado.presupuesto * 7.0 / 30.0
        else -> estado.presupuesto.toDouble()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CabeceraPantalla("Consumo", "El ahorro de tu hogar", estado)
        Pestanas(seleccionado) { seleccionado = it }
        DialPresupuesto(gasto, limite)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MiniDato(Icons.Filled.Bolt, "%.1f kWh".format(Locale.US, estado.energiaMes),
                "Este mes", Modifier.weight(1f))
            MiniDato(Icons.Filled.CalendarToday, soles(estado.gastoHoy), "Hoy", Modifier.weight(1f))
            MiniDato(Icons.Filled.CalendarMonth, "S/ 1.95", "Ayer", Modifier.weight(1f))
        }

        GraficoBarras(periodo.tituloGrafico, periodo.etiquetas, periodo.valores)

        // ----- Detalle del costo del mes -----
        TarjetaVidrio(modifier = Modifier.fillMaxWidth()) {
            Text("Detalle del costo del mes", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            val energia = estado.costoEnergia(estado.energiaMes)
            val subtotal = energia + estado.cargoFijo + estado.alumbrado
            FilaCosto(
                "Energía (%.1f kWh × S/ %.3f)".format(Locale.US, estado.energiaMes, estado.tarifa),
                soles(energia)
            )
            FilaCosto("Cargo fijo", soles(estado.cargoFijo))
            FilaCosto("Alumbrado público", soles(estado.alumbrado))
            FilaCosto("IGV (%.0f %%)".format(Locale.US, estado.igv * 100), soles(subtotal * estado.igv))
            HorizontalDivider(color = Borde, modifier = Modifier.padding(vertical = 8.dp))
            Row {
                Text("Total estimado", style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f))
                Text(soles(estado.gastoMes), style = MaterialTheme.typography.titleMedium, color = Menta)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Estimación con tu consumo medido. Tu recibo puede incluir otros cargos.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextoSecundario
            )
        }

        // ----- Tarifa -----
        if (!estado.tarifaConfigurada) {
            Text(
                "Configura tu tarifa en Ajustes para ver un costo más exacto.",
                style = MaterialTheme.typography.bodyMedium,
                color = Ambar,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun FilaCosto(concepto: String, valor: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(concepto, style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f))
        Text(valor, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun Pestanas(seleccionado: Int, onSeleccionar: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(Superficie)
            .padding(4.dp)
    ) {
        periodos.forEachIndexed { i, p ->
            val activa = i == seleccionado
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(if (activa) Menta else Superficie)
                    .clickable { onSeleccionar(i) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    p.nombre,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (activa) Fondo else TextoPrincipal
                )
            }
        }
    }
}

@Composable
fun DialPresupuesto(gasto: Double, limite: Double) {
    val porcentaje = (gasto / limite).toFloat().coerceIn(0f, 1f)
    val animado by animateFloatAsState(porcentaje, tween(800), label = "dial")
    val colorPista = SuperficieAlta
    val colorFin = if (porcentaje >= 1f) Peligro else if (porcentaje >= 0.85f) Ambar else Menta
    val colorInicio = colorFin.copy(alpha = 0.45f)

    Box(modifier = Modifier.size(220.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val grosor = 18.dp.toPx()
            val arco = Size(size.width - grosor, size.height - grosor)
            val origen = Offset(grosor / 2, grosor / 2)
            drawArc(
                color = colorPista, startAngle = -90f, sweepAngle = 360f,
                useCenter = false, topLeft = origen, size = arco,
                style = Stroke(width = grosor)
            )
            drawArc(
                brush = Brush.sweepGradient(listOf(colorInicio, colorFin)),
                startAngle = -90f, sweepAngle = 360f * animado,
                useCenter = false, topLeft = origen, size = arco,
                style = Stroke(width = grosor, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.AccountBalanceWallet, null, tint = Menta)
            Text(
                "S/ " + "%.2f".format(Locale.US, gasto),
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 34.sp)
            )
            Text(
                "de S/ " + "%.2f".format(Locale.US, limite),
                style = MaterialTheme.typography.bodyLarge,
                color = TextoSecundario
            )
        }
    }
    val restante = limite - gasto
    Text(
        if (restante >= 0)
            "Te quedan S/ %.2f de tu presupuesto".format(Locale.US, restante)
        else
            "Superaste tu presupuesto por S/ %.2f".format(Locale.US, -restante),
        style = MaterialTheme.typography.labelLarge,
        color = colorFin
    )
    Text(
        "Usaste el ${(gasto / limite * 100).toInt()} % de lo que planeaste gastar",
        style = MaterialTheme.typography.bodyMedium,
        color = TextoSecundario
    )
}

@Composable
fun MiniDato(icono: ImageVector, valor: String, etiqueta: String, modifier: Modifier = Modifier) {
    TarjetaVidrio(modifier = modifier) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Icon(icono, null, tint = Menta)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            valor, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            etiqueta, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium, color = TextoSecundario
        )
    }
}

@Composable
fun GraficoBarras(titulo: String, etiquetas: List<String>, valores: List<Double>) {
    val maximo = valores.max()
    TarjetaVidrio(modifier = Modifier.fillMaxWidth()) {
        Text(titulo, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth().height(150.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            valores.forEachIndexed { i, v ->
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Text(
                        "%.1f".format(Locale.US, v),
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp)
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .width(18.dp)
                            .height((90 * (v / maximo)).dp)
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(Brush.verticalGradient(listOf(Menta, MentaOscuro)))
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        etiquetas[i],
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                        color = TextoSecundario
                    )
                }
            }
        }
    }
}