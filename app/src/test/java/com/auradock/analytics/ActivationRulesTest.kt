package com.auradock.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class ActivationRulesTest {
    private val zone = ZoneId.of("UTC")
    private val hour = 3_600_000L
    private fun day(d: Int, h: Int = 12): Long = LocalDate.of(2026, 11, d).atStartOfDay(zone).toInstant().toEpochMilli() + h * hour
    private fun ev(name: String, at: Long, detail: String? = null) = AppEvent(name, at, detail)

    @Test fun `activated when the screensaver is set and the first dock happens within 24 hours of install`() {
        val installed = day(1, 8)
        val events = listOf(ev("screensaver_set", installed + 2 * hour), ev("first_dock", installed + 23 * hour, "DOCK_NOW"))
        assertTrue(activationSummary(events, installed, day(2), zone).activatedWithin24h)
    }

    @Test fun `not activated if one of the two comes late or is missing`() {
        val installed = day(1, 8)
        assertFalse(activationSummary(listOf(ev("screensaver_set", installed + 25 * hour), ev("first_dock", installed + hour)), installed, day(5), zone).activatedWithin24h)
        assertFalse(activationSummary(listOf(ev("first_dock", installed + hour)), installed, day(5), zone).activatedWithin24h)
        assertFalse(activationSummary(emptyList(), installed, day(5), zone).activatedWithin24h)
    }

    @Test fun `docked days count different days only, over the last seven`() {
        val events = listOf(
            ev("session_start", day(9, 7)), ev("session_start", day(9, 20)), // same day
            ev("session_start", day(8)), ev("session_start", day(6)), ev("session_start", day(4)), ev("session_start", day(3)),
            ev("session_start", day(2)) // too old: the window is 3..9
        )
        assertEquals(5, dockedDays(events, day(9, 22), zone))
    }

    @Test fun `five of the last seven is the line`() {
        val four = (6..9).map { ev("session_start", day(it)) }
        assertFalse(activationSummary(four, day(1), day(9), zone).dockedFiveOfSeven)
        val five = (5..9).map { ev("session_start", day(it)) }
        assertTrue(activationSummary(five, day(1), day(9), zone).dockedFiveOfSeven)
    }

    @Test fun `only session starts count as docking`() {
        val events = (5..9).map { ev("task_added", day(it)) }
        assertEquals(0, dockedDays(events, day(9), zone))
    }

    @Test fun `counts list the most frequent first`() {
        val s = activationSummary(listOf(ev("a", 1), ev("b", 2), ev("b", 3), ev("c", 4)), 0, 10, zone)
        assertEquals(listOf("b" to 2, "a" to 1, "c" to 1), s.counts)
        assertEquals(4, s.total)
    }
}
