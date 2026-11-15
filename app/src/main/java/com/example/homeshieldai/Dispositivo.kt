package com.example.homeshieldai

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector

enum class TipoDispositivo(val etiqueta: String, val icono: ImageVector) {
    ESTUFA("Estufa", Icons.Filled.LocalFireDepartment),
    VENTILADOR("Ventilador", Icons.Filled.Air),
    LUZ("Luz", Icons.Filled.Lightbulb),
    TERMA("Terma", Icons.Filled.WaterDrop),
    OTRO("Otro", Icons.Filled.Power)
}

class Dispositivo(
    val id: Int,
    nombre: String,
    tipo: TipoDispositivo,
    canal: Int,
    potenciaW: Int,
    encendido: Boolean = false
) {
    var nombre by mutableStateOf(nombre)
    var tipo by mutableStateOf(tipo)
    var canal by mutableIntStateOf(canal)
    var potenciaW by mutableIntStateOf(potenciaW)
    var encendido by mutableStateOf(encendido)

    // Temporizador: hora (en ms) en que se apagará. 0 = sin temporizador
    var apagarEnMs by mutableLongStateOf(0L)

    // Programación semanal (minutos desde las 00:00)
    var programado by mutableStateOf(false)
    var minutoEncendido by mutableIntStateOf(18 * 60)
    var minutoApagado by mutableIntStateOf(22 * 60)
    val dias = mutableStateListOf(true, true, true, true, true, true, true) // Lun..Dom

    var ultimaEjecucion = -1
}

fun minutosATexto(m: Int): String = "%02d:%02d".format(m / 60, m % 60)

fun minutosRestantes(d: Dispositivo, ahora: Long): Int =
    (((d.apagarEnMs - ahora).coerceAtLeast(0L) + 59_999L) / 60_000L).toInt()