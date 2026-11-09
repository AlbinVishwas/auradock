package com.auradock.intention

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class IntentionRulesTest {
    private val zone = ZoneId.of("Asia/Kolkata")
    private fun at(day: Int, hour: Int, minute: Int = 0) = ZonedDateTime.of(2026, 11, day, hour, minute, 0, 0, zone).toInstant().toEpochMilli()

    @Test
    fun `the day starts at 4 am`() {
        assertEquals("2026-11-08", intentionDay(at(9, 3, 59), zone))
        assertEquals("2026-11-09", intentionDay(at(9, 4, 0), zone))
        assertEquals("2026-11-09", intentionDay(at(9, 23, 59), zone))
        assertEquals("2026-11-09", intentionDay(at(10, 3, 59), zone))
    }

    @Test
    fun `tomorrow follows the same 4 am rule`() {
        assertEquals("2026-11-10", nextIntentionDay(at(9, 20), zone))
        assertEquals("2026-11-09", nextIntentionDay(at(9, 2), zone)) // still last night: tomorrow is today's date
    }

    @Test
    fun `clean intention trims joins and cuts`() {
        assertEquals("Ship the invoice", cleanIntention("  Ship \n the   invoice  "))
        assertNull(cleanIntention("   "))
        assertEquals(INTENTION_MAX_CHARS, cleanIntention("x".repeat(200))!!.length)
    }

    @Test
    fun `the question is asked when nothing is stored or only a suggestion waits`() {
        assertTrue(needsOffer(null))
        assertTrue(needsOffer(IntentionEntity("d", "Draft", status = IntentionStatus.SUGGESTED.name)))
        assertFalse(needsOffer(IntentionEntity("d", status = IntentionStatus.OFFERED.name)))
        assertFalse(needsOffer(IntentionEntity("d", "x", status = IntentionStatus.SET.name)))
        assertFalse(needsOffer(IntentionEntity("d", status = IntentionStatus.SKIPPED.name)))
    }

    @Test
    fun `only a written intention is shown`() {
        assertNull(intentionLine(null))
        assertNull(intentionLine(IntentionEntity("d", status = IntentionStatus.SKIPPED.name)))
        assertNull(intentionLine(IntentionEntity("d", "Draft", status = IntentionStatus.SUGGESTED.name)))
        assertEquals(IntentionLine("Ship", true), intentionLine(IntentionEntity("d", "Ship", done = true, status = IntentionStatus.SET.name)))
    }
}
