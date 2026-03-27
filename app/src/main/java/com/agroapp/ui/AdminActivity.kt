package com.agroapp.ui

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.agroapp.R
import com.agroapp.model.*
import com.agroapp.network.SessionManager
import com.agroapp.ui.adapters.PaymentsAdminAdapter
import com.agroapp.ui.adapters.ProductsAdminAdapter
import com.agroapp.viewmodel.AdminViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class AdminActivity : AppCompatActivity() {

    private lateinit var viewModel: AdminViewModel
    private lateinit var toolbar: Toolbar
    private lateinit var tabLayout: TabLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var fab: FloatingActionButton
    private lateinit var tvStats: TextView
    private lateinit var scrollStats: ScrollView
    private lateinit var progressBar: ProgressBar

    private lateinit var productsAdapter: ProductsAdminAdapter
    private lateinit var paymentsAdapter: PaymentsAdminAdapter

    private val formatter = NumberFormat.getCurrencyInstance(Locale.US)
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        initViews()
        setupViewModel()
        setupAdapters()
        setupTabs()
        setupObservers()
        setupFab()

        viewModel.loadDashboardStats()
        viewModel.loadCategories()
        viewModel.loadDrivers()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        tabLayout = findViewById(R.id.tabLayout)
        recyclerView = findViewById(R.id.recyclerView)
        fab = findViewById(R.id.fab)
        tvStats = findViewById(R.id.tvStats)
        scrollStats = findViewById(R.id.scrollStats)
        progressBar = findViewById(R.id.progressBar)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Panel Administrador"

        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[AdminViewModel::class.java]
    }

    private fun setupAdapters() {
        productsAdapter = ProductsAdminAdapter(
            onEditClick = { product -> showEditProductDialog(product) },
            onStockClick = { product -> showUpdateStockDialog(product) },
            onDeleteClick = { product -> showDeleteConfirmDialog(product) }
        )

        paymentsAdapter = PaymentsAdminAdapter { payment ->
            showProcessPaymentDialog(payment)
        }
    }

    private fun setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Dashboard"))
        tabLayout.addTab(tabLayout.newTab().setText("Productos"))
        tabLayout.addTab(tabLayout.newTab().setText("Inventario"))
        tabLayout.addTab(tabLayout.newTab().setText("Pagos"))
        tabLayout.addTab(tabLayout.newTab().setText("Logs"))

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        scrollStats.visibility = View.VISIBLE
                        recyclerView.visibility = View.GONE
                        fab.visibility = View.GONE
                        viewModel.loadDashboardStats()
                    }
                    1 -> {
                        scrollStats.visibility = View.GONE
                        recyclerView.visibility = View.VISIBLE
                        fab.visibility = View.VISIBLE
                        recyclerView.adapter = productsAdapter
                        viewModel.loadProducts()
                    }
                    2 -> {
                        scrollStats.visibility = View.GONE
                        recyclerView.visibility = View.VISIBLE
                        fab.visibility = View.GONE
                        recyclerView.adapter = productsAdapter
                        viewModel.loadProducts(lowStock = false)  // ← CORREGIDO: muestra todos los productos
                    }
                    3 -> {
                        scrollStats.visibility = View.GONE
                        recyclerView.visibility = View.VISIBLE
                        fab.visibility = View.GONE
                        recyclerView.adapter = paymentsAdapter
                        viewModel.loadDriverPayments()
                    }
                    4 -> {
                        scrollStats.visibility = View.GONE
                        recyclerView.visibility = View.VISIBLE
                        fab.visibility = View.GONE
                        viewModel.loadInventoryLogs()
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupObservers() {
        viewModel.loading.observe(this, Observer { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        })

        viewModel.message.observe(this, Observer { message ->
            message?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                viewModel.clearMessage()
            }
        })

        viewModel.dashboardStats.observe(this, Observer { stats ->
            if (stats != null) {
                tvStats.text = buildString {
                    appendLine("📊 DASHBOARD")
                    appendLine()
                    appendLine("📦 Productos totales: ${stats.totalProducts}")
                    appendLine("⚠️ Stock bajo: ${stats.lowStockProducts}")
                    appendLine("🚫 Agotados: ${stats.outOfStockProducts}")
                    appendLine()
                    appendLine("📅 Pedidos hoy: ${stats.totalOrdersToday}")
                    appendLine("💰 Ventas hoy: ${formatter.format(stats.totalRevenueToday)}")
                    appendLine()
                    appendLine("📆 Pedidos semana: ${stats.totalOrdersWeek}")
                    appendLine("💵 Ventas semana: ${formatter.format(stats.totalRevenueWeek)}")
                    appendLine()
                    appendLine("👥 Repartidores: ${stats.totalDrivers}")
                    appendLine("🚚 Activos: ${stats.activeDrivers}")
                    appendLine("💸 Pagos pendientes: ${formatter.format(stats.pendingPayments)}")
                }
            }
        })

        viewModel.products.observe(this, Observer { products ->
            productsAdapter.submitList(products ?: emptyList())
        })

        viewModel.driverPayments.observe(this, Observer { payments ->
            paymentsAdapter.submitList(payments ?: emptyList())
        })
    }

    private fun setupFab() {
        fab.setOnClickListener {
            showCreateProductDialog()
        }
    }

    private fun showCreateProductDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_product, null)
        val etName = dialogView.findViewById<EditText>(R.id.etProductName)
        val etDescription = dialogView.findViewById<EditText>(R.id.etProductDescription)
        val etPrice = dialogView.findViewById<EditText>(R.id.etProductPrice)
        val etUnit = dialogView.findViewById<EditText>(R.id.etProductUnit)
        val etStock = dialogView.findViewById<EditText>(R.id.etProductStock)
        val etMinStock = dialogView.findViewById<EditText>(R.id.etMinStock)
        val spinnerCategory = dialogView.findViewById<Spinner>(R.id.spinnerCategory)

        viewModel.categories.observe(this, Observer { categories ->
            val categoryNames = categories?.map { it.name } ?: emptyList()
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categoryNames)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerCategory.adapter = adapter
        })

        AlertDialog.Builder(this)
            .setTitle("Nuevo Producto")
            .setView(dialogView)
            .setPositiveButton("Crear") { _, _ ->
                val name = etName.text.toString()
                val description = etDescription.text.toString()
                val price = etPrice.text.toString().toDoubleOrNull() ?: 0.0
                val unit = etUnit.text.toString()
                val stock = etStock.text.toString().toDoubleOrNull() ?: 0.0
                val minStock = etMinStock.text.toString().toDoubleOrNull() ?: 0.0
                val categoryIndex = spinnerCategory.selectedItemPosition
                val categoryId = viewModel.categories.value?.getOrNull(categoryIndex)?.id ?: 1

                if (name.isNotEmpty() && price > 0) {
                    val request = CreateProductRequest(
                        name = name,
                        description = description,
                        price = price,
                        unit = unit,
                        categoryId = categoryId,
                        stock = stock,
                        minStock = minStock,
                        imageUrl = null
                    )
                    viewModel.createProduct(request)
                } else {
                    Toast.makeText(this, "Completa los campos requeridos", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showEditProductDialog(product: ProductWithInventory) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_product, null)
        val etName = dialogView.findViewById<EditText>(R.id.etProductName)
        val etDescription = dialogView.findViewById<EditText>(R.id.etProductDescription)
        val etPrice = dialogView.findViewById<EditText>(R.id.etProductPrice)
        val etUnit = dialogView.findViewById<EditText>(R.id.etProductUnit)
        val etStock = dialogView.findViewById<EditText>(R.id.etProductStock)
        val etMinStock = dialogView.findViewById<EditText>(R.id.etMinStock)
        val spinnerCategory = dialogView.findViewById<Spinner>(R.id.spinnerCategory)

        etName.setText(product.name)
        etDescription.setText(product.description ?: "")
        etPrice.setText(product.price.toString())
        etUnit.setText(product.unit)
        etStock.setText((product.stock ?: 0.0).toString())
        etMinStock.setText((product.minStock ?: 0.0).toString())

        viewModel.categories.observe(this, Observer { categories ->
            val categoryNames = categories?.map { it.name } ?: emptyList()
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categoryNames)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerCategory.adapter = adapter
            val categoryIndex = categories?.indexOfFirst { it.id == product.categoryId } ?: 0
            spinnerCategory.setSelection(categoryIndex)
        })

        AlertDialog.Builder(this)
            .setTitle("Editar Producto")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val name = etName.text.toString()
                val description = etDescription.text.toString()
                val price = etPrice.text.toString().toDoubleOrNull()
                val unit = etUnit.text.toString()
                val stock = etStock.text.toString().toDoubleOrNull()
                val minStock = etMinStock.text.toString().toDoubleOrNull()
                val categoryIndex = spinnerCategory.selectedItemPosition
                val categoryId = viewModel.categories.value?.getOrNull(categoryIndex)?.id

                val request = UpdateProductRequest(
                    name = if (name != product.name) name else null,
                    description = if (description != product.description) description else null,
                    price = if (price != null && price != product.price) price else null,
                    unit = if (unit != product.unit) unit else null,
                    stock = if (stock != null && stock != product.stock) stock else null,
                    minStock = if (minStock != null && minStock != product.minStock) minStock else null,
                    categoryId = if (categoryId != null && categoryId != product.categoryId) categoryId else null
                )
                viewModel.updateProduct(product.id, request)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showUpdateStockDialog(product: ProductWithInventory) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_stock, null)
        val tvProductName = dialogView.findViewById<TextView>(R.id.tvStockProductName)
        val tvCurrentStock = dialogView.findViewById<TextView>(R.id.tvCurrentStock)
        val etQuantity = dialogView.findViewById<EditText>(R.id.etStockQuantity)
        val radioAdd = dialogView.findViewById<RadioButton>(R.id.radioAdd)
        val radioSubtract = dialogView.findViewById<RadioButton>(R.id.radioSubtract)
        val radioSet = dialogView.findViewById<RadioButton>(R.id.radioSet)
        val etNotes = dialogView.findViewById<EditText>(R.id.etStockNotes)

        tvProductName.text = product.name
        tvCurrentStock.text = "Stock actual: ${product.stock ?: 0} ${product.unit}"

        AlertDialog.Builder(this)
            .setTitle("Actualizar Stock")
            .setView(dialogView)
            .setPositiveButton("Actualizar") { _, _ ->
                val quantity = etQuantity.text.toString().toDoubleOrNull() ?: 0.0
                val changeType = when {
                    radioAdd.isChecked -> "add"
                    radioSubtract.isChecked -> "subtract"
                    else -> "set"
                }
                val notes = etNotes.text.toString()
                viewModel.updateStock(product.id, quantity, changeType, notes)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showDeleteConfirmDialog(product: ProductWithInventory) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar Producto")
            .setMessage("¿Estás seguro de eliminar ${product.name}?")
            .setPositiveButton("Eliminar") { _, _ ->
                viewModel.deleteProduct(product.id)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showProcessPaymentDialog(payment: DriverPayment) {
        AlertDialog.Builder(this)
            .setTitle("Procesar Pago")
            .setMessage(buildString {
                appendLine("Conductor: ${payment.driverName}")
                appendLine("Semana: ${payment.weekStart} - ${payment.weekEnd}")
                appendLine("Pedidos: ${payment.totalOrders}")
                appendLine("Pago base: ${formatter.format(payment.totalBasePayment)}")
                appendLine("Propinas: ${formatter.format(payment.totalTips)}")
                appendLine("Comisión: ${formatter.format(payment.platformCommission)}")
                appendLine("Neto a pagar: ${formatter.format(payment.netAmount)}")
                appendLine()
                appendLine("¿Marcar como pagado?")
            })
            .setPositiveButton("Marcar como Pagado") { _, _ ->
                viewModel.processDriverPayment(payment.id, "paid")
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.admin_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
            R.id.action_refresh -> {
                when (tabLayout.selectedTabPosition) {
                    0 -> viewModel.loadDashboardStats()
                    1 -> viewModel.loadProducts()
                    2 -> viewModel.loadProducts(lowStock = false)  // ← CORREGIDO
                    3 -> viewModel.loadDriverPayments()
                    4 -> viewModel.loadInventoryLogs()
                }
            }
            R.id.action_calculate_payments -> {
                showCalculatePaymentsDialog()
            }
            R.id.action_logout -> {
                SessionManager.logout()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showCalculatePaymentsDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_calculate_payment, null)
        val spinnerDriver = dialogView.findViewById<Spinner>(R.id.spinnerDriver)
        val etWeekStart = dialogView.findViewById<EditText>(R.id.etWeekStart)

        viewModel.drivers.observe(this, Observer { drivers ->
            val driverNames = drivers?.map { "${it.fullName} (${it.email})" } ?: emptyList()
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, driverNames)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerDriver.adapter = adapter
        })

        etWeekStart.setText(getCurrentWeekStart())

        AlertDialog.Builder(this)
            .setTitle("Calcular Pago")
            .setView(dialogView)
            .setPositiveButton("Calcular") { _, _ ->
                val driverIndex = spinnerDriver.selectedItemPosition
                val driverId = viewModel.drivers.value?.getOrNull(driverIndex)?.id
                val weekStart = etWeekStart.text.toString()

                if (driverId != null && weekStart.isNotEmpty()) {
                    viewModel.calculateDriverPayment(driverId, weekStart)
                } else {
                    Toast.makeText(this, "Selecciona un conductor y una fecha", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun getCurrentWeekStart(): String {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        return dateFormat.format(calendar.time)
    }
}