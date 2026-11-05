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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.auradock.ui.theme.AuraDockTheme
import com.auradock.ui.theme.LocalAuraDockSpacing
import com.auradock.ui.viewmodel.CalendarPermissionUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    currentTemplate: DockTemplate,
    timeMillis: Long,
    modifier: Modifier = Modifier,
    weather: Weather? = null,
    locationPermissionState: CalendarPermissionUiState = CalendarPermissionUiState.GRANTED,
    onEnableWeather: () -> Unit = {},
    todayEvents: List<CalendarEvent>? = emptyList(),
    tasks: List<TaskEntity>? = emptyList(),
    exactAlarmsAllowed: Boolean = true,
    onAllowExactAlarms: () -> Unit = {},
    onSaveTask: (id: Long?, title: String, dueAt: Long?) -> Unit = { _, _, _ -> },
    onToggleTask: (TaskEntity) -> Unit = {},
    onDeleteTask: (TaskEntity) -> Unit = {},
    calendarPermissionState: CalendarPermissionUiState = CalendarPermissionUiState.DENIED,
    onOpenTemplates: () -> Unit = {},
    onOpenFocus: () -> Unit = {},
    onDockNow: () -> Unit = {},
    onEnableCalendarAccess: () -> Unit = {}
) {
    val spacing = LocalAuraDockSpacing.current
    val context = LocalContext.current

    val dateText = formatDashboardDate(timeMillis)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.lg),
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = greetingFor(timeMillis),
            modifier = Modifier.padding(top = spacing.xl),
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.SemiBold
        )

        Text(
            text = dateText,
            color = Color.Gray,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = spacing.xs)
        )

        Spacer(modifier = Modifier.height(spacing.sm))
        WeatherRow(weather = weather, permission = locationPermissionState, onEnable = onEnableWeather)

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
            onToggle = onToggleTask,
            onDelete = onDeleteTask,
            modifier = Modifier.fillMaxWidth()
        )
        sheetTaskId?.let { id ->
            val editing = tasks?.firstOrNull { it.id == id }
            TaskSheet(
                task = editing,
                nowMillis = timeMillis,
                exactAlarmsAllowed = exactAlarmsAllowed,
                onAllowExactAlarms = onAllowExactAlarms,
                onSave = { title, dueAt ->
                    onSaveTask(editing?.id, title, dueAt)
                    sheetTaskId = null
                },
                onDismiss = { sheetTaskId = null }
            )
        }

        Spacer(modifier = Modifier.height(spacing.lg))
        val summary = buildTodaySummary(
            events = todayEvents,
            permission = calendarPermissionState,
            formatTime = { timeFormat.format(Date(it)) }
        )
        TodaySnapshotCard(
            modifier = Modifier.fillMaxWidth(),
            title = "Calendar",
            items = summary.lines,
            emptyStateText = summary.emptyText,
            actionText = summary.actionText,
            onAction = onEnableCalendarAccess
        )

        Spacer(modifier = Modifier.height(spacing.lg))

        // FlowRow wraps buttons onto a new line when they don't fit, so none is squeezed or pushed off-screen.
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.sm)
        ) {
            QuickActionButton(text = "Templates", onClick = onOpenTemplates)
            QuickActionButton(text = "Focus", onClick = onOpenFocus)
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
                ScaledPreview(designWidth = 800.dp) {
                    DockContent(
                        template = currentTemplate,
                        state = DockUiState(timeMillis, weather, todayEvents.orEmpty(), tasks.orEmpty())
                    )
                }
            }
            QuickActionButton(text = "Dock now", onClick = onDockNow, isActive = true)
        }

        Spacer(modifier = Modifier.height(spacing.xl))
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun DashboardScreenPreview() {
    AuraDockTheme {
        DashboardScreen(currentTemplate = DockTemplate.CLASSIC, timeMillis = System.currentTimeMillis())
    }
}

private const val NEW_TASK = -1L

/** One quiet line under the date: icon, temperature, condition and city, or a dash (with a way to allow location). */
@Composable
private fun WeatherRow(weather: Weather?, permission: CalendarPermissionUiState, onEnable: () -> Unit) {
    val spacing = LocalAuraDockSpacing.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
        if (weather != null) {
            WeatherIcon(weather.condition.icon, AuraDockColors.OnSurfaceVariant, size = 22.dp)
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
