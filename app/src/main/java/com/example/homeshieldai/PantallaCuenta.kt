package com.example.homeshieldai

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.homeshieldai.ui.theme.*

@Composable
fun PantallaCuenta(estado: EstadoSistema, onVolver: () -> Unit) {
    var editando by remember { mutableStateOf<String?>(null) }
    var dialogoSalir by remember { mutableStateOf(false) }

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
            Text("Mi cuenta", style = MaterialTheme.typography.titleLarge)
        }

        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(MentaOscuro, Menta))),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    estado.nombre.take(1).uppercase(),
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 40.sp)
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(estado.nombre, style = MaterialTheme.typography.titleLarge)
            Text(estado.correo, style = MaterialTheme.typography.bodyMedium, color = TextoSecundario)
        }

        TituloSeccion("Perfil")
        FilaCuenta(Icons.Filled.Person, "Nombre", estado.nombre) { editando = "nombre" }
        FilaCuenta(Icons.Filled.Home, "Mi hogar", estado.nombreHogar) { editando = "hogar" }

        TituloSeccion("Dispositivo vinculado")
        TarjetaVidrio(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Memory, null, tint = Menta)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Controlador ESP32", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "ID: HS-ESP32-01",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextoSecundario
                    )
                }
                EstadoPunto("Conectado", Menta)
            }
        }

        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { dialogoSalir = true },
            border = BorderStroke(1.dp, Peligro.copy(alpha = 0.7f)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, null, tint = Peligro)
            Spacer(Modifier.width(8.dp))
            Text("Cerrar sesión", color = Peligro, style = MaterialTheme.typography.titleMedium)
        }
    }

    when (editando) {
        "nombre" -> DialogoEditar("Tu nombre", estado.nombre,
            { estado.cambiarNombre(it); editando = null }, { editando = null })
        "hogar" -> DialogoEditar("Nombre del hogar", estado.nombreHogar,
            { estado.nombreHogar = it; editando = null }, { editando = null })
    }

    if (dialogoSalir) {
        AlertDialog(
            onDismissRequest = { dialogoSalir = false },
            containerColor = Superficie,
            title = { Text("¿Cerrar sesión?") },
            text = { Text("Tendrás que ingresar de nuevo para ver tu hogar.") },
            confirmButton = {
                TextButton(onClick = {
                    dialogoSalir = false
                    estado.cerrarSesion()
                }) { Text("Cerrar sesión", color = Peligro) }
            },
            dismissButton = {
                TextButton(onClick = { dialogoSalir = false }) {
                    Text("Cancelar", color = TextoSecundario)
                }
            }
        )
    }
}

@Composable
fun FilaCuenta(icono: ImageVector, titulo: String, valor: String, onClick: () -> Unit) {
    TarjetaVidrio(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icono, null, tint = Menta)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(titulo, style = MaterialTheme.typography.bodyMedium, color = TextoSecundario)
                Text(valor, style = MaterialTheme.typography.titleMedium)
            }
            Icon(Icons.Filled.Edit, "Editar", tint = TextoSecundario)
        }
    }
}

@Composable
fun DialogoEditar(
    titulo: String,
    valorInicial: String,
    onGuardar: (String) -> Unit,
    onCancelar: () -> Unit
) {
    var texto by remember { mutableStateOf(valorInicial) }
    AlertDialog(
        onDismissRequest = onCancelar,
        containerColor = Superficie,
        title = { Text(titulo) },
        text = { OutlinedTextField(value = texto, onValueChange = { texto = it }, singleLine = true) },
        confirmButton = {
            TextButton(onClick = { if (texto.isNotBlank()) onGuardar(texto.trim()) }) {
                Text("Guardar", color = Menta)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text("Cancelar", color = TextoSecundario) }
        }
    )
}