package com.agroapp.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.agroapp.R
import com.agroapp.network.RetrofitClient
import com.agroapp.network.SessionManager
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Timer
import java.util.TimerTask

class OrderTrackingActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private var driverMarker: Marker? = null
    private var orderId: String = ""
    private var destinationLat: Double = 0.0
    private var destinationLng: Double = 0.0
    private val api = RetrofitClient.instance
    private var timer: Timer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_tracking)

        orderId = intent.getStringExtra("order_id") ?: ""
        destinationLat = intent.getDoubleExtra("destination_lat", 0.0)
        destinationLng = intent.getDoubleExtra("destination_lng", 0.0)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        val destination = LatLng(destinationLat, destinationLng)
        mMap.addMarker(MarkerOptions()
            .position(destination)
            .title("Destino")
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)))

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(destination, 14f))

        startLocationPolling()
    }

    private fun startLocationPolling() {
        timer = Timer()
        timer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                fetchDriverLocation()
            }
        }, 0, 5000)
    }

    private fun fetchDriverLocation() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val token = SessionManager.getToken()
                val response = api.getDriverLocation(token, orderId)

                withContext(Dispatchers.Main) {
                    response.body()?.let { location ->
                        if (location.latitude != null && location.longitude != null) {
                            updateDriverMarker(location.latitude!!, location.longitude!!)
                        }
                    }
                }
            } catch (e: Exception) {
                // Error
            }
        }
    }

    private fun updateDriverMarker(lat: Double, lng: Double) {
        val driverPos = LatLng(lat, lng)

        if (driverMarker == null) {
            driverMarker = mMap.addMarker(MarkerOptions()
                .position(driverPos)
                .title("Conductor")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)))
        } else {
            driverMarker?.position = driverPos
        }

        driverMarker?.let {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(driverPos, 15f))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
    }
}