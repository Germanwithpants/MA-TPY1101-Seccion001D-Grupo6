package com.conectatarot.app

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.conectatarot.app.network.CompletarPerfilRequest
import com.conectatarot.app.network.LoginRequest
import com.conectatarot.app.network.RegistroRequest
import com.conectatarot.app.network.RetrofitClient
import kotlinx.coroutines.launch

class RegistroTarotistaActivity : AppCompatActivity() {

    private val precios = listOf(5000, 8000, 10000, 12000, 15000, 18000, 20000, 25000, 30000, 35000, 40000, 45000)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro_tarotista)

        val etNombre      = findViewById<EditText>(R.id.etNombreTarotista)
        val etEmail       = findViewById<EditText>(R.id.etEmailTarotista)
        val etPassword    = findViewById<EditText>(R.id.etPasswordTarotista)
        val etNombrePro   = findViewById<EditText>(R.id.etNombreProfesional)
        val etDescripcion = findViewById<EditText>(R.id.etDescripcionTarotista)
        val spinnerPrecio = findViewById<Spinner>(R.id.spinnerPrecioTarotista)
        val btnRegistrar  = findViewById<Button>(R.id.btnRegistrarTarotista)
        val tvResultado   = findViewById<TextView>(R.id.tvResultadoTarotista)
        val tvVolver      = findViewById<TextView>(R.id.tvVolverTarotista)
        val checkTerminos = findViewById<CheckBox>(R.id.checkTerminosTarotista)
        val tvVerTerminos = findViewById<View>(R.id.tvVerTerminosTarotista)

        val precioLabels = precios.map { "$ ${"%,d".format(it)} CLP" }
        spinnerPrecio.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, precioLabels)
            .also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        spinnerPrecio.setSelection(precios.indexOf(15000))

        tvVolver.setOnClickListener { finish() }
        tvVerTerminos.setOnClickListener { PoliticaHelper.mostrar(this) }

        btnRegistrar.setOnClickListener {
            val nombre      = etNombre.text.toString().trim()
            val email       = etEmail.text.toString().trim()
            val password    = etPassword.text.toString().trim()
            val nombrePro   = etNombrePro.text.toString().trim()
            val descripcion = etDescripcion.text.toString().trim()

            if (nombre.isEmpty() || email.isEmpty() || password.isEmpty() ||
                nombrePro.isEmpty() || descripcion.isEmpty()) {
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

            val precio = precios[spinnerPrecio.selectedItemPosition].toDouble()
            btnRegistrar.isEnabled = false
            btnRegistrar.text = "Registrando..."
            tvResultado.text = ""

            lifecycleScope.launch {
                try {
                    // Step 1: create user account (public endpoint)
                    val regResp = RetrofitClient.instance.registrarUsuario(RegistroRequest(nombre, email, password))
                    if (!regResp.isSuccessful) {
                        val body = regResp.errorBody()?.string() ?: ""
                        val msg = try { org.json.JSONObject(body).optString("message", "") } catch (_: Exception) { "" }
                        tvResultado.text = "❌ ${msg.ifBlank { "Error al registrar (${regResp.code()})" }}"
                        tvResultado.setTextColor(getColor(android.R.color.holo_red_light))
                        btnRegistrar.isEnabled = true
                        btnRegistrar.text = "Registrarme como Tarotista"
                        return@launch
                    }

                    // Step 2: login to get token + userId
                    val loginResp = RetrofitClient.instance.login(LoginRequest(email, password))
                    if (!loginResp.isSuccessful || loginResp.body() == null) {
                        tvResultado.text = "✅ Cuenta creada. Inicia sesión para continuar."
                        tvResultado.setTextColor(getColor(android.R.color.holo_green_light))
                        btnRegistrar.text = "Registrado"
                        return@launch
                    }
                    val loginBody = loginResp.body()!!
                    val token = loginBody.token
                    val usuarioId = loginBody.idUsuario

                    // Step 3: complete tarotista profile (requires auth)
                    val perfilResp = RetrofitClient.instance.completarPerfilTarotista(
                        "Bearer $token",
                        CompletarPerfilRequest(usuarioId, nombrePro, descripcion, precio, email)
                    )
                    if (perfilResp.isSuccessful) {
                        tvResultado.text = "✅ Registro exitoso. Tu cuenta está pendiente de aprobación."
                        tvResultado.setTextColor(getColor(android.R.color.holo_green_light))
                        btnRegistrar.text = "Registrado"
                    } else {
                        val body = perfilResp.errorBody()?.string() ?: ""
                        val msg = try { org.json.JSONObject(body).optString("message", "") } catch (_: Exception) { "" }
                        tvResultado.text = "❌ ${msg.ifBlank { "Error al crear perfil (${perfilResp.code()})" }}"
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
