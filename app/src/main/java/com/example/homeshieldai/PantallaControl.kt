package com.example.homeshieldai

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.homeshieldai.ui.theme.*
import java.util.Locale

@Composable
fun PantallaControl(estado: EstadoSistema, onAbrir: (Int) -> Unit) {
    var agregando by remember { mutableStateOf(false) }
    var porEncender by remember { mutableStateOf<Dispositivo?>(null) }
    val bloqueado = estado.emergencia || !estado.dispositivoConectado
    val encendidos = estado.dispositivos.count { it.encendido }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Control", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Tus dispositivos conectados",
                style = MaterialTheme.typography.bodyMedium,
                color = TextoSecundario
            )
        }

        if (estado.emergencia) {
            Aviso(
                "Control bloqueado por la emergencia. Rearma el sistema en Ajustes.",
                Peligro, Icons.Filled.Warning
            )
        } else if (!estado.dispositivoConectado) {
            Aviso(
                "Sin conexión con el dispositivo. No se pueden enviar órdenes.",
                Ambar, Icons.Filled.CloudOff
            )
        }

        TarjetaVidrio(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "$encendidos de ${estado.dispositivos.size} encendidos",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "Consumo actual: %.0f W".format(Locale.US, estado.potencia),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextoSecundario
                    )
                }
                OutlinedButton(
                    onClick = { estado.apagarTodo() },
                    enabled = encendidos > 0 && !bloqueado,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Peligro.copy(alpha = 0.7f)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Filled.PowerSettingsNew, null, tint = Peligro,
                        modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Apagar todo", color = Peligro)
                }
            }
        }

        estado.dispositivos.forEach { d ->
            key(d.id) {
                FilaDispositivo(
                    d, estado.ahora, bloqueado,
                    onAbrir = { onAbrir(d.id) },
                    onCambio = { quiere ->
                        if (quiere) porEncender = d else estado.alternarDispositivo(d, false)
                    }
                )
            }
        }

        TarjetaVidrio(modifier = Modifier.fillMaxWidth(), onClick = { agregando = true }) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Add, null, tint = Menta)
                Spacer(Modifier.width(8.dp))
                Text("Agregar dispositivo", style = MaterialTheme.typography.titleMedium,
                    color = Menta)
            }
        }

        Text(
            "Cada dispositivo usa un canal del módulo de relés. El consumo individual es una estimación; el medidor mide el total de la casa.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextoSecundario,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }

    porEncender?.let { d ->
        DialogoConfirmarEncender(
            d.nombre,
            onConfirmar = { estado.alternarDispositivo(d, true); porEncender = null },
            onCancelar = { porEncender = null }
        )
    }

    if (agregando) {
        DialogoAgregarDispositivo(
            onAgregar = { nombre, tipo, canal, potencia ->
                estado.agregarDispositivo(nombre, tipo, canal, potencia)
                agregando = false
            },
            onCancelar = { agregando = false }
        )
    }
}

