package com.auradock.ui.screens.dashboard

import com.auradock.data.CalendarEvent
import com.auradock.ui.viewmodel.CalendarPermissionUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TodaySummaryTest {

    private val events = listOf(
        CalendarEvent("Team standup", 1_000),
        CalendarEvent("Investor call", 2_000),
        CalendarEvent("Ship beta", 3_000)
    )
    private val fmt: (Long) -> String = { "t${it / 1000}" }

    @Test
    fun `granted lists events with times`() {
        val s = buildTodaySummary(events, CalendarPermissionUiState.GRANTED, fmt)
        assertEquals(listOf("t1  Team standup", "t2  Investor call", "t3  Ship beta"), s.lines)
        assertNull(s.actionText)
    }

    @Test
    fun `granted with no events shows the empty text and no button`() {
        val s = buildTodaySummary(emptyList(), CalendarPermissionUiState.GRANTED, fmt)
        assertEquals(emptyList<String>(), s.lines)
        assertEquals("No events today", s.emptyText)
        assertNull(s.actionText)
    }

    @Test
    fun `long lists are capped with a +N more line`() {
        val many = (1..7).map { CalendarEvent("e$it", it * 1000L) }
        val s = buildTodaySummary(many, CalendarPermissionUiState.GRANTED, fmt, maxLines = 4)
        assertEquals(5, s.lines.size)
        assertEquals("+3 more", s.lines.last())
    }

    @Test
    fun `denied hides events and offers to ask again`() {
        val s = buildTodaySummary(events, CalendarPermissionUiState.DENIED, fmt)
        assertEquals(emptyList<String>(), s.lines)
        assertEquals("Enable Calendar Access", s.actionText)
    }

    @Test
    fun `permanently denied sends the user to settings`() {
        val s = buildTodaySummary(events, CalendarPermissionUiState.PERMANENTLY_DENIED, fmt)
        assertEquals(emptyList<String>(), s.lines)
        assertEquals("Open Settings", s.actionText)
    }

    @Test
    fun `all-day events show All day instead of a time`() {
        val all = listOf(CalendarEvent("Priya's birthday", 5_000, allDay = true), CalendarEvent("Standup", 1_000))
        val s = buildTodaySummary(all, CalendarPermissionUiState.GRANTED, fmt)
        assertEquals(listOf("All day  Priya's birthday", "t1  Standup"), s.lines)
    }

    @Test
    fun `before the first load it says nothing rather than claiming no events`() {
        val s = buildTodaySummary(null, CalendarPermissionUiState.GRANTED, fmt)
        assertEquals(emptyList<String>(), s.lines)
        assertEquals("", s.emptyText)
        assertNull(s.actionText)
    }
}
