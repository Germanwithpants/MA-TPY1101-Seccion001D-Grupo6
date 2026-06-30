package com.conectatarot.app

import android.content.SharedPreferences
import com.conectatarot.app.network.RetrofitClient

object TarotistaUtils {

    suspend fun resolverIdTarotista(token: String, prefs: SharedPreferences): Int {
        val cached = prefs.getInt("idTarotista", 0)
        if (cached != 0) return cached

        // Try sessions — each session carries tarotistaId
        try {
            val id = RetrofitClient.instance
                .getSesionesTarotista("Bearer $token")
                .body()?.data?.content?.firstOrNull()?.tarotistaId ?: 0
            if (id != 0) { prefs.edit().putInt("idTarotista", id).apply(); return id }
        } catch (_: Exception) {}

        // Fallback: match by nombreProfesional in the public tarotistas list
        val nombrePro = prefs.getString("nombreProfesional", "") ?: ""
        if (nombrePro.isNotBlank()) {
            try {
                val id = RetrofitClient.instance
                    .getTarotistas("Bearer $token")
                    .body()?.data?.firstOrNull { it.nombreProfesional == nombrePro }?.id ?: 0
                if (id != 0) { prefs.edit().putInt("idTarotista", id).apply(); return id }
            } catch (_: Exception) {}
        }

        return 0
    }
}
