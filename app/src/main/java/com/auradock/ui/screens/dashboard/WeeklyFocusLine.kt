package com.auradock.ui.screens.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.auradock.sessions.WeeklyFocus
import com.auradock.sessions.formatFocusTime
import com.auradock.ui.theme.AuraDockColors
import com.auradock.ui.theme.AuraDockSizes
import com.auradock.ui.theme.AuraDockSpacing
import com.auradock.ui.theme.AuraDockTypography

/**
 * One line under the greeting: "4 h 20 m focused this week" with a seven-day mini bar, today on the right in the accent. With
 * no focus time yet, an invitation instead: "Dock your phone to start a focus session."
 */
@Composable
fun WeeklyFocusLine(weekly: WeeklyFocus, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(top = AuraDockSpacing.smd),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AuraDockSpacing.smd + AuraDockSpacing.xxs)
    ) {
        if (weekly.isEmpty) {
            Text(text = "Dock your phone to start a focus session.", style = AuraDockTypography.Secondary, color = AuraDockColors.Tertiary)
        } else {
            Text(text = "${formatFocusTime(weekly.totalMillis)} focused this week", style = AuraDockTypography.Secondary, color = AuraDockColors.OnSurfaceVariant)
            WeekBars(weekly.perDayMillis)
        }
    }
}

/** Seven small bars, one a day, the tallest day full height. A day with nothing is a dot. Today (the last) is cream. */
@Composable
private fun WeekBars(perDayMillis: List<Long>) {
    val widest = perDayMillis.maxOrNull()?.coerceAtLeast(1L) ?: 1L
    val width = AuraDockSizes.weekBar * perDayMillis.size + AuraDockSizes.weekBarGap * (perDayMillis.size - 1)
    val description = "Focus time over the last seven days: " + perDayMillis.joinToString(", ") { formatFocusTime(it) }
    Canvas(Modifier.size(width, AuraDockSizes.weekBarMax).semantics { contentDescription = description }) {
        val barWidth = AuraDockSizes.weekBar.toPx()
        val gap = AuraDockSizes.weekBarGap.toPx()
        val min = AuraDockSizes.weekBarMin.toPx()
        val max = size.height
        perDayMillis.forEachIndexed { i, value ->
            val height = if (value <= 0L) AuraDockSizes.weekBarEmpty.toPx() else min + (max - min) * (value.toFloat() / widest)
            val color = when {
                value <= 0L -> AuraDockColors.Inactive
                i == perDayMillis.lastIndex -> AuraDockColors.Accent
                else -> AuraDockColors.Tertiary
            }
            drawRoundRect(color, Offset(i * (barWidth + gap), max - height), Size(barWidth, height), CornerRadius(barWidth / 2f))
        }
    }
}
