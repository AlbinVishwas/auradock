package com.auradock.weather

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.Executor
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * An approximate position from Android's own LocationManager (no Google Play services needed).
 * It only asks for ACCESS_COARSE_LOCATION: roughly a city block or two, which is all weather needs.
 */
@Singleton
class LocationProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    /** Null if permission is missing, location is switched off, or nothing could be found in time. */
    @SuppressLint("MissingPermission") // hasPermission() is checked first, and every call below is wrapped in runCatching
    suspend fun approximateLocation(): Location? {
        if (!hasPermission()) return null
        val manager = context.getSystemService(LocationManager::class.java) ?: return null
        val enabled = PROVIDERS.filter { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }

        // First choice: a position Android already knows (instant, no battery), taken from the best provider that has
        // a recent one: fused, then network, and only then "passive" (whatever any app last happened to get).
        // Age uses the time since boot (not the wall clock), which every location carries and which clock changes cannot
        // fool.
        val nowSinceBootMs = SystemClock.elapsedRealtimeNanos() / 1_000_000
        val known = PROVIDERS.firstNotNullOfOrNull { provider ->
            val last = runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
            val ageMs = last?.let { nowSinceBootMs - it.elapsedRealtimeNanos / 1_000_000 }
            Log.d(TAG, "provider $provider: last known ${last?.let { "%.2f,%.2f".format(it.latitude, it.longitude) }} age ${ageMs}ms")
            last?.takeIf { ageMs != null && ageMs < MAX_KNOWN_AGE_MS }
        }
        if (known != null) return known

        // Otherwise ask for one fresh fix, but give up after a few seconds.
        val provider = enabled.firstOrNull() ?: return null
        return withTimeoutOrNull(FIX_TIMEOUT_MS) { currentFix(manager, provider) }
    }

    @SuppressLint("MissingPermission") // only reached after hasPermission() succeeded
    private suspend fun currentFix(manager: LocationManager, provider: String): Location? =
        suspendCancellableCoroutine { cont ->
            val signal = CancellationSignal()
            cont.invokeOnCancellation { signal.cancel() }
            val executor = Executor { it.run() }
            runCatching {
                LocationManagerCompat.getCurrentLocation(manager, provider, signal, executor) { location ->
                    if (cont.isActive) cont.resume(location)
                }
            }.onFailure { if (cont.isActive) cont.resume(null) }
        }

    private companion object {
        const val TAG = "Weather"
        // "fused" blends the phone's sources and works with only the coarse permission; "network" is cell/Wi-Fi.
        // The GPS provider is deliberately absent: it requires the precise-location permission.
        val PROVIDERS = listOf("fused", LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
        const val MAX_KNOWN_AGE_MS = 60 * 60 * 1000L
        const val FIX_TIMEOUT_MS = 8_000L
    }
}
