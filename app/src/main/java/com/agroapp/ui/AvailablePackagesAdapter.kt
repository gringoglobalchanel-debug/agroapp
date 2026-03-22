package com.agroapp.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.agroapp.R
import com.agroapp.model.DynamicPackage
import com.agroapp.model.DynamicPackageOrder
import java.io.File
import java.text.NumberFormat
import java.util.Locale

class AvailablePackagesAdapter(
    private val onTakeClick: (DynamicPackage) -> Unit,
    private val onOrderStatusChange: (String, String, String?) -> Unit
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

class PackageOrderAdapter(
    orders: List<DynamicPackageOrder>,
    private val onStatusChange: (String, String, String?) -> Unit
) : RecyclerView.Adapter<PackageOrderAdapter.OrderViewHolder>() {

    private var orders: MutableList<DynamicPackageOrder> = orders.toMutableList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_package_order, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        holder.bind(orders[position], position)
    }

    override fun getItemCount() = orders.size

    // Eliminar pedido de la lista al entregar
    fun removeOrder(position: Int) {
        if (position >= 0 && position < orders.size) {
            orders.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, orders.size)
        }
    }

    inner class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCustomerName: TextView = itemView.findViewById(R.id.tvCustomerName)
        private val tvCustomerPhone: TextView = itemView.findViewById(R.id.tvCustomerPhone)
        private val tvDeliveryAddress: TextView = itemView.findViewById(R.id.tvDeliveryAddress)
        private val tvTotalAmount: TextView = itemView.findViewById(R.id.tvTotalAmount)
        private val tvOrderStatus: TextView = itemView.findViewById(R.id.tvOrderStatus)
        private val btnMarkPending: Button = itemView.findViewById(R.id.btnMarkPending)
        private val btnMarkDelivered: Button = itemView.findViewById(R.id.btnMarkDelivered)
        private val btnTakePhoto: Button = itemView.findViewById(R.id.btnTakePhoto)

        fun bind(order: DynamicPackageOrder, position: Int) {
            val formatter = NumberFormat.getCurrencyInstance(Locale.US)
            tvCustomerName.text = "👤 ${order.customer_name.ifEmpty { "Cliente" }}"
            tvCustomerPhone.text = "📞 ${order.customer_phone.ifEmpty { "No disponible" }}"
            tvDeliveryAddress.text = "📍 ${order.delivery_address ?: "Sin dirección"}"
            tvTotalAmount.text = formatter.format(order.total_amount)
            updateStatusUI("pending")

            btnMarkPending.setOnClickListener {
                updateStatusUI("pending")
                onStatusChange(order.order_id, "pending", null)
            }

            btnMarkDelivered.setOnClickListener {
                updateStatusUI("delivered")
                onStatusChange(order.order_id, "delivered", null)
                // Eliminar de la lista al marcar entregado
                removeOrder(position)
            }

            // Botón foto: abre cámara y al tomar foto marca como entregado
            btnTakePhoto.setOnClickListener {
                val context = itemView.context
                val photoFile = File(context.cacheDir, "delivery_${order.order_id}.jpg")
                val photoUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    photoFile
                )
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                    putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                }
                // Notificar con estado "photo" — DriverActivity maneja el resultado
                onStatusChange(order.order_id, "photo", photoUri.toString())

                if (context is Activity) {
                    context.startActivityForResult(intent, REQUEST_PHOTO)
                }
                // Al volver de la cámara, marcar entregado y eliminar
                updateStatusUI("delivered")
                removeOrder(position)
            }
        }

        private fun updateStatusUI(status: String) {
            when (status) {
                "pending" -> {
                    tvOrderStatus.text = "⏳ Pendiente"
                    tvOrderStatus.setTextColor(itemView.context.getColor(android.R.color.holo_orange_dark))
                }
                "delivered" -> {
                    tvOrderStatus.text = "✅ Entregado"
                    tvOrderStatus.setTextColor(itemView.context.getColor(android.R.color.holo_green_dark))
                }
            }
        }
    }

    companion object {
        const val REQUEST_PHOTO = 1001
    }
}