package com.auradock.tasks.reminders

import com.auradock.tasks.TaskEntity

/** What to do about a task's reminder. A plain value so the decision can be unit-tested. */
sealed interface AlarmPlan {
    /** No alarm: the task is finished, has no time, or its time has already passed. Cancel any existing one. */
    data object None : AlarmPlan

    /** An exact alarm: fires at [at] to the second. Needs the "exact alarms" permission on Android 12+. */
    data class Exact(val at: Long) : AlarmPlan

    /** Android picks a moment near [at] to save battery, so it can be a few minutes late. Needs no permission. */
    data class Inexact(val at: Long) : AlarmPlan
}

fun alarmPlan(task: TaskEntity, nowMillis: Long, canScheduleExact: Boolean): AlarmPlan {
    val due = task.dueAt
    return when {
        task.done || due == null || due <= nowMillis -> AlarmPlan.None
        canScheduleExact -> AlarmPlan.Exact(due)
        else -> AlarmPlan.Inexact(due)
    }
}
