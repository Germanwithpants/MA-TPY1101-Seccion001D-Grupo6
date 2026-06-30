package com.conectatarot.app

import android.content.SharedPreferences
import com.conectatarot.app.network.RetrofitClient

object TarotistaUtils {

    suspend fun resolverIdTarotista(token: String, prefs: SharedPreferences): Int {
        val cached = prefs.getInt("idTarotista", 0)
        if (cached != 0) return cached

        val idUsuario = prefs.getInt("idUsuario", 0)

        // 1. /me endpoint — returns current user's own tarotista profile
        try {
            val id = RetrofitClient.instance
                .getMiPerfilTarotista("Bearer $token")
                .body()?.data?.id ?: 0
            if (id != 0) { prefs.edit().putInt("idTarotista", id).apply(); return id }
        } catch (_: Exception) {}

        // 2. Direct lookup by usuarioId
        if (idUsuario != 0) {
            try {
                val id = RetrofitClient.instance
                    .getTarotistaByUsuario("Bearer $token", idUsuario)
                    .body()?.data?.id ?: 0
                if (id != 0) { prefs.edit().putInt("idTarotista", id).apply(); return id }
            } catch (_: Exception) {}
        }

        // 3. Try sessions — each session carries tarotistaId
        try {
            val id = RetrofitClient.instance
                .getSesionesTarotista("Bearer $token")
                .body()?.data?.content?.firstOrNull()?.tarotistaId ?: 0
            if (id != 0) { prefs.edit().putInt("idTarotista", id).apply(); return id }
        } catch (_: Exception) {}

        // 3. Match by nombreProfesional in the public tarotistas list
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
