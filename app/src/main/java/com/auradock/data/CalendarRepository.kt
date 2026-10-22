package com.auradock.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import android.util.Log
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.util.Calendar

data class CalendarEvent(
    val title: String,
    val startTime: Long
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

    suspend fun fetchTodayEvents(): List<CalendarEvent> {
        if (!hasCalendarPermission()) return emptyList()

        return runCatching {
            val now = Calendar.getInstance()
            val startOfDay = (now.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val endOfDay = (now.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis

            val projection = arrayOf(
                CalendarContract.Events.TITLE,
                CalendarContract.Events.DTSTART
            )

            val selection = "${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} <= ?"
            val selectionArgs = arrayOf(startOfDay.toString(), endOfDay.toString())
            val sortOrder = "${CalendarContract.Events.DTSTART} ASC"

            val items = mutableListOf<CalendarEvent>()
            context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val titleColumn = cursor.getColumnIndex(CalendarContract.Events.TITLE)
                val startColumn = cursor.getColumnIndex(CalendarContract.Events.DTSTART)

                while (cursor.moveToNext()) {
                    val rawTitle = if (titleColumn >= 0) cursor.getString(titleColumn) else null
                    val title = rawTitle?.takeIf { it.isNotBlank() } ?: "(No title)"
                    val startTime = if (startColumn >= 0) cursor.getLong(startColumn) else 0L
                    items += CalendarEvent(title = title, startTime = startTime)
                }
            }
            items
        }.onFailure {
            Log.e(TAG, "Failed to fetch calendar events", it)
        }.getOrDefault(emptyList())
    }
}
