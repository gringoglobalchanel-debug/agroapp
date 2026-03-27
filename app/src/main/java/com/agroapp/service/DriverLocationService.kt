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
    private val api = RetrofitClient.instance

    companion object {
        const val ACTION_START = "start"
        const val ACTION_STOP = "stop"
        const val EXTRA_ORDER_ID = "order_id"
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
                if (currentOrderId != null) {
                    val notification = createNotification().build()
                    startForeground(1, notification)
                    startLocationUpdates()
                }
            }
            ACTION_STOP -> {
                stopSelf()
            }
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
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
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
            } catch (e: Exception) {
                // Error silencioso
            }
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
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
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