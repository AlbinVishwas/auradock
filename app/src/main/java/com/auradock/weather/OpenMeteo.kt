package com.auradock.weather

import com.google.gson.JsonParser
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.round

/** The two numbers we read from Open-Meteo's answer. */
data class CurrentReading(val temperatureC: Double, val weatherCode: Int, val highC: Double? = null, val lowC: Double? = null)

/**
 * Reads the temperature and WMO code out of an Open-Meteo "current" response, or null if the answer is not what we
 * expect. Plain text in, plain data out, so it is unit-tested without a network.
 */
fun parseCurrentReading(json: String): CurrentReading? = runCatching {
    val root = JsonParser.parseString(json).asJsonObject
    val current = root.getAsJsonObject("current")
    // Today's high and low come from the daily block, when the answer has one.
    val daily = root.getAsJsonObject("daily")
    fun first(name: String): Double? = runCatching { daily?.getAsJsonArray(name)?.get(0)?.asDouble }.getOrNull()
    CurrentReading(
        temperatureC = current.get("temperature_2m").asDouble,
        weatherCode = current.get("weather_code").asInt,
        highC = first("temperature_2m_max"),
        lowC = first("temperature_2m_min")
    )
}.getOrNull()

/** Rounds to 2 decimals (about 1 km): enough for the weather, and we never send a more exact position. */
fun roundedCoordinate(value: Double): Double = round(value * 100.0) / 100.0

/** Open-Meteo: free, no account or key. Only coordinates (rounded) leave the phone. */
@Singleton
class OpenMeteoClient @Inject constructor() {

    /** Blocking: call from a background thread. Returns null on any network or format problem. */
    fun fetchCurrent(latitude: Double, longitude: Double): CurrentReading? {
        val url = String.format(
            Locale.US,
            "https://api.open-meteo.com/v1/forecast?latitude=%.2f&longitude=%.2f&current=temperature_2m,weather_code&daily=temperature_2m_max,temperature_2m_min&forecast_days=1&timezone=auto",
            roundedCoordinate(latitude),
            roundedCoordinate(longitude)
        )
        return runCatching {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8_000
                readTimeout = 8_000
                requestMethod = "GET"
            }
            try {
                if (connection.responseCode != HttpURLConnection.HTTP_OK) null
                else parseCurrentReading(connection.inputStream.bufferedReader().use { it.readText() })
            } finally {
                connection.disconnect()
            }
        }.getOrNull()
    }
}
