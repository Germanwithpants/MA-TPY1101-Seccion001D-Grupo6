package com.conectatarot.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.conectatarot.app.network.ResenaClienteRequest
import com.conectatarot.app.network.RetrofitClient
import com.conectatarot.app.network.SesionItem
import kotlinx.coroutines.launch

class TarotistaHomeActivity : AppCompatActivity() {

    private lateinit var token: String
    private lateinit var rvAgenda: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var progressAgenda: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tarotista_home)

        val prefs = getSharedPreferences("conectatarot", MODE_PRIVATE)
        token = prefs.getString("token", "") ?: ""
        val nombre = prefs.getString("nombre", "Tarotista") ?: "Tarotista"

        findViewById<TextView>(R.id.tvBienvenidoTarotista).text = "🔮 Bienvenida, $nombre"
        rvAgenda = findViewById(R.id.rvAgenda)
        tvEmpty = findViewById(R.id.tvEmptyAgenda)
        progressAgenda = findViewById(R.id.progressAgenda)
        rvAgenda.layoutManager = LinearLayoutManager(this)

        val irSettings = View.OnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
        findViewById<TextView>(R.id.tvAjustesTarotista).setOnClickListener(irSettings)
        findViewById<Button>(R.id.btnAjustesTarotista).setOnClickListener(irSettings)

        findViewById<Button>(R.id.btnEditarPerfilTarotista).setOnClickListener {
            startActivity(Intent(this, PerfilTarotistaActivity::class.java))
        }
        findViewById<Button>(R.id.btnDisponibilidad).setOnClickListener {
            startActivity(Intent(this, DisponibilidadActivity::class.java))
        }

        cargarSesiones()
    }

    private fun cargarSesiones() {
        progressAgenda.visibility = View.VISIBLE
        rvAgenda.visibility = View.GONE
        tvEmpty.visibility = View.GONE

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getSesionesTarotista("Bearer $token")
                progressAgenda.visibility = View.GONE
                if (response.isSuccessful && response.body() != null) {
                    val sesiones = (response.body()!!.data?.content ?: emptyList())
                        .sortedByDescending { it.fecha }
                    if (sesiones.isEmpty()) {
                        tvEmpty.visibility = View.VISIBLE
                    } else {
                        rvAgenda.visibility = View.VISIBLE
                        rvAgenda.adapter = AgendaAdapter(
                            buildSectionedList(sesiones),
                            onConfirmar = { sesion -> cambiarEstado(sesion.id, "confirmar") },
                            onRechazar  = { sesion -> cambiarEstado(sesion.id, "rechazar") },
                            onCalificarCliente = { sesion -> mostrarDialogoCalificarCliente(sesion) }
                        )
                    }
                } else {
                    tvEmpty.visibility = View.VISIBLE
                    Toast.makeText(this@TarotistaHomeActivity, "Error ${response.code()} al cargar sesiones", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                progressAgenda.visibility = View.GONE
                tvEmpty.visibility = View.VISIBLE
                Toast.makeText(this@TarotistaHomeActivity, "Error de conexión: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun esPasada(s: com.conectatarot.app.network.SesionItem): Boolean = try {
        val fin = java.time.LocalDateTime.parse(s.fecha).plusMinutes(s.duracionMinutos.toLong())
        fin.isBefore(java.time.LocalDateTime.now())
    } catch (e: Exception) { false }

    private fun buildSectionedList(sesiones: List<SesionItem>): List<AgendaListItem> {
        val pendientes = sesiones.filter {
            !esPasada(it) && it.estado in listOf("PENDIENTE", "CONFIRMADA")
        }
        val historial = sesiones.filter {
            esPasada(it) || it.estado in listOf("RECHAZADA", "CANCELADA", "COMPLETADA")
        }
        return buildList {
            if (pendientes.isNotEmpty()) {
                add(AgendaListItem.Header("SESIONES ACTIVAS"))
                pendientes.forEach { add(AgendaListItem.Item(it)) }
            }
            if (historial.isNotEmpty()) {
                add(AgendaListItem.Header("HISTORIAL"))
                historial.forEach { add(AgendaListItem.Item(it)) }
            }
        }
    }

    private fun cambiarEstado(id: Int, accion: String) {
        lifecycleScope.launch {
            try {
                val response = if (accion == "confirmar")
                    RetrofitClient.instance.confirmarSesion("Bearer $token", id)
                else
                    RetrofitClient.instance.rechazarSesion("Bearer $token", id)

                if (response.isSuccessful) {
                    Toast.makeText(this@TarotistaHomeActivity,
                        if (accion == "confirmar") "Sesión confirmada ✅" else "Sesión rechazada",
                        Toast.LENGTH_SHORT).show()
                    cargarSesiones()
                } else {
                    Toast.makeText(this@TarotistaHomeActivity, "Error al actualizar (${response.code()})", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@TarotistaHomeActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun mostrarDialogoCalificarCliente(sesion: SesionItem) {
        val prefs = getSharedPreferences("conectatarot", MODE_PRIVATE)
        val idTarotista = prefs.getInt("idUsuario", 0)

        val ratingBar = RatingBar(this).apply { numStars = 5; stepSize = 1f; rating = 5f }
        val etComentario = EditText(this).apply {
            hint = "Comentario sobre el cliente (opcional)"
            minLines = 2
        }
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 20)
            addView(ratingBar)
            addView(etComentario)
        }

        android.app.AlertDialog.Builder(this)
            .setTitle("⭐ Calificar a ${sesion.nombreCliente ?: "cliente"}")
            .setView(layout)
            .setPositiveButton("Enviar") { _, _ ->
                lifecycleScope.launch {
                    try {
                        val resp = RetrofitClient.instance.crearResenaCliente(
                            "Bearer $token",
                            ResenaClienteRequest(
                                sesionId = sesion.id,
                                tarotistaId = idTarotista,
                                calificacion = ratingBar.rating.toInt(),
                                comentario = etComentario.text.toString().trim()
                            )
                        )
                        Toast.makeText(this@TarotistaHomeActivity,
                            if (resp.isSuccessful) "Calificación enviada ✅" else "Error al enviar (${resp.code()})",
                            Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(this@TarotistaHomeActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
