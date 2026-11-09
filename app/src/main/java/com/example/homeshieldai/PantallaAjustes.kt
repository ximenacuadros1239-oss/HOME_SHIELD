package com.example.homeshieldai

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.homeshieldai.ui.theme.*
import java.util.Locale

@Composable
fun PantallaAjustes(estado: EstadoSistema) {
    var dialogoRearmar by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Ajustes", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Personalización y seguridad",
                style = MaterialTheme.typography.bodyMedium,
                color = TextoSecundario
            )
        }

        TituloSeccion("Límites de seguridad")
        TarjetaVidrio(modifier = Modifier.fillMaxWidth()) {
            FilaSlider(
                Icons.Filled.Thermostat, "Temperatura de alerta",
                "%.0f °C".format(Locale.US, estado.tempAlerta),
                estado.tempAlerta, 25f..60f, "25 °C", "60 °C"
            ) { estado.tempAlerta = it }
            Spacer(Modifier.height(16.dp))
            FilaSlider(
                Icons.Filled.Bolt, "Corriente de alerta",
                "%.1f A".format(Locale.US, estado.limiteCorriente),
                estado.limiteCorriente, 5f..32f, "5 A", "32 A"
            ) { estado.limiteCorriente = it }
            Spacer(Modifier.height(16.dp))
            FilaSlider(
                Icons.Filled.Payments, "Presupuesto mensual",
                "S/ %.0f".format(Locale.US, estado.presupuesto),
                estado.presupuesto, 20f..200f, "S/ 20", "S/ 200"
            ) { estado.presupuesto = it }
        }

        TituloSeccion("Notificaciones")
        TarjetaVidrio(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Notifications, null, tint = Menta)
                Spacer(Modifier.width(12.dp))
                Text(
                    "Alertas en el celular",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = estado.notificaciones,
                    onCheckedChange = { estado.notificaciones = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = TextoPrincipal,
                        checkedTrackColor = Menta,
                        uncheckedThumbColor = TextoSecundario,
                        uncheckedTrackColor = SuperficieAlta
                    )
                )
            }
        }

        TituloSeccion("Reactivación del sistema")
        TarjetaVidrio(
            modifier = Modifier.fillMaxWidth(),
            colorBorde = if (estado.emergencia) Peligro.copy(alpha = 0.6f) else Borde,
            onClick = if (estado.emergencia) ({ dialogoRearmar = true }) else null
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Refresh, null,
                    tint = if (estado.emergencia) Peligro else Menta
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Rearmar sistema", style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (estado.emergencia) "Solo después de revisar la causa de la alerta."
                        else "El sistema opera con normalidad.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextoSecundario
                    )
                }
                if (estado.emergencia) Icon(Icons.Filled.ChevronRight, null, tint = TextoSecundario)
            }
        }

        TituloSeccion("Prueba del sistema")
        TarjetaVidrio(
            modifier = Modifier.fillMaxWidth(),
            onClick = { estado.registrarLectura(18f, estado.tempTablero) }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Warning, null, tint = Ambar)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Simular cortocircuito", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Envía una lectura de 18 A. Actúa si superas tu corriente de alerta.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextoSecundario
                    )
                }
            }
        }

        TituloSeccion("Información del sistema")
        TarjetaVidrio(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Memory, null, tint = Menta)
                Spacer(Modifier.width(12.dp))
                Text(
                    "Controlador",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                Text("ESP32", style = MaterialTheme.typography.titleMedium)
            }
        }
    }

    if (dialogoRearmar) {
        AlertDialog(
            onDismissRequest = { dialogoRearmar = false },
            containerColor = Superficie,
            title = { Text("¿Rearmar el sistema?") },
            text = { Text("Hazlo solo después de desconectar el aparato que causó el problema.") },
            confirmButton = {
                TextButton(onClick = {
                    estado.rearmar()
                    dialogoRearmar = false
                }) { Text("Rearmar", color = Menta) }
            },
            dismissButton = {
                TextButton(onClick = { dialogoRearmar = false }) {
                    Text("Cancelar", color = TextoSecundario)
                }
            }
        )
    }
}

@Composable
fun TituloSeccion(texto: String) {
    Text(
        texto,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
    )
}

@Composable
fun FilaSlider(
    icono: ImageVector,
    titulo: String,
    valorTexto: String,
    valor: Float,
    rango: ClosedFloatingPointRange<Float>,
    etiquetaMin: String,
    etiquetaMax: String,
    onCambio: (Float) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icono, null, tint = Menta)
        Spacer(Modifier.width(12.dp))
        Text(titulo, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Text(valorTexto, style = MaterialTheme.typography.titleMedium)
    }
    Slider(
        value = valor,
        onValueChange = onCambio,
        valueRange = rango,
        colors = SliderDefaults.colors(
            thumbColor = TextoPrincipal,
            activeTrackColor = Menta,
            inactiveTrackColor = SuperficieAlta
        )
    )
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(etiquetaMin, style = MaterialTheme.typography.bodyMedium, color = TextoSecundario)
        Text(etiquetaMax, style = MaterialTheme.typography.bodyMedium, color = TextoSecundario)
    }
}