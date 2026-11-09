package com.auradock.dock.world

import java.time.Instant
import java.time.ZoneOffset
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.tan

/** Where the sun is over the earth at one moment: its declination and the longitude it is directly over, in radians. */
data class SunPosition(val declination: Double, val subsolarLongitude: Double)

/**
 * Solar position from the date and time (NOAA's general solar position approximation, good to a fraction of a degree): enough to draw the
 * day and night line and light the right dots. It is cheap, but it only needs to run every few minutes.
 */
object Solar {
    fun at(millis: Long): SunPosition {
        val t = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC)
        val minutes = t.hour * 60.0 + t.minute + t.second / 60.0
        val gamma = 2.0 * PI / 365.0 * (t.dayOfYear - 1 + (t.hour - 12) / 24.0)
        val equationOfTime = 229.18 * (0.000075 + 0.001868 * cos(gamma) - 0.032077 * sin(gamma) -
            0.014615 * cos(2 * gamma) - 0.040849 * sin(2 * gamma))
        val declination = 0.006918 - 0.399912 * cos(gamma) + 0.070257 * sin(gamma) - 0.006758 * cos(2 * gamma) +
            0.000907 * sin(2 * gamma) - 0.002697 * cos(3 * gamma) + 0.00148 * sin(3 * gamma)
        // Solar noon is where minutes of the day + equation of time = 720 at that longitude (4 minutes a degree).
        val subsolarDegrees = (720.0 - minutes - equationOfTime) / 4.0
        return SunPosition(declination, Math.toRadians(subsolarDegrees))
    }

    /** The sine of the sun's height above the horizon at a place: above 0 it is day there. */
    fun sineAltitude(sun: SunPosition, latDeg: Double, lonDeg: Double): Double {
        val lat = Math.toRadians(latDeg)
        val hourAngle = Math.toRadians(lonDeg) - sun.subsolarLongitude
        return sin(lat) * sin(sun.declination) + cos(lat) * cos(sun.declination) * cos(hourAngle)
    }

    fun isDay(sun: SunPosition, latDeg: Double, lonDeg: Double): Boolean = sineAltitude(sun, latDeg, lonDeg) > 0.0

    /** The latitude (degrees) of the day and night line at a longitude. */
    fun terminatorLatitude(sun: SunPosition, lonDeg: Double): Double {
        val dec = if (abs(sun.declination) < 1e-6) 1e-6 else sun.declination
        val hourAngle = Math.toRadians(lonDeg) - sun.subsolarLongitude
        return Math.toDegrees(atan(-cos(hourAngle) / tan(dec)))
    }
}
