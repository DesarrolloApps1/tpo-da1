package com.takeabreak.platform.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Task
import com.takeabreak.domain.model.Coordinates
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

// Ubicación puntual y solo en primer plano (RNF03). Null si no hay permiso
interface LocationProvider {
    suspend fun current(): Coordinates?
}

class FusedLocationProvider(private val context: Context) : LocationProvider {

    private val client = LocationServices.getFusedLocationProviderClient(context)

    // Una lectura nueva puede no llegar nunca (sin red ni GPS, o en el emulador): se espera unos
    // segundos y si no, la última ubicación conocida
    @SuppressLint("MissingPermission")
    override suspend fun current(): Coordinates? {
        if (!hasPermission()) return null
        val fresh = withTimeoutOrNull(FRESH_TIMEOUT_MS) {
            await(client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null))
        }
        return fresh ?: await(client.lastLocation)
    }

    private suspend fun await(task: Task<Location?>): Coordinates? = suspendCancellableCoroutine { cont ->
        task.addOnSuccessListener { location -> cont.resume(location?.let { Coordinates(it.latitude, it.longitude) }) }
            .addOnFailureListener { cont.resume(null) }
    }

    private companion object {
        const val FRESH_TIMEOUT_MS = 5_000L
    }

    private fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
}
