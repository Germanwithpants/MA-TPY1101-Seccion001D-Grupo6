package com.conectatarot.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.conectatarot.app.network.DisputaRequest
import com.conectatarot.app.network.ResenaRequest
import com.conectatarot.app.network.RetrofitClient
import com.conectatarot.app.network.SesionItem
import kotlinx.coroutines.launch

class MisSesionesActivity : AppCompatActivity() {

    private lateinit var token: String
    private lateinit var rvSesiones: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var progress: ProgressBar

    private val paymentLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) cargarSesiones()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mis_sesiones)

        val prefs = getSharedPreferences("conectatarot", MODE_PRIVATE)
        token = prefs.getString("token", "") ?: ""

        rvSesiones = findViewById(R.id.rvSesiones)
        tvEmpty = findViewById(R.id.tvEmptySesiones)
        progress = findViewById(R.id.progressMisSesiones)

        rvSesiones.layoutManager = LinearLayoutManager(this)
        findViewById<TextView>(R.id.tvVolverSesiones).setOnClickListener { finish() }

        cargarSesiones()
    }

    private fun cargarSesiones() {
        progress.visibility = View.VISIBLE
        rvSesiones.visibility = View.GONE
        tvEmpty.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getMisSesiones("Bearer $token")
                progress.visibility = View.GONE
                if (response.isSuccessful && response.body() != null) {
                    val sesiones = response.body()!!.data ?: emptyList()
                    if (sesiones.isEmpty()) {
                        tvEmpty.visibility = View.VISIBLE
                    } else {
                        rvSesiones.visibility = View.VISIBLE
                        rvSesiones.adapter = SesionAdapter(
                            buildSectionedList(sesiones),
                            onCancelar = { sesion -> cancelarSesion(sesion.id) },
                            onCalificar = { sesion -> mostrarDialogoCalificar(sesion) },
                            onPagar = { sesion -> iniciarPago(sesion) },
                            onReportar = { sesion -> mostrarDialogoDisputa(sesion) }
                        )
                    }
                } else {
                    tvEmpty.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                progress.visibility = View.GONE
                Toast.makeText(this@MisSesionesActivity, "Error al cargar sesiones", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun esPasada(s: SesionItem): Boolean {
        return try {
            val fin = java.time.LocalDateTime.parse(s.fecha).plusMinutes(s.duracionMinutos.toLong())
            fin.isBefore(java.time.LocalDateTime.now())
        } catch (e: Exception) { false }
    }

    private fun buildSectionedList(sesiones: List<SesionItem>): List<SesionListItem> {
        val activas = sesiones.filter {
            (it.estado == "PENDIENTE" || it.estado == "CONFIRMADA") && !esPasada(it)
        }
        val historial = sesiones.filter {
            it.estado !in listOf("PENDIENTE", "CONFIRMADA") || esPasada(it)
        }

        val result = mutableListOf<SesionListItem>()
        if (activas.isNotEmpty()) {
            result.add(SesionListItem.Header("PRÓXIMAS"))
            activas.forEach { result.add(SesionListItem.Item(it)) }
        }
        if (historial.isNotEmpty()) {
            result.add(SesionListItem.Header("HISTORIAL"))
            historial.forEach { result.add(SesionListItem.Item(it)) }
        }
        return result
    }

    private fun cancelarSesion(sesionId: Int) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.cancelarSesion("Bearer $token", sesionId)
                if (response.isSuccessful) {
                    Toast.makeText(this@MisSesionesActivity, "Sesión cancelada", Toast.LENGTH_SHORT).show()
                    cargarSesiones()
                } else {
                    Toast.makeText(this@MisSesionesActivity, "No se pudo cancelar (${response.code()})", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MisSesionesActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun mostrarDialogoCalificar(sesion: SesionItem) {
        val prefs = getSharedPreferences("conectatarot", MODE_PRIVATE)
        val idUsuario = prefs.getInt("idUsuario", 0)

        lifecycleScope.launch {
            try {
                val existe = RetrofitClient.instance.existeResena("Bearer $token", sesion.id)
                if (existe.isSuccessful && existe.body()?.existe == true) {
                    Toast.makeText(this@MisSesionesActivity, "Ya calificaste esta sesión", Toast.LENGTH_SHORT).show()
                    return@launch
                }
            } catch (_: Exception) {}

            val tagLabels = listOf("Empático", "Predictivo", "Terapéutico", "Directo", "Holístico")
            val tagChecked = BooleanArray(tagLabels.size) { false }

            val ratingBar = RatingBar(this@MisSesionesActivity).apply { numStars = 5; stepSize = 1f; rating = 5f }
            val etComentario = EditText(this@MisSesionesActivity).apply {
                hint = "Comentario (opcional)"
                minLines = 2
            }

            val tvTagsLabel = android.widget.TextView(this@MisSesionesActivity).apply {
                text = "¿Cómo describirías al tarotista?"
                setPadding(0, 16, 0, 8)
                setTextColor(android.graphics.Color.DKGRAY)
            }

            val tagsLayout = LinearLayout(this@MisSesionesActivity).apply {
                orientation = LinearLayout.VERTICAL
            }
            tagLabels.forEachIndexed { i, label ->
                android.widget.CheckBox(this@MisSesionesActivity).apply {
                    text = label
                    setOnCheckedChangeListener { _, checked -> tagChecked[i] = checked }
                    tagsLayout.addView(this)
                }
            }

            val layout = LinearLayout(this@MisSesionesActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(40, 20, 40, 20)
                addView(ratingBar)
                addView(tvTagsLabel)
                addView(tagsLayout)
                addView(etComentario)
            }

            android.app.AlertDialog.Builder(this@MisSesionesActivity)
                .setTitle("⭐ Califica a ${sesion.nombreTarotista}")
                .setView(layout)
                .setPositiveButton("Enviar") { _, _ ->
                    lifecycleScope.launch {
                        try {
                            val tarotistaId = if (sesion.tarotistaId != null && sesion.tarotistaId > 0) {
                                sesion.tarotistaId
                            } else {
                                val resp = RetrofitClient.instance.getTarotistas("Bearer $token")
                                resp.body()?.data?.find { it.nombreProfesional == sesion.nombreTarotista }?.id ?: 0
                            }
                            val tagsStr = tagLabels.filterIndexed { i, _ -> tagChecked[i] }.joinToString(",")
                            val response = RetrofitClient.instance.crearResena(
                                "Bearer $token",
                                ResenaRequest(
                                    sesionId = sesion.id,
                                    tarotistaId = tarotistaId,
                                    usuarioId = idUsuario,
                                    calificacion = ratingBar.rating.toInt(),
                                    comentario = etComentario.text.toString().trim(),
                                    tags = tagsStr
                                )
                            )
                            if (response.isSuccessful) {
                                Toast.makeText(this@MisSesionesActivity, "¡Gracias por tu calificación!", Toast.LENGTH_SHORT).show()
                            } else {
                                val msg = try {
                                    org.json.JSONObject(response.errorBody()?.string() ?: "").optString("message", "Error al enviar")
                                } catch (_: Exception) { "Error al enviar" }
                                Toast.makeText(this@MisSesionesActivity, msg, Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(this@MisSesionesActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    private fun mostrarDialogoDisputa(sesion: SesionItem) {
        val tipos = arrayOf(
            "No vino el tarotista",
            "No asistí (avisar al admin)",
            "Problema técnico con la llamada",
            "Calidad deficiente de la sesión",
            "Otro"
        )
        val tiposCodigo = arrayOf(
            "NO_SHOW_TAROTISTA", "NO_SHOW_CLIENTE", "PROBLEMA_TECNICO", "CALIDAD", "OTRO"
        )
        var tipoSeleccionado = 0

        val spinnerTipo = android.widget.Spinner(this).apply {
            adapter = android.widget.ArrayAdapter(this@MisSesionesActivity,
                android.R.layout.simple_spinner_dropdown_item, tipos)
            setSelection(0)
            onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(p: android.widget.AdapterView<*>?, v: android.view.View?, pos: Int, id: Long) { tipoSeleccionado = pos }
                override fun onNothingSelected(p: android.widget.AdapterView<*>?) {}
            }
        }
        val etDesc = EditText(this).apply {
            hint = "Describe lo que ocurrió (opcional)"
            minLines = 3
            gravity = android.view.Gravity.TOP
        }
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 10)
            addView(android.widget.TextView(this@MisSesionesActivity).apply {
                text = "¿Qué ocurrió con la sesión?"
                setPadding(0, 0, 0, 12)
            })
            addView(spinnerTipo)
            addView(android.widget.TextView(this@MisSesionesActivity).apply {
                text = "Detalles adicionales"
                setPadding(0, 20, 0, 8)
            })
            addView(etDesc)
        }

        android.app.AlertDialog.Builder(this)
            .setTitle("⚠️ Reportar problema — ${sesion.nombreTarotista}")
            .setView(layout)
            .setPositiveButton("Enviar reporte") { _, _ ->
                lifecycleScope.launch {
                    try {
                        val resp = RetrofitClient.instance.reportarDisputa(
                            "Bearer $token",
                            DisputaRequest(sesion.id, tiposCodigo[tipoSeleccionado], etDesc.text.toString().trim())
                        )
                        if (resp.isSuccessful) {
                            Toast.makeText(this@MisSesionesActivity,
                                "Reporte enviado. El administrador lo revisará.", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this@MisSesionesActivity,
                                "Error al enviar (${resp.code()})", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@MisSesionesActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun iniciarPago(sesion: SesionItem) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.iniciarPago("Bearer $token", sesion.id)
                if (response.isSuccessful && response.body()?.success == true) {
                    val url = response.body()!!.url ?: ""
                    val wsToken = response.body()!!.token ?: ""
                    val fullUrl = if (wsToken.isNotBlank()) "$url?token_ws=$wsToken" else url
                    paymentLauncher.launch(
                        Intent(this@MisSesionesActivity, PaymentWebViewActivity::class.java).apply {
                            putExtra("sesionId", sesion.id)
                            putExtra("paymentUrl", fullUrl)
                        }
                    )
                } else {
                    Toast.makeText(this@MisSesionesActivity, "No se pudo iniciar el pago (${response.code()})", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MisSesionesActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
