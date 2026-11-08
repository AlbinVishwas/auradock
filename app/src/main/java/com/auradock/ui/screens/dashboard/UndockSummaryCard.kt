package com.auradock.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import com.auradock.sessions.SessionSummary
import com.auradock.sessions.formatFocusTime
import com.auradock.sessions.tasksDoneLabel
import com.auradock.ui.components.BlurIn
import com.auradock.ui.theme.AuraDockColors
import com.auradock.ui.theme.AuraDockMotion
import com.auradock.ui.theme.AuraDockRadii
import com.auradock.ui.theme.AuraDockSizes
import com.auradock.ui.theme.AuraDockSpacing
import com.auradock.ui.theme.AuraDockTypography
import kotlinx.coroutines.delay

/**
 * What a focus session came to, shown for three seconds when the dock closes (tap to close it sooner): how long, how many
 * tasks were ticked off, how many notifications were held. It settles in with blur to focus. Counts only, never content.
 */
@Composable
fun UndockSummaryCard(summary: SessionSummary, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    LaunchedEffect(summary.endedAt) {
        delay(AuraDockMotion.UndockCard.toLong())
        onDismiss()
    }
    BlurIn(modifier = modifier) {
        val shape = RoundedCornerShape(AuraDockRadii.card)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(AuraDockColors.SurfaceRaised)
                .border(AuraDockSizes.line, AuraDockColors.TileOutline, shape)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onDismiss)
                .padding(AuraDockSpacing.mdl),
            verticalArrangement = Arrangement.spacedBy(AuraDockSpacing.md)
        ) {
            Text(text = "Focus session", style = AuraDockTypography.Caption, color = AuraDockColors.Tertiary)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AuraDockSpacing.md)) {
                Stat(formatFocusTime(summary.durationMs), "focused", Modifier.weight(1.4f))
                Stat(summary.tasksCompleted.toString(), tasksDoneLabel(summary.tasksCompleted), Modifier.weight(1f))
                Stat(summary.counts.held.toString(), "held", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = value, style = AuraDockTypography.SummaryValue, color = AuraDockColors.OnBackground, maxLines = 1)
        Text(text = label, style = AuraDockTypography.RowCaption, color = AuraDockColors.OnSurfaceVariant, modifier = Modifier.padding(top = AuraDockSpacing.xxs))
    }
}
