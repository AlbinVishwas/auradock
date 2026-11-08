package com.auradock.sessions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionRulesTest {
    private val minute = 60_000L

    @Test
    fun `a session starts when a dock shows and ends once the grace has passed`() {
        val m = SessionMachine()
        m.onShowing(true, SessionSource.DOCK_NOW, now = 1_000)
        assertEquals(1_000L, m.live?.startedAt)
        m.onShowing(false, SessionSource.DOCK_NOW, now = 10 * minute)
        assertNotNull(m.live) // still waiting out the grace
        assertNull(m.finishIfDue(now = 10 * minute + HANDOVER_GRACE_MS - 1))
        val done = m.finishIfDue(now = 10 * minute + HANDOVER_GRACE_MS)
        assertNotNull(done)
        assertEquals(10 * minute, done!!.endedAt) // it ended when the dock closed, not after the grace
        assertNull(m.live)
    }

    @Test
    fun `a handover inside the grace is one session`() {
        val m = SessionMachine()
        m.onShowing(true, SessionSource.SCREENSAVER, now = 0)
        m.onShowing(false, SessionSource.SCREENSAVER, now = 5 * minute)
        m.onShowing(true, SessionSource.DOCK_NOW, now = 5 * minute + 1_000) // the app takes over
        assertNull(m.finishIfDue(now = 5 * minute + 10_000))
        assertEquals(0L, m.live?.startedAt)
        assertEquals(SessionSource.SCREENSAVER, m.live?.source) // the first dock names the session
    }

    @Test
    fun `opening a dock after the grace starts a new session`() {
        val m = SessionMachine()
        m.onShowing(true, SessionSource.DOCK_NOW, now = 0)
        m.onShowing(false, SessionSource.DOCK_NOW, now = 3 * minute)
        val first = m.finishIfDue(now = 3 * minute + HANDOVER_GRACE_MS)
        assertNotNull(first)
        m.onShowing(true, SessionSource.WATCHER, now = 4 * minute)
        assertEquals(4 * minute, m.live?.startedAt)
        assertEquals(SessionSource.WATCHER, m.live?.source)
    }

    @Test
    fun `a session under two minutes is not worth keeping and two minutes is`() {
        val short = FinishedSession(0, MIN_SESSION_MS - 1, SessionSource.DOCK_NOW, SessionCounts())
        val exact = FinishedSession(0, MIN_SESSION_MS, SessionSource.DOCK_NOW, SessionCounts())
        assertFalse(short.worthKeeping)
        assertTrue(exact.worthKeeping)
    }

    @Test
    fun `a phone turned sideways for a moment leaves no session worth keeping`() {
        val m = SessionMachine()
        m.onShowing(true, SessionSource.WATCHER, now = 0)
        m.onShowing(false, SessionSource.WATCHER, now = 40_000)
        val done = m.finishIfDue(now = 40_000 + HANDOVER_GRACE_MS)!!
        assertFalse(done.worthKeeping)
    }

    @Test
    fun `notifications are counted while docked and carried into the finished session`() {
        val m = SessionMachine()
        m.count(Arrival.HELD) // nothing is docked: not counted
        m.onShowing(true, SessionSource.DOCK_NOW, now = 0)
        repeat(3) { m.count(Arrival.HELD) }
        m.count(Arrival.THROUGH)
        m.count(Arrival.IGNORED)
        assertEquals(SessionCounts(held = 3, through = 1), m.live?.counts)
        m.onShowing(false, SessionSource.DOCK_NOW, now = 5 * minute)
        m.count(Arrival.HELD) // the dock has closed: not counted
        val done = m.finishIfDue(now = 5 * minute + HANDOVER_GRACE_MS)!!
        assertEquals(SessionCounts(held = 3, through = 1), done.counts)
    }

    @Test
    fun `a closed dock twice in a row does not move the end`() {
        val m = SessionMachine()
        m.onShowing(true, SessionSource.DOCK_NOW, now = 0)
        m.onShowing(false, SessionSource.DOCK_NOW, now = 3 * minute)
        m.onShowing(false, SessionSource.DOCK_NOW, now = 3 * minute + 1_000)
        assertEquals(3 * minute, m.finishIfDue(now = 4 * minute)!!.endedAt)
    }

    private fun arrival(
        ongoing: Boolean = false, groupSummary: Boolean = false, empty: Boolean = false, own: Boolean = false,
        picked: Boolean = false, dndActive: Boolean = true, allowedByDnd: Boolean = false
    ) = classifyArrival(ongoing, groupSummary, empty, own, picked, dndActive, allowedByDnd)

    @Test
    fun `a notification from an app that is not picked is held when Do Not Disturb filters it`() {
        assertEquals(Arrival.HELD, arrival())
    }

    @Test
    fun `a picked app's notification goes through, and so does one Do Not Disturb allows`() {
        assertEquals(Arrival.THROUGH, arrival(picked = true))
        assertEquals(Arrival.THROUGH, arrival(allowedByDnd = true))
    }

    @Test
    fun `nothing is held when Do Not Disturb is not filtering`() {
        assertEquals(Arrival.IGNORED, arrival(dndActive = false))
        assertEquals(Arrival.THROUGH, arrival(dndActive = false, picked = true))
    }

    @Test
    fun `ongoing, group headers, empty ones and our own are never counted`() {
        assertEquals(Arrival.IGNORED, arrival(ongoing = true, picked = true))
        assertEquals(Arrival.IGNORED, arrival(groupSummary = true))
        assertEquals(Arrival.IGNORED, arrival(empty = true))
        assertEquals(Arrival.IGNORED, arrival(own = true, picked = true))
    }
}
