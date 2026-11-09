package com.auradock.intention

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Where a day's intention is: offered but not answered, written, or skipped. [SUGGESTED] is last evening's "tomorrow's top task". */
enum class IntentionStatus { SUGGESTED, OFFERED, SET, SKIPPED }

/**
 * One row of the `daily_intention` table: the one thing the user wants to do on [day] ("yyyy-MM-dd", the day counted from 4 am).
 * Stored on the phone only, never sent anywhere.
 */
@Entity(tableName = "daily_intention")
data class IntentionEntity(
    @PrimaryKey val day: String,
    val text: String = "",
    val done: Boolean = false,
    val doneAt: Long? = null,
    val status: String = IntentionStatus.OFFERED.name
)

@Dao
interface IntentionDao {
    @Query("SELECT * FROM daily_intention WHERE day = :day")
    fun observe(day: String): Flow<IntentionEntity?>

    @Query("SELECT * FROM daily_intention WHERE day = :day")
    suspend fun get(day: String): IntentionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: IntentionEntity)

    /** Housekeeping: nothing older than a month is needed. */
    @Query("DELETE FROM daily_intention WHERE day < :before")
    suspend fun deleteBefore(before: String)
}
