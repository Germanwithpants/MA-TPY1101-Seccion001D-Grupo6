package com.conectatarot.app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.conectatarot.app.network.DisponibilidadItem
import com.conectatarot.app.network.DisponibilidadRequest
import com.conectatarot.app.network.RetrofitClient
import kotlinx.coroutines.launch

class DisponibilidadActivity : AppCompatActivity() {

    private val DIAS = arrayOf("LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES", "SABADO", "DOMINGO")
    private val DIAS_DISPLAY = arrayOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")

    private var isOnboarding = false
    private var slotCount = 0
    private lateinit var btnContinuar: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_disponibilidad)

        isOnboarding = intent.getBooleanExtra("isOnboarding", false)

        val prefs = getSharedPreferences("conectatarot", MODE_PRIVATE)
        val token = prefs.getString("token", "") ?: ""

        val spinnerDia = findViewById<Spinner>(R.id.spinnerDia)
        val etHoraInicio = findViewById<EditText>(R.id.etHoraInicio)
        val etHoraFin = findViewById<EditText>(R.id.etHoraFin)
        val btnAgregar = findViewById<Button>(R.id.btnAgregarDisponibilidad)
        val rvDisponibilidad = findViewById<RecyclerView>(R.id.rvDisponibilidad)
        val tvEmpty = findViewById<TextView>(R.id.tvEmptyDisponibilidad)
        val progressDisp = findViewById<ProgressBar>(R.id.progressDisponibilidad)
        val tvVolver = findViewById<TextView>(R.id.tvVolverDisponibilidad)
        val bannerOnboarding = findViewById<View>(R.id.bannerOnboarding)
        btnContinuar = findViewById(R.id.btnContinuarOnboarding)

        if (isOnboarding) {
            tvVolver.visibility = View.INVISIBLE  // keep space but not clickable
            bannerOnboarding.visibility = View.VISIBLE
            btnContinuar.visibility = View.VISIBLE
            btnContinuar.isEnabled = false
            btnContinuar.alpha = 0.4f
            btnContinuar.setOnClickListener {
                startActivity(Intent(this, TarotistaHomeActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
            }
        } else {
            tvVolver.setOnClickListener { finish() }
        }

        spinnerDia.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, DIAS_DISPLAY)

        etHoraInicio.isFocusable = false
        etHoraInicio.setOnClickListener { showTimePicker(etHoraInicio) }
        etHoraFin.isFocusable = false
        etHoraFin.setOnClickListener { showTimePicker(etHoraFin) }

        rvDisponibilidad.layoutManager = LinearLayoutManager(this)

        var tarotistaId = 0

        fun cargar() {
            progressDisp.visibility = View.VISIBLE
            lifecycleScope.launch {
                if (tarotistaId == 0) tarotistaId = TarotistaUtils.resolverIdTarotista(token, prefs)
                try {
                    val resp = RetrofitClient.instance.getDisponibilidad("Bearer $token", tarotistaId)
                    val lista = resp.body()?.data ?: emptyList()
                    slotCount = lista.size
                    progressDisp.visibility = View.GONE
                    if (lista.isEmpty()) {
                        tvEmpty.visibility = View.VISIBLE
                        rvDisponibilidad.visibility = View.GONE
                    } else {
                        tvEmpty.visibility = View.GONE
                        rvDisponibilidad.visibility = View.VISIBLE
                        rvDisponibilidad.adapter = DisponibilidadAdapter(lista) { item ->
                            lifecycleScope.launch {
                                try {
                                    RetrofitClient.instance.deleteDisponibilidad("Bearer $token", tarotistaId, item.id)
                                    cargar()
                                } catch (e: Exception) {
                                    Toast.makeText(this@DisponibilidadActivity, "Error al eliminar", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                    updateContinuarButton()
                } catch (e: Exception) {
                    progressDisp.visibility = View.GONE
                    Toast.makeText(this@DisponibilidadActivity, "Error al cargar disponibilidad", Toast.LENGTH_SHORT).show()
                }
            }
        }

        cargar()

        btnAgregar.setOnClickListener {
            val horaInicio = etHoraInicio.text.toString().trim()
            val horaFin = etHoraFin.text.toString().trim()
            if (horaInicio.isEmpty() || horaFin.isEmpty()) {
                Toast.makeText(this, "Selecciona hora de inicio y fin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val dia = DIAS[spinnerDia.selectedItemPosition]
            btnAgregar.isEnabled = false
            lifecycleScope.launch {
                if (tarotistaId == 0) tarotistaId = TarotistaUtils.resolverIdTarotista(token, prefs)
                try {
                    val resp = RetrofitClient.instance.addDisponibilidad(
                        "Bearer $token", tarotistaId,
                        DisponibilidadRequest(dia, horaInicio, horaFin)
                    )
                    if (resp.isSuccessful) {
                        etHoraInicio.setText("")
                        etHoraFin.setText("")
                        cargar()
                    } else {
                        Toast.makeText(this@DisponibilidadActivity, "Error al guardar (${resp.code()})", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@DisponibilidadActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
                } finally {
                    btnAgregar.isEnabled = true
                }
            }
        }
    }

    private fun updateContinuarButton() {
        if (!isOnboarding) return
        val hasSlots = slotCount > 0
        btnContinuar.isEnabled = hasSlots
        btnContinuar.alpha = if (hasSlots) 1f else 0.4f
        btnContinuar.text = if (hasSlots)
            "Comenzar a recibir clientes ($slotCount horario${if (slotCount != 1) "s" else ""} agregado${if (slotCount != 1) "s" else ""})"
        else
            "Agrega al menos un horario para continuar"
    }

    override fun onBackPressed() {
        if (isOnboarding) {
            Toast.makeText(this, "Debes agregar al menos un horario para continuar", Toast.LENGTH_LONG).show()
        } else {
            super.onBackPressed()
        }
    }

    private fun showTimePicker(field: EditText) {
        val cal = java.util.Calendar.getInstance()
        android.app.TimePickerDialog(
            this,
            { _, h, m -> field.setText(String.format("%02d:%02d", h, m)) },
            cal.get(java.util.Calendar.HOUR_OF_DAY),
            cal.get(java.util.Calendar.MINUTE),
            true
        ).show()
    }

    inner class DisponibilidadAdapter(
        private val items: List<DisponibilidadItem>,
        private val onEliminar: (DisponibilidadItem) -> Unit
    ) : RecyclerView.Adapter<DisponibilidadAdapter.VH>() {

        inner class VH(view: View) : RecyclerView.ViewHolder(view) {
            val tvDia: TextView = view.findViewById(R.id.tvDiaDisp)
            val tvHoras: TextView = view.findViewById(R.id.tvHorasDisp)
            val btnEliminar: ImageButton = view.findViewById(R.id.btnEliminarDisp)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            VH(LayoutInflater.from(parent.context).inflate(R.layout.item_disponibilidad, parent, false))

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            holder.tvDia.text = item.diaSemana
            holder.tvHoras.text = "${item.horaInicio.take(5)} – ${item.horaFin.take(5)}"
            holder.btnEliminar.setOnClickListener { onEliminar(item) }
        }

        override fun getItemCount() = items.size
    }
}
