package com.auradock.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EventRulesTest {
    private val now = 10_000L
    private fun e(title: String, start: Long, allDay: Boolean = false) = CalendarEvent(title, start, allDay)

    @Test
    fun `the next event is the earliest one that has not started`() {
        val events = listOf(e("Later", 30_000), e("Earlier", 20_000), e("Past", 5_000))
        assertEquals("Earlier", nextEvent(events, now)?.title)
    }

    @Test
    fun `an event starting right now still counts as next`() =
        assertEquals("Now", nextEvent(listOf(e("Now", now)), now)?.title)

    @Test
    fun `nothing left today gives null`() {
        assertNull(nextEvent(listOf(e("Past", 1_000)), now))
        assertNull(nextEvent(emptyList(), now))
    }

    @Test
    fun `all-day events are never next`() =
        assertNull(nextEvent(listOf(e("Birthday", 50_000, allDay = true)), now))

    @Test
    fun `upcoming events are in time order and exclude finished and all-day ones`() {
        val events = listOf(
            e("C", 40_000), e("Birthday", 1, allDay = true), e("A", 15_000), e("Past", 2_000), e("B", 20_000)
        )
        assertEquals(listOf("A", "B", "C"), upcomingEvents(events, now).map { it.title })
    }
}
