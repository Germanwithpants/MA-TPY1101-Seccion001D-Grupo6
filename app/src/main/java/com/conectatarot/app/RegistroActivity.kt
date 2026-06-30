package com.conectatarot.app

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.conectatarot.app.network.RegistroRequest
import com.conectatarot.app.network.RetrofitClient
import kotlinx.coroutines.launch

class RegistroActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)

        val etNombre = findViewById<EditText>(R.id.etNombreRegistro)
        val etEmail = findViewById<EditText>(R.id.etEmailRegistro)
        val etPassword = findViewById<EditText>(R.id.etPasswordRegistro)
        val btnRegistrar = findViewById<Button>(R.id.btnRegistrar)
        val tvResultado = findViewById<TextView>(R.id.tvResultadoRegistro)
        val tvVolver = findViewById<TextView>(R.id.tvVolverRegistro)
        val checkTerminos = findViewById<CheckBox>(R.id.checkTerminos)
        val tvVerTerminos = findViewById<View>(R.id.tvVerTerminos)

        tvVolver.setOnClickListener { finish() }
        tvVerTerminos.setOnClickListener { PoliticaHelper.mostrar(this) }

        btnRegistrar.setOnClickListener {
            val nombre = etNombre.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (nombre.isEmpty() || email.isEmpty() || password.isEmpty()) {
                tvResultado.text = "Por favor completa todos los campos"
                tvResultado.setTextColor(getColor(android.R.color.holo_red_light))
                return@setOnClickListener
            }

            if (password.length < 6) {
                tvResultado.text = "La contraseña debe tener al menos 6 caracteres"
                tvResultado.setTextColor(getColor(android.R.color.holo_red_light))
                return@setOnClickListener
            }

            if (!checkTerminos.isChecked) {
                tvResultado.text = "Debes aceptar los Términos de Uso y la Política de Privacidad"
                tvResultado.setTextColor(getColor(android.R.color.holo_red_light))
                return@setOnClickListener
            }

            btnRegistrar.isEnabled = false
            btnRegistrar.text = "Registrando..."

            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.instance.registrarUsuario(
                        RegistroRequest(nombre, email, password)
                    )
                    if (response.isSuccessful) {
                        tvResultado.text = "✅ Cuenta creada exitosamente. Ya puedes iniciar sesión."
                        tvResultado.setTextColor(getColor(android.R.color.holo_green_light))
                        btnRegistrar.text = "Registrado"
                    } else {
                        tvResultado.text = "❌ El email ya está registrado"
                        tvResultado.setTextColor(getColor(android.R.color.holo_red_light))
                        btnRegistrar.isEnabled = true
                        btnRegistrar.text = "Crear cuenta"
                    }
                } catch (e: Exception) {
                    tvResultado.text = "❌ Error de conexión"
                    tvResultado.setTextColor(getColor(android.R.color.holo_red_light))
                    btnRegistrar.isEnabled = true
                    btnRegistrar.text = "Crear cuenta"
                }
            }
        }
    }
}