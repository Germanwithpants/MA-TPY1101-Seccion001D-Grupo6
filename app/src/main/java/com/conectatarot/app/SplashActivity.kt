package com.conectatarot.app

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.conectatarot.app.network.RetrofitClient
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        lifecycleScope.launch {
            val prefs = getSharedPreferences("conectatarot", MODE_PRIVATE)
            val token = prefs.getString("token", null)
            val rol = prefs.getString("rol", null)

            if (!token.isNullOrBlank() && !rol.isNullOrBlank()) {
                val valid = try {
                    val resp = RetrofitClient.instance.getMisSesiones("Bearer $token")
                    resp.code() != 401
                } catch (e: Exception) {
                    true
                }

                if (valid) {
                    val dest = when (rol) {
                        "TAROTISTA" -> TarotistaHomeActivity::class.java
                        "ADMIN"     -> AdminPanelActivity::class.java
                        else        -> ClienteActivity::class.java
                    }
                    startActivity(Intent(this@SplashActivity, dest))
                    finish()
                    return@launch
                } else {
                    prefs.edit().clear().apply()
                }
            }

            startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            finish()
        }
    }
}
