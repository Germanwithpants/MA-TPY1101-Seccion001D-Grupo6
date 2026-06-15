package com.conectatarot.app

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.conectatarot.app.network.RetrofitClient
import kotlinx.coroutines.launch
import android.widget.LinearLayout
import android.content.Intent

class MisSesionesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mis_sesiones)

        val prefs = getSharedPreferences("conectatarot", MODE_PRIVATE)
        val token = prefs.getString("token", "") ?: ""

        val rvSesiones = findViewById<RecyclerView>(R.id.rvSesiones)
        val tvVolver = findViewById<TextView>(R.id.tvVolverSesiones)
        val tvEmpty = findViewById<TextView>(R.id.tvEmptySesiones)

        rvSesiones.layoutManager = LinearLayoutManager(this)

        tvVolver.setOnClickListener { finish() }

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getMisSesiones("Bearer $token")
                if (response.isSuccessful && response.body() != null) {
                    val sesiones = response.body()!!.data ?: emptyList()
                    if (sesiones.isEmpty()) {
                        tvEmpty.visibility = android.view.View.VISIBLE
                        rvSesiones.visibility = android.view.View.GONE
                    } else {
                        tvEmpty.visibility = android.view.View.GONE
                        rvSesiones.visibility = android.view.View.VISIBLE
                        rvSesiones.adapter = SesionAdapter(
                            sesiones,
                            onCancelar = { sesion -> cancelarSesion(token, sesion.id) },
                            onCalificar = { sesion -> mostrarDialogoCalificar(token, sesion) },
                            onPagar = { sesion -> iniciarPago(sesion)}
                        )
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@MisSesionesActivity, "Error al cargar sesiones", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun cancelarSesion(token: String, sesionId: Int) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.cancelarSesion("Bearer $token", sesionId)
                if (response.isSuccessful) {
                    Toast.makeText(this@MisSesionesActivity, "Sesión cancelada", Toast.LENGTH_SHORT).show()
                    recreate()
                } else {
                    Toast.makeText(this@MisSesionesActivity, "No se pudo cancelar", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MisSesionesActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun mostrarDialogoCalificar(token: String, sesion: com.conectatarot.app.network.SesionItem) {
        val prefs = getSharedPreferences("conectatarot", MODE_PRIVATE)
        val idUsuario = prefs.getInt("idUsuario", 0)

        val ratingBar = android.widget.RatingBar(this)
        ratingBar.numStars = 5
        ratingBar.stepSize = 1f
        ratingBar.rating = 5f

        val etComentario = EditText(this)
        etComentario.hint = "Comentario (opcional)"

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(40, 20, 40, 20)
        layout.addView(ratingBar)
        layout.addView(etComentario)

        android.app.AlertDialog.Builder(this)
            .setTitle("Califica a ${sesion.nombreTarotista}")
            .setView(layout)
            .setPositiveButton("Enviar") { _, _ ->
                lifecycleScope.launch {
                    try {
                        // Necesitamos el tarotistaId, lo obtenemos desde el endpoint de tarotistas
                        val tarotistasResp = RetrofitClient.instance.getTarotistas("Bearer $token")
                        val tarotista = tarotistasResp.body()?.data?.find { it.nombreProfesional == sesion.nombreTarotista }
                        val tarotistaId = tarotista?.id ?: 1

                        val response = RetrofitClient.instance.crearResena(
                            com.conectatarot.app.network.ResenaRequest(
                                sesionId = sesion.id,
                                tarotistaId = tarotistaId,
                                usuarioId = idUsuario,
                                calificacion = ratingBar.rating.toInt(),
                                comentario = etComentario.text.toString()
                            )
                        )
                        if (response.isSuccessful) {
                            Toast.makeText(this@MisSesionesActivity, "¡Gracias por tu calificación!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@MisSesionesActivity, "Error al enviar calificación", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@MisSesionesActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
    private fun iniciarPago(sesion: com.conectatarot.app.network.SesionItem) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.iniciarPago(sesion.id)
                if (response.isSuccessful && response.body()?.success == true) {
                    val url = response.body()!!.url
                    val token = response.body()!!.token
                    val fullUrl = "$url?token_ws=$token"
                    val intent = Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(fullUrl))
                    startActivity(intent)
                } else {
                    Toast.makeText(this@MisSesionesActivity, "Error al iniciar pago", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MisSesionesActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
            }
        }
    }

}