package com.agroapp.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.agroapp.R
import com.agroapp.model.DriverPayment
import java.text.NumberFormat
import java.util.Locale

class PaymentsAdminAdapter(
    private val onProcessClick: (DriverPayment) -> Unit
) : RecyclerView.Adapter<PaymentsAdminAdapter.PaymentViewHolder>() {

    private var payments: List<DriverPayment> = emptyList()
    private val formatter = NumberFormat.getCurrencyInstance(Locale.US)

    fun submitList(list: List<DriverPayment>) {
        payments = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaymentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_payment, parent, false)
        return PaymentViewHolder(view)
    }

    override fun onBindViewHolder(holder: PaymentViewHolder, position: Int) {
        val payment = payments[position]
        holder.bind(payment)
    }

    override fun getItemCount(): Int = payments.size

    inner class PaymentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvDriverName: TextView = itemView.findViewById(R.id.tvDriverName)
        private val tvWeek: TextView = itemView.findViewById(R.id.tvWeekRange)
        private val tvTotalOrders: TextView = itemView.findViewById(R.id.tvTotalOrders)
        private val tvTotalTips: TextView = itemView.findViewById(R.id.tvTotalTips)
        private val tvNetAmount: TextView = itemView.findViewById(R.id.tvNetAmount)
        private val tvStatus: TextView = itemView.findViewById(R.id.tvPaymentStatus)
        private val btnProcess: Button = itemView.findViewById(R.id.btnProcessPayment)

        fun bind(payment: DriverPayment) {
            tvDriverName.text = payment.driverName ?: "Conductor"
            tvWeek.text = " - "
            tvTotalOrders.text = "Pedidos: "
            tvTotalTips.text = "Propinas: "
            tvNetAmount.text = formatter.format(payment.netAmount)
            
            when (payment.paymentStatus) {
                "paid" -> {
                    tvStatus.text = "✅ PAGADO"
                    tvStatus.setTextColor(itemView.context.getColor(android.R.color.holo_green_dark))
                    btnProcess.visibility = View.GONE
                }
                else -> {
                    tvStatus.text = "⏳ PENDIENTE"
                    tvStatus.setTextColor(itemView.context.getColor(android.R.color.holo_orange_dark))
                    btnProcess.visibility = View.VISIBLE
                    btnProcess.setOnClickListener { onProcessClick(payment) }
                }
            }
        }
    }
}
