package com.auradock.ui.screens.dashboard

import com.auradock.ui.viewmodel.CalendarPermissionUiState

/**
 * The "empty day" the handoff's Dashboard-Empty frame draws: nothing planned, no intention, the calendar allowed and with no events.
 * In that state the dashboard is the frame's simpler layout (a 36 sp greeting, a dock card with only the preview and Dock now, and
 * one-line Templates and Focus tiles). Anything else (a task, an event, a missing permission, still loading) is the fuller layout of
 * the Main frame.
 */
fun isEmptyDay(
    tasksEmpty: Boolean?,
    hasIntention: Boolean,
    calendar: CalendarPermissionUiState,
    eventsEmpty: Boolean?
): Boolean = tasksEmpty == true && !hasIntention && calendar == CalendarPermissionUiState.GRANTED && eventsEmpty == true

/** The Focus tile's line: whether Focus is on while docked. */
fun focusTileText(focusOn: Boolean): String = if (focusOn) "On while docked" else "Off"
