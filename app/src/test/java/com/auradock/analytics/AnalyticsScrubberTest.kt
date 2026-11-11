package com.auradock.analytics

import com.auradock.dock.DockTemplate
import com.auradock.dock.split.WidgetKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** What may leave the phone to PostHog: enums, counts and duration buckets, and never anything the user wrote. */
class AnalyticsScrubberTest {
    // ── What must never get through ──────────────────────────────────────────────────────────────────────────────────

    private val freeText = listOf(
        "Call the dentist about the crown", // a task title
        "Draft the Q1 plan",               // an intention
        "Mum: are you coming for dinner?", // notification text
        "Priya Sharma",                    // a contact name
        "+91 98765 43210",                 // a number
        "Dentist 3 pm",                    // an event title
        "Blinding Lights - The Weeknd",    // a media title
        "dentist",                         // one lower-case word is still free text
        "bold; drop table"                 // nothing that looks like code is trusted either
    )

    @Test fun `free text is rejected for every property of every event`() {
        for ((event, keys) in AnalyticsScrubber.EVENT_PROPERTIES) {
            for (key in keys) for (text in freeText) {
                val cleaned = AnalyticsScrubber.clean(event, mapOf(key to text))!!
                assertFalse("$event.$key let '$text' through", cleaned.containsKey(key))
            }
        }
    }

    @Test fun `a title, notification text or contact name is rejected under any property name`() {
        val stuffed = mapOf("title" to "Call the dentist", "text" to "Mum: dinner?", "contact" to "Priya Sharma", "body" to "x", "detail" to "y", "name" to "z")
        for (event in AnalyticsScrubber.EVENT_PROPERTIES.keys) {
            assertEquals("$event must send none of them", emptyMap<String, Any>(), AnalyticsScrubber.clean(event, stuffed))
        }
    }

    @Test fun `a free-text value is not accepted even when it is typed as a code key`() {
        val cleaned = AnalyticsScrubber.clean(AnalyticsEvents.TASK_ADDED, mapOf("title" to "Pay rent", "source" to "Pay rent", "count" to "Pay rent"))
        assertEquals(emptyMap<String, Any>(), cleaned)
    }

    @Test fun `an unknown event is dropped whole`() {
        assertNull(AnalyticsScrubber.clean("task_title_typed", mapOf("count" to 1)))
        assertNull(AnalyticsScrubber.clean("Call the dentist", emptyMap()))
        assertFalse(AnalyticsScrubber.isAllowedEvent("notification_received"))
    }

    @Test fun `only values that are exactly a listed code pass`() {
        assertFalse(AnalyticsScrubber.isAllowedValue("source", "DOCK_NOW")) // codes are lower case
        assertFalse(AnalyticsScrubber.isAllowedValue("source", "dock_now "))
        assertFalse(AnalyticsScrubber.isAllowedValue("step", "welcome to the app"))
        assertFalse(AnalyticsScrubber.isAllowedValue("widget", "tasks; drop"))
        assertFalse(AnalyticsScrubber.isAllowedValue("count", "5"))
        assertFalse(AnalyticsScrubber.isAllowedValue("count", -1))
        assertFalse(AnalyticsScrubber.isAllowedValue("count", AnalyticsScrubber.MAX_COUNT + 1))
        assertFalse(AnalyticsScrubber.isAllowedValue("nothing_listed", "dock_now"))
    }

    @Test fun `the local log's detail text is never passed through`() {
        // Whatever ends up in the detail column, it only becomes a property if it maps to a listed code or bucket.
        for (text in freeText) {
            for (event in AnalyticsScrubber.EVENT_PROPERTIES.keys) {
                val cleaned = AnalyticsScrubber.clean(event, AnalyticsScrubber.fromLocalEvent(event, text))!!
                assertTrue("$event leaked '$text' as $cleaned", cleaned.values.none { it.toString().contains(text) })
            }
        }
    }

    // ── What must get through ────────────────────────────────────────────────────────────────────────────────────────

    @Test fun `enums, counts and duration buckets pass`() {
        assertEquals(mapOf("step" to "screensaver"), AnalyticsScrubber.clean(AnalyticsEvents.ONBOARDING_STEP, mapOf("step" to "screensaver")))
        assertEquals(mapOf("source" to "watcher"), AnalyticsScrubber.clean(AnalyticsEvents.FIRST_DOCK, mapOf("source" to "watcher")))
        assertEquals(mapOf("template" to "split"), AnalyticsScrubber.clean(AnalyticsEvents.TEMPLATE_CHANGED, mapOf("template" to "split")))
        assertEquals(
            mapOf("widget" to "now_playing", "side" to "left"),
            AnalyticsScrubber.clean(AnalyticsEvents.WIDGET_ADDED, mapOf("widget" to "now_playing", "side" to "left"))
        )
        assertEquals(mapOf("duration" to "30_to_120_min"), AnalyticsScrubber.clean(AnalyticsEvents.SESSION_END, mapOf("duration" to "30_to_120_min")))
        assertTrue(AnalyticsScrubber.isAllowedValue("count", 7))
        assertTrue(AnalyticsScrubber.isAllowedValue("count", 0L))
    }

    @Test fun `events with no properties pass with none`() {
        assertEquals(emptyMap<String, Any>(), AnalyticsScrubber.clean(AnalyticsEvents.TASK_ADDED, emptyMap()))
        assertEquals(emptyMap<String, Any>(), AnalyticsScrubber.clean(AnalyticsEvents.SHUTDOWN_USED, emptyMap()))
    }

    @Test fun `durations are bucketed`() {
        fun code(seconds: Long) = AnalyticsScrubber.DurationBucket.of(seconds).code
        assertEquals("under_5_min", code(0))
        assertEquals("under_5_min", code(299))
        assertEquals("5_to_30_min", code(300))
        assertEquals("5_to_30_min", code(1_799))
        assertEquals("30_to_120_min", code(1_800))
        assertEquals("30_to_120_min", code(7_200))
        assertEquals("over_120_min", code(7_201))
    }

    @Test fun `the local log maps to the same names with codes and buckets`() {
        assertEquals(mapOf("source" to "dock_now"), AnalyticsScrubber.fromLocalEvent(AnalyticsEvents.SESSION_START, "DOCK_NOW"))
        assertEquals(mapOf("duration" to "5_to_30_min"), AnalyticsScrubber.fromLocalEvent(AnalyticsEvents.SESSION_END, "600"))
        assertEquals(mapOf("widget" to "dial", "side" to "right"), AnalyticsScrubber.fromLocalEvent(AnalyticsEvents.WIDGET_ADDED, "DIAL:right"))
        assertEquals(mapOf("permission" to "dnd_access"), AnalyticsScrubber.fromLocalEvent(AnalyticsEvents.PERMISSION_GRANTED, "DND_ACCESS"))
    }

    // ── The code lists match the app's enums ────────────────────────────────────────────────────────────────────────

    @Test fun `the template and widget codes are exactly the app's enums`() {
        assertEquals(DockTemplate.entries.map { it.name.lowercase() }.toSet(), AnalyticsScrubber.CODES.getValue("template"))
        assertEquals(WidgetKind.entries.map { it.name.lowercase() }.toSet(), AnalyticsScrubber.CODES.getValue("widget"))
    }

    @Test fun `every event the app logs is allowed`() {
        val names = AnalyticsEvents::class.java.declaredFields.filter { it.type == String::class.java }.map { it.get(null) as String }
        assertTrue(names.isNotEmpty())
        names.forEach { assertTrue("$it is not in the allow-list", AnalyticsScrubber.isAllowedEvent(it)) }
    }
}
