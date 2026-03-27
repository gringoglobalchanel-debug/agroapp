package com.agroapp.ui

import android.content.Intent
import android.net.Uri
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

class AvailablePackagesAdapter(
    private val onTakeClick: (DynamicPackage) -> Unit,
    private val onOrderStatusChange: (String, String, String?) -> Unit,
    private val onTakePhotoClick: (String, Int, PackageOrderAdapter) -> Unit
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
            val pricePerOrder = 2.50
            val totalPayment = packageItem.current_size * pricePerOrder
            val driverPayment = totalPayment * 0.90

            tvSize.text = "Paquete #${packageItem.id.takeLast(8)}"
            tvOrders.text = "${packageItem.current_size}/${packageItem.max_size}"
            tvPayment.text = "${formatter.format(driverPayment)} (${packageItem.current_size} × ${formatter.format(pricePerOrder * 0.90)})"
            tvCreatedAt.text = "Creado: ${formatDate(packageItem.created_at)}"

            val isAvailable = packageItem.status == "available"

            lateinit var ordersAdapter: PackageOrderAdapter

            ordersAdapter = PackageOrderAdapter(
                orders = packageItem.orders ?: emptyList(),
                onStatusChange = { orderId, newStatus, photoUri ->
                    onOrderStatusChange(orderId, newStatus, photoUri)
                },
                onTakePhotoClick = { orderId, position ->
                    onTakePhotoClick(orderId, position, ordersAdapter)
                },
                isAvailable = isAvailable
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

class PackageOrderAdapter(
    orders: List<DynamicPackageOrder>,
    private val onStatusChange: (String, String, String?) -> Unit,
    private val onTakePhotoClick: (String, Int) -> Unit,
    private val isAvailable: Boolean = false
) : RecyclerView.Adapter<PackageOrderAdapter.OrderViewHolder>() {

    private var orders: MutableList<DynamicPackageOrder> = orders.toMutableList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_package_order, parent, false)
        return OrderViewHolder(view, isAvailable)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(orders[position], position)
    }

    override fun getItemCount() = orders.size

    fun removeOrder(position: Int) {
        if (position >= 0 && position < orders.size) {
            orders.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, orders.size)
        }
    }

    inner class OrderViewHolder(
        itemView: View,
        private val isAvailable: Boolean
    ) : RecyclerView.ViewHolder(itemView) {
        private val tvCustomerName: TextView = itemView.findViewById(R.id.tvCustomerName)
        private val tvCustomerPhone: TextView = itemView.findViewById(R.id.tvCustomerPhone)
        private val tvDeliveryAddress: TextView = itemView.findViewById(R.id.tvDeliveryAddress)
        private val tvTotalAmount: TextView = itemView.findViewById(R.id.tvTotalAmount)
        private val tvTipAmount: TextView = itemView.findViewById(R.id.tvTipAmount)
        private val tvOrderStatus: TextView = itemView.findViewById(R.id.tvOrderStatus)
        private val btnMarkPending: Button = itemView.findViewById(R.id.btnMarkPending)
        private val btnMarkDelivered: Button = itemView.findViewById(R.id.btnMarkDelivered)
        private val btnTakePhoto: Button = itemView.findViewById(R.id.btnTakePhoto)
        private val btnWhatsApp: Button = itemView.findViewById(R.id.btnWhatsApp)

        fun bind(order: DynamicPackageOrder, position: Int) {
            val formatter = NumberFormat.getCurrencyInstance(Locale.US)
            tvCustomerName.text = "👤 ${order.customer_name.ifEmpty { "Cliente" }}"
            tvCustomerPhone.text = "📞 ${order.customer_phone.ifEmpty { "No disponible" }}"
            tvDeliveryAddress.text = "📍 ${order.delivery_address ?: "Sin dirección"}"
            tvTotalAmount.text = "💰 Total: ${formatter.format(order.total_amount)}"

            if (order.tip_amount > 0) {
                tvTipAmount.visibility = View.VISIBLE
                tvTipAmount.text = "💸 Propina: ${formatter.format(order.tip_amount)}"
            } else {
                tvTipAmount.visibility = View.GONE
            }

            // Botón WhatsApp
            if (order.customer_phone.isNotEmpty() && order.customer_phone != "No disponible") {
                btnWhatsApp.visibility = View.VISIBLE
                btnWhatsApp.setOnClickListener {
                    val phone = order.customer_phone.replace(Regex("[^0-9]"), "")
                    val url = "https://wa.me/$phone?text=Hola%20${order.customer_name}%2C%20soy%20tu%20repartidor%20de%20AgroApp.%20Estoy%20en%20camino%20con%20tu%20pedido."
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    itemView.context.startActivity(intent)
                }
            } else {
                btnWhatsApp.visibility = View.GONE
            }

            if (isAvailable) {
                tvOrderStatus.visibility = View.GONE
                btnMarkPending.visibility = View.GONE
                btnMarkDelivered.visibility = View.GONE
                btnTakePhoto.visibility = View.GONE
                btnWhatsApp.visibility = View.GONE
            } else {
                tvOrderStatus.visibility = View.VISIBLE
                updateStatusUI("pending")

                btnMarkPending.visibility = View.VISIBLE
                btnMarkDelivered.visibility = View.VISIBLE
                btnTakePhoto.visibility = View.VISIBLE

                btnMarkPending.setOnClickListener {
                    onStatusChange(order.order_id, "pending", null)
                }

                btnMarkDelivered.setOnClickListener {
                    onStatusChange(order.order_id, "delivered", null)
                }

                btnTakePhoto.setOnClickListener {
                    onTakePhotoClick(order.order_id, position)
                }
            }
        }

        private fun updateStatusUI(status: String) {
            when (status) {
                "pending" -> {
                    tvOrderStatus.text = "🚚 En camino"
                    tvOrderStatus.setTextColor(itemView.context.getColor(android.R.color.black))
                    tvOrderStatus.setBackgroundColor(itemView.context.getColor(android.R.color.holo_orange_light))
                }
            }
        }
    }
}