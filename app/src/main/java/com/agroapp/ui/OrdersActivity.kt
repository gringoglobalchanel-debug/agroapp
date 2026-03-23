package com.agroapp.ui

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.agroapp.R
import com.agroapp.viewmodel.OrderViewModel

class OrdersActivity : AppCompatActivity() {

    private val viewModel: OrderViewModel by viewModels()
    private lateinit var adapter: OrdersAdapter
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_orders)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        val rvOrders = findViewById<RecyclerView>(R.id.rvOrders)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val tvEmpty = findViewById<TextView>(R.id.tvEmpty)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)

        setSupportActionBar(toolbar)
        supportActionBar?.title = "Mis Pedidos"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        adapter = OrdersAdapter(
            orders = emptyList(),
            onCancel = { orderId ->
                viewModel.cancelOrder(orderId)
                Toast.makeText(this, "Pedido cancelado", Toast.LENGTH_SHORT).show()
            }
        )
        rvOrders.layoutManager = LinearLayoutManager(this)
        rvOrders.adapter = adapter

        // Configurar SwipeRefreshLayout para refrescar manualmente
        swipeRefreshLayout.setOnRefreshListener {
            loadOrders()
        }

        viewModel.myOrders.observe(this) { orders ->
            progressBar.visibility = View.GONE
            swipeRefreshLayout.isRefreshing = false
            if (orders.isNullOrEmpty()) {
                tvEmpty.visibility = View.VISIBLE
                rvOrders.visibility = View.GONE
            } else {
                tvEmpty.visibility = View.GONE
                rvOrders.visibility = View.VISIBLE
                adapter.updateOrders(orders)
            }
        }

        loadOrders()
    }

    private fun loadOrders() {
        viewModel.loadMyOrders()
    }

    // Recargar pedidos cuando la actividad vuelve a primer plano
    override fun onResume() {
        super.onResume()
        loadOrders()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}