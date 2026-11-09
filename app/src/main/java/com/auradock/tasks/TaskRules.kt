package com.auradock.tasks

import com.auradock.data.todayRange
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

/**
 * The task rules as plain functions, so they can be tested without a database or a phone.
 *
 *  1. Unfinished tasks carry over from day to day until they are done.
 *  2. A finished task stays visible for the rest of the day it was finished, and is hidden from midnight.
 */

/** Midnight at the start of the local day containing [nowMillis]. */
fun startOfToday(nowMillis: Long, zone: ZoneId = ZoneId.systemDefault()): Long = todayRange(nowMillis, zone).first

fun isVisibleToday(task: TaskEntity, startOfToday: Long, endOfToday: Long = Long.MAX_VALUE): Boolean =
    task.showFrom < endOfToday && (!task.done || (task.completedAt ?: 0L) >= startOfToday)

/** Not done, has a due time, and that time has arrived. */
fun isOverdue(task: TaskEntity, nowMillis: Long): Boolean =
    !task.done && task.dueAt != null && task.dueAt <= nowMillis

/**
 * What the dashboard shows: unfinished tasks first, the ones with a due time in time order (those without a
 * time after them, oldest first); finished tasks at the bottom in the order they were finished.
 */
fun visibleTasks(all: List<TaskEntity>, startOfToday: Long, endOfToday: Long = Long.MAX_VALUE): List<TaskEntity> =
    all.filter { isVisibleToday(it, startOfToday, endOfToday) }
        .sortedWith(
            compareBy<TaskEntity> { it.done }
                .thenBy { if (it.done) 0L else (it.dueAt ?: Long.MAX_VALUE) }
                .thenBy { if (it.done) (it.completedAt ?: 0L) else it.createdAt }
                .thenBy { it.id }
        )

/**
 * Turns a time-picker choice (hour and minute) into a moment *today*. Today only: there is no date picker, so a
 * time that has already passed still means earlier today (the task is simply overdue).
 */
fun dueTimeToday(hour: Int, minute: Int, nowMillis: Long, zone: ZoneId = ZoneId.systemDefault()): Long {
    val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
    return today.atTime(LocalTime.of(hour, minute)).atZone(zone).toInstant().toEpochMilli()
}

/** Trimmed title, or null if there is nothing worth saving. */
fun cleanTitle(raw: String): String? = raw.trim().takeIf { it.isNotEmpty() }?.take(MAX_TITLE_LENGTH)

const val MAX_TITLE_LENGTH = 120

/**
 * Unfinished tasks in the order the dock shows them: overdue ones first (earliest time first), then the rest by
 * earliest due time, and tasks without a time last, oldest first.
 */
fun priorityOrder(tasks: List<TaskEntity>, nowMillis: Long): List<TaskEntity> =
    tasks.filter { !it.done }
        .sortedWith(
            compareBy<TaskEntity> { !isOverdue(it, nowMillis) }
                .thenBy { it.dueAt ?: Long.MAX_VALUE }
                .thenBy { it.createdAt }
                .thenBy { it.id }
        )

/** The one task to show when there is room for just one. */
fun topTask(tasks: List<TaskEntity>, nowMillis: Long): TaskEntity? = priorityOrder(tasks, nowMillis).firstOrNull()

/**
 * The order a list shows: tasks still to do first, done ones at the bottom, each group in its own order. A task that has only just been
 * ticked is in [settling] and keeps its place for a moment (it moves down after the tick has been seen).
 */
fun displayOrder(tasks: List<TaskEntity>, settling: Set<Long> = emptySet()): List<TaskEntity> =
    tasks.sortedBy { it.done && it.id !in settling }

/** The tasks ticked since [before]: the ones that should settle before they sink. */
fun newlyDone(before: Set<Long>, doneNow: Set<Long>): Set<Long> = doneNow - before
