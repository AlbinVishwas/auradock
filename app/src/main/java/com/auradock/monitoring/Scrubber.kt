package com.auradock.monitoring

import io.sentry.Breadcrumb
import io.sentry.SentryEvent
import io.sentry.protocol.SentryTransaction

/**
 * What may leave the phone. The rule is "codes, states and counts only": no message text, notification content, task or intention titles, event
 * titles, contact names or numbers, media titles, or anything the user typed. Everything here runs before an event, a breadcrumb or a trace is sent,
 * and works by keeping a short list of known-safe things rather than by trying to find the unsafe ones.
 */
object Scrubber {
    /** Our own breadcrumbs, tags and extras are named `aura.<something>`; their values are codes (see [safeValue]). */
    const val OWN_PREFIX = "aura."

    /** A code is lower-case letters, digits and `_ . : -`, at most 48 long: a state name or a number, never a sentence. */
    private val CODE = Regex("^[a-z0-9_.:-]{1,48}$")

    /** Sentry's own breadcrumbs that carry no content (the app coming and going, orientation); they keep no message and no free-form data. */
    private val SAFE_CATEGORIES = setOf("app.lifecycle", "ui.lifecycle", "device.orientation", "device.event", "navigation")
    private val SAFE_DATA_KEYS = setOf("state", "screen", "orientation", "position")

    /** The only exception type whose message is kept: our own non-fatal reports, whose message is a validated code. */
    const val NON_FATAL_TYPE = "AuraNonFatal"

    fun isCode(value: String): Boolean = CODE.matches(value)

    /** A value that may be sent: a number, a boolean or a code. Anything else (a title, a name, a sentence) becomes null and is left out. */
    fun safeValue(value: Any?): Any? = when (value) {
        is Boolean, is Int, is Long, is Float, is Double -> value
        is Enum<*> -> value.name.lowercase().takeIf { isCode(it) }
        is String -> value.takeIf { isCode(it) }
        else -> null
    }

    fun scrubBreadcrumb(b: Breadcrumb): Breadcrumb? {
        val category = b.category.orEmpty()
        if (category.startsWith(OWN_PREFIX)) {
            // Ours: a code for a message, and data restricted to safe values.
            b.message = b.message?.takeIf { isCode(it) }
            val clean = b.data.entries.mapNotNull { (k, v) -> safeValue(v)?.let { k to it } }
            b.data.clear()
            clean.forEach { (k, v) -> b.setData(k, v) }
            return b
        }
        if (category !in SAFE_CATEGORIES) return null // http, console, user interaction, system and the rest: dropped
        b.message = null
        val clean = b.data.entries.filter { it.key in SAFE_DATA_KEYS }.mapNotNull { (k, v) -> safeValue(v)?.let { k to it } }
        b.data.clear()
        clean.forEach { (k, v) -> b.setData(k, v) }
        return b
    }

    fun scrubEvent(event: SentryEvent): SentryEvent {
        event.message = null
        event.user = null
        event.request = null
        event.serverName = null
        event.logger = null
        event.exceptions?.forEach { if (it.type != NON_FATAL_TYPE || it.value?.let(::isCode) != true) it.value = null }
        // Our tags and extras are codes; anything else is dropped.
        event.tags?.let { tags ->
            val keep = tags.filter { (k, v) -> k.startsWith(OWN_PREFIX) && isCode(v) }
            tags.clear(); tags.putAll(keep)
        }
        event.setExtras(null)
        event.contexts.device?.name = null // the phone's model stays; the name the owner gave it does not
        event.breadcrumbs = event.breadcrumbs?.mapNotNull { scrubBreadcrumb(it) }
        return event
    }

    fun scrubTransaction(tx: SentryTransaction): SentryTransaction {
        tx.user = null
        tx.request = null
        tx.serverName = null
        tx.setExtras(null)
        tx.spans.removeAll { s -> s.op?.startsWith("http") == true || s.op?.startsWith("db") == true }
        tx.breadcrumbs = tx.breadcrumbs?.mapNotNull { scrubBreadcrumb(it) }
        return tx
    }
}
