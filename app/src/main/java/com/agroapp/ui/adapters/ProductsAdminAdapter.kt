package com.agroapp.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
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
        private val tvName: TextView = itemView.findViewById(R.id.tvProductName)
        private val tvPrice: TextView = itemView.findViewById(R.id.tvProductPrice)
        private val tvStock: TextView = itemView.findViewById(R.id.tvProductStock)
        private val tvCategory: TextView = itemView.findViewById(R.id.tvProductCategory)
        private val btnEdit: Button = itemView.findViewById(R.id.btnEditProduct)
        private val btnStock: Button = itemView.findViewById(R.id.btnUpdateStock)
        private val btnDelete: Button = itemView.findViewById(R.id.btnDeleteProduct)

        fun bind(product: ProductWithInventory) {
            tvName.text = product.name
            tvPrice.text = formatter.format(product.price)
            
            val stock = product.stock ?: 0.0
            val minStock = product.minStock ?: 0.0
            
            tvStock.text = "Stock: "
            tvStock.setTextColor(
                if (stock <= 0) itemView.context.getColor(android.R.color.holo_red_dark)
                else if (stock <= minStock) itemView.context.getColor(android.R.color.holo_orange_dark)
                else itemView.context.getColor(android.R.color.black)
            )
            
            tvCategory.text = product.category ?: "Sin categoría"

            btnEdit.setOnClickListener { onEditClick(product) }
            btnStock.setOnClickListener { onStockClick(product) }
            btnDelete.setOnClickListener { onDeleteClick(product) }
        }
    }
}
