package com.agroapp.ui

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
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
        val btnCancel: Button = view.findViewById(R.id.btnCancel)
        val btnTrack: Button = view.findViewById(R.id.btnTrack)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = orders[position]

        // ── ID ──
        holder.tvOrderId.text = "Pedido #${order.id.take(8).uppercase()}"

        // ── Fecha + método de pago ──
        val deliveryDate = order.deliveryDate.take(10)
        val paymentMethod = when (order.paymentMethod) {
            "yappi" -> "\uD83D\uDCF1 Yappi"
            "card"  -> "\uD83D\uDCB3 Tarjeta"
            "cash"  -> "\uD83D\uDCB5 Efectivo"
            else    -> order.paymentMethod
        }
        holder.tvDate.text = "\uD83D\uDCC5 $deliveryDate  \u00b7  $paymentMethod"

        // ── Estado ──
        val (statusText, statusColor) = when (order.status) {
            "waiting_confirmation" -> "\u23F3 Esperando pago"        to "#E65100"
            "pending"              -> "\uD83D\uDCCB Pendiente"        to "#FF8F00"
            "pending_approval"     -> "\uD83D\uDD0D En revisi\u00f3n" to "#6A1B9A"
            "confirmed"            -> "\u2705 Confirmado"             to "#1976D2"
            "in_progress"          -> "\uD83D\uDEB4 En camino"        to "#1B5E20"
            "delivered",
            "completed"            -> "\uD83D\uDCE6 Entregado"        to "#2E7D32"
            "cancelled"            -> "\u274C Cancelado"              to "#D32F2F"
            else                   -> order.status                    to "#757575"
        }
        holder.tvStatus.text = statusText
        holder.tvStatus.setBackgroundColor(Color.parseColor(statusColor))

        // ── Productos ──
        val productsList = order.items?.joinToString("\n") { item ->
            val name = item.products?.name ?: "Producto"
            val qty  = formatQty(item.quantity)
            val unit = item.products?.unit ?: ""
            val sub  = "%.2f".format(item.subtotal)
            "\u2022 $name  \u00d7$qty $unit  \u2192  \$$sub"
        } ?: "Sin detalles"
        holder.tvProducts.text = productsList

        // ── Resumen de costos ──
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
            append("\nEnv\u00edo:        GRATIS \uD83C\uDF89")
            if (tip > 0) append("\nPropina:     \$%.2f".format(tip))
            append("\n────────────────────")
            append("\nTotal:         \$%.2f".format(finalTotal))
            if (order.status == "cancelled" && !order.notes.isNullOrEmpty()) {
                append("\n\n\u26A0\uFE0F ${order.notes}")
            }
        }
        holder.tvTotal.text = totalLines

        // ── Ventana de entrega ──
        val windowText = order.deliveryWindowStart?.let { start ->
            val end = order.deliveryWindowEnd ?: ""
            val date = order.deliveryWindowDate ?: deliveryDate
            "\u23F0 Entrega: $date  $start \u2013 $end"
        }
        if (!windowText.isNullOrEmpty()) {
            holder.tvDeliveryWindow.visibility = View.VISIBLE
            holder.tvDeliveryWindow.text = windowText
        } else {
            holder.tvDeliveryWindow.visibility = View.GONE
        }

        // ── Botón cancelar ──
        if (order.status == "waiting_confirmation" || order.status == "pending") {
            holder.btnCancel.visibility = View.VISIBLE
            holder.btnCancel.setOnClickListener { onCancel(order.id) }
        } else {
            holder.btnCancel.visibility = View.GONE
        }

        // ── Botón seguir pedido ──
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