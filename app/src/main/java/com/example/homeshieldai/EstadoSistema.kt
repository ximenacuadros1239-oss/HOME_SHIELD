package com.example.homeshieldai

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.example.homeshieldai.ui.theme.Ambar
import com.example.homeshieldai.ui.theme.Info
import com.example.homeshieldai.ui.theme.Menta
import com.example.homeshieldai.ui.theme.Peligro
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.UserProfileChangeRequest
import java.util.Locale
import com.google.firebase.auth.GoogleAuthProvider
import android.app.Activity
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

class EstadoSistema {
    private val auth = FirebaseAuth.getInstance()

    // Cuenta
    var sesionIniciada by mutableStateOf(auth.currentUser != null)
    var nombre by mutableStateOf("")
    var correo by mutableStateOf("")
    var nombreHogar by mutableStateOf("Mi casa")
    var cargando by mutableStateOf(false)
    var codigoEnviado by mutableStateOf(false)
    private var verificationId: String? = null
    private var tokenReenvio: PhoneAuthProvider.ForceResendingToken? = null

    // Lecturas
    var tempSala by mutableFloatStateOf(24.5f)
    var tempTablero by mutableFloatStateOf(32.0f)
    var corriente by mutableFloatStateOf(0.57f)
    var releEncendido by mutableStateOf(true)
    var emergencia by mutableStateOf(false)

    // Ajustes
    var tempAlerta by mutableFloatStateOf(35f)
    var limiteCorriente by mutableFloatStateOf(16f)
    var presupuesto by mutableFloatStateOf(80f)
    var notificaciones by mutableStateOf(true)

    var alAlerta: ((String) -> Unit)? = null

    val alertas = mutableStateListOf<Alerta>().apply { addAll(alertasIniciales) }

    val potencia: Float
        get() = if (emergencia || !releEncendido) 0f else corriente * 221f

    init { cargarUsuario() }

    fun cargarUsuario() {
        val u = auth.currentUser
        correo = u?.email ?: u?.phoneNumber ?: ""
        nombre = u?.displayName?.takeIf { it.isNotBlank() }
            ?: u?.email?.substringBefore("@")
                    ?: if (u?.phoneNumber != null) "Usuario" else ""
    }

    fun iniciarSesion(correo: String, clave: String, onError: (String) -> Unit) {
        cargando = true
        auth.signInWithEmailAndPassword(correo, clave).addOnCompleteListener { tarea ->
            cargando = false
            if (tarea.isSuccessful) {
                cargarUsuario()
                sesionIniciada = true
            } else onError(mensajeError(tarea.exception))
        }
    }

