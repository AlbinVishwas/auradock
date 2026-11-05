package com.auradock.ui.screens.dashboard

import androidx.compose.runtime.Composable
import com.auradock.dock.DockTemplate
import com.auradock.ui.preview.PreviewData
import com.auradock.ui.preview.PreviewPortrait
import com.auradock.ui.theme.AuraDockTheme
import com.auradock.ui.viewmodel.CalendarPermissionUiState

private val Full = DashboardUiState(
    template = DockTemplate.PRODUCTIVITY,
    timeMillis = PreviewData.now,
    weather = PreviewData.weather,
    events = PreviewData.events,
    tasks = PreviewData.tasks,
    calendarPermission = CalendarPermissionUiState.GRANTED
)

@PreviewPortrait
@Composable
private fun DashboardFull() = AuraDockTheme { DashboardScreen(Full, DashboardActions()) }

/** Nothing planned: no tasks, no events, no weather yet. */
@PreviewPortrait
@Composable
private fun DashboardEmpty() = AuraDockTheme {
    DashboardScreen(
        Full.copy(weather = null, events = emptyList(), tasks = emptyList(), calendarPermission = CalendarPermissionUiState.GRANTED),
        DashboardActions()
    )
}

/** First moments: calendar and tasks are still loading (null), so the cards show no body. */
@PreviewPortrait
@Composable
private fun DashboardLoading() = AuraDockTheme {
    DashboardScreen(Full.copy(weather = null, events = null, tasks = null), DashboardActions())
}

/** Calendar and location both denied: the cards offer to ask. */
@PreviewPortrait
@Composable
private fun DashboardPermissionsMissing() = AuraDockTheme {
    DashboardScreen(
        Full.copy(
            weather = null,
            events = emptyList(),
            locationPermission = CalendarPermissionUiState.DENIED,
            calendarPermission = CalendarPermissionUiState.DENIED,
            exactAlarmsAllowed = false
        ),
        DashboardActions()
    )
}

/** Both permanently denied: the prompts point to system Settings instead. */
@PreviewPortrait
@Composable
private fun DashboardPermissionsBlocked() = AuraDockTheme {
    DashboardScreen(
        Full.copy(
            weather = null,
            events = emptyList(),
            locationPermission = CalendarPermissionUiState.PERMANENTLY_DENIED,
            calendarPermission = CalendarPermissionUiState.PERMANENTLY_DENIED
        ),
        DashboardActions()
    )
}
