package com.auradock.weather

import java.util.Locale
import kotlin.math.roundToInt

/** A short condition label and which simple icon to draw for it. */
enum class WeatherCondition(val label: String, val icon: WeatherIconKind) {
    CLEAR("Clear", WeatherIconKind.SUN),
    MOSTLY_CLEAR("Mostly clear", WeatherIconKind.SUN),
    PARTLY_CLOUDY("Partly cloudy", WeatherIconKind.PARTLY_CLOUDY),
    CLOUDY("Cloudy", WeatherIconKind.CLOUD),
    FOG("Fog", WeatherIconKind.FOG),
    DRIZZLE("Drizzle", WeatherIconKind.DRIZZLE),
    RAIN("Rain", WeatherIconKind.RAIN),
    SNOW("Snow", WeatherIconKind.SNOW),
    THUNDERSTORM("Thunderstorm", WeatherIconKind.STORM)
}

enum class WeatherIconKind { SUN, PARTLY_CLOUDY, CLOUD, FOG, DRIZZLE, RAIN, SNOW, STORM }

/** The weather right now, as last fetched. [temperatureC] is always Celsius; [city] is null if unknown. */
data class Weather(
    val temperatureC: Double,
    val weatherCode: Int,
    val city: String?,
    val fetchedAt: Long,
    /** Today's high and low (Celsius), when known. */
    val highC: Double? = null,
    val lowC: Double? = null
) {
    val condition: WeatherCondition get() = conditionFor(weatherCode)
}

/**
 * Open-Meteo reports the sky as a WMO weather code (a number). This groups them into the few labels we show.
 * https://open-meteo.com/en/docs#weathervariables
 */
fun conditionFor(wmoCode: Int): WeatherCondition = when (wmoCode) {
    0 -> WeatherCondition.CLEAR
    1 -> WeatherCondition.MOSTLY_CLEAR
    2 -> WeatherCondition.PARTLY_CLOUDY
    3 -> WeatherCondition.CLOUDY
    45, 48 -> WeatherCondition.FOG
    51, 53, 55, 56, 57 -> WeatherCondition.DRIZZLE
    61, 63, 65, 66, 67, 80, 81, 82 -> WeatherCondition.RAIN
    71, 73, 75, 77, 85, 86 -> WeatherCondition.SNOW
    95, 96, 99 -> WeatherCondition.THUNDERSTORM
    else -> WeatherCondition.CLOUDY // unknown future code: a neutral, not-wrong choice
}

/** Fahrenheit for US locales only; everywhere else Celsius. Rounded to whole degrees, e.g. "29°". */
fun temperatureLabel(temperatureC: Double, locale: Locale = Locale.getDefault()): String {
    val value = if (locale.country == "US") temperatureC * 9.0 / 5.0 + 32.0 else temperatureC
    return "${value.roundToInt()}°"
}

/** "Mostly clear" or "Mostly clear · Mumbai". */
fun conditionLine(weather: Weather): String =
    listOfNotNull(weather.condition.label, weather.city).joinToString(" · ")

const val REFRESH_AFTER_MS = 30 * 60 * 1000L

/** Weather older than this is not shown at all (an old reading would be misleading): "—" instead. */
const val SHOW_UNTIL_MS = 3 * 60 * 60 * 1000L

/** Refresh when there is nothing cached or the cache is 30 minutes old or more. */
fun shouldRefresh(cached: Weather?, nowMillis: Long): Boolean =
    cached == null || nowMillis - cached.fetchedAt >= REFRESH_AFTER_MS

/**
 * The cache if it is recent enough to show, else null. [nowMillis] is often the minute clock (12:02:00) while the weather
 * may have arrived a few seconds later (12:02:29), so an "age" below zero must count as fresh, not as invalid.
 */
fun Weather?.usableAt(nowMillis: Long): Weather? =
    this?.takeIf { nowMillis - it.fetchedAt < SHOW_UNTIL_MS }
