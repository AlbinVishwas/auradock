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
    val timeFormat = remember(context) { DateFormat.getTimeFormat(context) }
    // Task list add/edit sheet. -1 means "new task"; null means the sheet is closed.
    var sheetTaskId by rememberSaveable { mutableStateOf<Long?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AuraDockColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.mdl),
        verticalArrangement = Arrangement.Top
    ) {
        // Everything settles in with blur to focus, one row after the other (40 ms apart).
        BlurIn(index = 0) {
            Column {
                Text(
                    text = greetingFor(timeMillis),
                    modifier = Modifier.padding(top = spacing.xl),
                    color = AuraDockColors.OnBackground,
                    style = AuraDockTypography.ScreenTitle
                )
                WeatherRow(
                    date = dateText,
                    weather = state.weather,
                    permission = state.locationPermission,
                    onEnable = actions.onEnableWeather
                )
            }
        }

        Spacer(modifier = Modifier.height(spacing.lg))
        BlurIn(index = 1) {
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
                onDismiss = { sheetTaskId = null }
            )
        }

        Spacer(modifier = Modifier.height(spacing.md))
        val summary = buildTodaySummary(
            events = state.events,
            permission = state.calendarPermission,
            formatTime = { timeFormat.format(Date(it)) }
        )
        BlurIn(index = 2) {
            TodaySnapshotCard(
                modifier = Modifier.fillMaxWidth(),
                title = "Calendar",
                items = summary.lines,
                emptyStateText = summary.emptyText,
                actionText = summary.actionText,
                onAction = actions.onEnableCalendarAccess
            )
        }

        Spacer(modifier = Modifier.height(spacing.md))
        // The dock card: a landscape thumbnail of the dock in use, and the one primary pill on this screen.
        BlurIn(index = 3) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(AuraDockRadii.card))
                    .background(AuraDockColors.Surface)
                    .padding(spacing.md),
                verticalArrangement = Arrangement.spacedBy(spacing.md)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(AuraDockRadii.control))
                ) {
                    // Lay the template out at a realistic landscape size, then shrink it to fit the card.
                    ScaledPreview(designWidth = AuraDockGallery.previewDesignWidth) {
                        DockContent(
                            template = state.template,
                            state = DockUiState(timeMillis, state.weather, state.events.orEmpty(), tasks.orEmpty())
                        )
                    }
                }
                val morph = LocalDockMorph.current
                var pillBounds by remember { mutableStateOf(Rect.Zero) }
                PrimaryPill(
                    text = "Dock now",
                    onClick = { morph(pillBounds, actions.onDockNow) },
                    modifier = Modifier.reportBounds { pillBounds = it }
                )
            }
        }

        Spacer(modifier = Modifier.height(spacing.md))
        BlurIn(index = 4) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.smd),
                verticalAlignment = Alignment.CenterVertically
            ) {
                QuietPill(text = "Templates", onClick = actions.onOpenTemplates)
                QuietPill(text = "Focus", onClick = actions.onOpenFocus)
                GreyTextButton(text = "Setup", onClick = actions.onOpenSetup)
            }
        }

        Spacer(modifier = Modifier.height(spacing.xl))
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
