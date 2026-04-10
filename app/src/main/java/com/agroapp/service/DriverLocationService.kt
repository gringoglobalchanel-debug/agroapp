package com.agroapp.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.agroapp.R
import com.agroapp.network.DriverLocationRequest
import com.agroapp.network.RetrofitClient
import com.agroapp.network.SessionManager
import com.google.android.gms.location.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DriverLocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private var currentOrderId: String? = null
    private var destinationLat: Double? = null
    private var destinationLng: Double? = null
    private var arrivedNotified = false
    private val api = RetrofitClient.instance

    companion object {
        const val ACTION_START = "start"
        const val ACTION_STOP = "stop"
        const val EXTRA_ORDER_ID = "order_id"
        const val EXTRA_DEST_LAT = "dest_lat"
        const val EXTRA_DEST_LNG = "dest_lng"
        const val ACTION_ARRIVED = "com.agroapp.DRIVER_ARRIVED"
        const val EXTRA_ARRIVED_ORDER_ID = "arrived_order_id"
        const val ARRIVAL_RADIUS_METERS = 100f
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateIntervalMillis(3000)
            .build()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                currentOrderId = intent.getStringExtra(EXTRA_ORDER_ID)
                destinationLat = intent.getDoubleExtra(EXTRA_DEST_LAT, 0.0).takeIf { it != 0.0 }
                destinationLng = intent.getDoubleExtra(EXTRA_DEST_LNG, 0.0).takeIf { it != 0.0 }
                arrivedNotified = false
                if (currentOrderId != null) {
                    startForeground(1, createNotification().build())
                    startLocationUpdates()
                }
            }
            ACTION_STOP -> stopSelf()
        }
        return START_STICKY
    }

    private fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            stopSelf()
            return
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    sendLocationToServer(location)
                    checkArrival(location)
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun checkArrival(location: Location) {
        if (arrivedNotified) return
        val destLat = destinationLat ?: return
        val destLng = destinationLng ?: return

        val destination = Location("destination").apply {
            latitude = destLat
            longitude = destLng
        }

        val distanceMeters = location.distanceTo(destination)
        if (distanceMeters <= ARRIVAL_RADIUS_METERS) {
            arrivedNotified = true
            val broadcast = Intent(ACTION_ARRIVED).apply {
                putExtra(EXTRA_ARRIVED_ORDER_ID, currentOrderId)
                setPackage(packageName)
            }
            sendBroadcast(broadcast)
        }
    }

    private fun sendLocationToServer(location: Location) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val token = SessionManager.getToken()
                val request = DriverLocationRequest(
                    orderId = currentOrderId!!,
                    latitude = location.latitude,
                    longitude = location.longitude
                )
                api.updateDriverLocation(token, request)
            } catch (e: Exception) { }
        }
    }

    private fun createNotification(): NotificationCompat.Builder {
        return NotificationCompat.Builder(this, "delivery_channel")
            .setContentTitle("Compartiendo ubicación")
            .setContentText("Tu ubicación se comparte con el cliente")
            .setSmallIcon(android.R.drawable.ic_dialog_map)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "delivery_channel",
                "Servicio de entrega",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    override fun onBind(intent: Intent): IBinder? = null
}