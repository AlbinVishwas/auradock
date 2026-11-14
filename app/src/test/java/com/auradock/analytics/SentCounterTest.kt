package com.auradock.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SentCounterTest {
    @Test fun `the count goes up per sent event`() {
        val counter = SentCounter()
        counter.recordSent()
        counter.recordSent()
        counter.recordSent()
        assertEquals(3, counter.takeSentCount())
    }

    @Test fun `taking the count returns it and resets it to 0`() {
        val counter = SentCounter()
        counter.recordSent()
        counter.recordSent()
        assertEquals(2, counter.takeSentCount())
        assertEquals(0, counter.takeSentCount())
        counter.recordSent()
        assertEquals(1, counter.takeSentCount())
    }

    @Test fun `a take with nothing sent gives 0`() {
        assertEquals(0, SentCounter().takeSentCount())
    }

    @Test fun `the message is the number only`() {
        assertEquals("flush sent, events=3", SentCounter.flushMessage(3))
        assertEquals("flush sent, events=0", SentCounter.flushMessage(0))
    }

    @Test fun `an event counts when sharing is on and the hand-off worked`() {
        assertTrue(SentCounter.shouldCount(sharing = true, handedOver = true))
    }

    @Test fun `an event does not count when sharing is off`() {
        assertFalse(SentCounter.shouldCount(sharing = false, handedOver = true))
    }

    @Test fun `an event does not count when the hand-off threw`() {
        assertFalse(SentCounter.shouldCount(sharing = true, handedOver = false))
    }

    @Test fun `an event does not count when sharing is off and the hand-off threw`() {
        assertFalse(SentCounter.shouldCount(sharing = false, handedOver = false))
    }
}
