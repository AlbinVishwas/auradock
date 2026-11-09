package com.auradock.data

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import android.util.Log
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn

data class CalendarEvent(
    val title: String,
    /** Start time in epoch millis. For all-day events this is a UTC midnight and must not be shown as a time. */
    val startTime: Long,
    val allDay: Boolean = false,
    /** The user declined it or it was cancelled: still listed, but never counted down to. */
    val declined: Boolean = false
)

@Singleton
class CalendarRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "CalendarRepository"
        private const val REFRESH_INTERVAL_MS = 60_000L
    }

    fun hasCalendarPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Today's events, re-read when the calendar provider reports a change and otherwise once a minute
     * (the minute tick moves the day on and drops events that have ended).
     *
     * Watching the provider needs the calendar permission, and registering without it throws, so the
     * observer is only registered once the permission is there (the minute tick notices a later grant).
     */
    fun observeTodayEvents(): Flow<List<CalendarEvent>> = channelFlow {
        val changes = Channel<Unit>(Channel.CONFLATED)
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                changes.trySend(Unit)
            }
        }
        var watching = false
        fun watchIfAllowed() {
            if (watching || !hasCalendarPermission()) return
            watching = runCatching {
                context.contentResolver.registerContentObserver(CalendarContract.CONTENT_URI, true, observer)
            }.onFailure { Log.w(TAG, "Could not watch the calendar", it) }.isSuccess
        }
        watchIfAllowed()
        launch {
            while (true) {
                delay(REFRESH_INTERVAL_MS)
                watchIfAllowed()
                changes.trySend(Unit)
            }
        }
        send(fetchTodayEvents())
        try {
            for (change in changes) send(fetchTodayEvents())
        } finally {
            if (watching) context.contentResolver.unregisterContentObserver(observer)
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Reads today's events from CalendarContract.Instances rather than Events.
     *
     * Events holds one row per *series*: a daily standup that began last month is a single row
     * dated last month. Instances holds one row per *occurrence*, with recurrences already expanded,
     * which is what "what is on today" needs. It also returns events that began earlier and are
     * still running, and all-day events that cover today.
     */
    suspend fun fetchTodayEvents(): List<CalendarEvent> {
        if (!hasCalendarPermission()) return emptyList()

        return runCatching {
            val (start, endExclusive) = todayRange(System.currentTimeMillis())
            val uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
                .also {
                    ContentUris.appendId(it, start)
                    ContentUris.appendId(it, endExclusive - 1)
                }
                .build()

            val projection = arrayOf(
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END,
                CalendarContract.Instances.ALL_DAY,
                CalendarContract.Instances.SELF_ATTENDEE_STATUS,
                CalendarContract.Instances.STATUS
            )

            val rows = mutableListOf<InstanceRow>()
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                val titleColumn = cursor.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
                val beginColumn = cursor.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
                val endColumn = cursor.getColumnIndexOrThrow(CalendarContract.Instances.END)
                val allDayColumn = cursor.getColumnIndexOrThrow(CalendarContract.Instances.ALL_DAY)
                val selfStatusColumn = cursor.getColumnIndexOrThrow(CalendarContract.Instances.SELF_ATTENDEE_STATUS)
                val statusColumn = cursor.getColumnIndexOrThrow(CalendarContract.Instances.STATUS)
                while (cursor.moveToNext()) {
                    rows += InstanceRow(
                        title = cursor.getString(titleColumn),
                        begin = cursor.getLong(beginColumn),
                        end = cursor.getLong(endColumn),
                        allDay = cursor.getInt(allDayColumn) == 1,
                        declined = cursor.getInt(selfStatusColumn) == CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED ||
                            cursor.getInt(statusColumn) == CalendarContract.Events.STATUS_CANCELED
                    )
                }
            }
            rowsToEvents(rows, today = LocalDate.now())
        }.onFailure {
            Log.e(TAG, "Failed to fetch calendar events", it)
        }.getOrDefault(emptyList())
    }
}
