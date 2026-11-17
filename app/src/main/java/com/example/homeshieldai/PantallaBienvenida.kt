package com.example.homeshieldai

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.homeshieldai.ui.theme.*
import java.util.Locale

private class FormUbicacion {
    var depto by mutableStateOf("")
    var distri by mutableStateOf("")
    var tarifa by mutableStateOf("")
    var fijo by mutableStateOf("")
    var referencia by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    /** true = se puede avanzar (guardó o no escribió nada) */
    fun guardar(estado: EstadoSistema): Boolean {
        if (depto.isBlank() && distri.isBlank() && tarifa.isBlank()) return true
        val t = tarifa.replace(',', '.').toFloatOrNull()
        val fj = if (fijo.isBlank()) 0f else fijo.replace(',', '.').toFloatOrNull()
        if (depto.isBlank()) { error = "Elige tu departamento"; return false }
        if (distri.isBlank()) { error = "Elige tu empresa distribuidora"; return false }
        if (t == null || t < 0.05f || t > 5f) {
            error = "El precio por kWh debe estar entre 0.05 y 5.00"; return false
        }
        if (fj == null) { error = "Revisa el cargo fijo"; return false }
        estado.departamento = depto
        estado.distribuidora = distri
        estado.tarifa = t
        estado.cargoFijo = fj
        estado.guardarTarifa()
        error = null
        return true
    }
}

