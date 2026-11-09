package com.auradock.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.auradock.dock.DockTemplate
import com.auradock.sessions.SessionCounts
import com.auradock.sessions.SessionSummary
import com.auradock.sessions.WeeklyFocus
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

// The card after a focus session: a normal one, one with nothing done and nothing held, and a long one.
private fun summary(minutes: Long, tasks: Int, held: Int, through: Int = 0) =
    SessionSummary(endedAt = PreviewData.now, durationMs = minutes * 60_000L, tasksCompleted = tasks, counts = SessionCounts(held, through))

@PreviewPortrait
@Composable
private fun DashboardUndockSummary() = AuraDockTheme { DashboardScreen(Full.copy(undockSummary = summary(72, 3, 6, 2)), DashboardActions()) }

@PreviewPortrait
@Composable
private fun DashboardUndockSummaryQuiet() = AuraDockTheme { DashboardScreen(Full.copy(undockSummary = summary(2, 0, 0)), DashboardActions()) }

@PreviewPortrait
@Composable
private fun DashboardUndockSummaryLong() = AuraDockTheme { DashboardScreen(Full.copy(undockSummary = summary(5 * 60 + 5, 1, 48)), DashboardActions()) }

// The weekly line: a busy week, a week with one session (so one bar), and a week with none (the invitation).
private fun week(vararg minutes: Long) = WeeklyFocus(minutes.map { it * 60_000L })

@PreviewPortrait
@Composable
private fun DashboardWeeklyFocus() = AuraDockTheme { DashboardScreen(Full.copy(weeklyFocus = week(0, 45, 90, 20, 0, 60, 45)), DashboardActions()) }

@PreviewPortrait
@Composable
private fun DashboardWeeklyFocusOneDay() = AuraDockTheme { DashboardScreen(Full.copy(weeklyFocus = week(0, 0, 0, 0, 0, 0, 25)), DashboardActions()) }

@PreviewPortrait
@Composable
private fun DashboardWeeklyFocusEmpty() = AuraDockTheme { DashboardScreen(Full.copy(weeklyFocus = week(0, 0, 0, 0, 0, 0, 0)), DashboardActions()) }

// The evening shutdown: the undock card's second step.
@PreviewPortrait
@Composable
private fun UndockTomorrowStep() = AuraDockTheme {
    androidx.compose.foundation.layout.Box(Modifier.fillMaxSize().background(com.auradock.ui.theme.AuraDockColors.Background).padding(horizontal = com.auradock.ui.theme.AuraDockSpacing.mdl), contentAlignment = androidx.compose.ui.Alignment.BottomCenter) {
        UndockSummaryCard(summary(72, 3, 6, 2), onDismiss = {}, offerShutdown = true, startOnTomorrow = true)
    }
}

// The intention at the top of the Today card, open and done.
@PreviewPortrait @Composable private fun DashboardIntention() = AuraDockTheme { DashboardScreen(Full.copy(intention = com.auradock.intention.IntentionLine("Ship the invoice to Meera", false)), DashboardActions()) }
@PreviewPortrait @Composable private fun DashboardIntentionDone() = AuraDockTheme { DashboardScreen(Full.copy(intention = com.auradock.intention.IntentionLine("Ship the invoice to Meera", true)), DashboardActions()) }

/** The screensaver appeared on its own (shown once, until "Done"). */
@PreviewPortrait
@Composable
private fun DashboardScreensaverAppeared() = AuraDockTheme {
    DashboardScreen(Full.copy(screensaverBanner = com.auradock.screensaver.ScreensaverBanner.APPEARED), DashboardActions())
}

/** A day after install and no screensaver yet: a gentle hint with "Try again". */
@PreviewPortrait
@Composable
private fun DashboardScreensaverHint() = AuraDockTheme {
    DashboardScreen(Full.copy(screensaverBanner = com.auradock.screensaver.ScreensaverBanner.HINT), DashboardActions())
}
