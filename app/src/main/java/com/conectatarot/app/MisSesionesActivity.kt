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
                            onPagar = { sesion -> iniciarPago(sesion) }
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

        val ratingBar = RatingBar(this).apply { numStars = 5; stepSize = 1f; rating = 5f }
        val etComentario = EditText(this).apply { hint = "Comentario (opcional)" }
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 20)
            addView(ratingBar)
            addView(etComentario)
        }

        android.app.AlertDialog.Builder(this)
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
                        val response = RetrofitClient.instance.crearResena(
                            ResenaRequest(
                                sesionId = sesion.id,
                                tarotistaId = tarotistaId,
                                usuarioId = idUsuario,
                                calificacion = ratingBar.rating.toInt(),
                                comentario = etComentario.text.toString().trim()
                            )
                        )
                        val msg = if (response.isSuccessful) "¡Gracias por tu calificación!" else "Error al enviar"
                        Toast.makeText(this@MisSesionesActivity, msg, Toast.LENGTH_SHORT).show()
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
