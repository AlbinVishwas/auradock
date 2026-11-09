package com.auradock.dock

import com.auradock.data.CalendarEvent
import com.auradock.dock.lines.DockLines
import com.auradock.data.NotificationItem
import com.auradock.tasks.TaskEntity
import com.auradock.ui.theme.BoldPalette
import com.auradock.weather.Weather

/**
 * Everything [DockContent] needs to draw a template. The live dock builds it from [DockData]; the dashboard thumbnail
 * and the gallery build it from the same data, so they all show the same thing.
 */
data class DockUiState(
    val timeMillis: Long,
    /** Null means "no weather": shown as a quiet dash, never as made-up numbers. */
    val weather: Weather? = null,
    /** Today's calendar events (empty if there are none or calendar access is off). */
    val events: List<CalendarEvent> = emptyList(),
    /** Today's tasks, as the task list holds them (finished ones included). */
    val tasks: List<TaskEntity> = emptyList(),
    /** The latest notification from each picked app (empty: no strip). Left empty in previews, which never show real messages. */
    val notifications: List<NotificationItem> = emptyList(),
    /** Whether the strip may show the message text itself. Off unless the user turns it on. */
    val showMessageText: Boolean = false,
    /** The focus session going on now (the live dock only; null for thumbnails, which draw no line and no counts). */
    val session: SessionSnapshot? = null,
    /** The quiet lines under the clock (morning intention, and the lines later items add). Empty in thumbnails. */
    val lines: DockLines = DockLines(),
    /** Set while the morning question is showing: the text to start the field with ("" when there is none). */
    val intentionPrompt: String? = null,
    /** The Bold clock's colourway. */
    val boldPalette: BoldPalette = BoldPalette.default
)

/** What the dock draws about the session: when it began, and how many notifications were held and let through. */
data class SessionSnapshot(val startedAt: Long, val held: Int = 0, val through: Int = 0)
