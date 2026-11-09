package com.auradock.intention

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** An evening shutdown is offered for a session that ends from 6 pm on (and through the small hours, which are still that evening: the day starts at 4 am). */
const val SHUTDOWN_FROM_HOUR = 18

/** True if [millis] falls in the evening: 6 pm up to 4 am. */
fun isEvening(millis: Long, zone: ZoneId = ZoneId.systemDefault()): Boolean {
    val hour = Instant.ofEpochMilli(millis).atZone(zone).hour
    return hour >= SHUTDOWN_FROM_HOUR || hour < INTENTION_DAY_STARTS_HOUR
}

/**
 * Whether the undock card gets its "Tomorrow's top task?" step: the session ended in the evening and this evening has not been
 * asked yet ([lastOfferedDay] is the [intentionDay] it was last asked on, or null).
 */
fun shouldOfferShutdown(sessionEndedAt: Long, lastOfferedDay: String?, zone: ZoneId = ZoneId.systemDefault()): Boolean =
    isEvening(sessionEndedAt, zone) && lastOfferedDay != intentionDay(sessionEndedAt, zone)

/** When tomorrow's task is due: [hour]:[minute] on the day after [intentionDay], or null for "no time". */
fun tomorrowDueAt(nowMillis: Long, hour: Int, minute: Int, zone: ZoneId = ZoneId.systemDefault()): Long =
    LocalDate.parse(nextIntentionDay(nowMillis, zone)).atTime(LocalTime.of(hour, minute)).atZone(zone).toInstant().toEpochMilli()

/** Midnight at the start of tomorrow's day: before it, tomorrow's task stays out of today's list. */
fun tomorrowStart(nowMillis: Long, zone: ZoneId = ZoneId.systemDefault()): Long =
    LocalDate.parse(nextIntentionDay(nowMillis, zone)).atStartOfDay(zone).toInstant().toEpochMilli()
