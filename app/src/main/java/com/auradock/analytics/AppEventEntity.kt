package com.auradock.analytics

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query

/**
 * One thing that happened in the app, kept on the phone only: what ([name], see [AnalyticsEvents]), when ([at]) and at most one short
 * value ([detail]: a step, a permission, a source, a template or a duration in seconds). Never any content: no task titles, no
 * intentions, no contacts, no notification text.
 */
@Entity(tableName = "app_event")
data class AppEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val at: Long,
    val detail: String? = null
)

@Dao
interface AppEventDao {
    @Insert
    suspend fun insert(event: AppEventEntity)

    @Query("SELECT * FROM app_event ORDER BY at ASC, id ASC")
    suspend fun all(): List<AppEventEntity>
}
