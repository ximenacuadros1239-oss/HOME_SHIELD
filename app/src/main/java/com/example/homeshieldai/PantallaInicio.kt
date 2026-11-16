package com.example.homeshieldai

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.homeshieldai.ui.theme.*
import java.util.Locale

@Composable
fun PantallaInicio(
    estado: EstadoSistema,
    onVerAlertas: () -> Unit,
    onCuenta: () -> Unit,
    onSensor: (String) -> Unit,
    onControl: () -> Unit
) {
    val (textoSala, colorSala) = estadoTemp(estado.tempSala, estado.tempAlerta)
    val (textoTablero, colorTablero) = estadoTemp(estado.tempTablero, estado.tempAlerta)
    val hayAvisos = estado.emergencia ||
            estado.alertas.take(3).any { it.tono == Tono.PELIGRO || it.tono == Tono.PRECAUCION }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Encabezado(
            estado.nombre.take(1).uppercase().ifBlank { "?" },
            hayAvisos, onVerAlertas, onCuenta
        )
        TarjetaProtegida(estado.emergencia, estado.dispositivoConectado, onVerAlertas)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TarjetaSensor(
                "Sala", "%.1f °C".format(Locale.US, estado.tempSala),
                textoSala, colorSala, Modifier.weight(1f)
            ) { onSensor("sala") }
            TarjetaSensor(
                "Tablero", "%.1f °C".format(Locale.US, estado.tempTablero),
                textoTablero, colorTablero, Modifier.weight(1f)
            ) { onSensor("tablero") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TarjetaDato(
                Icons.Filled.Bolt, "Consumo en vivo",
                "%.0f W".format(Locale.US, estado.potencia), Modifier.weight(1f)
            ) { onSensor("consumo") }
            TarjetaDato(
                Icons.Filled.Payments, "Gasto de hoy",
                "S/ %.2f".format(Locale.US, estado.gastoHoy), Modifier.weight(1f)
            )
        }
        TarjetaDispositivos(estado, onControl)
        TarjetaUltimaAlerta(estado, onVerAlertas)
    }
}

@Composable
fun Encabezado(
    inicial: String,
    hayAvisos: Boolean,
    onAlertas: () -> Unit,
    onCuenta: () -> Unit
) {
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
        IconButton(onClick = onAlertas) {
            BadgedBox(badge = { if (hayAvisos) Badge(containerColor = Menta) }) {
                Icon(Icons.Filled.Notifications, "Alertas", tint = TextoSecundario)
            }
        }
        Spacer(Modifier.width(4.dp))
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
fun TarjetaProtegida(emergencia: Boolean, conectado: Boolean, onClick: () -> Unit) {
    val sinConexion = !conectado && !emergencia
    val color = when {
        emergencia -> Peligro
        sinConexion -> Ambar
        else -> Menta
    }
    val pulso = rememberInfiniteTransition(label = "pulso")
    val alfa by pulso.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "alfa"
    )
    val borde = if (emergencia) Peligro.copy(alpha = alfa) else color.copy(alpha = 0.6f)

    TarjetaVidrio(
        modifier = Modifier.fillMaxWidth(),
        colorBorde = borde,
        fondo = when {
            emergencia -> Brush.horizontalGradient(
                listOf(Peligro.copy(alpha = 0.6f), Peligro.copy(alpha = 0.25f)))
            sinConexion -> Brush.horizontalGradient(
                listOf(Ambar.copy(alpha = 0.35f), Ambar.copy(alpha = 0.12f)))
            else -> Brush.horizontalGradient(
                listOf(MentaOscuro, Menta.copy(alpha = 0.35f)))
        },
        onClick = onClick
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                when {
                    emergencia -> Icons.Filled.Warning
                    sinConexion -> Icons.Filled.CloudOff
                    else -> Icons.Filled.VerifiedUser
                },
                null, tint = TextoPrincipal, modifier = Modifier.size(56.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    when {
                        emergencia -> "PELIGRO: Corte de emergencia"
                        sinConexion -> "Sin conexión con tu dispositivo"
                        else -> "Tu casa está protegida"
                    },
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(Modifier.height(6.dp))
                EstadoPunto(
                    when {
                        emergencia -> "Relé apagado por seguridad"
                        sinConexion -> "Revisa el Wi-Fi del ESP32"
                        else -> "Todo en orden"
                    },
                    if (emergencia) TextoPrincipal else color
                )
            }
            Icon(Icons.Filled.ChevronRight, null, tint = TextoPrincipal)
        }
    }
}

