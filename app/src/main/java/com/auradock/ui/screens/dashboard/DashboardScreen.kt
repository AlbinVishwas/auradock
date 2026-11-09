package com.auradock.ui.screens.dashboard

import androidx.compose.foundation.clickable
import com.auradock.ui.theme.AuraDockColors
import com.auradock.ui.theme.AuraDockTypography
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalContext
import java.util.Date
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.auradock.data.CalendarEvent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import com.auradock.ui.components.BlurIn
import com.auradock.ui.components.LocalDockMorph
import com.auradock.ui.components.reportBounds
import com.auradock.ui.components.GreyTextButton
import com.auradock.ui.components.PrimaryPill
import com.auradock.ui.components.QuietPill
import com.auradock.ui.theme.AuraDockRadii
import com.auradock.time.formatDashboardDate
import com.auradock.time.greetingFor
import com.auradock.ui.components.ScaledPreview
import com.auradock.ui.components.TodaySnapshotCard
import com.auradock.dock.DockTemplate
import com.auradock.dock.DockContent
import com.auradock.dock.DockUiState
import com.auradock.weather.Weather
import com.auradock.weather.conditionLine
import com.auradock.weather.temperatureLabel
import com.auradock.tasks.TaskEntity
import com.auradock.ui.tasks.TaskCard
import com.auradock.ui.tasks.TaskSheet
import com.auradock.ui.theme.AuraDockGallery
import com.auradock.ui.theme.AuraDockSizes
import com.auradock.ui.theme.AuraDockTheme
import com.auradock.ui.theme.LocalAuraDockSpacing
import com.auradock.ui.theme.AuraDockSpacing
import com.auradock.ui.theme.fromScreenBottom
import com.auradock.ui.theme.fromScreenTop
import com.auradock.intention.IntentionLine
import com.auradock.screensaver.ScreensaverBanner
import com.auradock.sessions.SessionSummary
import com.auradock.sessions.WeeklyFocus
import com.auradock.sessions.summaryIsFresh
import com.auradock.ui.viewmodel.CalendarPermissionUiState

/** Everything the dashboard shows. The ViewModel's flows are folded into this by DashboardRoute. */
data class DashboardUiState(
    val template: DockTemplate,
    val timeMillis: Long,
    val weather: Weather? = null,
    val locationPermission: CalendarPermissionUiState = CalendarPermissionUiState.GRANTED,
    /** Null until the first load finishes. */
    val events: List<CalendarEvent>? = emptyList(),
    /** Null until the first load finishes. */
    val tasks: List<TaskEntity>? = emptyList(),
    val exactAlarmsAllowed: Boolean = true,
    val calendarPermission: CalendarPermissionUiState = CalendarPermissionUiState.DENIED,
    /** A focus session just ended: its card shows for three seconds (null: no card). */
    val undockSummary: SessionSummary? = null,
    /** Focus time over the last seven days; null while it loads (nothing is drawn then). */
    val weeklyFocus: WeeklyFocus? = null,
    /** The morning intention, shown at the top of the Today card (null: none today). */
    val intention: IntentionLine? = null,
    /** The undock card gets a second step: "Tomorrow's top task?" (an evening session, not yet asked this evening). */
    val offerShutdown: Boolean = false,
    /** A quiet line about the screensaver test, if there is one to say. */
    val screensaverBanner: ScreensaverBanner = ScreensaverBanner.NONE
)

/** What the user can do on the dashboard. */
data class DashboardActions(
    val onEnableWeather: () -> Unit = {},
    val onAllowExactAlarms: () -> Unit = {},
    val onSaveTask: (id: Long?, title: String, dueAt: Long?) -> Unit = { _, _, _ -> },
    val onToggleTask: (TaskEntity) -> Unit = {},
    val onDeleteTask: (TaskEntity) -> Unit = {},
    val onOpenTemplates: () -> Unit = {},
    val onOpenFocus: () -> Unit = {},
    val onOpenSetup: () -> Unit = {},
    val onDockNow: () -> Unit = {},
    val onEnableCalendarAccess: () -> Unit = {},
    val onDismissUndockSummary: () -> Unit = {},
    val onToggleIntention: (done: Boolean) -> Unit = {},
    /** The "Tomorrow's top task?" step was shown, for the session that ended at this time. */
    val onShutdownShown: (endedAt: Long) -> Unit = {},
    val onSaveTomorrow: (title: String, dueAt: Long?) -> Unit = { _, _ -> },
    val onDismissScreensaverNote: () -> Unit = {},
    val onTryScreensaverAgain: () -> Unit = {}
)

