package com.agroapp.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.agroapp.R
import com.agroapp.network.YappiPendingOrder
import java.text.NumberFormat
import java.util.Locale

class YappiPendingAdapter(
    private val onApprove: (YappiPendingOrder) -> Unit,
    private val onReject: (YappiPendingOrder) -> Unit
) : ListAdapter<YappiPendingOrder, YappiPendingAdapter.ViewHolder>(DiffCallback()) {

    private val formatter = NumberFormat.getCurrencyInstance(Locale.US)

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvOrderId: TextView = view.findViewById(R.id.tvYappiOrderId)
        val tvCustomer: TextView = view.findViewById(R.id.tvYappiCustomer)
        val tvAmount: TextView = view.findViewById(R.id.tvYappiAmount)
        val tvReference: TextView = view.findViewById(R.id.tvYappiReference)
        val tvDate: TextView = view.findViewById(R.id.tvYappiDate)
        val tvAddress: TextView = view.findViewById(R.id.tvYappiAddress)
        val btnApprove: Button = view.findViewById(R.id.btnYappiApprove)
        val btnReject: Button = view.findViewById(R.id.btnYappiReject)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_yappi_pending, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val order = getItem(position)

        holder.tvOrderId.text = "Pedido #${order.id.take(8).uppercase()}"
        holder.tvCustomer.text = "\uD83D\uDC64 ${order.customer_name}   \uD83D\uDCF1 ${order.customer_phone}"
        holder.tvReference.text = "\uD83D\uDCDD Ref: ${order.reference_code}"
        holder.tvDate.text = "\uD83D\uDD50 ${order.created_at.take(16).replace("T", " ")}"
        holder.tvAddress.text = "\uD83D\uDCCD ${order.delivery_address ?: "Sin direcci\u00f3n"}"

        // total_amount = productos (SIN propina, asi lo guarda el backend actual)
        // tip_amount = propina
        // total real = total_amount + tip_amount
        val productos = order.total_amount
        val tip = order.tipAmount
        val totalReal = productos + tip

        val amountText = buildString {
            append("Productos:   ${formatter.format(productos)}\n")
            if (tip > 0) append("Propina:       ${formatter.format(tip)}\n")
            append("\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\n")
            append("Total:           ${formatter.format(totalReal)}")
        }
        holder.tvAmount.text = amountText

        holder.btnApprove.setOnClickListener { onApprove(order) }
        holder.btnReject.setOnClickListener { onReject(order) }
    }

    class DiffCallback : DiffUtil.ItemCallback<YappiPendingOrder>() {
        override fun areItemsTheSame(a: YappiPendingOrder, b: YappiPendingOrder) = a.id == b.id
        override fun areContentsTheSame(a: YappiPendingOrder, b: YappiPendingOrder) = a == b
    }
}