package com.agroapp.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.agroapp.R
import com.agroapp.model.DriverBlock  // ← IMPORTA ESTO
import java.text.NumberFormat
import java.util.Locale

class MyBlocksAdapter : RecyclerView.Adapter<MyBlocksAdapter.ViewHolder>() {

    private var blocks: List<DriverBlock> = emptyList()

    fun submitList(list: List<DriverBlock>) {
        blocks = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_my_block, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(blocks[position])
    }

    override fun getItemCount() = blocks.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvZone: TextView = itemView.findViewById(R.id.tvZone)
        private val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        private val tvEarned: TextView = itemView.findViewById(R.id.tvEarned)

        fun bind(block: DriverBlock) {
            val formatter = NumberFormat.getCurrencyInstance(Locale.US)

            tvZone.text = block.block?.zone ?: "Zona no especificada"
            tvTime.text = if (block.block != null) {
                "${block.block.start_time} - ${block.block.end_time}"
            } else {
                "Horario no disponible"
            }

            // Estado en español
            val statusText = when (block.status) {
                "assigned" -> "Asignado"
                "started" -> "En progreso"
                "completed" -> "Completado"
                "cancelled" -> "Cancelado"
                else -> block.status
            }
            tvStatus.text = statusText

            tvEarned.text = formatter.format(block.total_earned)
        }
    }
}