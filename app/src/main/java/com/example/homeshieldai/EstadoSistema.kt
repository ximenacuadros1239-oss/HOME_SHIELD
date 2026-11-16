package com.example.homeshieldai

import android.app.Activity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.homeshieldai.ui.theme.Ambar
import com.example.homeshieldai.ui.theme.Menta
import com.example.homeshieldai.ui.theme.ModoTema
import com.example.homeshieldai.ui.theme.Peligro
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.random.Random

fun formatoFecha(ts: Long): String {
    val hoy = Calendar.getInstance()
    val f = Calendar.getInstance().apply { timeInMillis = ts }
    val hora = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ts))
    val mismoDia = hoy.get(Calendar.YEAR) == f.get(Calendar.YEAR) &&
            hoy.get(Calendar.DAY_OF_YEAR) == f.get(Calendar.DAY_OF_YEAR)
    return if (mismoDia) "Hoy, $hora"
    else SimpleDateFormat("dd MMM, HH:mm", Locale("es", "PE")).format(Date(ts))
}

class EstadoSistema {
    private val auth = FirebaseAuth.getInstance()

    // ---------- Cuenta ----------
    var sesionIniciada by mutableStateOf(auth.currentUser != null)
    var nombre by mutableStateOf("")
    var correo by mutableStateOf("")
    var cargando by mutableStateOf(false)
    var codigoEnviado by mutableStateOf(false)
    private var verificationId: String? = null
    private var tokenReenvio: PhoneAuthProvider.ForceResendingToken? = null

    // ---------- Apariencia ----------
    var modoTema by mutableStateOf(ModoTema.SISTEMA)
    var alCambiarTema: ((ModoTema) -> Unit)? = null

    // ---------- Lecturas (de demostración) ----------
    var tempSala by mutableFloatStateOf(24.5f)
    var tempTablero by mutableFloatStateOf(31.0f)
    var corriente by mutableFloatStateOf(0.57f)
    var voltaje by mutableFloatStateOf(221f)
    var potencia by mutableFloatStateOf(126f)
    var energiaHoy by mutableFloatStateOf(3.8f)
    var emergencia by mutableStateOf(false)
    var rearmePendiente by mutableStateOf(false)
    var dispositivoConectado by mutableStateOf(true)
    var ahora by mutableLongStateOf(System.currentTimeMillis())
    var errorSync by mutableStateOf<String?>(null)
    private var alertaTempActiva = false

    val histSala = mutableStateListOf<Float>()
    val histTablero = mutableStateListOf<Float>()
    val histPotencia = mutableStateListOf<Float>()

    // ---------- Dispositivos controlables ----------
    val dispositivos = mutableStateListOf(
        Dispositivo(1, "Estufa", TipoDispositivo.ESTUFA, 1, 1500, true),
        Dispositivo(2, "Ventilador", TipoDispositivo.VENTILADOR, 2, 60, false)
    )
    private var proximoId = 3

    val releEncendido: Boolean get() = dispositivos.any { it.encendido }

    fun dispositivo(id: Int): Dispositivo? = dispositivos.firstOrNull { it.id == id }

    // ---------- Configuración ----------
    var tempAlerta by mutableFloatStateOf(35f)
    var limiteCorriente by mutableFloatStateOf(16f)
    var presupuesto by mutableFloatStateOf(80f)
    var tarifa by mutableFloatStateOf(0.50f)
    var nombreHogar by mutableStateOf("Mi casa")
    var notificaciones by mutableStateOf(true)

    // ---------- Tarifa eléctrica ----------
    var departamento by mutableStateOf("")
    var distribuidora by mutableStateOf("")
    var cargoFijo by mutableFloatStateOf(0f)
    var alumbrado by mutableFloatStateOf(0f)
    var diaCiclo by mutableIntStateOf(1)
    var tarifaConfigurada by mutableStateOf(false)
    var alGuardarTarifa: (() -> Unit)? = null
    val igv = 0.18f

    var energiaMesPrevio by mutableFloatStateOf(92.6f)
    val energiaMes: Float get() = energiaMesPrevio + energiaHoy

    fun costoEnergia(kWh: Float): Float = kWh * tarifa
    fun costoTotal(kWh: Float): Float = (costoEnergia(kWh) + cargoFijo + alumbrado) * (1 + igv)

    val gastoHoy: Float get() = costoEnergia(energiaHoy) * (1 + igv)
    val gastoMes: Float get() = costoTotal(energiaMes)

    fun guardarTarifa() {
        tarifaConfigurada = true
        alGuardarTarifa?.invoke()
    }

