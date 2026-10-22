package com.auradock.ui.screens.dashboard

import com.auradock.data.CalendarEvent
import com.auradock.ui.viewmodel.CalendarPermissionUiState

/** What the dashboard's "Today" card should show for a given calendar permission + event list. */
data class TodaySummary(
    /** One line per event, e.g. "10:30 AM  Team standup". Empty unless permission is granted. */
    val lines: List<String>,
    /** Shown instead of [lines] when there is nothing to list. */
    val emptyText: String,
    /** Label for a call-to-action button, or null when none is needed. */
    val actionText: String?
)

fun buildTodaySummary(
    events: List<CalendarEvent>,
    permission: CalendarPermissionUiState,
    formatTime: (Long) -> String,
    maxLines: Int = 4
): TodaySummary = when (permission) {
    CalendarPermissionUiState.DENIED -> TodaySummary(
        lines = emptyList(),
        emptyText = "Allow calendar access to see today's events",
        actionText = "Enable Calendar Access"
    )

    // Android stops showing the permission dialog after repeated denials, so the only way
    // back is the system settings screen.
    CalendarPermissionUiState.PERMANENTLY_DENIED -> TodaySummary(
        lines = emptyList(),
        emptyText = "Calendar access is turned off for AuraDock",
        actionText = "Open Settings"
    )

    CalendarPermissionUiState.GRANTED -> {
        val shown = events.take(maxLines).map { "${formatTime(it.startTime)}  ${it.title}" }
        val hidden = events.size - shown.size
        TodaySummary(
            lines = if (hidden > 0) shown + "+$hidden more" else shown,
            emptyText = "No events today",
            actionText = null
        )
    }
}
