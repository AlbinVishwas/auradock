package com.auradock.ui.tasks

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.auradock.tasks.TaskEntity
import com.auradock.tasks.isOverdue
import com.auradock.ui.components.QuickActionButton
import com.auradock.ui.theme.AuraDockColors
import com.auradock.ui.theme.AuraDockTypography
import com.auradock.ui.theme.LocalAuraDockSpacing

private val Overdue = Color(0xFFFFB300)
private val DeleteRed = Color(0xFF8E2A2A)

/**
 * The dashboard's "Today" task list: add, tick off (tap the circle), edit (tap the title), delete (swipe).
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
    modifier: Modifier = Modifier
) {
    val spacing = LocalAuraDockSpacing.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = AuraDockColors.Surface,
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.padding(spacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today",
                    style = AuraDockTypography.WidgetPrimary,
                    color = AuraDockColors.OnBackground
                )
                QuickActionButton(text = "+ Add", onClick = onAdd, isActive = true)
            }
            Spacer(Modifier.height(spacing.sm))

            if (tasks == null) {
                Spacer(Modifier.height(spacing.sm))
            } else if (tasks.isEmpty()) {
                Text(
                    text = "Nothing planned. Enjoy the quiet.",
                    style = AuraDockTypography.WidgetLabel,
                    color = AuraDockColors.OnSurfaceVariant,
                    modifier = Modifier.padding(vertical = spacing.sm)
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                    tasks.forEach { task ->
                        androidx.compose.runtime.key(task.id) {
                            TaskRow(
                                task = task,
                                overdue = isOverdue(task, timeMillis),
                                dueLabel = task.dueAt?.let(formatTime),
                                onEdit = { onEdit(task) },
                                onToggle = { onToggle(task) },
                                onDelete = { onDelete(task) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskRow(
    task: TaskEntity,
    overdue: Boolean,
    dueLabel: String?,
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
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DeleteRed)
                        .padding(end = 20.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Text(text = "Delete", color = Color.White, style = AuraDockTypography.WidgetLabel)
                }
            }
        }
    ) {
        val highlight = overdue && !task.done
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(AuraDockColors.Surface) // opaque base so nothing shows through when swiping
                .background(if (highlight) Overdue.copy(alpha = 0.14f) else Color.Transparent)
                .padding(start = 2.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CheckCircle(done = task.done, highlight = highlight, onClick = onToggle)
            Spacer(Modifier.width(4.dp))
            StrikeText(
                text = task.title,
                struck = task.done,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .clickable(onClick = onEdit)
                    .padding(vertical = 12.dp)
            )
            Spacer(Modifier.weight(0.01f))
            if (dueLabel != null) {
                Spacer(Modifier.width(12.dp))
                Text(
                    text = dueLabel,
                    style = AuraDockTypography.Caption,
                    color = if (highlight) Overdue else AuraDockColors.OnSurfaceVariant
                )
            }
        }
    }
}

/** A round tick box. Tap target is 44dp; the circle itself is 24dp. The fill and the tick fade in. */
@Composable
private fun CheckCircle(done: Boolean, highlight: Boolean, onClick: () -> Unit) {
    val fill by animateColorAsState(
        targetValue = if (done) AuraDockColors.AccentBlue else Color.Transparent,
        animationSpec = tween(220),
        label = "checkFill"
    )
    val tick by animateFloatAsState(if (done) 1f else 0f, tween(220), label = "checkTick")
    val ring = when {
        done -> AuraDockColors.AccentBlue
        highlight -> Overdue
        else -> AuraDockColors.OnSurfaceVariant
    }
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(fill)
                .border(2.dp, ring, CircleShape)
                .drawWithContent {
                    drawContent()
                    if (tick > 0f) {
                        val w = size.width
                        val h = size.height
                        val stroke = 2.dp.toPx()
                        val a = Offset(w * 0.28f, h * 0.52f)
                        val b = Offset(w * 0.44f, h * 0.68f)
                        val c = Offset(w * 0.74f, h * 0.34f)
                        val color = Color.White.copy(alpha = tick)
                        drawLine(color, a, b, stroke, StrokeCap.Round)
                        drawLine(color, b, c, stroke, StrokeCap.Round)
                    }
                }
        )
    }
}

/** Task title; when [struck] a line draws across it left to right and the text fades a little. */
@Composable
private fun StrikeText(text: String, struck: Boolean, modifier: Modifier = Modifier) {
    val progress by animateFloatAsState(if (struck) 1f else 0f, tween(280), label = "strike")
    val alpha by animateFloatAsState(if (struck) 0.5f else 1f, tween(280), label = "strikeFade")
    Text(
        text = text,
        style = AuraDockTypography.WidgetLabel,
        color = Color.White.copy(alpha = alpha),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.drawWithContent {
            drawContent()
            if (progress > 0f) {
                val y = size.height / 2f
                drawLine(
                    color = Color.White.copy(alpha = 0.55f),
                    start = Offset(0f, y),
                    end = Offset(size.width * progress, y),
                    strokeWidth = 1.5.dp.toPx()
                )
            }
        }
    )
}
