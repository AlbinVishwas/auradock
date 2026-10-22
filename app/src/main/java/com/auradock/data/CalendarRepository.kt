package com.auradock.data

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import android.util.Log
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

data class CalendarEvent(
    val title: String,
    /** Start time in epoch millis. For all-day events this is a UTC midnight and must not be shown as a time. */
    val startTime: Long,
    val allDay: Boolean = false
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

    fun observeTodayEvents(): Flow<List<CalendarEvent>> = flow {
        while (true) {
            emit(fetchTodayEvents())
            delay(REFRESH_INTERVAL_MS)
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
                CalendarContract.Instances.ALL_DAY
            )

            val rows = mutableListOf<InstanceRow>()
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                val titleColumn = cursor.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
                val beginColumn = cursor.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
                val endColumn = cursor.getColumnIndexOrThrow(CalendarContract.Instances.END)
                val allDayColumn = cursor.getColumnIndexOrThrow(CalendarContract.Instances.ALL_DAY)
                while (cursor.moveToNext()) {
                    rows += InstanceRow(
                        title = cursor.getString(titleColumn),
                        begin = cursor.getLong(beginColumn),
                        end = cursor.getLong(endColumn),
                        allDay = cursor.getInt(allDayColumn) == 1
                    )
                }
            }
            rowsToEvents(rows, today = LocalDate.now())
        }.onFailure {
            Log.e(TAG, "Failed to fetch calendar events", it)
        }.getOrDefault(emptyList())
    }
}
