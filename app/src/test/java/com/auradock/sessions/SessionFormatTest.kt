package com.auradock.sessions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionFormatTest {
    private val minute = 60_000L

    @Test fun `under an hour is minutes`() = assertEquals("45 m", formatFocusTime(45 * minute))
    @Test fun `an hour and minutes`() = assertEquals("1 h 12 m", formatFocusTime(72 * minute))
    @Test fun `whole hours drop the minutes`() = assertEquals("3 h", formatFocusTime(180 * minute))
    @Test fun `seconds are rounded down`() = assertEquals("2 m", formatFocusTime(2 * minute + 59_000))
    @Test fun `nothing is zero minutes`() = assertEquals("0 m", formatFocusTime(-5))

    @Test fun `one task reads in the singular`() {
        assertEquals("task done", tasksDoneLabel(1))
        assertEquals("tasks done", tasksDoneLabel(0))
        assertEquals("tasks done", tasksDoneLabel(3))
    }

    @Test fun `a card is fresh for ten minutes after the session`() {
        assertTrue(summaryIsFresh(endedAt = 1_000, now = 1_000 + SUMMARY_MAX_AGE_MS))
        assertFalse(summaryIsFresh(endedAt = 1_000, now = 1_001 + SUMMARY_MAX_AGE_MS))
    }

    @Test fun `the minute clock can be a little before the end and the card still shows`() {
        assertTrue(summaryIsFresh(endedAt = 90_000, now = 60_000))
    }

    @Test fun `the card is for the session as it stands when the dock closes, before the grace is over`() {
        val m = SessionMachine()
        assertNull(m.provisional())
        m.onShowing(true, SessionSource.DOCK_NOW, now = 0)
        assertNull(m.provisional()) // still docked
        m.count(Arrival.HELD)
        m.onShowing(false, SessionSource.DOCK_NOW, now = 3 * minute)
        val card = m.provisional()
        assertNotNull(card)
        assertEquals(3 * minute, card!!.durationMs)
        assertEquals(1, card.counts.held)
        assertTrue(card.worthKeeping)
        m.onShowing(true, SessionSource.DOCK_NOW, now = 3 * minute + 1_000) // a handover: no card
        assertNull(m.provisional())
    }
}
