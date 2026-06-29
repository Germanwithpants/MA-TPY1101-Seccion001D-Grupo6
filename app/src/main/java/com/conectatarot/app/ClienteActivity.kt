package com.conectatarot.app

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.conectatarot.app.network.RetrofitClient
import com.conectatarot.app.network.Tarotista
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

class ClienteActivity : AppCompatActivity() {

    private var todosLosTarotistas = listOf<Tarotista>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cliente)

        val prefs = getSharedPreferences("conectatarot", MODE_PRIVATE)
        val token = prefs.getString("token", "") ?: ""
        val nombre = prefs.getString("nombre", "Cliente") ?: "Cliente"

        findViewById<TextView>(R.id.tvBienvenido).text = "Hola, $nombre 👋"

        findViewById<TextView>(R.id.tvLogout).setOnClickListener {
            com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
            getSharedPreferences("conectatarot", MODE_PRIVATE).edit().clear().apply()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        val rv = findViewById<RecyclerView>(R.id.rvTarotistas)
        rv.layoutManager = LinearLayoutManager(this)

        val etBuscar = findViewById<EditText>(R.id.etBuscar)

        // Bottom Navigation
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.selectedItemId = R.id.nav_inicio
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_sesiones -> {
                    startActivity(Intent(this, MisSesionesActivity::class.java))
                    false
                }
                R.id.nav_perfil -> {
                    startActivity(Intent(this, PerfilActivity::class.java))
                    false
                }
                R.id.nav_ajustes -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    false
                }
                else -> true
            }
        }

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getTarotistas("Bearer $token")
                if (response.isSuccessful && response.body() != null) {
                    todosLosTarotistas = response.body()!!.data ?: emptyList()
                    rv.adapter = TarotistaAdapter(todosLosTarotistas)
                }
            } catch (e: Exception) {
                // error silencioso
            }
        }

        etBuscar.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().lowercase()
                val filtrados = todosLosTarotistas.filter {
                    it.nombreProfesional.lowercase().contains(query) ||
                            (it.descripcion?.lowercase()?.contains(query) == true) ||
                            (it.especialidades?.any { esp -> esp.lowercase().contains(query) } == true)
                }
                rv.adapter = TarotistaAdapter(filtrados)
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }
}