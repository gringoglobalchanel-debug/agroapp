package com.agroapp.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.agroapp.R
import com.agroapp.model.Product
import com.agroapp.viewmodel.ProductViewModel

class ProductsActivity : AppCompatActivity() {

    private val viewModel: ProductViewModel by viewModels()
    private lateinit var adapter: ProductAdapter
    private var selectedCategory: String? = null
    private var searchQuery: String? = null
    private var productId: Int? = null // ✅ NUEVO
    private var allProducts: List<Product> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_products)

        selectedCategory = intent.getStringExtra("CATEGORY")
        searchQuery = intent.getStringExtra("SEARCH_QUERY")
        productId = intent.getIntExtra("PRODUCT_ID", -1).takeIf { it > 0 } // ✅ NUEVO

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        val rvProducts = findViewById<RecyclerView>(R.id.rvProducts)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val tvHorario = findViewById<TextView>(R.id.tvHorario)
        val tvResultCount = findViewById<TextView>(R.id.tvResultCount)
        val searchInput = findViewById<AutoCompleteTextView>(R.id.searchInput)
        val btnClearSearch = findViewById<ImageView>(R.id.btnClearSearch)
        val cartBar = findViewById<LinearLayout>(R.id.cartBar)
        val tvCartTotal = findViewById<TextView>(R.id.tvCartTotal)
        val tvCartItemCount = findViewById<TextView>(R.id.tvCartItemCount)

        setSupportActionBar(toolbar)
        supportActionBar?.title = when {
            selectedCategory != null -> selectedCategory
            else -> "Productos"
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (searchQuery != null) {
            searchInput.setText(searchQuery)
        }

        val canOrder = true

        if (!canOrder) {
            tvHorario.visibility = View.VISIBLE
            tvHorario.text = "🕒 Pedidos solo de 8am a 12pm"
        } else {
            tvHorario.visibility = View.GONE
        }

        adapter = ProductAdapter(
            products = emptyList(),
            cartMap = emptyMap(),
            canOrder = canOrder,
            onAdd = { product, quantity -> viewModel.addToCart(product, quantity) }
        )
        rvProducts.layoutManager = LinearLayoutManager(this)
        rvProducts.adapter = adapter

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim() ?: ""
                btnClearSearch.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
                filterProducts(query, tvResultCount)
            }
        })

        btnClearSearch.setOnClickListener {
            searchInput.setText("")
            searchInput.clearFocus()
        }

        viewModel.products.observe(this) { products ->
            progressBar.visibility = View.GONE
            if (products.isNullOrEmpty()) return@observe

            allProducts = products

            val productNames = products.map { it.name }
            searchInput.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, productNames))

            val currentQuery = searchInput.text?.toString()?.trim() ?: ""
            filterProducts(currentQuery, tvResultCount)

            // ✅ NUEVO: si viene de un banner con product_id, hace scroll al producto
            productId?.let { targetId ->
                val index = allProducts.indexOfFirst { it.id == targetId }
                if (index >= 0) {
                    rvProducts.post {
                        (rvProducts.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(index, 0)
                    }
                }
            }
        }

        viewModel.cart.observe(this) { cart ->
            adapter.updateCart(cart ?: emptyMap())
            val itemCount = cart?.size ?: 0
            val total = cart?.entries?.sumOf { it.key.price * it.value } ?: 0.0
            if (itemCount > 0) {
                cartBar.visibility = View.VISIBLE
                tvCartItemCount.text = "$itemCount item${if (itemCount != 1) "s" else ""}"
                tvCartTotal.text = "$${"%.2f".format(total)}"
            } else {
                cartBar.visibility = View.GONE
            }
        }

        cartBar.setOnClickListener { startActivity(Intent(this, CartActivity::class.java)) }

        viewModel.loading.observe(this) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        progressBar.visibility = View.VISIBLE
        viewModel.loadProducts()
    }

    private fun filterProducts(query: String, tvResultCount: TextView) {
        var filtered = allProducts

        if (selectedCategory != null) {
            filtered = filtered.filter {
                it.categories?.name.equals(selectedCategory, ignoreCase = true)
            }
        }

        if (query.isNotEmpty()) {
            filtered = filtered.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.categories?.name?.contains(query, ignoreCase = true) == true
            }
        }

        adapter.updateProducts(filtered)

        if (query.isNotEmpty()) {
            tvResultCount.visibility = View.VISIBLE
            tvResultCount.text = "${filtered.size} resultado${if (filtered.size != 1) "s" else ""} para \"$query\""
        } else {
            tvResultCount.visibility = View.GONE
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}