@Composable
fun Aviso(texto: String, color: Color, icono: ImageVector) {
    TarjetaVidrio(
        modifier = Modifier.fillMaxWidth(),
        colorBorde = color.copy(alpha = 0.6f),
        fondo = Brush.verticalGradient(listOf(color.copy(alpha = 0.18f), color.copy(alpha = 0.06f)))
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icono, null, tint = color)
            Spacer(Modifier.width(10.dp))
            Text(texto, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun coloresSwitch() = SwitchDefaults.colors(
    checkedThumbColor = TextoPrincipal,
    checkedTrackColor = Menta,
    uncheckedThumbColor = TextoSecundario,
    uncheckedTrackColor = SuperficieAlta
)

@Composable
fun FilaDispositivo(
    d: Dispositivo,
    ahora: Long,
    bloqueado: Boolean,
    onAbrir: () -> Unit,
    onCambio: (Boolean) -> Unit
) {
    TarjetaVidrio(
        modifier = Modifier.fillMaxWidth(),
        colorBorde = if (d.encendido) Menta.copy(alpha = 0.5f) else Borde,
        onClick = onAbrir
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (d.encendido) MentaOscuro.copy(alpha = 0.6f) else SuperficieAlta),
                contentAlignment = Alignment.Center
            ) {
                Icon(d.tipo.icono, null, tint = if (d.encendido) Menta else TextoSecundario)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(d.nombre, style = MaterialTheme.typography.titleMedium)
                Text(
                    "Canal ${d.canal} · ${d.potenciaW} W",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextoSecundario
                )
                if (d.encendido) EstadoPunto("Encendido", Menta)
                else EstadoPunto("Apagado", TextoSecundario)
                if (d.apagarEnMs > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Timer, null, tint = Ambar, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Se apaga en ${minutosRestantes(d, ahora)} min",
                            style = MaterialTheme.typography.bodyMedium, color = Ambar
                        )
                    }
                }
                if (d.programado) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Schedule, null, tint = TextoSecundario,
                            modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Programado ${minutosATexto(d.minutoEncendido)} – ${minutosATexto(d.minutoApagado)}",
                            style = MaterialTheme.typography.bodyMedium, color = TextoSecundario
                        )
                    }
                }
            }
            Switch(
                checked = d.encendido,
                onCheckedChange = onCambio,
                enabled = !bloqueado,
                colors = coloresSwitch()
            )
        }
    }
}

@Composable
fun ChipOpcion(texto: String, activo: Boolean, habilitado: Boolean = true, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (activo) Menta else SuperficieAlta)
            .then(if (habilitado) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            texto,
            style = MaterialTheme.typography.labelLarge,
            color = when {
                activo -> Fondo
                habilitado -> TextoPrincipal
                else -> TextoSecundario
            }
        )
    }
}

@Composable
fun DialogoConfirmarEncender(nombre: String, onConfirmar: () -> Unit, onCancelar: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancelar,
        containerColor = Superficie,
        title = { Text("¿Encender $nombre?") },
        text = { Text("Asegúrate de que el aparato esté en buen estado y de que nadie lo esté manipulando.") },
        confirmButton = {
            TextButton(onClick = onConfirmar) { Text("Encender", color = Menta) }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text("Cancelar", color = TextoSecundario) }
        }
    )
}

@Composable
fun DialogoAgregarDispositivo(
    onAgregar: (String, TipoDispositivo, Int, Int) -> Unit,
    onCancelar: () -> Unit
) {
    var nombre by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf(TipoDispositivo.OTRO) }
    var canal by remember { mutableIntStateOf(1) }
    var potencia by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onCancelar,
        containerColor = Superficie,
        title = { Text("Nuevo dispositivo") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it; error = null },
                    label = { Text("Nombre") },
                    singleLine = true
                )
                Text("Tipo", style = MaterialTheme.typography.bodyMedium, color = TextoSecundario)
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TipoDispositivo.values().forEach {
                        ChipOpcion(it.etiqueta, it == tipo) { tipo = it }
                    }
                }
                Text("Canal del relé", style = MaterialTheme.typography.bodyMedium,
                    color = TextoSecundario)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (1..4).forEach { ChipOpcion("$it", it == canal) { canal = it } }
                }
                OutlinedTextField(
                    value = potencia,
                    onValueChange = { potencia = it.filter(Char::isDigit).take(5); error = null },
                    label = { Text("Potencia estimada (W)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                error?.let {
                    Text(it, color = Peligro, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val w = potencia.toIntOrNull()
                when {
                    nombre.isBlank() -> error = "Escribe un nombre"
                    w == null || w <= 0 -> error = "Indica la potencia en watts"
                    else -> onAgregar(nombre.trim(), tipo, canal, w)
                }
            }) { Text("Agregar", color = Menta) }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text("Cancelar", color = TextoSecundario) }
        }
    )
}