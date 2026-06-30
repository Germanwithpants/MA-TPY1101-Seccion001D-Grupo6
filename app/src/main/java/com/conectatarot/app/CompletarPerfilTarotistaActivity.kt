package com.conectatarot.app

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.json.JSONObject

class CompletarPerfilTarotistaActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_completar_perfil_tarotista)

        val etNombrePro = findViewById<EditText>(R.id.etCompNombrePro)
        val etDescripcion = findViewById<EditText>(R.id.etCompDescripcion)
        val etPrecio = findViewById<EditText>(R.id.etCompPrecio)
        val btnGuardar = findViewById<Button>(R.id.btnCompGuardar)
        val tvResultado = findViewById<TextView>(R.id.tvCompResultado)

        val prefs = getSharedPreferences("conectatarot", MODE_PRIVATE)
        val token = prefs.getString("token", "") ?: ""
        val idUsuario = prefs.getInt("idUsuario", 0)

        btnGuardar.setOnClickListener {
            val nombrePro = etNombrePro.text.toString().trim()
            val descripcion = etDescripcion.text.toString().trim()
            val precioStr = etPrecio.text.toString().trim()

            if (nombrePro.isEmpty() || descripcion.isEmpty() || precioStr.isEmpty()) {
                tvResultado.text = "Completa todos los campos"
                tvResultado.setTextColor(getColor(android.R.color.holo_red_light))
                return@setOnClickListener
            }

            if (nombrePro.length < 3) {
                tvResultado.text = "El nombre profesional debe tener al menos 3 caracteres"
                tvResultado.setTextColor(getColor(android.R.color.holo_red_light))
                return@setOnClickListener
            }

            if (descripcion.length < 20) {
                tvResultado.text = "La descripción necesita un mínimo de 20 caracteres (llevas ${descripcion.length})"
                tvResultado.setTextColor(getColor(android.R.color.holo_red_light))
                return@setOnClickListener
            }

            val precio = precioStr.toDoubleOrNull() ?: run {
                tvResultado.text = "Precio inválido — ingresa solo números"
                tvResultado.setTextColor(getColor(android.R.color.holo_red_light))
                return@setOnClickListener
            }

            if (precio <= 0) {
                tvResultado.text = "El precio debe ser mayor a 0"
                tvResultado.setTextColor(getColor(android.R.color.holo_red_light))
                return@setOnClickListener
            }

            tvResultado.text = ""
            btnGuardar.isEnabled = false
            btnGuardar.text = "Guardando..."

            lifecycleScope.launch {
                try {
                    val email = prefs.getString("email", "") ?: ""
                    val response = com.conectatarot.app.network.RetrofitClient.instance.completarPerfilTarotista(
                        "Bearer $token",
                        com.conectatarot.app.network.CompletarPerfilRequest(idUsuario, nombrePro, descripcion, precio, email)
                    )
                    if (response.isSuccessful) {
                        prefs.edit()
                            .putString("rol", "TAROTISTA")
                            .putString("nombreProfesional", nombrePro)
                            .putString("descripcion", descripcion)
                            .putString("precioBase", precioStr)
                            .apply()
                        startActivity(Intent(this@CompletarPerfilTarotistaActivity, DisponibilidadActivity::class.java).apply {
                            putExtra("isOnboarding", true)
                        })
                        finish()
                    } else {
                        val msg = try {
                            val body = response.errorBody()?.string() ?: ""
                            val json = JSONObject(body)
                            when {
                                json.optString("message").contains("ya tiene un perfil", ignoreCase = true) ->
                                    "Este usuario ya tiene un perfil de tarotista"
                                json.optString("message").contains("20 caracteres", ignoreCase = true) ->
                                    "La descripción necesita un mínimo de 20 caracteres"
                                json.optString("message").contains("precio", ignoreCase = true) ->
                                    "El precio debe ser mayor a 0"
                                json.optString("message").isNotEmpty() ->
                                    json.optString("message")
                                else -> "Error al guardar perfil (código ${response.code()})"
                            }
                        } catch (_: Exception) {
                            "Error al guardar perfil (código ${response.code()})"
                        }
                        tvResultado.text = msg
                        tvResultado.setTextColor(getColor(android.R.color.holo_red_light))
                        btnGuardar.isEnabled = true
                        btnGuardar.text = "Siguiente: configurar horarios →"
                    }
                } catch (e: Exception) {
                    tvResultado.text = "Error de conexión — revisa tu internet"
                    tvResultado.setTextColor(getColor(android.R.color.holo_red_light))
                    btnGuardar.isEnabled = true
                    btnGuardar.text = "Siguiente: configurar horarios →"
                }
            }
        }
    }
}