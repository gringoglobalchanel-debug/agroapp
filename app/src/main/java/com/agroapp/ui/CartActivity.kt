package com.agroapp.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.agroapp.R
import com.agroapp.network.SessionManager
import com.agroapp.viewmodel.OrderState
import com.agroapp.viewmodel.OrderViewModel
import com.agroapp.viewmodel.ProductViewModel
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult

class CartActivity : AppCompatActivity() {

    private val orderViewModel: OrderViewModel by viewModels()
    private val productViewModel: ProductViewModel by viewModels()
    private lateinit var adapter: CartAdapter
    private lateinit var paymentSheet: PaymentSheet
    private var clientSecret: String? = null
    private var pendingYappiOrderId: String? = null
    private var pendingYappiCode: String? = null
    private var pendingTotal: Double = 0.0
    private var selectedTip: Double = 0.0

    companion object {
        const val DELIVERY_FEE = 2.50
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
        val tvDeliveryFee = findViewById<TextView>(R.id.tvDeliveryFee)
        val tvTotal = findViewById<TextView>(R.id.tvTotal)
        val tvSelectedTip = findViewById<TextView>(R.id.tvSelectedTip)
        val layoutSelectedTip = findViewById<LinearLayout>(R.id.layoutSelectedTip)

        // Botones de propina
        val btnTip1 = findViewById<Button>(R.id.btnTip1)
        val btnTip3 = findViewById<Button>(R.id.btnTip3)
        val btnTip5 = findViewById<Button>(R.id.btnTip5)
        val etCustomTip = findViewById<EditText>(R.id.etCustomTip)
        val btnApplyCustomTip = findViewById<Button>(R.id.btnApplyCustomTip)

        PaymentConfiguration.init(this, "pk_test_51TCsa5BLWgq8LL5oSzSEf91oRnuyWGUUUhLtsA4dH4ZZC3LgqquMV4F63yg2GJ94wVqFaZWAPACC8xYHuQEHTGBY00qafIlQAu")
        paymentSheet = PaymentSheet(this, ::onPaymentSheetResult)

        setSupportActionBar(toolbar)
        supportActionBar?.title = "Mi Carrito"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        tvDeliveryFee.text = "$${"%.2f".format(DELIVERY_FEE)}"

        adapter = CartAdapter(
            cartItems = emptyMap(),
            onQuantityChange = { product, newQuantity ->
                productViewModel.updateQuantity(product, newQuantity)
            },
            onRemove = { product ->
                productViewModel.removeFromCart(product)
            }
        )
        rvCartItems.layoutManager = LinearLayoutManager(this)
        rvCartItems.adapter = adapter

        productViewModel.cart.observe(this, Observer { cartMap ->
            updateCartUI(cartMap ?: emptyMap(), tvEmpty, rvCartItems, tvSubtotal, tvTotal, tvSelectedTip, layoutSelectedTip)
        })

        // Botones de propina
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
                    Toast.makeText(this, "Ingresa un monto válido", Toast.LENGTH_SHORT).show()
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

            if (cartItems.isEmpty()) {
                Toast.makeText(this, "El carrito está vacío", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            when (paymentMethod) {
                "card" -> processCardPayment()
                "yappi" -> processYappiPayment()
                else -> {
                    val deliveryAddress = SessionManager.getAddress()
                    orderViewModel.createOrder(cartItems, paymentMethod, deliveryAddress, null, selectedTip)
                }
            }
        }

        orderViewModel.orderState.observe(this, Observer { state ->
            when (state) {
                is OrderState.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    btnConfirmOrder.isEnabled = false
                }
                is OrderState.Success -> {
                    progressBar.visibility = View.GONE
                    btnConfirmOrder.isEnabled = true
                    pendingYappiOrderId = null
                    pendingYappiCode = null
                    Toast.makeText(this, "¡Pedido confirmado!", Toast.LENGTH_LONG).show()
                    productViewModel.clearCart()
                    finish()
                }
                is OrderState.Error -> {
                    progressBar.visibility = View.GONE
                    btnConfirmOrder.isEnabled = true
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
                is OrderState.OutOfTime -> {
                    progressBar.visibility = View.GONE
                    btnConfirmOrder.isEnabled = true
                    Toast.makeText(this, "Pedidos solo de 8am a 12pm", Toast.LENGTH_LONG).show()
                }
                else -> {
                    progressBar.visibility = View.GONE
                    btnConfirmOrder.isEnabled = true
                }
            }
        })
    }

    private fun getProductsTotal(): Double {
        val cartItems = productViewModel.getCartItemsMap()
        return cartItems.entries.sumOf { it.key.price * it.value }
    }

    private fun getFinalTotal(): Double = getProductsTotal() + DELIVERY_FEE + selectedTip

    private fun updateTipUI(
        activeButton: Button?,
        btn1: Button,
        btn2: Button,
        btn3: Button,
        etCustomTip: EditText,
        tvSelectedTip: TextView,
        layoutSelectedTip: LinearLayout,
        isCustom: Boolean = false
    ) {
        val buttons = listOf(btn1, btn2, btn3)
        buttons.forEach { btn ->
            if (btn == activeButton) {
                btn.setBackgroundColor(ContextCompat.getColor(this, R.color.green_700))
                btn.setTextColor(ContextCompat.getColor(this, android.R.color.white))
            } else {
                btn.setBackgroundColor(ContextCompat.getColor(this, R.color.gray_100))
                btn.setTextColor(ContextCompat.getColor(this, R.color.green_700))
            }
        }

        if (isCustom || selectedTip > 0) {
            layoutSelectedTip.visibility = View.VISIBLE
            tvSelectedTip.text = "$${"%.2f".format(selectedTip)}"
            etCustomTip.text.clear()
        } else {
            layoutSelectedTip.visibility = View.GONE
        }
    }

    private fun updateTotal(
        tvSubtotal: TextView,
        tvTotal: TextView,
        tvSelectedTip: TextView,
        layoutSelectedTip: LinearLayout
    ) {
        val subtotal = getProductsTotal()
        tvSubtotal.text = "$${"%.2f".format(subtotal)}"

        val total = subtotal + DELIVERY_FEE + selectedTip
        tvTotal.text = "$${"%.2f".format(total)}"

        if (selectedTip > 0) {
            layoutSelectedTip.visibility = View.VISIBLE
            tvSelectedTip.text = "$${"%.2f".format(selectedTip)}"
        } else {
            layoutSelectedTip.visibility = View.GONE
        }
    }

    private fun processCardPayment() {
        val finalTotal = getFinalTotal()

        if (finalTotal <= 0) {
            Toast.makeText(this, "Monto inválido", Toast.LENGTH_SHORT).show()
            return
        }

        orderViewModel.createPaymentIntent(finalTotal) { success, secret ->
            if (success && secret != null) {
                clientSecret = secret
                presentPaymentSheet()
            } else {
                Toast.makeText(this, "Error iniciando pago", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun processYappiPayment() {
        val finalTotal = getFinalTotal()

        if (finalTotal <= 0) {
            Toast.makeText(this, "Monto inválido", Toast.LENGTH_SHORT).show()
            return
        }

        pendingTotal = finalTotal

        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        progressBar.visibility = View.VISIBLE

        val cartItems = productViewModel.getCartItemsMap()
        val deliveryAddress = SessionManager.getAddress()
        orderViewModel.createPendingYappiOrder(cartItems, deliveryAddress) { success, orderId, referenceCode ->
            progressBar.visibility = View.GONE

            if (success && orderId != null && referenceCode != null) {
                pendingYappiOrderId = orderId
                pendingYappiCode = referenceCode
                showYappiPaymentDialog(finalTotal, referenceCode, orderId)
            } else {
                Toast.makeText(this, "Error al procesar el pedido. Intenta de nuevo.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showYappiPaymentDialog(total: Double, referenceCode: String, orderId: String) {
        val yappiPhone = "50760000000"
        val productsTotal = total - DELIVERY_FEE - selectedTip

        AlertDialog.Builder(this)
            .setTitle("Pagar con YAPPI")
            .setMessage("""
                Por favor, realiza el pago con YAPPI:
                
                📱 Número: $yappiPhone
                🛒 Productos: $${"%.2f".format(productsTotal)}
                🚚 Envío: $${"%.2f".format(DELIVERY_FEE)}
                💰 Propina: $${"%.2f".format(selectedTip)}
                💵 Total: $${"%.2f".format(total)}
                📝 Referencia: $referenceCode
                
                Luego presiona "YA PAGUÉ" para confirmar tu pedido.
            """.trimIndent())
            .setPositiveButton("YA PAGUÉ") { _, _ ->
                verifyYappiPayment(orderId, referenceCode)
            }
            .setNegativeButton("Cancelar") { _, _ ->
                Toast.makeText(this, "Pedido cancelado", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("Abrir YAPPI") { _, _ ->
                openYappi(total, referenceCode)
            }
            .setCancelable(false)
            .show()
    }

    private fun verifyYappiPayment(orderId: String, referenceCode: String) {
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        progressBar.visibility = View.VISIBLE

        orderViewModel.confirmYappiPayment(orderId, referenceCode) { success, message ->
            progressBar.visibility = View.GONE

            if (success) {
                Toast.makeText(this, "¡Pedido confirmado! Gracias por tu compra.", Toast.LENGTH_LONG).show()
                productViewModel.clearCart()
                finish()
            } else {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                showPaymentRetryDialog(orderId, referenceCode)
            }
        }
    }

    private fun showPaymentRetryDialog(orderId: String, referenceCode: String) {
        AlertDialog.Builder(this)
            .setTitle("¿Problemas con el pago?")
            .setMessage("No pudimos confirmar tu pago. ¿Ya realizaste el pago con YAPPI?")
            .setPositiveButton("YA PAGUÉ, VERIFICAR") { _, _ ->
                verifyYappiPayment(orderId, referenceCode)
            }
            .setNegativeButton("Cancelar pedido") { _, _ ->
                Toast.makeText(this, "Pedido cancelado", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("Pagar ahora") { _, _ ->
                openYappi(pendingTotal, referenceCode)
            }
            .show()
    }

    private fun openYappi(total: Double, referenceCode: String) {
        val yappiPhone = "50765422618"
        val description = "Compra AgroApp - $referenceCode"
        val yappiUrl = "yappi://pay?phone=$yappiPhone&amount=${"%.2f".format(total)}&description=$description"

        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(yappiUrl))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Debes tener YAPPI instalado", Toast.LENGTH_SHORT).show()
            try {
                val playStore = Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=com.banistmo.yappy"))
                startActivity(playStore)
            } catch (ex: Exception) {
                Toast.makeText(this, "Descarga YAPPI desde la Play Store", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun presentPaymentSheet() {
        clientSecret?.let { secret ->
            val configuration = PaymentSheet.Configuration("AgroApp Grün")
            paymentSheet.presentWithPaymentIntent(secret, configuration)
        }
    }

    private fun onPaymentSheetResult(paymentSheetResult: PaymentSheetResult) {
        when (paymentSheetResult) {
            is PaymentSheetResult.Completed -> {
                val cartItems = productViewModel.getCartItemsMap()
                val deliveryAddress = SessionManager.getAddress()
                orderViewModel.createOrder(cartItems, "card", deliveryAddress, clientSecret, selectedTip)
            }
            is PaymentSheetResult.Canceled -> {
                Toast.makeText(this, "Pago cancelado", Toast.LENGTH_SHORT).show()
            }
            is PaymentSheetResult.Failed -> {
                Toast.makeText(this, "Error: ${paymentSheetResult.error.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun updateCartUI(
        cartMap: Map<com.agroapp.model.Product, Double>,
        tvEmpty: TextView,
        rvCartItems: RecyclerView,
        tvSubtotal: TextView,
        tvTotal: TextView,
        tvSelectedTip: TextView,
        layoutSelectedTip: LinearLayout
    ) {
        if (cartMap.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            rvCartItems.visibility = View.GONE
            tvSubtotal.text = "$0.00"
            tvTotal.text = "$0.00"
            layoutSelectedTip.visibility = View.GONE
            selectedTip = 0.0
        } else {
            tvEmpty.visibility = View.GONE
            rvCartItems.visibility = View.VISIBLE
            adapter.updateCart(cartMap)
            updateTotal(tvSubtotal, tvTotal, tvSelectedTip, layoutSelectedTip)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}