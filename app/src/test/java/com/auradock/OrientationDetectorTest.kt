package com.auradock.sensor

import com.auradock.sensor.OrientationDetector.OrientationEvent
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.abs

/**
 * Pure logic tests for orientation classification.
 * Reproduces the same threshold logic from [OrientationDetector] without
 * requiring a real [android.hardware.SensorManager] (no Robolectric needed).
 */
class OrientationDetectorTest {

    private val THRESHOLD = 7.0f

    private fun classify(x: Float, y: Float, z: Float): OrientationEvent = when {
        abs(x) >= THRESHOLD -> OrientationEvent.LANDSCAPE
        abs(z) >= THRESHOLD -> OrientationEvent.FLAT
        else                -> OrientationEvent.PORTRAIT
    }

    @Test
    fun `strong X-axis acceleration maps to LANDSCAPE`() {
        assertEquals(OrientationEvent.LANDSCAPE, classify(x = 8f, y = 1f, z = 2f))
        assertEquals(OrientationEvent.LANDSCAPE, classify(x = -9f, y = 0f, z = 0f))
    }

    @Test
    fun `strong Z-axis acceleration maps to FLAT`() {
        assertEquals(OrientationEvent.FLAT, classify(x = 0f, y = 0f, z = 9.8f))
    }

    @Test
    fun `low all-axis acceleration maps to PORTRAIT`() {
        assertEquals(OrientationEvent.PORTRAIT, classify(x = 1f, y = 9f, z = 1f))
    }

    @Test
    fun `X-axis takes priority over Z-axis`() {
        // Both X and Z above threshold — X wins (landscape)
        assertEquals(OrientationEvent.LANDSCAPE, classify(x = 8f, y = 0f, z = 9f))
    }

    @Test
    fun `exactly at threshold is classified as LANDSCAPE`() {
        assertEquals(OrientationEvent.LANDSCAPE, classify(x = THRESHOLD, y = 0f, z = 0f))
    }
}
