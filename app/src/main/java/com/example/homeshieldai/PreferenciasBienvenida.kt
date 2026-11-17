package com.example.homeshieldai

import android.content.Context

private const val ARCHIVO = "bienvenida"

fun bienvenidaVista(context: Context, uid: String): Boolean =
    context.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE).getBoolean("vista_$uid", false)

fun marcarBienvenidaVista(context: Context, uid: String) {
    context.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE).edit()
        .putBoolean("vista_$uid", true).apply()
}