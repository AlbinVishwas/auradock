package com.auradock.monitoring

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EventRateLimiterTest {
    @Test fun `the eleventh identical event in an hour is dropped`() {
        val l = EventRateLimiter()
        repeat(10) { assertTrue(l.allow("a", it * 1000L)) }
        assertFalse(l.allow("a", 11_000L))
    }

    @Test fun `different issues have their own share`() {
        val l = EventRateLimiter()
        repeat(10) { l.allow("a", 0L) }
        assertTrue(l.allow("b", 0L))
    }

    @Test fun `the share comes back after an hour`() {
        val l = EventRateLimiter()
        repeat(10) { l.allow("a", 0L) }
        assertFalse(l.allow("a", 59 * 60_000L))
        assertTrue(l.allow("a", 60 * 60_000L + 1))
    }
}

class ScrubberTest {
    @Test fun `only codes, numbers and booleans may leave`() {
        assertEquals("gate_passed", Scrubber.safeValue("gate_passed"))
        assertEquals(3, Scrubber.safeValue(3))
        assertEquals(true, Scrubber.safeValue(true))
        assertEquals(null, Scrubber.safeValue("Call Mum about the invoice")) // a sentence
        assertEquals(null, Scrubber.safeValue("Asha Rao")) // a name
        assertEquals(null, Scrubber.safeValue("+91 98765 43210")) // a number
        assertEquals(null, Scrubber.safeValue(listOf("x")))
    }
}

class ScrubberEventTest {
    private fun crumb(category: String, message: String?, vararg data: Pair<String, Any?>) =
        io.sentry.Breadcrumb().apply { this.category = category; this.message = message; data.forEach { (k, v) -> setData(k, v) } }

    @Test fun `an event leaves with no message, no exception text, no user and only code tags`() {
        val e = io.sentry.SentryEvent().apply {
            exceptions = listOf(io.sentry.protocol.SentryException().apply { type = "IllegalStateException"; value = "Call Mum about the invoice" })
            message = io.sentry.protocol.Message().apply { formatted = "Task: Call Mum" }
            user = io.sentry.protocol.User().apply { email = "a@b.c"; username = "Albin" }
            setTag("aura.code", "dnd.restore_failed")
            setTag("title", "Call Mum about the invoice")
            setTag("aura.who", "Asha Rao")
            setExtra("task", "Call Mum")
        }
        val clean = Scrubber.scrubEvent(e)
        assertEquals(null, clean.message)
        assertEquals(null, clean.user)
        assertEquals(null, clean.exceptions?.firstOrNull()?.value)
        assertEquals("IllegalStateException", clean.exceptions?.firstOrNull()?.type)
        assertEquals(mapOf("aura.code" to "dnd.restore_failed"), clean.tags)
        assertTrue(clean.extras.isNullOrEmpty())
    }

    @Test fun `only our own code breadcrumbs and a few lifecycle ones survive`() {
        assertEquals(null, Scrubber.scrubBreadcrumb(crumb("http", "GET https://api.open-meteo.com/?latitude=19.03&longitude=73.03")))
        assertEquals(null, Scrubber.scrubBreadcrumb(crumb("ui.click", "Save")))
        assertEquals(null, Scrubber.scrubBreadcrumb(crumb("console", "title=Call Mum")))
        val own = Scrubber.scrubBreadcrumb(crumb("aura.screensaver.gate", "passed", "count" to 4, "who" to "Asha Rao"))!!
        assertEquals("passed", own.message)
        assertEquals(4, own.data["count"])
        assertTrue("who" !in own.data) // a name is not a code
        val life = Scrubber.scrubBreadcrumb(crumb("ui.lifecycle", "created", "class" to "MainActivity", "state" to "resumed"))!!
        assertEquals(null, life.message)
        assertEquals(mapOf("state" to "resumed"), life.data.toMap())
    }

    @Test fun `our own non-fatal report keeps its code as the message`() {
        val e = io.sentry.SentryEvent().apply {
            exceptions = listOf(io.sentry.protocol.SentryException().apply { type = Scrubber.NON_FATAL_TYPE; value = "alarm.schedule_failed" })
        }
        assertEquals("alarm.schedule_failed", Scrubber.scrubEvent(e).exceptions?.firstOrNull()?.value)
    }
}