    fun entrarConGoogle(idToken: String, onError: (String) -> Unit) {
        cargando = true
        val credencial = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credencial).addOnCompleteListener { tarea ->
            cargando = false
            if (tarea.isSuccessful) {
                cargarUsuario()
                sesionIniciada = true
            } else onError(mensajeError(tarea.exception))
        }
    }

    fun enviarCodigo(
        activity: Activity,
        telefono: String,
        reenviar: Boolean,
        onError: (String) -> Unit
    ) {
        cargando = true
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credencial: PhoneAuthCredential) {
                entrarConCredencial(credencial, false, onError)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                cargando = false
                onError(mensajeTelefono(e))
            }

            override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                cargando = false
                verificationId = id
                tokenReenvio = token
                codigoEnviado = true
            }
        }
        val opciones = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(telefono)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
        if (reenviar) tokenReenvio?.let { opciones.setForceResendingToken(it) }
        PhoneAuthProvider.verifyPhoneNumber(opciones.build())
    }

    fun verificarCodigo(codigo: String, onError: (String) -> Unit) {
        val id = verificationId
        if (id == null) {
            onError("Solicita primero el código")
            return
        }
        entrarConCredencial(PhoneAuthProvider.getCredential(id, codigo), true, onError)
    }

    fun reiniciarTelefono() {
        codigoEnviado = false
        verificationId = null
        tokenReenvio = null
    }

    private fun entrarConCredencial(
        credencial: AuthCredential,
        esCodigo: Boolean,
        onError: (String) -> Unit
    ) {
        cargando = true
        auth.signInWithCredential(credencial).addOnCompleteListener { tarea ->
            cargando = false
            if (tarea.isSuccessful) {
                cargarUsuario()
                codigoEnviado = false
                sesionIniciada = true
            } else {
                val e = tarea.exception
                onError(
                    if (esCodigo && e is FirebaseAuthInvalidCredentialsException)
                        "Código incorrecto o vencido"
                    else mensajeError(e)
                )
            }
        }
    }

    private fun mensajeTelefono(e: FirebaseException): String = when (e) {
        is FirebaseAuthInvalidCredentialsException -> "El número de teléfono no es válido"
        is FirebaseTooManyRequestsException -> "Se superó el límite de SMS. Intenta más tarde"
        is FirebaseNetworkException -> "Sin conexión a internet"
        else -> "No se pudo enviar el código: ${e.message}"
    }

    fun crearCuenta(nombre: String, correo: String, clave: String, onError: (String) -> Unit) {
        cargando = true
        auth.createUserWithEmailAndPassword(correo, clave).addOnCompleteListener { tarea ->
            if (tarea.isSuccessful) {
                val perfil = UserProfileChangeRequest.Builder().setDisplayName(nombre).build()
                val actualizar = tarea.result.user?.updateProfile(perfil)
                if (actualizar != null) {
                    actualizar.addOnCompleteListener {
                        cargando = false
                        cargarUsuario()
                        sesionIniciada = true
                    }
                } else {
                    cargando = false
                    cargarUsuario()
                    sesionIniciada = true
                }
            } else {
                cargando = false
                onError(mensajeError(tarea.exception))
            }
        }
    }

    fun recuperarClave(correo: String, onResultado: (String) -> Unit) {
        auth.sendPasswordResetEmail(correo).addOnCompleteListener { tarea ->
            onResultado(
                if (tarea.isSuccessful) "Te enviamos un correo para restablecer tu contraseña."
                else mensajeError(tarea.exception)
            )
        }
    }

    fun cambiarNombre(nuevo: String) {
        nombre = nuevo
        val perfil = UserProfileChangeRequest.Builder().setDisplayName(nuevo).build()
        auth.currentUser?.updateProfile(perfil)
    }

    fun cerrarSesion() {
        auth.signOut()
        sesionIniciada = false
    }

    private fun mensajeError(e: Exception?): String = when (e) {
        is FirebaseAuthWeakPasswordException -> "La contraseña es muy débil (mínimo 6 caracteres)"
        is FirebaseAuthUserCollisionException -> "Ya existe una cuenta con ese correo"
        is FirebaseAuthInvalidUserException -> "No existe una cuenta con ese correo"
        is FirebaseAuthInvalidCredentialsException -> "Correo o contraseña incorrectos"
        is FirebaseNetworkException -> "Sin conexión a internet"
        else -> "No se pudo completar la operación. Intenta de nuevo."
    }

    // Aquí llegarán las lecturas reales del ESP32
    fun registrarLectura(nuevaCorriente: Float, nuevoTablero: Float) {
        tempTablero = nuevoTablero
        if (nuevaCorriente > limiteCorriente && !emergencia) {
            emergencia = true
            releEncendido = false
            corriente = 0f
            alertas.add(
                0,
                Alerta(
                    "Posible cortocircuito",
                    "Se detectó un consumo anormal (%.1f A). El relé se apagó por seguridad."
                        .format(Locale.US, nuevaCorriente),
                    "Ahora", Categoria.ELECTRICA, Peligro, Icons.Filled.Bolt
                )
            )
            if (notificaciones) {
                alAlerta?.invoke(
                    "Se detectó un consumo anormal (%.0f A). El relé se ha apagado automáticamente para evitar un incendio. Revisa tus aparatos."
                        .format(Locale.US, nuevaCorriente)
                )
            }
        } else if (!emergencia) {
            corriente = nuevaCorriente
        }
    }

    fun rearmar() {
        emergencia = false
        corriente = 0.57f
        alertas.add(
            0,
            Alerta(
                "Sistema rearmado",
                "El sistema volvió a operar. Enciende el relé cuando estés listo.",
                "Ahora", Categoria.GENERAL, Info, Icons.Filled.Info
            )
        )
    }
}

fun estadoTemp(temp: Float, alerta: Float): Pair<String, Color> = when {
    temp >= alerta -> "Peligro" to Peligro
    temp >= alerta - 2f -> "Precaución" to Ambar
    else -> "Normal" to Menta
}