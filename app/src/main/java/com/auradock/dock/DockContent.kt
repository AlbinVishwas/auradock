package com.auradock.dock

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import android.text.format.DateFormat
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import com.auradock.data.nextEvent
import com.auradock.tasks.isOverdue
import com.auradock.tasks.topTask
import java.util.Date
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.auradock.ui.components.GlassCard
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
fun DockContent(template: DockTemplate, state: DockUiState, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        when (template) {
            DockTemplate.MINIMAL -> ClockWidget(timeMillis = state.timeMillis, dimmed = true, showDate = false)
            DockTemplate.FOCUS -> FocusLayout(state)
            // PRODUCTIVITY is built in the next commit; until then it draws Classic.
            else -> ClassicLayout(state)
        }
    }
}

/**
 * Focus: a big clock and almost nothing else. Underneath, at most two quiet lines: the next calendar event today and
 * the one task that matters most. If there is neither, it is just the clock. Mostly black, no panels.
 */
@Composable
private fun FocusLayout(state: DockUiState) {
    val context = LocalContext.current
    val timeFormat = remember(context) { DateFormat.getTimeFormat(context) }
    val next = nextEvent(state.events, state.timeMillis)
    val top = topTask(state.tasks, state.timeMillis)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(36.dp, Alignment.CenterVertically)
    ) {
        ClockWidget(timeMillis = state.timeMillis, showDate = false)
        if (next != null || top != null) {
            Column(modifier = Modifier.widthIn(max = 560.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (next != null) {
                    FocusLine(label = "NEXT", text = "${timeFormat.format(Date(next.startTime))}  ${next.title}")
                }
                if (top != null) {
                    val overdue = isOverdue(top, state.timeMillis)
                    val due = top.dueAt?.let { "  " + timeFormat.format(Date(it)) }.orEmpty()
                    FocusLine(
                        label = if (overdue) "OVERDUE" else "TASK",
                        text = top.title + due,
                        labelColor = if (overdue) FocusOverdue else FocusLabel
                    )
                }
            }
        }
    }
}

/** Dim, letter-spaced label on the left and a soft-white line of text: small and calm, nothing pure white. */
@Composable
private fun FocusLine(label: String, text: String, labelColor: Color = FocusLabel) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            text = label,
            color = labelColor,
            fontSize = 12.sp,
            letterSpacing = 2.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(92.dp).padding(bottom = 4.dp)
        )
        Text(
            text = text,
            color = FocusText,
            fontSize = 24.sp,
            fontWeight = FontWeight.Light,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private val FocusLabel = Color(0xFF6E6E6E)
private val FocusOverdue = Color(0xFFB8892D)
private val FocusText = Color(0xFFD2D2D2)

/** Big clock, date, weather, on a glass card. */
@Composable
private fun ClassicLayout(state: DockUiState) {
    GlassCard(modifier = Modifier.fillMaxSize().padding(32.dp)) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically)
        ) {
            ClockWidget(timeMillis = state.timeMillis)
            WeatherLine(weather = state.weather)
        }
    }
}

/** Icon, temperature and condition (with the city if known), or a quiet dash when there is no weather. Never fake data. */
@Composable
private fun WeatherLine(weather: Weather?) {
    if (weather == null) {
        Text(text = "—", color = AuraDockColors.OnSurfaceVariant, fontSize = 28.sp, fontWeight = FontWeight.Light)
        return
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        WeatherIcon(weather.condition.icon, AuraDockColors.OnSurfaceVariant, size = 44.dp)
        Text(
            text = temperatureLabel(weather.temperatureC),
            color = Color.White,
            fontSize = 40.sp,
            fontWeight = FontWeight.Light
        )
        Text(
            text = conditionLine(weather),
            color = AuraDockColors.OnSurfaceVariant,
            fontSize = 20.sp,
            fontWeight = FontWeight.Light
        )
    }
}
