package com.agroapp.ui

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.agroapp.R
import com.agroapp.model.Product
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions

class ProductAdapter(
    private var products: List<Product>,
    private var cartMap: Map<Product, Double>,
    private val canOrder: Boolean,
    private val onAdd: (Product, Double) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    private val quantities = mutableMapOf<Int, Double>()

    fun updateProducts(newProducts: List<Product>) {
        products = newProducts
        quantities.clear()
        notifyDataSetChanged()
    }

    fun updateCart(newCart: Map<Product, Double>) {
        cartMap = newCart
        notifyDataSetChanged()
    }

    inner class ProductViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivProduct: ImageView = view.findViewById(R.id.tvEmoji)
        val tvName: TextView = view.findViewById(R.id.tvName)
        val tvPrice: TextView = view.findViewById(R.id.tvPrice)
        val tvUnit: TextView = view.findViewById(R.id.tvUnit)
        val tvQuantity: TextView = view.findViewById(R.id.tvQuantity)
        val btnPlus: ImageButton = view.findViewById(R.id.btnPlus)
        val btnMinus: ImageButton = view.findViewById(R.id.btnMinus)
        val btnAdd: Button = view.findViewById(R.id.btnAdd)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = products[position]
        val qty = quantities[product.id] ?: 0.0

        // ✅ Si tiene imagen en Supabase la carga con Glide, si no usa el drawable local
        if (!product.imageUrl.isNullOrEmpty()) {
            Glide.with(holder.ivProduct.context)
                .load(product.imageUrl)
                .placeholder(ProductImageMapper.getImage(product.name))
                .error(ProductImageMapper.getImage(product.name))
                .centerCrop()
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(holder.ivProduct)
        } else {
            holder.ivProduct.setImageResource(ProductImageMapper.getImage(product.name))
        }

        holder.tvName.text = product.name
        holder.tvPrice.text = "$${"%.2f".format(product.price)}"
        holder.tvUnit.text = "por ${product.unit}"
        holder.tvQuantity.text = formatQty(qty)

        holder.btnPlus.isEnabled = canOrder
        holder.btnMinus.isEnabled = canOrder
        holder.btnAdd.alpha = if (canOrder) 1.0f else 0.5f

        val inCart = cartMap.entries.find { it.key.id == product.id }?.value ?: 0.0
        if (inCart > 0) {
            holder.btnAdd.text = "En carrito: ${formatQty(inCart)}"
            holder.btnAdd.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#388E3C"))
            holder.btnAdd.isEnabled = false
        } else {
            holder.btnAdd.text = "Agregar"
            holder.btnAdd.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#2E7D32"))
            holder.btnAdd.isEnabled = canOrder
        }

        holder.btnPlus.setOnClickListener {
            val pos = holder.adapterPosition
            if (pos == RecyclerView.NO_ID.toInt()) return@setOnClickListener
            val p = products[pos]
            val step = if (p.unit == "kg" || p.unit == "lb") 0.5 else 1.0
            quantities[p.id] = (quantities[p.id] ?: 0.0) + step
            notifyItemChanged(pos)
        }

        holder.btnMinus.setOnClickListener {
            val pos = holder.adapterPosition
            if (pos == RecyclerView.NO_ID.toInt()) return@setOnClickListener
            val p = products[pos]
            val step = if (p.unit == "kg" || p.unit == "lb") 0.5 else 1.0
            val current = quantities[p.id] ?: 0.0
            if (current > 0) {
                quantities[p.id] = (current - step).coerceAtLeast(0.0)
                notifyItemChanged(pos)
            }
        }

        holder.btnAdd.setOnClickListener {
            val pos = holder.adapterPosition
            if (pos == RecyclerView.NO_ID.toInt()) return@setOnClickListener
            val p = products[pos]
            val q = quantities[p.id] ?: 0.0
            if (q > 0) {
                onAdd(p, q)
                quantities[p.id] = 0.0
                notifyItemChanged(pos)
            }
        }
    }

    override fun getItemCount() = products.size

    private fun formatQty(qty: Double): String =
        if (qty % 1 == 0.0) qty.toInt().toString() else String.format("%.1f", qty)
}