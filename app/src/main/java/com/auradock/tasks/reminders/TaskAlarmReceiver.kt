package com.auradock.tasks.reminders

import android.content.Context
import android.content.Intent
import android.util.Log
import com.auradock.tasks.TaskDao
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Woken by AlarmManager when a task is due. It looks the task up (so a task finished or deleted in the
 * meantime shows nothing) and posts the notification.
 */
@AndroidEntryPoint
class TaskAlarmReceiver : HiltBroadcastReceiver() {
    @Inject lateinit var dao: TaskDao
    @Inject lateinit var notifier: TaskNotifier

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent) // Hilt fills the @Inject fields here
        val taskId = intent.getLongExtra(TaskAlarmScheduler.EXTRA_TASK_ID, -1L)
        if (taskId < 0) return

        // A receiver must finish in a few seconds; database work is async, so ask the system to wait.
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val task = dao.get(taskId)
                if (task != null && !task.done) {
                    Log.d("TaskAlarms", "task $taskId due: showing notification")
                    notifier.show(task)
                } else {
                    Log.d("TaskAlarms", "task $taskId due but gone or done: nothing to show")
                }
            } finally {
                pending.finish()
            }
        }
    }
}
