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
import com.agroapp.network.RetrofitClient
import com.agroapp.network.SessionManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.imageview.ShapeableImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

class MyPackagesAdapter(
    private val onConfirmClick: (orderId: String, position: Int, adapter: MyPackageOrdersAdapter) -> Unit,
    private val onStartTripClick: (DynamicPackageOrder) -> Unit,
    private val onWhatsAppClick: (DynamicPackageOrder) -> Unit,
    private val onCancelClick: (DynamicPackageOrder) -> Unit // ✅ NUEVO
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
        holder.bind(packages[position])
    }

    override fun getItemCount(): Int = packages.size

    inner class PackageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvPackageSize: TextView = itemView.findViewById(R.id.tvPackageSize)
        private val tvOrdersCount: TextView = itemView.findViewById(R.id.tvOrdersCount)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        private val tvTakenAt: TextView = itemView.findViewById(R.id.tvTakenAt)
        private val rvOrdersInMyPackage: RecyclerView = itemView.findViewById(R.id.rvOrdersInMyPackage)

        fun bind(packageItem: DynamicPackage) {
            tvPackageSize.text = "Paquete #${packageItem.id.takeLast(8).uppercase()}"
            tvOrdersCount.text = "${packageItem.current_size} pedidos"

            when (packageItem.status) {
                "available" -> tvStatus.text = "✅ Disponible"
                "taken"     -> tvStatus.text = "🚚 En camino"
                "completed" -> tvStatus.text = "✅ Completado"
                else        -> tvStatus.text = packageItem.status
            }

            if (packageItem.taken_at != null) {
                tvTakenAt.visibility = View.VISIBLE
                tvTakenAt.text = "Tomado: ${formatDate(packageItem.taken_at)}"
            } else {
                tvTakenAt.visibility = View.GONE
            }

            val numOrders = packageItem.current_size
            val driverBase = numOrders * 2.50 * 0.90
            val totalTips = packageItem.orders?.sumOf { it.tip_amount } ?: 0.0
            val totalGanancia = driverBase + totalTips

            try {
                val tvPayment = itemView.findViewById<TextView>(R.id.tvPayment)
                val zone = when (packageItem.zone) {
                    "norte"  -> "🗺️ Zona Norte"
                    "sur"    -> "🗺️ Zona Sur"
                    "centro" -> "🗺️ Zona Centro"
                    else     -> if (!packageItem.zone.isNullOrEmpty()) "🗺️ ${packageItem.zone}" else ""
                }
                val date = packageItem.delivery_date ?: ""
                val start = packageItem.delivery_window_start ?: ""
                val end = packageItem.delivery_window_end ?: ""

                val paymentText = buildString {
                    if (zone.isNotEmpty()) append("$zone\n")
                    if (date.isNotEmpty()) append("⏰ Entrega: $date  $start – $end\n")
                    append("💰 Envío:      ${formatter.format(driverBase)}\n")
                    if (totalTips > 0) append("💸 Propinas: ${formatter.format(totalTips)}\n")
                    append("───────────────────────\n")
                    append("📊 Ganancia: ${formatter.format(totalGanancia)}")
                }
                tvPayment.text = paymentText
                tvPayment.visibility = View.VISIBLE
            } catch (e: Exception) { }

            val orders = packageItem.orders ?: emptyList()
            val adapter = MyPackageOrdersAdapter(
                orders,
                onConfirmClick,
                onStartTripClick,
                onWhatsAppClick,
                onCancelClick // ✅ NUEVO
            )
            rvOrdersInMyPackage.layoutManager = LinearLayoutManager(itemView.context)
            rvOrdersInMyPackage.adapter = adapter
        }

        private fun formatDate(dateString: String): String {
            return try {
                val date = java.time.format.DateTimeFormatter.ISO_DATE_TIME.parse(dateString)
                val localDate = java.time.LocalDateTime.from(date)
                localDate.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM HH:mm"))
            } catch (e: Exception) { dateString }
        }
    }
}

class MyPackageOrdersAdapter(
    private val orders: List<DynamicPackageOrder>,
    private val onConfirmClick: (orderId: String, position: Int, adapter: MyPackageOrdersAdapter) -> Unit,
    private val onStartTripClick: (DynamicPackageOrder) -> Unit,
    private val onWhatsAppClick: (DynamicPackageOrder) -> Unit,
    private val onCancelClick: (DynamicPackageOrder) -> Unit // ✅ NUEVO
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
        holder.bind(orders[position], position)
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
        private val btnCancel: Button = itemView.findViewById(R.id.btnCancelOrder) // ✅ NUEVO
        private val ivCustomerAvatar: ShapeableImageView = itemView.findViewById(R.id.ivCustomerAvatar)

        fun bind(order: DynamicPackageOrder, position: Int) {
            tvCustomerName.text = "👤 ${order.customer_name.ifEmpty { "Cliente" }}"
            tvCustomerPhone.text = "📞 ${order.customer_phone.ifEmpty { "No disponible" }}"
            tvDeliveryAddress.text = "📍 ${order.delivery_address ?: "Sin dirección"}"

            val tip = order.tip_amount
            val windowDate = order.delivery_window_date ?: ""
            val windowStart = order.delivery_window_start ?: ""
            val windowEnd = order.delivery_window_end ?: ""
            val windowLine = if (windowDate.isNotEmpty()) "\n⏰ $windowDate  $windowStart – $windowEnd" else ""
            tvTotalAmount.text = "💰 ${formatter.format(order.total_amount)}$windowLine"

            if (tip > 0) {
                tvTipAmount.visibility = View.VISIBLE
                tvTipAmount.text = "💸 Propina: ${formatter.format(tip)}"
            } else {
                tvTipAmount.visibility = View.GONE
            }

            tvPaymentMethod.text = when (order.payment_method) {
                "card"  -> "💳 Tarjeta"
                "yappi" -> "📱 YAPPI"
                else    -> "💰 Efectivo"
            }

            tvOrderStatus.text = "📦 Pendiente"

            btnStartTrip.setOnClickListener { onStartTripClick(order) }
            btnTakePhoto.setOnClickListener { onConfirmClick(order.order_id, position, this@MyPackageOrdersAdapter) }
            btnWhatsApp.setOnClickListener { onWhatsAppClick(order) }
            btnCancel.setOnClickListener { onCancelClick(order) } // ✅ NUEVO

            loadCustomerAvatar(order.user_id)
        }

        private fun loadCustomerAvatar(userId: String) {
            if (userId.isEmpty()) return
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = RetrofitClient.instance.getUserAvatar(
                        token = SessionManager.getToken(),
                        userId = userId
                    )
                    if (response.isSuccessful) {
                        val avatarUrl = response.body()?.avatarUrl
                        withContext(Dispatchers.Main) {
                            if (!avatarUrl.isNullOrEmpty()) {
                                Glide.with(itemView.context)
                                    .load(avatarUrl)
                                    .placeholder(R.drawable.ic_default_avatar)
                                    .error(R.drawable.ic_default_avatar)
                                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                                    .circleCrop()
                                    .into(ivCustomerAvatar)
                            }
                        }
                    }
                } catch (e: Exception) { }
            }
        }
    }
}