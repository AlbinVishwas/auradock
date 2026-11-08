package com.auradock.dock

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.auradock.ui.theme.AuraDockColors
import com.auradock.ui.theme.AuraDockDockLayout

/**
 * How much of the line to fill: how far into the current lap the session is. One lap is [lapMillis] (an hour); after that it
 * starts again, so a long session shows a line that keeps moving rather than one stuck at full. Never below 0 or above 1.
 */
fun sessionLineFill(elapsedMillis: Long, lapMillis: Long): Float {
    if (elapsedMillis <= 0L || lapMillis <= 0L) return 0f
    return (elapsedMillis % lapMillis).toFloat() / lapMillis
}

/**
 * A thin line under the dock that shows how long this session has gone on. Grey, never coral, so the clock stays the
 * loudest thing; it is part of the dock content, so it drifts with it (burn-in), and it only changes once a minute.
 * [dimmed] is for Minimal, which is lowered for OLED.
 */
@Composable
fun SessionLine(elapsedMillis: Long, dimmed: Boolean, modifier: Modifier = Modifier) {
    val fill = sessionLineFill(elapsedMillis, AuraDockDockLayout.SessionLapMinutes * 60_000L)
    val track = if (dimmed) AuraDockColors.SessionTrackDim else AuraDockColors.SessionTrack
    val bar = if (dimmed) AuraDockColors.SessionFillDim else AuraDockColors.SessionFill
    val minutes = (elapsedMillis / 60_000L).coerceAtLeast(0)
    Canvas(
        modifier = modifier
            .size(AuraDockDockLayout.sessionLineWidth, AuraDockDockLayout.sessionLineHeight)
            .semantics { contentDescription = "Focus session: $minutes minutes" }
    ) {
        val radius = CornerRadius(size.height / 2f)
        drawRoundRect(track, Offset.Zero, size, radius)
        if (fill > 0f) drawRoundRect(bar, Offset.Zero, Size(maxOf(size.width * fill, size.height), size.height), radius)
    }
}
