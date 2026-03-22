package com.agroapp.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.agroapp.R
import com.agroapp.network.SessionManager
import com.agroapp.viewmodel.DriverViewModel
import com.agroapp.viewmodel.TakePackageState
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class DriverActivity : AppCompatActivity() {

    private val viewModel: DriverViewModel by viewModels()
    private lateinit var availablePackagesAdapter: AvailablePackagesAdapter
    private lateinit var myPackagesAdapter: MyPackagesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver)

        val userType = SessionManager.getUserType()
        if (userType != "driver") {
            Toast.makeText(this, "No tienes permisos de repartidor", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        initViews()
        setupObservers()
        setupButtons()
    }

    private fun initViews() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Grün"
        supportActionBar?.setDisplayShowTitleEnabled(true)
        toolbar.setTitleTextColor(resources.getColor(android.R.color.white, theme))

        val userName = SessionManager.getUserName()
        if (userName.isNotEmpty()) {
            supportActionBar?.subtitle = "Bienvenido, $userName"
            toolbar.setSubtitleTextColor(resources.getColor(android.R.color.white, theme))
        }

        // FIX: pasar los dos parámetros que requiere AvailablePackagesAdapter
        availablePackagesAdapter = AvailablePackagesAdapter(
            onTakeClick = { packageItem ->
                showTakePackageDialog(packageItem)
            },
            onOrderStatusChange = { orderId, newStatus, photoUri ->
                viewModel.updateOrderStatus(orderId, newStatus)
            }
        )

        myPackagesAdapter = MyPackagesAdapter()

        findViewById<RecyclerView>(R.id.rvAvailableBlocks).apply {
            layoutManager = LinearLayoutManager(this@DriverActivity)
            adapter = availablePackagesAdapter
        }

        findViewById<RecyclerView>(R.id.rvMyBlocks).apply {
            layoutManager = LinearLayoutManager(this@DriverActivity)
            adapter = myPackagesAdapter
        }
    }

    private fun setupObservers() {
        viewModel.loading.observe(this, Observer { isLoading ->
            findViewById<ProgressBar>(R.id.progressBar).visibility =
                if (isLoading) View.VISIBLE else View.GONE
        })

        viewModel.availablePackages.observe(this, Observer { packages ->
            if (packages != null) availablePackagesAdapter.submitList(packages)
        })

        viewModel.myPackages.observe(this, Observer { packages ->
            if (packages != null) myPackagesAdapter.submitList(packages)
        })

        viewModel.packageEarnings.observe(this, Observer { earnings ->
            if (earnings != null) updateEarningsUI(earnings)
        })

        viewModel.takePackageState.observe(this, Observer { state ->
            when (state) {
                is TakePackageState.Loading -> {
                    Toast.makeText(this, "Procesando...", Toast.LENGTH_SHORT).show()
                }
                is TakePackageState.Success -> {
                    Toast.makeText(
                        this,
                        "${state.message}\nPago: ${formatter.format(state.payment)}\nTotal pedidos: ${state.totalOrders}",
                        Toast.LENGTH_LONG
                    ).show()
                    viewModel.resetTakePackageState()
                }
                is TakePackageState.Error -> {
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                    viewModel.resetTakePackageState()
                }
                else -> {}
            }
        })
    }

    private val formatter = NumberFormat.getCurrencyInstance(Locale.US)

    private fun setupButtons() {
        val btnAvailable = findViewById<Button>(R.id.btnAvailableBlocks)
        val btnMyBlocks = findViewById<Button>(R.id.btnMyBlocks)
        val btnLogout = findViewById<Button>(R.id.btnLogout)

        btnAvailable.setOnClickListener {
            findViewById<RecyclerView>(R.id.rvAvailableBlocks).visibility = View.VISIBLE
            findViewById<RecyclerView>(R.id.rvMyBlocks).visibility = View.GONE
            btnAvailable.setBackgroundColor(resources.getColor(R.color.green_700, theme))
            btnMyBlocks.setBackgroundColor(resources.getColor(R.color.gray_500, theme))
            viewModel.loadAvailablePackages()
        }

        btnMyBlocks.setOnClickListener {
            findViewById<RecyclerView>(R.id.rvAvailableBlocks).visibility = View.GONE
            findViewById<RecyclerView>(R.id.rvMyBlocks).visibility = View.VISIBLE
            btnMyBlocks.setBackgroundColor(resources.getColor(R.color.green_700, theme))
            btnAvailable.setBackgroundColor(resources.getColor(R.color.gray_500, theme))
            viewModel.loadMyPackages()
        }

        btnLogout.setOnClickListener {
            SessionManager.logout()
            Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun updateEarningsUI(earnings: com.agroapp.model.DriverPackageEarnings) {
        findViewById<TextView>(R.id.tvThisWeekEarnings).text =
            formatter.format(earnings.driver_net_amount)
        findViewById<TextView>(R.id.tvTotalDeliveries).text =
            earnings.total_orders.toString()

        val today = LocalDate.now()
        val daysUntilFriday = (5 - today.dayOfWeek.value).let { if (it <= 0) it + 7 else it }
        val nextFriday = today.plusDays(daysUntilFriday.toLong())
        val dateFormatter = DateTimeFormatter.ofPattern("dd/MM")
        findViewById<TextView>(R.id.tvNextPaymentDate).text =
            "Viernes ${nextFriday.format(dateFormatter)}"
    }

    private fun showTakePackageDialog(packageItem: com.agroapp.model.DynamicPackage) {
        val totalPayment = packageItem.current_size * 1.25
        val driverPayment = totalPayment * 0.90

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Tomar paquete")
            .setMessage("""
                📦 Paquete con ${packageItem.current_size} pedidos
                💰 Pago: ${formatter.format(driverPayment)}
                📍 Pedidos de distintas zonas
                🕐 Formado desde: ${formatDate(packageItem.created_at)}
                
                ¿Confirmas que quieres tomar este paquete?
            """.trimIndent())
            .setPositiveButton("Tomar") { _, _ ->
                viewModel.takePackage(packageItem.id)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun formatDate(dateString: String): String {
        return try {
            val date = java.time.format.DateTimeFormatter.ISO_DATE_TIME.parse(dateString)
            val localDate = java.time.LocalDateTime.from(date)
            localDate.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM HH:mm"))
        } catch (e: Exception) {
            dateString
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}