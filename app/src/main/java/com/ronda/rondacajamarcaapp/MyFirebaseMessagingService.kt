package com.ronda.rondacajamarcaapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlin.random.Random

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val CHANNEL_ID = "emergencies_channel"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Mostrar notificación si la app está en background
        if (remoteMessage.notification != null) {
            showEmergencyNotification(remoteMessage.notification?.title ?: "Emergencia",
                remoteMessage.notification?.body ?: "Nueva emergencia activa")
        }

        // Manejar datos personalizados (opcional)
        remoteMessage.data.forEach { key, value ->
            // Ej: Log.d("FCM", "$key: $value")
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Envía el token a tu servidor o Firestore para registrarlo
        // Ej: saveTokenToFirestore(token)
    }

    private fun showEmergencyNotification(title: String, body: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Crear canal para Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Emergencias",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de emergencias activas"
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Intent para abrir la app (AdminScreen)
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_admin", true) // Para abrir directamente AdminScreen
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM) // CORREGIDO
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        notificationManager.notify(Random.nextInt(10000), notification)
    }
}