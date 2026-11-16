package com.example.homeshieldai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.homeshieldai.ui.theme.*

@Composable
fun CabeceraPantalla(
    titulo: String,
    subtitulo: String,
    estado: EstadoSistema,
    accion: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.headlineMedium)
            Text(
                subtitulo,
                style = MaterialTheme.typography.bodyMedium,
                color = TextoSecundario
            )
        }
        PildoraEstado(estado)
        if (accion != null) {
            Spacer(Modifier.width(4.dp))
            accion()
        }
    }
}

@Composable
fun PildoraEstado(estado: EstadoSistema) {
    val (texto, color) = when {
        estado.emergencia -> "Emergencia" to Peligro
        !estado.dispositivoConectado -> "Sin conexión" to Ambar
        else -> "En línea" to Menta
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(texto, style = MaterialTheme.typography.labelLarge, color = color)
    }
}