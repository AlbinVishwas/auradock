package com.auradock.dock.lines

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import com.auradock.data.MeetingCountdown
import com.auradock.intention.IntentionLine
import com.auradock.ui.components.StrikeText
import com.auradock.ui.theme.AuraDockColors
import com.auradock.ui.theme.AuraDockDockLayout
import com.auradock.ui.theme.AuraDockMotion
import com.auradock.ui.theme.AuraDockSizes
import com.auradock.ui.theme.AuraDockTypography

/**
 * The quiet lines that sit under the clock on every template, top to bottom. Each is its own self-contained component
 * (its state in, its taps out), so the Split dock can place them as widgets of their own.
 */
data class DockLines(
    /** The next meeting, from ten minutes before it starts. Always on top. */
    val meeting: MeetingCountdown? = null,
    val intention: IntentionLine? = null
) {
    /** How many rows the stack takes: the dock leaves room for exactly this many. */
    val count: Int get() = listOfNotNull(meeting, intention).size
}

/** What the lines can do when touched. Only the in-app dock acts on touches; the screensaver ignores them. */
data class DockLineActions(
    val onToggleIntention: (done: Boolean) -> Unit = {}
)

/** The stack of [lines], one row each, centred. Draws nothing when there are none. */
@Composable
fun DockLineStack(lines: DockLines, dimmed: Boolean, actions: DockLineActions, modifier: Modifier = Modifier) {
    if (lines.count == 0) return
    Column(modifier = modifier.widthIn(max = AuraDockDockLayout.lineMaxWidth), horizontalAlignment = Alignment.CenterHorizontally) {
        lines.meeting?.let { MeetingDockLine(it, dimmed) }
        lines.intention?.let { IntentionDockLine(it, dimmed, actions.onToggleIntention) }
    }
}

/** The morning intention: a circle and the one thing, quiet. Ticking it strikes it through; it stays, dimmed. */
@Composable
fun IntentionDockLine(line: IntentionLine, dimmed: Boolean, onToggle: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.heightIn(min = AuraDockDockLayout.lineRow),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AuraDockDockLayout.lineGap)
    ) {
        LineCircle(done = line.done, dimmed = dimmed, onClick = { onToggle(!line.done) })
        StrikeText(
            text = line.text,
            struck = line.done,
            style = AuraDockTypography.DockLine,
            color = if (dimmed) AuraDockColors.DockLineDim else AuraDockColors.DockLine,
            struckColor = if (dimmed) AuraDockColors.DockLineDoneDim else AuraDockColors.DockLineDone,
            maxLines = 1
        )
    }
}

/** The circle checkbox of a dock line. */
@Composable
private fun LineCircle(done: Boolean, dimmed: Boolean, onClick: () -> Unit) {
    val ring = if (dimmed) AuraDockColors.DockLineRingDim else AuraDockColors.DockLineRing
    val fill by animateColorAsState(if (done) ring else Color.Transparent, AuraDockMotion.tween(AuraDockMotion.Check), label = "lineFill")
    val tick by animateFloatAsState(if (done) 1f else 0f, AuraDockMotion.tween(AuraDockMotion.Check), label = "lineTick")
    Box(
        modifier = Modifier
            .size(AuraDockDockLayout.lineRow)
            .clickable(role = Role.Checkbox, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(AuraDockDockLayout.lineCircle)
                .clip(CircleShape)
                .background(fill)
                .border(AuraDockDockLayout.strokeRing, ring, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (tick > 0f) Icon(Icons.Rounded.Check, contentDescription = null, tint = AuraDockColors.Background.copy(alpha = tick), modifier = Modifier.size(AuraDockDockLayout.lineCircle - AuraDockSizes.line * 4))
        }
    }
}

/**
 * The next meeting: its title and "in 8 min", the time in coral. From two minutes out the whole line breathes (opacity only: no
 * movement, no sound).
 */
@Composable
fun MeetingDockLine(meeting: MeetingCountdown, dimmed: Boolean, modifier: Modifier = Modifier) {
    val pulse = rememberInfiniteTransition(label = "meetingPulse")
    val breathing by pulse.animateFloat(
        initialValue = 1f,
        targetValue = AuraDockDockLayout.MeetingPulseLow,
        animationSpec = infiniteRepeatable(
            animation = tween(AuraDockMotion.MeetingPulse / 2, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "meetingAlpha"
    )
    Row(
        modifier = modifier
            .heightIn(min = AuraDockDockLayout.lineRow)
            .graphicsLayer { alpha = if (meeting.pulsing) breathing else 1f },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AuraDockDockLayout.lineGap)
    ) {
        Text(
            text = meeting.title,
            style = AuraDockTypography.DockLine,
            color = if (dimmed) AuraDockColors.DockLineDim else AuraDockColors.DockLine,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        Text(text = "in ${meeting.minutes} min", style = AuraDockTypography.DockLine, color = AuraDockColors.DockOverdue, maxLines = 1)
    }
}
