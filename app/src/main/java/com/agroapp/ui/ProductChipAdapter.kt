package com.agroapp.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.agroapp.R
import com.agroapp.model.Product
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions

class ProductChipAdapter(
    private var products: List<Product>,
    private val onClick: (Product) -> Unit
) : RecyclerView.Adapter<ProductChipAdapter.ChipViewHolder>() {

    inner class ChipViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivProduct: ImageView = view.findViewById(R.id.tvChipEmoji)
        val tvName: TextView = view.findViewById(R.id.tvChipName)
        val tvPrice: TextView = view.findViewById(R.id.tvChipPrice)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChipViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product_chip, parent, false)
        return ChipViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChipViewHolder, position: Int) {
        val product = products[position]

        // ✅ Cargar imagen desde Supabase si existe, sino usar drawable local
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
        holder.tvPrice.text = "$${"%.2f".format(product.price)}/${product.unit}"
        holder.itemView.setOnClickListener { onClick(product) }
    }

    override fun getItemCount() = products.size

    fun updateProducts(newProducts: List<Product>) {
        products = newProducts
        notifyDataSetChanged()
    }
}