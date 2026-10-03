package com.takeabreak.platform.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.takeabreak.domain.model.Coordinates
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

// Ubicación puntual y solo en primer plano (RNF03). Null si no hay permiso
interface LocationProvider {
    suspend fun current(): Coordinates?
}

class FusedLocationProvider(private val context: Context) : LocationProvider {

    private val client = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    override suspend fun current(): Coordinates? {
        if (!hasPermission()) return null
        return suspendCancellableCoroutine { cont ->
            client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                .addOnSuccessListener { location -> cont.resume(location?.let { Coordinates(it.latitude, it.longitude) }) }
                .addOnFailureListener { cont.resume(null) }
        }
    }

    private fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
}
