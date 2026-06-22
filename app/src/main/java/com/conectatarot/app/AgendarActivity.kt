package com.conectatarot.app

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.conectatarot.app.network.Especialidad
import com.conectatarot.app.network.RetrofitClient
import com.conectatarot.app.network.SesionRequest
import kotlinx.coroutines.launch

class AgendarActivity : AppCompatActivity() {

    private var especialidades: List<Especialidad> = emptyList()

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
        val spinnerEspecialidad = findViewById<Spinner>(R.id.spinnerEspecialidad)
        val tvResultado = findViewById<TextView>(R.id.tvResultado)
        val btnConfirmar = findViewById<Button>(R.id.btnConfirmar)
        val progressAgendar = findViewById<ProgressBar>(R.id.progressAgendar)

        val duraciones = arrayOf("30 minutos", "60 minutos", "90 minutos")
        spinnerDuracion.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, duraciones)

        // Load specialties for this tarotista
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.instance.getEspecialidadesTarotista(tarotistaId)
                if (resp.isSuccessful && resp.body() != null) {
                    especialidades = resp.body()!!.data ?: emptyList()
                }
                // Fallback to all specialties if tarotista has none configured
                if (especialidades.isEmpty()) {
                    val all = RetrofitClient.instance.getEspecialidades()
                    especialidades = all.body()?.data ?: emptyList()
                }
            } catch (e: Exception) {
                especialidades = emptyList()
            }

            val nombres = if (especialidades.isEmpty()) arrayOf("General") else
                especialidades.map { it.nombre }.toTypedArray()
            spinnerEspecialidad.adapter = ArrayAdapter(
                this@AgendarActivity,
                android.R.layout.simple_spinner_dropdown_item,
                nombres
            )
        }

        btnConfirmar.setOnClickListener {
            val fecha = etFecha.text.toString().trim()
            val hora = etHora.text.toString().trim()

            if (fecha.isEmpty() || hora.isEmpty()) {
                tvResultado.text = "Por favor selecciona fecha y hora"
                tvResultado.setTextColor(getColor(android.R.color.holo_red_light))
                return@setOnClickListener
            }

            val duracionMinutos = when (spinnerDuracion.selectedItemPosition) {
                0 -> 30
                1 -> 60
                else -> 90
            }
            val especialidadId = if (especialidades.isEmpty()) 1
                                 else especialidades[spinnerEspecialidad.selectedItemPosition].id
            val fechaCompleta = "${fecha}T${hora}:00"

            btnConfirmar.isEnabled = false
            btnConfirmar.text = "Agendando..."
            progressAgendar.visibility = View.VISIBLE

            lifecycleScope.launch {
                try {
                    val response = RetrofitClient.instance.agendarSesion(
                        "Bearer $token",
                        SesionRequest(
                            usuarioId = usuarioId,
                            tarotistaId = tarotistaId,
                            especialidadId = especialidadId,
                            fecha = fechaCompleta,
                            duracionMinutos = duracionMinutos
                        )
                    )
                    progressAgendar.visibility = View.GONE
                    if (response.isSuccessful) {
                        tvResultado.text = "✅ Sesión agendada. Espera la confirmación del tarotista."
                        tvResultado.setTextColor(getColor(android.R.color.holo_green_light))
                        btnConfirmar.text = "Agendado ✓"
                    } else {
                        val msg = when (response.code()) {
                            400 -> "El tarotista no tiene disponibilidad en ese horario"
                            409 -> "Ese horario ya está reservado"
                            else -> "Error al agendar (${response.code()})"
                        }
                        tvResultado.text = "❌ $msg"
                        tvResultado.setTextColor(getColor(android.R.color.holo_red_light))
                        btnConfirmar.isEnabled = true
                        btnConfirmar.text = "Confirmar sesión"
                    }
                } catch (e: Exception) {
                    progressAgendar.visibility = View.GONE
                    tvResultado.text = "❌ Error de conexión"
                    tvResultado.setTextColor(getColor(android.R.color.holo_red_light))
                    btnConfirmar.isEnabled = true
                    btnConfirmar.text = "Confirmar sesión"
                }
            }
        }

        findViewById<TextView>(R.id.tvVolverAgendar).setOnClickListener { finish() }
    }
}
