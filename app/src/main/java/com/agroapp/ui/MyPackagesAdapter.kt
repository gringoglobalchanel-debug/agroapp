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
import com.agroapp.model.DynamicPackageOrder
import java.text.NumberFormat
import java.util.Locale

class MyPackagesAdapter(
    private val onTakePhotoClick: (String, Int, MyPackageOrdersAdapter) -> Unit
) : RecyclerView.Adapter<MyPackagesAdapter.ViewHolder>() {

    private var packages: List<DynamicPackage> = emptyList()

    fun submitList(list: List<DynamicPackage>) {
        // Filtrar paquetes que tienen al menos un pedido (no vacíos)
        val nonEmptyPackages = list.filter { it.orders?.isNotEmpty() == true }
        packages = nonEmptyPackages
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

            // Crear adaptador de pedidos con callback para la foto
            lateinit var ordersAdapter: MyPackageOrdersAdapter

            ordersAdapter = MyPackageOrdersAdapter(
                orders = packageItem.orders ?: emptyList(),
                onTakePhotoClick = { orderId, position ->
                    onTakePhotoClick(orderId, position, ordersAdapter)
                }
            )
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
    private val orders: List<DynamicPackageOrder>,
    private val onTakePhotoClick: (String, Int) -> Unit
) : RecyclerView.Adapter<MyPackageOrdersAdapter.OrderViewHolder>() {

    private var ordersList: MutableList<DynamicPackageOrder> = orders.toMutableList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_my_package_order, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(ordersList[position], position)
    }

    override fun getItemCount() = ordersList.size

    fun removeOrder(position: Int) {
        if (position >= 0 && position < ordersList.size) {
            ordersList.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, ordersList.size)
        }
    }

    inner class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCustomerName: TextView = itemView.findViewById(R.id.tvCustomerName)
        private val tvCustomerPhone: TextView = itemView.findViewById(R.id.tvCustomerPhone)
        private val tvDeliveryAddress: TextView = itemView.findViewById(R.id.tvDeliveryAddress)
        private val tvTotalAmount: TextView = itemView.findViewById(R.id.tvTotalAmount)
        private val tvPaymentMethod: TextView = itemView.findViewById(R.id.tvPaymentMethod)
        private val tvOrderStatus: TextView = itemView.findViewById(R.id.tvOrderStatus)
        private val btnTakePhoto: Button = itemView.findViewById(R.id.btnTakePhoto)

        fun bind(order: DynamicPackageOrder, position: Int) {
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

            // Estado del pedido
            tvOrderStatus.text = "🚚 En camino"
            tvOrderStatus.setBackgroundColor(itemView.context.getColor(android.R.color.holo_orange_light))

            // Botón de confirmar entrega
            btnTakePhoto.setOnClickListener {
                onTakePhotoClick(order.order_id, position)
            }
        }
    }
}