@Composable
fun TarjetaSensor(
    nombre: String,
    temperatura: String,
    estadoTexto: String,
    estadoColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    TarjetaVidrio(
        modifier = modifier,
        colorBorde = if (estadoColor == Menta) Borde else estadoColor.copy(alpha = 0.6f),
        onClick = onClick
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Thermostat, null, tint = estadoColor)
            Spacer(Modifier.width(6.dp))
            Text(nombre, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Icon(Icons.Filled.ChevronRight, null, tint = TextoSecundario,
                modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(temperatura, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(4.dp))
        EstadoPunto(estadoTexto, estadoColor)
    }
}

@Composable
fun TarjetaDato(
    icono: ImageVector,
    titulo: String,
    valor: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    TarjetaVidrio(modifier = modifier, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icono, null, tint = Menta)
            Spacer(Modifier.width(6.dp))
            Text(titulo, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            if (onClick != null) {
                Icon(Icons.Filled.ChevronRight, null, tint = TextoSecundario,
                    modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(valor, style = MaterialTheme.typography.headlineMedium)
    }
}

@Composable
fun TarjetaDispositivos(estado: EstadoSistema, onControl: () -> Unit) {
    val encendidos = estado.dispositivos.count { it.encendido }
    TarjetaVidrio(modifier = Modifier.fillMaxWidth(), onClick = onControl) {
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
                Text("Dispositivos", style = MaterialTheme.typography.titleMedium)
                when {
                    estado.emergencia -> EstadoPunto("Bloqueados por seguridad", Peligro)
                    !estado.dispositivoConectado ->
                        EstadoPunto("Sin conexión con el dispositivo", TextoSecundario)
                    else -> EstadoPunto(
                        "$encendidos de ${estado.dispositivos.size} encendidos",
                        if (encendidos > 0) Menta else TextoSecundario
                    )
                }
            }
            if (encendidos > 0 && !estado.emergencia && estado.dispositivoConectado) {
                TextButton(onClick = { estado.apagarTodo() }) {
                    Text("Apagar todo", color = Peligro)
                }
            }
            Icon(Icons.Filled.ChevronRight, null, tint = TextoSecundario)
        }
    }
}

@Composable
fun TarjetaUltimaAlerta(estado: EstadoSistema, onVerAlertas: () -> Unit) {
    val ultima = estado.alertas.firstOrNull()
    TarjetaVidrio(modifier = Modifier.fillMaxWidth(), onClick = onVerAlertas) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.History, null, tint = TextoPrincipal)
            Spacer(Modifier.width(8.dp))
            Text("Última alerta", style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f))
            Text(ultima?.tiempo ?: "", style = MaterialTheme.typography.bodyMedium,
                color = TextoSecundario)
        }
        Spacer(Modifier.height(10.dp))
        if (ultima == null) {
            Text("Sin eventos de riesgo.", style = MaterialTheme.typography.bodyMedium,
                color = TextoSecundario)
        } else {
            val c = ultima.tono.color()
            TarjetaVidrio(
                modifier = Modifier.fillMaxWidth(),
                colorBorde = c.copy(alpha = 0.4f),
                fondo = Brush.verticalGradient(
                    listOf(c.copy(alpha = 0.12f), c.copy(alpha = 0.05f)))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(ultima.icono, null, tint = c, modifier = Modifier.size(30.dp))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            ultima.titulo,
                            style = MaterialTheme.typography.titleMedium,
                            color = c,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            ultima.mensaje,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
