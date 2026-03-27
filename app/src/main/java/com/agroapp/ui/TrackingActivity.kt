package com.agroapp.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.agroapp.R
import com.agroapp.network.RetrofitClient
import com.agroapp.network.SessionManager
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TrackingActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private var driverMarker: Marker? = null
    private var destinationMarker: Marker? = null

    private var orderId: Int = 0
    private var driverId: Int = 0
    private var orderTotal: Double = 0.0
    private var deliveryLat: Double = 0.0
    private var deliveryLng: Double = 0.0

    private var isPolling = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tracking)

        orderId    = intent.getIntExtra("order_id", 0)
        driverId   = intent.getIntExtra("driver_id", 0)
        orderTotal = intent.getDoubleExtra("order_total", 0.0)
        deliveryLat = intent.getDoubleExtra("delivery_lat", 0.0)
        deliveryLng = intent.getDoubleExtra("delivery_lng", 0.0)

        setupUI()
        setupMap()
        startPolling()
    }

    private fun setupUI() {
        findViewById<TextView>(R.id.tvOrderId).text = "#$orderId"
        findViewById<TextView>(R.id.tvTotal).text = "$${"%.2f".format(orderTotal)}"
        findViewById<TextView>(R.id.tvDeliveryFee).text = "$2.50"
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
    }

    private fun setupMap() {
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.uiSettings.isZoomControlsEnabled = false
        googleMap.uiSettings.isMapToolbarEnabled = false

        if (deliveryLat != 0.0 && deliveryLng != 0.0) {
            val destLatLng = LatLng(deliveryLat, deliveryLng)
            destinationMarker = googleMap.addMarker(
                MarkerOptions()
                    .position(destLatLng)
                    .title("Tu dirección de entrega")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            )
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(destLatLng, 15f))
        }
    }

    private fun startPolling() {
        lifecycleScope.launch {
            while (isPolling) {
                try {
                    val response = RetrofitClient.instance.getDriverLocationByDriver(
                        token = SessionManager.getToken(),
                        driverId = driverId
                    )
                    if (response.isSuccessful) {
                        response.body()?.let { loc ->
                            val lat = loc.latitude
                            val lng = loc.longitude
                            if (lat != null && lng != null) {
                                updateDriverMarker(lat, lng)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("TrackingActivity", "Error polling ubicación: ${e.message}")
                }
                delay(5000)
            }
        }
    }

    private fun updateDriverMarker(lat: Double, lng: Double) {
        runOnUiThread {
            val driverLatLng = LatLng(lat, lng)
            if (driverMarker == null) {
                driverMarker = googleMap.addMarker(
                    MarkerOptions()
                        .position(driverLatLng)
                        .title("Repartidor")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                )
                zoomToFitBothMarkers(driverLatLng)
            } else {
                driverMarker?.position = driverLatLng
            }
        }
    }

    private fun zoomToFitBothMarkers(driverLatLng: LatLng) {
        if (deliveryLat == 0.0 || deliveryLng == 0.0) return
        val destLatLng = LatLng(deliveryLat, deliveryLng)
        val bounds = LatLngBounds.builder()
            .include(driverLatLng)
            .include(destLatLng)
            .build()
        try {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 120))
        } catch (e: Exception) {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(driverLatLng, 14f))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isPolling = false
    }
}