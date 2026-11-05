package com.auradock.ui.preview

import androidx.compose.ui.tooling.preview.Preview
import com.auradock.data.CalendarEvent
import com.auradock.data.NotificationItem
import com.auradock.dock.DockUiState
import com.auradock.focus.AppEntry
import com.auradock.tasks.TaskEntity
import com.auradock.weather.Weather
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Made-up data for @Preview, so every screen can be seen in its full, empty and permission-missing states without a phone.
 * Nothing here is real: no real names, no real messages.
 */
object PreviewData {
    /** A fixed morning, so previews do not change from one render to the next. */
    val now: Long = at(9, 41)

    fun at(hour: Int, minute: Int): Long =
        LocalDateTime.of(2026, 11, 6, hour, minute).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    val weather = Weather(temperatureC = 19.0, weatherCode = 2, city = "Mumbai", fetchedAt = now)

    val events = listOf(
        CalendarEvent(title = "Team standup", startTime = at(10, 30)),
        CalendarEvent(title = "Design review", startTime = at(13, 0)),
        CalendarEvent(title = "Call with the client", startTime = at(16, 15))
    )

    val tasks = listOf(
        TaskEntity(id = 1, title = "Send the invoice", dueAt = at(9, 0), createdAt = at(8, 0)),
        TaskEntity(id = 2, title = "Reply to Sam", dueAt = at(11, 30), createdAt = at(8, 0)),
        TaskEntity(id = 3, title = "Book the flights", createdAt = at(8, 0)),
        TaskEntity(id = 4, title = "Water the plants", done = true, createdAt = at(8, 0), completedAt = at(9, 10))
    )

    val notifications = listOf(
        NotificationItem("1", "pkg.chat", "Messages", "Alex", "Running ten minutes late", at(9, 38))
    )

    val apps = listOf(
        AppEntry("pkg.calendar", "Calendar"),
        AppEntry("pkg.chat", "Messages"),
        AppEntry("pkg.mail", "Mail"),
        AppEntry("pkg.notes", "Notes")
    )

    /** The dock's data in its full state: weather, events, tasks and a notification. */
    val dockFull = DockUiState(now, weather, events, tasks, notifications)

    /** The dock with nothing to show: just the clock. */
    val dockEmpty = DockUiState(now)
}

/** The preview canvas colour (the app's pure black), as the preview tool needs it: a compile-time number. */
const val PREVIEW_BLACK = 0xFF000000

/** A phone held upright, on black. */
@Preview(name = "Portrait", widthDp = 360, heightDp = 780, showBackground = true, backgroundColor = PREVIEW_BLACK)
annotation class PreviewPortrait

/** A phone on its side (the dock, and anything shown while docked), on black. */
@Preview(name = "Landscape", widthDp = 800, heightDp = 360, showBackground = true, backgroundColor = PREVIEW_BLACK)
annotation class PreviewLandscape

/** A small card or control, on black. */
@Preview(name = "Component", widthDp = 360, showBackground = true, backgroundColor = PREVIEW_BLACK)
annotation class PreviewComponent
