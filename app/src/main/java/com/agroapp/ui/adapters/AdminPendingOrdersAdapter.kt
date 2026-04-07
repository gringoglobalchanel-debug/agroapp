package com.agroapp.ui.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.agroapp.R
import com.agroapp.model.AdminPendingOrder
import java.text.NumberFormat
import java.util.Locale

class AdminPendingOrdersAdapter(
    private val onAssignDriver: ((AdminPendingOrder) -> Unit)? = null
) : ListAdapter<AdminPendingOrder, AdminPendingOrdersAdapter.ViewHolder>(DiffCallback()) {

    private val formatter = NumberFormat.getCurrencyInstance(Locale.US)

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvOrderId: TextView = view.findViewById(R.id.tvAdminOrderId)
        val tvCustomer: TextView = view.findViewById(R.id.tvAdminOrderCustomer)
        val tvStatus: TextView = view.findViewById(R.id.tvAdminOrderStatus)
        val tvWindow: TextView = view.findViewById(R.id.tvAdminOrderWindow)
        val tvZone: TextView = view.findViewById(R.id.tvAdminOrderZone)
        val tvAddress: TextView = view.findViewById(R.id.tvAdminOrderAddress)
        val tvProducts: TextView = view.findViewById(R.id.tvAdminOrderProducts)
        val tvTotal: TextView = view.findViewById(R.id.tvAdminOrderTotal)
        val btnAssign: Button = view.findViewById(R.id.btnAssignDriver)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_pending_order, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val order = getItem(position)

        holder.tvOrderId.text = "Pedido #${order.id.take(8).uppercase()}"
        holder.tvCustomer.text = "\uD83D\uDC64 ${order.customerName}  \uD83D\uDCF1 ${order.customerPhone}"

        val (statusText, statusColor) = when (order.status) {
            "pending"     -> "\uD83D\uDCCB Pendiente"   to "#FF8F00"
            "confirmed"   -> "\u2705 Confirmado"         to "#1976D2"
            "in_progress" -> "\uD83D\uDEB4 En camino"   to "#1B5E20"
            else          -> order.status               to "#757575"
        }
        holder.tvStatus.text = statusText
        holder.tvStatus.setBackgroundColor(Color.parseColor(statusColor))

        val windowDate = order.deliveryWindowDate ?: order.deliveryDate.take(10)
        val windowStart = order.deliveryWindowStart ?: ""
        val windowEnd = order.deliveryWindowEnd ?: ""
        holder.tvWindow.text = "\u23F0 Entrega: $windowDate  $windowStart \u2013 $windowEnd"

        holder.tvZone.text = when (order.zone) {
            "norte"  -> "\uD83D\uDDFA\uFE0F Zona Norte"
            "sur"    -> "\uD83D\uDDFA\uFE0F Zona Sur"
            "centro" -> "\uD83D\uDDFA\uFE0F Zona Centro"
            else     -> "\uD83D\uDDFA\uFE0F ${order.zone ?: "Sin zona"}"
        }

        holder.tvAddress.text = "\uD83D\uDCCD ${order.deliveryAddress ?: "Sin direcci\u00f3n"}"

        val productsList = order.items.joinToString("\n") { item ->
            val qty = if (item.quantity % 1 == 0.0) item.quantity.toInt().toString() else "%.1f".format(item.quantity)
            "\u2022 ${item.name}  \u00d7$qty ${item.unit}  \u2192  ${formatter.format(item.subtotal)}"
        }.ifEmpty { "Sin detalles" }
        holder.tvProducts.text = productsList

        val tip = order.tipAmount
        val total = order.totalAmount + tip
        val paymentLabel = when (order.paymentMethod) {
            "yappi" -> "\uD83D\uDCF1 Yappi"
            "card"  -> "\uD83D\uDCB3 Tarjeta"
            else    -> order.paymentMethod
        }
        holder.tvTotal.text = buildString {
            append("Subtotal:   ${formatter.format(order.totalAmount)}\n")
            if (tip > 0) append("Propina:     ${formatter.format(tip)}\n")
            append("\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\n")
            append("Total:         ${formatter.format(total)}  $paymentLabel")
        }

        // ✅ Botón asignar driver — solo si no está asignado
        if (!order.isAssigned && onAssignDriver != null) {
            holder.btnAssign.visibility = View.VISIBLE
            holder.btnAssign.setOnClickListener { onAssignDriver.invoke(order) }
        } else {
            holder.btnAssign.visibility = View.GONE
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<AdminPendingOrder>() {
        override fun areItemsTheSame(a: AdminPendingOrder, b: AdminPendingOrder) = a.id == b.id
        override fun areContentsTheSame(a: AdminPendingOrder, b: AdminPendingOrder) = a == b
    }
}