package com.auradock.weather

import com.auradock.monitoring.Monitoring
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Turns coordinates into a city name with Android's Geocoder, if this phone has one. If it does not, or the lookup
 * fails, the answer is null and the weather simply shows without a city name.
 */
@Singleton
class CityLookup @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun cityAt(rawLatitude: Double, rawLongitude: Double): String? {
        if (!Geocoder.isPresent()) return null
        // The same 2 decimals (about 1 km) that Open-Meteo gets: a more exact position never goes to the system geocoder either.
        val latitude = roundedCoordinate(rawLatitude)
        val longitude = roundedCoordinate(rawLongitude)
        val geocoder = Geocoder(context, Locale.getDefault())
        val address: Address? = runCatching {
            if (Build.VERSION.SDK_INT >= 33) {
                suspendCancellableCoroutine<List<Address>> { cont ->
                    geocoder.getFromLocation(latitude, longitude, 1, object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) = cont.resume(addresses)
                        override fun onError(errorMessage: String?) = cont.resume(emptyList())
                    })
                }.firstOrNull()
            } else {
                @Suppress("DEPRECATION")
                withContext(Dispatchers.IO) { geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull() }
            }
        }.onFailure { Monitoring.nonFatal("geocoder.failed", sample = 0.1) }.getOrNull()
        return address?.let { it.locality ?: it.subAdminArea ?: it.adminArea }
    }
}
