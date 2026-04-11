package com.agroapp.ui

import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.agroapp.R
import com.agroapp.model.Order

class OrdersAdapter(
    private var orders: List<Order>,
    private val onCancel: (String) -> Unit
) : RecyclerView.Adapter<OrdersAdapter.OrderViewHolder>() {

    fun updateOrders(newOrders: List<Order>) {
        orders = newOrders
        notifyDataSetChanged()
    }

    inner class OrderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvOrderId: TextView = view.findViewById(R.id.tvOrderId)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
        val tvProducts: TextView = view.findViewById(R.id.tvProducts)
        val tvTotal: TextView = view.findViewById(R.id.tvTotal)
        val tvDeliveryWindow: TextView = view.findViewById(R.id.tvDeliveryWindow)
        val btnTrack: Button = view.findViewById(R.id.btnTrack)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = orders[position]

        holder.tvOrderId.text = "Pedido #${order.id.take(8).uppercase()}"

        val deliveryDate = order.deliveryDate.take(10)
        val paymentMethod = when (order.paymentMethod) {
            "yappi" -> "Yappi"
            "card"  -> "Tarjeta"
            "cash"  -> "Efectivo"
            else    -> order.paymentMethod
        }
        holder.tvDate.text = "$deliveryDate  -  $paymentMethod"

        val (statusText, statusColor) = when (order.status) {
            "waiting_confirmation" -> "Esperando pago"   to "#E65100"
            "pending"              -> "Pendiente"         to "#FF8F00"
            "pending_approval"     -> "En revision"       to "#6A1B9A"
            "confirmed"            -> "Confirmado"        to "#1976D2"
            "in_progress"          -> "En camino"         to "#1B5E20"
            "delivered",
            "completed"            -> "Entregado"         to "#2E7D32"
            "cancelled"            -> "Cancelado"         to "#D32F2F"
            else                   -> order.status        to "#757575"
        }
        holder.tvStatus.text = statusText
        holder.tvStatus.setBackgroundColor(Color.parseColor(statusColor))

        val productsList = order.items?.joinToString("\n") { item ->
            val name = item.products?.name ?: "Producto"
            val qty  = formatQty(item.quantity)
            val unit = item.products?.unit ?: ""
            val sub  = "%.2f".format(item.subtotal)
            "- $name  x$qty $unit  ->  \$$sub"
        } ?: "Sin detalles"
        holder.tvProducts.text = productsList

        val tip = order.tipAmount
        val itemsSubtotal = order.items?.sumOf { it.subtotal }?.takeIf { it > 0.0 }
        val productosTotal = when {
            itemsSubtotal != null && itemsSubtotal > 0.0 -> itemsSubtotal
            order.totalAmount > tip -> order.totalAmount - tip
            else -> order.totalAmount
        }
        val finalTotal = productosTotal + tip

        val totalLines = buildString {
            append("Subtotal:   \$%.2f".format(productosTotal))
            append("\nEnvio:        GRATIS")
            if (tip > 0) append("\nPropina:     \$%.2f".format(tip))
            append("\n--------------------")
            append("\nTotal:         \$%.2f".format(finalTotal))
            if (order.status == "cancelled" && !order.notes.isNullOrEmpty()) {
                append("\n\n${order.notes}")
            }
        }
        holder.tvTotal.text = totalLines

        val windowText = order.deliveryWindowStart?.let { start ->
            val end = order.deliveryWindowEnd ?: ""
            val date = order.deliveryWindowDate ?: deliveryDate
            "Entrega: $date  $start - $end"
        }
        if (!windowText.isNullOrEmpty()) {
            holder.tvDeliveryWindow.visibility = View.VISIBLE
            holder.tvDeliveryWindow.text = windowText
        } else {
            holder.tvDeliveryWindow.visibility = View.GONE
        }

        // ✅ Boton cancelar eliminado

        if (order.status == "in_progress") {
            holder.btnTrack.visibility = View.VISIBLE
            holder.btnTrack.setOnClickListener {
                val intent = Intent(holder.itemView.context, TrackingActivity::class.java).apply {
                    putExtra("order_id", order.id)
                    putExtra("driver_id", order.driverId ?: "")
                    putExtra("order_total", order.totalAmount)
                    putExtra("delivery_lat", order.deliveryLatitude ?: 0.0)
                    putExtra("delivery_lng", order.deliveryLongitude ?: 0.0)
                }
                holder.itemView.context.startActivity(intent)
            }
        } else {
            holder.btnTrack.visibility = View.GONE
        }
    }

    override fun getItemCount() = orders.size

    private fun formatQty(qty: Double): String =
        if (qty % 1 == 0.0) qty.toInt().toString() else "%.1f".format(qty)
}