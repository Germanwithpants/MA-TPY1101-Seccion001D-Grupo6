package com.conectatarot.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.conectatarot.app.network.AdminUsuario

class AdminUsuariosAdapter(
    private var items: List<AdminUsuario>,
    private val onToggle: (AdminUsuario) -> Unit
) : RecyclerView.Adapter<AdminUsuariosAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombre: TextView = view.findViewById(R.id.tvUsuarioNombre)
        val tvEmail: TextView = view.findViewById(R.id.tvUsuarioEmail)
        val tvRol: TextView = view.findViewById(R.id.tvUsuarioRol)
        val tvEstado: TextView = view.findViewById(R.id.tvUsuarioEstado)
        val btnToggle: Button = view.findViewById(R.id.btnToggleBloqueo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_admin_usuario, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(h: VH, pos: Int) {
        val u = items[pos]
        h.tvNombre.text = u.nombre
        h.tvEmail.text = u.email
        h.tvRol.text = "Rol: ${u.rol ?: "—"}"

        val activo = u.activo != false
        h.tvEstado.text = if (activo) "✅ Activo" else "🚫 Bloqueado"
        h.tvEstado.setTextColor(if (activo) 0xFF60e090.toInt() else 0xFFe74c3c.toInt())
        h.btnToggle.text = if (activo) "🚫 Bloquear" else "✅ Desbloquear"
        h.btnToggle.backgroundTintList = android.content.res.ColorStateList.valueOf(
            if (activo) 0xFFe74c3c.toInt() else 0xFF27ae60.toInt()
        )
        h.btnToggle.setOnClickListener { onToggle(u) }
    }

    fun update(newItems: List<AdminUsuario>) {
        items = newItems
        notifyDataSetChanged()
    }
}
