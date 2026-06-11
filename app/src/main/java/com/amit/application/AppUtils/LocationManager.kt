package com.amit.application.AppUtils

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class LocationManager(context: Context) {
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission") // Assumes permissions checked at Compose UI layer
    suspend fun getCurrentLocation(): Location? = suspendCancellableCoroutine { continuation ->
        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            null
        ).addOnSuccessListener { location: Location? ->
            if (continuation.isActive) continuation.resume(location)
        }.addOnFailureListener {
            if (continuation.isActive) continuation.resume(null)
        }
    }
}