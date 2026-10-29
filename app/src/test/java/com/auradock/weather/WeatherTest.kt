package com.auradock.weather

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import java.util.Locale

class WeatherTest {
    private fun weather(code: Int = 1, fetchedAt: Long = 0, city: String? = "Mumbai", tempC: Double = 30.4) =
        Weather(tempC, code, city, fetchedAt)

    // ── WMO code -> label ─────────────────────────────────────────────────────

    @Test
    fun `codes map to short labels`() {
        assertEquals("Clear", conditionFor(0).label)
        assertEquals("Mostly clear", conditionFor(1).label)
        assertEquals("Partly cloudy", conditionFor(2).label)
        assertEquals("Cloudy", conditionFor(3).label)
        assertEquals("Fog", conditionFor(45).label)
        assertEquals("Drizzle", conditionFor(53).label)
        assertEquals("Rain", conditionFor(63).label)
        assertEquals("Rain", conditionFor(81).label) // showers
        assertEquals("Snow", conditionFor(75).label)
        assertEquals("Thunderstorm", conditionFor(95).label)
        assertEquals("Thunderstorm", conditionFor(99).label)
    }

    @Test
    fun `an unknown code gets a neutral label instead of failing`() = assertEquals(WeatherCondition.CLOUDY, conditionFor(1234))

    @Test
    fun `every condition has an icon`() {
        assertEquals(WeatherIconKind.RAIN, WeatherCondition.RAIN.icon)
        assertEquals(WeatherIconKind.STORM, WeatherCondition.THUNDERSTORM.icon)
    }

    // ── units ─────────────────────────────────────────────────────────────────

    @Test
    fun `celsius outside the US`() {
        assertEquals("30°", temperatureLabel(30.4, Locale("en", "IN")))
        assertEquals("21°", temperatureLabel(20.6, Locale.UK))
        assertEquals("-3°", temperatureLabel(-2.6, Locale.GERMANY))
    }

    @Test
    fun `fahrenheit only for US locales`() {
        assertEquals("86°", temperatureLabel(30.0, Locale.US))
        assertEquals("32°", temperatureLabel(0.0, Locale.US))
        assertEquals("86°", temperatureLabel(30.0, Locale("es", "US"))) // Spanish in the US is still the US
    }

    @Test
    fun `a Canadian or British English phone stays in celsius`() {
        assertEquals("30°", temperatureLabel(30.0, Locale.CANADA))
        assertEquals("30°", temperatureLabel(30.0, Locale("en", "GB")))
    }

    // ── label line ────────────────────────────────────────────────────────────

    @Test
    fun `the line shows the city when known and omits it otherwise`() {
        assertEquals("Mostly clear · Mumbai", conditionLine(weather(city = "Mumbai")))
        assertEquals("Mostly clear", conditionLine(weather(city = null)))
    }

    // ── cache rules ───────────────────────────────────────────────────────────

    @Test
    fun `refresh when there is nothing cached`() = assertEquals(true, shouldRefresh(null, 1_000))

    @Test
    fun `do not refresh inside 30 minutes, refresh at 30`() {
        val w = weather(fetchedAt = 0)
        assertEquals(false, shouldRefresh(w, REFRESH_AFTER_MS - 1))
        assertEquals(true, shouldRefresh(w, REFRESH_AFTER_MS))
    }

    @Test
    fun `old weather is hidden rather than shown as current`() {
        val w = weather(fetchedAt = 0)
        assertSame(w, w.usableAt(SHOW_UNTIL_MS - 1))
        assertNull(w.usableAt(SHOW_UNTIL_MS))
        assertNull((null as Weather?).usableAt(0))
    }

    @Test
    fun `weather fetched a few seconds after the minute tick is shown, not hidden`() {
        // the screen's clock says 12:02:00 but the weather arrived at 12:02:29
        val w = weather(fetchedAt = 29_000)
        assertSame(w, w.usableAt(0))
    }

    // ── Open-Meteo answer ─────────────────────────────────────────────────────

    @Test
    fun `parses the current temperature and code`() {
        val json = """{"latitude":19.07,"longitude":72.88,"current_units":{"temperature_2m":"°C","weather_code":"wmo code"},
            "current":{"time":"2026-10-29T11:45","interval":900,"temperature_2m":31.4,"weather_code":2}}"""
        assertEquals(CurrentReading(31.4, 2), parseCurrentReading(json))
    }

    @Test
    fun `anything unexpected parses to null`() {
        assertNull(parseCurrentReading("not json"))
        assertNull(parseCurrentReading("""{"error":true,"reason":"bad request"}"""))
        assertNull(parseCurrentReading("""{"current":{"temperature_2m":31.4}}"""))
        assertNull(parseCurrentReading(""))
    }

    @Test
    fun `coordinates are rounded to two decimals`() {
        assertEquals(19.08, roundedCoordinate(19.0760), 0.0)
        assertEquals(72.88, roundedCoordinate(72.8777), 0.0)
        assertEquals(-33.87, roundedCoordinate(-33.8688), 0.0)
    }
}
