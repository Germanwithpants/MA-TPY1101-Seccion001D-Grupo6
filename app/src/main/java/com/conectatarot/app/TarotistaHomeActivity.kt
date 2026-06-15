package com.conectatarot.app

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.conectatarot.app.network.RetrofitClient
import kotlinx.coroutines.launch

class TarotistaHomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tarotista_home)

        val prefs = getSharedPreferences("conectatarot", MODE_PRIVATE)
        val token = prefs.getString("token", "") ?: ""
        val nombre = prefs.getString("nombre", "Tarotista") ?: "Tarotista"

        findViewById<TextView>(R.id.tvBienvenidoTarotista).text = "🔮 Bienvenida, $nombre"

        val rvAgenda = findViewById<RecyclerView>(R.id.rvAgenda)
        val tvEmpty = findViewById<TextView>(R.id.tvEmptyAgenda)
        val tvCerrar = findViewById<TextView>(R.id.tvCerrarSesionTarotista)

        rvAgenda.layoutManager = LinearLayoutManager(this)

        tvCerrar.setOnClickListener {
            getSharedPreferences("conectatarot", MODE_PRIVATE).edit().clear().apply()
            startActivity(android.content.Intent(this, MainActivity::class.java))
            finish()
        }
        cargarSesiones(token, rvAgenda, tvEmpty)

        findViewById<android.widget.Button>(R.id.btnEditarPerfilTarotista).setOnClickListener {
            startActivity(android.content.Intent(this, PerfilTarotistaActivity::class.java))
        }
    }

    private fun cargarSesiones(token: String, rv: RecyclerView, tvEmpty: TextView) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getSesionesTarotista("Bearer $token")
                if (response.isSuccessful && response.body() != null) {
                    val sesiones = response.body()!!.data?.content ?: emptyList()
                    if (sesiones.isEmpty()) {
                        tvEmpty.visibility = android.view.View.VISIBLE
                        rv.visibility = android.view.View.GONE
                    } else {
                        tvEmpty.visibility = android.view.View.GONE
                        rv.visibility = android.view.View.VISIBLE
                        rv.adapter = AgendaAdapter(sesiones,
                            onConfirmar = { sesion -> cambiarEstado(token, sesion.id, "confirmar", rv, tvEmpty) },
                            onRechazar = { sesion -> cambiarEstado(token, sesion.id, "rechazar", rv, tvEmpty) }
                        )
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@TarotistaHomeActivity, "Error al cargar agenda", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun cambiarEstado(token: String, id: Int, accion: String, rv: RecyclerView, tvEmpty: TextView) {
        lifecycleScope.launch {
            try {
                val response = if (accion == "confirmar")
                    RetrofitClient.instance.confirmarSesion("Bearer $token", id)
                else
                    RetrofitClient.instance.rechazarSesion("Bearer $token", id)

                if (response.isSuccessful) {
                    val msg = if (accion == "confirmar") "Sesión confirmada ✅" else "Sesión rechazada"
                    Toast.makeText(this@TarotistaHomeActivity, msg, Toast.LENGTH_SHORT).show()
                    cargarSesiones(token, rv, tvEmpty)
                }
            } catch (e: Exception) {
                Toast.makeText(this@TarotistaHomeActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
            }
        }
    }
}