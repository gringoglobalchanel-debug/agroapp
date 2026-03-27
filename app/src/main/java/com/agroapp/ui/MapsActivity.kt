package com.agroapp.ui

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.agroapp.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import java.util.Locale

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val LOCATION_PERMISSION_CODE = 1001
    private var currentMarker: Marker? = null
    private var selectedLatLng: LatLng? = null
    private var selectedAddress: String = ""

    private lateinit var tvSelectedAddress: TextView
    private lateinit var btnConfirmLocation: Button
    private lateinit var btnCurrentLocation: Button
    private lateinit var toolbar: Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        toolbar = findViewById(R.id.toolbar)
        tvSelectedAddress = findViewById(R.id.tvSelectedAddress)
        btnConfirmLocation = findViewById(R.id.btnConfirmLocation)
        btnCurrentLocation = findViewById(R.id.btnCurrentLocation)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        btnConfirmLocation.setOnClickListener {
            confirmLocation()
        }

        btnCurrentLocation.setOnClickListener {
            getCurrentLocation()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        mMap.uiSettings.isMyLocationButtonEnabled = true
        mMap.uiSettings.isZoomControlsEnabled = true

        mMap.setOnCameraIdleListener {
            val centerLatLng = mMap.cameraPosition.target
            updateMarker(centerLatLng)
        }

        mMap.setOnMarkerDragListener(object : GoogleMap.OnMarkerDragListener {
            override fun onMarkerDragStart(marker: Marker) {}
            override fun onMarkerDrag(marker: Marker) {}
            override fun onMarkerDragEnd(marker: Marker) {
                updateMarker(marker.position)
            }
        })

        checkLocationPermission()
    }

    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED -> {
                enableMyLocation()
            }
            else -> {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ),
                    LOCATION_PERMISSION_CODE
                )
            }
        }
    }

    private fun enableMyLocation() {
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
                mMap.isMyLocationEnabled = true
                mMap.uiSettings.isMyLocationButtonEnabled = true
                getCurrentLocation()
            }
        } catch (e: SecurityException) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getCurrentLocation() {
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        val newLocation = LatLng(location.latitude, location.longitude)
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(newLocation, 15f))
                        updateMarker(newLocation)
                    } else {
                        val david = LatLng(8.4333, -82.4333)
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(david, 12f))
                        updateMarker(david)
                        Toast.makeText(this, "Usando ubicación predeterminada: David", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } catch (e: SecurityException) {
            Toast.makeText(this, "No se pudo obtener ubicación", Toast.LENGTH_SHORT).show()
            val david = LatLng(8.4333, -82.4333)
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(david, 12f))
            updateMarker(david)
        }
    }

    private fun updateMarker(latLng: LatLng) {
        selectedLatLng = latLng

        if (currentMarker != null) {
            currentMarker?.remove()
        }

        currentMarker = mMap.addMarker(
            MarkerOptions()
                .position(latLng)
                .title("Ubicación de entrega")
                .snippet("Arrastra para ajustar")
                .draggable(true)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
        )

        getAddressFromLocation(latLng) { address ->
            selectedAddress = address
            if (address.isNotEmpty()) {
                tvSelectedAddress.text = "📍 $address"
            } else {
                tvSelectedAddress.text = String.format("📍 %.6f, %.6f", latLng.latitude, latLng.longitude)
            }
        }
    }

    private fun getAddressFromLocation(latLng: LatLng, callback: (String) -> Unit) {
        try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            val address = if (addresses != null && addresses.isNotEmpty()) {
                addresses[0].getAddressLine(0)
            } else {
                ""
            }
            callback(address)
        } catch (e: Exception) {
            callback("")
        }
    }

    private fun confirmLocation() {
        if (selectedLatLng == null) {
            Toast.makeText(this, "Selecciona una ubicación primero", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = intent
        intent.putExtra("latitude", selectedLatLng!!.latitude)
        intent.putExtra("longitude", selectedLatLng!!.longitude)
        intent.putExtra("address", selectedAddress)
        setResult(RESULT_OK, intent)
        finish()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            LOCATION_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    enableMyLocation()
                } else {
                    Toast.makeText(this, "Permiso de ubicación denegado. Selecciona manualmente.", Toast.LENGTH_LONG).show()
                    val david = LatLng(8.4333, -82.4333)
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(david, 12f))
                    updateMarker(david)
                }
            }
        }
    }

    override fun onBackPressed() {
        if (selectedLatLng != null) {
            confirmLocation()
        } else {
            super.onBackPressed()
        }
    }
}