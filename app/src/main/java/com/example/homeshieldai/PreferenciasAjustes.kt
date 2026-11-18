package com.example.homeshieldai

import android.content.Context

private const val ARCHIVO = "ajustes_app"

fun cargarAjustes(context: Context, e: EstadoSistema) {
    val p = context.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)
    e.tempAlerta = p.getFloat("tempAlerta", e.tempAlerta)
    e.limiteCorriente = p.getFloat("limiteCorriente", e.limiteCorriente)
    e.presupuesto = p.getFloat("presupuesto", e.presupuesto)
    e.nombreHogar = p.getString("nombreHogar", e.nombreHogar) ?: e.nombreHogar
    e.notificaciones = p.getBoolean("notificaciones", e.notificaciones)
    e.avisoTemperatura = p.getBoolean("avisoTemperatura", e.avisoTemperatura)
    e.avisoPresupuesto = p.getBoolean("avisoPresupuesto", e.avisoPresupuesto)
}

fun guardarAjustes(context: Context, e: EstadoSistema) {
    context.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE).edit()
        .putFloat("tempAlerta", e.tempAlerta)
        .putFloat("limiteCorriente", e.limiteCorriente)
        .putFloat("presupuesto", e.presupuesto)
        .putString("nombreHogar", e.nombreHogar)
        .putBoolean("notificaciones", e.notificaciones)
        .putBoolean("avisoTemperatura", e.avisoTemperatura)
        .putBoolean("avisoPresupuesto", e.avisoPresupuesto)
        .apply()
}