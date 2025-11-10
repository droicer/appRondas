// File: app/src/main/java/com/ronda/rondacajamarcaapp/MyFirebaseMessagingService.kt
package com.ronda.rondacajamarcaapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.ronda.rondacajamarcaapp.MainActivity

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // === PRIORIDAD: DATA PAYLOAD (tu PHP lo envía) ===
        if (remoteMessage.data.isNotEmpty()) {
            val type = remoteMessage.data["type"] ?: "reporte"
            val title = remoteMessage.data["title"] ?: "Notificación"
            val body = remoteMessage.data["body"] ?: ""
            val channelId = remoteMessage.data["channel_id"] ?: "reportes_channel"

            when (type.lowercase()) {
                "emergencia" -> showEmergencyNotification(title, body, channelId)
                "reporte" -> showReportNotification(title, body, channelId)
                else -> showReportNotification(title, body, channelId)
            }
        }

        // === NOTIFICATION PAYLOAD (opcional) ===
        remoteMessage.notification?.let {
            showReportNotification(it.title ?: "Notificación", it.body ?: "")
        }
    }

    // EMERGENCIA: ROJO, SONIDO FUERTE, VIBRACIÓN
    private fun showEmergencyNotification(title: String, body: String, channelId: String = "emergencias_channel") {
        createChannel(
            channelId = channelId,
            name = "Emergencias",
            importance = NotificationManager.IMPORTANCE_HIGH,
            soundRes = R.raw.emergency_sound
        )

        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("open_tab", 1) // Pestaña de emergencias
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 100, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.drawable.ic_emergency)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(0xFFFF0000.toInt()) // Rojo
            .setVibrate(longArrayOf(0, 500, 200, 500)) // Vibración fuerte
            .build()

        getSystemService(NotificationManager::class.java)
            .notify(EMERGENCY_NOTIFICATION_ID, notification)
    }

    // REPORTE: AZUL, SONIDO NORMAL
    private fun showReportNotification(title: String, body: String, channelId: String = "reportes_channel") {
        createChannel(
            channelId = channelId,
            name = "Reportes",
            importance = NotificationManager.IMPORTANCE_DEFAULT,
            soundRes = R.raw.report_sound
        )

        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("open_tab", 0) // Pestaña de reportes
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 101, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.drawable.ic_report)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(0xFF2196F3.toInt()) // Azul
            .build()

        getSystemService(NotificationManager::class.java)
            .notify(REPORT_NOTIFICATION_ID, notification)
    }

    private fun createChannel(
        channelId: String,
        name: String,
        importance: Int,
        soundRes: Int?
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = "Notificaciones de $name"
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                soundRes?.let {
                    setSound(
                        android.net.Uri.parse("android.resource://$packageName/$it"),
                        android.media.AudioAttributes.Builder()
                            .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                            .build()
                    )
                }
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser ?: return
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("users")
            .document(user.uid)
            .update("fcmToken", token)
    }

    companion object {
        private const val EMERGENCY_NOTIFICATION_ID = 100
        private const val REPORT_NOTIFICATION_ID = 101
    }
}