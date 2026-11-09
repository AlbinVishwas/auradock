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
import android.text.format.DateFormat
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import com.auradock.intention.tomorrowDueAt
import com.auradock.tasks.MAX_TITLE_LENGTH
import com.auradock.tasks.cleanTitle
import com.auradock.ui.components.AuraTextField
import com.auradock.ui.components.GreyTextButton
import com.auradock.ui.components.PrimaryPill
import com.auradock.ui.tasks.TimeWheel
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
 *
 * After an evening session ([offerShutdown]) the card gains a second step instead of going away: "Tomorrow's top task?", which
 * stays until it is saved or skipped. Tapping the first step then moves on to it at once.
 */
@Composable
fun UndockSummaryCard(
    summary: SessionSummary,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    offerShutdown: Boolean = false,
    nowMillis: Long = summary.endedAt,
    onShutdownShown: () -> Unit = {},
    onSaveTomorrow: (title: String, dueAt: Long?) -> Unit = { _, _ -> },
    /** For previews: open on the second step. */
    startOnTomorrow: Boolean = false
) {
    // Whether to ask is decided when the card appears, so asking (which marks the evening done) cannot take the step away.
    val asking = remember(summary.endedAt) { offerShutdown }
    var step by remember(summary.endedAt) { mutableStateOf(if (startOnTomorrow) UndockStep.TOMORROW else UndockStep.SUMMARY) }
    fun next() {
        if (asking && step == UndockStep.SUMMARY) { step = UndockStep.TOMORROW; onShutdownShown() } else onDismiss()
    }
    LaunchedEffect(summary.endedAt, step) {
        if (step == UndockStep.SUMMARY) {
            delay(AuraDockMotion.UndockCard.toLong())
            next()
        }
    }
    BlurIn(modifier = modifier) {
        val shape = RoundedCornerShape(AuraDockRadii.card)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(AuraDockColors.SurfaceRaised)
                .border(AuraDockSizes.line, AuraDockColors.TileOutline, shape)
                .padding(AuraDockSpacing.mdl),
            verticalArrangement = Arrangement.spacedBy(AuraDockSpacing.md)
        ) {
            if (step == UndockStep.SUMMARY) {
                Column(
                    modifier = Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = ::next),
                    verticalArrangement = Arrangement.spacedBy(AuraDockSpacing.md)
                ) {
                    Text(text = "Focus session", style = AuraDockTypography.Caption, color = AuraDockColors.Tertiary)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AuraDockSpacing.md)) {
                        Stat(formatFocusTime(summary.durationMs), "focused", Modifier.weight(1.4f))
                        Stat(summary.tasksCompleted.toString(), tasksDoneLabel(summary.tasksCompleted), Modifier.weight(1f))
                        Stat(summary.counts.held.toString(), "held", Modifier.weight(1f))
                    }
                }
            } else {
                TomorrowStep(nowMillis = nowMillis, onSave = { title, dueAt -> onSaveTomorrow(title, dueAt); onDismiss() }, onSkip = onDismiss)
            }
        }
    }
}

private enum class UndockStep { SUMMARY, TOMORROW }

/** "Tomorrow's top task?": a text field, a time chip (no time unless chosen) and Save / Skip. Saving makes a task for tomorrow. */
@Composable
private fun TomorrowStep(nowMillis: Long, onSave: (title: String, dueAt: Long?) -> Unit, onSkip: () -> Unit) {
    val context = LocalContext.current
    val is24Hour = remember(context) { DateFormat.is24HourFormat(context) }
    var title by rememberSaveable { mutableStateOf("") }
    var hasTime by rememberSaveable { mutableStateOf(false) }
    var hour by rememberSaveable { mutableIntStateOf(TOMORROW_DEFAULT_HOUR) }
    var minute by rememberSaveable { mutableIntStateOf(0) }
    var picking by rememberSaveable { mutableStateOf(false) }
    val canSave = cleanTitle(title) != null
    // Read the fields when it is called, not when it was made: a tap must save what is typed now.
    val save = {
        cleanTitle(title)?.let { onSave(it, if (hasTime) tomorrowDueAt(nowMillis, hour, minute) else null) }
        Unit
    }

    Text(text = "Tomorrow's top task?", style = AuraDockTypography.SheetTitle, color = AuraDockColors.OnBackground)
    AuraTextField(value = title, onValueChange = { if (it.length <= MAX_TITLE_LENGTH) title = it }, hint = "One task", onDone = { save() })
    TimeChip(
        label = if (hasTime) formatChipTime(hour, minute, is24Hour) else "No time",
        active = hasTime,
        onClick = { if (!hasTime) hasTime = true; picking = !picking },
        onClear = if (hasTime) ({ hasTime = false; picking = false }) else null
    )
    if (picking && hasTime) TimeWheel(hour = hour, minute = minute, is24Hour = is24Hour, onChange = { h, m -> hour = h; minute = m })
    PrimaryPill(text = "Save", onClick = { save() }, enabled = canSave)
    GreyTextButton(text = "Skip", onClick = onSkip, modifier = Modifier.fillMaxWidth())
}

private const val TOMORROW_DEFAULT_HOUR = 9

/** The time chip: "No time" until tapped, then the time (tap again to change it, the cross to take it off). */
@Composable
private fun TimeChip(label: String, active: Boolean, onClick: () -> Unit, onClear: (() -> Unit)?) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AuraDockSpacing.sm)) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(AuraDockRadii.pill))
                .background(AuraDockColors.Field)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = AuraDockSpacing.md, vertical = AuraDockSpacing.smd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AuraDockSpacing.sm)
        ) {
            Icon(Icons.Outlined.Schedule, contentDescription = null, tint = AuraDockColors.OnSurfaceVariant, modifier = Modifier.size(AuraDockSizes.iconSmall))
            Text(text = label, style = AuraDockTypography.Secondary, color = if (active) AuraDockColors.OnBackground else AuraDockColors.OnSurfaceVariant)
        }
        if (onClear != null) {
            Text(text = "Remove", style = AuraDockTypography.Meta, color = AuraDockColors.OnSurfaceVariant, modifier = Modifier.clickable(role = Role.Button, onClick = onClear).padding(AuraDockSpacing.sm))
        }
    }
}

/** "9:00 AM" or "09:00", the way the phone writes the time. */
private fun formatChipTime(hour: Int, minute: Int, is24Hour: Boolean): String =
    if (is24Hour) "%02d:%02d".format(hour, minute)
    else "%d:%02d %s".format((hour % 12).let { if (it == 0) 12 else it }, minute, if (hour >= 12) "PM" else "AM")

@Composable
private fun Stat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = value, style = AuraDockTypography.SummaryValue, color = AuraDockColors.OnBackground, maxLines = 1)
        Text(text = label, style = AuraDockTypography.RowCaption, color = AuraDockColors.OnSurfaceVariant, modifier = Modifier.padding(top = AuraDockSpacing.xxs))
    }
}
