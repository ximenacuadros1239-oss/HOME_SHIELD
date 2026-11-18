package com.example.homeshieldai

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

const val CANAL_PELIGRO = "alertas_peligro"
const val CANAL_AVISO = "alertas_aviso"
private var contador = 2000

fun crearCanalesNotificacion(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CANAL_PELIGRO, "Emergencias", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Cortocircuitos y cortes de emergencia"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 400, 200, 400)
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CANAL_AVISO, "Avisos", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Temperatura alta y presupuesto de luz"
                enableVibration(true)
            }
        )
    }
}

fun permisoNotificaciones(context: Context): Boolean {
    val permiso = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    return permiso && NotificationManagerCompat.from(context).areNotificationsEnabled()
}

@SuppressLint("MissingPermission")
fun mostrarNotificacion(context: Context, titulo: String, mensaje: String, tono: Tono): Boolean {
    if (!permisoNotificaciones(context)) return false
    val peligro = tono == Tono.PELIGRO

    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val pendiente = PendingIntent.getActivity(
        context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    val notificacion = NotificationCompat.Builder(context, if (peligro) CANAL_PELIGRO else CANAL_AVISO)
        .setSmallIcon(android.R.drawable.ic_dialog_alert)
        .setContentTitle(titulo)
        .setContentText(mensaje)
        .setStyle(NotificationCompat.BigTextStyle().bigText(mensaje))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setCategory(if (peligro) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_STATUS)
        .setColor(if (peligro) 0xFFE04A55.toInt() else 0xFFF2B33D.toInt())
        .setVibrate(longArrayOf(0, 400, 200, 400))
        .setContentIntent(pendiente)
        .setAutoCancel(true)
        .build()

    NotificationManagerCompat.from(context).notify(contador++, notificacion)
    return true
}

fun abrirAjustesNotificaciones(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}