package com.conectatarot.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.conectatarot.app.network.DisputaItem

class AdminDisputasAdapter(
    private var items: List<DisputaItem>,
    private val onResolver: (DisputaItem) -> Unit,
    private val onEnRevision: (DisputaItem) -> Unit
) : RecyclerView.Adapter<AdminDisputasAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvTipo: TextView = view.findViewById(R.id.tvDisputaTipo)
        val tvPartes: TextView = view.findViewById(R.id.tvDisputaPartes)
        val tvDesc: TextView = view.findViewById(R.id.tvDisputaDesc)
        val tvEstado: TextView = view.findViewById(R.id.tvDisputaEstado)
        val tvFecha: TextView = view.findViewById(R.id.tvDisputaFecha)
        val btnRevision: Button = view.findViewById(R.id.btnDisputaRevision)
        val btnResolver: Button = view.findViewById(R.id.btnDisputaResolver)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_disputa, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val d = items[position]

        val tipoTexto = when (d.tipo) {
            "NO_SHOW_TAROTISTA" -> "🚫 No vino el tarotista"
            "NO_SHOW_CLIENTE"   -> "🚶 Cliente no asistió"
            "PROBLEMA_TECNICO"  -> "📡 Problema técnico"
            "CALIDAD"           -> "⭐ Calidad deficiente"
            else                -> "❓ Otro"
        }
        holder.tvTipo.text = tipoTexto
        holder.tvPartes.text = "Cliente: ${d.nombreCliente}  •  Tarotista: ${d.nombreTarotista}"
        holder.tvDesc.text = if (d.descripcion.isNullOrBlank()) "Sin descripción adicional" else d.descripcion
        holder.tvFecha.text = "Sesión #${d.sesionId}  •  ${d.fechaCreacion.take(16).replace("T", " ")}"

        val (color, estadoTexto) = when (d.estado) {
            "PENDIENTE"   -> "#f39c12" to "⏳ Pendiente"
            "EN_REVISION" -> "#3498db" to "🔍 En revisión"
            "RESUELTA"    -> "#27ae60" to "✅ Resuelta"
            else          -> "#9b59b6" to d.estado
        }
        holder.tvEstado.text = estadoTexto
        holder.tvEstado.setTextColor(android.graphics.Color.parseColor(color))

        holder.btnRevision.visibility = if (d.estado == "PENDIENTE") View.VISIBLE else View.GONE
        holder.btnResolver.visibility = if (d.estado != "RESUELTA") View.VISIBLE else View.GONE

        holder.btnRevision.setOnClickListener { onEnRevision(d) }
        holder.btnResolver.setOnClickListener { onResolver(d) }
    }

    fun update(newItems: List<DisputaItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
