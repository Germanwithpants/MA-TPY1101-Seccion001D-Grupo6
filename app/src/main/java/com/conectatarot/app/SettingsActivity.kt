package com.conectatarot.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.conectatarot.app.network.CambiarPasswordRequest
import com.conectatarot.app.network.RetrofitClient
import kotlinx.coroutines.launch

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

        findViewById<TextView>(R.id.tvVolverSettings).setOnClickListener { finish() }

        val version = try { packageManager.getPackageInfo(packageName, 0).versionName } catch (e: Exception) { "1.0.0" }
        findViewById<TextView>(R.id.tvVersion).text = version

        // Editar perfil
        findViewById<View>(R.id.rowEditarPerfil).setOnClickListener {
            startActivity(Intent(this, PerfilActivity::class.java))
        }

        // Cambiar contraseña — implementado
        findViewById<View>(R.id.rowCambiarPassword).setOnClickListener {
            mostrarDialogoCambiarPassword()
        }

        // Próximamente
        listOf(R.id.rowDispositivos, R.id.rowSuscripcion, R.id.rowDosFactor).forEach { id ->
            findViewById<View>(id).setOnClickListener {
                Toast.makeText(this, "Próximamente disponible", Toast.LENGTH_SHORT).show()
            }
        }

        // Limpiar caché
        findViewById<View>(R.id.rowLimpiarCache).setOnClickListener {
            cacheDir.deleteRecursively()
            Toast.makeText(this, "Caché limpiada", Toast.LENGTH_SHORT).show()
        }

        // Notificaciones
        val switchNoti = findViewById<Switch>(R.id.switchNotificaciones)
        switchNoti.isChecked = prefs.getBoolean("notificaciones_activas", true)
        switchNoti.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("notificaciones_activas", isChecked).apply()
            Toast.makeText(this,
                if (isChecked) "Notificaciones activadas" else "Notificaciones desactivadas",
                Toast.LENGTH_SHORT).show()
        }

        // Modo oscuro
        val switchDark = findViewById<Switch>(R.id.switchModoOscuro)
        switchDark.isChecked = prefs.getBoolean("modo_oscuro", false)
        switchDark.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("modo_oscuro", isChecked).apply()
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        // Colores de acento
        val savedColor = prefs.getString("accent_color", "#9b59b6")
        accentColors.forEach { (viewId, hex) ->
            val swatch = findViewById<View>(viewId)
            if (hex == savedColor) { swatch.scaleX = 1.3f; swatch.scaleY = 1.3f }
            swatch.setOnClickListener {
                accentColors.keys.forEach { id -> findViewById<View>(id).also { v -> v.scaleX = 1f; v.scaleY = 1f } }
                swatch.scaleX = 1.3f; swatch.scaleY = 1.3f
                prefs.edit().putString("accent_color", hex).apply()
                Toast.makeText(this, "Color guardado", Toast.LENGTH_SHORT).show()
            }
        }

        // Permisos del SO
        findViewById<View>(R.id.rowPermissions).setOnClickListener {
            startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
            })
        }

        // FAQ
        findViewById<View>(R.id.rowFaq).setOnClickListener {
            mostrarFaq()
        }

        // Soporte
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

    private fun mostrarDialogoCambiarPassword() {
        val prefs = getSharedPreferences("conectatarot", MODE_PRIVATE)
        val token = prefs.getString("token", "") ?: ""
        val idUsuario = prefs.getInt("idUsuario", 0)

        val etActual = EditText(this).apply { hint = "Contraseña actual"; inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD }
        val etNueva = EditText(this).apply { hint = "Nueva contraseña"; inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD }
        val etConfirmar = EditText(this).apply { hint = "Confirmar nueva contraseña"; inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD }
        val tvError = TextView(this).apply { setTextColor(0xFFe74c3c.toInt()); visibility = View.GONE }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 8)
            addView(etActual)
            addView(etNueva)
            addView(etConfirmar)
            addView(tvError)
        }

        val dialog = android.app.AlertDialog.Builder(this)
            .setTitle("🔒 Cambiar contraseña")
            .setView(layout)
            .setPositiveButton("Guardar", null)
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val actual = etActual.text.toString()
                val nueva = etNueva.text.toString()
                val confirmar = etConfirmar.text.toString()

                if (actual.isEmpty() || nueva.isEmpty() || confirmar.isEmpty()) {
                    tvError.text = "Completa todos los campos"; tvError.visibility = View.VISIBLE; return@setOnClickListener
                }
                if (nueva.length < 6) {
                    tvError.text = "La nueva contraseña debe tener al menos 6 caracteres"; tvError.visibility = View.VISIBLE; return@setOnClickListener
                }
                if (nueva != confirmar) {
                    tvError.text = "Las contraseñas no coinciden"; tvError.visibility = View.VISIBLE; return@setOnClickListener
                }

                tvError.visibility = View.GONE
                dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).isEnabled = false

                lifecycleScope.launch {
                    try {
                        val resp = RetrofitClient.instance.cambiarPassword(
                            "Bearer $token", idUsuario,
                            CambiarPasswordRequest(actual, nueva)
                        )
                        if (resp.isSuccessful) {
                            Toast.makeText(this@SettingsActivity, "✅ Contraseña actualizada", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                        } else {
                            val msg = try {
                                org.json.JSONObject(resp.errorBody()?.string() ?: "").optString("message", "")
                            } catch (_: Exception) { "" }
                            tvError.text = msg.ifBlank { "Contraseña actual incorrecta" }
                            tvError.visibility = View.VISIBLE
                            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).isEnabled = true
                        }
                    } catch (e: Exception) {
                        tvError.text = "Error de conexión"
                        tvError.visibility = View.VISIBLE
                        dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).isEnabled = true
                    }
                }
            }
        }
        dialog.show()
    }

    private fun mostrarFaq() {
        val preguntas = """
            ❓ ¿Cómo agendo una sesión?
            Busca un tarotista, toca su nombre para ver su perfil y selecciona "Agendar sesión".

            ❓ ¿Cuándo puedo pagar?
            Después de que el tarotista confirme tu sesión, aparecerá el botón de pago en "Mis sesiones".

            ❓ ¿Cómo me uno a la videollamada?
            El botón aparece 15 minutos antes del inicio de la sesión confirmada y pagada.

            ❓ ¿Puedo cancelar una sesión?
            Sí, desde "Mis sesiones" puedes cancelar sesiones pendientes o confirmadas.

            ❓ ¿Cómo califico a un tarotista?
            En "Mis sesiones", al completarse una sesión aparece el botón "⭐ Calificar tarotista".

            ❓ ¿Qué hago si hubo un problema con mi sesión?
            Usa el botón "Reportar problema" en "Mis sesiones". El administrador lo revisará.
        """.trimIndent()

        android.app.AlertDialog.Builder(this)
            .setTitle("Preguntas frecuentes")
            .setMessage(preguntas)
            .setPositiveButton("Entendido", null)
            .show()
    }
}
