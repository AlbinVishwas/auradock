package com.auradock.dock

import androidx.compose.ui.graphics.Color
import com.auradock.dock.lines.DockLineActions
import com.auradock.dock.lines.DockLineStack
import com.auradock.dock.lines.IntentionPrompt
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.layout
import com.auradock.ui.theme.AuraDockTypography
import com.auradock.dock.world.WorldClock
import com.auradock.ui.theme.AuraDockBold
import com.auradock.ui.theme.AuraDockSpacing
import com.auradock.ui.theme.AuraDockSizes
import com.auradock.ui.theme.AuraDockDockLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import android.text.format.DateFormat
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import com.auradock.data.nextEvent
import com.auradock.data.upcomingEvents
import com.auradock.tasks.priorityOrder
import com.auradock.tasks.isOverdue
import com.auradock.tasks.topTask
import java.util.Date
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.auradock.ui.theme.AuraDockColors
import com.auradock.ui.weather.WeatherIcon
import com.auradock.ui.widgets.ClockWidget
import com.auradock.weather.Weather
import com.auradock.weather.conditionLine
import com.auradock.weather.temperatureLabel

/**
 * What the dock looks like for [template]. This is the one place that draws the dock: the screensaver, the
 * in-app dock, the dashboard thumbnail and the gallery all call it, so they cannot look different.
 *
 * It is "stateless": it is handed the time and draws. The live version that watches the clock and the saved
 * template is [DockScreen].
 */
@Composable
fun DockContent(template: DockTemplate, state: DockUiState, modifier: Modifier = Modifier, actions: DockActions = DockActions()) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AuraDockColors.Background),
        contentAlignment = Alignment.Center
    ) {
        val lines = stripLines(state.notifications, state.showMessageText)
        val caption = state.session?.let { heldCaption(it.held, it.through) }
        val rows = maxOf(lines.size, if (caption != null) 1 else 0)
        // The strip's own room under the template (Bold draws its strip inside the clock, so it needs none here).
        val stripRoom = if (rows == 0 || template == DockTemplate.BOLD || template == DockTemplate.WORLD) AuraDockSizes.none else AuraDockDockLayout.stripBase + AuraDockDockLayout.stripLine * rows
        // The quiet lines (intention, ...) sit just above the strip and the session line; the template leaves room for them too.
        val lineRoom = if (state.lines.count == 0) AuraDockSizes.none else AuraDockDockLayout.lineRow * state.lines.count
        val linesBottom = if (stripRoom > AuraDockSizes.none) stripRoom else AuraDockDockLayout.stripBase
        // Make room under the template for the strip and the lines so they never overlap (the clock moves up a little).
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = stripRoom + lineRoom),
            contentAlignment = Alignment.Center
        ) {
        if (state.intentionPrompt != null) {
            IntentionPrompt(prefill = state.intentionPrompt, onSave = actions.onSaveIntention, onSkip = actions.onSkipIntention)
        } else
        when (template) {
            DockTemplate.BOLD -> BoldClock(
                timeMillis = state.timeMillis,
                use24Hour = DateFormat.is24HourFormat(LocalContext.current),
                palette = state.boldPalette,
                strip = lines,
                caption = caption
            )
            DockTemplate.WORLD -> WorldClock(
                timeMillis = state.timeMillis,
                use24Hour = DateFormat.is24HourFormat(LocalContext.current),
                homeName = state.weather?.city,
                state = state.world,
                strip = lines,
                caption = caption
            )
            DockTemplate.MINIMAL -> ClockWidget(timeMillis = state.timeMillis, dimmed = true, showDate = false)
            DockTemplate.FOCUS -> SafeZone { FocusLayout(state) }
            DockTemplate.PRODUCTIVITY -> SafeZone { ProductivityLayout(state) }
            DockTemplate.CLASSIC -> SafeZone { ClassicLayout(state) }
        }
        }
        if (state.intentionPrompt == null) {
            DockLineStack(
                lines = state.lines,
                dimmed = template == DockTemplate.MINIMAL,
                actions = actions.lines,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = linesBottom)
            )
        }
        state.session?.let {
            SessionLine(
                elapsedMillis = state.timeMillis - it.startedAt,
                dimmed = template == DockTemplate.MINIMAL,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = AuraDockDockLayout.sessionLineBottom)
            )
        }
        NotificationStrip(
            lines = if (template == DockTemplate.BOLD || template == DockTemplate.WORLD) emptyList() else lines,
            caption = if (template == DockTemplate.BOLD || template == DockTemplate.WORLD) null else caption,
            dimmed = template == DockTemplate.MINIMAL,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

/** What touching the live dock can do: tick a line, answer the morning question. The screensaver passes none. */
data class DockActions(
    val lines: DockLineActions = DockLineActions(),
    val onSaveIntention: (String) -> Unit = {},
    val onSkipIntention: () -> Unit = {}
)

/** Keeps a template inside the safe zone: 40 dp at the sides, 24 dp at the top and bottom. */
@Composable
private fun SafeZone(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().padding(horizontal = AuraDockDockLayout.screenPadding, vertical = AuraDockDockLayout.safeVertical),
        contentAlignment = Alignment.Center
    ) { content() }
}

