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
    private val onConfirmClick: (orderId: String, position: Int, adapter: MyPackageOrdersAdapter) -> Unit,
    private val onStartTripClick: (DynamicPackageOrder) -> Unit,
    private val onWhatsAppClick: (DynamicPackageOrder) -> Unit
) : RecyclerView.Adapter<MyPackagesAdapter.PackageViewHolder>() {

    private var packages: List<DynamicPackage> = emptyList()
    private val formatter = NumberFormat.getCurrencyInstance(Locale.US)

    fun submitList(list: List<DynamicPackage>) {
        packages = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PackageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_my_package, parent, false)
        return PackageViewHolder(view)
    }

    override fun onBindViewHolder(holder: PackageViewHolder, position: Int) {
        val packageItem = packages[position]
        holder.bind(packageItem)
    }

    override fun getItemCount(): Int = packages.size

    inner class PackageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvPackageSize: TextView = itemView.findViewById(R.id.tvPackageSize)
        private val tvOrdersCount: TextView = itemView.findViewById(R.id.tvOrdersCount)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        private val tvTakenAt: TextView = itemView.findViewById(R.id.tvTakenAt)
        private val rvOrdersInMyPackage: RecyclerView = itemView.findViewById(R.id.rvOrdersInMyPackage)

        fun bind(packageItem: DynamicPackage) {
            tvPackageSize.text = "Paquete #${packageItem.id.takeLast(8)}"
            tvOrdersCount.text = "${packageItem.current_size} pedidos"

            when (packageItem.status) {
                "available" -> tvStatus.text = "✅ Disponible"
                "taken" -> tvStatus.text = "🚚 En camino"
                "completed" -> tvStatus.text = "✅ Completado"
                else -> tvStatus.text = packageItem.status
            }

            if (packageItem.taken_at != null) {
                tvTakenAt.visibility = View.VISIBLE
                tvTakenAt.text = "Tomado: ${formatDate(packageItem.taken_at)}"
            } else {
                tvTakenAt.visibility = View.GONE
            }

            val orders = packageItem.orders ?: emptyList()
            val adapter = MyPackageOrdersAdapter(orders, onConfirmClick, onStartTripClick, onWhatsAppClick)
            rvOrdersInMyPackage.layoutManager = LinearLayoutManager(itemView.context)
            rvOrdersInMyPackage.adapter = adapter
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

class MyPackageOrdersAdapter(
    private val orders: List<DynamicPackageOrder>,
    private val onConfirmClick: (orderId: String, position: Int, adapter: MyPackageOrdersAdapter) -> Unit,
    private val onStartTripClick: (DynamicPackageOrder) -> Unit,
    private val onWhatsAppClick: (DynamicPackageOrder) -> Unit
) : RecyclerView.Adapter<MyPackageOrdersAdapter.OrderViewHolder>() {

    private val formatter = NumberFormat.getCurrencyInstance(Locale.US)

    fun removeOrder(position: Int) {
        (orders as MutableList).removeAt(position)
        notifyItemRemoved(position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_my_package_order, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = orders[position]
        holder.bind(order, position)
    }

    override fun getItemCount(): Int = orders.size

    inner class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCustomerName: TextView = itemView.findViewById(R.id.tvCustomerName)
        private val tvCustomerPhone: TextView = itemView.findViewById(R.id.tvCustomerPhone)
        private val tvDeliveryAddress: TextView = itemView.findViewById(R.id.tvDeliveryAddress)
        private val tvTotalAmount: TextView = itemView.findViewById(R.id.tvTotalAmount)
        private val tvPaymentMethod: TextView = itemView.findViewById(R.id.tvPaymentMethod)
        private val tvTipAmount: TextView = itemView.findViewById(R.id.tvTipAmount)
        private val tvOrderStatus: TextView = itemView.findViewById(R.id.tvOrderStatus)
        private val btnStartTrip: Button = itemView.findViewById(R.id.btnStartTrip)
        private val btnTakePhoto: Button = itemView.findViewById(R.id.btnTakePhoto)
        private val btnWhatsApp: Button = itemView.findViewById(R.id.btnWhatsApp)

        fun bind(order: DynamicPackageOrder, position: Int) {
            tvCustomerName.text = "👤 ${order.customer_name}"
            tvCustomerPhone.text = "📞 ${order.customer_phone}"
            tvDeliveryAddress.text = "📍 ${order.delivery_address ?: "Sin dirección"}"
            tvTotalAmount.text = formatter.format(order.total_amount)
            tvPaymentMethod.text = when (order.payment_method) {
                "card" -> "💳 Tarjeta"
                "yappi" -> "📱 YAPPI"
                else -> "💰 Efectivo"
            }

            if (order.tip_amount > 0) {
                tvTipAmount.visibility = View.VISIBLE
                tvTipAmount.text = "💸 Propina: ${formatter.format(order.tip_amount)}"
            } else {
                tvTipAmount.visibility = View.GONE
            }

            tvOrderStatus.text = "📦 Pendiente"

            btnStartTrip.setOnClickListener {
                onStartTripClick(order)
            }

            btnTakePhoto.setOnClickListener {
                onConfirmClick(order.order_id, position, this@MyPackageOrdersAdapter)
            }

            btnWhatsApp.setOnClickListener {
                onWhatsAppClick(order)
            }
        }
    }
}