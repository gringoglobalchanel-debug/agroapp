package com.agroapp.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.agroapp.R
import com.agroapp.model.DynamicPackage
import java.text.NumberFormat
import java.util.Locale

class AvailablePackagesAdapter(
    private val onTakeClick: (DynamicPackage) -> Unit
) : RecyclerView.Adapter<AvailablePackagesAdapter.ViewHolder>() {

    private var packages: List<DynamicPackage> = emptyList()

    fun submitList(list: List<DynamicPackage>) {
        packages = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_available_package, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(packages[position])
    }

    override fun getItemCount() = packages.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvSize: TextView = itemView.findViewById(R.id.tvPackageSize)
        private val tvOrders: TextView = itemView.findViewById(R.id.tvOrdersCount)
        private val tvPayment: TextView = itemView.findViewById(R.id.tvPayment)
        private val tvCreatedAt: TextView = itemView.findViewById(R.id.tvCreatedAt)
        private val btnTake: Button = itemView.findViewById(R.id.btnTakePackage)

        fun bind(packageItem: DynamicPackage) {
            val formatter = NumberFormat.getCurrencyInstance(Locale.US)
            val totalPayment = packageItem.current_size * 1.25
            val driverPayment = totalPayment * 0.90

            tvSize.text = "Paquete #${packageItem.id.takeLast(8)}"
            tvOrders.text = "${packageItem.current_size}/${packageItem.max_size} pedidos"
            tvPayment.text = formatter.format(driverPayment)
            tvCreatedAt.text = "Creado: ${formatDate(packageItem.created_at)}"

            btnTake.setOnClickListener {
                onTakeClick(packageItem)
            }
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