/**
 * Focus: giant, tall, condensed digits in coral on the left (hour over minutes); on the right the weekday and date, the
 * temperature, the next event and the top task. Pure black behind.
 */
@Composable
private fun FocusLayout(state: DockUiState) {
    val context = LocalContext.current
    val timeFormat = remember(context) { DateFormat.getTimeFormat(context) }
    val use24Hour = DateFormat.is24HourFormat(context)
    val next = nextEvent(state.events, state.timeMillis)
    val top = topTask(state.tasks, state.timeMillis)
    val parts = remember(state.timeMillis, use24Hour) { boldTimeString(state.timeMillis, use24Hour).split(":") }
    val date = remember(state.timeMillis) { DockDate.of(state.timeMillis) }

    Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        BoxWithConstraints(modifier = Modifier.weight(AuraDockDockLayout.FocusLeftShare).fillMaxHeight(), contentAlignment = Alignment.Center) {
            val fixed = Density(LocalDensity.current.density, fontScale = 1f)
            val em = constraints.maxHeight / (2f * AuraDockDockLayout.FocusCapEm + AuraDockDockLayout.FocusGapEm)
            val style = AuraDockTypography.DockCondensed.copy(fontSize = with(fixed) { em.toSp() }, color = AuraDockColors.Coral)
            Column(horizontalAlignment = Alignment.Start, verticalArrangement = Arrangement.spacedBy(with(fixed) { (em * AuraDockDockLayout.FocusGapEm).toDp() })) {
                parts.forEach { CondensedLine(it, style, capPx = em * AuraDockDockLayout.FocusCapEm) }
            }
        }
        Column(
            modifier = Modifier.weight(1f - AuraDockDockLayout.FocusLeftShare).padding(start = AuraDockDockLayout.columnGap),
            verticalArrangement = Arrangement.spacedBy(AuraDockDockLayout.listGap, Alignment.CenterVertically)
        ) {
            Column {
                Text(text = date.weekday, color = AuraDockColors.DockText, style = AuraDockTypography.DockHeading)
                Text(text = date.dayMonth, color = AuraDockColors.DockSecondary, style = AuraDockTypography.DockSecondary)
            }
            val weather = state.weather
            if (weather != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AuraDockDockLayout.weatherGap)) {
                    WeatherIcon(weather.condition.icon, AuraDockColors.DockSecondary, size = AuraDockDockLayout.weatherIconLarge)
                    Column {
                        Text(text = temperatureLabel(weather.temperatureC), color = AuraDockColors.DockText, style = AuraDockTypography.DockTemperature)
                        Text(text = conditionLine(weather), color = AuraDockColors.DockSecondary, style = AuraDockTypography.DockSecondary)
                    }
                }
            }
            if (next != null) FocusLine(label = "Next", text = "${timeFormat.format(Date(next.startTime))}  ${next.title}")
            if (top != null) {
                val overdue = isOverdue(top, state.timeMillis)
                val due = top.dueAt?.let { "  " + timeFormat.format(Date(it)) }.orEmpty()
                FocusLine(
                    label = if (overdue) "Overdue" else "Task",
                    text = top.title + due,
                    labelColor = if (overdue) AuraDockColors.DockOverdue else AuraDockColors.DockLabel
                )
            }
        }
    }
}

