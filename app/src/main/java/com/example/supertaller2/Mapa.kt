package com.example.supertaller2

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.supertaller2.databinding.ActivityMapaBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.UiSettings
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.PolylineOptions
import java.io.IOException

class Mapa : AppCompatActivity(), OnMapReadyCallback  {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapaBinding
    private val PERM_LOCATION_CODE = 3000
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var locationRequest: LocationRequest? = null
    private lateinit var sensorManager: SensorManager
    private lateinit var lightSensorListener: SensorEventListener
    private lateinit var mGeocoder: Geocoder
    private var currentMarker: Marker? = null
    private val puntos: ArrayList<LatLng> = ArrayList()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mGeocoder = Geocoder(this)

        lightSensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (mMap != null) {
                    if (event.values[0] < 5000) {
                        Log.i("MAPS", "DARK MAP " + event.values[0])
                        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this@Mapa, R.raw.night))
                    } else {
                        Log.i("MAPS", "LIGHT MAP " + event.values[0])
                        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this@Mapa, R.raw.light))
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
            }
        }

        binding.texto.setOnEditorActionListener { v, actionId, event ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                binding.switchCenterOnLocation.isChecked = true
                buscarDireccion()
                return@setOnEditorActionListener true
            }
            false
        }


        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                initializeMap()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                Toast.makeText(this, "El permiso de Localizacion es necesario para usar esta actividad 😭", Toast.LENGTH_SHORT).show()
            }
            else -> {
                requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), PERM_LOCATION_CODE)
            }
        }

    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        val uiSettings: UiSettings = mMap.uiSettings
        uiSettings.isZoomControlsEnabled = true
        mMap.setOnMapLongClickListener { latLng ->
            addMarkerWithAddress(latLng)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(1000)
        }
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    updateLocation(location)
                }
            }
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.isMyLocationEnabled = true
            startLocationUpdates()
        } else {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), PERM_LOCATION_CODE)
        }
    }
    private fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationRequest?.let {
                fusedLocationClient.requestLocationUpdates(
                    it,
                    locationCallback,
                    null
                )
            }
        }
    }
    private fun updateLocation(location: Location) {
        val currentLatLng = LatLng(location.latitude, location.longitude)
        if (!binding.switchCenterOnLocation.isChecked) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 17f))
        }
        puntos.add(currentLatLng)
        val polylineOptions = PolylineOptions()
            .addAll(puntos)
            .color(Color.GREEN)
            .width(5f)
        mMap.addPolyline(polylineOptions)
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERM_LOCATION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initializeMap()
                } else {
                    showDefaultLocation()
                    Toast.makeText(this, "Me acaban de negar los permisos de Localizacion 😭", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun initializeMap() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }
    private fun showDefaultLocation() {
        val bogotaLocation = LatLng(4.6097, -74.0817)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(bogotaLocation, 10f))
    }

    private fun addMarkerWithAddress(latLng: LatLng) {
        try {
            currentMarker?.remove()
            val addresses = mGeocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)

            if (addresses != null) {
                if (addresses.isNotEmpty()) {
                    val address = addresses?.get(0)
                    val addressString =
                        address?.getAddressLine(0)
                    currentMarker = mMap.addMarker(MarkerOptions().position(latLng).title(addressString))
                } else {
                    Toast.makeText(this, "Dirección no encontrada", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Error al obtener la dirección", Toast.LENGTH_SHORT).show()
        }
    }
    private fun buscarDireccion() {
        val addressString = binding.texto.text.toString()
        if (addressString.isNotEmpty()) {
            try {
                currentMarker?.remove()
                val addresses = mGeocoder.getFromLocationName(addressString, 1)
                if (addresses != null && addresses.isNotEmpty()) {
                    val addressResult = addresses[0]
                    val position = LatLng(addressResult.latitude, addressResult.longitude)
                    currentMarker = mMap.addMarker(
                        MarkerOptions().position(position).title(addressResult.getAddressLine(0))
                    )
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 17f))
                } else {
                    Toast.makeText(this, "Dirección no encontrada", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Error al buscar la dirección", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Por favor ingrese una dirección", Toast.LENGTH_SHORT).show()
        }
    }
    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(
            lightSensorListener,
            sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT),
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(lightSensorListener)
    }
}