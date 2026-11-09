package com.example.homeshieldai

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Patterns
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.homeshieldai.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun Context.buscarActividad(): Activity? {
    var c = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

@Composable
fun PantallaLogin(estado: EstadoSistema) {
    val context = LocalContext.current
    val actividad = remember(context) { context.buscarActividad() }
    val scope = rememberCoroutineScope()

    var metodo by remember { mutableIntStateOf(0) }
    var modoRegistro by remember { mutableStateOf(false) }
    var nombre by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var clave by remember { mutableStateOf("") }
    var verClave by remember { mutableStateOf(false) }
    var telefono by remember { mutableStateOf("") }
    var codigo by remember { mutableStateOf("") }
    var segundos by remember { mutableIntStateOf(0) }
    var ciclo by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(estado.codigoEnviado, ciclo) {
        if (estado.codigoEnviado) {
            segundos = 60
            while (segundos > 0) {
                delay(1000)
                segundos--
            }
        }
    }

    val colores = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Menta,
        unfocusedBorderColor = Borde,
        focusedLabelColor = Menta,
        unfocusedLabelColor = TextoSecundario,
        cursorColor = Menta,
        focusedTextColor = TextoPrincipal,
        unfocusedTextColor = TextoPrincipal
    )

    fun validarCorreo(): String? = when {
        modoRegistro && nombre.isBlank() -> "Ingresa tu nombre"
        !Patterns.EMAIL_ADDRESS.matcher(correo.trim()).matches() -> "Ingresa un correo válido"
        clave.length < 6 -> "La contraseña debe tener al menos 6 caracteres"
        else -> null
    }

    fun telefonoValido() = telefono.length == 9 && telefono.startsWith("9")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(FondoAlto, Fondo)))
    ) {
        val brillo = Menta.copy(alpha = 0.22f)
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
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(Menta.copy(alpha = 0.35f), MentaOscuro.copy(alpha = 0.2f))
                        )
                    )
                    .border(1.dp, Menta.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.VerifiedUser, null, tint = Menta, modifier = Modifier.size(54.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "Home Shield AI",
                style = MaterialTheme.typography.headlineMedium,
                color = TextoPrincipal
            )
            Text(
                "Tu hogar más seguro, siempre.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextoSecundario
            )
            Spacer(Modifier.height(24.dp))

            TarjetaVidrio(modifier = Modifier.fillMaxWidth()) {
                SelectorMetodo(metodo) {
                    metodo = it
                    error = null
                    info = null
                }
                Spacer(Modifier.height(16.dp))

                if (metodo == 0) {
                    // ----- CORREO -----
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
                    Mensajes(error, info)
                    Spacer(Modifier.height(16.dp))
                    BotonPrincipal(
                        texto = if (modoRegistro) "Crear cuenta" else "Iniciar sesión",
                        cargando = estado.cargando
                    ) {
                        error = validarCorreo()
                        info = null
                        if (error == null) {
                            if (modoRegistro)
                                estado.crearCuenta(nombre.trim(), correo.trim(), clave) { error = it }
                            else
                                estado.iniciarSesion(correo.trim(), clave) { error = it }
                        }
                    }
                    if (!modoRegistro) {
                        TextButton(
                            onClick = {
                                if (!Patterns.EMAIL_ADDRESS.matcher(correo.trim()).matches()) {
                                    error = "Escribe tu correo arriba para recuperar la contraseña"
                                } else {
                                    estado.recuperarClave(correo.trim()) { info = it }
                                }
                            },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) { Text("¿Olvidaste tu contraseña?", color = TextoSecundario) }
                    }
                } else if (!estado.codigoEnviado) {
                    // ----- TELÉFONO: pedir número -----
                    OutlinedTextField(
                        value = telefono,
                        onValueChange = { telefono = it.filter(Char::isDigit).take(9); error = null },
                        label = { Text("Número de celular") },
                        leadingIcon = { Icon(Icons.Filled.Phone, null) },
                        prefix = { Text("+51 ") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        colors = colores,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Mensajes(error, info)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Te enviaremos un código por SMS. Pueden aplicar tarifas de tu operador.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextoSecundario
                    )
                    Spacer(Modifier.height(16.dp))
                    BotonPrincipal("Enviar código", estado.cargando) {
                        error = null
                        if (!telefonoValido()) {
                            error = "Ingresa un celular de 9 dígitos (empieza con 9)"
                        } else if (actividad != null) {
                            codigo = ""
                            estado.enviarCodigo(actividad, "+51$telefono", false) { error = it }
                        }
                    }
                } else {
                    // ----- TELÉFONO: ingresar código -----
                    Text(
                        "Ingresa el código enviado a +51 $telefono",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(Modifier.height(14.dp))
                    CampoCodigo(codigo) { codigo = it; error = null }
                    Mensajes(error, info)
                    Spacer(Modifier.height(16.dp))
                    BotonPrincipal("Verificar y entrar", estado.cargando) {
                        if (codigo.length < 6) error = "Ingresa los 6 dígitos"
                        else estado.verificarCodigo(codigo) { error = it }
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = {
                            codigo = ""
                            error = null
                            estado.reiniciarTelefono()
                        }) { Text("Cambiar número", color = TextoSecundario) }
                        TextButton(
                            enabled = segundos == 0 && !estado.cargando,
                            onClick = {
                                if (actividad != null) {
                                    error = null
                                    ciclo++
                                    estado.enviarCodigo(actividad, "+51$telefono", true) { error = it }
                                }
                            }
                        ) {
                            Text(
                                if (segundos > 0) "Reenviar en ${segundos}s" else "Reenviar código",
                                color = if (segundos > 0) TextoSecundario else Menta
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f).height(1.dp).background(Borde))
                Text(
                    "  o continúa con  ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextoSecundario
                )
                Box(Modifier.weight(1f).height(1.dp).background(Borde))
            }
            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = {
                    error = null
                    info = null
                    scope.launch {
                        when (val r = pedirTokenGoogle(context)) {
                            is ResultadoGoogle.Token ->
                                estado.entrarConGoogle(r.idToken) { error = it }
                            is ResultadoGoogle.Error -> error = r.mensaje
                            ResultadoGoogle.Cancelado -> Unit
                        }
                    }
                },
                enabled = !estado.cargando,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Borde),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Icon(Icons.Filled.AccountCircle, null, tint = Menta)
                Spacer(Modifier.width(10.dp))
                Text(
                    "Google",
                    color = TextoPrincipal,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (metodo == 0) {
                Spacer(Modifier.height(8.dp))
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
}

@Composable
private fun SelectorMetodo(seleccion: Int, onSeleccionar: (Int) -> Unit) {
    val opciones = listOf("Correo" to Icons.Filled.Email, "Teléfono" to Icons.Filled.Phone)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(Fondo.copy(alpha = 0.5f))
            .padding(4.dp)
    ) {
        opciones.forEachIndexed { i, (texto, icono) ->
            val activa = i == seleccion
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(if (activa) Menta else Color.Transparent)
                    .clickable { onSeleccionar(i) }
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icono, null, tint = if (activa) Fondo else TextoSecundario,
                    modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    texto,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (activa) Fondo else TextoPrincipal
                )
            }
        }
    }
}

@Composable
private fun Mensajes(error: String?, info: String?) {
    error?.let {
        Spacer(Modifier.height(10.dp))
        Text(it, color = Peligro, style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
    info?.let {
        Spacer(Modifier.height(10.dp))
        Text(it, color = Menta, style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun BotonPrincipal(texto: String, cargando: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !cargando,
        colors = ButtonDefaults.buttonColors(containerColor = Menta, contentColor = Fondo),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().height(52.dp)
    ) {
        if (cargando) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp), color = Fondo, strokeWidth = 2.dp
            )
        } else {
            Text(texto, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun CampoCodigo(valor: String, onCambio: (String) -> Unit) {
    val foco = remember { FocusRequester() }
    LaunchedEffect(Unit) { foco.requestFocus() }
    BasicTextField(
        value = valor,
        onValueChange = { if (it.length <= 6 && it.all(Char::isDigit)) onCambio(it) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        modifier = Modifier.focusRequester(foco),
        decorationBox = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(6) { i ->
                    val forma = RoundedCornerShape(12.dp)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .clip(forma)
                            .background(Superficie)
                            .border(1.dp, if (i == valor.length) Menta else Borde, forma),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            valor.getOrNull(i)?.toString() ?: "",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextoPrincipal
                        )
                    }
                }
            }
        }
    )
}