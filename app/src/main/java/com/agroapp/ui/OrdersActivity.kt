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
import com.agroapp.R
import com.agroapp.viewmodel.OrderViewModel

class OrdersActivity : AppCompatActivity() {

    private val viewModel: OrderViewModel by viewModels()
    private lateinit var adapter: OrdersAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_orders)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        val rvOrders = findViewById<RecyclerView>(R.id.rvOrders)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val tvEmpty = findViewById<TextView>(R.id.tvEmpty)

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

        viewModel.myOrders.observe(this) { orders ->
            progressBar.visibility = View.GONE
            if (orders.isNullOrEmpty()) {
                tvEmpty.visibility = View.VISIBLE
                rvOrders.visibility = View.GONE
            } else {
                tvEmpty.visibility = View.GONE
                rvOrders.visibility = View.VISIBLE
                adapter.updateOrders(orders)
            }
        }

        progressBar.visibility = View.VISIBLE
        viewModel.loadMyOrders()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}