package com.auradock.dock

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/** What the "Dock from anywhere" pill shows. */
sealed interface Pill {
    /** Charging: "Docking in [secondsLeft]...", then the dock opens by itself. */
    data class Countdown(val secondsLeft: Int) : Pill

    /** Not charging: "Switch to dock?", hides by itself after a few seconds. */
    data object Offer : Pill
}

/** What is going on around the dock watcher. All of it is read by the service; none of it is guessed here. */
data class WatchContext(
    val charging: Boolean,
    val mediaPlaying: Boolean,
    val inCall: Boolean,
    /** AuraDock's own dock or screensaver is on screen. */
    val dockShowing: Boolean
)

/** What to do now: show [pill] (null: show nothing), and open the dock if [dockNow]. */
data class WatchOutput(val pill: Pill?, val dockNow: Boolean = false)

/**
 * The rules of "Dock from anywhere", free of Android so they can be tested with made-up readings.
 *
 * It is fed accelerometer readings ([onSample]) and asked what to do every fraction of a second ([step]):
 *  - the phone must be physically landscape and held still for [STILL_MS] (a hand always moves a little);
 *  - and nothing may be in the way: no audio or video playing, no call, no dock or screensaver already showing;
 *  - then a pill appears: a countdown if charging, an offer if not;
 *  - after a Cancel, a tap-away or the offer timing out it stays quiet until the phone is really picked up or
 *    turned to portrait, so someone reading on a stand sees the pill at most once; the same after the dock
 *    closes (the phone is still on the stand).
 *
 * Constants are first guesses to be tuned on the real phone.
 */
class DockWatchLogic {
    private class Sample(val t: Long, val x: Float, val y: Float, val z: Float)

    private enum class Phase { IDLE, COUNTDOWN, OFFER }

    private val samples = ArrayDeque<Sample>()
    private var phase = Phase.IDLE
    private var phaseStart = 0L
    private var quiet = false
    private var quietRef: Sample? = null
    private var movedCount = 0
    private var notLandscapeCount = 0
    private var dockWasShowing = false

    /** Forget everything seen so far (the screen just came on: readings from before are stale). */
    fun reset() {
        samples.clear()
        phase = Phase.IDLE
        movedCount = 0
        notLandscapeCount = 0
    }

    fun onSample(nowMs: Long, x: Float, y: Float, z: Float) {
        samples.addLast(Sample(nowMs, x, y, z))
        while (samples.isNotEmpty() && samples.first().t < nowMs - KEEP_MS) samples.removeFirst()

        // Counted per reading, so one bump (a tap on the screen) does not count as a pickup but a lift does.
        notLandscapeCount = if (classifyOrientation(x, y, z).isLandscape) 0 else notLandscapeCount + 1
        val ref = quietRef
        movedCount = if (quiet && ref != null && distance(ref, x, y, z) > MOVE_DEVIATION) movedCount + 1 else 0
    }

    /** The user tapped Cancel, or let the offer go. */
    fun onCancel() {
        goQuiet()
    }

    /** The user tapped the pill itself. */
    fun onPillTapped(): WatchOutput {
        if (phase == Phase.IDLE) return WatchOutput(null)
        goQuiet() // if the dock does not open, do not keep nagging
        return WatchOutput(null, dockNow = true)
    }

    fun step(nowMs: Long, context: WatchContext): WatchOutput {
        // A dock that was showing and closed leaves the phone on the stand: do not offer it again straight away.
        if (context.dockShowing) {
            dockWasShowing = true
            phase = Phase.IDLE
            return WatchOutput(null)
        }
        if (dockWasShowing) {
            dockWasShowing = false
            goQuiet()
        }

        val latest = samples.lastOrNull() ?: return hide()

        if (quiet && (notLandscapeCount >= CONFIRM_READINGS || movedCount >= CONFIRM_READINGS)) {
            quiet = false
            quietRef = null
        }

        val eligible = isLandscapeAndStill(nowMs) && !context.mediaPlaying && !context.inCall
        if (!eligible) return hide()
        if (quiet) return WatchOutput(null)

        val wanted = if (context.charging) Phase.COUNTDOWN else Phase.OFFER
        if (phase != wanted) {
            phase = wanted
            phaseStart = nowMs
        }
        val elapsed = nowMs - phaseStart
        return when (phase) {
            Phase.COUNTDOWN ->
                if (elapsed >= COUNTDOWN_MS) {
                    goQuiet(latest)
                    WatchOutput(null, dockNow = true)
                } else {
                    WatchOutput(Pill.Countdown(ceil((COUNTDOWN_MS - elapsed) / 1000.0).toInt()))
                }
            Phase.OFFER ->
                if (elapsed >= OFFER_MS) {
                    goQuiet(latest)
                    WatchOutput(null)
                } else {
                    WatchOutput(Pill.Offer)
                }
            Phase.IDLE -> WatchOutput(null)
        }
    }

    private fun hide(): WatchOutput {
        phase = Phase.IDLE
        return WatchOutput(null)
    }

    private fun goQuiet(ref: Sample? = samples.lastOrNull()) {
        quiet = true
        quietRef = ref
        movedCount = 0
        notLandscapeCount = 0
        phase = Phase.IDLE
    }

    /** Held landscape and still over the last [STILL_MS]: the readings cover that long and barely vary. */
    private fun isLandscapeAndStill(nowMs: Long): Boolean {
        val window = samples.filter { it.t >= nowMs - STILL_MS }
        val oldest = samples.firstOrNull() ?: return false
        if (window.size < MIN_READINGS || oldest.t > nowMs - STILL_MS + COVER_SLACK_MS) return false
        if (nowMs - samples.last().t > COVER_SLACK_MS) return false // the readings stopped coming
        val still = rangeOf(window) { it.x } < STILL_RANGE && rangeOf(window) { it.y } < STILL_RANGE &&
            rangeOf(window) { it.z } < STILL_RANGE
        if (!still) return false
        val n = window.size
        val orientation = classifyOrientation(
            window.sumOf { it.x.toDouble() }.toFloat() / n,
            window.sumOf { it.y.toDouble() }.toFloat() / n,
            window.sumOf { it.z.toDouble() }.toFloat() / n
        )
        return orientation.isLandscape
    }

    private fun rangeOf(window: List<Sample>, value: (Sample) -> Float): Float {
        var lo = Float.MAX_VALUE
        var hi = -Float.MAX_VALUE
        for (s in window) {
            lo = min(lo, value(s))
            hi = max(hi, value(s))
        }
        return hi - lo
    }

    private fun distance(a: Sample, x: Float, y: Float, z: Float): Float {
        val dx = a.x - x
        val dy = a.y - y
        val dz = a.z - z
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    companion object {
        const val STILL_MS = 3000L
        const val COUNTDOWN_MS = 5000L
        const val OFFER_MS = 6000L

        /** Readings kept: the still window plus a little. */
        private const val KEEP_MS = STILL_MS + 1000L

        /** How much each axis may vary (m/s^2) over the still window. A phone on a stand varies by hundredths; a hand by tenths. */
        const val STILL_RANGE = 0.3f

        /** How far gravity must move from where it was when we went quiet to count as a pickup (about a 12 degree tilt). */
        const val MOVE_DEVIATION = 2.0f

        /** In a row, so a bump is not a pickup. */
        private const val CONFIRM_READINGS = 3
        private const val MIN_READINGS = 6
        private const val COVER_SLACK_MS = 500L
    }
}
