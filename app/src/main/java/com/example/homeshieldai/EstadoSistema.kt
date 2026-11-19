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
import androidx.lifecycle.ViewModel

fun formatoFecha(ts: Long): String {
    val hoy = Calendar.getInstance()
    val f = Calendar.getInstance().apply { timeInMillis = ts }
    val hora = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ts))
    val mismoDia = hoy.get(Calendar.YEAR) == f.get(Calendar.YEAR) &&
            hoy.get(Calendar.DAY_OF_YEAR) == f.get(Calendar.DAY_OF_YEAR)
    return if (mismoDia) "Hoy, $hora"
    else SimpleDateFormat("dd MMM, HH:mm", Locale("es", "PE")).format(Date(ts))
}

class EstadoSistema : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    override fun onCleared() {
        alCambiarTema = null
        alGuardarTarifa = null
        alGuardarConfig = null
        alAlerta = null
        super.onCleared()
    }

    // ---------- Cuenta ----------
    var sesionIniciada by mutableStateOf(auth.currentUser != null)
    var nombre by mutableStateOf("")
    var correo by mutableStateOf("")
    var cargando by mutableStateOf(false)
    var codigoEnviado by mutableStateOf(false)
    private var verificationId: String? = null
    private var tokenReenvio: PhoneAuthProvider.ForceResendingToken? = null
    val uidActual: String get() = auth.currentUser?.uid ?: ""

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
    private var avisoPres80 = false
    private var avisoPres100 = false

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
    var nombreHogar by mutableStateOf("Mi casa")
    var notificaciones by mutableStateOf(true)
    var avisoTemperatura by mutableStateOf(true)
    var avisoPresupuesto by mutableStateOf(true)
    var alGuardarConfig: (() -> Unit)? = null
    fun guardarConfig() { alGuardarConfig?.invoke() }

    // ---------- Tarifa eléctrica ----------
    var tarifa by mutableFloatStateOf(0.50f)
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
    /** (título, mensaje, tono). Lo conecta la app para mostrar la notificación del celular. */
    var alAlerta: ((String, String, Tono) -> Unit)? = null

    val alertas = mutableStateListOf<Alerta>().apply {
        add(Alerta("Sistema normal", "Las mediciones están dentro de los rangos que configuraste.",
            "Hoy, 08:10", Categoria.GENERAL, Tono.NORMAL, Icons.Filled.CheckCircle))
        add(Alerta("Conexión restablecida", "El dispositivo volvió a conectarse correctamente.",
            "Ayer, 18:30", Categoria.GENERAL, Tono.INFO, Icons.Filled.Info))
    }

    private fun agregarAlerta(
        titulo: String,
        mensaje: String,
        categoria: Categoria,
        tono: Tono,
        icono: ImageVector,
        notificar: Boolean = false
    ) {
        alertas.add(
            0,
            Alerta(titulo, mensaje, formatoFecha(System.currentTimeMillis()), categoria, tono, icono)
        )
        if (notificar && notificaciones) alAlerta?.invoke(titulo, mensaje, tono)
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

        revisarTemperatura()
        revisarPresupuesto()
    }

    private fun revisarTemperatura() {
        if (tempTablero >= tempAlerta && !alertaTempActiva) {
            alertaTempActiva = true
            agregarAlerta(
                "Temperatura alta en el tablero",
                "El tablero eléctrico llegó a %.1f °C y tu límite es de %.0f °C. Revisa que tenga buena ventilación."
                    .format(Locale.US, tempTablero, tempAlerta),
                Categoria.TEMPERATURA, Tono.PRECAUCION, Icons.Filled.Thermostat,
                notificar = avisoTemperatura
            )
        } else if (tempTablero < tempAlerta - 2f) {
            alertaTempActiva = false
        }
    }

    private fun revisarPresupuesto() {
        if (presupuesto <= 0f) return
        val gastado = gastoMes
        val razon = gastado / presupuesto

        if (razon >= 1f && !avisoPres100) {
            avisoPres100 = true
            avisoPres80 = true
            agregarAlerta(
                "Superaste tu presupuesto",
                "Este mes ya gastaste S/ %.2f y tu presupuesto era de S/ %.2f (S/ %.2f de más). Revisa qué aparatos puedes apagar."
                    .format(Locale.US, gastado, presupuesto, gastado - presupuesto),
                Categoria.ELECTRICA, Tono.PRECAUCION, Icons.Filled.Bolt,
                notificar = avisoPresupuesto
            )
        } else if (razon >= 0.8f && razon < 1f && !avisoPres80) {
            avisoPres80 = true
            agregarAlerta(
                "Vas al 80 % de tu presupuesto",
                "Llevas gastados S/ %.2f de S/ %.2f este mes. Te quedan S/ %.2f."
                    .format(Locale.US, gastado, presupuesto, presupuesto - gastado),
                Categoria.ELECTRICA, Tono.PRECAUCION, Icons.Filled.Bolt,
                notificar = avisoPresupuesto
            )
        }
        if (razon < 0.75f) avisoPres80 = false
        if (razon < 0.95f) avisoPres100 = false
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
        if (!d.configurado) return
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
        val encendidos = dispositivos.filter { it.encendido }
        if (encendidos.isEmpty()) return
        encendidos.forEach {
            it.encendido = false
            it.apagarEnMs = 0L
        }
        agregarAlerta(
            "Dispositivos apagados",
            "Apagaste ${encendidos.size} dispositivo(s) con «Apagar todo».",
            Categoria.GENERAL, Tono.INFO, Icons.Filled.Info
        )
    }

    fun encenderTodo() {
        if (emergencia || !dispositivoConectado) return
        val apagados = dispositivos.filter { !it.encendido && it.configurado }
        if (apagados.isEmpty()) return
        apagados.forEach { it.encendido = true }
        agregarAlerta(
            "Dispositivos encendidos",
            "Encendiste ${apagados.size} dispositivo(s) con «Encender todo».",
            Categoria.GENERAL, Tono.INFO, Icons.Filled.Info
        )
    }

    fun programarApagado(d: Dispositivo, minutos: Int) {
        d.apagarEnMs = if (minutos > 0) System.currentTimeMillis() + minutos * 60_000L else 0L
    }

    fun configurarDispositivo(d: Dispositivo, nombre: String, tipo: TipoDispositivo, potencia: Int) {
        val eraNuevo = !d.configurado
        d.nombre = nombre
        d.tipo = tipo
        d.potenciaW = potencia
        d.configurado = true
        if (eraNuevo) {
            agregarAlerta(
                "Dispositivo agregado",
                "$nombre ya está listo para usarse desde la app.",
                Categoria.GENERAL, Tono.INFO, Icons.Filled.Info
            )
        }
    }

    /** Demostración: hace lo que hará el módulo cuando detecte un dispositivo nuevo. */
    fun simularDispositivoNuevo() {
        val canal = (dispositivos.maxOfOrNull { it.canal } ?: 0) + 1
        dispositivos.add(
            Dispositivo(proximoId++, "Dispositivo nuevo", TipoDispositivo.OTRO, canal, 100, false, false)
        )
        agregarAlerta(
            "Dispositivo nuevo detectado",
            "Tu módulo detectó un dispositivo nuevo. Dile qué aparato es para poder usarlo.",
            Categoria.GENERAL, Tono.INFO, Icons.Filled.Info,
            notificar = true
        )
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
                "Se detectó una corriente de %.0f A, mayor al límite de %.0f A. Se cortó la energía de tus dispositivos para evitar un incendio. Revisa tus aparatos antes de rearmar."
                    .format(Locale.US, nuevaCorriente, limiteCorriente),
                Categoria.ELECTRICA, Tono.PELIGRO, Icons.Filled.Bolt,
                notificar = true
            )
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
    fun simularCortocircuito() = registrarLectura(maxOf(18f, limiteCorriente + 2f), tempTablero)
    fun simularLecturas() { if (emergencia) rearmar() }
    fun alternarConexion() { dispositivoConectado = !dispositivoConectado }
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