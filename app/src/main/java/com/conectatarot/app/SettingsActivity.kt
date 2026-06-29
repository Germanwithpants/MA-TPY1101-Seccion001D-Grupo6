package com.conectatarot.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate

class SettingsActivity : AppCompatActivity() {

    private val accentColors = mapOf(
        R.id.colorPurple to "#9b59b6",
        R.id.colorBlue   to "#3498db",
        R.id.colorGreen  to "#27ae60",
        R.id.colorRed    to "#e74c3c",
        R.id.colorOrange to "#e67e22",
        R.id.colorPink   to "#e91e8c"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val prefs = getSharedPreferences("conectatarot", MODE_PRIVATE)

        // Back
        findViewById<TextView>(R.id.tvVolverSettings).setOnClickListener { finish() }

        // Version
        val version = try { packageManager.getPackageInfo(packageName, 0).versionName } catch (e: Exception) { "1.0.0" }
        findViewById<TextView>(R.id.tvVersion).text = version

        // Account rows
        findViewById<View>(R.id.rowEditarPerfil).setOnClickListener {
            startActivity(Intent(this, PerfilActivity::class.java))
        }
        listOf(R.id.rowCambiarPassword, R.id.rowDispositivos, R.id.rowSuscripcion, R.id.rowDosFactor).forEach { id ->
            findViewById<View>(id).setOnClickListener {
                Toast.makeText(this, "Próximamente disponible", Toast.LENGTH_SHORT).show()
            }
        }

        // Clear cache
        findViewById<View>(R.id.rowLimpiarCache).setOnClickListener {
            cacheDir.deleteRecursively()
            Toast.makeText(this, "Caché limpiada", Toast.LENGTH_SHORT).show()
        }

        // Notifications toggle
        val switchNoti = findViewById<Switch>(R.id.switchNotificaciones)
        switchNoti.isChecked = prefs.getBoolean("notificaciones_activas", true)
        switchNoti.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("notificaciones_activas", isChecked).apply()
            Toast.makeText(this, if (isChecked) "Notificaciones activadas" else "Notificaciones desactivadas", Toast.LENGTH_SHORT).show()
        }

        // Dark mode toggle
        val switchDark = findViewById<Switch>(R.id.switchModoOscuro)
        switchDark.isChecked = AppCompatDelegate.getDefaultNightMode() != AppCompatDelegate.MODE_NIGHT_NO
        switchDark.setOnCheckedChangeListener { _, isChecked ->
            val mode = if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            AppCompatDelegate.setDefaultNightMode(mode)
            prefs.edit().putBoolean("modo_oscuro", isChecked).apply()
        }

        // Color swatches
        val savedColor = prefs.getString("accent_color", "#9b59b6")
        accentColors.forEach { (viewId, hex) ->
            val swatch = findViewById<View>(viewId)
            if (hex == savedColor) swatch.scaleX = 1.3f.also { swatch.scaleY = 1.3f }
            swatch.setOnClickListener {
                accentColors.keys.forEach { id ->
                    val v = findViewById<View>(id)
                    v.scaleX = 1f; v.scaleY = 1f
                }
                swatch.scaleX = 1.3f; swatch.scaleY = 1.3f
                prefs.edit().putString("accent_color", hex).apply()
                Toast.makeText(this, "Color guardado — reinicia la app para aplicarlo", Toast.LENGTH_SHORT).show()
            }
        }

        // Permissions → system app settings
        findViewById<View>(R.id.rowPermissions).setOnClickListener {
            startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            })
        }

        // FAQ / support
        findViewById<View>(R.id.rowFaq).setOnClickListener {
            Toast.makeText(this, "Próximamente disponible", Toast.LENGTH_SHORT).show()
        }
        findViewById<View>(R.id.rowSoporte).setOnClickListener {
            startActivity(Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:soporte@conectatarot.com")
                putExtra(Intent.EXTRA_SUBJECT, "Soporte ConectaTarot")
            })
        }

        // Logout
        findViewById<View>(R.id.btnLogoutSettings).setOnClickListener {
            com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
            prefs.edit().clear().apply()
            startActivity(Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }
    }
}
