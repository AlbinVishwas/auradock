package com.auradock.sessions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

/**
 * Regression: a finished session did not appear on the dashboard. The length was taken from the wall clock, so when the clock
 * was stepped during a session (network time sync, a manual change) a session of more than two minutes was measured as a few
 * seconds, fell under [MIN_SESSION_MS] and was thrown away. The length must come from the monotonic clock.
 */
class SessionClockTest {
    private val minute = 60_000L

    @Test fun `a wall clock stepped back during a session does not shorten it`() {
        val m = SessionMachine()
        m.onShowing(true, SessionSource.DOCK_NOW, now = 1_000_000, elapsed = 0)
        // 2 min 5 s really pass, but the wall clock was stepped back by 110 s in the middle.
        m.onShowing(false, SessionSource.DOCK_NOW, now = 1_000_000 + 15_000, elapsed = 125_000)
        val done = m.finishIfDue(now = 1_000_000 + 15_000 + HANDOVER_GRACE_MS + 100, elapsed = 125_000 + HANDOVER_GRACE_MS + 100)
        assertNotNull(done)
        assertEquals(125_000L, done!!.durationMs)
        assertTrue(done.worthKeeping)
    }

    @Test fun `a wall clock stepped forward does not lengthen it`() {
        val m = SessionMachine()
        m.onShowing(true, SessionSource.SCREENSAVER, now = 0, elapsed = 0)
        m.onShowing(false, SessionSource.SCREENSAVER, now = 10 * 60 * minute, elapsed = 30_000)
        val done = m.finishIfDue(now = 10 * 60 * minute + 4_000, elapsed = 30_000 + HANDOVER_GRACE_MS + 100)!!
        assertEquals(30_000L, done.durationMs)
        assertTrue(!done.worthKeeping)
    }

    @Test fun `the handover grace is measured on the monotonic clock too`() {
        val m = SessionMachine()
        m.onShowing(true, SessionSource.DOCK_NOW, now = 0, elapsed = 0)
        m.onShowing(false, SessionSource.DOCK_NOW, now = 3 * minute, elapsed = 3 * minute)
        // The wall clock jumped a day ahead, but only one second has passed: still inside the grace.
        assertEquals(null, m.finishIfDue(now = 3 * minute + 86_400_000L, elapsed = 3 * minute + HANDOVER_GRACE_MS - 1_000))
    }

    /**
     * Reproduced on the emulator: the dock Activity is recreated (the screen turns, the font or theme changes), the dock is gone
     * for about four seconds, and the one session became two halves, each under two minutes, so neither was kept.
     */
    @Test fun `a dock recreated in six seconds is still one session`() {
        val m = SessionMachine()
        m.onShowing(true, SessionSource.DOCK_NOW, now = 0, elapsed = 0)
        m.onShowing(false, SessionSource.DOCK_NOW, now = 96_000, elapsed = 96_000) // the Activity is destroyed
        assertEquals(null, m.finishIfDue(now = 96_000 + 5_000, elapsed = 96_000 + 5_000)) // the new one is still starting
        m.onShowing(true, SessionSource.DOCK_NOW, now = 96_000 + 6_000, elapsed = 96_000 + 6_000)
        m.onShowing(false, SessionSource.DOCK_NOW, now = 130_000, elapsed = 130_000)
        val done = m.finishIfDue(now = 130_000 + HANDOVER_GRACE_MS + 100, elapsed = 130_000 + HANDOVER_GRACE_MS + 100)!!
        assertEquals(130_000L, done.durationMs)
        assertTrue(done.worthKeeping)
    }

    @Test fun `screensaver and Dock now sessions both count toward the week`() {
        val zone = ZoneId.of("UTC")
        val now = 3L * 86_400_000L + 12 * 60 * minute // day 3, noon
        fun session(source: SessionSource, start: Long, mins: Long) =
            FocusSessionEntity(startedAt = start, endedAt = start + mins * minute, source = source.name, notificationsHeld = 0, vipThrough = 0, tasksCompleted = 0)
        val week = weeklyFocus(
            listOf(
                session(SessionSource.SCREENSAVER, now - 3 * 60 * minute, 40),
                session(SessionSource.DOCK_NOW, now - 2 * 60 * minute, 25),
                session(SessionSource.WATCHER, now - 60 * minute, 10)
            ),
            now, zone
        )
        assertEquals(75 * minute, week.totalMillis)
    }
}
