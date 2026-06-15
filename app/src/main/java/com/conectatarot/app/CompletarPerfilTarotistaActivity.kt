package com.conectatarot.app

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

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

            val precio = precioStr.toDoubleOrNull() ?: run {
                tvResultado.text = "Precio inválido"
                tvResultado.setTextColor(getColor(android.R.color.holo_red_light))
                return@setOnClickListener
            }

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
                        startActivity(Intent(this@CompletarPerfilTarotistaActivity, TarotistaHomeActivity::class.java))
                        finish()
                    } else {
                        tvResultado.text = "Error al guardar perfil"
                        tvResultado.setTextColor(getColor(android.R.color.holo_red_light))
                        btnGuardar.isEnabled = true
                        btnGuardar.text = "Comenzar"
                    }
                } catch (e: Exception) {
                    tvResultado.text = "Error de conexión"
                    tvResultado.setTextColor(getColor(android.R.color.holo_red_light))
                    btnGuardar.isEnabled = true
                    btnGuardar.text = "Comenzar"
                }
            }
        }
    }
}