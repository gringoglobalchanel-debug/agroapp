package com.agroapp.ui

import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.agroapp.R
import java.util.Locale

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private var selectedLatitude: Double = 0.0
    private var selectedLongitude: Double = 0.0
    private var selectedAddress: String = ""
    private var marker: Marker? = null
    private lateinit var tvSelectedAddress: TextView
    private lateinit var btnConfirm: Button
    private lateinit var btnCurrentLocation: Button

    companion object {
        const val EXTRA_LATITUDE = "extra_latitude"
        const val EXTRA_LONGITUDE = "extra_longitude"
        const val EXTRA_ADDRESS = "extra_address"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        tvSelectedAddress = findViewById(R.id.tvSelectedAddress)
        btnConfirm = findViewById(R.id.btnConfirmLocation)
        btnCurrentLocation = findViewById(R.id.btnCurrentLocation)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        btnConfirm.setOnClickListener {
            if (selectedLatitude != 0.0 && selectedLongitude != 0.0) {
                val intent = Intent()
                intent.putExtra(EXTRA_LATITUDE, selectedLatitude)
                intent.putExtra(EXTRA_LONGITUDE, selectedLongitude)
                intent.putExtra(EXTRA_ADDRESS, selectedAddress)
                setResult(RESULT_OK, intent)
                finish()
            } else {
                Toast.makeText(this, "Selecciona una ubicación en el mapa", Toast.LENGTH_SHORT).show()
            }
        }

        btnCurrentLocation.setOnClickListener {
            // Ir a la ubicación actual (se puede implementar con FusedLocationProviderClient)
            Toast.makeText(this, "Toca el mapa para seleccionar tu ubicación", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        // Centrar en David, Panamá (por defecto)
        val david = LatLng(8.4333, -82.4333)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(david, 12f))

        // Configurar listener de clic en el mapa
        map.setOnMapClickListener { latLng ->
            updateMarker(latLng)
        }

        // Configurar listener de arrastre del marcador
        map.setOnMarkerDragListener(object : GoogleMap.OnMarkerDragListener {
            override fun onMarkerDragStart(marker: Marker) {}
            override fun onMarkerDrag(marker: Marker) {}
            override fun onMarkerDragEnd(marker: Marker) {
                updateMarker(marker.position)
            }
        })

        // Verificar si vienen coordenadas existentes
        val existingLat = intent.getDoubleExtra("latitude", 0.0)
        val existingLng = intent.getDoubleExtra("longitude", 0.0)
        if (existingLat != 0.0 && existingLng != 0.0) {
            val location = LatLng(existingLat, existingLng)
            updateMarker(location)
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
        }
    }

    private fun updateMarker(latLng: LatLng) {
        selectedLatitude = latLng.latitude
        selectedLongitude = latLng.longitude

        // Obtener dirección desde coordenadas
        selectedAddress = getAddressFromLocation(latLng.latitude, latLng.longitude)

        tvSelectedAddress.text = if (selectedAddress.isNotEmpty()) {
            "📍 $selectedAddress"
        } else {
            "📍 ${"%.6f".format(latLng.latitude)}, ${"%.6f".format(latLng.longitude)}"
        }

        // Actualizar o crear marcador
        if (marker == null) {
            marker = map.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title("Tu ubicación")
                    .draggable(true)
            )
        } else {
            marker?.position = latLng
        }
    }

    private fun getAddressFromLocation(latitude: Double, longitude: Double): String {
        try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses: List<Address>? = geocoder.getFromLocation(latitude, longitude, 1)
            if (addresses != null && addresses.isNotEmpty()) {
                val address = addresses[0]
                val addressLine = address.getAddressLine(0)
                return addressLine ?: ""
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""
    }
}