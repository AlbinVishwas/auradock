package com.auradock.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class CalendarMappingTest {

    @Test
    fun `today range covers exactly the local calendar day`() {
        val zone = ZoneId.of("Asia/Kolkata") // UTC+5:30, no DST
        val noon = LocalDate.of(2026, 10, 22).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        val (start, end) = todayRange(noon, zone)
        assertEquals(LocalDate.of(2026, 10, 22).atStartOfDay(zone).toInstant().toEpochMilli(), start)
        assertEquals(24 * 3_600_000L, end - start)
    }

    @Test
    fun `just before midnight still belongs to the same day`() {
        val zone = ZoneId.of("Asia/Kolkata")
        val late = LocalDate.of(2026, 10, 22).atTime(23, 59, 59).atZone(zone).toInstant().toEpochMilli()
        val (start, end) = todayRange(late, zone)
        assertTrue(late in start until end)
    }

    @Test
    fun `daylight saving days are 23 or 25 hours long, not 24`() {
        val zone = ZoneId.of("America/New_York")
        val springForward = LocalDate.of(2026, 3, 8).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        val (s1, e1) = todayRange(springForward, zone)
        assertEquals(23 * 3_600_000L, e1 - s1)
        val fallBack = LocalDate.of(2026, 11, 1).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        val (s2, e2) = todayRange(fallBack, zone)
        assertEquals(25 * 3_600_000L, e2 - s2)
    }

    private val today = LocalDate.of(2026, 10, 22)
    private fun utcMidnight(d: LocalDate) = d.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    private fun allDay(title: String, first: LocalDate, lastExclusive: LocalDate) =
        InstanceRow(title, utcMidnight(first), utcMidnight(lastExclusive), allDay = true)

    @Test
    fun `all-day events sort before timed ones, timed by start`() {
        val events = rowsToEvents(
            listOf(
                InstanceRow("Late", 3_000, 4_000, allDay = false),
                allDay("Birthday", today, today.plusDays(1)),
                InstanceRow("Early", 1_000, 2_000, allDay = false)
            ),
            today
        )
        assertEquals(listOf("Birthday", "Early", "Late"), events.map { it.title })
        assertEquals(listOf(true, false, false), events.map { it.allDay })
    }

    @Test
    fun `all-day events from yesterday and tomorrow are dropped`() {
        // The provider can hand these back when the local day overlaps a neighbouring UTC day.
        val events = rowsToEvents(
            listOf(
                allDay("Yesterday", today.minusDays(1), today),
                allDay("Today", today, today.plusDays(1)),
                allDay("Tomorrow", today.plusDays(1), today.plusDays(2))
            ),
            today
        )
        assertEquals(listOf("Today"), events.map { it.title })
    }

    @Test
    fun `multi-day all-day events show on every day they cover`() {
        val trip = allDay("Trip", today.minusDays(1), today.plusDays(2)) // covers 21st, 22nd, 23rd
        assertEquals(1, rowsToEvents(listOf(trip), today).size)
        assertEquals(1, rowsToEvents(listOf(trip), today.minusDays(1)).size)
        assertEquals(1, rowsToEvents(listOf(trip), today.plusDays(1)).size)
        assertEquals(0, rowsToEvents(listOf(trip), today.plusDays(2)).size)
    }

    @Test
    fun `timed events are never filtered by date`() {
        val overnight = InstanceRow("Overnight", 1_000, 2_000, allDay = false)
        assertEquals(1, rowsToEvents(listOf(overnight), today).size)
    }

    @Test
    fun `missing or blank titles become No title`() {
        val events = rowsToEvents(
            listOf(InstanceRow(null, 1, 2, false), InstanceRow("  ", 2, 3, false)),
            today
        )
        assertEquals(listOf("(No title)", "(No title)"), events.map { it.title })
    }
}
