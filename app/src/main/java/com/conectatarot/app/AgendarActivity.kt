package com.conectatarot.app

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.conectatarot.app.network.RetrofitClient
import com.conectatarot.app.network.SesionRequest
import kotlinx.coroutines.launch

class AgendarActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_agendar)

        val tarotistaId = intent.getIntExtra("tarotistaId", 0)
        val nombreTarotista = intent.getStringExtra("nombre") ?: ""

        val prefs = getSharedPreferences("conectatarot", MODE_PRIVATE)
        val token = prefs.getString("token", "") ?: ""
        val usuarioId = prefs.getInt("idUsuario", 0)

        findViewById<TextView>(R.id.tvAgendarTitulo).text = "Agendar con $nombreTarotista"

        val etFecha = findViewById<EditText>(R.id.etFecha)
        etFecha.isFocusable = false
        etFecha.isClickable = true
        etFecha.setOnClickListener {
            val cal = java.util.Calendar.getInstance()
            android.app.DatePickerDialog(
                this,
                { _, year, month, day ->
                    etFecha.setText(String.format("%04d-%02d-%02d", year, month + 1, day))
                },
                cal.get(java.util.Calendar.YEAR),
                cal.get(java.util.Calendar.MONTH),
                cal.get(java.util.Calendar.DAY_OF_MONTH)
            ).show()
        }
        val etHora = findViewById<EditText>(R.id.etHora)
        etHora.isFocusable = false
        etHora.isClickable = true
        etHora.setOnClickListener {
            val cal = java.util.Calendar.getInstance()
            android.app.TimePickerDialog(
                this,
                { _, hour, minute ->
                    etHora.setText(String.format("%02d:%02d", hour, minute))
                },
                cal.get(java.util.Calendar.HOUR_OF_DAY),
                cal.get(java.util.Calendar.MINUTE),
                true
            ).show()
        }
        val spinnerDuracion = findViewById<Spinner>(R.id.spinnerDuracion)
        val tvResultado = findViewById<TextView>(R.id.tvResultado)
        val btnConfirmar = findViewById<Button>(R.id.btnConfirmar)

        val duraciones = arrayOf("30 minutos", "60 minutos", "90 minutos")
        spinnerDuracion.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, duraciones)

        btnConfirmar.setOnClickListener {
            val fecha = etFecha.text.toString().trim()
            val hora = etHora.text.toString().trim()

            if (fecha.isEmpty() || hora.isEmpty()) {
                tvResultado.text = "Por favor completa todos los campos"
                tvResultado.setTextColor(getColor(android.R.color.holo_red_light))
                return@setOnClickListener
            }

            val duracionMinutos = when (spinnerDuracion.selectedItemPosition) {
                0 -> 30
                1 -> 60
                else -> 90
            }

            val fechaCompleta = "${fecha}T${hora}:00"

            btnConfirmar.isEnabled = false
            btnConfirmar.text = "Agendando..."

            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.instance.agendarSesion(
                        "Bearer $token",
                        SesionRequest(
                            usuarioId = usuarioId,
                            tarotistaId = tarotistaId,
                            especialidadId = 1,
                            fecha = fechaCompleta,
                            duracionMinutos = duracionMinutos
                        )
                    )
                    if (response.isSuccessful) {
                        tvResultado.text = "✅ Sesión agendada correctamente"
                        tvResultado.setTextColor(getColor(android.R.color.holo_green_light))
                        btnConfirmar.text = "Agendado"
                    } else {
                        tvResultado.text = "❌ Error al agendar. Verifica el horario."
                        tvResultado.setTextColor(getColor(android.R.color.holo_red_light))
                        btnConfirmar.isEnabled = true
                        btnConfirmar.text = "Confirmar"
                    }
                } catch (e: Exception) {
                    tvResultado.text = "❌ Error de conexión"
                    tvResultado.setTextColor(getColor(android.R.color.holo_red_light))
                    btnConfirmar.isEnabled = true
                    btnConfirmar.text = "Confirmar"
                }
            }
        }

        findViewById<TextView>(R.id.tvVolverAgendar).setOnClickListener {
            finish()
        }
    }
}