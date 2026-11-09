package com.auradock.ui.tasks

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextOverflow
import com.auradock.intention.IntentionLine
import com.auradock.tasks.TaskEntity
import com.auradock.ui.components.StrikeText
import com.auradock.tasks.isOverdue
import com.auradock.ui.theme.AuraDockColors
import com.auradock.ui.theme.AuraDockMotion
import com.auradock.ui.theme.AuraDockRadii
import com.auradock.ui.theme.AuraDockSizes
import com.auradock.ui.theme.AuraDockSpacing
import com.auradock.ui.theme.AuraDockTypography

/**
 * The dashboard's "Today" card (handoff Main / Empty): a title with how many are left, a row per task, and an "Add" row at the
 * end. Tick off (tap the circle), edit (tap the title), delete (swipe).
 *
 * [timeMillis] is the shared minute clock, so a task turns "overdue" by itself when its time arrives.
 */
@Composable
fun TaskCard(
    /** Null while the first load is still running: the card shows no body rather than a wrong "nothing planned". */
    tasks: List<TaskEntity>?,
    timeMillis: Long,
    formatTime: (Long) -> String,
    onAdd: () -> Unit,
    onEdit: (TaskEntity) -> Unit,
    onToggle: (TaskEntity) -> Unit,
    onDelete: (TaskEntity) -> Unit,
    modifier: Modifier = Modifier,
    /** The morning intention: the first row of the card, with its own circle. */
    intention: IntentionLine? = null,
    onToggleIntention: (Boolean) -> Unit = {}
) {
    val left = tasks?.count { !it.done } ?: 0
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(AuraDockRadii.card))
            .background(AuraDockColors.Surface)
            .padding(start = AuraDockSpacing.mdl, end = AuraDockSpacing.mdl, top = AuraDockSpacing.mdl, bottom = AuraDockSpacing.sm)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Text(text = "Today", style = AuraDockTypography.Section, color = AuraDockColors.OnBackground)
            if (tasks != null && tasks.isNotEmpty()) Text(text = "$left left", style = AuraDockTypography.Meta, color = AuraDockColors.Tertiary)
        }
        if (intention != null) IntentionRow(intention, onToggleIntention, divider = false)
        if (tasks != null && tasks.isEmpty()) {
            Text(
                text = "Nothing planned.\nEnjoy the quiet.",
                style = AuraDockTypography.EmptyState,
                color = AuraDockColors.OnSurfaceVariant,
                modifier = Modifier.padding(top = AuraDockSpacing.xl - AuraDockSpacing.xs, bottom = AuraDockSpacing.lg)
            )
        } else if (tasks != null) {
            Column(modifier = Modifier.padding(top = AuraDockSpacing.xs)) {
                // Done tasks sink to the bottom; each group keeps its order.
                tasks.sortedBy { it.done }.forEachIndexed { index, task ->
                    key(task.id) {
                        TaskRow(
                            task = task,
                            overdue = isOverdue(task, timeMillis),
                            dueLabel = task.dueAt?.let(formatTime),
                            divider = index > 0,
                            onEdit = { onEdit(task) },
                            onToggle = { onToggle(task) },
                            onDelete = { onDelete(task) }
                        )
                    }
                }
            }
        }
        AddRow(onAdd, divider = tasks != null)
    }
}

/** The morning intention at the top of the card: "Today's one thing" over the line, with the same circle and strike as a task. */
@Composable
private fun IntentionRow(intention: IntentionLine, onToggle: (Boolean) -> Unit, divider: Boolean) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = AuraDockSpacing.sm)) {
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
            CheckCircle(done = intention.done, highlight = false, onClick = { onToggle(!intention.done) })
            Column(modifier = Modifier.weight(1f).padding(vertical = AuraDockSpacing.md - AuraDockSpacing.xxs)) {
                Text(text = "The one thing", style = AuraDockTypography.Caption, color = AuraDockColors.Tertiary)
                StrikeText(text = intention.text, struck = intention.done, style = AuraDockTypography.Body, color = AuraDockColors.OnBackground)
            }
        }
        if (divider) Box(Modifier.fillMaxWidth().size(width = AuraDockSizes.none, height = AuraDockSizes.line).background(AuraDockColors.RowDivider))
    }
}

