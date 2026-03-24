package com.agroapp.ui

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.agroapp.R
import com.agroapp.model.Banner
import com.agroapp.network.SessionManager
import com.agroapp.viewmodel.ProductViewModel
import com.google.android.material.navigation.NavigationView
import com.google.android.material.textview.MaterialTextView

class HomeActivity : AppCompatActivity() {

    private val viewModel: ProductViewModel by viewModels()
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var chipAdapter: ProductChipAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        if (!SessionManager.isLoggedIn()) {
            goToLogin()
            return
        }

        initViews()
        setupDrawer()
        setupDireccion()
        setupSearch()
        setupPopularProducts()
        setupActionButtons()
        setupCategoryCards()
        setupCart()
        setupBannerCarousel()
        setupFooter()
    }

    private fun initViews() {
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)

        val headerView = navigationView.getHeaderView(0)
        val tvHeaderName = headerView.findViewById<MaterialTextView>(R.id.tvHeaderName)
        val tvHeaderEmail = headerView.findViewById<MaterialTextView>(R.id.tvHeaderEmail)

        tvHeaderName.text = SessionManager.getUserName()
        tvHeaderEmail.text = SessionManager.getUserEmail()
    }

    private fun setupDrawer() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> drawerLayout.closeDrawer(GravityCompat.START)
                R.id.nav_profile -> startActivity(Intent(this, ProfileActivity::class.java))
                R.id.nav_orders -> startActivity(Intent(this, OrdersActivity::class.java))
                R.id.nav_cart -> startActivity(Intent(this, CartActivity::class.java))
                R.id.nav_logout -> {
                    SessionManager.logout()
                    goToLogin()
                }
                R.id.nav_settings -> Toast.makeText(this, "Próximamente", Toast.LENGTH_SHORT).show()
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    private fun setupDireccion() {
        val tvDireccion = findViewById<TextView>(R.id.tvDireccion)
        val direccion = SessionManager.getAddress()
        tvDireccion.text = if (direccion.isNotEmpty()) direccion else "Dirección no especificada"
    }

    private fun setupSearch() {
        val searchView = findViewById<AutoCompleteTextView>(R.id.searchView)

        viewModel.loadProducts()
        viewModel.products.observe(this) { products ->
            if (!products.isNullOrEmpty()) {
                val productNames = products.map { it.name }
                val autoAdapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_dropdown_item_1line,
                    productNames
                )
                searchView.setAdapter(autoAdapter)
            }
        }

        searchView.setOnItemClickListener { _, _, _, _ ->
            val query = searchView.text.toString().trim()
            if (query.isNotEmpty()) {
                startActivity(
                    Intent(this, ProductsActivity::class.java)
                        .putExtra("SEARCH_QUERY", query)
                )
                searchView.setText("")
            }
        }

        searchView.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = searchView.text.toString().trim()
                if (query.isNotEmpty()) {
                    startActivity(
                        Intent(this, ProductsActivity::class.java)
                            .putExtra("SEARCH_QUERY", query)
                    )
                    searchView.setText("")
                }
                true
            } else false
        }
    }

    private fun setupPopularProducts() {
        val rvChips = findViewById<RecyclerView>(R.id.rvProductChips)
        chipAdapter = ProductChipAdapter(emptyList()) { product ->
            startActivity(
                Intent(this, ProductsActivity::class.java)
                    .putExtra("SEARCH_QUERY", product.name)
            )
        }
        rvChips.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvChips.adapter = chipAdapter

        viewModel.products.observe(this) { products ->
            if (!products.isNullOrEmpty()) {
                chipAdapter.updateProducts(products)
            }
        }
    }

    private fun setupActionButtons() {
        findViewById<LinearLayout>(R.id.llMisPedidos).setOnClickListener {
            startActivity(Intent(this, OrdersActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.llMercado).setOnClickListener {
            startActivity(Intent(this, ProductsActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.llPromociones).setOnClickListener {
            Toast.makeText(this, "Próximamente...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupCategoryCards() {
        findViewById<androidx.cardview.widget.CardView>(R.id.cardFrutas).setOnClickListener {
            openCategory("Frutas")
        }
        findViewById<androidx.cardview.widget.CardView>(R.id.cardVerduras).setOnClickListener {
            openCategory("Verduras")
        }
        findViewById<androidx.cardview.widget.CardView>(R.id.cardGranos).setOnClickListener {
            openCategory("Granos")
        }
        findViewById<androidx.cardview.widget.CardView>(R.id.cardRaices).setOnClickListener {
            openCategory("Raíces y tubérculos")
        }
        findViewById<androidx.cardview.widget.CardView>(R.id.cardHierbas).setOnClickListener {
            openCategory("Hierbas y condimentos")
        }
        findViewById<androidx.cardview.widget.CardView>(R.id.cardCultivos).setOnClickListener {
            openCategory("Cultivos comerciales")
        }
    }

    private fun setupCart() {
        findViewById<ImageView>(R.id.ivCart).setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
        }
    }

    private fun setupBannerCarousel() {
        val banners = listOf(
            Banner(R.drawable.ic_delivery, "Primer pedido", "ENVÍO GRATIS"),
            Banner(R.drawable.ic_yucas, "Yuca a 80 Centavos", "la libra"),
            Banner(R.drawable.ic_frutaspromo, "LUNES DE FRUTAS", "15% DE DESCUENTO"),
            Banner(R.drawable.ic_anuncio, "ANÚNCIATE AQUÍ", "Espacio publicitario")
        )

        val viewPager = findViewById<ViewPager2>(R.id.viewPagerBanners)
        viewPager.adapter = BannerAdapter(banners)

        val layoutIndicator = findViewById<LinearLayout>(R.id.layoutIndicator)
        val dots = arrayOfNulls<ImageView>(banners.size)
        for (i in dots.indices) {
            dots[i] = ImageView(this).apply {
                setImageResource(R.drawable.dot_inactive)
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.setMargins(8, 0, 8, 0) }
                layoutIndicator.addView(this, params)
            }
        }
        dots[0]?.setImageResource(R.drawable.dot_active)

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                dots.forEachIndexed { i, dot ->
                    dot?.setImageResource(
                        if (i == position) R.drawable.dot_active else R.drawable.dot_inactive
                    )
                }
            }
        })
    }

    private fun setupFooter() {
        findViewById<TextView>(R.id.tvSocio).setOnClickListener {
            Toast.makeText(this, "Conviértete en Socio - Próximamente", Toast.LENGTH_SHORT).show()
        }
        findViewById<TextView>(R.id.tvPrivacidad).setOnClickListener {
            Toast.makeText(this, "Política de Privacidad - Próximamente", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openCategory(categoryName: String) {
        startActivity(
            Intent(this, ProductsActivity::class.java)
                .putExtra("CATEGORY", categoryName)
        )
    }

    private fun goToLogin() {
        startActivity(
            Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        finish()
    }
}