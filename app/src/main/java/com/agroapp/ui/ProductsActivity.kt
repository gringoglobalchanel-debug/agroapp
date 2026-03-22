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
    private var allProducts: List<Product> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_products)

        selectedCategory = intent.getStringExtra("CATEGORY")
        searchQuery = intent.getStringExtra("SEARCH_QUERY")

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

        // TEMPORAL: canOrder = true para pruebas fuera de horario
        // Cambiar a: val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY); val canOrder = hour in 8..12
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
            onAdd = { product, quantity ->
                viewModel.addToCart(product, quantity)
            }
        )
        rvProducts.layoutManager = LinearLayoutManager(this)
        rvProducts.adapter = adapter

        // Búsqueda predictiva en tiempo real
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

        // Observar productos
        viewModel.products.observe(this) { products ->
            progressBar.visibility = View.GONE
            if (products.isNullOrEmpty()) return@observe

            allProducts = products

            val productNames = products.map { it.name }
            val autoAdapter = ArrayAdapter(
                this,
                android.R.layout.simple_dropdown_item_1line,
                productNames
            )
            searchInput.setAdapter(autoAdapter)

            val currentQuery = searchInput.text?.toString()?.trim() ?: ""
            filterProducts(currentQuery, tvResultCount)
        }

        // Observar carrito → actualizar barra flotante en tiempo real
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

        // Al tocar la barra del carrito → abrir CartActivity
        cartBar.setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
        }

        // Observar carga
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