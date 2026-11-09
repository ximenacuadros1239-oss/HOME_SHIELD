package com.example.homeshieldai

import android.util.Patterns
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.homeshieldai.ui.theme.*

@Composable
fun PantallaLogin(estado: EstadoSistema) {
    var modoRegistro by remember { mutableStateOf(false) }
    var nombre by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var clave by remember { mutableStateOf("") }
    var verClave by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }

    val colores = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Menta,
        unfocusedBorderColor = Borde,
        focusedLabelColor = Menta,
        unfocusedLabelColor = TextoSecundario,
        cursorColor = Menta,
        focusedTextColor = TextoPrincipal,
        unfocusedTextColor = TextoPrincipal
    )

    fun validar(): String? = when {
        modoRegistro && nombre.isBlank() -> "Ingresa tu nombre"
        !Patterns.EMAIL_ADDRESS.matcher(correo.trim()).matches() -> "Ingresa un correo válido"
        clave.length < 6 -> "La contraseña debe tener al menos 6 caracteres"
        else -> null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(FondoAlto, Fondo)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Filled.VerifiedUser, null, tint = Menta, modifier = Modifier.size(80.dp))
            Spacer(Modifier.height(12.dp))
            Text("Home Shield AI", style = MaterialTheme.typography.headlineMedium)
            Text(
                if (modoRegistro) "Crea tu cuenta" else "Tu hogar más seguro, siempre.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextoSecundario
            )
            Spacer(Modifier.height(28.dp))

            if (modoRegistro) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it; error = null },
                    label = { Text("Nombre") },
                    leadingIcon = { Icon(Icons.Filled.Person, null) },
                    singleLine = true,
                    colors = colores,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
            }
            OutlinedTextField(
                value = correo,
                onValueChange = { correo = it; error = null; info = null },
                label = { Text("Correo electrónico") },
                leadingIcon = { Icon(Icons.Filled.Email, null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                colors = colores,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = clave,
                onValueChange = { clave = it; error = null },
                label = { Text("Contraseña") },
                leadingIcon = { Icon(Icons.Filled.Lock, null) },
                trailingIcon = {
                    IconButton(onClick = { verClave = !verClave }) {
                        Icon(
                            if (verClave) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            "Mostrar contraseña"
                        )
                    }
                },
                singleLine = true,
                visualTransformation =
                    if (verClave) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                colors = colores,
                modifier = Modifier.fillMaxWidth()
            )

            error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = Peligro, style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center)
            }
            info?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = Menta, style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center)
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    error = validar()
                    info = null
                    if (error == null) {
                        if (modoRegistro)
                            estado.crearCuenta(nombre.trim(), correo.trim(), clave) { error = it }
                        else
                            estado.iniciarSesion(correo.trim(), clave) { error = it }
                    }
                },
                enabled = !estado.cargando,
                colors = ButtonDefaults.buttonColors(containerColor = Menta, contentColor = Fondo),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (estado.cargando) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp), color = Fondo, strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        if (modoRegistro) "Crear cuenta" else "Iniciar sesión",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            if (!modoRegistro) {
                TextButton(onClick = {
                    if (!Patterns.EMAIL_ADDRESS.matcher(correo.trim()).matches()) {
                        error = "Escribe tu correo arriba para recuperar la contraseña"
                    } else {
                        estado.recuperarClave(correo.trim()) { info = it }
                    }
                }) { Text("¿Olvidaste tu contraseña?", color = TextoSecundario) }
            }

            TextButton(onClick = {
                modoRegistro = !modoRegistro
                error = null
                info = null
            }) {
                Text(
                    if (modoRegistro) "¿Ya tienes cuenta? Inicia sesión"
                    else "¿No tienes cuenta? Regístrate",
                    color = Menta
                )
            }
        }
    }
}