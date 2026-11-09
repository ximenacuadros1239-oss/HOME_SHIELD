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
import androidx.compose.ui.unit.dp
import com.example.homeshieldai.ui.theme.*

enum class Categoria { TEMPERATURA, ELECTRICA, GENERAL }


enum class Tono { NORMAL, PRECAUCION, PELIGRO, INFO }

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
    val icono: ImageVector
)

val alertasIniciales = listOf(

    Alerta(
        "Temperatura alta",
        "El tablero eléctrico superó los 35 °C (38.0 °C).",
        "Hace 10 min", Categoria.TEMPERATURA, Tono.PRECAUCION, Icons.Filled.Thermostat
    ),
    Alerta(
        "Consumo elevado",
        "Estás al 80% de tu límite mensual de electricidad.",
        "Hace 1 h", Categoria.ELECTRICA, Tono.PRECAUCION, Icons.Filled.Bolt
    ),
    Alerta(
        "Sistema normal",
        "Las mediciones se encuentran dentro de los rangos configurados.",
        "Hace 2 h", Categoria.GENERAL, Tono.NORMAL, Icons.Filled.CheckCircle
    ),
    Alerta(
        "Conexión restablecida",
        "El dispositivo volvió a conectarse correctamente.",
        "Ayer, 18:30", Categoria.GENERAL, Tono.INFO, Icons.Filled.Info
    )
)
val filtros = listOf("Todas", "Temperatura", "Eléctrica")

@Composable
fun TarjetaAlerta(alerta: Alerta) {
    val color = alerta.tono.color()
    TarjetaVidrio(
        modifier = Modifier.fillMaxWidth(),
        colorBorde = color.copy(alpha = 0.55f),
        fondo = Brush.verticalGradient(
            listOf(color.copy(alpha = 0.20f), color.copy(alpha = 0.06f))
        )
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
                        alerta.tiempo,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextoSecundario
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(alerta.mensaje, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
@Composable
fun PantallaAlertas(estado: EstadoSistema) {
    var filtro by remember { mutableIntStateOf(0) }
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
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Alertas", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Historial de seguridad",
                style = MaterialTheme.typography.bodyMedium,
                color = TextoSecundario
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            filtros.forEachIndexed { i, nombre ->
                ChipFiltro(nombre, i == filtro, Modifier.weight(1f)) { filtro = i }
            }
        }
        lista.forEach { TarjetaAlerta(it) }
        if (lista.isEmpty()) {
            Text(
                "Sin alertas en esta categoría.",
                color = TextoSecundario,
                modifier = Modifier.padding(24.dp)
            )
        }
    }
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
