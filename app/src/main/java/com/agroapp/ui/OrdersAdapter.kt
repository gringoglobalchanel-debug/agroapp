package com.agroapp.ui

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
        val btnCancel: Button = view.findViewById(R.id.btnCancel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_order, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = orders[position]

        // ID del pedido (primeros 8 caracteres)
        holder.tvOrderId.text = "Pedido #${order.id.take(8).uppercase()}"

        // Estado con color
        val (statusText, statusColor) = when (order.status) {
            "pending" -> Pair("⏳ Pendiente", "#FF8F00")
            "confirmed" -> Pair("✅ Confirmado", "#1976D2")
            "delivered" -> Pair("📦 Entregado", "#2E7D32")
            "cancelled" -> Pair("❌ Cancelado", "#D32F2F")
            else -> Pair(order.status, "#757575")
        }
        holder.tvStatus.text = statusText
        holder.tvStatus.setBackgroundColor(Color.parseColor(statusColor))

        // Fecha de entrega
        val deliveryDate = order.deliveryDate.take(10)
        val paymentMethod = if (order.paymentMethod == "cash") "Contra entrega" else "Tarjeta"
        holder.tvDate.text = "📅 Entrega: $deliveryDate  •  💳 $paymentMethod"

        // Productos
        val productsList = order.items?.joinToString("\n") { item ->
            "• ${item.products?.name ?: "Producto"} x${formatQty(item.quantity)} ${item.products?.unit ?: ""} — $${"%.2f".format(item.subtotal)}"
        } ?: "Sin detalles"
        holder.tvProducts.text = productsList

        // Total
        holder.tvTotal.text = "Total: $${"%.2f".format(order.totalAmount)}"

        // Botón cancelar — solo si está pendiente
        if (order.status == "pending") {
            holder.btnCancel.visibility = View.VISIBLE
            holder.btnCancel.setOnClickListener {
                onCancel(order.id)
            }
        } else {
            holder.btnCancel.visibility = View.GONE
        }
    }

    override fun getItemCount() = orders.size

    private fun formatQty(qty: Double): String =
        if (qty % 1 == 0.0) qty.toInt().toString() else String.format("%.1f", qty)
}