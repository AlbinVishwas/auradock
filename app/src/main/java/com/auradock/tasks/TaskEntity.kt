package com.auradock.tasks

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One row of the `tasks` table: a simple to-do for today.
 *
 * All times are epoch milliseconds (UTC instants), the same thing `System.currentTimeMillis()` returns.
 * [dueAt] is optional and always means a time on the day the task was made ("today only": there are no future
 * dates in V1). [completedAt] is set when [done] becomes true and cleared if it is un-done.
 */
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val dueAt: Long? = null,
    val done: Boolean = false,
    val createdAt: Long,
    val completedAt: Long? = null,
    /** Not shown before this moment (epoch millis; 0 = always). Tomorrow's top task from the evening shutdown waits for midnight. */
    @ColumnInfo(defaultValue = "0") val showFrom: Long = 0
)
