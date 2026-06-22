package com.conectatarot.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class ConectaTarotApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "conectatarot_sesiones",
                "Sesiones de Tarot",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de confirmación y estado de sesiones"
                enableVibration(true)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        lateinit var instance: ConectaTarotApp
            private set
    }
}
