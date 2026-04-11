package com.agroapp.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.agroapp.BuildConfig
import com.agroapp.R
import com.agroapp.network.SessionManager
import com.agroapp.viewmodel.OrderState
import com.agroapp.viewmodel.OrderViewModel
import com.agroapp.viewmodel.ProductViewModel
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class CartActivity : AppCompatActivity() {

    private val orderViewModel: OrderViewModel by viewModels()
    private val productViewModel: ProductViewModel by viewModels()
    private lateinit var adapter: CartAdapter
    private lateinit var paymentSheet: PaymentSheet
    private var clientSecret: String? = null
    private var pendingTotal: Double = 0.0
    private var selectedTip: Double = 0.0
    private var pendingLatitude: Double? = null
    private var pendingLongitude: Double? = null
    private var selectedAddress: String = ""

    private val STRIPE_PUBLISHABLE_KEY = BuildConfig.STRIPE_PUBLISHABLE_KEY
    private val STRIPE_SECRET_KEY = BuildConfig.STRIPE_SECRET_KEY

    companion object {
        const val YAPPI_PHONE = "68358190"
    }

    private val mapLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            pendingLatitude = data?.getDoubleExtra("latitude", 0.0)?.takeIf { it != 0.0 }
            pendingLongitude = data?.getDoubleExtra("longitude", 0.0)?.takeIf { it != 0.0 }
            selectedAddress = data?.getStringExtra("address") ?: ""
            val tvDeliveryAddress = findViewById<TextView>(R.id.tvDeliveryAddress)
            if (selectedAddress.isNotEmpty()) {
                tvDeliveryAddress.text = selectedAddress
            } else if (pendingLatitude != null && pendingLongitude != null) {
                tvDeliveryAddress.text = "${"%.6f".format(pendingLatitude!!)}, ${"%.6f".format(pendingLongitude!!)}"
            }
            if (pendingLatitude != null && pendingLongitude != null) {
                SessionManager.saveDeliveryLocation(pendingLatitude!!, pendingLongitude!!, selectedAddress)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        val rvCartItems = findViewById<RecyclerView>(R.id.rvCartItems)
        val tvEmpty = findViewById<TextView>(R.id.tvEmpty)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val rgPaymentMethod = findViewById<RadioGroup>(R.id.rgPaymentMethod)
        val btnConfirmOrder = findViewById<Button>(R.id.btnConfirmOrder)
        val tvSubtotal = findViewById<TextView>(R.id.tvSubtotal)
        val tvTotal = findViewById<TextView>(R.id.tvTotal)
        val tvSelectedTip = findViewById<TextView>(R.id.tvSelectedTip)
        val layoutSelectedTip = findViewById<LinearLayout>(R.id.layoutSelectedTip)
        val tvDeliveryAddress = findViewById<TextView>(R.id.tvDeliveryAddress)
        val btnSelectLocation = findViewById<Button>(R.id.btnSelectLocation)
        val btnTip1 = findViewById<Button>(R.id.btnTip1)
        val btnTip3 = findViewById<Button>(R.id.btnTip3)
        val btnTip5 = findViewById<Button>(R.id.btnTip5)
        val etCustomTip = findViewById<EditText>(R.id.etCustomTip)
        val btnApplyCustomTip = findViewById<Button>(R.id.btnApplyCustomTip)
        val tvSeguirComprando = findViewById<TextView>(R.id.tvSeguirComprando)

        tvSeguirComprando.setOnClickListener { startActivity(Intent(this, ProductsActivity::class.java)) }

        PaymentConfiguration.init(this, STRIPE_PUBLISHABLE_KEY)
        paymentSheet = PaymentSheet(this, ::onPaymentSheetResult)

        setSupportActionBar(toolbar)
        supportActionBar?.title = "Mi Carrito"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val savedAddress = SessionManager.getAddress()
        tvDeliveryAddress.text = if (savedAddress.isNotEmpty()) savedAddress else "Agrega tu direccion de entrega"

        btnSelectLocation.setOnClickListener {
            val intent = Intent(this, MapsActivity::class.java)
            pendingLatitude?.let { intent.putExtra("latitude", it) }
            pendingLongitude?.let { intent.putExtra("longitude", it) }
            mapLauncher.launch(intent)
        }

        adapter = CartAdapter(
            cartItems = emptyMap(),
            onQuantityChange = { product, newQuantity -> productViewModel.updateQuantity(product, newQuantity) },
            onRemove = { product -> productViewModel.removeFromCart(product) }
        )
        rvCartItems.layoutManager = LinearLayoutManager(this)
        rvCartItems.adapter = adapter

        productViewModel.cart.observe(this, Observer { cartMap ->
            updateCartUI(cartMap ?: emptyMap(), tvEmpty, rvCartItems, tvSubtotal, tvTotal, tvSelectedTip, layoutSelectedTip)
        })

        btnTip1.setOnClickListener {
            selectedTip = 1.0
            updateTipUI(btnTip1, btnTip1, btnTip3, btnTip5, etCustomTip, tvSelectedTip, layoutSelectedTip)
            updateTotal(tvSubtotal, tvTotal, tvSelectedTip, layoutSelectedTip)
        }
        btnTip3.setOnClickListener {
            selectedTip = 3.0
            updateTipUI(btnTip3, btnTip1, btnTip3, btnTip5, etCustomTip, tvSelectedTip, layoutSelectedTip)
            updateTotal(tvSubtotal, tvTotal, tvSelectedTip, layoutSelectedTip)
        }
        btnTip5.setOnClickListener {
            selectedTip = 5.0
            updateTipUI(btnTip5, btnTip1, btnTip3, btnTip5, etCustomTip, tvSelectedTip, layoutSelectedTip)
            updateTotal(tvSubtotal, tvTotal, tvSelectedTip, layoutSelectedTip)
        }
        btnApplyCustomTip.setOnClickListener {
            val tipText = etCustomTip.text.toString()
            if (!TextUtils.isEmpty(tipText)) {
                try {
                    selectedTip = tipText.toDouble()
                    if (selectedTip < 0) selectedTip = 0.0
                    updateTipUI(null, btnTip1, btnTip3, btnTip5, etCustomTip, tvSelectedTip, layoutSelectedTip, true)
                    updateTotal(tvSubtotal, tvTotal, tvSelectedTip, layoutSelectedTip)
                    etCustomTip.text.clear()
                } catch (e: NumberFormatException) {
                    Toast.makeText(this, "Ingresa un monto valido", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Ingresa un monto", Toast.LENGTH_SHORT).show()
            }
        }

        btnConfirmOrder.setOnClickListener {
            val paymentMethod = when (rgPaymentMethod.checkedRadioButtonId) {
                R.id.rbYappi -> "yappi"
                R.id.rbCard -> "card"
                else -> "yappi"
            }
            val cartItems = productViewModel.getCartItemsMap()
            val deliveryAddress = if (selectedAddress.isNotEmpty()) selectedAddress else tvDeliveryAddress.text.toString()
            if (cartItems.isEmpty()) {
                Toast.makeText(this, "El carrito esta vacio", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (deliveryAddress.isEmpty() || deliveryAddress == "Agrega tu direccion de entrega") {
                Toast.makeText(this, "Por favor selecciona una direccion de entrega", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            when (paymentMethod) {
                "card" -> processCardPayment(pendingLatitude, pendingLongitude, deliveryAddress)
                "yappi" -> processYappiPayment(pendingLatitude, pendingLongitude, deliveryAddress)
                else -> orderViewModel.createOrder(cartItems, paymentMethod, deliveryAddress, null, selectedTip, pendingLatitude, pendingLongitude)
            }
        }

        orderViewModel.orderState.observe(this, Observer { state ->
            when (state) {
                is OrderState.Loading -> { progressBar.visibility = View.VISIBLE; btnConfirmOrder.isEnabled = false }
                is OrderState.Success -> {
                    progressBar.visibility = View.GONE; btnConfirmOrder.isEnabled = true
                    Toast.makeText(this, "Pedido confirmado!", Toast.LENGTH_LONG).show()
                    productViewModel.clearCart(); finish()
                }
                is OrderState.Error -> {
                    progressBar.visibility = View.GONE; btnConfirmOrder.isEnabled = true
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
                is OrderState.OutOfTime -> {
                    progressBar.visibility = View.GONE; btnConfirmOrder.isEnabled = true
                    Toast.makeText(this, "Pedidos solo de 8am a 12pm", Toast.LENGTH_LONG).show()
                }
                else -> { progressBar.visibility = View.GONE; btnConfirmOrder.isEnabled = true }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        val savedLat = SessionManager.getDeliveryLatitude()
        val savedLng = SessionManager.getDeliveryLongitude()
        val savedAddress = SessionManager.getAddress()
        if (savedLat != 0.0 && savedLng != 0.0) {
            pendingLatitude = savedLat; pendingLongitude = savedLng; selectedAddress = savedAddress
            findViewById<TextView>(R.id.tvDeliveryAddress).text =
                if (savedAddress.isNotEmpty()) savedAddress
                else "${"%.6f".format(savedLat)}, ${"%.6f".format(savedLng)}"
        }
    }

    private fun processCardPayment(latitude: Double?, longitude: Double?, deliveryAddress: String) {
        val finalTotal = getFinalTotal()
        if (finalTotal <= 0) { Toast.makeText(this, "Monto invalido", Toast.LENGTH_SHORT).show(); return }
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        progressBar.visibility = View.VISIBLE
        pendingLatitude = latitude; pendingLongitude = longitude
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val amountInCents = (finalTotal * 100).toInt()
                val url = URL("https://api.stripe.com/v1/payment_intents")
                val connection = url.openConnection() as HttpsURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Authorization", "Bearer $STRIPE_SECRET_KEY")
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                connection.doOutput = true; connection.connectTimeout = 15000; connection.readTimeout = 15000
                val postData = "amount=$amountInCents&currency=usd&automatic_payment_methods[enabled]=true"
                connection.outputStream.write(postData.toByteArray())
                val responseCode = connection.responseCode
                val response = if (responseCode == 200) connection.inputStream.bufferedReader().readText()
                else connection.errorStream.bufferedReader().readText()
                val json = JSONObject(response)
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    if (responseCode == 200) {
                        clientSecret = json.getString("client_secret")
                        presentPaymentSheet(deliveryAddress)
                    } else {
                        val errorMsg = json.optJSONObject("error")?.optString("message") ?: "Error al iniciar pago"
                        Toast.makeText(this@CartActivity, errorMsg, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@CartActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun presentPaymentSheet(deliveryAddress: String) {
        clientSecret?.let { paymentSheet.presentWithPaymentIntent(it, PaymentSheet.Configuration("AgroApp Grun")) }
    }

    private fun onPaymentSheetResult(paymentSheetResult: PaymentSheetResult) {
        when (paymentSheetResult) {
            is PaymentSheetResult.Completed -> {
                val cartItems = productViewModel.getCartItemsMap()
                val deliveryAddress = if (selectedAddress.isNotEmpty()) selectedAddress
                else findViewById<TextView>(R.id.tvDeliveryAddress).text.toString()
                orderViewModel.createOrder(cartItems, "card", deliveryAddress, clientSecret, selectedTip, pendingLatitude, pendingLongitude)
            }
            is PaymentSheetResult.Canceled -> Toast.makeText(this, "Pago cancelado", Toast.LENGTH_SHORT).show()
            is PaymentSheetResult.Failed -> Toast.makeText(this, "Error: ${paymentSheetResult.error.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun processYappiPayment(latitude: Double?, longitude: Double?, deliveryAddress: String) {
        val finalTotal = getFinalTotal()
        if (finalTotal <= 0) { Toast.makeText(this, "Monto invalido", Toast.LENGTH_SHORT).show(); return }
        pendingTotal = finalTotal; pendingLatitude = latitude; pendingLongitude = longitude
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        progressBar.visibility = View.VISIBLE
        val cartItems = productViewModel.getCartItemsMap()
        orderViewModel.createPendingYappiOrder(cartItems, deliveryAddress, selectedTip, latitude, longitude) { success, orderId, referenceCode, errorMsg ->
            progressBar.visibility = View.GONE
            if (success && orderId != null && referenceCode != null) showYappiPaymentDialog(finalTotal)
            else Toast.makeText(this, errorMsg ?: "Error al procesar el pedido.", Toast.LENGTH_LONG).show()
        }
    }

    private fun showYappiPaymentDialog(total: Double) = buildYappiDialogView(total, getProductsTotal())

    private fun buildYappiDialogView(total: Double, productsTotal: Double): View {
        val context = this
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(android.graphics.Color.WHITE)
        }

        // Header verde
        val header = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setPadding(32, 48, 32, 32)
            setBackgroundColor(android.graphics.Color.parseColor("#2E7D32"))
        }
        header.addView(TextView(context).apply {
            text = "YAPPI"
            textSize = 28f
            setTextColor(android.graphics.Color.WHITE)
            gravity = android.view.Gravity.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        })
        header.addView(TextView(context).apply {
            text = "Pagar con YAPPI"
            textSize = 22f
            setTextColor(android.graphics.Color.WHITE)
            gravity = android.view.Gravity.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, 12, 0, 4)
        })
        header.addView(TextView(context).apply {
            text = "Realiza tu transferencia y confirma"
            textSize = 13f
            setTextColor(android.graphics.Color.parseColor("#A5D6A7"))
            gravity = android.view.Gravity.CENTER
        })
        container.addView(header)

        val body = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 8)
        }

        addStep(body, "1", "Envia a este numero", YAPPI_PHONE, "#2E7D32")
        addDivider(body)

        val desglose = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 16, 0, 16)
        }
        addStepLabel(desglose, "2", "Monto a transferir")
        addAmountRow(desglose, "Productos", "$${"%.2f".format(productsTotal)}", "#424242")
        if (selectedTip > 0) addAmountRow(desglose, "Propina", "$${"%.2f".format(selectedTip)}", "#424242")

        val totalRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 16, 0, 0)
        }
        totalRow.addView(TextView(context).apply {
            text = "TOTAL"
            textSize = 18f
            setTextColor(android.graphics.Color.parseColor("#1B5E20"))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        totalRow.addView(TextView(context).apply {
            text = "$${"%.2f".format(total)}"
            textSize = 22f
            setTextColor(android.graphics.Color.parseColor("#2E7D32"))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        })
        desglose.addView(totalRow)
        body.addView(desglose)
        addDivider(body)

        body.addView(TextView(context).apply {
            text = "Tu pedido quedara en estado Esperando confirmacion hasta que verifiquemos tu pago."
            textSize = 12f
            setTextColor(android.graphics.Color.parseColor("#E65100"))
            setPadding(0, 16, 0, 8)
        })
        container.addView(body)

        val btnContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 8, 32, 32)
        }

        val btnOpenYappi = Button(context).apply {
            text = "Abrir YAPPI"
            textSize = 15f
            setTextColor(android.graphics.Color.WHITE)
            setBackgroundColor(android.graphics.Color.parseColor("#2E7D32"))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 12 }
            setPadding(0, 16, 0, 16)
        }
        btnOpenYappi.setOnClickListener { openYappiApp(total) }

        val btnYaPague = Button(context).apply {
            text = "Ya realice el pago"
            textSize = 15f
            setTextColor(android.graphics.Color.parseColor("#2E7D32"))
            setBackgroundColor(android.graphics.Color.parseColor("#E8F5E9"))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 12 }
            setPadding(0, 16, 0, 16)
        }

        val btnCancelar = Button(context).apply {
            text = "Cancelar"
            textSize = 14f
            setTextColor(android.graphics.Color.parseColor("#757575"))
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            setPadding(0, 8, 0, 8)
        }

        btnContainer.addView(btnOpenYappi)
        btnContainer.addView(btnYaPague)
        btnContainer.addView(btnCancelar)
        container.addView(btnContainer)

        var dialog: AlertDialog? = null
        btnYaPague.setOnClickListener { dialog?.dismiss(); showYappiConfirmationModal() }
        btnCancelar.setOnClickListener { dialog?.dismiss() }
        dialog = AlertDialog.Builder(this).setView(container).setCancelable(false).create()
        dialog.show()
        return container
    }

    private fun showYappiConfirmationModal() {
        productViewModel.clearCart()
        AlertDialog.Builder(this)
            .setTitle("Pago enviado!")
            .setMessage("En unos minutos tu pedido sera confirmado.\n\nVerifica el estado en Mis Pedidos.")
            .setPositiveButton("Ver mis pedidos") { _, _ -> startActivity(Intent(this, OrdersActivity::class.java)); finish() }
            .setCancelable(false).show()
    }

    private fun addStep(parent: LinearLayout, num: String, label: String, value: String, valueColor: String) {
        val row = LinearLayout(parent.context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 16, 0, 16)
        }
        addStepLabel(row, num, label)
        row.addView(TextView(parent.context).apply {
            text = value
            textSize = 28f
            setTextColor(android.graphics.Color.parseColor(valueColor))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = android.view.Gravity.CENTER
            setPadding(0, 8, 0, 0)
        })
        parent.addView(row)
    }

    private fun addStepLabel(parent: LinearLayout, num: String, label: String) {
        val row = LinearLayout(parent.context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, 8)
        }
        row.addView(TextView(parent.context).apply {
            text = num
            textSize = 12f
            setTextColor(android.graphics.Color.WHITE)
            setBackgroundColor(android.graphics.Color.parseColor("#2E7D32"))
            setPadding(16, 6, 16, 6)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        })
        row.addView(TextView(parent.context).apply {
            text = "  $label"
            textSize = 14f
            setTextColor(android.graphics.Color.parseColor("#757575"))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        })
        parent.addView(row)
    }

    private fun addAmountRow(parent: LinearLayout, label: String, value: String, valueColor: String) {
        val row = LinearLayout(parent.context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 6, 0, 6)
        }
        row.addView(TextView(parent.context).apply {
            text = label
            textSize = 14f
            setTextColor(android.graphics.Color.parseColor("#666666"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        row.addView(TextView(parent.context).apply {
            text = value
            textSize = 14f
            setTextColor(android.graphics.Color.parseColor(valueColor))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        })
        parent.addView(row)
    }

    private fun addDivider(parent: LinearLayout) {
        parent.addView(View(parent.context).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1).apply { topMargin = 8; bottomMargin = 8 }
            setBackgroundColor(android.graphics.Color.parseColor("#E0E0E0"))
        })
    }

    private fun openYappiApp(total: Double) {
        try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("yappi://pay?phone=$YAPPI_PHONE&amount=${"%.2f".format(total)}"))) }
        catch (e: Exception) {
            try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.banistmo.yappy"))) }
            catch (ex: Exception) { Toast.makeText(this, "Descarga YAPPI desde la Play Store", Toast.LENGTH_LONG).show() }
        }
    }

    private fun getProductsTotal(): Double = productViewModel.getCartItemsMap().entries.sumOf { it.key.price * it.value }
    private fun getFinalTotal(): Double = getProductsTotal() + selectedTip

    private fun updateTipUI(activeButton: Button?, btn1: Button, btn2: Button, btn3: Button, etCustomTip: EditText, tvSelectedTip: TextView, layoutSelectedTip: LinearLayout, isCustom: Boolean = false) {
        listOf(btn1, btn2, btn3).forEach { btn ->
            if (btn == activeButton) { btn.setBackgroundColor(ContextCompat.getColor(this, R.color.green_700)); btn.setTextColor(ContextCompat.getColor(this, android.R.color.white)) }
            else { btn.setBackgroundColor(ContextCompat.getColor(this, R.color.gray_100)); btn.setTextColor(ContextCompat.getColor(this, R.color.green_700)) }
        }
        if (isCustom || selectedTip > 0) { layoutSelectedTip.visibility = View.VISIBLE; tvSelectedTip.text = "$${"%.2f".format(selectedTip)}"; etCustomTip.text.clear() }
        else layoutSelectedTip.visibility = View.GONE
    }

    private fun updateTotal(tvSubtotal: TextView, tvTotal: TextView, tvSelectedTip: TextView, layoutSelectedTip: LinearLayout) {
        val subtotal = getProductsTotal()
        tvSubtotal.text = "$${"%.2f".format(subtotal)}"
        tvTotal.text = "$${"%.2f".format(subtotal + selectedTip)}"
        if (selectedTip > 0) { layoutSelectedTip.visibility = View.VISIBLE; tvSelectedTip.text = "$${"%.2f".format(selectedTip)}" }
        else layoutSelectedTip.visibility = View.GONE
    }

    private fun updateCartUI(cartMap: Map<com.agroapp.model.Product, Double>, tvEmpty: TextView, rvCartItems: RecyclerView, tvSubtotal: TextView, tvTotal: TextView, tvSelectedTip: TextView, layoutSelectedTip: LinearLayout) {
        if (cartMap.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE; rvCartItems.visibility = View.GONE
            tvSubtotal.text = "$0.00"; tvTotal.text = "$0.00"; layoutSelectedTip.visibility = View.GONE; selectedTip = 0.0
        } else {
            tvEmpty.visibility = View.GONE; rvCartItems.visibility = View.VISIBLE
            adapter.updateCart(cartMap); updateTotal(tvSubtotal, tvTotal, tvSelectedTip, layoutSelectedTip)
        }
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}