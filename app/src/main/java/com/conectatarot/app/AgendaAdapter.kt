package com.conectatarot.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.conectatarot.app.network.SesionItem

class AgendaAdapter(
    private val sesiones: List<SesionItem>,
    private val onConfirmar: (SesionItem) -> Unit,
    private val onRechazar: (SesionItem) -> Unit
) : RecyclerView.Adapter<AgendaAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvCliente: TextView = view.findViewById(R.id.tvAgendaCliente)
        val tvFecha: TextView = view.findViewById(R.id.tvAgendaFecha)
        val tvEstado: TextView = view.findViewById(R.id.tvAgendaEstado)
        val tvPrecio: TextView = view.findViewById(R.id.tvAgendaPrecio)
        val btnConfirmar: Button = view.findViewById(R.id.btnConfirmarAgenda)
        val btnRechazar: Button = view.findViewById(R.id.btnRechazarAgenda)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_agenda, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val s = sesiones[position]
        holder.tvCliente.text = "👤 ${s.nombreCliente ?: "Cliente"}"
        holder.tvFecha.text = "📅 ${s.fecha.take(16).replace("T", " ")}"
        holder.tvPrecio.text = "$ ${s.precioTotal.toInt()}"

        val (color, texto) = when (s.estado) {
            "PENDIENTE" -> Pair("#f39c12", "⏳ Pendiente")
            "CONFIRMADA" -> Pair("#27ae60", "✅ Confirmada")
            "CANCELADA" -> Pair("#e74c3c", "❌ Cancelada")
            "RECHAZADA" -> Pair("#95a5a6", "🚫 Rechazada")
            else -> Pair("#9b59b6", s.estado)
        }
        holder.tvEstado.text = texto
        holder.tvEstado.setTextColor(android.graphics.Color.parseColor(color))

        if (s.estado == "PENDIENTE") {
            holder.btnConfirmar.visibility = View.VISIBLE
            holder.btnRechazar.visibility = View.VISIBLE
            holder.btnConfirmar.setOnClickListener { onConfirmar(s) }
            holder.btnRechazar.setOnClickListener { onRechazar(s) }
        } else {
            holder.btnConfirmar.visibility = View.GONE
            holder.btnRechazar.visibility = View.GONE
        }
    }

    override fun getItemCount() = sesiones.size
}