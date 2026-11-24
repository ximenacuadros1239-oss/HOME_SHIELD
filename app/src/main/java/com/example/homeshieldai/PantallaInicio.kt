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
import java.util.Calendar
import java.util.Locale
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role

private fun saludoSegunHora(): String {
    val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        h < 12 -> "Buenos días"
        h < 19 -> "Buenas tardes"
        else -> "Buenas noches"
    }
}

@Composable
fun PantallaInicio(
    estado: EstadoSistema,
    onVerAlertas: () -> Unit,
    onCuenta: () -> Unit,
    onSensor: (String) -> Unit,
    onControl: () -> Unit,
    onConsumo: () -> Unit
) {
    var verEstado by remember { mutableStateOf(false) }
    val (textoSala, colorSala) = estadoTemp(estado.tempSala, estado.tempAlerta)
    val (textoTablero, colorTablero) = estadoTemp(estado.tempTablero, estado.tempAlerta)
    val hayAvisos = estado.emergencia ||
            estado.alertas.take(3).any { it.tono == Tono.PELIGRO || it.tono == Tono.PRECAUCION }
    val primerNombre = estado.nombre.substringBefore(" ")
    val saludo = if (primerNombre.isBlank()) saludoSegunHora()
    else "${saludoSegunHora()}, $primerNombre"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(Modifier.fillMaxWidth().aparecer(0)) {
            Encabezado(
                inicial = estado.nombre.take(1).uppercase().ifBlank { "?" },
                saludo = saludo, hogar = estado.nombreHogar, hayAvisos = hayAvisos,
                onAlertas = onVerAlertas, onCuenta = onCuenta
            )
        }
        Box(Modifier.fillMaxWidth().aparecer(1)) {
            TarjetaProtegida(estado.emergencia, estado.dispositivoConectado) { verEstado = true }
        }
        Box(Modifier.fillMaxWidth().aparecer(2)) {
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
        }
        Box(Modifier.fillMaxWidth().aparecer(3)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TarjetaDato(
                    Icons.Filled.Bolt, "Consumo en vivo", "",
                    Modifier.weight(1f),
                    numero = estado.potencia, formato = { "%.0f W".format(Locale.US, it) }
                ) { onSensor("consumo") }
                TarjetaDato(
                    Icons.Filled.Payments, "Gasto de hoy", "",
                    Modifier.weight(1f),
                    numero = estado.gastoHoy, formato = { "S/ %.2f".format(Locale.US, it) }
                )
            }
        }
        Box(Modifier.fillMaxWidth().aparecer(4)) { TarjetaPresupuesto(estado, onConsumo) }
        Box(Modifier.fillMaxWidth().aparecer(5)) { TarjetaDispositivos(estado, onControl) }
        Box(Modifier.fillMaxWidth().aparecer(6)) { TarjetaUltimaAlerta(estado, onVerAlertas) }
    }

    if (verEstado) HojaEstadoSistema(estado) { verEstado = false }
}

@Composable
fun Encabezado(
    inicial: String,
    saludo: String,
    hogar: String,
    hayAvisos: Boolean,
    onAlertas: () -> Unit,
    onCuenta: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.Home, null, tint = Menta, modifier = Modifier.size(38.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                saludo,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                hogar,
                style = MaterialTheme.typography.bodyMedium,
                color = TextoSecundario,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
                .clickable(onClickLabel = "Abrir mi cuenta", role = Role.Button) { onCuenta() },
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
    val activas = animacionesActivas()
    val latido by pulso.animateFloat(
        initialValue = 1f,
        targetValue = if (!activas) 1f else if (emergencia) 1.12f else 1.05f,
        animationSpec = infiniteRepeatable(
            tween(if (emergencia) 500 else 1600), RepeatMode.Reverse
        ),
        label = "latido"
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
                null, tint = TextoPrincipal, modifier = Modifier
                    .size(56.dp)
                    .graphicsLayer { scaleX = latido; scaleY = latido }
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
                        sinConexion -> "Revisa el Wi-Fi del módulo"
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
    numero: Float? = null,
    formato: (Float) -> String = { "%.0f".format(Locale.US, it) },
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
        if (numero != null) {
            NumeroAnimado(numero, formato, MaterialTheme.typography.headlineMedium)
        } else {
            Text(valor, style = MaterialTheme.typography.headlineMedium)
        }
    }
}

@Composable
fun BarraProgreso(progreso: Float, color: Color) {
    val p by animateFloatAsState(progreso.coerceIn(0f, 1f), tween(700), label = "barra")
    Box(
        Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(50))
            .background(SuperficieAlta)
    ) {
        Box(
            Modifier
                .fillMaxWidth(p)
                .fillMaxHeight()
                .clip(RoundedCornerShape(50))
                .background(color)
        )
    }
}

