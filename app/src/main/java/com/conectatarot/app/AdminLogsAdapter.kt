package com.conectatarot.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.conectatarot.app.network.AdminLogItem

class AdminLogsAdapter(
    private var items: List<AdminLogItem>
) : RecyclerView.Adapter<AdminLogsAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvAccion: TextView = view.findViewById(R.id.tvLogAccion)
        val tvTimestamp: TextView = view.findViewById(R.id.tvLogTimestamp)
        val tvDetalle: TextView = view.findViewById(R.id.tvLogDetalle)
        val tvAdmin: TextView = view.findViewById(R.id.tvLogAdmin)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_admin_log, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val log = items[pos]
        h.tvAccion.text = log.accion
        h.tvTimestamp.text = log.timestamp.take(16).replace("T", " ")
        h.tvDetalle.text = log.detalle ?: "${log.entidad ?: ""} #${log.entidadId ?: ""}"
        h.tvAdmin.text = "Por: ${log.adminEmail ?: "system"}"
    }

    fun update(newItems: List<AdminLogItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
