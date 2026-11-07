package com.auradock.ui.tasks

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import com.auradock.tasks.MAX_TITLE_LENGTH
import com.auradock.tasks.TaskEntity
import com.auradock.tasks.cleanTitle
import com.auradock.tasks.dueTimeToday
import com.auradock.ui.components.AuraSheet
import com.auradock.ui.components.GreyTextButton
import com.auradock.ui.components.PrimaryPill
import com.auradock.ui.components.QuietPill
import com.auradock.ui.theme.AuraDockColors
import com.auradock.ui.theme.AuraDockRadii
import com.auradock.ui.theme.AuraDockSizes
import com.auradock.ui.theme.AuraDockSpacing
import com.auradock.ui.theme.AuraDockTypography
import java.time.Instant
import java.time.ZoneId

/**
 * The sheet to add a task, or edit [task] when it is not null: a title and, optionally, a time today (handoff Task-Add and
 * Task-Edit). [onSave] gets the cleaned title and the due time as epoch millis (or null for "no time"). [onDelete] is shown
 * as "Delete" under Save when editing.
 */
@Composable
fun TaskSheet(
    task: TaskEntity?,
    nowMillis: Long,
    exactAlarmsAllowed: Boolean,
    onAllowExactAlarms: () -> Unit,
    onSave: (title: String, dueAt: Long?) -> Unit,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val zone = remember { ZoneId.systemDefault() }
    val is24Hour = remember(context) { DateFormat.is24HourFormat(context) }

    var title by rememberSaveable { mutableStateOf(task?.title.orEmpty()) }
    var hasTime by rememberSaveable { mutableStateOf(task?.dueAt != null) }
    val startAt = remember { Instant.ofEpochMilli(task?.dueAt ?: nowMillis).atZone(zone) }
    var hour by rememberSaveable { mutableIntStateOf(startAt.hour) }
    var minute by rememberSaveable { mutableIntStateOf(startAt.minute) }
    val focus = remember { FocusRequester() }
    // A new task opens the keyboard on its title; editing one does not (the time wheel needs the room). The field can be gone again (for example the screen turned) before this runs, and requesting focus then throws.
    LaunchedEffect(Unit) { if (task == null) runCatching { focus.requestFocus() } }

    val canSave = cleanTitle(title) != null
    fun save() {
        val clean = cleanTitle(title) ?: return
        onSave(clean, if (hasTime) dueTimeToday(hour, minute, nowMillis, zone) else null)
    }

    AuraSheet(onDismiss = onDismiss) {
        Text(
            text = if (task == null) "New task" else "Edit task",
            style = AuraDockTypography.SheetTitle,
            color = AuraDockColors.OnBackground,
            modifier = Modifier.padding(top = AuraDockSpacing.xs)
        )
        Column(verticalArrangement = Arrangement.spacedBy(AuraDockSpacing.sm)) {
            Text(text = "Title", style = AuraDockTypography.FieldLabel, color = AuraDockColors.OnSurfaceVariant)
            var focused by remember { mutableStateOf(false) }
            val shape = RoundedCornerShape(AuraDockRadii.field)
            BasicTextField(
                value = title,
                onValueChange = { if (it.length <= MAX_TITLE_LENGTH) title = it },
                singleLine = true,
                textStyle = AuraDockTypography.FieldText.copy(color = AuraDockColors.OnBackground),
                cursorBrush = SolidColor(AuraDockColors.OnBackground),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (canSave) save() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AuraDockSizes.field)
                    .clip(shape)
                    .background(AuraDockColors.Field)
                    .border(AuraDockSizes.lineThick, if (focused) AuraDockColors.OnBackground else androidx.compose.ui.graphics.Color.Transparent, shape)
                    .onFocusChanged { focused = it.isFocused }
                    .focusRequester(focus),
                decorationBox = { inner -> Box(Modifier.padding(horizontal = AuraDockSpacing.md), contentAlignment = Alignment.CenterStart) { inner() } }
            )
        }

        if (!hasTime) {
            val shape = RoundedCornerShape(AuraDockRadii.field)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(AuraDockSizes.field)
                    .clip(shape)
                    .background(AuraDockColors.Field)
                    .clickable(role = Role.Button) { hasTime = true }
                    .padding(horizontal = AuraDockSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AuraDockSpacing.smd)
            ) {
                Icon(Icons.Outlined.Schedule, contentDescription = null, tint = AuraDockColors.OnBackground, modifier = Modifier.size(AuraDockSizes.factIcon))
                Text(text = "Time", style = AuraDockTypography.Body, color = AuraDockColors.OnBackground, modifier = Modifier.weight(1f))
                Text(text = "Optional", style = AuraDockTypography.Secondary, color = AuraDockColors.OnSurfaceVariant)
                Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = AuraDockColors.Tertiary, modifier = Modifier.size(AuraDockSizes.rowChevron))
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(AuraDockSpacing.sm)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Time", style = AuraDockTypography.FieldLabel, color = AuraDockColors.OnSurfaceVariant)
                    Text(
                        text = "Remove time",
                        style = AuraDockTypography.Meta,
                        color = AuraDockColors.OnSurfaceVariant,
                        modifier = Modifier.clickable(role = Role.Button) { hasTime = false }.padding(vertical = AuraDockSpacing.sm - AuraDockSpacing.xxs)
                    )
                }
                TimeWheel(hour = hour, minute = minute, is24Hour = is24Hour, onChange = { h, m -> hour = h; minute = m })
                if (!exactAlarmsAllowed) {
                    // Without the exact-alarm permission Android may deliver the reminder a few minutes late.
                    Text(
                        text = "Reminders may arrive a few minutes late. Allow exact alarms for on-the-minute reminders.",
                        style = AuraDockTypography.Caption,
                        color = AuraDockColors.OnSurfaceVariant
                    )
                    QuietPill(text = "Allow exact alarms", onClick = onAllowExactAlarms)
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(AuraDockSpacing.xs + AuraDockSpacing.xxs), modifier = Modifier.padding(top = AuraDockSpacing.xs)) {
            PrimaryPill(text = "Save", onClick = { if (canSave) save() }, enabled = canSave)
            if (task != null && onDelete != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(AuraDockSizes.touchTarget)
                        .clickable(role = Role.Button, onClick = onDelete),
                    horizontalArrangement = Arrangement.spacedBy(AuraDockSpacing.sm, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = null, tint = AuraDockColors.OnSurfaceVariant, modifier = Modifier.size(AuraDockSizes.iconSmall))
                    Text(text = "Delete", style = AuraDockTypography.Secondary.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Medium), color = AuraDockColors.OnSurfaceVariant)
                }
            }
        }
    }
}
