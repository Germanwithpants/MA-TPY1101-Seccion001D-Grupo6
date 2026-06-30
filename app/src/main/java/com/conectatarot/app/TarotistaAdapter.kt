package com.conectatarot.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.conectatarot.app.network.Tarotista

class TarotistaAdapter(private val tarotistas: List<Tarotista>) :
    RecyclerView.Adapter<TarotistaAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombre: TextView = view.findViewById(R.id.tvNombre)
        val tvDescripcion: TextView = view.findViewById(R.id.tvDescripcion)
        val tvPrecio: TextView = view.findViewById(R.id.tvPrecio)
        val tvEspecialidades: TextView = view.findViewById(R.id.tvEspecialidades)
        val tvPromedio: TextView = view.findViewById(R.id.tvPromedio)
        val btnVerPerfil: Button = view.findViewById(R.id.btnVerPerfil)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tarotista, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val t = tarotistas[position]
        holder.tvNombre.text = if (t.verificado == true) "✅ ${t.nombreProfesional}" else t.nombreProfesional
        holder.tvDescripcion.text = t.descripcion ?: "Sin descripción"
        holder.tvPrecio.text = "$ ${t.precioBase?.toInt() ?: 0}"
        holder.tvEspecialidades.text = t.especialidades?.joinToString(" • ") ?: ""

        if (t.promedio != null && (t.totalResenas ?: 0) > 0) {
            val estrellas = "★".repeat(t.promedio.toInt()) + "☆".repeat(5 - t.promedio.toInt())
            holder.tvPromedio.text = "$estrellas ${t.promedio} (${t.totalResenas})"
            holder.tvPromedio.visibility = View.VISIBLE
        } else {
            holder.tvPromedio.visibility = View.GONE
        }

        holder.btnVerPerfil.setOnClickListener {
            val context = holder.itemView.context
            val intent = android.content.Intent(context, TarotistaDetalleActivity::class.java).apply {
                putExtra("nombre", t.nombreProfesional)
                putExtra("descripcion", t.descripcion ?: "")
                putExtra("precio", t.precioBase ?: 0.0)
                putExtra("especialidades", t.especialidades?.joinToString(", ") ?: "")
                putExtra("tarotistaId", t.id)
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount() = tarotistas.size
}
