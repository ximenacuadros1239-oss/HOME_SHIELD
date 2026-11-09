package com.example.homeshieldai

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import com.example.homeshieldai.ui.theme.*
import java.util.Locale

class EstadoSistema {
    var tempSala by mutableFloatStateOf(24.5f)
    var tempTablero by mutableFloatStateOf(32.0f)
    var corriente by mutableFloatStateOf(0.57f)
    var releEncendido by mutableStateOf(true)
    var emergencia by mutableStateOf(false)

    var tempAlerta by mutableFloatStateOf(35f)
    var limiteCorriente by mutableFloatStateOf(16f)
    var presupuesto by mutableFloatStateOf(80f)
    var notificaciones by mutableStateOf(true)

    var alAlerta: ((String) -> Unit)? = null

    val alertas = mutableStateListOf<Alerta>().apply { addAll(alertasIniciales) }

    val potencia: Float
        get() = if (emergencia || !releEncendido) 0f else corriente * 221f

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