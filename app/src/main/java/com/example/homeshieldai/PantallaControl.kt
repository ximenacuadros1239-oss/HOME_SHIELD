package com.example.homeshieldai

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.homeshieldai.ui.theme.*
import java.util.Locale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

@Composable
fun PantallaControl(estado: EstadoSistema, onAbrir: (Int) -> Unit) {
    var configurando by remember { mutableStateOf<Dispositivo?>(null) }
    var porEncender by remember { mutableStateOf<Dispositivo?>(null) }
    var confirmarTodo by remember { mutableStateOf(false) }
    val bloqueado = estado.emergencia || !estado.dispositivoConectado
    val listos = estado.dispositivos.filter { it.configurado }
    val encendidos = listos.count { it.encendido }
    val apagados = listos.size - encendidos

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CabeceraPantalla("Control", "Enciende y programa tus aparatos", estado)

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

        if (listos.isNotEmpty()) {
            TarjetaVidrio(modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "$encendidos de ${listos.size} encendidos",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "Consumo actual: %.0f W".format(Locale.US, estado.potencia),
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextoSecundario
                        )
                    }
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        OutlinedButton(
                            onClick = { confirmarTodo = true },
                            enabled = apagados > 0 && !bloqueado,
                            border = BorderStroke(1.dp, Menta.copy(alpha = 0.7f)),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Filled.PlayArrow, null, tint = Menta,
                                modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Encender todo", color = Menta)
                        }
                        OutlinedButton(
                            onClick = { estado.apagarTodo() },
                            enabled = encendidos > 0 && !bloqueado,
                            border = BorderStroke(1.dp, Peligro.copy(alpha = 0.7f)),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Filled.PowerSettingsNew, null, tint = Peligro,
                                modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Apagar todo", color = Peligro)
                        }
                    }
                }
            }
        }

        if (estado.dispositivos.isEmpty()) {
            EstadoSinDispositivos()
        } else {
            estado.dispositivos.forEach { d ->
                key(d.id) {
                    if (d.configurado) {
                        FilaDispositivo(
                            d, estado.ahora, bloqueado,
                            onAbrir = { onAbrir(d.id) },
                            onCambio = { quiere ->
                                if (quiere) porEncender = d else estado.alternarDispositivo(d, false)
                            }
                        )
                    } else {
                        TarjetaDispositivoNuevo { configurando = d }
                    }
                }
            }
        }

        Text(
            "Tu módulo Home Shield detecta los dispositivos conectados y los muestra aquí. El consumo de cada aparato es aproximado; el medidor mide el total de tu casa.",
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

    configurando?.let { d ->
        HojaConfigurarDispositivo(
            onGuardar = { nombre, tipo, potencia ->
                estado.configurarDispositivo(d, nombre, tipo, potencia)
                configurando = null
            },
            onCerrar = { configurando = null }
        )
    }

    if (confirmarTodo) {
        AlertDialog(
            onDismissRequest = { confirmarTodo = false },
            containerColor = Superficie,
            title = { Text("¿Encender todo?") },
            text = {
                Text("Se encenderán los $apagados aparatos apagados. Asegúrate de que estén en buen estado y de que nadie los esté manipulando.")
            },
            confirmButton = {
                TextButton(onClick = {
                    estado.encenderTodo()
                    confirmarTodo = false
                }) { Text("Encender todo", color = Menta) }
            },
            dismissButton = {
                TextButton(onClick = { confirmarTodo = false }) {
                    Text("Cancelar", color = TextoSecundario)
                }
            }
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
fun EstadoSinDispositivos() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Filled.Sensors, null, tint = Menta, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(12.dp))
        Text("Aún no hay dispositivos", style = MaterialTheme.typography.titleLarge)
        Text(
            "Cuando tu módulo Home Shield detecte uno, aparecerá aquí para que lo configures.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextoSecundario,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun TarjetaDispositivoNuevo(onClick: () -> Unit) {
    TarjetaVidrio(
        modifier = Modifier.fillMaxWidth(),
        colorBorde = Menta.copy(alpha = 0.6f),
        fondo = Brush.verticalGradient(
            listOf(Menta.copy(alpha = 0.18f), Menta.copy(alpha = 0.06f))
        ),
        onClick = onClick
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MentaOscuro.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Filled.Sensors, null, tint = Menta) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Dispositivo nuevo detectado", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Toca para decirnos qué aparato es y poder usarlo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextoSecundario
                )
            }
            Icon(Icons.Filled.ChevronRight, null, tint = TextoSecundario)
        }
    }
}

@Composable
fun FilaDispositivo(
    d: Dispositivo,
    ahora: Long,
    bloqueado: Boolean,
    onAbrir: () -> Unit,
    onCambio: (Boolean) -> Unit
) {
    val haptic = LocalHapticFeedback.current
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
                if (d.encendido) EstadoPunto("Encendido · ~${d.potenciaW} W", Menta)
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
                onCheckedChange = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onCambio(it)
                },
                enabled = !bloqueado,
                colors = coloresSwitch(),
                modifier = Modifier.semantics { contentDescription = "Encender o apagar ${d.nombre}" }
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
private fun TileTipo(
    tipo: TipoDispositivo,
    activo: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (activo) Menta.copy(alpha = 0.2f) else SuperficieAlta)
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            tipo.icono, null,
            tint = if (activo) Menta else TextoSecundario,
            modifier = Modifier.size(28.dp)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            tipo.etiqueta,
            style = MaterialTheme.typography.bodyMedium,
            color = if (activo) Menta else TextoPrincipal,
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HojaConfigurarDispositivo(
    onGuardar: (String, TipoDispositivo, Int) -> Unit,
    onCerrar: () -> Unit
) {
    var tipo by remember { mutableStateOf<TipoDispositivo?>(null) }
    var nombre by remember { mutableStateOf("") }
    var potencia by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(onDismissRequest = onCerrar, containerColor = Superficie) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("¿Qué aparato es?", style = MaterialTheme.typography.titleLarge)
            Text(
                "Tu módulo detectó un dispositivo nuevo. Elígelo para poder encenderlo y estimar su consumo.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextoSecundario
            )

            TipoDispositivo.values().toList().chunked(3).forEach { fila ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    fila.forEach { t ->
                        TileTipo(t, t == tipo, Modifier.weight(1f)) {
                            tipo = t
                            nombre = t.etiqueta
                            potencia = t.potenciaTipica.toString()
                            error = null
                        }
                    }
                    repeat(3 - fila.size) { Spacer(Modifier.weight(1f)) }
                }
            }

            if (tipo != null) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it; error = null },
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = potencia,
                    onValueChange = { potencia = it.filter(Char::isDigit).take(5); error = null },
                    label = { Text("Consumo aproximado (W)") },
                    supportingText = {
                        Text("Sirve para estimar el gasto. Puedes cambiarlo si lo conoces.")
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                error?.let {
                    Text(it, color = Peligro, style = MaterialTheme.typography.bodyMedium)
                }
                Button(
                    onClick = {
                        val w = potencia.toIntOrNull()
                        when {
                            nombre.isBlank() -> error = "Escribe un nombre"
                            w == null || w <= 0 -> error = "Indica el consumo en watts"
                            else -> onGuardar(nombre.trim(), tipo!!, w)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Menta, contentColor = Fondo),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) { Text("Guardar", style = MaterialTheme.typography.titleMedium) }
            }
        }
    }
}