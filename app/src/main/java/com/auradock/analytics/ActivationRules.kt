package com.auradock.analytics

import java.time.Instant
import java.time.ZoneId

/** An event as the rules see it. */
data class AppEvent(val name: String, val at: Long, val detail: String? = null)

const val ACTIVATION_WINDOW_MS = 24L * 60 * 60 * 1000
const val DOCKED_DAYS_WINDOW = 7
const val DOCKED_DAYS_NEEDED = 5

/** The two numbers of the hidden debug screen, plus what they were worked out from. */
data class ActivationSummary(
    /** The screensaver was set and the first dock happened, both within 24 hours of install. */
    val activatedWithin24h: Boolean,
    val screensaverSetAt: Long?,
    val firstDockAt: Long?,
    /** On how many of the last seven days (counting today) there was a dock. */
    val dockedDays: Int,
    /** Docked on five or more of the last seven days. */
    val dockedFiveOfSeven: Boolean,
    /** How often each event happened, most often first. */
    val counts: List<Pair<String, Int>>,
    val total: Int
)

fun activationSummary(events: List<AppEvent>, installedAt: Long, now: Long, zone: ZoneId = ZoneId.systemDefault()): ActivationSummary {
    val screensaverAt = events.filter { it.name == AnalyticsEvents.SCREENSAVER_SET }.minOfOrNull { it.at }
    val firstDockAt = events.filter { it.name == AnalyticsEvents.FIRST_DOCK }.minOfOrNull { it.at }
    val limit = installedAt + ACTIVATION_WINDOW_MS
    val activated = screensaverAt != null && firstDockAt != null && screensaverAt <= limit && firstDockAt <= limit
    val days = dockedDays(events, now, zone)
    return ActivationSummary(
        activatedWithin24h = activated,
        screensaverSetAt = screensaverAt,
        firstDockAt = firstDockAt,
        dockedDays = days,
        dockedFiveOfSeven = days >= DOCKED_DAYS_NEEDED,
        counts = events.groupingBy { it.name }.eachCount().toList().sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy { it.first }),
        total = events.size
    )
}

/** How many of the last seven days (today and the six before it) have at least one session start. */
fun dockedDays(events: List<AppEvent>, now: Long, zone: ZoneId = ZoneId.systemDefault()): Int {
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val first = today.minusDays((DOCKED_DAYS_WINDOW - 1).toLong())
    return events.filter { it.name == AnalyticsEvents.SESSION_START }
        .map { Instant.ofEpochMilli(it.at).atZone(zone).toLocalDate() }
        .filter { !it.isBefore(first) && !it.isAfter(today) }
        .distinct()
        .size
}
