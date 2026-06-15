package com.conectatarot.app

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.conectatarot.app.network.EditarPerfilRequest
import com.conectatarot.app.network.RetrofitClient
import kotlinx.coroutines.launch

class PerfilActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil)

        val prefs = getSharedPreferences("conectatarot", MODE_PRIVATE)
        val token = prefs.getString("token", "") ?: ""
        val idUsuario = prefs.getInt("idUsuario", 0)
        val nombreActual = prefs.getString("nombre", "") ?: ""

        val etNombre = findViewById<EditText>(R.id.etPerfilNombre)
        val etEmail = findViewById<EditText>(R.id.etPerfilEmail)
        val btnGuardar = findViewById<Button>(R.id.btnGuardarPerfil)
        val tvResultado = findViewById<TextView>(R.id.tvResultadoPerfil)
        val tvVolver = findViewById<TextView>(R.id.tvVolverPerfil)

        etNombre.setText(nombreActual)
        etEmail.setText(prefs.getString("email", "") ?: "")

        tvVolver.setOnClickListener { finish() }

        btnGuardar.setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val email = etEmail.text.toString().trim()

            if (nombre.isEmpty() || email.isEmpty()) {
                tvResultado.text = "Por favor completa todos los campos"
                tvResultado.setTextColor(getColor(android.R.color.holo_red_light))
                return@setOnClickListener
            }

            btnGuardar.isEnabled = false
            btnGuardar.text = "Guardando..."

            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.instance.editarPerfil(
                        "Bearer $token",
                        idUsuario,
                        EditarPerfilRequest(nombre, email)
                    )
                    if (response.isSuccessful) {
                        prefs.edit()
                            .putString("nombre", nombre)
                            .putString("email", email)
                            .apply()
                        tvResultado.text = "✅ Perfil actualizado correctamente"
                        tvResultado.setTextColor(getColor(android.R.color.holo_green_light))
                        btnGuardar.text = "Guardado"
                    } else {
                        tvResultado.text = "❌ Error al actualizar"
                        tvResultado.setTextColor(getColor(android.R.color.holo_red_light))
                        btnGuardar.isEnabled = true
                        btnGuardar.text = "Guardar cambios"
                    }
                } catch (e: Exception) {
                    tvResultado.text = "❌ Error de conexión"
                    tvResultado.setTextColor(getColor(android.R.color.holo_red_light))
                    btnGuardar.isEnabled = true
                    btnGuardar.text = "Guardar cambios"
                }
            }
        }
    }
}