/** "+ Add" at the end of the list. */
@Composable
private fun AddRow(onAdd: () -> Unit, divider: Boolean) {
    Column(modifier = Modifier.fillMaxWidth().clickable(onClick = onAdd)) {
        if (divider) Box(Modifier.fillMaxWidth().size(width = AuraDockSizes.none, height = AuraDockSizes.line).background(AuraDockColors.RowDivider))
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = AuraDockSpacing.md - AuraDockSpacing.xxs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AuraDockSpacing.md - AuraDockSpacing.xxs)
        ) {
            Box(Modifier.size(AuraDockSizes.icon), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Add, contentDescription = null, tint = AuraDockColors.OnSurfaceVariant, modifier = Modifier.size(AuraDockSizes.tileIcon))
            }
            Text(text = "Add", style = AuraDockTypography.Body, color = AuraDockColors.OnSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskRow(
    task: TaskEntity,
    overdue: Boolean,
    dueLabel: String?,
    divider: Boolean,
    onEdit: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            // Only drawn while the row is being swiped, so it never peeks out from behind a resting row.
            if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                Box(
                    modifier = Modifier.fillMaxSize().background(AuraDockColors.Inactive).padding(end = AuraDockSpacing.mdl),
                    contentAlignment = Alignment.CenterEnd
                ) { Text(text = "Delete", color = AuraDockColors.OnBackground, style = AuraDockTypography.Caption) }
            }
        }
    ) {
        val highlight = overdue && !task.done
        Column(modifier = Modifier.fillMaxWidth().background(AuraDockColors.Surface)) {
            if (divider) Box(Modifier.fillMaxWidth().size(width = AuraDockSizes.none, height = AuraDockSizes.line).background(AuraDockColors.RowDivider))
            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
                CheckCircle(done = task.done, highlight = highlight, onClick = onToggle)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onEdit)
                        .padding(vertical = AuraDockSpacing.md - AuraDockSpacing.xxs)
                ) {
                    StrikeText(text = task.title, struck = task.done, style = AuraDockTypography.Body, color = AuraDockColors.OnBackground)
                    if (highlight && dueLabel != null) {
                        Text(
                            text = "Overdue · $dueLabel",
                            style = AuraDockTypography.Caption,
                            color = AuraDockColors.Overdue,
                            modifier = Modifier.padding(top = AuraDockSpacing.xxs + AuraDockSpacing.hairline)
                        )
                    }
                }
                if (!highlight && dueLabel != null && !task.done) {
                    Text(text = dueLabel, style = AuraDockTypography.Meta, color = AuraDockColors.OnSurfaceVariant)
                }
            }
        }
    }
}

/** A round tick box: 24 dp, in a tap area that is the full height of the row. The fill and the tick fade in. */
@Composable
private fun CheckCircle(done: Boolean, highlight: Boolean, onClick: () -> Unit) {
    val fill by animateColorAsState(
        targetValue = if (done) AuraDockColors.DoneCircle else androidx.compose.ui.graphics.Color.Transparent,
        animationSpec = AuraDockMotion.tween(AuraDockMotion.Check),
        label = "checkFill"
    )
    val tick by animateFloatAsState(if (done) 1f else 0f, AuraDockMotion.tween(AuraDockMotion.Check), label = "checkTick")
    val ring = when {
        done -> AuraDockColors.DoneCircle
        highlight -> AuraDockColors.Overdue
        else -> AuraDockColors.Outline
    }
    Box(
        modifier = Modifier.width(AuraDockSizes.taskCircleTap).fillMaxHeight().clickable(onClick = onClick),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .size(AuraDockSizes.icon)
                .clip(CircleShape)
                .background(fill)
                .border(AuraDockSizes.lineThick, ring, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (tick > 0f) Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                tint = AuraDockColors.OnBackground.copy(alpha = tick),
                modifier = Modifier.size(AuraDockSizes.allowedCheckIcon)
            )
        }
    }
}
