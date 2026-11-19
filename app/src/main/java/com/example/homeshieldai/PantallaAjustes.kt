package com.example.homeshieldai

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.homeshieldai.ui.theme.*
import java.util.Locale
import androidx.compose.animation.AnimatedVisibility

@Composable
fun PantallaAjustes(estado: EstadoSistema, onCuenta: () -> Unit, onTarifa: () -> Unit) {
    val context = LocalContext.current
    var dialogo by remember { mutableStateOf<String?>(null) }
    var confirmarRearme by remember { mutableStateOf(false) }
    var confirmarSalir by remember { mutableStateOf(false) }
    var pruebas by remember { mutableStateOf(false) }
    var resultadoPrueba by remember { mutableStateOf<String?>(null) }

    val permitido = remember(estado.ahora, resultadoPrueba) { permisoNotificaciones(context) }
    val lanzador = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { ok ->
        estado.notificaciones = ok
        estado.guardarConfig()
    }
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull() ?: "1.0"
    }
    val avisosActivos = estado.notificaciones && permitido

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CabeceraPantalla("Ajustes", "Personaliza tu sistema", estado)

        // ---------- Perfil ----------
        TarjetaVidrio(modifier = Modifier.fillMaxWidth(), onClick = onCuenta) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(52.dp).clip(CircleShape).background(MentaOscuro),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        estado.nombre.take(1).uppercase().ifBlank { "?" },
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(estado.nombre.ifBlank { "Mi cuenta" },
                        style = MaterialTheme.typography.titleMedium)
                    Text(estado.correo, style = MaterialTheme.typography.bodyMedium,
                        color = TextoSecundario)
                }
                Icon(Icons.Filled.ChevronRight, null, tint = TextoSecundario)
            }
        }

        // ---------- Mi hogar ----------
        TituloSeccion("Mi hogar")
        TarjetaVidrio(modifier = Modifier.fillMaxWidth()) {
            FilaAjuste(
                Icons.Filled.Home, "Nombre del hogar",
                valor = estado.nombreHogar,
                onClick = { dialogo = "hogar" }
            )
            DivisorAjuste()
            FilaAjuste(
                Icons.Filled.Receipt, "Tarifa eléctrica",
                detalle = if (estado.tarifaConfigurada)
                    "${estado.distribuidora} · S/ ${"%.3f".format(Locale.US, estado.tarifa)} por kWh"
                else "Sin configurar. Toca para elegir tu zona.",
                onClick = onTarifa
            )
            DivisorAjuste()
            FilaSliderAjuste(
                Icons.Filled.Payments, "Presupuesto mensual", "Te avisamos cuando te acerques",
                estado.presupuesto, 20f..300f, { "S/ %.0f".format(Locale.US, it) }, Menta,
                onCambio = { estado.presupuesto = it },
                onTerminado = { estado.guardarConfig() }
            )
        }

        // ---------- Seguridad ----------
        TituloSeccion("Seguridad")
        TarjetaVidrio(modifier = Modifier.fillMaxWidth()) {
            FilaSliderAjuste(
                Icons.Filled.Thermostat, "Temperatura de alerta", "Avisa si el tablero la supera",
                estado.tempAlerta, 25f..60f, { "%.0f °C".format(Locale.US, it) }, Ambar,
                onCambio = { estado.tempAlerta = it },
                onTerminado = { estado.guardarConfig() }
            )
            DivisorAjuste()
            FilaSliderAjuste(
                Icons.Filled.Bolt, "Corriente de corte", "Si se supera, se corta la energía",
                estado.limiteCorriente, 5f..32f, { "%.1f A".format(Locale.US, it) }, Peligro,
                onCambio = { estado.limiteCorriente = it },
                onTerminado = { estado.guardarConfig() }
            )
            FilaAjuste(
                Icons.Filled.Refresh, "Rearmar sistema",
                detalle = when {
                    estado.rearmePendiente -> "Orden enviada. Esperando al dispositivo..."
                    estado.emergencia -> "Hubo un corte. Revisa la causa y rearma."
                    else -> "El sistema opera con normalidad."
                },
                colorIcono = if (estado.emergencia) Peligro else Menta,
                onClick = if (estado.emergencia && !estado.rearmePendiente)
                    ({ confirmarRearme = true }) else null
            )
        }

        // ---------- Notificaciones ----------
        TituloSeccion("Notificaciones")
        TarjetaVidrio(modifier = Modifier.fillMaxWidth()) {
            FilaSwitch(
                Icons.Filled.Notifications, "Alertas en el celular",
                detalle = "Recibe avisos importantes aunque no estés en la app",
                checked = estado.notificaciones
            ) { quiere ->
                estado.notificaciones = quiere
                estado.guardarConfig()
                if (quiere && !permitido) {
                    if (Build.VERSION.SDK_INT >= 33)
                        lanzador.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else abrirAjustesNotificaciones(context)
                }
            }
            if (!permitido) {
                DivisorAjuste()
                FilaAjuste(
                    Icons.Filled.NotificationsOff, "Notificaciones bloqueadas",
                    detalle = "El sistema no deja a la app avisarte. Toca para activarlas.",
                    colorIcono = Ambar,
                    onClick = { abrirAjustesNotificaciones(context) }
                )
            }
            DivisorAjuste()
            FilaAjuste(
                Icons.Filled.Warning, "Cortocircuito",
                detalle = "Siempre se avisa si las alertas están activadas",
                colorIcono = Peligro,
                trailing = { Text("Siempre", style = MaterialTheme.typography.bodyMedium,
                    color = TextoSecundario) }
            )
            DivisorAjuste()
            FilaSwitch(
                Icons.Filled.Thermostat, "Temperatura alta",
                detalle = "Cuando el tablero supera tu límite",
                checked = estado.avisoTemperatura, habilitado = avisosActivos
            ) { estado.avisoTemperatura = it; estado.guardarConfig() }
            DivisorAjuste()
            FilaSwitch(
                Icons.Filled.Payments, "Presupuesto de luz",
                detalle = "Al llegar al 80 % y al superarlo",
                checked = estado.avisoPresupuesto, habilitado = avisosActivos
            ) { estado.avisoPresupuesto = it; estado.guardarConfig() }
            DivisorAjuste()
            FilaAjuste(
                Icons.Filled.Send, "Enviar notificación de prueba",
                detalle = resultadoPrueba ?: "Comprueba que las alertas llegan a tu celular",
                onClick = {
                    val ok = mostrarNotificacion(
                        context, "Notificación de prueba",
                        "Si ves este mensaje, las alertas de Home Shield AI funcionan.",
                        Tono.PRECAUCION
                    )
                    resultadoPrueba = if (ok) "Enviada. Mira la barra de notificaciones."
                    else "No se pudo enviar: el permiso está desactivado."
                }
            )
        }

        // ---------- Apariencia ----------
        TituloSeccion("Apariencia")
        TarjetaVidrio(modifier = Modifier.fillMaxWidth()) {
            FilaAjuste(
                Icons.Filled.Palette, "Tema de la app",
                detalle = "Automático sigue el modo de tu celular."
            )
            Spacer(Modifier.height(6.dp))
            SelectorTema(estado.modoTema) { estado.cambiarTema(it) }
        }

        // ---------- Dispositivo ----------
        TituloSeccion("Dispositivo")
        TarjetaVidrio(modifier = Modifier.fillMaxWidth()) {
            FilaAjuste(
                Icons.Filled.Memory, "Controlador ESP32",
                detalle = "ID: HS-ESP32-01",
                trailing = {
                    if (estado.dispositivoConectado) EstadoPunto("Conectado", Menta)
                    else EstadoPunto("Sin conexión", Ambar)
                }
            )
        }

        // ---------- Acerca de ----------
        TituloSeccion("Acerca de")
        TarjetaVidrio(modifier = Modifier.fillMaxWidth()) {
            FilaAjuste(Icons.Filled.Info, "Home Shield AI", valor = "v$version", colorIcono = TextoSecundario)
            DivisorAjuste()
            FilaSwitch(
                Icons.Filled.Build, "Herramientas de prueba",
                detalle = "Simula datos mientras no hay ESP32",
                checked = pruebas
            ) { pruebas = it }
            if (pruebas) {
                DivisorAjuste()
                FilaAjuste(Icons.Filled.Sensors, "Simular lecturas normales",
                    detalle = "Quita la emergencia si la hay",
                    onClick = { estado.simularLecturas() })
                DivisorAjuste()
                FilaAjuste(Icons.Filled.Warning, "Simular cortocircuito",
                    detalle = "Corta los dispositivos y avisa",
                    colorIcono = Peligro, onClick = { estado.simularCortocircuito() })
                DivisorAjuste()
                FilaAjuste(Icons.Filled.CloudOff, "Simular desconexión",
                    detalle = if (estado.dispositivoConectado) "Deja de recibir lecturas"
                    else "Volver a conectar",
                    colorIcono = Ambar, onClick = { estado.alternarConexion() })
            }
        }

        TarjetaVidrio(
            modifier = Modifier.fillMaxWidth(),
            colorBorde = Peligro.copy(alpha = 0.5f),
            onClick = { confirmarSalir = true }
        ) {
            FilaAjuste(
                Icons.Filled.Logout, "Cerrar sesión",
                detalle = estado.correo, colorIcono = Peligro
            )
        }
        Spacer(Modifier.height(8.dp))
    }

    when (dialogo) {
        "hogar" -> DialogoEditar(
            "Nombre del hogar", estado.nombreHogar,
            { estado.nombreHogar = it; estado.guardarConfig(); dialogo = null },
            { dialogo = null }
        )
    }

    if (confirmarRearme) {
        AlertDialog(
            onDismissRequest = { confirmarRearme = false },
            containerColor = Superficie,
            title = { Text("¿Rearmar el sistema?") },
            text = { Text("Hazlo solo después de desconectar el aparato que causó el problema.") },
            confirmButton = {
                TextButton(onClick = {
                    estado.solicitarRearme()
                    confirmarRearme = false
                }) { Text("Rearmar", color = Menta) }
            },
            dismissButton = {
                TextButton(onClick = { confirmarRearme = false }) {
                    Text("Cancelar", color = TextoSecundario)
                }
            }
        )
    }

    if (confirmarSalir) {
        AlertDialog(
            onDismissRequest = { confirmarSalir = false },
            containerColor = Superficie,
            title = { Text("¿Cerrar sesión?") },
            text = { Text("Tendrás que ingresar de nuevo para ver tu hogar.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmarSalir = false
                    estado.cerrarSesion()
                }) { Text("Cerrar sesión", color = Peligro) }
            },
            dismissButton = {
                TextButton(onClick = { confirmarSalir = false }) {
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
        color = TextoSecundario,
        modifier = Modifier.padding(start = 4.dp, top = 6.dp)
    )
}

@Composable
fun DivisorAjuste() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = 50.dp)
            .height(1.dp)
            .background(Borde)
    )
}

