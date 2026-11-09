package com.auradock.sessions

import com.auradock.time.MinuteClock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/** Focus time over the last seven days, today last: one entry per day, in milliseconds. */
data class WeeklyFocus(val perDayMillis: List<Long>) {
    val totalMillis: Long get() = perDayMillis.sum()

    /** No focus time at all in the seven days: the dashboard then invites the user to start one. */
    val isEmpty: Boolean get() = totalMillis < 60_000L
}

/** The start of the day six days before [now]: sessions that ended before it are not part of the week. */
fun weekWindowStart(now: Long, zone: ZoneId): Long =
    Instant.ofEpochMilli(now).atZone(zone).toLocalDate().minusDays(6).atStartOfDay(zone).toInstant().toEpochMilli()

/**
 * Adds up [sessions] into the seven days ending today. A session that runs past midnight is split between the two days, so
 * each day's bar is what really happened on it. Anything outside the window is left out.
 */
fun weeklyFocus(sessions: List<FocusSessionEntity>, now: Long, zone: ZoneId): WeeklyFocus {
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
    val perDay = days.map { day ->
        val from = day.atStartOfDay(zone).toInstant().toEpochMilli()
        val to = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        sessions.sumOf { (minOf(it.endedAt, to) - maxOf(it.startedAt, from)).coerceAtLeast(0L) }
    }
    return WeeklyFocus(perDay)
}

/** The weekly figures as a flow: they update when a session is saved and when the day changes. */
@Singleton
class FocusStats @Inject constructor(
    private val sessions: FocusSessionDao,
    private val clock: MinuteClock
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    val weekly: Flow<WeeklyFocus> = clock.minutes
        .map { weekWindowStart(it, ZoneId.systemDefault()) }
        .distinctUntilChanged()
        .flatMapLatest { start ->
            sessions.observeSince(start).combine(clock.minutes) { list, now -> weeklyFocus(list, now, ZoneId.systemDefault()) }
        }
}
