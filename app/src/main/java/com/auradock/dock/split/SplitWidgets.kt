package com.auradock.dock.split

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.auradock.dock.DockUiState
import com.auradock.ui.theme.AuraDockColors
import com.auradock.ui.theme.AuraDockSplit
import com.auradock.ui.theme.AuraDockTypography
import com.auradock.ui.theme.SplitTones
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/** What a widget draws from: the dock's data, whether the phone is on a 24-hour clock, and the day or night tones. */
data class WidgetEnv(val state: DockUiState, val use24Hour: Boolean, val tones: SplitTones, val firstDay: java.time.DayOfWeek = phoneFirstDay())

/** One widget, filling a 405 x 364 half. Each is self-contained, so it works on either side. */
@Composable
fun SplitWidget(kind: WidgetKind, env: WidgetEnv, modifier: Modifier = Modifier) {
    when (kind) {
        WidgetKind.DIAL -> DialWidget(env, modifier)
        WidgetKind.MONTH -> MonthWidget(env, modifier)
        else -> Box(modifier.fillMaxSize())
    }
}

/**
 * The analog dial: a ring of minute dots, twelve ticks, 12 / 3 / 6 / 9, white hour and minute hands and, by day, a thin coral second hand
 * that ticks once a second. At night it has no second hand, so it redraws only once a minute.
 */
@Composable
fun DialWidget(env: WidgetEnv, modifier: Modifier = Modifier) {
    val tones = env.tones
    val night = tones.night
    val clock by produceState(env.state.timeMillis, night, env.state.timeMillis) {
        value = env.state.timeMillis
        if (!night) {
            while (true) {
                val t = System.currentTimeMillis()
                value = t
                delay(1000L - t % 1000L)
            }
        }
    }
    val time = remember(clock) { Instant.ofEpochMilli(clock).atZone(ZoneId.systemDefault()) }
    val measurer = rememberTextMeasurer()
    val numeral = AuraDockTypography.SplitNumeral.copy(color = tones.secondary)
    val stage = AuraDockColors.Background
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(AuraDockSplit.DialSize)) {
            val u = 1.dp.toPx()
            val c = Offset(size.width / 2f, size.height / 2f)
            fun at(angleDeg: Double, r: Float): Offset {
                val a = Math.toRadians(angleDeg)
                return Offset(c.x + (r * u * sin(a)).toFloat(), c.y - (r * u * cos(a)).toFloat())
            }
            for (i in 0 until 60) if (i % 5 != 0) drawCircle(tones.ring, AuraDockSplit.DialDotRadius * u, at(i * 6.0, AuraDockSplit.DialRingRadius))
            for (h in 0 until 12) drawLine(tones.tertiary, at(h * 30.0, AuraDockSplit.DialTickInner), at(h * 30.0, AuraDockSplit.DialTickOuter), AuraDockSplit.DialTickStroke * u, StrokeCap.Round)
            listOf(0 to "12", 90 to "3", 180 to "6", 270 to "9").forEach { (deg, label) ->
                val m = measurer.measure(label, numeral)
                val p = at(deg.toDouble(), AuraDockSplit.DialNumeralRadius)
                drawText(m, topLeft = Offset(p.x - m.size.width / 2f, p.y - m.size.height / 2f))
            }
            val hourAngle = (time.hour % 12) * 30.0 + time.minute * 0.5
            val minuteAngle = time.minute * 6.0
            drawLine(tones.primary, c, at(hourAngle, AuraDockSplit.DialHourLength), AuraDockSplit.DialHourStroke * u, StrokeCap.Round)
            drawLine(tones.primary, c, at(minuteAngle, AuraDockSplit.DialMinuteLength), AuraDockSplit.DialMinuteStroke * u, StrokeCap.Round)
            if (!night) {
                val a = time.second * 6.0
                drawLine(AuraDockColors.Coral, at(a + 180.0, AuraDockSplit.DialSecondTail), at(a, AuraDockSplit.DialSecondLength), AuraDockSplit.DialSecondStroke * u, StrokeCap.Round)
            }
            drawCircle(tones.accent, AuraDockSplit.DialHubRadius * u, c)
            drawCircle(stage, AuraDockSplit.DialHubHole * u, c)
        }
    }
}

/** The month: its name, the weekday initials (from the phone's first day of the week), and the days, today in a coral disc, a dot under days with events. */
@Composable
fun MonthWidget(env: WidgetEnv, modifier: Modifier = Modifier) {
    val tones = env.tones
    val today = remember(env.state.timeMillis) { Instant.ofEpochMilli(env.state.timeMillis).atZone(ZoneId.systemDefault()).toLocalDate() }
    val locale = Locale.getDefault()
    val cells = remember(today.year, today.monthValue, env.firstDay) { monthCells(YearMonth.of(today.year, today.monthValue), env.firstDay) }
    val initials = remember(env.firstDay) { weekdayInitials(env.firstDay, locale) }
    val eventDays = env.state.split.monthEventDays
    Column(
        modifier.fillMaxSize().padding(horizontal = AuraDockSplit.MonthSidePad),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            monthTitle(today, locale), style = AuraDockTypography.SplitMonthTitle, color = tones.primary,
            modifier = Modifier.padding(start = AuraDockSplit.MonthTitleInset)
        )
        Row(Modifier.fillMaxWidth().padding(top = 16.dp)) {
            initials.forEach { Text(it, style = AuraDockTypography.SplitWeekday, color = tones.tertiary, textAlign = TextAlign.Center, modifier = Modifier.weight(1f)) }
        }
        Column(Modifier.padding(top = 6.dp)) {
            cells.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth().height(AuraDockSplit.MonthRow)) {
                    week.forEach { day ->
                        Box(Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
                            if (day != null) DayCell(day, today.dayOfMonth, tones, day in eventDays)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(day: Int, today: Int, tones: SplitTones, hasEvent: Boolean) {
    when {
        day == today -> Box(Modifier.size(AuraDockSplit.MonthTodayDisc).clip(CircleShape).background(tones.accent), contentAlignment = Alignment.Center) {
            Text(day.toString(), style = AuraDockTypography.SplitToday, color = AuraDockColors.Background)
        }
        else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(day.toString(), style = AuraDockTypography.SplitDay, color = if (day < today) tones.tertiary else tones.primary)
            if (hasEvent) Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 1.dp).size(AuraDockSplit.MonthEventDot).clip(CircleShape).background(tones.secondary))
        }
    }
}
