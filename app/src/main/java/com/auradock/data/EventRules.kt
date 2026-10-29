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
