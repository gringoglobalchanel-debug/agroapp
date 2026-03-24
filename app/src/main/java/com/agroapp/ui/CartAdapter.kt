package com.agroapp.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.agroapp.R
import com.agroapp.model.Product
import java.text.NumberFormat
import java.util.Locale

class CartAdapter(
    private var cartItems: Map<Product, Double>,
    private val onQuantityChange: (Product, Double) -> Unit,
    private val onRemove: (Product) -> Unit
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    private val formatter = NumberFormat.getCurrencyInstance(Locale.US)

    fun updateCart(newItems: Map<Product, Double>) {
        cartItems = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cart, parent, false)
        return CartViewHolder(view)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        val entry = cartItems.entries.elementAt(position)
        holder.bind(entry.key, entry.value)
    }

    override fun getItemCount() = cartItems.size

    inner class CartViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tvCartItemName)
        private val tvPrice: TextView = itemView.findViewById(R.id.tvCartItemPrice)
        private val tvQuantity: TextView = itemView.findViewById(R.id.tvCartItemQuantity)
        private val tvSubtotal: TextView = itemView.findViewById(R.id.tvCartItemSubtotal)
        private val btnDecrease: ImageButton = itemView.findViewById(R.id.btnDecrease)
        private val btnIncrease: ImageButton = itemView.findViewById(R.id.btnIncrease)
        private val btnRemove: ImageButton = itemView.findViewById(R.id.btnRemove)

        fun bind(product: Product, quantity: Double) {
            tvName.text = product.name
            tvPrice.text = "${formatter.format(product.price)} / ${product.unit}"
            tvQuantity.text = if (quantity % 1 == 0.0) quantity.toInt().toString() else String.format("%.1f", quantity)
            val subtotal = product.price * quantity
            tvSubtotal.text = formatter.format(subtotal)

            btnDecrease.setOnClickListener {
                val newQuantity = quantity - 1.0
                if (newQuantity > 0) {
                    onQuantityChange(product, newQuantity)
                } else {
                    onRemove(product)
                }
            }

            btnIncrease.setOnClickListener {
                onQuantityChange(product, quantity + 1.0)
            }

            btnRemove.setOnClickListener {
                onRemove(product)
            }
        }
    }
}