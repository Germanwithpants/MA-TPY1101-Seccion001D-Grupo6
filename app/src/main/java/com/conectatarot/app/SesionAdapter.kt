package com.conectatarot.app

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.conectatarot.app.network.SesionItem

sealed class SesionListItem {
    data class Header(val title: String) : SesionListItem()
    data class Item(val sesion: SesionItem) : SesionListItem()
}

class SesionAdapter(
    private val items: List<SesionListItem>,
    private val onCancelar: (SesionItem) -> Unit,
    private val onCalificar: (SesionItem) -> Unit,
    private val onPagar: (SesionItem) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
    }

    class HeaderVH(view: View) : RecyclerView.ViewHolder(view) {
        val tv: TextView = view as TextView
    }

    class ItemVH(view: View) : RecyclerView.ViewHolder(view) {
        val tvTarotista: TextView = view.findViewById(R.id.tvSesionTarotista)
        val tvFecha: TextView = view.findViewById(R.id.tvSesionFecha)
        val tvEstado: TextView = view.findViewById(R.id.tvSesionEstado)
        val tvPrecio: TextView = view.findViewById(R.id.tvSesionPrecio)
        val btnCancelar: Button = view.findViewById(R.id.btnCancelarSesion)
        val btnPagar: Button = view.findViewById(R.id.btnPagarSesion)
        val btnVideollamada: Button = view.findViewById(R.id.btnVideollamada)
        val tvVideollamadaInfo: TextView = view.findViewById(R.id.tvVideollamadaInfo)
    }

    override fun getItemViewType(position: Int) =
        if (items[position] is SesionListItem.Header) TYPE_HEADER else TYPE_ITEM

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        if (viewType == TYPE_HEADER)
            HeaderVH(LayoutInflater.from(parent.context).inflate(R.layout.item_sesion_header, parent, false))
        else
            ItemVH(LayoutInflater.from(parent.context).inflate(R.layout.item_sesion, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is SesionListItem.Header -> (holder as HeaderVH).tv.text = item.title
            is SesionListItem.Item   -> bindItem(holder as ItemVH, item.sesion)
        }
    }

    private fun sesionCompletada(s: SesionItem): Boolean {
        if (s.estado != "CONFIRMADA") return false
        return try {
            val fin = java.time.LocalDateTime.parse(s.fecha).plusMinutes(s.duracionMinutos.toLong())
            fin.isBefore(java.time.LocalDateTime.now())
        } catch (e: Exception) { false }
    }

    private fun ventanaVideollamada(s: SesionItem): Boolean {
        if (s.estado != "CONFIRMADA" || s.estadoPago != "PAGADO") return false
        return try {
            val fecha = java.time.LocalDateTime.parse(s.fecha)
            val ahora = java.time.LocalDateTime.now()
            ahora.isAfter(fecha.minusMinutes(15)) && ahora.isBefore(fecha.plusMinutes(s.duracionMinutos.toLong()))
        } catch (e: Exception) { false }
    }

    private fun esSesionFuturaPagada(s: SesionItem): Boolean {
        if (s.estado != "CONFIRMADA" || s.estadoPago != "PAGADO") return false
        return try {
            java.time.LocalDateTime.parse(s.fecha).isAfter(java.time.LocalDateTime.now())
        } catch (e: Exception) { false }
    }

    private fun bindItem(holder: ItemVH, s: SesionItem) {
        holder.tvTarotista.text = "🌙 ${s.nombreTarotista}"
        holder.tvFecha.text = "📅 ${s.fecha.take(16).replace("T", " ")}"
        holder.tvPrecio.text = "$ ${s.precioTotal.toInt()}"

        val (color, texto) = when {
            sesionCompletada(s)                                  -> "#3498db" to "✔ Completada"
            s.estado == "PENDIENTE" && s.estadoPago == "PAGADO" -> "#27ae60" to "✅ Pagada – pendiente confirmación"
            s.estado == "PENDIENTE"                              -> "#f39c12" to "⏳ Pendiente de pago"
            s.estado == "CONFIRMADA"                             -> "#27ae60" to "✅ Confirmada"
            s.estado == "CANCELADA"                              -> "#e74c3c" to "❌ Cancelada"
            s.estado == "RECHAZADA"                              -> "#95a5a6" to "🚫 Rechazada"
            else                                                 -> "#9b59b6" to s.estado
        }
        holder.tvEstado.text = texto
        holder.tvEstado.setTextColor(android.graphics.Color.parseColor(color))

        holder.btnCancelar.isEnabled = true
        when {
            s.estado == "PENDIENTE" && s.estadoPago != "PAGADO" -> {
                holder.btnCancelar.visibility = View.VISIBLE
                holder.btnCancelar.text = "Cancelar sesión"
                holder.btnCancelar.setBackgroundColor(android.graphics.Color.parseColor("#e74c3c"))
                holder.btnCancelar.setOnClickListener { onCancelar(s) }
                holder.btnPagar.visibility = View.VISIBLE
                holder.btnPagar.setOnClickListener { onPagar(s) }
            }
            s.estado == "PENDIENTE" && s.estadoPago == "PAGADO" -> {
                holder.btnCancelar.visibility = View.VISIBLE
                holder.btnCancelar.text = "Cancelar (solicitar reembolso)"
                holder.btnCancelar.setBackgroundColor(android.graphics.Color.parseColor("#e74c3c"))
                holder.btnCancelar.setOnClickListener { onCancelar(s) }
                holder.btnPagar.visibility = View.GONE
            }
            sesionCompletada(s) -> {
                holder.btnCancelar.visibility = View.VISIBLE
                holder.btnCancelar.text = "⭐ Calificar tarotista"
                holder.btnCancelar.setBackgroundColor(android.graphics.Color.parseColor("#f39c12"))
                holder.btnCancelar.setOnClickListener { onCalificar(s) }
                holder.btnPagar.visibility = View.GONE
            }
            else -> {
                holder.btnCancelar.visibility = View.GONE
                holder.btnPagar.visibility = View.GONE
            }
        }

        when {
            ventanaVideollamada(s) -> {
                holder.btnVideollamada.visibility = View.VISIBLE
                holder.tvVideollamadaInfo.visibility = View.GONE
                holder.btnVideollamada.setOnClickListener {
                    val ctx = holder.itemView.context
                    ctx.startActivity(Intent(ctx, VideoCallActivity::class.java).apply {
                        putExtra("sesionId", s.id)
                    })
                }
            }
            esSesionFuturaPagada(s) -> {
                holder.btnVideollamada.visibility = View.GONE
                holder.tvVideollamadaInfo.visibility = View.VISIBLE
            }
            else -> {
                holder.btnVideollamada.visibility = View.GONE
                holder.tvVideollamadaInfo.visibility = View.GONE
            }
        }
    }
}
