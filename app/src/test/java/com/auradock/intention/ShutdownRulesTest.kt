package com.auradock.intention

import com.auradock.tasks.TaskEntity
import com.auradock.tasks.isVisibleToday
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class ShutdownRulesTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private fun at(day: Int, hour: Int, minute: Int = 0) = ZonedDateTime.of(2026, 11, day, hour, minute, 0, 0, zone).toInstant().toEpochMilli()

    @Test
    fun `evening is 6 pm to 4 am`() {
        assertFalse(isEvening(at(9, 17, 59), zone))
        assertTrue(isEvening(at(9, 18), zone))
        assertTrue(isEvening(at(10, 1), zone))
        assertFalse(isEvening(at(10, 4), zone))
    }

    @Test
    fun `asked once per evening`() {
        assertTrue(shouldOfferShutdown(at(9, 19), null, zone))
        assertFalse(shouldOfferShutdown(at(9, 20), "2026-11-09", zone)) // already asked this evening
        assertTrue(shouldOfferShutdown(at(10, 19), "2026-11-09", zone)) // a new evening
        assertFalse(shouldOfferShutdown(at(9, 12), null, zone)) // midday
        assertFalse(shouldOfferShutdown(at(10, 1), "2026-11-09", zone)) // 1 am is still the evening of the 9th
    }

    @Test
    fun `tomorrow's task is due tomorrow and waits for midnight`() {
        assertEquals(at(10, 9, 30), tomorrowDueAt(at(9, 19), 9, 30, zone))
        assertEquals(at(10, 0), tomorrowStart(at(9, 19), zone))
        val task = TaskEntity(title = "Draft", createdAt = at(9, 19), showFrom = tomorrowStart(at(9, 19), zone))
        val midnight = at(9, 0)
        assertFalse(isVisibleToday(task, midnight, endOfToday = at(10, 0)))
        assertTrue(isVisibleToday(task, at(10, 0), endOfToday = at(11, 0)))
    }
}
