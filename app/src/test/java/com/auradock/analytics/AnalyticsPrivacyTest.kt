package com.auradock.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

/**
 * End to end through [Analytics.capture] (what the app calls) and [AnalyticsScrubber.fromLocalEvent] (what the local event log feeds it): whatever
 * the caller passes, what is handed over for sending holds only fixed codes, counts and buckets.
 */
class AnalyticsPrivacyTest {
    @Before fun clear() { Analytics.sent.clear() }

    private val things = listOf(
        "Call the dentist about the crown", "Draft the Q1 plan", "Mum: are you coming for dinner?", "Priya Sharma", "+91 98765 43210",
        "Dentist 3 pm", "Blinding Lights - The Weeknd", "Pay rent", "hunter2", "me@example.com"
    )

    private fun leaks(text: String) = Analytics.sent.any { (name, props) -> name.contains(text) || props.any { (k, v) -> k.contains(text) || v.toString().contains(text) } }

    @Test fun `a title in any property of any event never reaches the send queue`() {
        for (event in AnalyticsScrubber.EVENT_PROPERTIES.keys) for (text in things) {
            Analytics.capture(event, mapOf("title" to text, "text" to text, "name" to text, "contact" to text, "step" to text, "source" to text, "widget" to text, "count" to text))
        }
        things.forEach { assertFalse("'$it' leaked", leaks(it)) }
    }

    @Test fun `the local event log's detail is mapped, never forwarded`() {
        for (event in AnalyticsScrubber.EVENT_PROPERTIES.keys) for (text in things) {
            Analytics.capture(event, AnalyticsScrubber.fromLocalEvent(event, text))
        }
        things.forEach { assertFalse("'$it' leaked", leaks(it)) }
    }

    @Test fun `an event that is not listed is not queued at all`() {
        Analytics.capture("notification_received", mapOf("count" to 1))
        Analytics.capture("task_title_changed", mapOf("title" to "x"))
        assertTrue(Analytics.sent.isEmpty())
    }

    @Test fun `random strings never become a property value unless they are a listed code`() {
        val random = Random(7)
        val alphabet = "abcdefghijklmnopqrstuvwxyz ABCDEFGHIJ0123456789_-:;.,'\"@+"
        repeat(4_000) {
            val text = String(CharArray(random.nextInt(1, 40)) { alphabet[random.nextInt(alphabet.length)] })
            for (event in AnalyticsScrubber.EVENT_PROPERTIES.keys) {
                val cleaned = AnalyticsScrubber.clean(event, AnalyticsScrubber.EVENT_PROPERTIES.getValue(event).associateWith { text })!!
                for ((key, value) in cleaned) {
                    assertTrue("$event.$key accepted '$text'", value is String && value in AnalyticsScrubber.CODES.getValue(key))
                }
            }
        }
    }

    @Test fun `what is queued for the listed events has the expected shape`() {
        Analytics.capture(AnalyticsEvents.SESSION_END, AnalyticsScrubber.fromLocalEvent(AnalyticsEvents.SESSION_END, "4000"))
        Analytics.capture(AnalyticsEvents.TEMPLATE_CHANGED, AnalyticsScrubber.fromLocalEvent(AnalyticsEvents.TEMPLATE_CHANGED, "WORLD"))
        Analytics.capture(AnalyticsEvents.TASK_DONE)
        assertEquals(
            listOf(
                AnalyticsEvents.SESSION_END to mapOf<String, Any>("duration" to "30_to_120_min"),
                AnalyticsEvents.TEMPLATE_CHANGED to mapOf<String, Any>("template" to "world"),
                AnalyticsEvents.TASK_DONE to emptyMap<String, Any>()
            ),
            Analytics.sent.toList()
        )
    }
}
