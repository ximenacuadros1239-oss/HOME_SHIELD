package com.example.homeshieldai

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.homeshieldai.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class Categoria { TEMPERATURA, ELECTRICA, GENERAL }

enum class Tono { NORMAL, PRECAUCION, PELIGRO, INFO }

enum class AccionAlerta(val etiqueta: String) {
    VER_CONSUMO("Ver mi consumo"),
    VER_TABLERO("Ver temperatura del tablero"),
    IR_AJUSTES("Ir a Ajustes"),
    VER_CONTROL("Ir a Control")
}

@Composable
fun Tono.color(): Color = when (this) {
    Tono.NORMAL -> Menta
    Tono.PRECAUCION -> Ambar
    Tono.PELIGRO -> Peligro
    Tono.INFO -> Info
}

data class Alerta(
    val titulo: String,
    val mensaje: String,
    val tiempo: String,
    val categoria: Categoria,
    val tono: Tono,
    val icono: ImageVector,
    val id: Long = System.nanoTime(),
    val ts: Long = System.currentTimeMillis(),
    val accion: AccionAlerta? = null,
    val consejo: String? = null
)

val filtros = listOf("Todas", "Temperatura", "Eléctrica")

fun fechaCompleta(ts: Long): String =
    SimpleDateFormat("EEEE d 'de' MMMM, HH:mm", Locale("es", "PE"))
        .format(Date(ts))
        .replaceFirstChar { it.uppercase() }

@Composable
fun PantallaAlertas(estado: EstadoSistema, onAccion: (AccionAlerta) -> Unit) {
    var filtro by remember { mutableIntStateOf(0) }
    var confirmarBorrado by remember { mutableStateOf(false) }
    var abierta by remember { mutableStateOf<Alerta?>(null) }
    val lista = when (filtro) {
        1 -> estado.alertas.filter { it.categoria == Categoria.TEMPERATURA }
        2 -> estado.alertas.filter { it.categoria == Categoria.ELECTRICA }
        else -> estado.alertas.toList()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CabeceraPantalla("Alertas", "${estado.alertas.size} eventos registrados", estado) {
            IconButton(
                onClick = { confirmarBorrado = true },
                enabled = estado.alertas.isNotEmpty()
            ) {
                Icon(Icons.Filled.DeleteSweep, "Borrar todas", tint = TextoSecundario)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            filtros.forEachIndexed { i, nombre ->
                ChipFiltro(nombre, i == filtro, Modifier.weight(1f)) { filtro = i }
            }
        }

        if (lista.isEmpty()) {
            EstadoVacio()
        } else {
            Text(
                "Toca una alerta para ver el detalle.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextoSecundario,
                modifier = Modifier.padding(start = 4.dp)
            )
            lista.forEach { alerta ->
                key(alerta.id) {
                    TarjetaAlerta(alerta) { abierta = alerta }
                }
            }
        }
    }

    abierta?.let { alerta ->
        HojaDetalleAlerta(
            alerta = alerta,
            onCerrar = { abierta = null },
            onAccion = { accion ->
                abierta = null
                onAccion(accion)
            },
            onBorrar = {
                estado.alertas.remove(alerta)
                abierta = null
            }
        )
    }

    if (confirmarBorrado) {
        AlertDialog(
            onDismissRequest = { confirmarBorrado = false },
            containerColor = Superficie,
            title = { Text("¿Borrar todas las alertas?") },
            text = { Text("Se eliminará todo el historial de seguridad. Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    estado.alertas.clear()
                    confirmarBorrado = false
                }) { Text("Borrar todo", color = Peligro) }
            },
            dismissButton = {
                TextButton(onClick = { confirmarBorrado = false }) {
                    Text("Cancelar", color = TextoSecundario)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HojaDetalleAlerta(
    alerta: Alerta,
    onCerrar: () -> Unit,
    onAccion: (AccionAlerta) -> Unit,
    onBorrar: () -> Unit
) {
    val color = alerta.tono.color()
    ModalBottomSheet(onDismissRequest = onCerrar, containerColor = Superficie) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(color.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) { Icon(alerta.icono, null, tint = color) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(alerta.titulo, style = MaterialTheme.typography.titleLarge, color = color)
                    Text(
                        fechaCompleta(alerta.ts),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextoSecundario
                    )
                }
            }

            Text(alerta.mensaje, style = MaterialTheme.typography.bodyLarge)

            alerta.consejo?.let {
                TarjetaVidrio(
                    modifier = Modifier.fillMaxWidth(),
                    colorBorde = color.copy(alpha = 0.4f)
                ) {
                    Text("Qué puedes hacer", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = TextoSecundario)
                }
            }

            alerta.accion?.let { accion ->
                Button(
                    onClick = { onAccion(accion) },
                    colors = ButtonDefaults.buttonColors(containerColor = Menta, contentColor = Fondo),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) { Text(accion.etiqueta, style = MaterialTheme.typography.titleMedium) }
            }

            TextButton(onClick = onBorrar, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Borrar esta alerta", color = Peligro)
            }
        }
    }
}

@Composable
fun EstadoVacio() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Filled.NotificationsNone, null, tint = Menta, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(12.dp))
        Text("Sin alertas", style = MaterialTheme.typography.titleLarge)
        Text(
            "Tu hogar está tranquilo. Cuando algo requiera atención, lo verás aquí.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextoSecundario,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertaDeslizable(alerta: Alerta, onClick: () -> Unit, onBorrar: () -> Unit) {
    val estadoDeslizar = rememberSwipeToDismissBoxState(
        confirmValueChange = { valor ->
            if (valor != SwipeToDismissBoxValue.Settled) {
                onBorrar()
                true
            } else false
        }
    )
    SwipeToDismissBox(
        state = estadoDeslizar,
        backgroundContent = {
            val direccion = estadoDeslizar.dismissDirection
            val fondo = if (direccion == SwipeToDismissBoxValue.Settled) Color.Transparent
            else Peligro.copy(alpha = 0.25f)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(fondo)
                    .padding(horizontal = 20.dp),
                contentAlignment =
                    if (direccion == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart
                    else Alignment.CenterEnd
            ) {
                if (direccion != SwipeToDismissBoxValue.Settled)
                    Icon(Icons.Filled.Delete, "Borrar", tint = Peligro)
            }
        }
    ) { TarjetaAlerta(alerta, onClick) }
}

@Composable
fun ChipFiltro(texto: String, activo: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (activo) Menta else Superficie)
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            texto,
            style = MaterialTheme.typography.labelLarge,
            color = if (activo) Fondo else TextoPrincipal
        )
    }
}

@Composable
fun TarjetaAlerta(alerta: Alerta, onClick: (() -> Unit)? = null) {
    val color = alerta.tono.color()
    TarjetaVidrio(
        modifier = Modifier.fillMaxWidth(),
        colorBorde = color.copy(alpha = 0.55f),
        fondo = Brush.verticalGradient(
            listOf(color.copy(alpha = 0.20f), color.copy(alpha = 0.06f))
        ),
        onClick = onClick
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(alerta.icono, null, tint = color, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        alerta.titulo,
                        style = MaterialTheme.typography.titleMedium,
                        color = color,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        formatoFecha(alerta.ts),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextoSecundario
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(alerta.mensaje, style = MaterialTheme.typography.bodyMedium, maxLines = 3)
            }
        }
    }
}