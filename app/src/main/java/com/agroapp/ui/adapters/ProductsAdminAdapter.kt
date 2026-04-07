package com.agroapp.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.agroapp.R
import com.agroapp.model.ProductWithInventory
import com.bumptech.glide.Glide
import java.text.NumberFormat
import java.util.Locale

class ProductsAdminAdapter(
    private val onEditClick: (ProductWithInventory) -> Unit,
    private val onStockClick: (ProductWithInventory) -> Unit,
    private val onDeleteClick: (ProductWithInventory) -> Unit
) : RecyclerView.Adapter<ProductsAdminAdapter.ProductViewHolder>() {

    private var allProducts: List<ProductWithInventory> = emptyList()
    private var filteredProducts: List<ProductWithInventory> = emptyList()
    private val formatter = NumberFormat.getCurrencyInstance(Locale.US)

    fun submitList(list: List<ProductWithInventory>) {
        // ✅ Ordenar alfabéticamente
        allProducts = list.sortedBy { it.name.lowercase() }
        filteredProducts = allProducts
        notifyDataSetChanged()
    }

    // ✅ Buscador
    fun filter(query: String) {
        filteredProducts = if (query.isEmpty()) {
            allProducts
        } else {
            allProducts.filter { it.name.contains(query, ignoreCase = true) || it.category?.contains(query, ignoreCase = true) == true }
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(filteredProducts[position])
    }

    override fun getItemCount(): Int = filteredProducts.size

    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: CardView = itemView.findViewById(R.id.cardView)
        private val ivProduct: ImageView = itemView.findViewById(R.id.ivProductImage)
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
            tvCategory.text = product.category ?: "Sin categor\u00eda"
            tvUnit.text = "por ${product.unit}"

            // ✅ Cargar imagen desde Supabase
            if (!product.imageUrl.isNullOrEmpty()) {
                Glide.with(itemView.context)
                    .load(product.imageUrl)
                    .placeholder(R.drawable.ic_product_placeholder)
                    .error(R.drawable.ic_product_placeholder)
                    .centerCrop()
                    .into(ivProduct)
            } else {
                ivProduct.setImageResource(R.drawable.ic_product_placeholder)
            }

            val stock = product.stock ?: 0.0
            val minStock = product.minStock ?: 0.0
            val stockText = if (stock % 1 == 0.0) stock.toInt().toString() else "%.1f".format(stock)

            when {
                stock <= 0 -> {
                    tvStock.text = "\uD83D\uDEAB Agotado"
                    tvStock.setTextColor(itemView.context.getColor(android.R.color.holo_red_dark))
                    cardView.setCardBackgroundColor(itemView.context.getColor(R.color.red_50))
                }
                stock <= minStock -> {
                    tvStock.text = "\u26A0\uFE0F Stock bajo: $stockText"
                    tvStock.setTextColor(itemView.context.getColor(android.R.color.holo_orange_dark))
                    cardView.setCardBackgroundColor(itemView.context.getColor(R.color.yellow_50))
                }
                else -> {
                    tvStock.text = "\u2705 Stock: $stockText"
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