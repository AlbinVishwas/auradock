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
import com.auradock.ui.components.QuickActionButton
import com.auradock.time.formatDashboardDate
import com.auradock.time.greetingFor
import com.auradock.ui.components.ScaledPreview
import com.auradock.ui.components.StandbyPreviewCard
import com.auradock.ui.components.TodaySnapshotCard
import com.auradock.dock.DockTemplate
import com.auradock.dock.DockContent
import com.auradock.dock.DockUiState
import com.auradock.weather.Weather
import com.auradock.weather.conditionLine
import com.auradock.weather.temperatureLabel
import com.auradock.ui.weather.WeatherIcon
import com.auradock.tasks.TaskEntity
import com.auradock.ui.tasks.TaskCard
import com.auradock.ui.tasks.TaskSheet
import com.auradock.ui.theme.AuraDockGallery
import com.auradock.ui.theme.AuraDockSizes
import com.auradock.ui.theme.AuraDockTheme
import com.auradock.ui.theme.LocalAuraDockSpacing
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
    val calendarPermission: CalendarPermissionUiState = CalendarPermissionUiState.DENIED
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
    val onEnableCalendarAccess: () -> Unit = {}
)

/** The "today" screen. Stateless: draws [state] and reports taps through [actions]. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    state: DashboardUiState,
    actions: DashboardActions,
    modifier: Modifier = Modifier
) {
    val spacing = LocalAuraDockSpacing.current
    val context = LocalContext.current
    val timeMillis = state.timeMillis
    val tasks = state.tasks

    val dateText = formatDashboardDate(timeMillis)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AuraDockColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.lg),
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = greetingFor(timeMillis),
            modifier = Modifier.padding(top = spacing.xl),
            color = AuraDockColors.OnBackground,
            style = AuraDockTypography.ScreenTitle
        )

        Text(
            text = dateText,
            color = AuraDockColors.OnSurfaceVariant,
            style = AuraDockTypography.WidgetLabel,
            modifier = Modifier.padding(top = spacing.xs)
        )

        Spacer(modifier = Modifier.height(spacing.sm))
        WeatherRow(weather = state.weather, permission = state.locationPermission, onEnable = actions.onEnableWeather)

        Spacer(modifier = Modifier.height(spacing.lg))

        val timeFormat = remember(context) { DateFormat.getTimeFormat(context) }

        // Task list + its add/edit sheet. -1 means "new task"; null means the sheet is closed.
        var sheetTaskId by rememberSaveable { mutableStateOf<Long?>(null) }
        TaskCard(
            tasks = tasks,
            timeMillis = timeMillis,
            formatTime = { timeFormat.format(Date(it)) },
            onAdd = { sheetTaskId = NEW_TASK },
            onEdit = { sheetTaskId = it.id },
            onToggle = actions.onToggleTask,
            onDelete = actions.onDeleteTask,
            modifier = Modifier.fillMaxWidth()
        )
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
                onDismiss = { sheetTaskId = null }
            )
        }

        Spacer(modifier = Modifier.height(spacing.lg))
        val summary = buildTodaySummary(
            events = state.events,
            permission = state.calendarPermission,
            formatTime = { timeFormat.format(Date(it)) }
        )
        TodaySnapshotCard(
            modifier = Modifier.fillMaxWidth(),
            title = "Calendar",
            items = summary.lines,
            emptyStateText = summary.emptyText,
            actionText = summary.actionText,
            onAction = actions.onEnableCalendarAccess
        )

        Spacer(modifier = Modifier.height(spacing.lg))

        // FlowRow wraps buttons onto a new line when they don't fit, so none is squeezed or pushed off-screen.
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            QuickActionButton(text = "Templates", onClick = actions.onOpenTemplates)
            QuickActionButton(text = "Focus", onClick = actions.onOpenFocus)
            QuickActionButton(text = "Setup", onClick = actions.onOpenSetup)
        }

        Spacer(modifier = Modifier.height(spacing.lg))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StandbyPreviewCard(
                selected = true,
                modifier = Modifier.weight(1f)
            ) {
                // Lay the template out at a realistic landscape size, then shrink it to fit the card.
                ScaledPreview(designWidth = AuraDockGallery.previewDesignWidth) {
                    DockContent(
                        template = state.template,
                        state = DockUiState(timeMillis, state.weather, state.events.orEmpty(), tasks.orEmpty())
                    )
                }
            }
            QuickActionButton(text = "Dock now", onClick = actions.onDockNow, isActive = true)
        }

        Spacer(modifier = Modifier.height(spacing.xl))
    }
}

private const val NEW_TASK = -1L

/** One quiet line under the date: icon, temperature, condition and city, or a dash (with a way to allow location). */
@Composable
private fun WeatherRow(weather: Weather?, permission: CalendarPermissionUiState, onEnable: () -> Unit) {
    val spacing = LocalAuraDockSpacing.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
        if (weather != null) {
            WeatherIcon(weather.condition.icon, AuraDockColors.OnSurfaceVariant, size = AuraDockSizes.weatherIconSmall)
            Text(
                text = temperatureLabel(weather.temperatureC) + "  " + conditionLine(weather),
                style = AuraDockTypography.WidgetLabel,
                color = AuraDockColors.OnSurfaceVariant
            )
        } else {
            Text(text = "—", style = AuraDockTypography.WidgetLabel, color = AuraDockColors.OnSurfaceVariant)
            if (permission != CalendarPermissionUiState.GRANTED) {
                Text(
                    text = if (permission == CalendarPermissionUiState.PERMANENTLY_DENIED) "Allow location in Settings for weather"
                    else "Allow location for weather",
                    style = AuraDockTypography.Caption,
                    color = AuraDockColors.AccentBlue,
                    modifier = Modifier.clickable(onClick = onEnable)
                )
            }
        }
    }
}
