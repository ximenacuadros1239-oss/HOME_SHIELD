package com.example.homeshieldai

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.homeshieldai.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogoHora(
    titulo: String,
    minutoInicial: Int,
    onConfirmar: (Int) -> Unit,
    onCancelar: () -> Unit
) {
    val reloj = rememberTimePickerState(
        initialHour = minutoInicial / 60,
        initialMinute = minutoInicial % 60,
        is24Hour = true
    )
    AlertDialog(
        onDismissRequest = onCancelar,
        containerColor = Superficie,
        title = { Text(titulo) },
        text = { TimePicker(state = reloj) },
        confirmButton = {
            TextButton(onClick = { onConfirmar(reloj.hour * 60 + reloj.minute) }) {
                Text("Aceptar", color = Menta)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text("Cancelar", color = TextoSecundario) }
        }
    )
}

@Composable
fun PantallaDispositivo(estado: EstadoSistema, id: Int, onVolver: () -> Unit) {
    var editarNombre by remember { mutableStateOf(false) }
    var editarPotencia by remember { mutableStateOf(false) }
    var dialogoHora by remember { mutableStateOf<String?>(null) }
    var confirmarEncender by remember { mutableStateOf(false) }
    var confirmarEliminar by remember { mutableStateOf(false) }

    val d = estado.dispositivo(id)
    if (d == null) {
        LaunchedEffect(Unit) { onVolver() }
        return
    }
    val bloqueado = estado.emergencia || !estado.dispositivoConectado

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
            Text(d.nombre, style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f))
            IconButton(onClick = { editarNombre = true }) {
                Icon(Icons.Filled.Edit, "Cambiar nombre", tint = TextoSecundario)
            }
        }

        if (bloqueado) {
            Aviso(
                if (estado.emergencia) "Bloqueado por la emergencia."
                else "Sin conexión con el dispositivo.",
                if (estado.emergencia) Peligro else Ambar,
                if (estado.emergencia) Icons.Filled.Warning else Icons.Filled.CloudOff
            )
        }

        TarjetaVidrio(
            modifier = Modifier.fillMaxWidth(),
            colorBorde = if (d.encendido) Menta.copy(alpha = 0.5f) else Borde
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (d.encendido) MentaOscuro.copy(alpha = 0.6f) else SuperficieAlta
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(d.tipo.icono, null, modifier = Modifier.size(30.dp),
                        tint = if (d.encendido) Menta else TextoSecundario)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Estado", style = MaterialTheme.typography.bodyMedium,
                        color = TextoSecundario)
                    Text(
                        if (d.encendido) "Encendido" else "Apagado",
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
                Switch(
                    checked = d.encendido,
                    onCheckedChange = { quiere ->
                        if (quiere) confirmarEncender = true
                        else estado.alternarDispositivo(d, false)
                    },
                    enabled = !bloqueado,
                    colors = coloresSwitch()
                )
            }
        }

        TituloSeccion("Temporizador")
        TarjetaVidrio(modifier = Modifier.fillMaxWidth()) {
            if (d.apagarEnMs > 0) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Timer, null, tint = Ambar)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Se apagará en ${minutosRestantes(d, estado.ahora)} min",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { estado.programarApagado(d, 0) }) {
                        Text("Cancelar", color = Peligro)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            Text(
                if (d.encendido) "Apagar automáticamente en:"
                else "Enciende el dispositivo para usar el temporizador.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextoSecundario
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(15 to "15 min", 30 to "30 min", 60 to "1 h", 120 to "2 h").forEach { (m, t) ->
                    ChipOpcion(t, false, habilitado = d.encendido && !bloqueado) {
                        estado.programarApagado(d, m)
                    }
                }
            }
        }

        TituloSeccion("Programación semanal")
        TarjetaVidrio(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Encender y apagar solo", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Se aplica en los días elegidos.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextoSecundario
                    )
                }
                Switch(
                    checked = d.programado,
                    onCheckedChange = { d.programado = it },
                    colors = coloresSwitch()
                )
            }
            if (d.programado) {
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BloqueHora("Enciende", d.minutoEncendido, Modifier.weight(1f)) {
                        dialogoHora = "on"
                    }
                    BloqueHora("Apaga", d.minutoApagado, Modifier.weight(1f)) {
                        dialogoHora = "off"
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("L", "M", "X", "J", "V", "S", "D").forEachIndexed { i, letra ->
                        val activo = d.dias[i]
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (activo) Menta else SuperficieAlta)
                                .clickable { d.dias[i] = !activo },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                letra,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (activo) Fondo else TextoPrincipal
                            )
                        }
                    }
                }
            }
        }

        TituloSeccion("Datos del dispositivo")
        TarjetaVidrio(modifier = Modifier.fillMaxWidth()) {
            Text("Tipo", style = MaterialTheme.typography.bodyMedium, color = TextoSecundario)
            Spacer(Modifier.height(6.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                TipoDispositivo.values().forEach {
                    ChipOpcion(it.etiqueta, it == d.tipo) { d.tipo = it }
                }
            }
            Spacer(Modifier.height(14.dp))
            Text("Conexión", style = MaterialTheme.typography.bodyMedium, color = TextoSecundario)
            Text("Toma ${d.canal} del módulo", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier.fillMaxWidth().clickable { editarPotencia = true },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Potencia estimada", style = MaterialTheme.typography.bodyMedium,
                        color = TextoSecundario)
                    Text("${d.potenciaW} W", style = MaterialTheme.typography.titleMedium)
                }
                Icon(Icons.Filled.Edit, "Editar", tint = TextoSecundario)
            }
        }

        OutlinedButton(
            onClick = { confirmarEliminar = true },
            border = BorderStroke(1.dp, Peligro.copy(alpha = 0.7f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Icon(Icons.Filled.Delete, null, tint = Peligro)
            Spacer(Modifier.width(8.dp))
            Text("Quitar de la toma ${d.canal}", color = Peligro,
                style = MaterialTheme.typography.titleMedium)
        }
        Spacer(Modifier.height(8.dp))
    }

    if (editarNombre) {
        DialogoEditar("Nombre del dispositivo", d.nombre,
            { d.nombre = it; editarNombre = false }, { editarNombre = false })
    }
    if (editarPotencia) {
        DialogoEditar("Potencia estimada (W)", d.potenciaW.toString(),
            {
                it.toIntOrNull()?.takeIf { w -> w > 0 }?.let { w -> d.potenciaW = w }
                editarPotencia = false
            },
            { editarPotencia = false })
    }
    when (dialogoHora) {
        "on" -> DialogoHora("Hora de encendido", d.minutoEncendido,
            { d.minutoEncendido = it; dialogoHora = null }, { dialogoHora = null })
        "off" -> DialogoHora("Hora de apagado", d.minutoApagado,
            { d.minutoApagado = it; dialogoHora = null }, { dialogoHora = null })
    }
    if (confirmarEncender) {
        DialogoConfirmarEncender(
            d.nombre,
            onConfirmar = { estado.alternarDispositivo(d, true); confirmarEncender = false },
            onCancelar = { confirmarEncender = false }
        )
    }
    if (confirmarEliminar) {
        AlertDialog(
            onDismissRequest = { confirmarEliminar = false },
            containerColor = Superficie,
            title = { Text("¿Quitar ${d.nombre} de la toma ${d.canal}?") },
            text = { Text("La toma quedará libre y se perderá la programación de este aparato.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarEliminar = false
                    estado.desvincular(d)
                }) { Text("Quitar", color = Peligro) }
            },
            dismissButton = {
                TextButton(onClick = { confirmarEliminar = false }) {
                    Text("Cancelar", color = TextoSecundario)
                }
            }
        )
    }
}

@Composable
fun BloqueHora(etiqueta: String, minutos: Int, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(SuperficieAlta)
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Text(etiqueta, style = MaterialTheme.typography.bodyMedium, color = TextoSecundario)
        Text(minutosATexto(minutos), style = MaterialTheme.typography.headlineMedium)
    }
}