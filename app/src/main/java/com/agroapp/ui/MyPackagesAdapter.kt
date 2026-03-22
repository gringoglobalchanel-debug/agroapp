package com.agroapp.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.agroapp.R
import com.agroapp.model.DynamicPackage
import com.agroapp.model.DynamicPackageOrder
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
        private val rvOrders: RecyclerView = itemView.findViewById(R.id.rvOrdersInMyPackage)

        fun bind(packageItem: DynamicPackage) {
            tvSize.text = "Paquete #${packageItem.id.takeLast(8)}"
            tvOrders.text = "${packageItem.current_size} pedidos"

            val statusText = when (packageItem.status) {
                "taken" -> "✅ En reparto"
                "available" -> "Disponible"
                "forming" -> "Formándose"
                else -> packageItem.status
            }
            tvStatus.text = statusText

            tvTakenAt.text = packageItem.taken_at?.let {
                "Tomado: ${formatDate(it)}"
            } ?: "Pendiente"

            // Mostrar pedidos del cliente
            val ordersAdapter = MyPackageOrdersAdapter(packageItem.orders ?: emptyList())
            rvOrders.layoutManager = LinearLayoutManager(itemView.context)
            rvOrders.adapter = ordersAdapter
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

// Adapter para los pedidos dentro de mis paquetes tomados
class MyPackageOrdersAdapter(
    private val orders: List<DynamicPackageOrder>
) : RecyclerView.Adapter<MyPackageOrdersAdapter.OrderViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_my_package_order, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(orders[position])
    }

    override fun getItemCount() = orders.size

    inner class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCustomerName: TextView = itemView.findViewById(R.id.tvCustomerName)
        private val tvCustomerPhone: TextView = itemView.findViewById(R.id.tvCustomerPhone)
        private val tvDeliveryAddress: TextView = itemView.findViewById(R.id.tvDeliveryAddress)
        private val tvTotalAmount: TextView = itemView.findViewById(R.id.tvTotalAmount)
        private val tvPaymentMethod: TextView = itemView.findViewById(R.id.tvPaymentMethod)

        fun bind(order: DynamicPackageOrder) {
            val formatter = NumberFormat.getCurrencyInstance(Locale.US)
            tvCustomerName.text = "👤 ${order.customer_name ?: "Cliente"}"
            tvCustomerPhone.text = "📞 ${order.customer_phone ?: "No disponible"}"
            tvDeliveryAddress.text = "📍 ${order.delivery_address ?: "Sin dirección"}"
            tvTotalAmount.text = "💰 ${formatter.format(order.total_amount)}"
            tvPaymentMethod.text = when (order.payment_method) {
                "card" -> "💳 Tarjeta"
                "cash" -> "💵 Efectivo"
                "yappi" -> "📱 Yappi"
                else -> order.payment_method ?: ""
            }
        }
    }
}