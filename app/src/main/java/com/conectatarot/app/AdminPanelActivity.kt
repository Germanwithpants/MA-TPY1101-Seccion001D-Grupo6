package com.conectatarot.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.conectatarot.app.network.RetrofitClient
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

class AdminPanelActivity : AppCompatActivity() {

    private lateinit var token: String

    // Sections
    private lateinit var sectionDashboard: View
    private lateinit var sectionUsuarios: View
    private lateinit var sectionTarotistas: View
    private lateinit var sectionPagos: View
    private lateinit var sectionDisputas: View

    // Adapters
    private lateinit var usuariosAdapter: AdminUsuariosAdapter
    private lateinit var tarotistasAdapter: AdminTarotistasAdapter
    private lateinit var pagosAdapter: AdminPagosAdapter
    private lateinit var disputasAdapter: AdminDisputasAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_panel)

        token = "Bearer ${getSharedPreferences("conectatarot", MODE_PRIVATE).getString("token", "") ?: ""}"

        sectionDashboard  = findViewById(R.id.sectionDashboard)
        sectionUsuarios   = findViewById(R.id.sectionUsuarios)
        sectionTarotistas = findViewById(R.id.sectionTarotistas)
        sectionPagos      = findViewById(R.id.sectionPagos)
        sectionDisputas   = findViewById(R.id.sectionDisputas)

        setupRecyclerViews()

        findViewById<TextView>(R.id.tvAdminLogout).setOnClickListener {
            getSharedPreferences("conectatarot", MODE_PRIVATE).edit().clear().apply()
            startActivity(Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavAdmin)
        bottomNav.selectedItemId = R.id.admin_nav_dashboard
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.admin_nav_dashboard  -> { showSection(0); cargarDashboard(); true }
                R.id.admin_nav_usuarios   -> { showSection(1); cargarUsuarios();  true }
                R.id.admin_nav_tarotistas -> { showSection(2); cargarTarotistas(); true }
                R.id.admin_nav_pagos      -> { showSection(3); cargarPagos();     true }
                R.id.admin_nav_disputas   -> { showSection(4); cargarDisputas();  true }
                else -> false
            }
        }

        cargarDashboard()
    }

    private fun setupRecyclerViews() {
        val miId = getSharedPreferences("conectatarot", MODE_PRIVATE).getInt("idUsuario", -1)
        usuariosAdapter = AdminUsuariosAdapter(emptyList()) { usuario ->
            if (usuario.idUsuario == miId) {
                Toast.makeText(this, "No puedes bloquearte a ti mismo", Toast.LENGTH_SHORT).show()
                return@AdminUsuariosAdapter
            }
            val activo = usuario.activo != false
            lifecycleScope.launch {
                try {
                    val resp = if (activo)
                        RetrofitClient.instance.bloquearUsuario(token, usuario.idUsuario)
                    else
                        RetrofitClient.instance.desbloquearUsuario(token, usuario.idUsuario)
                    if (resp.isSuccessful) {
                        Toast.makeText(this@AdminPanelActivity,
                            if (activo) "Usuario bloqueado" else "Usuario desbloqueado",
                            Toast.LENGTH_SHORT).show()
                        cargarUsuarios()
                    } else {
                        Toast.makeText(this@AdminPanelActivity, "Error (${resp.code()})", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@AdminPanelActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
                }
            }
        }
        findViewById<RecyclerView>(R.id.rvUsuarios).apply {
            layoutManager = LinearLayoutManager(this@AdminPanelActivity)
            adapter = usuariosAdapter
        }

        tarotistasAdapter = AdminTarotistasAdapter(
            emptyList(),
            onAprobar = { t ->
                lifecycleScope.launch {
                    try {
                        val resp = RetrofitClient.instance.aprobarTarotista(token, t.id)
                        if (resp.isSuccessful) {
                            Toast.makeText(this@AdminPanelActivity, "✅ ${t.nombreProfesional} aprobado", Toast.LENGTH_SHORT).show()
                            cargarTarotistas()
                        } else {
                            Toast.makeText(this@AdminPanelActivity, "Error (${resp.code()})", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@AdminPanelActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onRechazar = { t ->
                lifecycleScope.launch {
                    try {
                        val resp = RetrofitClient.instance.rechazarTarotista(token, t.id)
                        if (resp.isSuccessful) {
                            Toast.makeText(this@AdminPanelActivity, "❌ ${t.nombreProfesional} rechazado", Toast.LENGTH_SHORT).show()
                            cargarTarotistas()
                        } else {
                            Toast.makeText(this@AdminPanelActivity, "Error (${resp.code()})", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@AdminPanelActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
        findViewById<RecyclerView>(R.id.rvTarotistas).apply {
            layoutManager = LinearLayoutManager(this@AdminPanelActivity)
            adapter = tarotistasAdapter
        }

        pagosAdapter = AdminPagosAdapter(emptyList())
        findViewById<RecyclerView>(R.id.rvPagos).apply {
            layoutManager = LinearLayoutManager(this@AdminPanelActivity)
            adapter = pagosAdapter
        }

        disputasAdapter = AdminDisputasAdapter(
            emptyList(),
            onResolver = { d -> resolverDisputa(d) },
            onEnRevision = { d ->
                lifecycleScope.launch {
                    try {
                        RetrofitClient.instance.marcarEnRevision(token, d.id)
                        cargarDisputas()
                    } catch (e: Exception) { }
                }
            }
        )
        findViewById<RecyclerView>(R.id.rvDisputas).apply {
            layoutManager = LinearLayoutManager(this@AdminPanelActivity)
            adapter = disputasAdapter
        }
    }

    private fun showSection(index: Int) {
        listOf(sectionDashboard, sectionUsuarios, sectionTarotistas, sectionPagos, sectionDisputas)
            .forEachIndexed { i, v -> v.visibility = if (i == index) View.VISIBLE else View.GONE }
    }

    private fun cargarDashboard() {
        val progress = findViewById<ProgressBar>(R.id.progressDashboard)
        progress.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.instance.getAdminEstadisticas(token)
                progress.visibility = View.GONE
                if (resp.isSuccessful) {
                    val d = resp.body()?.data ?: return@launch
                    findViewById<TextView>(R.id.tvStatTotalUsuarios).text = d.totalUsuarios.toString()
                    findViewById<TextView>(R.id.tvStatActivosUsuarios).text = "activos: ${d.usuariosActivos}"
                    findViewById<TextView>(R.id.tvStatTarotistaActivos).text = d.tarotistaActivos.toString()
                    findViewById<TextView>(R.id.tvStatTotalSesiones).text = d.totalSesiones.toString()
                    findViewById<TextView>(R.id.tvStatIngresoTotal).text = "$${"%.0f".format(d.ingresoTotal)}"
                }
            } catch (e: Exception) {
                progress.visibility = View.GONE
            }
        }
    }

    private fun cargarUsuarios() {
        val progress = findViewById<ProgressBar>(R.id.progressUsuarios)
        val tvEmpty = findViewById<TextView>(R.id.tvEmptyUsuarios)
        progress.visibility = View.VISIBLE
        tvEmpty.visibility = View.GONE
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.instance.getAdminUsuarios(token)
                progress.visibility = View.GONE
                if (resp.isSuccessful) {
                    val lista = resp.body()?.data ?: emptyList()
                    if (lista.isEmpty()) tvEmpty.visibility = View.VISIBLE
                    else usuariosAdapter.update(lista)
                }
            } catch (e: Exception) {
                progress.visibility = View.GONE
                Toast.makeText(this@AdminPanelActivity, "Error al cargar usuarios", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun cargarTarotistas() {
        val progress = findViewById<ProgressBar>(R.id.progressTarotistas)
        val tvEmpty = findViewById<TextView>(R.id.tvEmptyTarotistas)
        progress.visibility = View.VISIBLE
        tvEmpty.visibility = View.GONE
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.instance.getTarotistasPendientes(token)
                progress.visibility = View.GONE
                if (resp.isSuccessful) {
                    val lista = resp.body()?.data ?: emptyList()
                    tarotistasAdapter.update(lista)
                    tvEmpty.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
                }
            } catch (e: Exception) {
                progress.visibility = View.GONE
                Toast.makeText(this@AdminPanelActivity, "Error al cargar tarotistas", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun cargarPagos() {
        val progress = findViewById<ProgressBar>(R.id.progressPagos)
        val tvResumen = findViewById<TextView>(R.id.tvResumenComisiones)
        progress.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val pagosResp = RetrofitClient.instance.getAdminPagos(token)
                val comResp = RetrofitClient.instance.getAdminComisiones(token)
                progress.visibility = View.GONE
                if (pagosResp.isSuccessful) {
                    val lista = pagosResp.body()?.data ?: emptyList()
                    pagosAdapter.update(lista)
                }
                if (comResp.isSuccessful) {
                    val data = comResp.body()?.data
                    if (data != null) {
                        tvResumen.text = "Total comisiones: $${"%.0f".format(data.totalComisiones)} (${data.cantidad} pagos)"
                    }
                }
            } catch (e: Exception) {
                progress.visibility = View.GONE
                Toast.makeText(this@AdminPanelActivity, "Error al cargar pagos", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun cargarDisputas() {
        val progress = findViewById<ProgressBar>(R.id.progressDisputas)
        val tvEmpty = findViewById<TextView>(R.id.tvEmptyDisputas)
        progress.visibility = View.VISIBLE
        tvEmpty.visibility = View.GONE
        lifecycleScope.launch {
            try {
                val resp = RetrofitClient.instance.getAdminDisputas(token)
                progress.visibility = View.GONE
                if (resp.isSuccessful) {
                    val lista = resp.body()?.data ?: emptyList()
                    if (lista.isEmpty()) tvEmpty.visibility = View.VISIBLE
                    else disputasAdapter.update(lista)
                }
            } catch (e: Exception) {
                progress.visibility = View.GONE
                Toast.makeText(this@AdminPanelActivity, "Error al cargar disputas", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun resolverDisputa(d: com.conectatarot.app.network.DisputaItem) {
        val etResolucion = android.widget.EditText(this).apply {
            hint = "Escribe la resolución (opcional)"
            minLines = 2
            setPadding(40, 20, 40, 10)
        }
        android.app.AlertDialog.Builder(this)
            .setTitle("Resolver disputa #${d.id}")
            .setView(etResolucion)
            .setPositiveButton("Marcar resuelta") { _, _ ->
                lifecycleScope.launch {
                    try {
                        RetrofitClient.instance.resolverDisputa(token, d.id,
                            mapOf("resolucion" to etResolucion.text.toString().trim()))
                        Toast.makeText(this@AdminPanelActivity, "Disputa resuelta", Toast.LENGTH_SHORT).show()
                        cargarDisputas()
                    } catch (e: Exception) {
                        Toast.makeText(this@AdminPanelActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
