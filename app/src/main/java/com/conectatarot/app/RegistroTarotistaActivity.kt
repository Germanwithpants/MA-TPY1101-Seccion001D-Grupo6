package com.conectatarot.app

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.conectatarot.app.network.RegistroTarotistaRequest
import com.conectatarot.app.network.RetrofitClient
import kotlinx.coroutines.launch

class RegistroTarotistaActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro_tarotista)

        val etNombre = findViewById<EditText>(R.id.etNombreTarotista)
        val etEmail = findViewById<EditText>(R.id.etEmailTarotista)
        val etPassword = findViewById<EditText>(R.id.etPasswordTarotista)
        val etNombrePro = findViewById<EditText>(R.id.etNombreProfesional)
        val etDescripcion = findViewById<EditText>(R.id.etDescripcionTarotista)
        val etPrecio = findViewById<EditText>(R.id.etPrecioTarotista)
        val btnRegistrar = findViewById<Button>(R.id.btnRegistrarTarotista)
        val tvResultado = findViewById<TextView>(R.id.tvResultadoTarotista)
        val tvVolver = findViewById<TextView>(R.id.tvVolverTarotista)
        val checkTerminos = findViewById<CheckBox>(R.id.checkTerminosTarotista)
        val tvVerTerminos = findViewById<View>(R.id.tvVerTerminosTarotista)

        tvVolver.setOnClickListener { finish() }
        tvVerTerminos.setOnClickListener { PoliticaHelper.mostrar(this) }

        btnRegistrar.setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val nombrePro = etNombrePro.text.toString().trim()
            val descripcion = etDescripcion.text.toString().trim()
            val precioStr = etPrecio.text.toString().trim()

            if (nombre.isEmpty() || email.isEmpty() || password.isEmpty() ||
                nombrePro.isEmpty() || descripcion.isEmpty() || precioStr.isEmpty()) {
                tvResultado.text = "Por favor completa todos los campos"
                tvResultado.setTextColor(getColor(android.R.color.holo_red_light))
                return@setOnClickListener
            }

            if (!checkTerminos.isChecked) {
                tvResultado.text = "Debes aceptar los Términos de Uso y la Política de Privacidad"
                tvResultado.setTextColor(getColor(android.R.color.holo_red_light))
                return@setOnClickListener
            }

            val precio = precioStr.toDoubleOrNull() ?: run {
                tvResultado.text = "El precio debe ser un número válido"
                tvResultado.setTextColor(getColor(android.R.color.holo_red_light))
                return@setOnClickListener
            }

            btnRegistrar.isEnabled = false
            btnRegistrar.text = "Registrando..."

            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.instance.registrarTarotista(
                        RegistroTarotistaRequest(nombre, email, password, nombrePro, descripcion, precio)
                    )
                    if (response.isSuccessful) {
                        tvResultado.text = "✅ Registro exitoso. Tu cuenta está pendiente de aprobación."
                        tvResultado.setTextColor(getColor(android.R.color.holo_green_light))
                        btnRegistrar.text = "Registrado"
                    } else {
                        tvResultado.text = "❌ El email ya está registrado"
                        tvResultado.setTextColor(getColor(android.R.color.holo_red_light))
                        btnRegistrar.isEnabled = true
                        btnRegistrar.text = "Registrarme como Tarotista"
                    }
                } catch (e: Exception) {
                    tvResultado.text = "❌ Error de conexión"
                    tvResultado.setTextColor(getColor(android.R.color.holo_red_light))
                    btnRegistrar.isEnabled = true
                    btnRegistrar.text = "Registrarme como Tarotista"
                }
            }
        }
    }
}