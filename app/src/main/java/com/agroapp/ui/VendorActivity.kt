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
import com.agroapp.viewmodel.VendorViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class VendorActivity : AppCompatActivity() {

    private val viewModel: VendorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vendor)

        // Inicializar vistas
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        val tvDeliveryDate = findViewById<TextView>(R.id.tvDeliveryDate)
        val btnByClient = findViewById<Button>(R.id.btnByClient)
        val btnByProduct = findViewById<Button>(R.id.btnByProduct)
        val rvOrders = findViewById<RecyclerView>(R.id.rvOrders)
        val rvProducts = findViewById<RecyclerView>(R.id.rvProducts)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val btnLogout = findViewById<Button>(R.id.btnLogout)

        // Configurar Toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Panel de Vendedor"

        // Mostrar nombre del usuario
        val userName = SessionManager.getUserName()
        if (userName.isNotEmpty()) {
            supportActionBar?.subtitle = "Bienvenido, $userName"
        }

        // Configurar fecha
        val tomorrow = LocalDate.now().plusDays(1)
            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        tvDeliveryDate.text = "Fecha de entrega: $tomorrow"

        // Configurar RecyclerViews
        rvOrders.layoutManager = LinearLayoutManager(this)
        rvProducts.layoutManager = LinearLayoutManager(this)

        // Botones
        btnByClient.setOnClickListener {
            rvOrders.visibility = View.VISIBLE
            rvProducts.visibility = View.GONE
            viewModel.loadOrdersByClient()

            btnByClient.setBackgroundColor(resources.getColor(R.color.green_700, theme))
            btnByProduct.setBackgroundColor(resources.getColor(R.color.gray_500, theme))
        }

        btnByProduct.setOnClickListener {
            rvOrders.visibility = View.GONE
            rvProducts.visibility = View.VISIBLE
            viewModel.loadOrdersByProduct()

            btnByProduct.setBackgroundColor(resources.getColor(R.color.green_700, theme))
            btnByClient.setBackgroundColor(resources.getColor(R.color.gray_500, theme))
        }

        // Botón Logout - AHORA USA logout() SIN CONTEXTO
        btnLogout.setOnClickListener {
            SessionManager.logout()  // ¡Así de simple!
            Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        // Observar loading
        viewModel.loading.observe(this, Observer { isLoading ->
            progressBar.visibility = if (isLoading == true) View.VISIBLE else View.GONE
        })

        // Observar datos
        viewModel.ordersByClient.observe(this, Observer { orders ->
            if (orders != null) {
                Toast.makeText(this, "Pedidos: ${orders.size}", Toast.LENGTH_SHORT).show()
            }
        })

        viewModel.ordersByProduct.observe(this, Observer { orders ->
            if (orders != null) {
                Toast.makeText(this, "Productos: ${orders.size}", Toast.LENGTH_SHORT).show()
            }
        })

        // Cargar datos iniciales
        viewModel.loadOrdersByClient()
    }
}