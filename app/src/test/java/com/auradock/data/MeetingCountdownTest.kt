package com.auradock.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MeetingCountdownTest {
    private val min = 60_000L
    private val now = 10_000 * min
    private fun event(inMin: Long, title: String = "Standup", allDay: Boolean = false, declined: Boolean = false) =
        CalendarEvent(title, now + inMin * min, allDay, declined)

    @Test
    fun `nothing until ten minutes before`() {
        assertNull(meetingCountdown(listOf(event(11)), now))
        assertEquals(MeetingCountdown("Standup", 10), meetingCountdown(listOf(event(10)), now))
    }

    @Test
    fun `minutes round up and it is gone at the start time`() {
        assertEquals(8, meetingCountdown(listOf(event(8)), now)!!.minutes)
        assertEquals(1, meetingCountdown(listOf(CalendarEvent("Standup", now + 30_000)), now)!!.minutes)
        assertNull(meetingCountdown(listOf(event(0)), now))
        assertNull(meetingCountdown(listOf(event(-3)), now))
    }

    @Test
    fun `pulses from two minutes`() {
        assertFalse(meetingCountdown(listOf(event(3)), now)!!.pulsing)
        assertTrue(meetingCountdown(listOf(event(2)), now)!!.pulsing)
        assertTrue(meetingCountdown(listOf(event(1)), now)!!.pulsing)
    }

    @Test
    fun `all-day and declined events are skipped, the next real one counts`() {
        val events = listOf(event(5, "Holiday", allDay = true), event(4, "Declined", declined = true), event(7, "Design review"))
        assertEquals("Design review", meetingCountdown(events, now)!!.title)
        assertNull(meetingCountdown(listOf(event(5, allDay = true), event(4, declined = true)), now))
    }

    @Test
    fun `the earliest of several is used`() {
        val events = listOf(event(9, "Later"), event(3, "Sooner"))
        assertEquals("Sooner", meetingCountdown(events, now)!!.title)
    }
}
