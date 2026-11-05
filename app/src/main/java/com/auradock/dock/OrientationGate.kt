package com.auradock.dock

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * How the phone is physically held, from gravity alone (independent of what the display is doing).
 *
 * LANDSCAPE_LEFT  = the phone's top edge points to the left  (Android's ROTATION_90)
 * LANDSCAPE_RIGHT = the phone's top edge points to the right (Android's ROTATION_270)
 */
enum class PhysicalOrientation(val degrees: Int) {
    PORTRAIT(0),
    LANDSCAPE_LEFT(90),
    LANDSCAPE_RIGHT(270),
    FLAT(0);

    val isLandscape: Boolean get() = this == LANDSCAPE_LEFT || this == LANDSCAPE_RIGHT
}

/** Below this total acceleration (m/s^2) the reading is not trustworthy (free fall, sensor glitch). */
private const val MIN_GRAVITY = 4f

/**
 * Fraction of gravity that must point through the screen for the phone to count as "lying flat".
 * 0.8 means flatter than about 53 degrees back from upright. Stands usually lean the phone back 15-30
 * degrees, so they stay far from this limit; a phone on a desk is at ~1.0.
 */
private const val FLAT_THRESHOLD = 0.8f

/**
 * Decides the physical orientation from one accelerometer reading.
 *
 * The accelerometer reports the pull of gravity along three axes fixed to the phone: x points out of the
 * phone's right edge, y out of its top edge, z out of the screen. At rest it reads about 9.8 along
 * whichever axis points *up*. So:
 *  - phone upright (portrait): the top edge points up, so y is about +9.8;
 *  - top edge turned to the left: the right edge now points up, so x is about +9.8 (LANDSCAPE_LEFT);
 *  - top edge turned to the right: the left edge points up, so x is about -9.8 (LANDSCAPE_RIGHT);
 *  - phone flat on its back: the screen points up, so z is about +9.8 (or -9.8 face down).
 *
 * Steps: if most of gravity goes through the screen (z), it is FLAT, whichever way it is facing. That check
 * comes first and is the tilt margin: a phone leaning back 25 degrees on a stand has z of only about 0.42 of
 * gravity, nowhere near flat. Otherwise look at the in-screen axes: if x is the bigger one it is landscape
 * (sign of x tells which way), else portrait.
 */
fun classifyOrientation(x: Float, y: Float, z: Float): PhysicalOrientation {
    val g = sqrt(x * x + y * y + z * z)
    if (g < MIN_GRAVITY) return PhysicalOrientation.FLAT
    if (abs(z) / g >= FLAT_THRESHOLD) return PhysicalOrientation.FLAT
    return if (abs(x) > abs(y)) {
        if (x > 0) PhysicalOrientation.LANDSCAPE_LEFT else PhysicalOrientation.LANDSCAPE_RIGHT
    } else {
        PhysicalOrientation.PORTRAIT
    }
}

/**
 * Clockwise degrees to turn the dock content so it reads upright, given how the phone is held and how
 * the window is currently rotated by the system ([displayRotationDegrees]: 0, 90, 180 or 270, the same
 * numbers as Surface.ROTATION_x times 90).
 *
 * Example: the display stays portrait (0) but the phone is held with its top to the left (90): the
 * content must be turned 90 degrees clockwise. If the display has already rotated to match (90), nothing
 * more is needed. Landscape only: the dock is never shown for portrait or flat.
 */
fun contentRotationDegrees(physical: PhysicalOrientation, displayRotationDegrees: Int): Int {
    require(physical.isLandscape) { "dock content is only rotated for landscape, not $physical" }
    return ((physical.degrees - displayRotationDegrees) % 360 + 360) % 360
}

/**
 * Sensors are noisy for a moment after you pick the phone up or set it down. This only reports an
 * orientation once it has been seen [requiredConsecutive] times in a row.
 */
class OrientationStabilizer(private val requiredConsecutive: Int = 4) {
    private var candidate: PhysicalOrientation? = null
    private var count = 0

    /** Returns the orientation once it is stable, or null while still settling. */
    fun onSample(orientation: PhysicalOrientation): PhysicalOrientation? {
        if (orientation == candidate) count++ else {
            candidate = orientation
            count = 1
        }
        return if (count >= requiredConsecutive) orientation else null
    }
}

sealed interface GateDecision {
    /** Keep waiting (screen stays black) or, once showing, keep what is on screen. */
    data object Wait : GateDecision
    data class Show(val orientation: PhysicalOrientation) : GateDecision
    /** Close the dream so the phone sleeps normally. */
    data object Finish : GateDecision
}

/**
 * The screensaver's decision rules (from the V1 spec):
 *  - first stable reading is landscape: show the dock;
 *  - first stable reading is portrait or flat: finish;
 *  - nothing stable in time: finish;
 *  - once showing, a switch between left and right landscape turns the content; a bump to portrait or
 *    flat is ignored (touching the screen is what closes the dream, so a nudge does not).
 */
class DockOrientationGate {
    private var showing = false

    fun onStableOrientation(orientation: PhysicalOrientation): GateDecision = when {
        orientation.isLandscape -> {
            showing = true
            GateDecision.Show(orientation)
        }
        !showing -> GateDecision.Finish
        else -> GateDecision.Wait
    }

    /** The first-reading deadline passed. */
    fun onTimeout(): GateDecision = if (showing) GateDecision.Wait else GateDecision.Finish
}

/**
 * A short record of what the gate saw, so a screensaver that closed (or never opened) can be explained from
 * the log afterwards. Keeps the first [MAX_KEPT] readings with the time each arrived and how it was read.
 */
class GateTrace(private val startMillis: Long) {
    private val samples = mutableListOf<String>()
    private var count = 0

    fun onSample(nowMillis: Long, x: Float, y: Float, z: Float, read: PhysicalOrientation) {
        count++
        if (samples.size < MAX_KEPT) {
            samples += "+${nowMillis - startMillis}ms (%.1f, %.1f, %.1f)=%s".format(java.util.Locale.ROOT, x, y, z, read)
        }
    }

    /** One line for the log: the decision, how long the gate took, and the readings it used. */
    fun describe(nowMillis: Long, decision: String, stable: PhysicalOrientation?): String =
        "gate $decision after ${nowMillis - startMillis}ms; samples=$count, stable=${stable ?: "none"}; " +
            if (samples.isEmpty()) "no sensor readings arrived" else samples.joinToString("; ")

    private companion object {
        const val MAX_KEPT = 8
    }
}
