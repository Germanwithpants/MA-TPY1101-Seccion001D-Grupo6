package com.conectatarot.app

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.conectatarot.app.network.FcmTokenRequest
import com.conectatarot.app.network.RetrofitClient

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val prefs = getSharedPreferences("conectatarot", Context.MODE_PRIVATE)
        val authToken = prefs.getString("token", null) ?: return
        val userId = prefs.getInt("idUsuario", 0)
        if (userId == 0) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                RetrofitClient.instance.saveFcmToken(
                    "Bearer $authToken",
                    userId,
                    FcmTokenRequest(token)
                )
            } catch (e: Exception) {
                // Will retry on next login
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val prefs = getSharedPreferences("conectatarot", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("notificaciones_activas", true)) return

        val title = message.notification?.title ?: message.data["title"] ?: "ConectaTarot"
        val body = message.notification?.body ?: message.data["body"] ?: ""
        val tipo = message.data["tipo"]

        val rol = prefs.getString("rol", "CLIENTE")

        val destClass = when {
            rol == "TAROTISTA" -> TarotistaHomeActivity::class.java
            else -> MisSesionesActivity::class.java
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, destClass).apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val iconRes = when (tipo) {
            "CONFIRMADA" -> android.R.drawable.ic_dialog_info
            "CANCELADA" -> android.R.drawable.ic_dialog_alert
            "RECHAZADA" -> android.R.drawable.ic_delete
            else -> android.R.drawable.ic_dialog_info
        }

        val notification = NotificationCompat.Builder(this, "conectatarot_sesiones")
            .setSmallIcon(iconRes)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
