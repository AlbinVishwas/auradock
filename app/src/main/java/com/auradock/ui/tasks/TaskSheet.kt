package com.auradock.ui.tasks

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.auradock.tasks.MAX_TITLE_LENGTH
import com.auradock.tasks.TaskEntity
import com.auradock.tasks.cleanTitle
import com.auradock.tasks.dueTimeToday
import com.auradock.ui.components.QuickActionButton
import com.auradock.ui.theme.AuraDockColors
import com.auradock.ui.theme.AuraDockTypography
import java.time.Instant
import java.time.ZoneId

/**
 * Bottom sheet to add a task, or edit [task] when it is not null: a title and, optionally, a time today.
 * [onSave] gets the cleaned title and the due time as epoch millis (or null for "no time").
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskSheet(
    task: TaskEntity?,
    nowMillis: Long,
    onSave: (title: String, dueAt: Long?) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val zone = remember { ZoneId.systemDefault() }

    var title by rememberSaveable { mutableStateOf(task?.title.orEmpty()) }
    var hasTime by rememberSaveable { mutableStateOf(task?.dueAt != null) }
    val startAt = remember { Instant.ofEpochMilli(task?.dueAt ?: nowMillis).atZone(zone) }
    val timeState = rememberTimePickerState(
        initialHour = startAt.hour,
        initialMinute = startAt.minute,
        is24Hour = DateFormat.is24HourFormat(context)
    )
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    val canSave = cleanTitle(title) != null
    fun save() {
        val clean = cleanTitle(title) ?: return
        onSave(clean, if (hasTime) dueTimeToday(timeState.hour, timeState.minute, nowMillis, zone) else null)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF141414),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (task == null) "New task" else "Edit task",
                style = AuraDockTypography.WidgetPrimary,
                color = Color.White
            )
            OutlinedTextField(
                value = title,
                onValueChange = { if (it.length <= MAX_TITLE_LENGTH) title = it },
                label = { Text("What needs doing?") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (canSave) save() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focus)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !hasTime,
                    onClick = { hasTime = false },
                    label = { Text("No time") }
                )
                FilterChip(
                    selected = hasTime,
                    onClick = { hasTime = true },
                    label = { Text("Remind me at…") }
                )
            }
            if (hasTime) {
                TimeInput(state = timeState)
            }
            Spacer(Modifier.height(4.dp))
            QuickActionButton(
                text = if (task == null) "Add task" else "Save",
                onClick = { if (canSave) save() },
                isActive = canSave,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
