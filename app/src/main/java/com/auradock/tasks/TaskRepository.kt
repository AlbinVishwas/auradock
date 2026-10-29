package com.auradock.tasks

import com.auradock.time.MinuteClock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The one place tasks are read and changed. Screens never touch the database directly.
 */
@Singleton
class TaskRepository @Inject constructor(
    private val dao: TaskDao,
    private val clock: MinuteClock
) {
    /**
     * Today's list. It is a Flow: it emits the current list straight away and again after every change to
     * the table, so the screen updates by itself. The day boundary comes from the minute clock: when midnight
     * passes, "start of today" changes and finished tasks from yesterday drop out without any user action.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val todayTasks: Flow<List<TaskEntity>> = clock.minutes
        .map { startOfToday(it) }
        .distinctUntilChanged()
        .flatMapLatest { start -> dao.observeAll().map { visibleTasks(it, start) } }

    suspend fun get(id: Long): TaskEntity? = dao.get(id)

    /** Returns the new task's id, or null if [title] is blank. */
    suspend fun add(title: String, dueAt: Long?): Long? {
        val clean = cleanTitle(title) ?: return null
        return dao.insert(TaskEntity(title = clean, dueAt = dueAt, createdAt = clock.currentMillis()))
    }

    suspend fun setDone(id: Long, done: Boolean) {
        val task = dao.get(id) ?: return
        dao.update(task.copy(done = done, completedAt = if (done) clock.currentMillis() else null))
    }

    /** Changes the title and due time. Returns false if the task is gone or [title] is blank. */
    suspend fun edit(id: Long, title: String, dueAt: Long?): Boolean {
        val clean = cleanTitle(title) ?: return false
        val task = dao.get(id) ?: return false
        dao.update(task.copy(title = clean, dueAt = dueAt))
        return true
    }

    suspend fun delete(id: Long) = dao.delete(id)

    /** Removes finished tasks from before today (they are already hidden). */
    suspend fun purgeOldDone() = dao.deleteDoneBefore(startOfToday(clock.currentMillis()))
}
