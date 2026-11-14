package com.auradock.analytics

import java.util.concurrent.atomic.AtomicInteger

/**
 * How many events the app has handed to PostHog since the last flush, for the "flush sent" log line.
 *
 * The number is only a count: it does not include the SDK's own lifecycle events (installed, opened, backgrounded, updated), and it shows that a
 * flush was requested, not that the server received the events. Safe to call from any thread.
 */
internal class SentCounter {
    private val count = AtomicInteger(0)

    /** One event was handed to PostHog. */
    fun recordSent() {
        count.incrementAndGet()
    }

    /** The count since the last take, and back to 0. */
    fun takeSentCount(): Int = count.getAndSet(0)

    companion object {
        /** The log line for a flush: the number only, no event names, properties, ids, key or host. */
        fun flushMessage(events: Int): String = "flush sent, events=$events"
    }
}
