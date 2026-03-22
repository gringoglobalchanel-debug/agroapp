package com.agroapp.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cart)

        // Inicializar vistas
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        val rvCartItems = findViewById<RecyclerView>(R.id.rvCartItems)
        val tvTotal = findViewById<TextView>(R.id.tvTotal)
        val tvEmpty = findViewById<TextView>(R.id.tvEmpty)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val rgPaymentMethod = findViewById<RadioGroup>(R.id.rgPaymentMethod)
        val btnConfirmOrder = findViewById<Button>(R.id.btnConfirmOrder)

        // Configurar Stripe
        PaymentConfiguration.init(this, "pk_test_51TCsa5BLWgq8LL5oSzSEf91oRnuyWGUUUhLtsA4dH4ZZC3LgqquMV4F63yg2GJ94wVqFaZWAPACC8xYHuQEHTGBY00qafIlQAu")
        paymentSheet = PaymentSheet(this, ::onPaymentSheetResult)

        // Configurar toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Mi Carrito"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Configurar adaptador
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

        // Observar cambios en el carrito
        productViewModel.cart.observe(this, Observer { cartMap ->
            updateCartUI(cartMap ?: emptyMap(), tvTotal, tvEmpty, rvCartItems)
        })

        // Botón confirmar pedido
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
                    orderViewModel.createOrder(cartItems, paymentMethod, deliveryAddress, null)
                }
            }
        }

        // Observar estado del pedido
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

    private fun processCardPayment() {
        val cartItems = productViewModel.getCartItemsMap()
        val total = cartItems.entries.sumOf { it.key.price * it.value }

        if (total <= 0) {
            Toast.makeText(this, "Monto inválido", Toast.LENGTH_SHORT).show()
            return
        }

        orderViewModel.createPaymentIntent(total) { success, secret ->
            if (success && secret != null) {
                clientSecret = secret
                presentPaymentSheet()
            } else {
                Toast.makeText(this, "Error iniciando pago", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun processYappiPayment() {
        val cartItems = productViewModel.getCartItemsMap()
        val total = cartItems.entries.sumOf { it.key.price * it.value }

        if (total <= 0) {
            Toast.makeText(this, "Monto inválido", Toast.LENGTH_SHORT).show()
            return
        }

        pendingTotal = total

        // Mostrar progreso
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        progressBar.visibility = View.VISIBLE

        // Crear pedido pendiente con código de referencia
        val deliveryAddress = SessionManager.getAddress()
        orderViewModel.createPendingYappiOrder(cartItems, deliveryAddress) { success, orderId, referenceCode ->
            progressBar.visibility = View.GONE

            if (success && orderId != null && referenceCode != null) {
                pendingYappiOrderId = orderId
                pendingYappiCode = referenceCode

                // Mostrar diálogo con instrucciones y código de referencia
                showYappiPaymentDialog(total, referenceCode, orderId)
            } else {
                Toast.makeText(this, "Error al procesar el pedido. Intenta de nuevo.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showYappiPaymentDialog(total: Double, referenceCode: String, orderId: String) {
        val yappiPhone = "50760000000"  // ← CAMBIA AQUÍ TU NÚMERO DE YAPPI

        AlertDialog.Builder(this)
            .setTitle("Pagar con YAPPI")
            .setMessage("""
                Por favor, realiza el pago con YAPPI:
                
                📱 Número: $yappiPhone
                💰 Monto: $${"%.2f".format(total)}
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
                // Mostrar diálogo para reintentar
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
        val yappiPhone = "50765422618"  // ← CAMBIA AQUÍ TU NÚMERO DE YAPPI
        val description = "Compra AgroApp - $referenceCode"
        val yappiUrl = "yappi://pay?phone=$yappiPhone&amount=${"%.2f".format(total)}&description=$description"

        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(yappiUrl))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Debes tener YAPPI instalado", Toast.LENGTH_SHORT).show()
            // Abrir Play Store para descargar YAPPI
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
                orderViewModel.createOrder(cartItems, "card", deliveryAddress, clientSecret)
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
        tvTotal: TextView,
        tvEmpty: TextView,
        rvCartItems: RecyclerView
    ) {
        if (cartMap.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            rvCartItems.visibility = View.GONE
            tvTotal.text = "Total: $0.00"
        } else {
            tvEmpty.visibility = View.GONE
            rvCartItems.visibility = View.VISIBLE
            adapter.updateCart(cartMap)

            val total = cartMap.entries.sumOf { it.key.price * it.value }
            tvTotal.text = "Total: $${String.format("%.2f", total)}"
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}