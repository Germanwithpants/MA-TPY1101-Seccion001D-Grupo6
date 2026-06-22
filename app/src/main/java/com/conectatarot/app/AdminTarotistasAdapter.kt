package com.conectatarot.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.conectatarot.app.network.AdminTarotistaPendiente

class AdminTarotistasAdapter(
    private var items: List<AdminTarotistaPendiente>,
    private val onAprobar: (AdminTarotistaPendiente) -> Unit,
    private val onRechazar: (AdminTarotistaPendiente) -> Unit
) : RecyclerView.Adapter<AdminTarotistasAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombre: TextView = view.findViewById(R.id.tvTarotistaNombre)
        val tvDesc: TextView = view.findViewById(R.id.tvTarotistaDescripcion)
        val tvPrecio: TextView = view.findViewById(R.id.tvTarotistaPrecio)
        val tvEstado: TextView = view.findViewById(R.id.tvTarotistaEstado)
        val btnAprobar: Button = view.findViewById(R.id.btnAprobarTarotista)
        val btnRechazar: Button = view.findViewById(R.id.btnRechazarTarotista)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_admin_tarotista_pendiente, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val t = items[pos]
        h.tvNombre.text = t.nombreProfesional
        h.tvDesc.text = t.descripcion ?: "Sin descripción"
        h.tvPrecio.text = "$${t.precioBase ?: 0} / sesión"
        h.tvEstado.text = "Estado: ${t.estado ?: "PENDIENTE"}"
        h.tvEstado.setTextColor(0xFF9b59b6.toInt())

        val isPendiente = t.estado?.uppercase() == "PENDIENTE"
        h.btnAprobar.isEnabled = isPendiente
        h.btnRechazar.isEnabled = isPendiente
        h.btnAprobar.alpha = if (isPendiente) 1f else 0.4f
        h.btnRechazar.alpha = if (isPendiente) 1f else 0.4f

        h.btnAprobar.setOnClickListener { if (isPendiente) onAprobar(t) }
        h.btnRechazar.setOnClickListener { if (isPendiente) onRechazar(t) }
    }

    fun update(newItems: List<AdminTarotistaPendiente>) {
        items = newItems
        notifyDataSetChanged()
    }
}
