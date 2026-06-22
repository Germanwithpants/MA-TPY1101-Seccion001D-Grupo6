package com.conectatarot.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.conectatarot.app.network.AdminPagoItem

class AdminPagosAdapter(
    private var items: List<AdminPagoItem>
) : RecyclerView.Adapter<AdminPagosAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvMonto: TextView = view.findViewById(R.id.tvPagoMonto)
        val tvEstado: TextView = view.findViewById(R.id.tvPagoEstado)
        val tvTarotista: TextView = view.findViewById(R.id.tvPagoTarotista)
        val tvCliente: TextView = view.findViewById(R.id.tvPagoCliente)
        val tvFecha: TextView = view.findViewById(R.id.tvPagoFecha)
        val tvComision: TextView = view.findViewById(R.id.tvPagoComision)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_admin_pago, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val p = items[pos]
        h.tvMonto.text = "$${"%.0f".format(p.monto)}"
        h.tvTarotista.text = "🔮 ${p.tarotista}"
        h.tvCliente.text = "👤 ${p.cliente}"
        h.tvFecha.text = p.fecha.take(16).replace("T", " ")

        val comision = p.monto * 0.10
        h.tvComision.text = "Comisión: $${"%.0f".format(comision)}"

        when (p.estadoPago.uppercase()) {
            "PAGADO" -> {
                h.tvEstado.text = "PAGADO"
                h.tvEstado.setTextColor(0xFF60e090.toInt())
                h.tvEstado.setBackgroundColor(0x2260e090.toInt())
            }
            "RECHAZADO" -> {
                h.tvEstado.text = "RECHAZADO"
                h.tvEstado.setTextColor(0xFFe74c3c.toInt())
                h.tvEstado.setBackgroundColor(0x22e74c3c.toInt())
            }
            else -> {
                h.tvEstado.text = p.estadoPago
                h.tvEstado.setTextColor(0xFF9b59b6.toInt())
                h.tvEstado.setBackgroundColor(0x009b59b6.toInt())
            }
        }
    }

    fun update(newItems: List<AdminPagoItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
