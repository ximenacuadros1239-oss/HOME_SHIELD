package com.example.homeshieldai

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector

enum class TipoDispositivo(val etiqueta: String, val icono: ImageVector, val potenciaTipica: Int) {
    ESTUFA("Estufa", Icons.Filled.LocalFireDepartment, 1500),
    TERMA("Terma", Icons.Filled.WaterDrop, 1500),
    VENTILADOR("Ventilador", Icons.Filled.Air, 60),
    LUZ("Luz", Icons.Filled.Lightbulb, 15),
    TV("Televisor", Icons.Filled.Tv, 100),
    REFRIGERADORA("Refrigeradora", Icons.Filled.Kitchen, 150),
    COMPUTADORA("Computadora", Icons.Filled.Computer, 120),
    CARGADOR("Cargador", Icons.Filled.PhoneAndroid, 20),
    OTRO("Otro aparato", Icons.Filled.Power, 100)
}

/** El canal lo define el módulo (hardware); la persona solo elige nombre e ícono. */
class Dispositivo(
    val id: Int,
    nombre: String,
    tipo: TipoDispositivo,
    val canal: Int,
    potenciaW: Int,
    encendido: Boolean = false,
    configurado: Boolean = true
) {
    var nombre by mutableStateOf(nombre)
    var tipo by mutableStateOf(tipo)
    var potenciaW by mutableIntStateOf(potenciaW)
    var encendido by mutableStateOf(encendido)
    var configurado by mutableStateOf(configurado)

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