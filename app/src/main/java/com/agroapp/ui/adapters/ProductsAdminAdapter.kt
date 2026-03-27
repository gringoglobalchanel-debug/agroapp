package com.agroapp.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.agroapp.R
import com.agroapp.model.ProductWithInventory
import java.text.NumberFormat
import java.util.Locale

class ProductsAdminAdapter(
    private val onEditClick: (ProductWithInventory) -> Unit,
    private val onStockClick: (ProductWithInventory) -> Unit,
    private val onDeleteClick: (ProductWithInventory) -> Unit
) : RecyclerView.Adapter<ProductsAdminAdapter.ProductViewHolder>() {

    private var products: List<ProductWithInventory> = emptyList()
    private val formatter = NumberFormat.getCurrencyInstance(Locale.US)

    fun submitList(list: List<ProductWithInventory>) {
        products = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = products[position]
        holder.bind(product)
    }

    override fun getItemCount(): Int = products.size

    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: CardView = itemView.findViewById(R.id.cardView)
        private val tvName: TextView = itemView.findViewById(R.id.tvProductName)
        private val tvPrice: TextView = itemView.findViewById(R.id.tvProductPrice)
        private val tvStock: TextView = itemView.findViewById(R.id.tvProductStock)
        private val tvCategory: TextView = itemView.findViewById(R.id.tvProductCategory)
        private val tvUnit: TextView = itemView.findViewById(R.id.tvProductUnit)
        private val btnEdit: Button = itemView.findViewById(R.id.btnEditProduct)
        private val btnStock: Button = itemView.findViewById(R.id.btnUpdateStock)
        private val btnDelete: Button = itemView.findViewById(R.id.btnDeleteProduct)

        fun bind(product: ProductWithInventory) {
            tvName.text = product.name
            tvPrice.text = formatter.format(product.price)
            tvCategory.text = product.category ?: "Sin categoría"
            tvUnit.text = product.unit

            val stock = product.stock ?: 0.0
            val minStock = product.minStock ?: 0.0

            when {
                stock <= 0 -> {
                    tvStock.text = "🚫 AGOTADO"
                    tvStock.setTextColor(itemView.context.getColor(android.R.color.holo_red_dark))
                    cardView.setCardBackgroundColor(itemView.context.getColor(R.color.red_50))
                }
                stock <= minStock -> {
                    val stockText = if (stock % 1 == 0.0) stock.toInt().toString() else String.format("%.2f", stock)
                    tvStock.text = "⚠️ Stock bajo:  "
                    tvStock.setTextColor(itemView.context.getColor(android.R.color.holo_orange_dark))
                    cardView.setCardBackgroundColor(itemView.context.getColor(R.color.yellow_50))
                }
                else -> {
                    val stockText = if (stock % 1 == 0.0) stock.toInt().toString() else String.format("%.2f", stock)
                    tvStock.text = "✅ Stock:  "
                    tvStock.setTextColor(itemView.context.getColor(android.R.color.holo_green_dark))
                    cardView.setCardBackgroundColor(itemView.context.getColor(android.R.color.white))
                }
            }

            btnEdit.setOnClickListener { onEditClick(product) }
            btnStock.setOnClickListener { onStockClick(product) }
            btnDelete.setOnClickListener { onDeleteClick(product) }
        }
    }
}
