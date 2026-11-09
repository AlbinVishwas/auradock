package com.auradock.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

/** One row read from CalendarContract.Instances, before it becomes a [CalendarEvent]. */
data class InstanceRow(
    val title: String?,
    val begin: Long,
    val end: Long,
    val allDay: Boolean,
    /** The user declined it, or it was cancelled. */
    val declined: Boolean = false,
    val location: String? = null
)

/**
 * Start (inclusive) and end (exclusive) of the local calendar day containing [nowMillis].
 * Built from the date rather than "now + 24h", so days that are 23 or 25 hours long (daylight
 * saving changes) are still covered exactly.
 */
fun todayRange(nowMillis: Long, zone: ZoneId = ZoneId.systemDefault()): Pair<Long, Long> {
    val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
    val start = today.atStartOfDay(zone).toInstant().toEpochMilli()
    val end = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
    return start to end
}

/**
 * All-day events are stored as whole *UTC* days (UTC midnight to UTC midnight), not in the user's
 * time zone. The calendar provider matches them to a query by instant, so a UTC+5:30 user's "today"
 * (which starts at 18:30 UTC the evening before) also overlaps yesterday's all-day event, and a
 * UTC-5 user's today overlaps tomorrow's. So for all-day rows we ignore the instants and compare the
 * *dates* instead: the event covers [begin date, end date) read in UTC, and we check that [today]
 * falls inside that.
 */
private fun InstanceRow.coversDate(today: LocalDate): Boolean {
    if (!allDay) return true
    val first = Instant.ofEpochMilli(begin).atZone(ZoneOffset.UTC).toLocalDate()
    val lastExclusive = Instant.ofEpochMilli(end).atZone(ZoneOffset.UTC).toLocalDate()
    return !today.isBefore(first) && today.isBefore(lastExclusive)
}

/** All-day events first (they have no time of day), then timed events by start time. */
fun rowsToEvents(rows: List<InstanceRow>, today: LocalDate): List<CalendarEvent> =
    rows.filter { it.coversDate(today) }
        .sortedWith(compareByDescending<InstanceRow> { it.allDay }.thenBy { it.begin })
        .map {
            CalendarEvent(
                title = it.title?.takeIf { t -> t.isNotBlank() } ?: "(No title)",
                startTime = it.begin,
                allDay = it.allDay,
                declined = it.declined,
                location = it.location?.takeIf { l -> l.isNotBlank() }
            )
        }
