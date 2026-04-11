package com.agroapp.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.agroapp.R
import com.agroapp.model.ActiveOrderResponse
import com.agroapp.model.Banner
import com.agroapp.network.AppBanner
import com.agroapp.network.RetrofitClient
import com.agroapp.network.SessionManager
import com.agroapp.viewmodel.ProductViewModel
import com.bumptech.glide.Glide
import com.google.android.material.navigation.NavigationView
import com.google.android.material.textview.MaterialTextView
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {

    private val viewModel: ProductViewModel by viewModels()
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var chipAdapter: ProductChipAdapter
    private lateinit var cardActiveOrder: CardView
    private lateinit var viewPager: ViewPager2

    private val autoScrollHandler = Handler(Looper.getMainLooper())
    private var currentBannerPage = 0
    private var bannerCount = 0
    private val AUTO_SCROLL_DELAY = 3500L
    private var loadedBanners: List<AppBanner> = emptyList()

    private val autoScrollRunnable = object : Runnable {
        override fun run() {
            if (bannerCount > 0) {
                currentBannerPage = (currentBannerPage + 1) % bannerCount
                viewPager.setCurrentItem(currentBannerPage, true)
                autoScrollHandler.postDelayed(this, AUTO_SCROLL_DELAY)
            }
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) Log.d("HomeActivity", "Permiso de notificaciones concedido")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        if (!SessionManager.isLoggedIn()) {
            goToLogin()
            return
        }

        requestNotificationPermission()
        initViews()
        setupDrawer()
        setupDireccion()
        setupSearch()
        setupPopularProducts()
        setupActionButtons()
        setupCategoryCards()
        setupCart()
        setupBannerCarousel()
        checkActiveOrder()
        loadStaticBanners()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun loadStaticBanners() {
        val ivBanner1 = findViewById<ImageView>(R.id.ivBanner1)
        val ivBanner2 = findViewById<ImageView>(R.id.ivBanner2)
        val ivBanner3 = findViewById<ImageView>(R.id.ivBanner3)
        val cardBanner1 = findViewById<CardView>(R.id.cardBanner1)
        val cardBanner2 = findViewById<CardView>(R.id.cardBanner2)
        val cardBanner3 = findViewById<CardView>(R.id.cardBanner3)

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getBanners()
                if (response.isSuccessful) {
                    val banners = response.body() ?: return@launch
                    loadedBanners = banners
                    banners.forEach { banner ->
                        val iv = when (banner.slot) { 1 -> ivBanner1; 2 -> ivBanner2; 3 -> ivBanner3; else -> null } ?: return@forEach
                        val card = when (banner.slot) { 1 -> cardBanner1; 2 -> cardBanner2; 3 -> cardBanner3; else -> null } ?: return@forEach
                        if (!banner.imageUrl.isNullOrEmpty()) {
                            Glide.with(this@HomeActivity).load(banner.imageUrl).centerCrop().into(iv)
                        }
                        card.setOnClickListener { handleBannerCardClick(banner) }
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeActivity", "Error cargando banners: ${e.message}")
            }
        }
    }

    private fun handleBannerCardClick(banner: AppBanner) {
        when {
            !banner.title.isNullOrEmpty() -> {
                startActivity(Intent(this, ProductsActivity::class.java).putExtra("SEARCH_QUERY", banner.title))
            }
            !banner.linkUrl.isNullOrEmpty() -> {
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(banner.linkUrl)))
                } catch (e: Exception) {
                    Toast.makeText(this, "No se pudo abrir el enlace", Toast.LENGTH_SHORT).show()
                }
            }
            else -> startActivity(Intent(this, ProductsActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        if (SessionManager.isLoggedIn()) {
            checkActiveOrder()
            loadStaticBanners()
            if (bannerCount > 0) {
                autoScrollHandler.removeCallbacks(autoScrollRunnable)
                autoScrollHandler.postDelayed(autoScrollRunnable, AUTO_SCROLL_DELAY)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        autoScrollHandler.removeCallbacks(autoScrollRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        autoScrollHandler.removeCallbacks(autoScrollRunnable)
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START, false)
        } else {
            moveTaskToBack(true)
        }
        super.onBackPressed()
    }

    private fun initViews() {
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        cardActiveOrder = findViewById(R.id.cardActiveOrder)
        viewPager = findViewById(R.id.viewPagerBanners)

        val headerView = navigationView.getHeaderView(0)
        val tvHeaderName = headerView.findViewById<MaterialTextView>(R.id.tvHeaderName)
        val tvHeaderEmail = headerView.findViewById<MaterialTextView>(R.id.tvHeaderEmail)

        tvHeaderName.text = SessionManager.getUserName()
        tvHeaderEmail.text = SessionManager.getUserEmail()
    }

    private fun checkActiveOrder() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.instance.getActiveOrder(token = SessionManager.getToken())
                if (response.isSuccessful && response.body() != null) {
                    val order = response.body()!!
                    if (order.driver_id != null) showActiveOrderBanner(order)
                    else cardActiveOrder.visibility = View.GONE
                } else {
                    cardActiveOrder.visibility = View.GONE
                }
            } catch (e: Exception) {
                Log.e("HomeActivity", "Error verificando pedido activo: ${e.message}")
                cardActiveOrder.visibility = View.GONE
            }
        }
    }

    private fun showActiveOrderBanner(order: ActiveOrderResponse) {
        cardActiveOrder.visibility = View.VISIBLE
        cardActiveOrder.setOnClickListener {
            val intent = Intent(this, TrackingActivity::class.java).apply {
                putExtra("order_id", order.id)
                putExtra("driver_id", order.driver_id)
                putExtra("order_total", order.total)
                putExtra("delivery_lat", order.delivery_lat ?: 0.0)
                putExtra("delivery_lng", order.delivery_lng ?: 0.0)
            }
            startActivity(intent)
        }
    }

    private fun setupDrawer() {
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        toolbar.setNavigationOnClickListener {
            if (!drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.openDrawer(GravityCompat.START, false)
            }
        }

        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> { }
                R.id.nav_profile -> startActivity(Intent(this, ProfileActivity::class.java))
                R.id.nav_orders -> startActivity(Intent(this, OrdersActivity::class.java))
                R.id.nav_cart -> startActivity(Intent(this, CartActivity::class.java))
                R.id.nav_logout -> { SessionManager.logout(); goToLogin() }
                R.id.nav_settings -> Toast.makeText(this, "Proximamente", Toast.LENGTH_SHORT).show()
            }
            drawerLayout.closeDrawer(GravityCompat.START, false)
            true
        }
    }

    private fun setupDireccion() {
        val tvDireccion = findViewById<TextView>(R.id.tvDireccion)
        val tvCambiarDireccion = findViewById<TextView>(R.id.tvCambiarDireccion)
        val direccion = SessionManager.getAddress()
        tvDireccion.text = if (direccion.isNotEmpty()) direccion else "Direccion no especificada"
        tvCambiarDireccion.setOnClickListener { startActivity(Intent(this, ProfileActivity::class.java)) }
    }

    private fun setupSearch() {
        val searchView = findViewById<AutoCompleteTextView>(R.id.searchView)
        viewModel.loadProducts()
        viewModel.products.observe(this) { products ->
            if (!products.isNullOrEmpty()) {
                searchView.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, products.map { it.name }))
            }
        }
        searchView.setOnItemClickListener { _, _, _, _ ->
            val query = searchView.text.toString().trim()
            if (query.isNotEmpty()) {
                startActivity(Intent(this, ProductsActivity::class.java).putExtra("SEARCH_QUERY", query))
                searchView.setText("")
            }
        }
        searchView.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = searchView.text.toString().trim()
                if (query.isNotEmpty()) {
                    startActivity(Intent(this, ProductsActivity::class.java).putExtra("SEARCH_QUERY", query))
                    searchView.setText("")
                }
                true
            } else false
        }
    }

    private fun setupPopularProducts() {
        val rvChips = findViewById<RecyclerView>(R.id.rvProductChips)
        chipAdapter = ProductChipAdapter(emptyList()) { product ->
            startActivity(Intent(this, ProductsActivity::class.java).putExtra("SEARCH_QUERY", product.name))
        }
        rvChips.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvChips.adapter = chipAdapter
        viewModel.products.observe(this) { products ->
            if (!products.isNullOrEmpty()) chipAdapter.updateProducts(products)
        }
    }

    private fun setupActionButtons() {
        findViewById<LinearLayout>(R.id.llMisPedidos).setOnClickListener { startActivity(Intent(this, OrdersActivity::class.java)) }
        findViewById<LinearLayout>(R.id.llMercado).setOnClickListener { startActivity(Intent(this, ProductsActivity::class.java)) }
        findViewById<LinearLayout>(R.id.llPromociones).setOnClickListener { Toast.makeText(this, "Proximamente...", Toast.LENGTH_SHORT).show() }
    }

    private fun setupCategoryCards() {
        findViewById<CardView>(R.id.cardFrutas).setOnClickListener { openCategory("Frutas") }
        findViewById<CardView>(R.id.cardVerduras).setOnClickListener { openCategory("Verduras") }
        findViewById<CardView>(R.id.cardGranos).setOnClickListener { openCategory("Granos") }
        findViewById<CardView>(R.id.cardRaices).setOnClickListener { openCategory("Raices y tuberculos") }
        findViewById<CardView>(R.id.cardHierbas).setOnClickListener { openCategory("Hierbas y condimentos") }
        findViewById<CardView>(R.id.cardCultivos).setOnClickListener { openCategory("Cultivos comerciales") }
    }

    private fun setupCart() {
        findViewById<ImageView>(R.id.ivCart).setOnClickListener { startActivity(Intent(this, CartActivity::class.java)) }
    }

    private fun setupBannerCarousel() {
        val banners = listOf(
            Banner(imageRes = R.drawable.banner_envio_gratis, title = "Envio Gratis", description = "En todos tus pedidos. Compra ahora!", destination = "mercado"),
            Banner(imageRes = R.drawable.banner_yappi, title = "Aceptamos YAPPI", description = "Paga facil y rapido con tu app favorita", destination = "mercado"),
            Banner(imageRes = R.drawable.banner_comparte, title = "Comparte Grun", description = "Invita a tus amigos y familiares", destination = "compartir")
        )

        bannerCount = banners.size
        viewPager.adapter = BannerAdapter(banners) { banner -> handleBannerClick(banner) }

        val layoutIndicator = findViewById<LinearLayout>(R.id.layoutIndicator)
        layoutIndicator.removeAllViews()

        val dots = arrayOfNulls<View>(banners.size)
        for (i in dots.indices) {
            val dot = View(this).apply {
                val params = LinearLayout.LayoutParams(
                    if (i == 0) 24.dpToPx() else 8.dpToPx(), 8.dpToPx()
                ).also { it.setMargins(4, 0, 4, 0) }
                layoutParams = params
                background = ContextCompat.getDrawable(this@HomeActivity, if (i == 0) R.drawable.dot_active else R.drawable.dot_inactive)
            }
            dots[i] = dot
            layoutIndicator.addView(dot)
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                currentBannerPage = position
                dots.forEachIndexed { i, dot ->
                    val isActive = i == position
                    dot?.layoutParams = (dot?.layoutParams as? LinearLayout.LayoutParams)?.also {
                        it.width = if (isActive) 24.dpToPx() else 8.dpToPx()
                    }
                    dot?.background = ContextCompat.getDrawable(this@HomeActivity, if (isActive) R.drawable.dot_active else R.drawable.dot_inactive)
                }
            }
            override fun onPageScrollStateChanged(state: Int) {
                if (state == ViewPager2.SCROLL_STATE_DRAGGING) {
                    autoScrollHandler.removeCallbacks(autoScrollRunnable)
                } else if (state == ViewPager2.SCROLL_STATE_IDLE) {
                    autoScrollHandler.removeCallbacks(autoScrollRunnable)
                    autoScrollHandler.postDelayed(autoScrollRunnable, AUTO_SCROLL_DELAY)
                }
            }
        })

        autoScrollHandler.postDelayed(autoScrollRunnable, AUTO_SCROLL_DELAY)
    }

    private fun handleBannerClick(banner: Banner) {
        when (banner.destination) {
            "mercado" -> startActivity(Intent(this, ProductsActivity::class.java))
            "compartir" -> {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "Descarga Grun y recibe tus productos frescos!")
                }
                startActivity(Intent.createChooser(shareIntent, "Compartir Grun"))
            }
            else -> {}
        }
    }

    private fun openCategory(categoryName: String) {
        startActivity(Intent(this, ProductsActivity::class.java).putExtra("CATEGORY", categoryName))
    }

    private fun goToLogin() {
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
}