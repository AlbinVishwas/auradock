package com.auradock.sessions

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * One row of the `focus_sessions` table. Times are epoch milliseconds. [source] is a [SessionSource] name. Only counts are
 * kept about notifications, never what they said.
 */
@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startedAt: Long,
    val endedAt: Long,
    val source: String,
    val notificationsHeld: Int,
    val vipThrough: Int,
    val tasksCompleted: Int
) {
    val durationMs: Long get() = endedAt - startedAt
}

/**
 * The session that is open right now, saved every minute (one row, id 1). If the app is killed while docked, the next launch
 * turns this into a finished session ending at [lastAt] (see [recoveredSession]).
 */
@Entity(tableName = "session_checkpoint")
data class SessionCheckpointEntity(
    @PrimaryKey val id: Int = 1,
    val startedAt: Long,
    val lastAt: Long,
    val source: String,
    val notificationsHeld: Int,
    val vipThrough: Int
)

@Dao
interface FocusSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCheckpoint(checkpoint: SessionCheckpointEntity)

    @Query("SELECT * FROM session_checkpoint WHERE id = 1")
    suspend fun checkpoint(): SessionCheckpointEntity?

    @Query("DELETE FROM session_checkpoint")
    suspend fun clearCheckpoint()

    @Insert
    suspend fun insert(session: FocusSessionEntity): Long

    /** Sessions that ended at or after [since], oldest first: what the weekly line is drawn from. */
    @Query("SELECT * FROM focus_sessions WHERE endedAt >= :since ORDER BY startedAt")
    fun observeSince(since: Long): Flow<List<FocusSessionEntity>>
}
