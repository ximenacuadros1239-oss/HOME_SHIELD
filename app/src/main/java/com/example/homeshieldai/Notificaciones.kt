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
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

const val CANAL_ALERTAS = "alertas_seguridad"

fun crearCanalAlertas(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val canal = NotificationChannel(
            CANAL_ALERTAS,
            "Alertas de seguridad",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Avisos de cortocircuito y consumo anormal"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 400, 200, 400)
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(canal)
    }
}

@SuppressLint("MissingPermission")
fun mostrarNotificacionPeligro(context: Context, mensaje: String) {
    if (Build.VERSION.SDK_INT >= 33 &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
        != PackageManager.PERMISSION_GRANTED
    ) return

    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val pendiente = PendingIntent.getActivity(
        context, 0, intent,
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    val notificacion = NotificationCompat.Builder(context, CANAL_ALERTAS)
        .setSmallIcon(android.R.drawable.ic_dialog_alert)
        .setContentTitle("PELIGRO: Corte de emergencia")
        .setContentText(mensaje)
        .setStyle(NotificationCompat.BigTextStyle().bigText(mensaje))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setCategory(NotificationCompat.CATEGORY_ALARM)
        .setColor(0xFFE04A55.toInt())
        .setVibrate(longArrayOf(0, 400, 200, 400))
        .setContentIntent(pendiente)
        .setAutoCancel(true)
        .build()

    NotificationManagerCompat.from(context).notify(1001, notificacion)
}