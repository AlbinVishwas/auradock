package com.auradock.tasks.reminders

import com.auradock.monitoring.Monitoring
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.auradock.permissions.canScheduleExactAlarms
import com.auradock.tasks.TaskDao
import com.auradock.tasks.TaskEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sets and cancels the alarm that fires when a task is due. Android's AlarmManager keeps the alarms (not
 * AuraDock), and wakes our [TaskAlarmReceiver] at the right time, even if the app is not running.
 */
@Singleton
class TaskAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: TaskDao
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    /** Sets (or replaces) the alarm for [task], or cancels it if the task no longer needs one. */
    fun schedule(task: TaskEntity, nowMillis: Long = System.currentTimeMillis()) {
        try {
            scheduleNow(task, nowMillis)
        } catch (e: Exception) {
            // Whatever the reason (a revoked permission, the system's alarm limit, a missing service), the task has no alarm: report it.
            Monitoring.nonFatal("alarm.schedule_failed", "reason" to e.javaClass.simpleName)
            Log.w(TAG, "alarm for task ${task.id} could not be set", e)
        }
    }

    private fun scheduleNow(task: TaskEntity, nowMillis: Long) {
        when (val plan = alarmPlan(task, nowMillis, canScheduleExactAlarms(context))) {
            AlarmPlan.None -> cancel(task.id)
            is AlarmPlan.Exact -> {
                try {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, plan.at, pendingIntent(task.id))
                    Monitoring.breadcrumb("alarm", "scheduled", "kind" to "exact")
                } catch (e: SecurityException) {
                    Monitoring.nonFatal("alarm.exact_refused", "fallback" to "inexact")
                    Monitoring.breadcrumb("alarm", "scheduled", "kind" to "inexact")
                    // The permission was withdrawn between the check and the call: fall back.
                    Log.w(TAG, "exact alarm refused, using an inexact one", e)
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, plan.at, pendingIntent(task.id))
                }
            }
            is AlarmPlan.Inexact -> {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, plan.at, pendingIntent(task.id))
                Monitoring.breadcrumb("alarm", "scheduled", "kind" to "inexact")
            }
        }
        Log.d(TAG, "task ${task.id}: ${alarmPlan(task, nowMillis, canScheduleExactAlarms(context))}")
    }

    fun cancel(taskId: Long) {
        // FLAG_NO_CREATE: only find an alarm we already set; do not make a new one just to cancel it.
        val existing = PendingIntent.getBroadcast(
            context, taskId.toInt(), alarmIntent(taskId), PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (existing != null) {
            alarmManager.cancel(existing)
            existing.cancel()
            Log.d(TAG, "task $taskId: alarm cancelled")
        }
    }

    /** Alarms are lost when the phone restarts: set them all again from the database. */
    suspend fun rescheduleAll() {
        val now = System.currentTimeMillis()
        val pending = dao.pendingWithDue(now)
        pending.forEach { schedule(it, now) }
        Log.d(TAG, "rescheduled ${pending.size} alarm(s)")
    }

    private fun pendingIntent(taskId: Long): PendingIntent = PendingIntent.getBroadcast(
        context, taskId.toInt(), alarmIntent(taskId), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun alarmIntent(taskId: Long) = Intent(context, TaskAlarmReceiver::class.java)
        .setAction(ACTION_TASK_DUE)
        .putExtra(EXTRA_TASK_ID, taskId)

    companion object {
        const val ACTION_TASK_DUE = "com.auradock.action.TASK_DUE"
        const val EXTRA_TASK_ID = "task_id"
        private const val TAG = "TaskAlarms"
    }
}
