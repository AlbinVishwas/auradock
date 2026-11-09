package com.auradock.weather

import android.content.Context
import android.util.Log
import com.auradock.time.MinuteClock
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Holds the latest weather. There is no background work: [refreshIfStale] is called when the app or the dock becomes
 * visible, and does nothing unless the cached weather is 30 minutes old or more. The result is saved to the phone so a
 * freshly started screensaver can show it straight away.
 */
@Singleton
class WeatherRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val location: LocationProvider,
    private val client: OpenMeteoClient,
    private val cities: CityLookup,
    private val clock: MinuteClock
) {
    private val prefs = context.getSharedPreferences("auradock_weather", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val refreshing = AtomicBoolean(false)
    @Volatile private var lastFailureAt = 0L

    private val _weather = MutableStateFlow(load())

    /** The last weather we have (it may be old: use [usableAt] before showing it). Null means "—". */
    val weather: StateFlow<Weather?> = _weather.asStateFlow()

    private val _place = MutableStateFlow(loadPlace())

    /** Where the last weather was read (null until it has been): the World dock's home dot. */
    val place: StateFlow<com.auradock.dock.world.GeoPoint?> = _place.asStateFlow()

    private fun loadPlace(): com.auradock.dock.world.GeoPoint? =
        if (prefs.contains("lat") && prefs.contains("lon")) com.auradock.dock.world.GeoPoint(prefs.getFloat("lat", 0f).toDouble(), prefs.getFloat("lon", 0f).toDouble()) else null

    fun hasLocationPermission(): Boolean = location.hasPermission()

    fun refreshIfStale() {
        val now = clock.currentMillis()
        if (!shouldRefresh(_weather.value, now)) return
        if (!location.hasPermission()) return // nothing to do, and no reason to slow down the next try
        if (now - lastFailureAt < RETRY_AFTER_FAILURE_MS) return // do not hammer a network that just failed
        if (!refreshing.compareAndSet(false, true)) return
        scope.launch {
            try {
                refresh()
            } finally {
                refreshing.set(false)
            }
        }
    }

    private suspend fun refresh() {
        val place = location.approximateLocation()
        if (place == null) {
            Log.d(TAG, "no location (location off, or no fix)")
            lastFailureAt = clock.currentMillis()
            return
        }
        val reading = client.fetchCurrent(place.latitude, place.longitude)
        if (reading == null) {
            Log.d(TAG, "weather request failed")
            lastFailureAt = clock.currentMillis()
            return
        }
        // Same area as last time: keep the city name instead of asking the Geocoder again.
        val previous = _weather.value
        val city = previous?.city?.takeIf { prefs.nearLastPlace(place.latitude, place.longitude) }
            ?: cities.cityAt(place.latitude, place.longitude)
        val fresh = Weather(reading.temperatureC, reading.weatherCode, city, clock.currentMillis())
        _weather.value = fresh
        save(fresh, place.latitude, place.longitude)
        Log.d(TAG, "weather updated: ${fresh.temperatureC}C code ${fresh.weatherCode} city=${fresh.city}")
    }

    private fun load(): Weather? {
        if (!prefs.contains("fetchedAt")) return null
        return Weather(
            temperatureC = prefs.getFloat("tempC", 0f).toDouble(),
            weatherCode = prefs.getInt("code", 0),
            city = prefs.getString("city", null),
            fetchedAt = prefs.getLong("fetchedAt", 0L)
        )
    }

    private fun save(weather: Weather, lat: Double, lon: Double) {
        prefs.edit()
            .putFloat("tempC", weather.temperatureC.toFloat())
            .putInt("code", weather.weatherCode)
            .putString("city", weather.city)
            .putLong("fetchedAt", weather.fetchedAt)
            .putFloat("lat", lat.toFloat())
            .putFloat("lon", lon.toFloat())
            .apply()
        _place.value = com.auradock.dock.world.GeoPoint(lat, lon)
    }

    private fun android.content.SharedPreferences.nearLastPlace(lat: Double, lon: Double): Boolean =
        contains("lat") && abs(getFloat("lat", 0f) - lat) < 0.1 && abs(getFloat("lon", 0f) - lon) < 0.1

    private companion object {
        const val TAG = "Weather"
        const val RETRY_AFTER_FAILURE_MS = 60_000L
    }
}
