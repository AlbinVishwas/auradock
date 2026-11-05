package com.auradock.ui.tasks

import androidx.compose.runtime.Composable
import com.auradock.ui.preview.PreviewComponent
import com.auradock.ui.preview.PreviewData
import com.auradock.ui.preview.PreviewPortrait
import com.auradock.ui.theme.AuraDockTheme

private fun card(tasks: List<com.auradock.tasks.TaskEntity>?) = @Composable {
    AuraDockTheme {
        TaskCard(
            tasks = tasks,
            timeMillis = PreviewData.now,
            formatTime = { "9:41 AM" },
            onAdd = {}, onEdit = {}, onToggle = {}, onDelete = {}
        )
    }
}

/** An overdue task (amber), a timed one, an open one and a finished one. */
@PreviewComponent
@Composable
private fun TaskCardFull() = card(PreviewData.tasks)()

@PreviewComponent
@Composable
private fun TaskCardEmpty() = card(emptyList())()

/** Still loading: no body, so it never claims "nothing planned" too early. */
@PreviewComponent
@Composable
private fun TaskCardLoading() = card(null)()

/** The add sheet. (Shown as a bottom sheet: open the preview in interactive mode.) */
@PreviewPortrait
@Composable
private fun TaskSheetNew() = AuraDockTheme {
    TaskSheet(task = null, nowMillis = PreviewData.now, exactAlarmsAllowed = true, onAllowExactAlarms = {}, onSave = { _, _ -> }, onDismiss = {})
}

@PreviewPortrait
@Composable
private fun TaskSheetEditWithTimeNoExactAlarms() = AuraDockTheme {
    TaskSheet(
        task = PreviewData.tasks[1], nowMillis = PreviewData.now, exactAlarmsAllowed = false,
        onAllowExactAlarms = {}, onSave = { _, _ -> }, onDismiss = {}
    )
}