@Composable
fun FilaAjuste(
    icono: ImageVector,
    titulo: String,
    detalle: String? = null,
    valor: String? = null,
    colorIcono: Color = Menta,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(colorIcono.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) { Icon(icono, null, tint = colorIcono, modifier = Modifier.size(20.dp)) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium)
            if (detalle != null) {
                Text(detalle, style = MaterialTheme.typography.bodyMedium, color = TextoSecundario)
            }
        }
        if (valor != null) {
            Spacer(Modifier.width(8.dp))
            Text(valor, style = MaterialTheme.typography.titleMedium, color = Menta)
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        } else if (onClick != null) {
            Icon(Icons.Filled.ChevronRight, null, tint = TextoSecundario)
        }
    }
}

@Composable
fun FilaSwitch(
    icono: ImageVector,
    titulo: String,
    detalle: String? = null,
    checked: Boolean,
    habilitado: Boolean = true,
    onCambio: (Boolean) -> Unit
) {
    FilaAjuste(
        icono, titulo, detalle,
        colorIcono = if (habilitado) Menta else TextoSecundario,
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = onCambio,
                enabled = habilitado,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = TextoPrincipal,
                    checkedTrackColor = Menta,
                    uncheckedThumbColor = TextoSecundario,
                    uncheckedTrackColor = SuperficieAlta
                )
            )
        }
    )
}

