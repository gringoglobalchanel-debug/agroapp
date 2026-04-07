package com.agroapp.ui

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.agroapp.R
import com.agroapp.network.RetrofitClient
import com.agroapp.network.SessionManager
import com.bumptech.glide.Glide
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.material.imageview.ShapeableImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.*

class OrderTrackingActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private var driverMarker: Marker? = null
    private var destinationMarker: Marker? = null
    private var routePolyline: Polyline? = null

    private lateinit var tvStatus: TextView
    private lateinit var tvStatusBadge: TextView
    private lateinit var tvEta: TextView
    private lateinit var tvDistance: TextView
    private lateinit var tvLastUpdate: TextView
    private lateinit var ivStatusIcon: ImageView
    private lateinit var btnBack: ImageButton

    // Driver card
    private lateinit var cardDriver: CardView
    private lateinit var ivDriverAvatar: ShapeableImageView
    private lateinit var tvDriverName: TextView

    private var orderId: String = ""
    private var destinationLat: Double = 0.0
    private var destinationLng: Double = 0.0

    private val api = RetrofitClient.instance
    private val handler = Handler(Looper.getMainLooper())
    private var isMapReady = false
    private var driverInfoLoaded = false

    private val pollingRunnable = object : Runnable {
        override fun run() {
            fetchDriverLocation()
            fetchOrderStatus()
            handler.postDelayed(this, 5000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_tracking)

        orderId = intent.getStringExtra("order_id") ?: ""
        destinationLat = intent.getDoubleExtra("destination_lat", 0.0)
        destinationLng = intent.getDoubleExtra("destination_lng", 0.0)

        bindViews()
        setupMap()
    }

    private fun bindViews() {
        tvStatus = findViewById(R.id.tvStatus)
        tvStatusBadge = findViewById(R.id.tvStatusBadge)
        tvEta = findViewById(R.id.tvEta)
        tvDistance = findViewById(R.id.tvDistance)
        tvLastUpdate = findViewById(R.id.tvLastUpdate)
        ivStatusIcon = findViewById(R.id.ivStatusIcon)
        btnBack = findViewById(R.id.btnBack)
        cardDriver = findViewById(R.id.cardDriver)
        ivDriverAvatar = findViewById(R.id.ivDriverAvatar)
        tvDriverName = findViewById(R.id.tvDriverName)

        btnBack.setOnClickListener { finish() }
    }

    private fun setupMap() {
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        isMapReady = true

        mMap.uiSettings.isZoomControlsEnabled = false
        mMap.uiSettings.isMyLocationButtonEnabled = false

        val destination = LatLng(destinationLat, destinationLng)
        destinationMarker = mMap.addMarker(
            MarkerOptions()
                .position(destination)
                .title("Tu dirección")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
        )
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(destination, 14f))
        handler.post(pollingRunnable)
    }

    // ==================== FETCH UBICACIÓN + INFO DRIVER ====================

    private fun fetchDriverLocation() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val token = SessionManager.getToken()
                val response = api.getDriverLocation(token, orderId)

                withContext(Dispatchers.Main) {
                    response.body()?.let { location ->
                        if (location.latitude != null && location.longitude != null) {
                            updateDriverMarker(location.latitude, location.longitude)
                            updateEtaAndDistance(location.latitude, location.longitude)
                            updateLastUpdateTime()
                        }

                        // Mostrar info del driver si viene en la respuesta
                        if (!driverInfoLoaded) {
                            val driverName = location.driverName
                            val driverAvatar = location.driverAvatar
                            if (!driverName.isNullOrEmpty()) {
                                showDriverCard(driverName, driverAvatar)
                                driverInfoLoaded = true
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Silencioso
            }
        }
    }

    private fun showDriverCard(name: String, avatarUrl: String?) {
        cardDriver.visibility = View.VISIBLE
        tvDriverName.text = name

        if (!avatarUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(avatarUrl)
                .placeholder(R.drawable.ic_default_avatar)
                .error(R.drawable.ic_default_avatar)
                .circleCrop()
                .into(ivDriverAvatar)
        } else {
            ivDriverAvatar.setImageResource(R.drawable.ic_default_avatar)
        }
    }

    // ==================== FETCH ESTADO DEL PEDIDO ====================

    private fun fetchOrderStatus() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val token = SessionManager.getToken()
                val response = api.getActiveOrder(token)
                withContext(Dispatchers.Main) {
                    response.body()?.let { order ->
                        updateStatusUI(order.status ?: "pending")
                    }
                }
            } catch (e: Exception) { }
        }
    }

    // ==================== MARCADOR DRIVER ====================

    private fun updateDriverMarker(lat: Double, lng: Double) {
        if (!isMapReady) return
        val driverPos = LatLng(lat, lng)
        if (driverMarker == null) {
            driverMarker = mMap.addMarker(
                MarkerOptions()
                    .position(driverPos)
                    .title("Repartidor")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                    .zIndex(1f)
            )
            centerCameraBetweenPoints(driverPos, LatLng(destinationLat, destinationLng))
        } else {
            driverMarker?.position = driverPos
        }
        drawRouteLine(driverPos, LatLng(destinationLat, destinationLng))
    }

    private fun drawRouteLine(origin: LatLng, destination: LatLng) {
        routePolyline?.remove()
        routePolyline = mMap.addPolyline(
            PolylineOptions()
                .add(origin).add(destination)
                .width(8f)
                .color(Color.parseColor("#1B5E20"))
                .geodesic(true)
        )
    }

    private fun centerCameraBetweenPoints(point1: LatLng, point2: LatLng) {
        val centerLat = (point1.latitude + point2.latitude) / 2
        val centerLng = (point1.longitude + point2.longitude) / 2
        val distanceKm = calculateDistanceKm(point1.latitude, point1.longitude, point2.latitude, point2.longitude)
        val zoom = when {
            distanceKm < 0.5 -> 16f
            distanceKm < 1.0 -> 15f
            distanceKm < 2.0 -> 14f
            distanceKm < 5.0 -> 13f
            else -> 12f
        }
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(centerLat, centerLng), zoom))
    }

    // ==================== ETA Y DISTANCIA ====================

    private fun updateEtaAndDistance(driverLat: Double, driverLng: Double) {
        val distanceKm = calculateDistanceKm(driverLat, driverLng, destinationLat, destinationLng)
        val distanceText = if (distanceKm < 1.0) "${(distanceKm * 1000).toInt()} m" else String.format("%.1f km", distanceKm)
        val etaMinutes = ((distanceKm / 25.0) * 60).toInt()
        val etaText = when {
            etaMinutes <= 1 -> "~1 min"
            etaMinutes < 60 -> "~$etaMinutes min"
            else -> "~${etaMinutes / 60}h ${etaMinutes % 60}min"
        }
        tvDistance.text = distanceText
        tvEta.text = etaText
    }

    private fun calculateDistanceKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val R = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
        return R * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    // ==================== UI ESTADO ====================

    private fun updateStatusUI(status: String) {
        when (status) {
            "pending" -> {
                tvStatus.text = "Preparando tu pedido"
                tvStatusBadge.text = "Pendiente"
                tvStatusBadge.setBackgroundColor(Color.parseColor("#FF9800"))
            }
            "in_progress" -> {
                tvStatus.text = "El repartidor está en camino"
                tvStatusBadge.text = "En camino"
                tvStatusBadge.setBackgroundColor(Color.parseColor("#1B5E20"))
            }
            "completed" -> {
                tvStatus.text = "¡Pedido entregado!"
                tvStatusBadge.text = "Entregado ✓"
                tvStatusBadge.setBackgroundColor(Color.parseColor("#4CAF50"))
                handler.removeCallbacks(pollingRunnable)
                tvEta.text = "Entregado"
                tvDistance.text = "0 m"
            }
            "cancelled" -> {
                tvStatus.text = "Pedido cancelado"
                tvStatusBadge.text = "Cancelado"
                tvStatusBadge.setBackgroundColor(Color.parseColor("#F44336"))
                handler.removeCallbacks(pollingRunnable)
            }
            else -> {
                tvStatus.text = "Procesando pedido"
                tvStatusBadge.text = status
                tvStatusBadge.setBackgroundColor(Color.parseColor("#9E9E9E"))
            }
        }
    }

    private fun updateLastUpdateTime() {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        tvLastUpdate.text = "Última actualización: ${sdf.format(Date())}"
    }

    override fun onResume() {
        super.onResume()
        if (isMapReady) handler.post(pollingRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(pollingRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(pollingRunnable)
    }
}