package com.example.homeshieldai

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.homeshieldai.ui.theme.*
import java.util.Locale

private fun String.aNumero(): Float? = replace(',', '.').trim().toFloatOrNull()
private fun Float.texto(dec: Int) = "%.${dec}f".format(Locale.US, this)

@Composable
fun CampoSelector(
    etiqueta: String,
    valor: String,
    opciones: List<String>,
    habilitado: Boolean = true,
    onSeleccion: (String) -> Unit
) {
    var abierto by remember { mutableStateOf(false) }
    Box {
        OutlinedTextField(
            value = valor,
            onValueChange = {},
            readOnly = true,
            label = { Text(etiqueta) },
            trailingIcon = { Icon(Icons.Filled.ArrowDropDown, null) },
            modifier = Modifier.fillMaxWidth()
        )
        Box(
            Modifier
                .matchParentSize()
                .then(if (habilitado) Modifier.clickable { abierto = true } else Modifier)
        )
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            opciones.forEach { o ->
                DropdownMenuItem(
                    text = { Text(o) },
                    onClick = {
                        onSeleccion(o)
                        abierto = false
                    }
                )
            }
        }
    }
}

@Composable
fun PantallaTarifa(estado: EstadoSistema, onVolver: () -> Unit) {
    val uri = LocalUriHandler.current

    var depto by remember { mutableStateOf(estado.departamento) }
    var distri by remember { mutableStateOf(estado.distribuidora) }
    var tarifaTxt by remember {
        mutableStateOf(if (estado.tarifaConfigurada) estado.tarifa.texto(3) else "")
    }
    var fijoTxt by remember {
        mutableStateOf(if (estado.tarifaConfigurada) estado.cargoFijo.texto(2) else "")
    }
    var alumTxt by remember {
        mutableStateOf(if (estado.tarifaConfigurada) estado.alumbrado.texto(2) else "")
    }
    var diaTxt by remember { mutableStateOf(estado.diaCiclo.toString()) }
    var referencia by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val opcionesDistri = (departamentos[depto]?.map { it.nombre } ?: emptyList()) + OTRA_DISTRIBUIDORA

    val t = tarifaTxt.aNumero()
    val f = fijoTxt.aNumero() ?: 0f
    val a = alumTxt.aNumero() ?: 0f
    val ejemplo100 = if (t != null) (100f * t + f + a) * (1 + estado.igv) else null

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
            Column {
                Text("Tarifa eléctrica", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Para calcular tu gasto como en tu recibo",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextoSecundario
                )
            }
        }

        TituloSeccion("¿Dónde vives?")
        TarjetaVidrio(modifier = Modifier.fillMaxWidth()) {
            CampoSelector("Departamento", depto, departamentos.keys.toList()) {
                depto = it
                distri = ""
                referencia = false
                error = null
            }
            Spacer(Modifier.height(12.dp))
            CampoSelector(
                "Empresa distribuidora", distri, opcionesDistri,
                habilitado = depto.isNotBlank()
            ) { nombre ->
                distri = nombre
                error = null
                val ref = departamentos[depto]?.firstOrNull { it.nombre == nombre }
                if (ref?.tarifaRef != null) {
                    tarifaTxt = ref.tarifaRef.texto(3)
                    fijoTxt = (ref.cargoFijoRef ?: 0f).texto(2)
                    referencia = true
                } else {
                    referencia = false
                }
            }
            if (depto.isBlank()) {
                Text(
                    "Elige primero tu departamento.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextoSecundario,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }

        TituloSeccion("Datos de tu recibo")
        TarjetaVidrio(modifier = Modifier.fillMaxWidth()) {
            if (referencia) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Filled.Info, null, tint = Menta, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(REFERENCIA_NOTA, style = MaterialTheme.typography.bodyMedium, color = Menta)
                }
                Spacer(Modifier.height(12.dp))
            } else if (distri.isNotBlank()) {
                Text(
                    "No tengo un valor de referencia para tu zona. Cópialo de tu recibo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Ambar
                )
                Spacer(Modifier.height(12.dp))
            }
            OutlinedTextField(
                value = tarifaTxt,
                onValueChange = { tarifaTxt = it; error = null; referencia = false },
                label = { Text("Precio por kWh, sin IGV (S/)") },
                supportingText = { Text("En el recibo: «Cargo por energía activa»") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = fijoTxt,
                onValueChange = { fijoTxt = it; error = null; referencia = false },
                label = { Text("Cargo fijo mensual (S/)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = alumTxt,
                onValueChange = { alumTxt = it; error = null },
                label = { Text("Alumbrado público (S/ al mes)") },
                supportingText = { Text("Opcional. Lo ves en el detalle del recibo.") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = diaTxt,
                onValueChange = { diaTxt = it.filter(Char::isDigit).take(2); error = null },
                label = { Text("Día en que empieza tu ciclo (1–28)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
        }

        TarjetaVidrio(modifier = Modifier.fillMaxWidth()) {
            Text("Vista previa", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                if (ejemplo100 != null)
                    "100 kWh al mes costarían unos S/ %.2f".format(Locale.US, ejemplo100)
                else "Ingresa el precio por kWh para ver un ejemplo.",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                "Incluye cargo fijo, alumbrado público e IGV (18 %).",
                style = MaterialTheme.typography.bodyMedium,
                color = TextoSecundario
            )
        }

        error?.let {
            Text(it, color = Peligro, style = MaterialTheme.typography.bodyMedium)
        }

        Button(
            onClick = {
                val dia = diaTxt.toIntOrNull()
                when {
                    depto.isBlank() -> error = "Elige tu departamento"
                    distri.isBlank() -> error = "Elige tu empresa distribuidora"
                    t == null || t < 0.05f || t > 5f ->
                        error = "El precio por kWh debe estar entre 0.05 y 5.00"
                    fijoTxt.isNotBlank() && fijoTxt.aNumero() == null -> error = "Revisa el cargo fijo"
                    alumTxt.isNotBlank() && alumTxt.aNumero() == null -> error = "Revisa el alumbrado público"
                    dia == null || dia !in 1..28 -> error = "El día debe estar entre 1 y 28"
                    else -> {
                        estado.departamento = depto
                        estado.distribuidora = distri
                        estado.tarifa = t
                        estado.cargoFijo = f
                        estado.alumbrado = a
                        estado.diaCiclo = dia
                        estado.guardarTarifa()
                        onVolver()
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Menta, contentColor = Fondo),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) { Text("Guardar tarifa", style = MaterialTheme.typography.titleMedium) }

        OutlinedButton(
            onClick = { uri.openUri("https://www.osinergmin.gob.pe") },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("Ver tarifas oficiales (Osinergmin)", color = Menta)
        }
        Text(
            "En su web: Tarifas → Electricidad → Pliego tarifario.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextoSecundario
        )
        Spacer(Modifier.height(8.dp))
    }
}