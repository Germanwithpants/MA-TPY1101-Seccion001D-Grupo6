package com.conectatarot.app

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.conectatarot.app.network.SesionItem

sealed class AgendaListItem {
    data class Header(val titulo: String) : AgendaListItem()
    data class Item(val sesion: SesionItem) : AgendaListItem()
}

class AgendaAdapter(
    private val items: List<AgendaListItem>,
    private val onConfirmar: (SesionItem) -> Unit,
    private val onRechazar: (SesionItem) -> Unit,
    private val onCalificarCliente: ((SesionItem) -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM   = 1
    }

    class HeaderVH(view: View) : RecyclerView.ViewHolder(view) {
        val tv: TextView = view as TextView
    }

    class ItemVH(view: View) : RecyclerView.ViewHolder(view) {
        val tvCliente: TextView       = view.findViewById(R.id.tvAgendaCliente)
        val tvFecha: TextView         = view.findViewById(R.id.tvAgendaFecha)
        val tvFechaCreacion: TextView = view.findViewById(R.id.tvAgendaFechaCreacion)
        val tvEstado: TextView        = view.findViewById(R.id.tvAgendaEstado)
        val tvPrecio: TextView        = view.findViewById(R.id.tvAgendaPrecio)
        val tvPago: TextView          = view.findViewById(R.id.tvAgendaPago)
        val btnConfirmar: Button      = view.findViewById(R.id.btnConfirmarAgenda)
        val btnRechazar: Button       = view.findViewById(R.id.btnRechazarAgenda)
        val btnVideollamada: Button   = view.findViewById(R.id.btnVideollamadaAgenda)
        val btnCalificarCliente: Button = view.findViewById(R.id.btnCalificarCliente)
    }

    override fun getItemViewType(position: Int) =
        if (items[position] is AgendaListItem.Header) TYPE_HEADER else TYPE_ITEM

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        if (viewType == TYPE_HEADER)
            HeaderVH(LayoutInflater.from(parent.context).inflate(R.layout.item_sesion_header, parent, false))
        else
            ItemVH(LayoutInflater.from(parent.context).inflate(R.layout.item_agenda, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is AgendaListItem.Header -> (holder as HeaderVH).tv.text = item.titulo
            is AgendaListItem.Item   -> bindItem(holder as ItemVH, item.sesion)
        }
    }

    private fun esPasada(s: SesionItem): Boolean = try {
        val fin = java.time.LocalDateTime.parse(s.fecha).plusMinutes(s.duracionMinutos.toLong())
        fin.isBefore(java.time.LocalDateTime.now())
    } catch (e: Exception) { false }

    private fun enVentanaLlamada(s: SesionItem): Boolean {
        if (s.estado != "CONFIRMADA" || s.estadoPago != "PAGADO") return false
        return try {
            val inicio = java.time.LocalDateTime.parse(s.fecha)
            val ahora  = java.time.LocalDateTime.now()
            ahora.isAfter(inicio.minusMinutes(15)) && ahora.isBefore(inicio.plusMinutes(s.duracionMinutos.toLong()))
        } catch (e: Exception) { false }
    }

    private fun bindItem(holder: ItemVH, s: SesionItem) {
        holder.tvCliente.text = "👤 ${s.nombreCliente ?: "Cliente"}"
        holder.tvFecha.text   = "📅 Sesión: ${s.fecha.take(16).replace("T", " ")}"
        holder.tvPrecio.text  = "$ ${s.precioTotal.toInt()}"

        if (!s.fechaCreacion.isNullOrBlank()) {
            holder.tvFechaCreacion.text = "📋 Agendada: ${s.fechaCreacion.take(16).replace("T", " ")}"
            holder.tvFechaCreacion.visibility = View.VISIBLE
        } else {
            holder.tvFechaCreacion.visibility = View.GONE
        }

        val (color, texto) = when {
            esPasada(s) && s.estado in listOf("COMPLETADA", "CONFIRMADA") -> "#3498db" to "✔ Completada"
            s.estado == "PENDIENTE"  -> "#f39c12" to "⏳ Pendiente confirmación"
            s.estado == "CONFIRMADA" -> "#27ae60" to "✅ Confirmada"
            s.estado == "CANCELADA"  -> "#e74c3c" to "❌ Cancelada"
            s.estado == "RECHAZADA"  -> "#95a5a6" to "🚫 Rechazada"
            else                     -> "#9b59b6" to s.estado
        }
        holder.tvEstado.text = texto
        holder.tvEstado.setTextColor(android.graphics.Color.parseColor(color))

        // Only show payment status for active sessions — not for rejected/cancelled
        val estadoFinal = s.estado in listOf("RECHAZADA", "CANCELADA") || (esPasada(s) && s.estado == "COMPLETADA")
        if (estadoFinal) {
            holder.tvPago.visibility = View.GONE
        } else {
            holder.tvPago.visibility = View.VISIBLE
            val (pagoColor, pagoTexto) = when (s.estadoPago) {
                "PAGADO"    -> "#27ae60" to "💰 Pagado"
                "RECHAZADO" -> "#e74c3c" to "❌ Pago rechazado"
                else        -> "#f39c12" to "⏳ Pago pendiente"
            }
            holder.tvPago.text = pagoTexto
            holder.tvPago.setTextColor(android.graphics.Color.parseColor(pagoColor))
        }

        // Confirm / reject buttons only for pending non-past sessions
        if (s.estado == "PENDIENTE" && !esPasada(s)) {
            holder.btnConfirmar.visibility = View.VISIBLE
            holder.btnRechazar.visibility  = View.VISIBLE
            holder.btnConfirmar.setOnClickListener { onConfirmar(s) }
            holder.btnRechazar.setOnClickListener  { onRechazar(s) }
        } else {
            holder.btnConfirmar.visibility = View.GONE
            holder.btnRechazar.visibility  = View.GONE
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

        if (esPasada(s) && s.estado in listOf("COMPLETADA", "CONFIRMADA") && onCalificarCliente != null) {
            holder.btnCalificarCliente.visibility = View.VISIBLE
            holder.btnCalificarCliente.setOnClickListener { onCalificarCliente.invoke(s) }
        } else {
            holder.btnCalificarCliente.visibility = View.GONE
        }
    }
}
