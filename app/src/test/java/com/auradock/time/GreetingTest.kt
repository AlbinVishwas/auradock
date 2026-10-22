package com.auradock.time

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class GreetingTest {
    private val zone = ZoneId.of("Asia/Kolkata")

    private fun at(hour: Int, minute: Int = 0) =
        LocalDate.of(2026, 10, 22).atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun `each part of the day gets its greeting`() {
        assertEquals("Good morning", greetingFor(at(8), zone))
        assertEquals("Good afternoon", greetingFor(at(14), zone))
        assertEquals("Good evening", greetingFor(at(18), zone))
        assertEquals("Good night", greetingFor(at(23), zone))
    }

    @Test
    fun `boundaries switch exactly on the hour`() {
        assertEquals("Good night", greetingFor(at(4, 59), zone))
        assertEquals("Good morning", greetingFor(at(5, 0), zone))
        assertEquals("Good morning", greetingFor(at(11, 59), zone))
        assertEquals("Good afternoon", greetingFor(at(12, 0), zone))
        assertEquals("Good afternoon", greetingFor(at(16, 59), zone))
        assertEquals("Good evening", greetingFor(at(17, 0), zone))
        assertEquals("Good evening", greetingFor(at(21, 59), zone))
        assertEquals("Good night", greetingFor(at(22, 0), zone))
    }

    @Test
    fun `midnight and the small hours are night`() {
        assertEquals("Good night", greetingFor(at(0), zone))
        assertEquals("Good night", greetingFor(at(3), zone))
    }

    @Test
    fun `uses the time zone it is given`() {
        val moment = at(8) // 08:00 in India = 02:30 UTC
        assertEquals("Good morning", greetingFor(moment, zone))
        assertEquals("Good night", greetingFor(moment, ZoneId.of("UTC")))
    }
}
