package com.agroapp.ui

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.agroapp.R
import com.agroapp.model.*
import com.agroapp.network.AppBanner
import com.agroapp.network.RetrofitClient
import com.agroapp.network.SessionManager
import com.agroapp.network.YappiPendingOrder
import com.agroapp.ui.adapters.AdminPendingOrdersAdapter
import com.agroapp.ui.adapters.PaymentsAdminAdapter
import com.agroapp.ui.adapters.ProductsAdminAdapter
import com.agroapp.ui.adapters.YappiPendingAdapter
import com.agroapp.viewmodel.AdminViewModel
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.URL
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import javax.net.ssl.HttpsURLConnection

class AdminActivity : AppCompatActivity() {

    private lateinit var viewModel: AdminViewModel
    private lateinit var toolbar: Toolbar
    private lateinit var tabLayout: TabLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var fab: FloatingActionButton
    private lateinit var tvStats: TextView
    private lateinit var scrollStats: ScrollView
    private lateinit var progressBar: ProgressBar
    private lateinit var searchView: androidx.appcompat.widget.SearchView

    private lateinit var productsAdapter: ProductsAdminAdapter
    private lateinit var paymentsAdapter: PaymentsAdminAdapter
    private lateinit var yappiAdapter: YappiPendingAdapter
    private lateinit var pendingOrdersAdapter: AdminPendingOrdersAdapter

    private val formatter = NumberFormat.getCurrencyInstance(Locale.US)
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    private var selectedImageBase64: String? = null
    private var selectedImageBytes: ByteArray? = null
    private var currentImagePreview: ImageView? = null
    private var currentImageStatus: TextView? = null
    private var currentBannerGalleryCallback: ((ByteArray, String) -> Unit)? = null

