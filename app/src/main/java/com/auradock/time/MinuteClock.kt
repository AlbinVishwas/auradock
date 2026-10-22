package com.auradock.time

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

/**
 * The app's single source of "what time is it", ticking once per minute.
 *
 * [minutes] emits the current time right away, then sleeps until the next minute boundary
 * (hh:mm:00.000) and emits again, forever. The coroutine is idle between ticks, so a clock that only
 * shows hours and minutes wakes the CPU 60x less than a once-per-second loop would.
 *
 * [now] is injectable so tests can drive it with virtual time.
 */
class MinuteClock(private val now: () -> Long) {

    @Inject
    constructor() : this(System::currentTimeMillis)

    fun currentMillis(): Long = now()

    val minutes: Flow<Long> = flow {
        while (true) {
            val t = now()
            emit(t)
            delay(MINUTE_MS - t % MINUTE_MS)
        }
    }

    private companion object {
        const val MINUTE_MS = 60_000L
    }
}

/** e.g. "Thursday • October 22", in the device's language and time zone. */
fun formatDashboardDate(
    millis: Long,
    zone: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.getDefault()
): String = DateTimeFormatter.ofPattern("EEEE • MMMM d", locale)
    .format(Instant.ofEpochMilli(millis).atZone(zone))