@Composable
fun DialogoSlider(
    titulo: String,
    inicial: Float,
    rango: ClosedFloatingPointRange<Float>,
    formato: (Float) -> String,
    onGuardar: (Float) -> Unit,
    onCancelar: () -> Unit
) {
    var valor by remember { mutableFloatStateOf(inicial) }
    AlertDialog(
        onDismissRequest = onCancelar,
        containerColor = Superficie,
        title = { Text(titulo) },
        text = {
            Column {
                Text(formato(valor), style = MaterialTheme.typography.headlineMedium, color = Menta)
                Slider(
                    value = valor,
                    onValueChange = { valor = it },
                    valueRange = rango,
                    colors = SliderDefaults.colors(
                        thumbColor = TextoPrincipal,
                        activeTrackColor = Menta,
                        inactiveTrackColor = SuperficieAlta
                    )
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formato(rango.start), style = MaterialTheme.typography.bodyMedium,
                        color = TextoSecundario)
                    Text(formato(rango.endInclusive), style = MaterialTheme.typography.bodyMedium,
                        color = TextoSecundario)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onGuardar(valor) }) { Text("Guardar", color = Menta) }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text("Cancelar", color = TextoSecundario) }
        }
    )
}

@Composable
fun SelectorTema(actual: ModoTema, onCambio: (ModoTema) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(Fondo.copy(alpha = 0.5f))
            .padding(4.dp)
    ) {
        ModoTema.values().forEach { modo ->
            val activo = modo == actual
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(if (activo) Menta else Color.Transparent)
                    .clickable { onCambio(modo) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    modo.etiqueta,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (activo) Fondo else TextoPrincipal
                )
            }
        }
    }
}
@Composable
fun FilaSliderAjuste(
    icono: ImageVector,
    titulo: String,
    detalle: String,
    valor: Float,
    rango: ClosedFloatingPointRange<Float>,
    formato: (Float) -> String,
    colorIcono: Color,
    onCambio: (Float) -> Unit,
    onTerminado: () -> Unit
) {
    var abierto by remember { mutableStateOf(false) }
    Column {
        FilaAjuste(
            icono, titulo, detalle,
            valor = formato(valor),
            colorIcono = colorIcono,
            onClick = { abierto = !abierto },
            trailing = {
                Icon(
                    if (abierto) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    null, tint = TextoSecundario
                )
            }
        )
        AnimatedVisibility(visible = abierto) {
            Column(Modifier.padding(start = 50.dp, bottom = 6.dp)) {
                Slider(
                    value = valor,
                    onValueChange = onCambio,
                    onValueChangeFinished = onTerminado,
                    valueRange = rango,
                    colors = SliderDefaults.colors(
                        thumbColor = TextoPrincipal,
                        activeTrackColor = colorIcono,
                        inactiveTrackColor = SuperficieAlta
                    )
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formato(rango.start), style = MaterialTheme.typography.bodyMedium,
                        color = TextoSecundario)
                    Text(formato(rango.endInclusive), style = MaterialTheme.typography.bodyMedium,
                        color = TextoSecundario)
                }
            }
        }
    }
}