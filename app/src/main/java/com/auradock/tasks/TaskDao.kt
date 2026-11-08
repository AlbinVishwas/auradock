package com.auradock.tasks

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** Room turns these annotated functions into real SQL code at build time. */
@Dao
interface TaskDao {

    /** Every task. [observeAll] re-emits whenever the table changes; filtering by day happens in [TaskRules]. */
    @Query("SELECT * FROM tasks")
    fun observeAll(): Flow<List<TaskEntity>>

    @Insert
    suspend fun insert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun get(id: Long): TaskEntity?

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun delete(id: Long)

    /** Housekeeping: done tasks from before today are hidden anyway, so remove them. */
    @Query("DELETE FROM tasks WHERE done = 1 AND completedAt < :startOfToday")
    suspend fun deleteDoneBefore(startOfToday: Long)

    /** How many tasks were ticked off between [from] and [to] (and are still done): the focus session summary. */
    @Query("SELECT COUNT(*) FROM tasks WHERE done = 1 AND completedAt >= :from AND completedAt <= :to")
    suspend fun countCompletedBetween(from: Long, to: Long): Int

    /** Not done, has a due time still in the future: the ones that need an alarm. */
    @Query("SELECT * FROM tasks WHERE done = 0 AND dueAt IS NOT NULL AND dueAt > :now")
    suspend fun pendingWithDue(now: Long): List<TaskEntity>
}
