package com.auradock.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.auradock.analytics.AppEventDao
import com.auradock.analytics.AppEventEntity
import com.auradock.intention.IntentionDao
import com.auradock.intention.IntentionEntity
import com.auradock.sessions.FocusSessionDao
import com.auradock.sessions.FocusSessionEntity
import com.auradock.sessions.SessionCheckpointEntity

/**
 * `auradock_local.db`: what happened on this phone. Never backed up and never moved to another phone: focus sessions, the open
 * session's checkpoint, the morning intention and the local event log. (The Do Not Disturb and VIP restore state and the weather
 * cache are also local-only; they stay in their own no-backup preference files, see docs/SECURITY_REVIEW.md.)
 */
@Database(
    entities = [FocusSessionEntity::class, SessionCheckpointEntity::class, IntentionEntity::class, AppEventEntity::class],
    version = 1,
    exportSchema = false
)
abstract class LocalDatabase : RoomDatabase() {
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun intentionDao(): IntentionDao
    abstract fun appEventDao(): AppEventDao
}