/** The "today" screen. Stateless: draws [state] and reports taps through [actions]. */
@Composable
fun DashboardScreen(
    state: DashboardUiState,
    actions: DashboardActions,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val timeMillis = state.timeMillis
    val tasks = state.tasks

    val dateText = formatDashboardDate(timeMillis)
    val timeFormat = remember(context) { DateFormat.getTimeFormat(context) }
    // Task list add/edit sheet. -1 means "new task"; null means the sheet is closed.
    var sheetTaskId by rememberSaveable { mutableStateOf<Long?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AuraDockColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(start = AuraDockSpacing.mdl, end = AuraDockSpacing.mdl, top = fromScreenTop(AuraDockSpacing.huge), bottom = AuraDockSpacing.xxl),
        verticalArrangement = Arrangement.spacedBy(AuraDockSpacing.md)
    ) {
        // Everything settles in with blur to focus, one row after the other (40 ms apart).
        BlurIn(index = 0) {
            Column(modifier = Modifier.padding(start = AuraDockSpacing.xs, end = AuraDockSpacing.xs, bottom = AuraDockSpacing.smd)) {
                Text(text = greetingFor(timeMillis), color = AuraDockColors.OnBackground, style = AuraDockTypography.Greeting)
                WeatherRow(date = dateText, weather = state.weather, permission = state.locationPermission, onEnable = actions.onEnableWeather)
                state.weeklyFocus?.let { WeeklyFocusLine(it) }
                ScreensaverNote(state.screensaverBanner, onDone = actions.onDismissScreensaverNote, onTryAgain = actions.onTryScreensaverAgain)
            }
        }

        BlurIn(index = 1) {
            TaskCard(
                tasks = tasks,
                intention = state.intention,
                onToggleIntention = actions.onToggleIntention,
                timeMillis = timeMillis,
                formatTime = { timeFormat.format(Date(it)) },
                onAdd = { sheetTaskId = NEW_TASK },
                onEdit = { sheetTaskId = it.id },
                onToggle = actions.onToggleTask,
                onDelete = actions.onDeleteTask,
                modifier = Modifier.fillMaxWidth()
            )
        }
        BlurIn(index = 2) {
            CalendarCard(
                events = state.events,
                permission = state.calendarPermission,
                nowMillis = timeMillis,
                formatTime = { timeFormat.format(Date(it)) },
                onEnableAccess = actions.onEnableCalendarAccess
            )
        }

        BlurIn(index = 3) {
            DockCard(
                template = state.template,
                dockState = DockUiState(timeMillis, state.weather, state.events.orEmpty(), tasks.orEmpty()),
                onChange = actions.onOpenTemplates,
                onDockNow = actions.onDockNow
            )
        }

        BlurIn(index = 4) {
            Column(verticalArrangement = Arrangement.spacedBy(AuraDockSpacing.xs)) {
                NavTiles(templateCount = DockTemplate.available.size, onOpenTemplates = actions.onOpenTemplates, onOpenFocus = actions.onOpenFocus)
                // Not in the handoff: the only way back to Setup, so a skipped permission can still be fixed.
                GreyTextButton(text = "Setup", onClick = actions.onOpenSetup, modifier = Modifier.fillMaxWidth())
            }
        }
    }
    sheetTaskId?.let { id ->
            val editing = tasks?.firstOrNull { it.id == id }
            TaskSheet(
                task = editing,
                nowMillis = timeMillis,
                exactAlarmsAllowed = state.exactAlarmsAllowed,
                onAllowExactAlarms = actions.onAllowExactAlarms,
                onSave = { title, dueAt ->
                    actions.onSaveTask(editing?.id, title, dueAt)
                    sheetTaskId = null
                },
                onDismiss = { sheetTaskId = null },
                onDelete = editing?.let { task -> { actions.onDeleteTask(task); sheetTaskId = null } }
            )
        }
        state.undockSummary?.takeIf { summaryIsFresh(it.endedAt, timeMillis) }?.let {
            UndockSummaryCard(
                summary = it,
                offerShutdown = state.offerShutdown,
                nowMillis = timeMillis,
                onShutdownShown = { actions.onShutdownShown(it.endedAt) },
                onSaveTomorrow = actions.onSaveTomorrow,
                onDismiss = actions.onDismissUndockSummary,
                modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = AuraDockSpacing.mdl).padding(bottom = fromScreenBottom(AuraDockSpacing.xl))
            )
        }

    }
}

private const val NEW_TASK = -1L

/** One Secondary line under the title: "Thursday, 5 November · 31° Clear · Navi Mumbai". Without weather, a way to allow location. */
@Composable
private fun WeatherRow(date: String, weather: Weather?, permission: CalendarPermissionUiState, onEnable: () -> Unit) {
    val spacing = LocalAuraDockSpacing.current
    Column(modifier = Modifier.padding(top = spacing.xs)) {
        Text(
            text = if (weather != null) "$date · ${temperatureLabel(weather.temperatureC)} ${conditionLine(weather)}" else date,
            style = AuraDockTypography.Secondary,
            color = AuraDockColors.OnSurfaceVariant
        )
        if (weather == null && permission != CalendarPermissionUiState.GRANTED) {
            Text(
                text = if (permission == CalendarPermissionUiState.PERMANENTLY_DENIED) "Allow location in Settings for weather"
                else "Allow location for weather",
                style = AuraDockTypography.Secondary,
                color = AuraDockColors.Tertiary,
                modifier = Modifier
                    .defaultMinSize(minHeight = AuraDockSizes.touchTarget)
                    .clickable(onClick = onEnable)
                    .wrapContentHeight(Alignment.CenterVertically)
            )
        }
    }
}
