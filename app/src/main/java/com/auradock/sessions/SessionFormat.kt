package com.auradock.sessions

/** "1 h 12 m", "45 m", "3 h": a duration as the summary card and the weekly line write it. Rounded down to whole minutes. */
fun formatFocusTime(millis: Long): String {
    val minutes = (millis / 60_000L).coerceAtLeast(0)
    val hours = minutes / 60
    val rest = minutes % 60
    return when {
        hours == 0L -> "$rest m"
        rest == 0L -> "$hours h"
        else -> "$hours h $rest m"
    }
}

/** "task done" or "tasks done": the label under the number on the summary card. */
fun tasksDoneLabel(count: Int): String = if (count == 1) "task done" else "tasks done"

/**
 * How long after a session the summary card may still appear. A session that ends while the app is not showing (the
 * screensaver) leaves its card waiting; if the app is opened much later the card would be stale, so it is dropped.
 */
const val SUMMARY_MAX_AGE_MS = 10 * 60_000L

/** The card is only for a session that ended recently. [now] ticks once a minute, so it can be a little before [endedAt]. */
fun summaryIsFresh(endedAt: Long, now: Long): Boolean = now - endedAt <= SUMMARY_MAX_AGE_MS
