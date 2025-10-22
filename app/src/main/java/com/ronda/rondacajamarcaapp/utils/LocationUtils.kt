package com.ronda.rondacajamarcaapp.utils

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat

object LocationUtils {
    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun getCurrentLocation(context: Context, onResult: (Double, Double) -> Unit) {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val provider = when {
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            else -> LocationManager.NETWORK_PROVIDER
        }

        val last = lm.getLastKnownLocation(provider)
        if (last != null) {
            onResult(last.latitude, last.longitude)
            return
        }

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                onResult(location.latitude, location.longitude)
                lm.removeUpdates(this)
            }
        }
        lm.requestSingleUpdate(provider, listener, Looper.getMainLooper())
    }
}
