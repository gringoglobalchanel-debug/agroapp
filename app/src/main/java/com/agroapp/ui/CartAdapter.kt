package com.agroapp.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.agroapp.R
import com.agroapp.model.Product

class CartAdapter(
    private var cartItems: Map<Product, Double>,
    private val onQuantityChange: (Product, Double) -> Unit,
    private val onRemove: (Product) -> Unit
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    fun updateCart(newCart: Map<Product, Double>) {
        cartItems = newCart
        notifyDataSetChanged()
    }

    inner class CartViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvCartItemName)
        val tvPrice: TextView = itemView.findViewById(R.id.tvCartItemPrice)
        val tvQuantity: TextView = itemView.findViewById(R.id.tvCartItemQuantity)
        val tvSubtotal: TextView = itemView.findViewById(R.id.tvCartItemSubtotal)
        val btnIncrease: ImageButton = itemView.findViewById(R.id.btnIncrease)
        val btnDecrease: ImageButton = itemView.findViewById(R.id.btnDecrease)
        val btnRemove: Button = itemView.findViewById(R.id.btnRemove)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cart, parent, false)
        return CartViewHolder(view)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        val entry = cartItems.entries.toList()[position]
        val product = entry.key
        val quantity = entry.value
        val subtotal = product.price * quantity

        holder.tvName.text = product.name
        holder.tvPrice.text = "$${"%.2f".format(product.price)} / ${product.unit}"
        holder.tvQuantity.text = formatQuantity(quantity)
        holder.tvSubtotal.text = "$${"%.2f".format(subtotal)}"

        holder.btnIncrease.setOnClickListener {
            val step = if (product.unit == "kg" || product.unit == "lb") 0.5 else 1.0
            onQuantityChange(product, quantity + step)
        }

        holder.btnDecrease.setOnClickListener {
            val step = if (product.unit == "kg" || product.unit == "lb") 0.5 else 1.0
            if (quantity > step) {
                onQuantityChange(product, quantity - step)
            } else {
                onRemove(product)
            }
        }

        holder.btnRemove.setOnClickListener {
            onRemove(product)
        }
    }

    override fun getItemCount() = cartItems.size

    private fun formatQuantity(quantity: Double): String {
        return if (quantity % 1 == 0.0) quantity.toInt().toString()
        else String.format("%.1f", quantity)
    }
}