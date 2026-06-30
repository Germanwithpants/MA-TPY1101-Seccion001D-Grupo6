package com.conectatarot.app

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.conectatarot.app.network.RetrofitClient
import com.conectatarot.app.network.SesionItem
import com.conectatarot.app.network.Tarotista
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch

class ClienteActivity : AppCompatActivity() {

    private var todosLosTarotistas = listOf<Tarotista>()
    private var filtroEspecialidad: String? = null
    private lateinit var token: String
    private lateinit var rv: RecyclerView
    private lateinit var etBuscar: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cliente)

        val prefs = getSharedPreferences("conectatarot", MODE_PRIVATE)
        token = prefs.getString("token", "") ?: ""
        val nombre = prefs.getString("nombre", "Cliente") ?: "Cliente"

        findViewById<TextView>(R.id.tvBienvenido).text = "Hola, $nombre 👋"

        rv = findViewById(R.id.rvTarotistas)
        rv.layoutManager = LinearLayoutManager(this)
        etBuscar = findViewById(R.id.etBuscar)

        setupFiltros()

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_inicio
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_sesiones -> { startActivity(Intent(this, MisSesionesActivity::class.java)); false }
                R.id.nav_perfil   -> { startActivity(Intent(this, PerfilActivity::class.java)); false }
                R.id.nav_ajustes  -> { startActivity(Intent(this, SettingsActivity::class.java)); false }
                else -> true
            }
        }

        cargarTarotistas(null)
        cargarPendientesCalificar()

        etBuscar.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { aplicarFiltroLocal(s.toString()) }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    override fun onResume() {
        super.onResume()
        cargarTarotistas(filtroEspecialidad)
        cargarPendientesCalificar()
    }

    private fun cargarPendientesCalificar() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getMisSesiones("Bearer $token")
                if (response.isSuccessful) {
                    val sesiones = response.body()?.data ?: emptyList()
                    val pendientes = sesiones.filter { esSesionCompletadaSinCalificar(it) }
                    val card = findViewById<CardView>(R.id.cardPendientesCalificar)
                    val tv = findViewById<TextView>(R.id.tvPendientesCalificar)
                    if (pendientes.isNotEmpty()) {
                        tv.text = "⭐ Tienes ${pendientes.size} sesión${if (pendientes.size > 1) "es" else ""} por calificar"
                        card.visibility = View.VISIBLE
                        card.setOnClickListener {
                            startActivity(Intent(this@ClienteActivity, MisSesionesActivity::class.java))
                        }
                    } else {
                        card.visibility = View.GONE
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun esSesionCompletadaSinCalificar(s: SesionItem): Boolean {
        val completada = s.estado == "COMPLETADA" || (s.estado == "CONFIRMADA" && run {
            try {
                val fin = java.time.LocalDateTime.parse(s.fecha).plusMinutes(s.duracionMinutos.toLong())
                fin.isBefore(java.time.LocalDateTime.now())
            } catch (e: Exception) { false }
        })
        return completada && s.estadoPago == "PAGADO"
    }

    private fun setupFiltros() {
        val chipGroup = findViewById<ChipGroup>(R.id.chipGroupFiltros)
        val categorias = listOf("Todos", "Predictivo", "Terapéutico", "Holístico", "Negocios", "Amor")
        categorias.forEachIndexed { index, label ->
            val chip = Chip(this).apply {
                text = label
                isCheckable = true
                isChecked = index == 0
                setTextColor(getColor(android.R.color.white))
                chipBackgroundColor = android.content.res.ColorStateList.valueOf(
                    if (index == 0) android.graphics.Color.parseColor("#7d3cae")
                    else android.graphics.Color.parseColor("#2d1654")
                )
            }
            chipGroup.addView(chip)
        }

        chipGroup.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            val chip = group.findViewById<Chip>(checkedIds[0])
            val label = chip?.text?.toString() ?: "Todos"
            for (i in 0 until group.childCount) {
                val c = group.getChildAt(i) as? Chip ?: continue
                c.chipBackgroundColor = android.content.res.ColorStateList.valueOf(
                    if (c.isChecked) android.graphics.Color.parseColor("#7d3cae")
                    else android.graphics.Color.parseColor("#2d1654")
                )
            }
            filtroEspecialidad = if (label == "Todos") null else label
            cargarTarotistas(filtroEspecialidad)
        }
    }

    private fun cargarTarotistas(especialidad: String?) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getTarotistas("Bearer $token", especialidad)
                if (response.isSuccessful && response.body() != null) {
                    todosLosTarotistas = response.body()!!.data ?: emptyList()
                    aplicarFiltroLocal(etBuscar.text.toString())
                }
            } catch (_: Exception) {}
        }
    }

    private fun aplicarFiltroLocal(query: String) {
        val q = query.lowercase()
        val filtrados = if (q.isBlank()) todosLosTarotistas
        else todosLosTarotistas.filter {
            it.nombreProfesional.lowercase().contains(q) ||
            (it.descripcion?.lowercase()?.contains(q) == true) ||
            (it.especialidades?.any { esp -> esp.lowercase().contains(q) } == true)
        }
        rv.adapter = TarotistaAdapter(filtrados)
    }
}
