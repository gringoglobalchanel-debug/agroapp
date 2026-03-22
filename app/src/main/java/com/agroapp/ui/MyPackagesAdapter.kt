package com.agroapp.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.agroapp.R
import com.agroapp.model.DynamicPackage
import java.text.NumberFormat
import java.util.Locale

class MyPackagesAdapter : RecyclerView.Adapter<MyPackagesAdapter.ViewHolder>() {

    private var packages: List<DynamicPackage> = emptyList()

    fun submitList(list: List<DynamicPackage>) {
        packages = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_my_package, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(packages[position])
    }

    override fun getItemCount() = packages.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvSize: TextView = itemView.findViewById(R.id.tvPackageSize)
        private val tvOrders: TextView = itemView.findViewById(R.id.tvOrdersCount)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        private val tvTakenAt: TextView = itemView.findViewById(R.id.tvTakenAt)

        fun bind(packageItem: DynamicPackage) {
            tvSize.text = "Paquete #${packageItem.id.takeLast(8)}"
            tvOrders.text = "${packageItem.current_size} pedidos"

            val statusText = when (packageItem.status) {
                "taken" -> "Tomado"
                "available" -> "Disponible"
                "forming" -> "Formándose"
                else -> packageItem.status
            }
            tvStatus.text = statusText

            tvTakenAt.text = packageItem.taken_at?.let {
                "Tomado: ${formatDate(it)}"
            } ?: "Pendiente"
        }

        private fun formatDate(dateString: String): String {
            return try {
                val date = java.time.format.DateTimeFormatter.ISO_DATE_TIME.parse(dateString)
                val localDate = java.time.LocalDateTime.from(date)
                localDate.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM HH:mm"))
            } catch (e: Exception) {
                dateString
            }
        }
    }
}