    // ---------- Alertas ----------
    var alAlerta: ((String) -> Unit)? = null
    val alertas = mutableStateListOf<Alerta>().apply {
        add(Alerta("Consumo elevado", "Estás al 80% de tu límite mensual de electricidad.",
            "Hoy, 09:40", Categoria.ELECTRICA, Tono.PRECAUCION, Icons.Filled.Bolt))
        add(Alerta("Sistema normal", "Las mediciones se encuentran dentro de los rangos configurados.",
            "Hoy, 08:10", Categoria.GENERAL, Tono.NORMAL, Icons.Filled.CheckCircle))
        add(Alerta("Conexión restablecida", "El dispositivo volvió a conectarse correctamente.",
            "Ayer, 18:30", Categoria.GENERAL, Tono.INFO, Icons.Filled.Info))
    }

    private fun agregarAlerta(
        titulo: String, mensaje: String, categoria: Categoria, tono: Tono, icono: ImageVector
    ) {
        alertas.add(0, Alerta(titulo, mensaje, formatoFecha(System.currentTimeMillis()),
            categoria, tono, icono))
    }

    init {
        cargarUsuario()
        var s = tempSala
        var t = tempTablero
        repeat(40) {
            s = (s + Random.nextFloat() * 0.4f - 0.2f).coerceIn(22f, 27f)
            t = (t + Random.nextFloat() * 0.6f - 0.3f).coerceIn(29f, 33.5f)
            histSala.add(s)
            histTablero.add(t)
            histPotencia.add(110f + Random.nextFloat() * 40f)
        }
    }

    private fun registrarHistorial() {
        fun agregar(lista: MutableList<Float>, valor: Float) {
            lista.add(valor)
            if (lista.size > 60) lista.removeAt(0)
        }
        agregar(histSala, tempSala)
        agregar(histTablero, tempTablero)
        agregar(histPotencia, potencia)
    }

    // ---------- Motor de demostración ----------
    fun tick() {
        ahora = System.currentTimeMillis()
        if (!dispositivoConectado) return

        tempSala = (tempSala + Random.nextFloat() * 0.3f - 0.15f).coerceIn(22f, 27f)
        tempTablero = (tempTablero + Random.nextFloat() * 0.5f - 0.25f).coerceIn(29f, 33.5f)
        voltaje = 219f + Random.nextFloat() * 4f

        revisarAutomatismos()

        val carga = if (emergencia) 0 else
            6 + dispositivos.filter { it.encendido }.sumOf { it.potenciaW }
        potencia = if (emergencia) 0f else carga * (0.97f + Random.nextFloat() * 0.06f)
        corriente = potencia / voltaje
        energiaHoy += potencia / 1000f * (2f / 3600f)
        registrarHistorial()

        if (tempTablero >= tempAlerta && !alertaTempActiva) {
            alertaTempActiva = true
            agregarAlerta(
                "Temperatura alta",
                "El tablero eléctrico superó los %.0f °C (%.1f °C)."
                    .format(Locale.US, tempAlerta, tempTablero),
                Categoria.TEMPERATURA, Tono.PRECAUCION, Icons.Filled.Thermostat
            )
        } else if (tempTablero < tempAlerta - 2f) {
            alertaTempActiva = false
        }
    }

    private fun revisarAutomatismos() {
        if (emergencia) return
        val cal = Calendar.getInstance()
        val minuto = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val dia = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7
        val clave = cal.get(Calendar.DAY_OF_YEAR) * 1440 + minuto

        dispositivos.forEach { d ->
            if (d.apagarEnMs in 1..ahora) {
                d.apagarEnMs = 0L
                if (d.encendido) {
                    d.encendido = false
                    agregarAlerta(
                        "${d.nombre} apagado",
                        "El temporizador terminó y se apagó automáticamente.",
                        Categoria.GENERAL, Tono.INFO, Icons.Filled.Info
                    )
                }
            }
            if (d.programado && d.dias[dia] && d.ultimaEjecucion != clave) {
                when (minuto) {
                    d.minutoEncendido -> {
                        d.ultimaEjecucion = clave
                        if (!d.encendido) {
                            d.encendido = true
                            agregarAlerta(
                                "${d.nombre} encendido",
                                "Se encendió según su programación (${minutosATexto(minuto)}).",
                                Categoria.GENERAL, Tono.INFO, Icons.Filled.Info
                            )
                        }
                    }
                    d.minutoApagado -> {
                        d.ultimaEjecucion = clave
                        if (d.encendido) {
                            d.encendido = false
                            d.apagarEnMs = 0L
                            agregarAlerta(
                                "${d.nombre} apagado",
                                "Se apagó según su programación (${minutosATexto(minuto)}).",
                                Categoria.GENERAL, Tono.INFO, Icons.Filled.Info
                            )
                        }
                    }
                }
            }
        }
    }