/** One line of the Focus digits, cut to the height of the digit itself (the font's own line is much taller). */
@Composable
private fun CondensedLine(text: String, style: androidx.compose.ui.text.TextStyle, capPx: Float) {
    Text(
        text = text,
        style = style,
        softWrap = false,
        modifier = Modifier.layout { measurable, constraints ->
            val placeable = measurable.measure(constraints.copy(minHeight = 0, maxHeight = androidx.compose.ui.unit.Constraints.Infinity))
            val height = capPx.toInt()
            layout(placeable.width, height) { placeable.place(0, -(placeable[FirstBaseline] - height)) }
        }
    )
}

/** A small grey label over one line of text. */
@Composable
private fun FocusLine(label: String, text: String, labelColor: Color = AuraDockColors.DockLabel) {
    Column {
        Text(text = label, color = labelColor, style = AuraDockTypography.DockLabel)
        Text(
            text = text,
            color = AuraDockColors.DockText,
            style = AuraDockTypography.DockBody,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** The pieces of a date the dock writes in different places. */
private data class DockDate(val weekday: String, val month: String, val day: String, val dayMonth: String) {
    companion object {
        fun of(millis: Long): DockDate {
            val t = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
            val locale = Locale.getDefault()
            return DockDate(
                weekday = DateTimeFormatter.ofPattern("EEEE", locale).format(t),
                month = DateTimeFormatter.ofPattern("MMMM", locale).format(t),
                day = t.dayOfMonth.toString(),
                dayMonth = DateTimeFormatter.ofPattern("d MMMM", locale).format(t)
            )
        }
    }
}

/**
 * Productivity: the clock and weather on the left; on the right, up to four of today's open tasks ("+N more" for the
 * rest) and up to three upcoming events. Only text and one hairline on black, so nothing stays lit as a big block on an OLED.
 */
@Composable
private fun ProductivityLayout(state: DockUiState) {
    val context = LocalContext.current
    val timeFormat = remember(context) { DateFormat.getTimeFormat(context) }
    val tasks = priorityOrder(state.tasks, state.timeMillis)
    val shownTasks = tasks.take(PRODUCTIVITY_TASKS)
    val moreTasks = tasks.size - shownTasks.size
    val events = upcomingEvents(state.events, state.timeMillis).take(PRODUCTIVITY_EVENTS)

    Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        Column(
            modifier = Modifier.weight(0.42f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(AuraDockDockLayout.listGap, Alignment.CenterVertically)
        ) {
            ClockWidget(timeMillis = state.timeMillis)
            WeatherBlock(state.weather)
        }

        // one thin line between the two halves
        Box(Modifier.padding(horizontal = AuraDockDockLayout.columnGap).width(AuraDockSizes.line).fillMaxHeight(0.7f).background(AuraDockColors.DockDivider))

        Column(
            modifier = Modifier.weight(0.58f),
            verticalArrangement = Arrangement.spacedBy(AuraDockSpacing.xxs, Alignment.CenterVertically)
        ) {
            SectionLabel("Today")
            if (shownTasks.isEmpty()) {
                QuietLine("Nothing left to do")
            } else {
                shownTasks.forEach { task ->
                    TaskLine(
                        title = task.title,
                        due = task.dueAt?.let { timeFormat.format(Date(it)) },
                        overdue = isOverdue(task, state.timeMillis)
                    )
                }
                if (moreTasks > 0) QuietLine("+$moreTasks more")
            }

            Spacer(Modifier.height(AuraDockSpacing.smd))
            SectionLabel("Up next")
            if (events.isEmpty()) {
                QuietLine("No more events today")
            } else {
                events.forEach { event ->
                    EventLine(time = timeFormat.format(Date(event.startTime)), title = event.title)
                }
            }
        }
    }
}

private const val PRODUCTIVITY_TASKS = 4
private const val PRODUCTIVITY_EVENTS = 3

@Composable
private fun SectionLabel(text: String) {
    Text(text = text, color = AuraDockColors.DockLabel, style = AuraDockTypography.DockLabel)
}

@Composable
private fun QuietLine(text: String) {
    Text(text = text, color = AuraDockColors.DockLabel, style = AuraDockTypography.DockSecondary, modifier = Modifier.padding(vertical = AuraDockSpacing.xxs))
}

/** An open circle, the task title, and its due time on the right (coral once it is overdue). */
@Composable
private fun TaskLine(title: String, due: String?, overdue: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = AuraDockSizes.line)) {
        Box(
            Modifier
                .size(AuraDockDockLayout.taskRing)
                .border(AuraDockDockLayout.strokeRing, if (overdue) AuraDockColors.DockOverdue else AuraDockColors.Outline, CircleShape)
        )
        Spacer(Modifier.width(AuraDockDockLayout.taskRingGap))
        Text(
            text = title,
            color = AuraDockColors.DockText,
            style = AuraDockTypography.DockTask,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        if (due != null) {
            Spacer(Modifier.width(AuraDockDockLayout.taskRingGap))
            Text(text = due, color = if (overdue) AuraDockColors.DockOverdue else AuraDockColors.DockSecondary, style = AuraDockTypography.DockSecondary)
        }
    }
}

@Composable
private fun EventLine(time: String, title: String) {
    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(vertical = AuraDockSizes.line)) {
        Text(text = time, color = AuraDockColors.DockSecondary, style = AuraDockTypography.DockSecondary, modifier = Modifier.width(AuraDockDockLayout.timeColumn))
        Text(text = title, color = AuraDockColors.DockText, style = AuraDockTypography.DockBody, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Icon and temperature with the condition under it, or a quiet dash. */
@Composable
private fun WeatherBlock(weather: Weather?) {
    if (weather == null) {
        Text(text = "—", color = AuraDockColors.DockLabel, style = AuraDockTypography.DockTemperature)
        return
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AuraDockDockLayout.weatherGap)) {
            WeatherIcon(weather.condition.icon, AuraDockColors.DockSecondary, size = AuraDockDockLayout.weatherIcon)
            Text(text = temperatureLabel(weather.temperatureC), color = AuraDockColors.DockText, style = AuraDockTypography.DockTemperature)
        }
        Text(text = conditionLine(weather), color = AuraDockColors.DockSecondary, style = AuraDockTypography.DockCondition)
    }
}

/**
 * Classic: a big date on the left (the weekday in coral, the month in grey, a huge white day number); on the right the
 * city, a large temperature, the condition and an icon.
 */
@Composable
private fun ClassicLayout(state: DockUiState) {
    val date = remember(state.timeMillis) { DockDate.of(state.timeMillis) }
    BoxWithConstraints(Modifier.fillMaxSize()) {
    // The day number is as big as fits under the weekday and month: smaller when a notification strip takes a row of the screen.
    val fixed = Density(LocalDensity.current.density, fontScale = 1f)
    val full = AuraDockTypography.DockDayNumber.fontSize.value
    val fitted = ((maxHeight - AuraDockDockLayout.classicHeading).value / AuraDockDockLayout.DayLineEm).coerceIn(full / 2, full)
    val daySize = with(fixed) { fitted.dp.toSp() }
    Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text(text = date.weekday, color = AuraDockColors.Coral, style = AuraDockTypography.DockWeekday)
            Text(text = date.month, color = AuraDockColors.DockLabel, style = AuraDockTypography.DockMonth)
            Text(
                text = date.day,
                color = AuraDockColors.DockText,
                style = AuraDockTypography.DockDayNumber.copy(fontSize = daySize, lineHeight = daySize * AuraDockDockLayout.DayLineEm),
                // The glyph is taller than its line box: do not cut it at the box.
                overflow = androidx.compose.ui.text.style.TextOverflow.Visible,
                softWrap = false
            )
        }
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.Center) {
            val weather = state.weather
            if (weather == null) {
                Text(text = "—", color = AuraDockColors.DockLabel, style = AuraDockTypography.DockTemperatureLarge)
            } else {
                weather.city?.let { Text(text = it, color = AuraDockColors.DockSecondary, style = AuraDockTypography.DockHeading) }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AuraDockDockLayout.taskRingGap)) {
                    WeatherIcon(weather.condition.icon, AuraDockColors.DockSecondary, size = AuraDockDockLayout.weatherIconLarge)
                    Text(text = temperatureLabel(weather.temperatureC), color = AuraDockColors.DockText, style = AuraDockTypography.DockTemperatureLarge)
                }
                Text(text = weather.condition.label, color = AuraDockColors.DockSecondary, style = AuraDockTypography.DockCondition)
            }
        }
    }
    }
}
