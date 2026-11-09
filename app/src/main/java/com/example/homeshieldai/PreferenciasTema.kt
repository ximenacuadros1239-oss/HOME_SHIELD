package com.example.homeshieldai

import android.content.Context
import com.example.homeshieldai.ui.theme.ModoTema

private const val ARCHIVO = "ajustes"
private const val CLAVE = "modo_tema"

fun leerModoTema(context: Context): ModoTema {
    val nombre = context.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)
        .getString(CLAVE, ModoTema.SISTEMA.name) ?: ModoTema.SISTEMA.name
    return runCatching { ModoTema.valueOf(nombre) }.getOrDefault(ModoTema.SISTEMA)
}

fun guardarModoTema(context: Context, modo: ModoTema) {
    context.getSharedPreferences(ARCHIVO, Context.MODE_PRIVATE)
        .edit().putString(CLAVE, modo.name).apply()
}