package com.conectatarot.app

import android.content.Intent
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
        val tvPago: TextView = view.findViewById(R.id.tvAgendaPago)
        val btnConfirmar: Button = view.findViewById(R.id.btnConfirmarAgenda)
        val btnRechazar: Button = view.findViewById(R.id.btnRechazarAgenda)
        val btnVideollamada: Button = view.findViewById(R.id.btnVideollamadaAgenda)
    }

    private fun enVentanaLlamada(s: SesionItem): Boolean {
        if (s.estado != "CONFIRMADA" || s.estadoPago != "PAGADO") return false
        return try {
            val fecha = java.time.LocalDateTime.parse(s.fecha)
            val ahora = java.time.LocalDateTime.now()
            ahora.isAfter(fecha.minusMinutes(15)) && ahora.isBefore(fecha.plusMinutes(s.duracionMinutos.toLong()))
        } catch (e: Exception) { false }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_agenda, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val s = sesiones[position]
        holder.tvCliente.text = "👤 ${s.nombreCliente ?: "Cliente"}"
        holder.tvFecha.text = "📅 ${s.fecha.take(16).replace("T", " ")}"
        holder.tvPrecio.text = "$ ${s.precioTotal.toInt()}"

        val (color, texto) = when (s.estado) {
            "PENDIENTE"  -> "#f39c12" to "⏳ Pendiente confirmación"
            "CONFIRMADA" -> "#27ae60" to "✅ Confirmada"
            "CANCELADA"  -> "#e74c3c" to "❌ Cancelada"
            "RECHAZADA"  -> "#95a5a6" to "🚫 Rechazada"
            else         -> "#9b59b6" to s.estado
        }
        holder.tvEstado.text = texto
        holder.tvEstado.setTextColor(android.graphics.Color.parseColor(color))

        val (pagoColor, pagoTexto) = when (s.estadoPago) {
            "PAGADO"   -> "#27ae60" to "💰 Pagado"
            "RECHAZADO" -> "#e74c3c" to "❌ Pago rechazado"
            else        -> "#f39c12" to "⏳ Pago pendiente"
        }
        holder.tvPago.text = pagoTexto
        holder.tvPago.setTextColor(android.graphics.Color.parseColor(pagoColor))

        if (s.estado == "PENDIENTE") {
            holder.btnConfirmar.visibility = View.VISIBLE
            holder.btnRechazar.visibility = View.VISIBLE
            holder.btnConfirmar.setOnClickListener { onConfirmar(s) }
            holder.btnRechazar.setOnClickListener { onRechazar(s) }
        } else {
            holder.btnConfirmar.visibility = View.GONE
            holder.btnRechazar.visibility = View.GONE
        }

        if (enVentanaLlamada(s)) {
            holder.btnVideollamada.visibility = View.VISIBLE
            holder.btnVideollamada.setOnClickListener {
                val ctx = holder.itemView.context
                ctx.startActivity(Intent(ctx, VideoCallActivity::class.java).apply {
                    putExtra("sesionId", s.id)
                })
            }
        } else {
            holder.btnVideollamada.visibility = View.GONE
        }
    }

    override fun getItemCount() = sesiones.size
}
