package com.conectatarot.app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.conectatarot.app.network.ResenaItem
import com.conectatarot.app.network.RetrofitClient
import kotlinx.coroutines.launch

class TarotistaDetalleActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tarotista_detalle)

        val tarotistaId = intent.getIntExtra("tarotistaId", 0)
        val nombre = intent.getStringExtra("nombre") ?: ""
        val descripcion = intent.getStringExtra("descripcion") ?: ""
        val precio = intent.getDoubleExtra("precio", 0.0)
        val especialidades = intent.getStringExtra("especialidades") ?: ""

        val prefs = getSharedPreferences("conectatarot", MODE_PRIVATE)
        val token = prefs.getString("token", "") ?: ""

        findViewById<TextView>(R.id.tvDetNombre).text = "🌙 $nombre"
        findViewById<TextView>(R.id.tvDetDescripcion).text = descripcion
        findViewById<TextView>(R.id.tvDetPrecio).text = "$ ${precio.toInt()} / hora"
        findViewById<TextView>(R.id.tvDetEspecialidades).text = especialidades

        findViewById<Button>(R.id.btnAgendar).setOnClickListener {
            startActivity(Intent(this, AgendarActivity::class.java).apply {
                putExtra("tarotistaId", tarotistaId)
                putExtra("nombre", nombre)
            })
        }

        findViewById<TextView>(R.id.tvVolver).setOnClickListener { finish() }

        if (tarotistaId > 0) cargarResenas(token, tarotistaId)
    }

    private fun cargarResenas(token: String, tarotistaId: Int) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getResenasTarotista("Bearer $token", tarotistaId)
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    val resenas = body.data ?: emptyList()
                    val promedio = body.promedio
                    val total = body.total

                    val tvPromedio = findViewById<TextView>(R.id.tvDetPromedio)
                    val llResenas = findViewById<LinearLayout>(R.id.llResenas)
                    val tvSinResenas = findViewById<TextView>(R.id.tvSinResenas)

                    if (total == 0 || resenas.isEmpty()) {
                        tvPromedio.text = "Sin reseñas aún"
                        tvSinResenas.visibility = View.VISIBLE
                    } else {
                        val estrellas = "★".repeat(promedio.toInt()) + "☆".repeat(5 - promedio.toInt())
                        tvPromedio.text = "$estrellas $promedio ($total)"
                        resenas.forEach { resena -> llResenas.addView(buildResenaView(resena)) }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun buildResenaView(r: ResenaItem): View {
        val view = LayoutInflater.from(this).inflate(R.layout.item_resena, null)

        val estrellas = "★".repeat(r.calificacion) + "☆".repeat(5 - r.calificacion)
        view.findViewById<TextView>(R.id.tvResenaEstrellas).text = estrellas
        view.findViewById<TextView>(R.id.tvResenaFecha).text = r.fecha?.take(10) ?: ""

        val tvTags = view.findViewById<TextView>(R.id.tvResenaTags)
        if (!r.tags.isNullOrBlank()) {
            tvTags.text = r.tags.split(",").joinToString("  ·  ") { "• $it" }
            tvTags.visibility = View.VISIBLE
        }

        val tvComentario = view.findViewById<TextView>(R.id.tvResenaComentario)
        if (!r.comentario.isNullOrBlank()) {
            tvComentario.text = "\"${r.comentario}\""
            tvComentario.visibility = View.VISIBLE
        }

        return view
    }
}
