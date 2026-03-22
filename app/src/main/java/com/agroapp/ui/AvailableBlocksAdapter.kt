package com.agroapp.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.agroapp.R
import com.agroapp.model.DeliveryBlock
import java.text.NumberFormat
import java.util.Locale

class AvailableBlocksAdapter(
    private val onTakeClick: (DeliveryBlock) -> Unit
) : RecyclerView.Adapter<AvailableBlocksAdapter.ViewHolder>() {

    private var blocks: List<DeliveryBlock> = emptyList()

    fun submitList(list: List<DeliveryBlock>) {
        blocks = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_available_block, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(blocks[position])
    }

    override fun getItemCount() = blocks.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvZone: TextView = itemView.findViewById(R.id.tvZone)
        private val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        private val tvOrders: TextView = itemView.findViewById(R.id.tvOrders)
        private val tvPayment: TextView = itemView.findViewById(R.id.tvPayment)
        private val btnTake: Button = itemView.findViewById(R.id.btnTake)

        fun bind(block: DeliveryBlock) {
            val formatter = NumberFormat.getCurrencyInstance(Locale.US)

            tvZone.text = block.zone ?: "Zona no especificada"
            tvTime.text = "${block.start_time} - ${block.end_time}"
            tvOrders.text = "${block.available_orders}/${block.total_orders} entregas disponibles"
            tvPayment.text = formatter.format(block.driver_payment)

            btnTake.setOnClickListener {
                onTakeClick(block)
            }
        }
    }
}