@Composable
fun PantallaBienvenida(estado: EstadoSistema, onTerminar: () -> Unit) {
    var paso by remember { mutableIntStateOf(0) }
    val form = remember { FormUbicacion() }

    val lanzadorPermiso = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido -> if (!concedido) estado.notificaciones = false }

    BackHandler(enabled = paso > 0) { paso-- }

    fun avanzar() {
        if (paso == 1 && !form.guardar(estado)) return
        if (paso < 2) paso++ else {
            estado.guardarConfig()
            onTerminar()
        }
    }

    val brillo = Menta.copy(alpha = 0.18f)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(FondoAlto, Fondo)))
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val centro = Offset(size.width / 2, 0f)
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(brillo, Color.Transparent),
                    center = centro, radius = size.width * 1.1f
                ),
                radius = size.width * 1.1f, center = centro
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Paso ${paso + 1} de 3",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextoSecundario,
                    modifier = Modifier.weight(1f)
                )
                if (paso < 2) {
                    TextButton(onClick = onTerminar) {
                        Text("Omitir", color = TextoSecundario)
                    }
                }
            }

            Box(Modifier.weight(1f)) {
                Crossfade(targetState = paso, label = "paso") { p ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        when (p) {
                            0 -> PasoBienvenida(estado)
                            1 -> PasoUbicacion(form) { paso = 2 }
                            else -> PasoSeguridad(estado) {
                                if (Build.VERSION.SDK_INT >= 33) {
                                    lanzadorPermiso.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                        }
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(3) { i ->
                    Box(
                        Modifier
                            .padding(horizontal = 4.dp)
                            .height(8.dp)
                            .width(if (i == paso) 24.dp else 8.dp)
                            .clip(CircleShape)
                            .background(if (i == paso) Menta else SuperficieAlta)
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (paso > 0) {
                    OutlinedButton(
                        onClick = { paso-- },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f).height(52.dp)
                    ) { Text("Atrás", color = TextoPrincipal) }
                }
                Button(
                    onClick = { avanzar() },
                    colors = ButtonDefaults.buttonColors(containerColor = Menta, contentColor = Fondo),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f).height(52.dp)
                ) {
                    Text(
                        if (paso == 2) "Empezar" else "Siguiente",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun PasoBienvenida(estado: EstadoSistema) {
    val primerNombre = estado.nombre.substringBefore(" ")
    Spacer(Modifier.height(8.dp))
    Image(
        painter = painterResource(R.drawable.ic_splash),
        contentDescription = null,
        modifier = Modifier.size(150.dp)
    )
    Spacer(Modifier.height(8.dp))
    Text(
        if (primerNombre.isBlank()) "Hola" else "Hola, $primerNombre",
        style = MaterialTheme.typography.titleLarge,
        color = TextoSecundario
    )
    Text(
        "Bienvenido a Home Shield AI",
        style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(8.dp))
    Text(
        "Protege tu casa y entiende lo que gastas en luz.",
        style = MaterialTheme.typography.bodyLarge,
        color = TextoSecundario,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(24.dp))
    Beneficio(Icons.Filled.Thermostat, "Temperatura bajo control",
        "Vigila la sala y el tablero eléctrico.")
    Beneficio(Icons.Filled.Bolt, "Consumo y gasto en soles",
        "Mira cuánto gastas y cuánto te queda de presupuesto.")
    Beneficio(Icons.Filled.Power, "Control de tus dispositivos",
        "Enciende, apaga y programa desde el celular.")
    Beneficio(Icons.Filled.Warning, "Alerta de emergencia",
        "Si hay un cortocircuito, se corta la energía y te avisamos.")
}

@Composable
private fun Beneficio(icono: ImageVector, titulo: String, detalle: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MentaOscuro.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) { Icon(icono, null, tint = Menta) }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(titulo, style = MaterialTheme.typography.titleMedium)
            Text(detalle, style = MaterialTheme.typography.bodyMedium, color = TextoSecundario)
        }
    }
}

@Composable
private fun PasoUbicacion(f: FormUbicacion, onDespues: () -> Unit) {
    Text("¿Dónde vives?", style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center)
    Text(
        "Con tu ubicación calculamos tu gasto de luz como en tu recibo.",
        style = MaterialTheme.typography.bodyLarge,
        color = TextoSecundario,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 6.dp, bottom = 16.dp)
    )
    TarjetaVidrio(modifier = Modifier.fillMaxWidth()) {
        CampoSelector("Departamento", f.depto, departamentos.keys.toList()) {
            f.depto = it
            f.distri = ""
            f.referencia = false
            f.error = null
        }
        Spacer(Modifier.height(12.dp))
        val opciones = (departamentos[f.depto]?.map { it.nombre } ?: emptyList()) + OTRA_DISTRIBUIDORA
        CampoSelector(
            "Empresa distribuidora", f.distri, opciones,
            habilitado = f.depto.isNotBlank()
        ) { nombre ->
            f.distri = nombre
            f.error = null
            val ref = departamentos[f.depto]?.firstOrNull { it.nombre == nombre }
            val t = ref?.tarifaRef
            if (t != null) {
                f.tarifa = "%.3f".format(Locale.US, t)
                f.fijo = "%.2f".format(Locale.US, ref.cargoFijoRef ?: 0f)
                f.referencia = true
            } else {
                f.referencia = false
            }
        }
        Spacer(Modifier.height(12.dp))
        if (f.referencia) {
            Text(REFERENCIA_NOTA, style = MaterialTheme.typography.bodyMedium, color = Menta)
            Spacer(Modifier.height(8.dp))
        } else if (f.distri.isNotBlank()) {
            Text(
                "No tengo un valor de referencia para tu zona. Cópialo de tu recibo.",
                style = MaterialTheme.typography.bodyMedium, color = Ambar
            )
            Spacer(Modifier.height(8.dp))
        }
        OutlinedTextField(
            value = f.tarifa,
            onValueChange = { f.tarifa = it; f.error = null; f.referencia = false },
            label = { Text("Precio por kWh, sin IGV (S/)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = f.fijo,
            onValueChange = { f.fijo = it; f.error = null; f.referencia = false },
            label = { Text("Cargo fijo mensual (S/)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
    }
    f.error?.let {
        Text(it, color = Peligro, style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp))
    }
    TextButton(onClick = onDespues) {
        Text("Configurar después en Ajustes", color = TextoSecundario)
    }
}

@Composable
private fun PasoSeguridad(estado: EstadoSistema, onPedirPermiso: () -> Unit) {
    Text("Tu seguridad", style = MaterialTheme.typography.headlineMedium,
        textAlign = TextAlign.Center)
    Text(
        "Define cuándo avisarte. Puedes cambiarlo luego en Ajustes.",
        style = MaterialTheme.typography.bodyLarge,
        color = TextoSecundario,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 6.dp, bottom = 16.dp)
    )
    TarjetaVidrio(modifier = Modifier.fillMaxWidth()) {
        SliderAjuste(
            Icons.Filled.Thermostat, "Temperatura de alerta",
            "%.0f °C".format(Locale.US, estado.tempAlerta),
            estado.tempAlerta, 25f..60f
        ) { estado.tempAlerta = it }
        Spacer(Modifier.height(12.dp))
        SliderAjuste(
            Icons.Filled.Bolt, "Corriente de alerta",
            "%.1f A".format(Locale.US, estado.limiteCorriente),
            estado.limiteCorriente, 5f..32f
        ) { estado.limiteCorriente = it }
    }
    Spacer(Modifier.height(12.dp))
    TarjetaVidrio(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Notifications, null, tint = Menta)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Avisos en el celular", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Cortocircuitos y temperaturas altas.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextoSecundario
                )
            }
            Switch(
                checked = estado.notificaciones,
                onCheckedChange = {
                    estado.notificaciones = it
                    if (it) onPedirPermiso()
                },
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
private fun SliderAjuste(
    icono: ImageVector,
    titulo: String,
    valorTexto: String,
    valor: Float,
    rango: ClosedFloatingPointRange<Float>,
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
}