@Composable
fun TarjetaPresupuesto(estado: EstadoSistema, onClick: () -> Unit) {
    val gastado = estado.gastoMes
    val limite = estado.presupuesto
    val razon = if (limite > 0f) gastado / limite else 0f
    val color = when {
        razon >= 1f -> Peligro
        razon >= 0.8f -> Ambar
        else -> Menta
    }
    val restante = limite - gastado

    TarjetaVidrio(
        modifier = Modifier.fillMaxWidth(),
        colorBorde = if (razon >= 0.8f) color.copy(alpha = 0.5f) else Borde,
        onClick = onClick
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.AccountBalanceWallet, null, tint = color)
            Spacer(Modifier.width(8.dp))
            Text("Presupuesto del mes", style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f))
            Text(
                "%.0f %%".format(Locale.US, razon * 100),
                style = MaterialTheme.typography.titleMedium, color = color
            )
        }
        Spacer(Modifier.height(10.dp))
        BarraProgreso(razon, color)
        Spacer(Modifier.height(8.dp))
        Row {
            Text(
                "S/ %.2f de S/ %.0f".format(Locale.US, gastado, limite),
                style = MaterialTheme.typography.bodyMedium,
                color = TextoSecundario,
                modifier = Modifier.weight(1f)
            )
            Text(
                if (restante >= 0) "Te quedan S/ %.2f".format(Locale.US, restante)
                else "Te pasaste S/ %.2f".format(Locale.US, -restante),
                style = MaterialTheme.typography.bodyMedium,
                color = color
            )
        }
    }
}

@Composable
fun TarjetaDispositivos(estado: EstadoSistema, onControl: () -> Unit) {
    val listos = estado.dispositivos.filter { it.configurado }
    val encendidos = listos.filter { it.encendido }
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
                        EstadoPunto("Sin conexión con el módulo", TextoSecundario)
                    listos.isEmpty() ->
                        EstadoPunto("Aún no se detecta ninguno", TextoSecundario)
                    else -> {
                        EstadoPunto(
                            "${encendidos.size} de ${listos.size} encendidos",
                            if (encendidos.isNotEmpty()) Menta else TextoSecundario
                        )
                        if (encendidos.isNotEmpty()) {
                            Text(
                                encendidos.joinToString(", ") { it.nombre },
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextoSecundario,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
            if (encendidos.isNotEmpty() && !estado.emergencia && estado.dispositivoConectado) {
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
            Text(
                ultima?.let { formatoFecha(it.ts) } ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = TextoSecundario
            )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HojaEstadoSistema(estado: EstadoSistema, onCerrar: () -> Unit) {
    val (textoSala, colorSala) = estadoTemp(estado.tempSala, estado.tempAlerta)
    val (textoTablero, colorTablero) = estadoTemp(estado.tempTablero, estado.tempAlerta)
    val listos = estado.dispositivos.filter { it.configurado }
    val encendidos = listos.count { it.encendido }

    ModalBottomSheet(onDismissRequest = onCerrar, containerColor = Superficie) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("Estado del sistema", style = MaterialTheme.typography.titleLarge)
            Text(
                when {
                    estado.emergencia -> "Hubo un corte de emergencia. Rearma el sistema en Ajustes."
                    !estado.dispositivoConectado -> "No llegan lecturas del módulo."
                    else -> "Todo funciona con normalidad."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = TextoSecundario
            )
            Spacer(Modifier.height(10.dp))
            FilaEstado(
                Icons.Filled.Wifi, "Conexión con el módulo",
                if (estado.dispositivoConectado) "Conectado" else "Sin conexión",
                if (estado.dispositivoConectado) Menta else Ambar
            )
            FilaEstado(
                Icons.Filled.Thermostat, "Sensor de la sala",
                "%.1f °C · %s".format(Locale.US, estado.tempSala, textoSala), colorSala
            )
            FilaEstado(
                Icons.Filled.Thermostat, "Sensor del tablero",
                "%.1f °C · %s".format(Locale.US, estado.tempTablero, textoTablero), colorTablero
            )
            FilaEstado(
                Icons.Filled.Bolt, "Medidor de consumo",
                "%.0f W · %.2f A".format(Locale.US, estado.potencia, estado.corriente),
                if (estado.emergencia) Peligro else Menta
            )
            FilaEstado(
                Icons.Filled.Power, "Dispositivos",
                "$encendidos de ${listos.size} encendidos",
                if (estado.emergencia) Peligro else Menta
            )
        }
    }
}

@Composable
fun FilaEstado(icono: ImageVector, titulo: String, valor: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) { Icon(icono, null, tint = color, modifier = Modifier.size(20.dp)) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium)
            Text(valor, style = MaterialTheme.typography.bodyMedium, color = color)
        }
    }
}