package com.example.homeshieldai

import android.content.Context

private const val ARCHIVO = "tarifa"

fun cargarTarifa(context: Context, estado: EstadoSistema) {
    val p = context.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)
    if (!p.getBoolean("configurada", false)) return
    estado.departamento = p.getString("depto", "") ?: ""
    estado.distribuidora = p.getString("distri", "") ?: ""
    estado.tarifa = p.getFloat("tarifa", estado.tarifa)
    estado.cargoFijo = p.getFloat("fijo", 0f)
    estado.alumbrado = p.getFloat("alumbrado", 0f)
    estado.diaCiclo = p.getInt("dia", 1)
    estado.tarifaConfigurada = true
}

fun guardarTarifa(context: Context, estado: EstadoSistema) {
    context.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE).edit()
        .putBoolean("configurada", true)
        .putString("depto", estado.departamento)
        .putString("distri", estado.distribuidora)
        .putFloat("tarifa", estado.tarifa)
        .putFloat("fijo", estado.cargoFijo)
        .putFloat("alumbrado", estado.alumbrado)
        .putInt("dia", estado.diaCiclo)
        .apply()
}