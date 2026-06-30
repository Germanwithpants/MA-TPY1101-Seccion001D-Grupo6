package com.conectatarot.app

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.conectatarot.app.network.EditarPerfilTarotistaRequest
import com.conectatarot.app.network.RetrofitClient
import kotlinx.coroutines.launch
import org.json.JSONObject

class PerfilTarotistaActivity : AppCompatActivity() {

    private val precios = listOf(5000, 8000, 10000, 12000, 15000, 18000, 20000, 25000, 30000, 35000, 40000, 45000)

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
        val idx = precios.indexOfFirst { it >= precioGuardado }.takeIf { it >= 0 } ?: 0
        spinnerPrecio.setSelection(idx)

        tvVolver.setOnClickListener { finish() }

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
                    val idTarotista = resolverIdTarotista(token, prefs)
                    if (idTarotista == 0) {
                        tvResultado.text = "❌ No se encontró tu perfil de tarotista"
                        tvResultado.setTextColor(getColor(android.R.color.holo_red_light))
                        btnGuardar.isEnabled = true
                        btnGuardar.text = "Guardar cambios"
                        return@launch
                    }

                    val response = RetrofitClient.instance.editarPerfilTarotista(
                        "Bearer $token",
                        idTarotista,
                        EditarPerfilTarotistaRequest(nombrePro, descripcion, precio)
                    )
                    if (response.isSuccessful) {
                        prefs.edit()
                            .putString("nombreProfesional", nombrePro)
                            .putString("descripcion", descripcion)
                            .putString("precioBase", precio.toInt().toString())
                            .putInt("idTarotista", idTarotista)
                            .apply()
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

    // Returns the real tarotista row ID, caching it in prefs after first lookup.
    // Tries: 1) cached value, 2) sessions endpoint, 3) tarotistas list by nombreProfesional.
    private suspend fun resolverIdTarotista(token: String, prefs: android.content.SharedPreferences): Int {
        val cached = prefs.getInt("idTarotista", 0)
        if (cached != 0) return cached

        // Try sessions — each session carries the tarotistaId
        try {
            val resp = RetrofitClient.instance.getSesionesTarotista("Bearer $token")
            val id = resp.body()?.data?.content?.firstOrNull()?.tarotistaId ?: 0
            if (id != 0) { prefs.edit().putInt("idTarotista", id).apply(); return id }
        } catch (_: Exception) {}

        // Fallback: match by stored nombreProfesional in the public tarotistas list
        val nombrePro = prefs.getString("nombreProfesional", "") ?: ""
        if (nombrePro.isNotBlank()) {
            try {
                val resp = RetrofitClient.instance.getTarotistas("Bearer $token")
                val id = resp.body()?.data?.firstOrNull { it.nombreProfesional == nombrePro }?.id ?: 0
                if (id != 0) { prefs.edit().putInt("idTarotista", id).apply(); return id }
            } catch (_: Exception) {}
        }

        return 0
    }
}
