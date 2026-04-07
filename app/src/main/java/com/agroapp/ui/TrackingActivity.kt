package com.agroapp.ui

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.agroapp.R
import com.agroapp.network.RetrofitClient
import com.agroapp.network.SessionManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.imageview.ShapeableImageView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class TrackingActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private var driverMarker: Marker? = null
    private var destinationMarker: Marker? = null

    private var orderId: String = ""
    private var driverId: String = ""
    private var orderTotal: Double = 0.0
    private var deliveryLat: Double = 0.0
    private var deliveryLng: Double = 0.0

    private var isPolling = true

    // ✅ NUEVO: referencias UI del driver
    private lateinit var ivDriverAvatar: ShapeableImageView
    private lateinit var tvDriverName: TextView
    private lateinit var layoutDriverInfo: View
    private lateinit var dividerDriver: View

    // ✅ Para no recargar avatar en cada poll si ya se cargó
    private var driverInfoLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tracking)

        orderId     = intent.getStringExtra("order_id") ?: ""
        driverId    = intent.getStringExtra("driver_id") ?: ""
        orderTotal  = intent.getDoubleExtra("order_total", 0.0)
        deliveryLat = intent.getDoubleExtra("delivery_lat", 0.0)
        deliveryLng = intent.getDoubleExtra("delivery_lng", 0.0)

        // ✅ NUEVO: bind vistas del driver
        ivDriverAvatar  = findViewById(R.id.ivDriverAvatar)
        tvDriverName    = findViewById(R.id.tvDriverName)
        layoutDriverInfo = findViewById(R.id.layoutDriverInfo)
        dividerDriver   = findViewById(R.id.dividerDriver)

        setupUI()
        setupMap()
        startPolling()
    }

    private fun setupUI() {
        findViewById<TextView>(R.id.tvOrderId).text = "#${orderId.take(8).uppercase()}"
        findViewById<TextView>(R.id.tvTotal).text = "$${"%.2f".format(orderTotal)}"
        findViewById<TextView>(R.id.tvDeliveryFee).text = "GRATIS"
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
        if (driverId.isEmpty()) {
            Log.e("TrackingActivity", "driverId vacío, no se puede rastrear")
            return
        }

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
                            if (lat != null && lng != null && lat != 0.0 && lng != 0.0) {
                                updateDriverMarker(lat, lng)
                            }
                            // ✅ NUEVO: mostrar info del driver solo la primera vez
                            if (!driverInfoLoaded) {
                                val name = loc.driverName
                                val avatar = loc.driverAvatar
                                if (!name.isNullOrEmpty()) {
                                    showDriverInfo(name, avatar)
                                    driverInfoLoaded = true
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("TrackingActivity", "Error polling: ${e.message}")
                }
                delay(5000)
            }
        }
    }

    // ✅ NUEVO: muestra la sección del driver con foto y nombre
    private fun showDriverInfo(name: String, avatarUrl: String?) {
        runOnUiThread {
            tvDriverName.text = name
            layoutDriverInfo.visibility = View.VISIBLE
            dividerDriver.visibility = View.VISIBLE

            if (!avatarUrl.isNullOrEmpty()) {
                Glide.with(this)
                    .load(avatarUrl)
                    .placeholder(R.drawable.ic_default_avatar)
                    .error(R.drawable.ic_default_avatar)
                    .skipMemoryCache(true)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .circleCrop()
                    .into(ivDriverAvatar)
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
                        .title("Tu repartidor")
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