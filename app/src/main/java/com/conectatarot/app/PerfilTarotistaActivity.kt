package com.conectatarot.app

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.conectatarot.app.network.AgregarEspecialidadRequest
import com.conectatarot.app.network.EditarPerfilTarotistaRequest
import com.conectatarot.app.network.Especialidad
import com.conectatarot.app.network.RetrofitClient
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch
import org.json.JSONObject

class PerfilTarotistaActivity : AppCompatActivity() {

    private val precios = listOf(5000, 8000, 10000, 12000, 15000, 18000, 20000, 25000, 30000, 35000, 40000, 45000)
    private var idTarotista = 0
    private var todasEspecialidades = listOf<Especialidad>()
    private var especialidadesActuales = setOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil_tarotista)

        val prefs = getSharedPreferences("conectatarot", MODE_PRIVATE)
        val token = prefs.getString("token", "") ?: ""

        val etNombrePro = findViewById<EditText>(R.id.etEditNombrePro)
        val etDescripcion = findViewById<EditText>(R.id.etEditDescripcion)
        val spinnerPrecio = findViewById<Spinner>(R.id.spinnerEditPrecio)
        val btnGuardar = findViewById<Button>(R.id.btnGuardarPerfilTarotista)
        val tvResultado = findViewById<TextView>(R.id.tvResultadoPerfilTarotista)
        val tvVolver = findViewById<TextView>(R.id.tvVolverPerfilTarotista)

        val precioLabels = precios.map { "$ ${"%,d".format(it)} CLP" }
        spinnerPrecio.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, precioLabels)
            .also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        etNombrePro.setText(prefs.getString("nombreProfesional", "") ?: "")
        etDescripcion.setText(prefs.getString("descripcion", "") ?: "")
        val precioGuardado = prefs.getString("precioBase", "15000")?.toDoubleOrNull()?.toInt() ?: 15000
        spinnerPrecio.setSelection((precios.indexOfFirst { it >= precioGuardado }).takeIf { it >= 0 } ?: 0)

        tvVolver.setOnClickListener { finish() }

        lifecycleScope.launch {
            idTarotista = TarotistaUtils.resolverIdTarotista(token, prefs)
            cargarEspecialidades(token)
        }

        btnGuardar.setOnClickListener {
            val nombrePro = etNombrePro.text.toString().trim()
            val descripcion = etDescripcion.text.toString().trim()

            if (nombrePro.isEmpty() || descripcion.isEmpty()) {
                tvResultado.text = "Por favor completa todos los campos"
                tvResultado.setTextColor(getColor(android.R.color.holo_red_light))
                return@setOnClickListener
            }

            val precio = precios[spinnerPrecio.selectedItemPosition].toDouble()
            btnGuardar.isEnabled = false
            btnGuardar.text = "Guardando..."

            lifecycleScope.launch {
                try {
                    if (idTarotista == 0) idTarotista = TarotistaUtils.resolverIdTarotista(token, prefs)
                    if (idTarotista == 0) {
                        tvResultado.text = "❌ No se encontró tu perfil de tarotista"
                        tvResultado.setTextColor(getColor(android.R.color.holo_red_light))
                        btnGuardar.isEnabled = true
                        btnGuardar.text = "Guardar cambios"
                        return@launch
                    }

                    val response = RetrofitClient.instance.editarPerfilTarotista(
                        "Bearer $token", idTarotista,
                        EditarPerfilTarotistaRequest(nombrePro, descripcion, precio)
                    )

                    if (response.isSuccessful) {
                        prefs.edit()
                            .putString("nombreProfesional", nombrePro)
                            .putString("descripcion", descripcion)
                            .putString("precioBase", precio.toInt().toString())
                            .putInt("idTarotista", idTarotista)
                            .apply()

                        guardarEspecialidades(token)

                        tvResultado.text = "✅ Perfil actualizado correctamente"
                        tvResultado.setTextColor(getColor(android.R.color.holo_green_light))
                        btnGuardar.text = "Guardado ✓"
                    } else {
                        val errorMsg = try {
                            JSONObject(response.errorBody()?.string() ?: "").optString("message", "")
                        } catch (_: Exception) { "" }
                        tvResultado.text = "❌ ${errorMsg.ifBlank { "Error al actualizar (${response.code()})" }}"
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

    private suspend fun cargarEspecialidades(token: String) {
        try {
            val todas = RetrofitClient.instance.getEspecialidades().body()?.data ?: return
            todasEspecialidades = todas

            val actuales = if (idTarotista != 0)
                RetrofitClient.instance.getEspecialidadesTarotista(idTarotista).body()?.data ?: emptyList()
            else emptyList()
            especialidadesActuales = actuales.map { it.id }.toSet()

            val chipGroup = ChipGroup(this).apply {
                isSingleSelection = false
            }
            todas.forEach { esp ->
                val chip = Chip(this).apply {
                    text = esp.nombre
                    isCheckable = true
                    isChecked = esp.id in especialidadesActuales
                    tag = esp.id
                    chipBackgroundColor = ColorStateList(
                        arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
                        intArrayOf(Color.parseColor("#7d3cae"), Color.parseColor("#2d1654"))
                    )
                    setTextColor(Color.WHITE)
                    chipStrokeColor = ColorStateList.valueOf(Color.parseColor("#9b59b6"))
                    chipStrokeWidth = 1f
                }
                chipGroup.addView(chip)
            }

            val layout = findViewById<LinearLayout>(R.id.layoutEspecialidades)
            layout.removeAllViews()
            layout.addView(chipGroup)
        } catch (_: Exception) {}
    }

    private suspend fun guardarEspecialidades(token: String) {
        if (idTarotista == 0) return
        val layout = findViewById<LinearLayout>(R.id.layoutEspecialidades)
        if (layout.childCount == 0) return
        val chipGroup = layout.getChildAt(0) as? ChipGroup ?: return

        val seleccionadas = mutableSetOf<Int>()
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as? Chip ?: continue
            if (chip.isChecked) seleccionadas.add(chip.tag as Int)
        }

        seleccionadas.filter { it !in especialidadesActuales }.forEach { id ->
            try { RetrofitClient.instance.agregarEspecialidad("Bearer $token", idTarotista, AgregarEspecialidadRequest(id)) } catch (_: Exception) {}
        }
        especialidadesActuales.filter { it !in seleccionadas }.forEach { id ->
            try { RetrofitClient.instance.eliminarEspecialidad("Bearer $token", idTarotista, id) } catch (_: Exception) {}
        }
        especialidadesActuales = seleccionadas
    }
}
