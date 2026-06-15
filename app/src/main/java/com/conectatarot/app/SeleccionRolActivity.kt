package com.conectatarot.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class SeleccionRolActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_seleccion_rol)

        val btnCliente = findViewById<Button>(R.id.btnSoyCliente)
        val btnTarotista = findViewById<Button>(R.id.btnSoyTarotista)

        btnCliente.setOnClickListener {
            startActivity(Intent(this, ClienteActivity::class.java))
            finish()
        }

        btnTarotista.setOnClickListener {
            startActivity(Intent(this, CompletarPerfilTarotistaActivity::class.java))
            finish()
        }
    }
}