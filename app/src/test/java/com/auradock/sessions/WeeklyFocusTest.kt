package com.auradock.sessions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class WeeklyFocusTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private val minute = 60_000L
    private fun at(day: Int, hour: Int, minute: Int = 0): Long =
        LocalDateTime.of(2026, 11, day, hour, minute).atZone(zone).toInstant().toEpochMilli()

    private fun session(start: Long, end: Long) =
        FocusSessionEntity(startedAt = start, endedAt = end, source = "DOCK_NOW", notificationsHeld = 0, vipThrough = 0, tasksCompleted = 0)

    private val now = at(9, 18) // Monday 9 November, evening

    @Test fun `no sessions is an empty week with seven zero days`() {
        val week = weeklyFocus(emptyList(), now, zone)
        assertEquals(List(7) { 0L }, week.perDayMillis)
        assertTrue(week.isEmpty)
    }

    @Test fun `today is the last bar and the days before it come in order`() {
        val week = weeklyFocus(
            listOf(session(at(9, 10), at(9, 11)), session(at(8, 10), at(8, 12)), session(at(3, 9), at(3, 9, 30))),
            now, zone
        )
        assertEquals(60 * minute, week.perDayMillis[6])
        assertEquals(120 * minute, week.perDayMillis[5])
        assertEquals(30 * minute, week.perDayMillis[0]) // 3 November is six days before the 9th
        assertEquals(210 * minute, week.totalMillis)
        assertFalse(week.isEmpty)
    }

    @Test fun `a session across midnight is split between the two days`() {
        val week = weeklyFocus(listOf(session(at(8, 23, 30), at(9, 0, 45))), now, zone)
        assertEquals(30 * minute, week.perDayMillis[5])
        assertEquals(45 * minute, week.perDayMillis[6])
    }

    @Test fun `sessions older than the window are left out`() {
        val week = weeklyFocus(listOf(session(at(2, 10), at(2, 12))), now, zone)
        assertEquals(0L, week.totalMillis)
        assertEquals(at(3, 0), weekWindowStart(now, zone))
    }

    @Test fun `less than a minute in total counts as an empty week`() {
        assertTrue(WeeklyFocus(listOf(0, 0, 0, 0, 0, 0, 30_000L)).isEmpty)
        assertFalse(WeeklyFocus(listOf(0, 0, 0, 0, 0, 0, 60_000L)).isEmpty)
    }
}