    private val SUPABASE_URL = "https://eaozzabxruvqcrayejfk.supabase.co"
    private val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImVhb3p6YWJ4cnV2cWNyYXllamZrIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzMxMDg3MDcsImV4cCI6MjA4ODY4NDcwN30.fSxW1KtUAUfqv1vTdBOZsSbEXiF2DMPr1ENf9v022iM"

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            if (currentBannerGalleryCallback != null) processBannerImage(it)
            else processProductImage(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)
        initViews(); setupViewModel(); setupAdapters(); setupTabs(); setupObservers(); setupFab()
        viewModel.loadDashboardStats(); viewModel.loadCategories(); viewModel.loadDrivers(); setupSearch()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar); tabLayout = findViewById(R.id.tabLayout)
        recyclerView = findViewById(R.id.recyclerView); fab = findViewById(R.id.fab)
        tvStats = findViewById(R.id.tvStats); scrollStats = findViewById(R.id.scrollStats)
        progressBar = findViewById(R.id.progressBar); searchView = findViewById(R.id.searchView)
        setSupportActionBar(toolbar); supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Panel Administrador"
        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun setupViewModel() { viewModel = ViewModelProvider(this)[AdminViewModel::class.java] }

    private fun setupAdapters() {
        productsAdapter = ProductsAdminAdapter(
            onEditClick = { showEditProductDialog(it) },
            onStockClick = { showUpdateStockDialog(it) },
            onDeleteClick = { showDeleteConfirmDialog(it) }
        )
        paymentsAdapter = PaymentsAdminAdapter { showProcessPaymentDialog(it) }
        yappiAdapter = YappiPendingAdapter(onApprove = { showApproveYappiDialog(it) }, onReject = { showRejectYappiDialog(it) })
        pendingOrdersAdapter = AdminPendingOrdersAdapter(onAssignDriver = { order -> showAssignDriverDialog(order) })
    }

    private fun setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Dashboard"))
        tabLayout.addTab(tabLayout.newTab().setText("Productos"))
        tabLayout.addTab(tabLayout.newTab().setText("Pagos"))
        tabLayout.addTab(tabLayout.newTab().setText("YAPPI"))
        tabLayout.addTab(tabLayout.newTab().setText("Pedidos"))
        tabLayout.addTab(tabLayout.newTab().setText("Logs"))
        tabLayout.addTab(tabLayout.newTab().setText("Banners"))

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                hideBannersScroll()
                when (tab?.position) {
                    0 -> { scrollStats.visibility = View.VISIBLE; recyclerView.visibility = View.GONE; searchView.visibility = View.GONE; fab.visibility = View.GONE; viewModel.loadDashboardStats() }
                    1 -> { scrollStats.visibility = View.GONE; recyclerView.visibility = View.VISIBLE; searchView.visibility = View.VISIBLE; fab.visibility = View.VISIBLE; recyclerView.adapter = productsAdapter; searchView.setQuery("", false); viewModel.loadProducts() }
                    2 -> { scrollStats.visibility = View.GONE; recyclerView.visibility = View.VISIBLE; searchView.visibility = View.GONE; fab.visibility = View.GONE; recyclerView.adapter = paymentsAdapter; viewModel.loadDriverPayments() }
                    3 -> { scrollStats.visibility = View.GONE; recyclerView.visibility = View.VISIBLE; searchView.visibility = View.GONE; fab.visibility = View.GONE; recyclerView.adapter = yappiAdapter; loadYappiPendingOrders() }
                    4 -> { scrollStats.visibility = View.GONE; recyclerView.visibility = View.VISIBLE; searchView.visibility = View.GONE; fab.visibility = View.GONE; recyclerView.adapter = pendingOrdersAdapter; loadAdminPendingOrders() }
                    5 -> { scrollStats.visibility = View.GONE; recyclerView.visibility = View.VISIBLE; searchView.visibility = View.GONE; fab.visibility = View.GONE; viewModel.loadInventoryLogs() }
                    6 -> { scrollStats.visibility = View.GONE; recyclerView.visibility = View.GONE; searchView.visibility = View.GONE; fab.visibility = View.GONE; loadAndShowBanners() }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun hideBannersScroll() {
        val parent = recyclerView.parent as? android.view.ViewGroup ?: return
        parent.findViewWithTag<View>("banners_scroll")?.let {
            it.visibility = View.GONE
        }
    }

    private fun setupSearch() {
        searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false
            override fun onQueryTextChange(newText: String?): Boolean { productsAdapter.filter(newText ?: ""); return true }
        })
    }

    // ==================== BANNERS ====================

    private fun loadAndShowBanners() {
        progressBar.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.getAdminBanners(SessionManager.getToken())
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    if (response.isSuccessful) showBannersUI(response.body() ?: emptyList())
                    else Toast.makeText(this@AdminActivity, "Error cargando banners", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { progressBar.visibility = View.GONE; Toast.makeText(this@AdminActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    private fun showBannersUI(banners: List<AppBanner>) {
        val parent = recyclerView.parent as? android.view.ViewGroup ?: return

        // Reusar o crear scroll
        var scrollView = parent.findViewWithTag<ScrollView>("banners_scroll")
        if (scrollView == null) {
            scrollView = ScrollView(this).apply {
                tag = "banners_scroll"
                layoutParams = android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                )
            }
            parent.addView(scrollView)
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        scrollView.removeAllViews()
        scrollView.addView(container)
        scrollView.visibility = View.VISIBLE

        banners.forEach { banner ->
            // Título del banner
            container.addView(TextView(this).apply {
                text = "Banner ${banner.slot}"
                textSize = 16f
                setTextColor(android.graphics.Color.parseColor("#2E7D32"))
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setPadding(0, 0, 0, 8)
            })

            // Preview imagen
            val ivPreview = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 360)
                scaleType = ImageView.ScaleType.CENTER_CROP
                setBackgroundColor(android.graphics.Color.parseColor("#F5F5F5"))
            }
            if (!banner.imageUrl.isNullOrEmpty()) {
                Glide.with(this).load(banner.imageUrl).centerCrop().into(ivPreview)
            }
            container.addView(ivPreview)

            // Campo nombre
            val etTitle = EditText(this).apply {
                setText(banner.title ?: "")
                hint = "Nombre del banner"
                textSize = 14f
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = 12 }
            }
            container.addView(etTitle)

            // Campo precio
            val etPrice = EditText(this).apply {
                setText(if (banner.price != null && banner.price > 0) banner.price.toString() else "")
                hint = "Precio (opcional)"
                textSize = 14f
                inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = 8 }
            }
            container.addView(etPrice)

            // Campo product_id
            val etProductId = EditText(this).apply {
                setText(if (banner.productId != null && banner.productId > 0) banner.productId.toString() else "")
                hint = "ID del producto (para link al carrito)"
                textSize = 14f
                inputType = android.text.InputType.TYPE_CLASS_NUMBER
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = 8 }
            }
            container.addView(etProductId)

            // Botón cambiar imagen
            val btnChange = Button(this).apply {
                text = "📷 Cambiar imagen"
                setBackgroundColor(android.graphics.Color.parseColor("#2E7D32"))
                setTextColor(android.graphics.Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = 8 }
            }
            btnChange.setOnClickListener {
                currentBannerGalleryCallback = { imageBytes, _ ->
                    uploadBannerData(banner.id, imageBytes, etTitle.text.toString(), etPrice.text.toString(), etProductId.text.toString(), ivPreview)
                    currentBannerGalleryCallback = null
                }
                galleryLauncher.launch("image/*")
            }
            container.addView(btnChange)

            // Botón guardar datos (sin cambiar imagen)
            val btnSave = Button(this).apply {
                text = "💾 Guardar nombre/precio/producto"
                setBackgroundColor(android.graphics.Color.parseColor("#1565C0"))
                setTextColor(android.graphics.Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = 8 }
            }
            btnSave.setOnClickListener {
                uploadBannerData(banner.id, null, etTitle.text.toString(), etPrice.text.toString(), etProductId.text.toString(), ivPreview)
            }
            container.addView(btnSave)

            // Divider
            container.addView(View(this).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1).apply { topMargin = 24; bottomMargin = 24 }
                setBackgroundColor(android.graphics.Color.parseColor("#E0E0E0"))
            })
        }
    }

    private fun processBannerImage(uri: Uri) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream); inputStream?.close()
                val resized = resizeBitmap(bitmap, 1200)
                val outputStream = ByteArrayOutputStream()
                resized.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                val imageBytes = outputStream.toByteArray()
                withContext(Dispatchers.Main) { currentBannerGalleryCallback?.invoke(imageBytes, "image/jpeg") }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { Toast.makeText(this@AdminActivity, "Error al leer imagen: ${e.message}", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    private fun uploadBannerData(bannerId: Int, imageBytes: ByteArray?, title: String, price: String, productId: String, ivPreview: ImageView) {
        progressBar.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val body = mutableMapOf<String, String>()
                if (imageBytes != null) {
                    body["imageBase64"] = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
                    body["mimeType"] = "image/jpeg"
                }
                if (title.isNotEmpty()) body["title"] = title
                if (price.isNotEmpty()) body["price"] = price
                if (productId.isNotEmpty()) body["product_id"] = productId

                val response = RetrofitClient.instance.updateBanner(
                    token = SessionManager.getToken(),
                    bannerId = bannerId,
                    body = body
                )
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    if (response.isSuccessful) {
                        Toast.makeText(this@AdminActivity, "✅ Banner actualizado", Toast.LENGTH_SHORT).show()
                        if (imageBytes != null) {
                            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                            Glide.with(this@AdminActivity).load(bitmap).centerCrop().into(ivPreview)
                        }
                    } else {
                        Toast.makeText(this@AdminActivity, "Error al actualizar banner", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { progressBar.visibility = View.GONE; Toast.makeText(this@AdminActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show() }
            }
        }
    }

    // ==================== IMAGEN PRODUCTOS ====================

    private fun processProductImage(uri: Uri) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream); inputStream?.close()
                val resized = resizeBitmap(bitmap, 800)
                val outputStream = ByteArrayOutputStream()
                resized.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
                selectedImageBytes = outputStream.toByteArray()
                selectedImageBase64 = Base64.encodeToString(selectedImageBytes, Base64.NO_WRAP)
                withContext(Dispatchers.Main) {
                    currentImagePreview?.let { Glide.with(this@AdminActivity).load(bitmap).centerCrop().into(it) }
                    currentImageStatus?.text = "✅ Imagen seleccionada"
                    currentImageStatus?.setTextColor(android.graphics.Color.parseColor("#2E7D32"))
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    currentImageStatus?.text = "❌ Error al cargar imagen"
                    Toast.makeText(this@AdminActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private suspend fun uploadProductImage(imageBytes: ByteArray): String? {
        return try {
            val fileName = "product_${System.currentTimeMillis()}.jpg"
            val url = URL("$SUPABASE_URL/storage/v1/object/product-images/$fileName")
            val connection = url.openConnection() as HttpsURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Authorization", "Bearer $SUPABASE_ANON_KEY")
            connection.setRequestProperty("Content-Type", "image/jpeg")
            connection.setRequestProperty("x-upsert", "true")
            connection.doOutput = true; connection.connectTimeout = 30000; connection.readTimeout = 30000
            connection.outputStream.write(imageBytes); connection.outputStream.flush()
            if (connection.responseCode in 200..299) "$SUPABASE_URL/storage/v1/object/public/product-images/$fileName" else null
        } catch (e: Exception) { null }
    }

    private fun resizeBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
        return if (bitmap.width > bitmap.height) Bitmap.createScaledBitmap(bitmap, maxSize, (maxSize / ratio).toInt(), true)
        else Bitmap.createScaledBitmap(bitmap, (maxSize * ratio).toInt(), maxSize, true)
    }

    // ==================== YAPPI ====================

    private fun loadYappiPendingOrders() {
        progressBar.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.getYappiPendingOrders(SessionManager.getToken())
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    if (response.isSuccessful) yappiAdapter.submitList(response.body() ?: emptyList())
                    else Toast.makeText(this@AdminActivity, "Error cargando YAPPI", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) { withContext(Dispatchers.Main) { progressBar.visibility = View.GONE } }
        }
    }

    // ==================== PEDIDOS PENDIENTES ====================

    private fun loadAdminPendingOrders() {
        progressBar.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.getAdminPendingOrders(SessionManager.getToken())
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    if (response.isSuccessful) {
                        val orders = response.body() ?: emptyList()
                        pendingOrdersAdapter.submitList(orders)
                        if (orders.isEmpty()) Toast.makeText(this@AdminActivity, "No hay pedidos pendientes", Toast.LENGTH_SHORT).show()
                    } else Toast.makeText(this@AdminActivity, "Error cargando pedidos", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) { withContext(Dispatchers.Main) { progressBar.visibility = View.GONE; Toast.makeText(this@AdminActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show() } }
        }
    }

    // ==================== ASIGNACION MANUAL DRIVER ====================

    private fun showAssignDriverDialog(order: AdminPendingOrder) {
        val drivers = viewModel.drivers.value
        if (drivers.isNullOrEmpty()) { Toast.makeText(this, "No hay drivers disponibles", Toast.LENGTH_SHORT).show(); return }
        val driverNames = drivers.map { "${it.fullName} (${it.email})" }.toTypedArray()
        var selectedIndex = 0
        AlertDialog.Builder(this)
            .setTitle("🚚 Asignar driver")
            .setMessage("Pedido #${order.id.take(8).uppercase()}\nCliente: ${order.customerName}")
            .setSingleChoiceItems(driverNames, 0) { _, which -> selectedIndex = which }
            .setPositiveButton("Asignar") { _, _ -> assignDriverToOrder(order, drivers[selectedIndex]) }
            .setNegativeButton("Cancelar", null).show()
    }

    private fun assignDriverToOrder(order: AdminPendingOrder, driver: User) {
        progressBar.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.assignDriverToOrder(SessionManager.getToken(), order.id, AssignDriverRequest(driverId = driver.id))
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    if (response.isSuccessful) { Toast.makeText(this@AdminActivity, "✅ Pedido asignado a ${driver.fullName}", Toast.LENGTH_LONG).show(); loadAdminPendingOrders() }
                    else Toast.makeText(this@AdminActivity, "Error: ${response.errorBody()?.string()}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) { withContext(Dispatchers.Main) { progressBar.visibility = View.GONE; Toast.makeText(this@AdminActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show() } }
        }
    }

    private fun showApproveYappiDialog(order: YappiPendingOrder) {
        AlertDialog.Builder(this).setTitle("✅ Aprobar pago YAPPI")
            .setMessage("Cliente: ${order.customer_name}\nMonto: ${formatter.format(order.total_amount)}\nReferencia: ${order.reference_code}\n\n¿Confirmas que recibiste este pago?")
            .setPositiveButton("Sí, aprobar") { _, _ -> approveYappiPayment(order) }
            .setNegativeButton("Cancelar", null).show()
    }

    private fun showRejectYappiDialog(order: YappiPendingOrder) {
        val input = EditText(this).apply { hint = "Motivo del rechazo (opcional)"; setPadding(48, 32, 48, 32) }
        AlertDialog.Builder(this).setTitle("❌ Rechazar pago YAPPI")
            .setMessage("Cliente: ${order.customer_name}\nMonto: ${formatter.format(order.total_amount)}\nRef: ${order.reference_code}")
            .setView(input).setPositiveButton("Rechazar") { _, _ -> rejectYappiPayment(order, input.text.toString().trim()) }
            .setNegativeButton("Cancelar", null).show()
    }

    private fun approveYappiPayment(order: YappiPendingOrder) {
        progressBar.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.approveYappiPayment(SessionManager.getToken(), order.id)
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    if (response.isSuccessful) { Toast.makeText(this@AdminActivity, "✅ Pago aprobado", Toast.LENGTH_LONG).show(); loadYappiPendingOrders() }
                    else Toast.makeText(this@AdminActivity, "Error al aprobar", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) { withContext(Dispatchers.Main) { progressBar.visibility = View.GONE } }
        }
    }

    private fun rejectYappiPayment(order: YappiPendingOrder, reason: String) {
        progressBar.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.rejectYappiPayment(SessionManager.getToken(), order.id, com.agroapp.network.RejectYappiRequest(reason.ifEmpty { "Pago no verificado" }))
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    if (response.isSuccessful) { Toast.makeText(this@AdminActivity, "❌ Pago rechazado", Toast.LENGTH_LONG).show(); loadYappiPendingOrders() }
                    else Toast.makeText(this@AdminActivity, "Error al rechazar", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) { withContext(Dispatchers.Main) { progressBar.visibility = View.GONE } }
        }
    }

    // ==================== OBSERVERS ====================

    private fun setupObservers() {
        viewModel.loading.observe(this, Observer { progressBar.visibility = if (it) View.VISIBLE else View.GONE })
        viewModel.message.observe(this, Observer { it?.let { msg -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show(); viewModel.clearMessage() } })
        viewModel.dashboardStats.observe(this, Observer { stats ->
            if (stats != null) tvStats.text = buildString {
                appendLine("📊 DASHBOARD"); appendLine()
                appendLine("📦 Productos totales: ${stats.totalProducts}")
                appendLine("⚠️ Stock bajo: ${stats.lowStockProducts}")
                appendLine("🚫 Agotados: ${stats.outOfStockProducts}"); appendLine()
                appendLine("📅 Pedidos hoy: ${stats.totalOrdersToday}")
                appendLine("💰 Ventas hoy: ${formatter.format(stats.totalRevenueToday)}"); appendLine()
                appendLine("📆 Pedidos semana: ${stats.totalOrdersWeek}")
                appendLine("💵 Ventas semana: ${formatter.format(stats.totalRevenueWeek)}"); appendLine()
                appendLine("👥 Repartidores: ${stats.totalDrivers}")
                appendLine("🚚 Activos: ${stats.activeDrivers}")
                appendLine("💸 Pagos pendientes: ${formatter.format(stats.pendingPayments)}"); appendLine()
                if ((stats.pendingYappiApprovals ?: 0) > 0) appendLine("⚠️ YAPPI por aprobar: ${stats.pendingYappiApprovals}")
            }
        })
        viewModel.products.observe(this, Observer { productsAdapter.submitList(it ?: emptyList()) })
        viewModel.driverPayments.observe(this, Observer { paymentsAdapter.submitList(it ?: emptyList()) })
    }

    private fun setupFab() { fab.setOnClickListener { showCreateProductDialog() } }

    // ==================== PRODUCTOS ====================

    private fun showCreateProductDialog() {
        selectedImageBase64 = null; selectedImageBytes = null; currentBannerGalleryCallback = null
        val dialogView = layoutInflater.inflate(R.layout.dialog_product, null)
        val etName = dialogView.findViewById<EditText>(R.id.etProductName)
        val etDescription = dialogView.findViewById<EditText>(R.id.etProductDescription)
        val etPrice = dialogView.findViewById<EditText>(R.id.etProductPrice)
        val etUnit = dialogView.findViewById<EditText>(R.id.etProductUnit)
        val etStock = dialogView.findViewById<EditText>(R.id.etProductStock)
        val etMinStock = dialogView.findViewById<EditText>(R.id.etMinStock)
        val spinnerCategory = dialogView.findViewById<Spinner>(R.id.spinnerCategory)
        val btnPickImage = dialogView.findViewById<Button>(R.id.btnPickImage)
        val ivPreview = dialogView.findViewById<ImageView>(R.id.ivProductImagePreview)
        val tvImageStatus = dialogView.findViewById<TextView>(R.id.tvImageStatus)
        currentImagePreview = ivPreview; currentImageStatus = tvImageStatus
        btnPickImage.setOnClickListener { galleryLauncher.launch("image/*") }
        viewModel.categories.observe(this, Observer { categories ->
            spinnerCategory.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories?.map { it.name } ?: emptyList()).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
        })
        AlertDialog.Builder(this).setTitle("Nuevo Producto").setView(dialogView)
            .setPositiveButton("Crear") { _, _ ->
                val name = etName.text.toString().trim(); val price = etPrice.text.toString().toDoubleOrNull() ?: 0.0
                if (name.isEmpty() || price <= 0) { Toast.makeText(this, "Completa nombre y precio", Toast.LENGTH_SHORT).show(); return@setPositiveButton }
                progressBar.visibility = View.VISIBLE
                CoroutineScope(Dispatchers.IO).launch {
                    val imageUrl = selectedImageBytes?.let { uploadProductImage(it) }
                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE
                        viewModel.createProduct(CreateProductRequest(name = name, description = etDescription.text.toString().trim(), price = price, unit = etUnit.text.toString().trim(), categoryId = viewModel.categories.value?.getOrNull(spinnerCategory.selectedItemPosition)?.id ?: 1, stock = etStock.text.toString().toDoubleOrNull() ?: 0.0, minStock = etMinStock.text.toString().toDoubleOrNull() ?: 0.0, imageUrl = imageUrl))
                        selectedImageBase64 = null; selectedImageBytes = null
                    }
                }
            }
            .setNegativeButton("Cancelar") { _, _ -> selectedImageBase64 = null; selectedImageBytes = null }.show()
    }

    private fun showEditProductDialog(product: ProductWithInventory) {
        selectedImageBase64 = null; selectedImageBytes = null; currentBannerGalleryCallback = null
        val dialogView = layoutInflater.inflate(R.layout.dialog_product, null)
        val etName = dialogView.findViewById<EditText>(R.id.etProductName)
        val etDescription = dialogView.findViewById<EditText>(R.id.etProductDescription)
        val etPrice = dialogView.findViewById<EditText>(R.id.etProductPrice)
        val etUnit = dialogView.findViewById<EditText>(R.id.etProductUnit)
        val etStock = dialogView.findViewById<EditText>(R.id.etProductStock)
        val etMinStock = dialogView.findViewById<EditText>(R.id.etMinStock)
        val spinnerCategory = dialogView.findViewById<Spinner>(R.id.spinnerCategory)
        val btnPickImage = dialogView.findViewById<Button>(R.id.btnPickImage)
        val ivPreview = dialogView.findViewById<ImageView>(R.id.ivProductImagePreview)
        val tvImageStatus = dialogView.findViewById<TextView>(R.id.tvImageStatus)
        currentImagePreview = ivPreview; currentImageStatus = tvImageStatus
        etName.setText(product.name); etDescription.setText(product.description ?: "")
        etPrice.setText(product.price.toString()); etUnit.setText(product.unit)
        etStock.setText((product.stock ?: 0.0).toString()); etMinStock.setText((product.minStock ?: 0.0).toString())
        if (!product.imageUrl.isNullOrEmpty()) { Glide.with(this).load(product.imageUrl).centerCrop().into(ivPreview); tvImageStatus.text = "Imagen actual cargada" }
        btnPickImage.setOnClickListener { galleryLauncher.launch("image/*") }
        viewModel.categories.observe(this, Observer { categories ->
            spinnerCategory.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories?.map { it.name } ?: emptyList()).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }
            spinnerCategory.setSelection(categories?.indexOfFirst { it.id == product.categoryId } ?: 0)
        })
        AlertDialog.Builder(this).setTitle("Editar Producto").setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                progressBar.visibility = View.VISIBLE
                CoroutineScope(Dispatchers.IO).launch {
                    val newImageUrl = selectedImageBytes?.let { uploadProductImage(it) }
                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE
                        viewModel.updateProduct(product.id, UpdateProductRequest(name = etName.text.toString().takeIf { it != product.name }, description = etDescription.text.toString().takeIf { it != product.description }, price = etPrice.text.toString().toDoubleOrNull()?.takeIf { it != product.price }, unit = etUnit.text.toString().takeIf { it != product.unit }, stock = etStock.text.toString().toDoubleOrNull()?.takeIf { it != product.stock }, minStock = etMinStock.text.toString().toDoubleOrNull()?.takeIf { it != product.minStock }, categoryId = viewModel.categories.value?.getOrNull(spinnerCategory.selectedItemPosition)?.id?.takeIf { it != product.categoryId }, imageUrl = newImageUrl ?: product.imageUrl))
                        selectedImageBase64 = null; selectedImageBytes = null
                    }
                }
            }
            .setNegativeButton("Cancelar") { _, _ -> selectedImageBase64 = null; selectedImageBytes = null }.show()
    }

    private fun showUpdateStockDialog(product: ProductWithInventory) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_stock, null)
        val tvProductName = dialogView.findViewById<TextView>(R.id.tvStockProductName)
        val tvCurrentStock = dialogView.findViewById<TextView>(R.id.tvCurrentStock)
        val etQuantity = dialogView.findViewById<EditText>(R.id.etStockQuantity)
        val radioAdd = dialogView.findViewById<RadioButton>(R.id.radioAdd)
        val radioSubtract = dialogView.findViewById<RadioButton>(R.id.radioSubtract)
        val radioSet = dialogView.findViewById<RadioButton>(R.id.radioSet)
        val layoutAdd = dialogView.findViewById<android.widget.LinearLayout>(R.id.layoutAdd)
        val layoutSubtract = dialogView.findViewById<android.widget.LinearLayout>(R.id.layoutSubtract)
        val layoutSet = dialogView.findViewById<android.widget.LinearLayout>(R.id.layoutSet)
        val etNotes = dialogView.findViewById<EditText>(R.id.etStockNotes)
        tvProductName.text = product.name
        tvCurrentStock.text = "Stock actual: ${if ((product.stock ?: 0.0) % 1 == 0.0) (product.stock ?: 0.0).toInt().toString() else "%.1f".format(product.stock ?: 0.0)} ${product.unit}"
        fun updateSelection(s: String) { layoutAdd.alpha = if (s == "add") 1f else 0.4f; layoutSubtract.alpha = if (s == "subtract") 1f else 0.4f; layoutSet.alpha = if (s == "set") 1f else 0.4f; radioAdd.isChecked = s == "add"; radioSubtract.isChecked = s == "subtract"; radioSet.isChecked = s == "set" }
        updateSelection("set"); layoutAdd.setOnClickListener { updateSelection("add") }; layoutSubtract.setOnClickListener { updateSelection("subtract") }; layoutSet.setOnClickListener { updateSelection("set") }
        AlertDialog.Builder(this).setTitle("Actualizar Stock").setView(dialogView)
            .setPositiveButton("Actualizar") { _, _ -> viewModel.updateStock(product.id, etQuantity.text.toString().toDoubleOrNull() ?: 0.0, when { radioAdd.isChecked -> "add"; radioSubtract.isChecked -> "subtract"; else -> "set" }, etNotes.text.toString()) }
            .setNegativeButton("Cancelar", null).show()
    }

    private fun showDeleteConfirmDialog(product: ProductWithInventory) {
        AlertDialog.Builder(this).setTitle("Eliminar Producto").setMessage("¿Estás seguro de eliminar ${product.name}?")
            .setPositiveButton("Eliminar") { _, _ -> viewModel.deleteProduct(product.id) }.setNegativeButton("Cancelar", null).show()
    }

    private fun showProcessPaymentDialog(payment: DriverPayment) {
        AlertDialog.Builder(this).setTitle("Procesar Pago")
            .setMessage(buildString {
                appendLine("Conductor: ${payment.driverName}"); appendLine("Semana: ${payment.weekStart} - ${payment.weekEnd}")
                appendLine("Pedidos: ${payment.totalOrders}"); appendLine("Pago base: ${formatter.format(payment.totalBasePayment)}")
                appendLine("Propinas: ${formatter.format(payment.totalTips)}"); appendLine("Comisión: ${formatter.format(payment.platformCommission)}")
                appendLine("Neto a pagar: ${formatter.format(payment.netAmount)}"); appendLine(); appendLine("¿Marcar como pagado?")
            })
            .setPositiveButton("Marcar como Pagado") { _, _ -> viewModel.processDriverPayment(payment.id, "paid") }
            .setNegativeButton("Cancelar", null).show()
    }

    // ==================== MENU ====================

    override fun onCreateOptionsMenu(menu: Menu?): Boolean { menuInflater.inflate(R.menu.admin_menu, menu); return true }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.action_refresh -> when (tabLayout.selectedTabPosition) {
                0 -> viewModel.loadDashboardStats(); 1 -> viewModel.loadProducts()
                2 -> viewModel.loadDriverPayments(); 3 -> loadYappiPendingOrders()
                4 -> loadAdminPendingOrders(); 5 -> viewModel.loadInventoryLogs()
                6 -> loadAndShowBanners()
            }
            R.id.action_calculate_payments -> showCalculatePaymentsDialog()
            R.id.action_logout -> { SessionManager.logout(); startActivity(Intent(this, LoginActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK }); finish() }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() { moveTaskToBack(true) }

    private fun showCalculatePaymentsDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_calculate_payment, null)
        val spinnerDriver = dialogView.findViewById<Spinner>(R.id.spinnerDriver)
        val etWeekStart = dialogView.findViewById<EditText>(R.id.etWeekStart)
        viewModel.drivers.observe(this, Observer { drivers -> spinnerDriver.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, drivers?.map { "${it.fullName} (${it.email})" } ?: emptyList()).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) } })
        etWeekStart.setText(getCurrentWeekStart())
        AlertDialog.Builder(this).setTitle("Calcular Pago").setView(dialogView)
            .setPositiveButton("Calcular") { _, _ ->
                val driverId = viewModel.drivers.value?.getOrNull(spinnerDriver.selectedItemPosition)?.id
                val weekStart = etWeekStart.text.toString()
                if (driverId != null && weekStart.isNotEmpty()) viewModel.calculateDriverPayment(driverId, weekStart)
                else Toast.makeText(this, "Selecciona un conductor y una fecha", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null).show()
    }

    private fun getCurrentWeekStart(): String {
        val calendar = Calendar.getInstance(); calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY); return dateFormat.format(calendar.time)
    }
}