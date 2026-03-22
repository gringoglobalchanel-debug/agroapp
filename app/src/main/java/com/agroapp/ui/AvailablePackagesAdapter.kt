package com.agroapp.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.agroapp.R
import com.agroapp.model.DynamicPackage
import java.text.NumberFormat
import java.util.Locale

class AvailablePackagesAdapter(
    private val onTakeClick: (DynamicPackage) -> Unit,
    private val onOrderStatusChange: (String, String, String?) -> Unit // (orderId, newStatus, photoUri)
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
        private val rvOrdersInPackage: RecyclerView = itemView.findViewById(R.id.rvOrdersInPackage)

        fun bind(packageItem: DynamicPackage) {
            val formatter = NumberFormat.getCurrencyInstance(Locale.US)
            val totalPayment = packageItem.current_size * 1.25
            val driverPayment = totalPayment * 0.90

            tvSize.text = "Paquete #${packageItem.id.takeLast(8)}"
            tvOrders.text = "${packageItem.current_size} pedidos"
            tvPayment.text = formatter.format(driverPayment)
            tvCreatedAt.text = "Creado: ${formatDate(packageItem.created_at)}"

            // Configurar el RecyclerView de pedidos dentro del paquete
            val ordersAdapter = PackageOrderAdapter(
                orders = packageItem.orders ?: emptyList(),
                onStatusChange = { orderId, newStatus, photoUri ->
                    onOrderStatusChange(orderId, newStatus, photoUri)
                }
            )
            rvOrdersInPackage.layoutManager = LinearLayoutManager(itemView.context)
            rvOrdersInPackage.adapter = ordersAdapter

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

// Adaptador para mostrar cada pedido dentro del paquete
class PackageOrderAdapter(
    private val orders: List<com.agroapp.model.DynamicPackageOrder>,
    private val onStatusChange: (String, String, String?) -> Unit
) : RecyclerView.Adapter<PackageOrderAdapter.OrderViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_package_order, parent, false)
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
        private val tvOrderStatus: TextView = itemView.findViewById(R.id.tvOrderStatus)
        private val btnMarkPending: Button = itemView.findViewById(R.id.btnMarkPending)
        private val btnMarkDelivered: Button = itemView.findViewById(R.id.btnMarkDelivered)
        private val btnTakePhoto: Button = itemView.findViewById(R.id.btnTakePhoto)

        fun bind(order: com.agroapp.model.DynamicPackageOrder) {
            val formatter = NumberFormat.getCurrencyInstance(Locale.US)

            tvCustomerName.text = order.customer_name
            tvCustomerPhone.text = "📞 ${order.customer_phone}"
            tvDeliveryAddress.text = "📍 ${order.delivery_address ?: "Dirección no especificada"}"
            tvTotalAmount.text = formatter.format(order.total_amount)

            // Estado actual del pedido (por defecto "pending")
            updateStatusUI("pending")

            // Botón Pendiente
            btnMarkPending.setOnClickListener {
                updateStatusUI("pending")
                onStatusChange(order.order_id, "pending", null)
            }

            // Botón Entregado
            btnMarkDelivered.setOnClickListener {
                updateStatusUI("delivered")
                onStatusChange(order.order_id, "delivered", null)
            }

            // Botón Foto
            btnTakePhoto.setOnClickListener {
                // Aquí se abrirá la cámara
                onStatusChange(order.order_id, "photo", null)
            }
        }

        private fun updateStatusUI(status: String) {
            when (status) {
                "pending" -> {
                    tvOrderStatus.text = "Pendiente"
                    tvOrderStatus.setTextColor(itemView.context.getColor(android.R.color.holo_orange_dark))
                    tvOrderStatus.setBackgroundColor(itemView.context.getColor(android.R.color.holo_orange_light))
                }
                "delivered" -> {
                    tvOrderStatus.text = "Entregado ✓"
                    tvOrderStatus.setTextColor(itemView.context.getColor(android.R.color.holo_green_dark))
                    tvOrderStatus.setBackgroundColor(itemView.context.getColor(android.R.color.holo_green_light))
                }
            }
        }
    }
}