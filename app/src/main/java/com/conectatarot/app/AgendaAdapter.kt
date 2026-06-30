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
    private val onRechazar: (SesionItem) -> Unit,
    private val onCalificarCliente: ((SesionItem) -> Unit)? = null
) : RecyclerView.Adapter<AgendaAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvCliente: TextView = view.findViewById(R.id.tvAgendaCliente)
        val tvFecha: TextView = view.findViewById(R.id.tvAgendaFecha)
        val tvFechaCreacion: TextView = view.findViewById(R.id.tvAgendaFechaCreacion)
        val tvEstado: TextView = view.findViewById(R.id.tvAgendaEstado)
        val tvPrecio: TextView = view.findViewById(R.id.tvAgendaPrecio)
        val tvPago: TextView = view.findViewById(R.id.tvAgendaPago)
        val btnConfirmar: Button = view.findViewById(R.id.btnConfirmarAgenda)
        val btnRechazar: Button = view.findViewById(R.id.btnRechazarAgenda)
        val btnVideollamada: Button = view.findViewById(R.id.btnVideollamadaAgenda)
        val btnCalificarCliente: Button = view.findViewById(R.id.btnCalificarCliente)
    }

    private fun esPasada(s: SesionItem): Boolean {
        return try {
            val fin = java.time.LocalDateTime.parse(s.fecha).plusMinutes(s.duracionMinutos.toLong())
            fin.isBefore(java.time.LocalDateTime.now())
        } catch (e: Exception) { false }
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

    override fun getItemCount() = sesiones.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val s = sesiones[position]
        holder.tvCliente.text = "👤 ${s.nombreCliente ?: "Cliente"}"
        holder.tvFecha.text = "📅 Sesión: ${s.fecha.take(16).replace("T", " ")}"
        holder.tvPrecio.text = "$ ${s.precioTotal.toInt()}"

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
            else -> "#9b59b6" to s.estado
        }
        holder.tvEstado.text = texto
        holder.tvEstado.setTextColor(android.graphics.Color.parseColor(color))

        val (pagoColor, pagoTexto) = when (s.estadoPago) {
            "PAGADO"    -> "#27ae60" to "💰 Pagado"
            "RECHAZADO" -> "#e74c3c" to "❌ Pago rechazado"
            else        -> "#f39c12" to "⏳ Pago pendiente"
        }
        holder.tvPago.text = pagoTexto
        holder.tvPago.setTextColor(android.graphics.Color.parseColor(pagoColor))

        if (s.estado == "PENDIENTE" && !esPasada(s)) {
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

        if (esPasada(s) && s.estado in listOf("COMPLETADA", "CONFIRMADA") && onCalificarCliente != null) {
            holder.btnCalificarCliente.visibility = View.VISIBLE
            holder.btnCalificarCliente.setOnClickListener { onCalificarCliente.invoke(s) }
        } else {
            holder.btnCalificarCliente.visibility = View.GONE
        }
    }
}
