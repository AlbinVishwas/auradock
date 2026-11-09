package com.auradock.data

/**
 * Events that have not started yet, in time order. All-day events are left out: they have no time of day, so they are
 * never "next". An event that has already started is not upcoming (we do not know when it ends).
 */
fun upcomingEvents(events: List<CalendarEvent>, nowMillis: Long): List<CalendarEvent> =
    events.filter { !it.allDay && it.startTime >= nowMillis }.sortedBy { it.startTime }

/** The next event today, or null if none is left. */
fun nextEvent(events: List<CalendarEvent>, nowMillis: Long): CalendarEvent? =
    upcomingEvents(events, nowMillis).firstOrNull()

/** How long before a meeting the dock starts counting down. */
const val MEETING_COUNTDOWN_MS = 10 * 60_000L

/** From this many minutes out the countdown breathes (a gentle opacity pulse). */
const val MEETING_PULSE_MINUTES = 2

/** The meeting the dock counts down to: its title and the whole minutes left (rounded up, so "in 1 min" lasts until it starts). */
data class MeetingCountdown(val title: String, val minutes: Int) {
    val pulsing: Boolean get() = minutes <= MEETING_PULSE_MINUTES
}

/**
 * The timed, not-declined event that starts within the next 10 minutes, or null. All-day and declined events are skipped.
 * It is gone the moment the start time arrives.
 */
fun meetingCountdown(events: List<CalendarEvent>, nowMillis: Long): MeetingCountdown? {
    val next = events
        .filter { !it.allDay && !it.declined && it.startTime > nowMillis }
        .minByOrNull { it.startTime } ?: return null
    val left = next.startTime - nowMillis
    if (left > MEETING_COUNTDOWN_MS) return null
    return MeetingCountdown(next.title, ((left + 59_999) / 60_000).toInt())
}
