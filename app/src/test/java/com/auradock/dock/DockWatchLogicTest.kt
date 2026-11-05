package com.auradock.dock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** The "Dock from anywhere" rules, driven with made-up accelerometer readings every 200 ms. */
class DockWatchLogicTest {
    private lateinit var logic: DockWatchLogic
    private var now = 0L

    private val charging = WatchContext(charging = true, mediaPlaying = false, inCall = false, dockShowing = false)
    private val unplugged = charging.copy(charging = false)

    // gravity as the phone reports it: x ~ 9.8 is "top edge to the left" (landscape), y ~ 9.8 is upright (portrait)
    private val landscape = Triple(9.77f, 0.1f, 0.7f)
    private val portrait = Triple(0.1f, 9.77f, 0.7f)
    private val flat = Triple(0.1f, 0.3f, 9.8f)

    @Before
    fun setUp() {
        logic = DockWatchLogic()
        now = 0L
    }

    /** Holds the phone as [pose] for [ms], reading every 200 ms. Returns the last pill, and dockNow if it fired at any point. */
    private fun hold(
        pose: Triple<Float, Float, Float>,
        ms: Long,
        context: WatchContext = charging,
        jitter: Float = 0f
    ): WatchOutput {
        var out = WatchOutput(null)
        var docked = false
        var i = 0
        val end = now + ms
        while (now < end) {
            now += 200
            // alternate +/- jitter so the range over the window is 2 * jitter
            val j = if (i++ % 2 == 0) jitter else -jitter
            logic.onSample(now, pose.first + j, pose.second + j, pose.third)
            out = logic.step(now, context)
            docked = docked || out.dockNow
        }
        return out.copy(dockNow = docked)
    }

    // -- the happy paths ------------------------------------------------------------------------------------------

    @Test
    fun `nothing shows until the phone has been still for three seconds`() {
        assertNull(hold(landscape, 2600).pill)
        assertTrue(hold(landscape, 1000).pill is Pill.Countdown)
    }

    @Test
    fun `landscape, still and charging counts down from five and then docks`() {
        assertEquals(Pill.Countdown(5), hold(landscape, 3200).pill)
        assertEquals(Pill.Countdown(3), hold(landscape, 2000).pill)
        val end = hold(landscape, 3000)
        assertTrue(end.dockNow)
        assertNull(end.pill)
    }

    @Test
    fun `landscape, still and not charging offers the dock`() {
        assertEquals(Pill.Offer, hold(landscape, 3200, unplugged).pill)
    }

    @Test
    fun `the offer hides itself after six seconds and stays quiet`() {
        hold(landscape, 3200, unplugged)
        assertNull(hold(landscape, 6200, unplugged).pill)
        assertNull(hold(landscape, 20_000, unplugged).pill)
        assertFalse(hold(landscape, 20_000, unplugged).dockNow)
    }

    @Test
    fun `tapping the pill docks at once`() {
        hold(landscape, 3200)
        assertTrue(logic.onPillTapped().dockNow)
    }

    @Test
    fun `tapping with no pill showing does nothing`() {
        hold(landscape, 1000)
        assertFalse(logic.onPillTapped().dockNow)
    }

    @Test
    fun `plugging in or out while the pill shows swaps it`() {
        assertEquals(Pill.Offer, hold(landscape, 3200, unplugged).pill)
        assertTrue(hold(landscape, 200, charging).pill is Pill.Countdown)
    }

    // -- false-trigger protection ---------------------------------------------------------------------------------

    @Test
    fun `portrait never triggers`() {
        assertNull(hold(portrait, 30_000).pill)
        assertNull(hold(portrait, 30_000, unplugged).pill)
    }

    @Test
    fun `lying flat never triggers`() {
        assertNull(hold(flat, 30_000).pill)
    }

    @Test
    fun `a hand-held phone never triggers`() {
        val out = hold(landscape, 30_000, jitter = 0.4f) // 0.8 range: tremor
        assertNull(out.pill)
        assertFalse(out.dockNow)
    }

    @Test
    fun `audio or video playing stops the pill, even mid-countdown`() {
        assertNull(hold(landscape, 10_000, charging.copy(mediaPlaying = true)).pill)
        logic.reset()
        hold(landscape, 3200)
        assertNull(hold(landscape, 200, charging.copy(mediaPlaying = true)).pill)
    }

    @Test
    fun `a pill that was stopped by media is offered again when playback ends`() {
        hold(landscape, 10_000, charging.copy(mediaPlaying = true))
        assertTrue(hold(landscape, 200).pill is Pill.Countdown)
    }

    @Test
    fun `a phone call stops the pill`() {
        assertNull(hold(landscape, 10_000, charging.copy(inCall = true)).pill)
        assertNull(hold(landscape, 10_000, unplugged.copy(inCall = true)).pill)
    }

    @Test
    fun `nothing shows while the dock or screensaver is already showing`() {
        val out = hold(landscape, 10_000, charging.copy(dockShowing = true))
        assertNull(out.pill)
        assertFalse(out.dockNow)
    }

    @Test
    fun `after the dock closes the phone is left alone`() {
        hold(landscape, 5000, charging.copy(dockShowing = true))
        assertNull(hold(landscape, 30_000).pill) // closed, still on the stand
    }

    @Test
    fun `readings that stop arriving cancel the pill`() {
        hold(landscape, 3200)
        now += 2000 // no readings
        assertNull(logic.step(now, charging).pill)
    }

    // -- after Cancel: quiet until picked up or turned -------------------------------------------------------------

    @Test
    fun `after cancel it stays quiet however long the phone sits there`() {
        hold(landscape, 3200)
        logic.onCancel()
        assertNull(hold(landscape, 60_000).pill)
        assertNull(hold(landscape, 60_000, unplugged).pill)
    }

    @Test
    fun `after cancel, turning to portrait and back brings the pill again`() {
        hold(landscape, 3200)
        logic.onCancel()
        hold(portrait, 1000)
        assertTrue(hold(landscape, 3400).pill is Pill.Countdown)
    }

    @Test
    fun `after cancel, a real pickup brings the pill again once put down`() {
        hold(landscape, 3200)
        logic.onCancel()
        hold(Triple(6f, 6f, 3f), 1000) // lifted and tilted
        assertTrue(hold(landscape, 3400).pill is Pill.Countdown)
    }

    @Test
    fun `after cancel, a bump from tapping the screen is not a pickup`() {
        hold(landscape, 3200)
        logic.onCancel()
        logic.onSample(now + 200, 9.77f + 2.5f, 0.1f, 0.7f) // one reading jolted
        now += 200
        logic.step(now, charging)
        assertNull(hold(landscape, 30_000).pill)
    }

    @Test
    fun `after the offer times out, it stays quiet until picked up or turned`() {
        hold(landscape, 3200, unplugged)
        hold(landscape, 6200, unplugged)
        assertNull(hold(landscape, 30_000, unplugged).pill)
        hold(portrait, 1000, unplugged)
        assertEquals(Pill.Offer, hold(landscape, 3400, unplugged).pill)
    }

    @Test
    fun `after the countdown docks, it does not dock again if the dock never opened`() {
        hold(landscape, 3200)
        assertTrue(hold(landscape, 5200).dockNow)
        assertFalse(hold(landscape, 10_000).dockNow)
    }

    @Test
    fun `reset forgets old readings so a fresh three seconds are needed`() {
        hold(landscape, 3200)
        logic.reset()
        assertNull(hold(landscape, 1000).pill)
    }
}
