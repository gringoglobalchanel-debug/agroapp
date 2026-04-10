package com.agroapp.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.agroapp.R
import com.agroapp.model.DynamicPackage
import com.agroapp.model.DynamicPackageOrder
import com.agroapp.network.RetrofitClient
import com.agroapp.network.SessionManager
import com.agroapp.service.DriverLocationService
import com.agroapp.viewmodel.DriverViewModel
import com.agroapp.viewmodel.TakePackageState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class DriverActivity : AppCompatActivity() {

    private val viewModel: DriverViewModel by viewModels()
    private lateinit var availablePackagesAdapter: AvailablePackagesAdapter
    private lateinit var myPackagesAdapter: MyPackagesAdapter

    var pendingPhotoOrderId: String? = null
    var pendingPhotoPosition: Int = -1
    var pendingPhotoAdapter: PackageOrderAdapter? = null
    var pendingPhotoAdapterForMyPackages: MyPackageOrdersAdapter? = null
    private var pendingPhotoFile: File? = null

    private val formatter = NumberFormat.getCurrencyInstance(Locale.US)

    private val takePhotoLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val orderId = pendingPhotoOrderId
            val position = pendingPhotoPosition
            val adapter = pendingPhotoAdapter
            val adapterForMyPackages = pendingPhotoAdapterForMyPackages
            val photoFile = pendingPhotoFile

            if (orderId != null && position >= 0 && photoFile != null && photoFile.exists()) {
                showPhotoConfirmationDialog(
                    orderId = orderId,
                    position = position,
                    adapter = adapter,
                    adapterForMyPackages = adapterForMyPackages,
                    photoFile = photoFile
                )
            } else {
                Toast.makeText(this, "Error al procesar la foto", Toast.LENGTH_SHORT).show()
                clearPendingPhoto()
            }
        } else {
            Toast.makeText(this, "Foto cancelada", Toast.LENGTH_SHORT).show()
            clearPendingPhoto()
        }
    }

    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) openCamera()
        else {
            Toast.makeText(this, "Se necesita permiso de cámara", Toast.LENGTH_LONG).show()
            clearPendingPhoto()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver)

        if (SessionManager.getUserType() != "driver") {
            Toast.makeText(this, "No tienes permisos de repartidor", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        initViews()
        setupObservers()
        setupButtons()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        moveTaskToBack(true)
        super.onBackPressed()
    }

    override fun onSupportNavigateUp(): Boolean {
        moveTaskToBack(true)
        return true
    }

    private fun initViews() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Grün"
        toolbar.setTitleTextColor(resources.getColor(android.R.color.white, theme))

        val userName = SessionManager.getUserName()
        if (userName.isNotEmpty()) {
            supportActionBar?.subtitle = "Bienvenido, $userName"
            toolbar.setSubtitleTextColor(resources.getColor(android.R.color.white, theme))
        }

        availablePackagesAdapter = AvailablePackagesAdapter(
            onTakeClick = { packageItem -> showTakePackageDialog(packageItem) },
            onOrderStatusChange = { orderId, newStatus, _ -> viewModel.updateOrderStatus(orderId, newStatus) },
            onTakePhotoClick = { orderId, position, adapter ->
                pendingPhotoOrderId = orderId
                pendingPhotoPosition = position
                pendingPhotoAdapter = adapter
                pendingPhotoAdapterForMyPackages = null
                checkCameraPermissionAndOpen()
            }
        )

        myPackagesAdapter = MyPackagesAdapter(
            onConfirmClick = { orderId, position, adapter ->
                pendingPhotoOrderId = orderId
                pendingPhotoPosition = position
                pendingPhotoAdapter = null
                pendingPhotoAdapterForMyPackages = adapter
                checkCameraPermissionAndOpen()
            },
            onStartTripClick = { order -> startTrip(order) },
            onWhatsAppClick = { order -> openWhatsApp(order) }
        )

        findViewById<RecyclerView>(R.id.rvAvailableBlocks).apply {
            layoutManager = LinearLayoutManager(this@DriverActivity)
            adapter = availablePackagesAdapter
        }

        findViewById<RecyclerView>(R.id.rvMyBlocks).apply {
            layoutManager = LinearLayoutManager(this@DriverActivity)
            adapter = myPackagesAdapter
        }
    }

    // ==================== MODAL TOMAR PAQUETE ====================

    private fun showTakePackageDialog(packageItem: DynamicPackage) {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(android.graphics.Color.WHITE)
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setPadding(32, 48, 32, 32)
            setBackgroundColor(android.graphics.Color.parseColor("#2E7D32"))
        }
        header.addView(TextView(this).apply {
            text = "📦"
            textSize = 40f
            gravity = android.view.Gravity.CENTER
        })
        header.addView(TextView(this).apply {
            text = "¿Confirmas que quieres\ntomar este viaje?"
            textSize = 20f
            setTextColor(android.graphics.Color.WHITE)
            gravity = android.view.Gravity.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, 16, 0, 0)
        })
        container.addView(header)

        val btnContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 24, 32, 32)
        }

        val btnTomar = Button(this).apply {
            text = "Tomar paquete"
            textSize = 16f
            setTextColor(android.graphics.Color.WHITE)
            setBackgroundColor(android.graphics.Color.parseColor("#2E7D32"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 12 }
            setPadding(0, 20, 0, 20)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        val btnCancelar = Button(this).apply {
            text = "Cancelar"
            textSize = 15f
            setTextColor(android.graphics.Color.parseColor("#757575"))
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(0, 12, 0, 12)
        }

        btnContainer.addView(btnTomar)
        btnContainer.addView(btnCancelar)
        container.addView(btnContainer)

        var dialog: AlertDialog? = null
        btnTomar.setOnClickListener {
            dialog?.dismiss()
            viewModel.takePackage(packageItem.id)
        }
        btnCancelar.setOnClickListener { dialog?.dismiss() }

        dialog = AlertDialog.Builder(this)
            .setView(container)
            .setCancelable(true)
            .create()
        dialog.show()
    }

    // ==================== INICIAR VIAJE ====================

    // ✅ CORREGIDO: espera respuesta del backend antes de abrir el mapa
    private fun startTrip(order: DynamicPackageOrder) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.startTrip(
                    SessionManager.getToken(), order.order_id
                )
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(
                            this@DriverActivity,
                            "🚴 Viaje iniciado — el cliente puede seguirte",
                            Toast.LENGTH_LONG
                        ).show()
                        startLocationTracking(order)
                        openNavigation(order)
                    } else {
                        val error = response.errorBody()?.string() ?: "Error desconocido"
                        Toast.makeText(
                            this@DriverActivity,
                            "Error al iniciar viaje: $error",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@DriverActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun startLocationTracking(order: DynamicPackageOrder) {
        val intent = Intent(this, DriverLocationService::class.java).apply {
            action = DriverLocationService.ACTION_START
            putExtra(DriverLocationService.EXTRA_ORDER_ID, order.order_id)
        }
        ContextCompat.startForegroundService(this, intent)
        Toast.makeText(this, "📍 Compartiendo ubicación con el cliente", Toast.LENGTH_SHORT).show()
    }

    private fun openNavigation(order: DynamicPackageOrder) {
        val lat = order.delivery_latitude
        val lng = order.delivery_longitude
        if (lat != null && lng != null && lat != 0.0 && lng != 0.0) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$lat,$lng"))
                intent.setPackage("com.google.android.apps.maps")
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "No se pudo abrir el mapa", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "No hay ubicación disponible", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openWhatsApp(order: DynamicPackageOrder) {
        val phone = order.customer_phone.replace("+", "").replace(" ", "").trim()
        val message = "Hola ${order.customer_name}, soy tu repartidor de Grun. Estoy en camino con tu pedido."
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$phone?text=${Uri.encode(message)}")))
        } catch (e: Exception) {
            Toast.makeText(this, "No se pudo abrir WhatsApp", Toast.LENGTH_SHORT).show()
        }
    }

    // ==================== OBSERVERS ====================

    private fun setupObservers() {
        viewModel.loading.observe(this, Observer { isLoading ->
            findViewById<ProgressBar>(R.id.progressBar).visibility = if (isLoading) View.VISIBLE else View.GONE
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
                is TakePackageState.Loading -> Toast.makeText(this, "Procesando...", Toast.LENGTH_SHORT).show()
                is TakePackageState.Success -> {
                    Toast.makeText(this, "${state.message}\nTotal pedidos: ${state.totalOrders}", Toast.LENGTH_LONG).show()
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
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }
    }

    private fun updateEarningsUI(earnings: com.agroapp.model.DriverPackageEarnings) {
        findViewById<TextView>(R.id.tvThisWeekEarnings).text = formatter.format(earnings.driver_net_amount)
        findViewById<TextView>(R.id.tvTotalDeliveries).text = earnings.total_orders.toString()

        val tvTotalTips = findViewById<TextView>(R.id.tvTotalTips)
        if (earnings.total_tips > 0) {
            tvTotalTips.visibility = View.VISIBLE
            tvTotalTips.text = "💸 Propinas: ${formatter.format(earnings.total_tips)}"
        } else {
            tvTotalTips.visibility = View.GONE
        }

        val today = LocalDate.now()
        val daysUntilFriday = (5 - today.dayOfWeek.value).let { if (it <= 0) it + 7 else it }
        val nextFriday = today.plusDays(daysUntilFriday.toLong())
        findViewById<TextView>(R.id.tvNextPaymentDate).text =
            "Viernes ${nextFriday.format(DateTimeFormatter.ofPattern("dd/MM"))}"
    }

    // ==================== CAMARA ====================

    private fun checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun openCamera() {
        val orderId = pendingPhotoOrderId ?: run {
            Toast.makeText(this, "Error: No hay pedido pendiente", Toast.LENGTH_SHORT).show()
            return
        }
        pendingPhotoFile = File(cacheDir, "delivery_${orderId}.jpg")
        val photoUri = FileProvider.getUriForFile(this, "${packageName}.provider", pendingPhotoFile!!)
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply { putExtra(MediaStore.EXTRA_OUTPUT, photoUri) }
        if (intent.resolveActivity(packageManager) != null) {
            takePhotoLauncher.launch(intent)
        } else {
            Toast.makeText(this, "No hay cámara disponible", Toast.LENGTH_SHORT).show()
            clearPendingPhoto()
        }
    }

    private fun showPhotoConfirmationDialog(
        orderId: String, position: Int,
        adapter: PackageOrderAdapter?,
        adapterForMyPackages: MyPackageOrdersAdapter?,
        photoFile: File
    ) {
        val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
        val dialogView = layoutInflater.inflate(R.layout.dialog_photo_confirmation, null)
        dialogView.findViewById<ImageView>(R.id.ivPhotoPreview).setImageBitmap(bitmap)

        AlertDialog.Builder(this)
            .setTitle("Confirmar entrega")
            .setView(dialogView)
            .setMessage("¿Confirmas que este pedido fue entregado correctamente?")
            .setPositiveButton("Aceptar entrega") { _, _ ->
                viewModel.updateOrderStatus(orderId, "completed")
                Toast.makeText(this, "✅ Pedido entregado correctamente", Toast.LENGTH_SHORT).show()
                adapter?.removeOrder(position)
                adapterForMyPackages?.removeOrder(position)
                photoFile.delete()
                clearPendingPhoto()
                viewModel.loadMyPackages()
                Handler(Looper.getMainLooper()).postDelayed({ viewModel.loadPackageEarnings() }, 500)
            }
            .setNegativeButton("Reintentar") { _, _ ->
                photoFile.delete()
                checkCameraPermissionAndOpen()
            }
            .setNeutralButton("Cancelar") { _, _ ->
                photoFile.delete()
                clearPendingPhoto()
            }
            .setCancelable(false)
            .show()
    }

    private fun clearPendingPhoto() {
        pendingPhotoOrderId = null
        pendingPhotoPosition = -1
        pendingPhotoAdapter = null
        pendingPhotoAdapterForMyPackages = null
        pendingPhotoFile = null
    }
}