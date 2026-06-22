package com.conectatarot.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.conectatarot.app.network.RetrofitClient
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

        findViewById<TextView>(R.id.tvCerrarSesionTarotista).setOnClickListener {
            prefs.edit().clear().apply()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

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
                    val sesiones = response.body()!!.data?.content ?: emptyList()
                    if (sesiones.isEmpty()) {
                        tvEmpty.visibility = View.VISIBLE
                    } else {
                        rvAgenda.visibility = View.VISIBLE
                        rvAgenda.adapter = AgendaAdapter(
                            sesiones,
                            onConfirmar = { sesion -> cambiarEstado(sesion.id, "confirmar") },
                            onRechazar = { sesion -> cambiarEstado(sesion.id, "rechazar") }
                        )
                    }
                } else {
                    tvEmpty.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                progressAgenda.visibility = View.GONE
                Toast.makeText(this@TarotistaHomeActivity, "Error al cargar agenda", Toast.LENGTH_SHORT).show()
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
                    val msg = if (accion == "confirmar") "Sesión confirmada ✅" else "Sesión rechazada"
                    Toast.makeText(this@TarotistaHomeActivity, msg, Toast.LENGTH_SHORT).show()
                    cargarSesiones()
                } else {
                    Toast.makeText(this@TarotistaHomeActivity, "Error al actualizar (${response.code()})", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@TarotistaHomeActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
