package com.example.homeshieldai

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.homeshieldai.ui.theme.*
import java.util.Locale
import androidx.compose.ui.graphics.Color

@Composable
fun PantallaInicio(estado: EstadoSistema, onVerAlertas: () -> Unit, onCuenta: () -> Unit) {
    val (textoSala, colorSala) = estadoTemp(estado.tempSala, estado.tempAlerta)
    val (textoTablero, colorTablero) = estadoTemp(estado.tempTablero, estado.tempAlerta)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Encabezado(estado.nombre.take(1).uppercase(), onCuenta)
        TarjetaProtegida(estado.emergencia, onVerAlertas)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TarjetaSensor("Sala", "%.1f °C".format(Locale.US, estado.tempSala),
                textoSala, colorSala, Modifier.weight(1f))
            TarjetaSensor("Tablero", "%.1f °C".format(Locale.US, estado.tempTablero),
                textoTablero, colorTablero, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TarjetaDato(Icons.Filled.Bolt, "Consumo en vivo",
                "%.0f W".format(Locale.US, estado.potencia), Modifier.weight(1f))
            TarjetaDato(Icons.Filled.Payments, "Gasto de hoy", "S/ 2.10", Modifier.weight(1f))
        }
        TarjetaRele(estado)
        TarjetaUltimaAlerta()
    }
}

@Composable
fun TarjetaProtegida(emergencia: Boolean, onClick: () -> Unit) {
    val color = if (emergencia) Peligro else Menta
    TarjetaVidrio(
        modifier = Modifier.fillMaxWidth(),
        colorBorde = color.copy(alpha = 0.6f),
        fondo = if (emergencia)
            Brush.horizontalGradient(listOf(Peligro.copy(alpha = 0.6f), Peligro.copy(alpha = 0.25f)))
        else
            Brush.horizontalGradient(listOf(MentaOscuro, Menta.copy(alpha = 0.35f))),
        onClick = onClick
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (emergencia) Icons.Filled.Warning else Icons.Filled.VerifiedUser,
                null, tint = TextoPrincipal, modifier = Modifier.size(56.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (emergencia) "PELIGRO: Corte de emergencia" else "Tu casa está protegida",
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(Modifier.height(6.dp))
                EstadoPunto(
                    if (emergencia) "Relé apagado por seguridad" else "Todo en orden",
                    if (emergencia) TextoPrincipal else Menta
                )
            }
            Icon(Icons.Filled.ChevronRight, null, tint = TextoPrincipal)
        }
    }
}

@Composable
fun TarjetaSensor(
    nombre: String, temperatura: String,
    estadoTexto: String, estadoColor: Color,
    modifier: Modifier = Modifier
) {
    TarjetaVidrio(
        modifier = modifier,
        colorBorde = if (estadoColor == Menta) Borde else estadoColor.copy(alpha = 0.6f)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Thermostat, null, tint = estadoColor)
            Spacer(Modifier.width(6.dp))
            Text(nombre, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(8.dp))
        Text(temperatura, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(4.dp))
        EstadoPunto(estadoTexto, estadoColor)
    }
}

@Composable
fun TarjetaRele(estado: EstadoSistema) {
    TarjetaVidrio(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MentaOscuro.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Power, null, tint = Menta)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Estufa (Relé)", style = MaterialTheme.typography.titleMedium)
                when {
                    estado.emergencia -> EstadoPunto("Bloqueado por seguridad", Peligro)
                    estado.releEncendido -> EstadoPunto("Encendida", Menta)
                    else -> EstadoPunto("Apagada", TextoSecundario)
                }
            }
            Switch(
                checked = estado.releEncendido,
                onCheckedChange = { estado.releEncendido = it },
                enabled = !estado.emergencia,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = TextoPrincipal,
                    checkedTrackColor = Menta,
                    uncheckedThumbColor = TextoSecundario,
                    uncheckedTrackColor = SuperficieAlta
                )
            )
        }
    }
}

@Composable
fun Encabezado(inicial: String, onCuenta: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.Home, null, tint = Menta, modifier = Modifier.size(38.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("Home Shield AI", style = MaterialTheme.typography.titleLarge)
            Text(
                "Tu hogar más seguro, siempre.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextoSecundario
            )
        }
        BadgedBox(badge = { Badge(containerColor = Menta) }) {
            Icon(Icons.Filled.Notifications, "Notificaciones", tint = TextoSecundario)
        }
        Spacer(Modifier.width(14.dp))
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(MentaOscuro)
                .clickable { onCuenta() },
            contentAlignment = Alignment.Center
        ) {
            Text(inicial, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun TarjetaDato(icono: ImageVector, titulo: String, valor: String, modifier: Modifier = Modifier) {
    TarjetaVidrio(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icono, null, tint = Menta)
            Spacer(Modifier.width(6.dp))
            Text(titulo, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(8.dp))
        Text(valor, style = MaterialTheme.typography.headlineMedium)
    }
}

@Composable
fun TarjetaUltimaAlerta() {
    TarjetaVidrio(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.History, null, tint = TextoPrincipal)
            Spacer(Modifier.width(8.dp))
            Text("Última alerta", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text("Hace 2 h", style = MaterialTheme.typography.bodyMedium, color = TextoSecundario)
        }
        Spacer(Modifier.height(10.dp))
        TarjetaVidrio(
            modifier = Modifier.fillMaxWidth(),
            colorBorde = Menta.copy(alpha = 0.4f),
            fondo = Brush.verticalGradient(listOf(Menta.copy(alpha = 0.12f), Menta.copy(alpha = 0.05f)))
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CheckCircle, null, tint = Menta, modifier = Modifier.size(30.dp))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        "Sistema normal",
                        style = MaterialTheme.typography.titleMedium,
                        color = Menta,
                        fontWeight = FontWeight.Bold
                    )
                    Text("Sin eventos de riesgo.", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}