package com.conectatarot.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.conectatarot.app.network.SesionItem

class SesionAdapter(
    private val sesiones: List<SesionItem>,
    private val onCancelar: (SesionItem) -> Unit,
    private val onCalificar: (SesionItem) -> Unit,
    private val onPagar: (SesionItem) -> Unit
) : RecyclerView.Adapter<SesionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTarotista: TextView = view.findViewById(R.id.tvSesionTarotista)
        val tvFecha: TextView = view.findViewById(R.id.tvSesionFecha)
        val tvEstado: TextView = view.findViewById(R.id.tvSesionEstado)
        val tvPrecio: TextView = view.findViewById(R.id.tvSesionPrecio)
        val btnCancelar: Button = view.findViewById(R.id.btnCancelarSesion)
        val btnPagar: Button = view.findViewById(R.id.btnPagarSesion)

        val btnVideollamada: Button = view.findViewById(R.id.btnVideollamada)

        val tvVideollamadaInfo: TextView = view.findViewById(R.id.tvVideollamadaInfo)
    }

    private fun sesionCompletada(s: SesionItem): Boolean {
        if (s.estado != "CONFIRMADA") return false
        return try {
            val fechaSesion = java.time.LocalDateTime.parse(s.fecha)
            val fin = fechaSesion.plusMinutes(s.duracionMinutos.toLong())
            fin.isBefore(java.time.LocalDateTime.now())
        } catch (e: Exception) {
            false
        }
    }

    private fun ventanaVideollamada(s: SesionItem): Boolean {
        if (s.estado != "CONFIRMADA" || s.estadoPago != "PAGADO") return false
        return try {
            val fechaSesion = java.time.LocalDateTime.parse(s.fecha)
            val inicio = fechaSesion.minusMinutes(15)
            val fin = fechaSesion.plusMinutes(s.duracionMinutos.toLong())
            val ahora = java.time.LocalDateTime.now()
            ahora.isAfter(inicio) && ahora.isBefore(fin)
        } catch (e: Exception) { false }
    }

    private fun esSesionFutura(s: SesionItem): Boolean {
        if (s.estado != "CONFIRMADA") return false
        return try {
            val fechaSesion = java.time.LocalDateTime.parse(s.fecha)
            fechaSesion.isAfter(java.time.LocalDateTime.now())
        } catch (e: Exception) { false }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sesion, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val s = sesiones[position]
        holder.tvTarotista.text = "🌙 ${s.nombreTarotista}"
        holder.tvFecha.text = "📅 ${s.fecha.take(16).replace("T", " ")}"
        holder.tvPrecio.text = "$ ${s.precioTotal.toInt()}"

        val (color, texto) = when {
            sesionCompletada(s) -> Pair("#3498db", "✔ Completada")
            s.estado == "PENDIENTE" && s.estadoPago == "PAGADO" -> Pair("#27ae60", "✅ Pagado")
            s.estado == "PENDIENTE" -> Pair("#f39c12", "⏳ Pendiente")
            s.estado == "CONFIRMADA" -> Pair("#27ae60", "✅ Confirmada")
            s.estado == "CANCELADA" -> Pair("#e74c3c", "❌ Cancelada")
            else -> Pair("#9b59b6", s.estado)
        }
        holder.tvEstado.text = texto
        holder.tvEstado.setTextColor(android.graphics.Color.parseColor(color))

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
                holder.btnCancelar.text = "Cancelar (reembolso)"
                holder.btnCancelar.isEnabled = true
                holder.btnCancelar.setBackgroundColor(android.graphics.Color.parseColor("#e74c3c"))
                holder.btnCancelar.setOnClickListener { onCancelar(s) }
                holder.btnPagar.visibility = View.GONE
            }

            sesionCompletada(s) -> {
                holder.btnCancelar.visibility = View.VISIBLE
                holder.btnCancelar.text = "⭐ Calificar"
                holder.btnCancelar.isEnabled = true
                holder.btnCancelar.setBackgroundColor(android.graphics.Color.parseColor("#f39c12"))
                holder.btnCancelar.setOnClickListener { onCalificar(s) }
                holder.btnPagar.visibility = View.GONE
            }

            else -> {
                holder.btnCancelar.visibility = View.GONE
                holder.btnPagar.visibility = View.GONE
                holder.btnCancelar.isEnabled = true
            }
        }
        when {
            ventanaVideollamada(s) -> {
                holder.btnVideollamada.visibility = View.VISIBLE
                holder.tvVideollamadaInfo.visibility = View.GONE
                holder.btnVideollamada.setOnClickListener {
                    val url = "https://meet.jit.si/ConectaTarot-Sesion${s.id}"
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                    holder.itemView.context.startActivity(intent)
                }
            }
            esSesionFutura(s) && s.estadoPago == "PAGADO" -> {
                holder.btnVideollamada.visibility = View.GONE
                holder.tvVideollamadaInfo.visibility = View.VISIBLE
            }
            else -> {
                holder.btnVideollamada.visibility = View.GONE
                holder.tvVideollamadaInfo.visibility = View.GONE
            }
        }
    }
        override fun getItemCount() = sesiones.size
}