    // ---------- Control de dispositivos ----------
    fun alternarDispositivo(d: Dispositivo, encender: Boolean) {
        if (emergencia || !dispositivoConectado) return
        d.encendido = encender
        if (!encender) d.apagarEnMs = 0L
        agregarAlerta(
            if (encender) "${d.nombre} encendido" else "${d.nombre} apagado",
            if (encender) "Encendiste ${d.nombre} desde la app." else "Apagaste ${d.nombre} desde la app.",
            Categoria.GENERAL, Tono.INFO, Icons.Filled.Info
        )
    }

    fun apagarTodo() {
        if (!dispositivoConectado) return
        val apagados = dispositivos.filter { it.encendido }
        if (apagados.isEmpty()) return
        apagados.forEach {
            it.encendido = false
            it.apagarEnMs = 0L
        }
        agregarAlerta(
            "Dispositivos apagados",
            "Apagaste ${apagados.size} dispositivo(s) con «Apagar todo».",
            Categoria.GENERAL, Tono.INFO, Icons.Filled.Info
        )
    }

    fun programarApagado(d: Dispositivo, minutos: Int) {
        d.apagarEnMs = if (minutos > 0) System.currentTimeMillis() + minutos * 60_000L else 0L
    }

    fun agregarDispositivo(nombre: String, tipo: TipoDispositivo, canal: Int, potencia: Int) {
        dispositivos.add(Dispositivo(proximoId++, nombre, tipo, canal, potencia))
    }

    fun eliminarDispositivo(d: Dispositivo) {
        dispositivos.remove(d)
    }

    // ---------- Emergencia ----------
    fun registrarLectura(nuevaCorriente: Float, nuevoTablero: Float) {
        tempTablero = nuevoTablero
        if (nuevaCorriente > limiteCorriente && !emergencia) {
            emergencia = true
            dispositivos.forEach {
                it.encendido = false
                it.apagarEnMs = 0L
            }
            corriente = 0f
            potencia = 0f
            agregarAlerta(
                "Posible cortocircuito",
                "Se detectó un consumo anormal (%.1f A). Los dispositivos se apagaron por seguridad."
                    .format(Locale.US, nuevaCorriente),
                Categoria.ELECTRICA, Tono.PELIGRO, Icons.Filled.Bolt
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
        rearmePendiente = false
        agregarAlerta(
            "Sistema rearmado",
            "El sistema volvió a operar. Enciende los dispositivos cuando estés listo.",
            Categoria.GENERAL, Tono.INFO, Icons.Filled.Info
        )
    }

    fun solicitarRearme() = rearmar()
    fun simularCortocircuito() = registrarLectura(18f, tempTablero)
    fun simularLecturas() { if (emergencia) rearmar() }
    fun alternarConexion() { dispositivoConectado = !dispositivoConectado }
    fun guardarConfig() { /* Por ahora los ajustes viven en memoria; luego se guardarán. */ }
    fun iniciarSincronizacion() {}
    fun detenerSincronizacion() {}

    fun cambiarTema(modo: ModoTema) {
        modoTema = modo
        alCambiarTema?.invoke(modo)
    }

    // ---------- Cuenta ----------
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

    fun entrarConGoogle(idToken: String, onError: (String) -> Unit) {
        entrarConCredencial(GoogleAuthProvider.getCredential(idToken, null), false, onError)
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

    fun enviarCodigo(
        activity: Activity, telefono: String, reenviar: Boolean, onError: (String) -> Unit
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
        credencial: AuthCredential, esCodigo: Boolean, onError: (String) -> Unit
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
        else -> "No se pudo enviar el código. Intenta de nuevo."
    }

    private fun mensajeError(e: Exception?): String = when (e) {
        is FirebaseAuthWeakPasswordException -> "La contraseña es muy débil (mínimo 6 caracteres)"
        is FirebaseAuthUserCollisionException -> "Ya existe una cuenta con ese correo"
        is FirebaseAuthInvalidUserException -> "No existe una cuenta con ese correo"
        is FirebaseAuthInvalidCredentialsException -> "Correo o contraseña incorrectos"
        is FirebaseNetworkException -> "Sin conexión a internet"
        else -> "No se pudo completar la operación. Intenta de nuevo."
    }
}

@Composable
fun estadoTemp(temp: Float, alerta: Float): Pair<String, Color> = when {
    temp >= alerta -> "Peligro" to Peligro
    temp >= alerta - 2f -> "Precaución" to Ambar
    else -> "Normal" to Menta
}