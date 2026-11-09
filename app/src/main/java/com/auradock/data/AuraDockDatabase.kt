package com.auradock.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.auradock.intention.IntentionDao
import com.auradock.intention.IntentionEntity
import com.auradock.sessions.FocusSessionDao
import com.auradock.sessions.FocusSessionEntity
import com.auradock.sessions.SessionCheckpointEntity
import com.auradock.tasks.TaskDao
import com.auradock.tasks.TaskEntity

@Database(entities = [TaskEntity::class, FocusSessionEntity::class, SessionCheckpointEntity::class, IntentionEntity::class], version = 5, exportSchema = false)
abstract class AuraDockDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun intentionDao(): IntentionDao
}

/** 0.2.0: adds the focus sessions table. Tasks are untouched. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `focus_sessions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`startedAt` INTEGER NOT NULL, `endedAt` INTEGER NOT NULL, `source` TEXT NOT NULL, " +
                "`notificationsHeld` INTEGER NOT NULL, `vipThrough` INTEGER NOT NULL, `tasksCompleted` INTEGER NOT NULL)"
        )
    }
}

/** 0.2.1: the open session is checkpointed every minute. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `session_checkpoint` (`id` INTEGER PRIMARY KEY NOT NULL, `startedAt` INTEGER NOT NULL, " +
                "`lastAt` INTEGER NOT NULL, `source` TEXT NOT NULL, `notificationsHeld` INTEGER NOT NULL, `vipThrough` INTEGER NOT NULL)"
        )
    }
}

/** 0.2.1: the morning intention. */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `daily_intention` (`day` TEXT NOT NULL, `text` TEXT NOT NULL, `done` INTEGER NOT NULL, " +
                "`doneAt` INTEGER, `status` TEXT NOT NULL, PRIMARY KEY(`day`))"
        )
    }
}

/** 0.2.1: a task can wait for a later day (tomorrow's top task from the evening shutdown). */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `tasks` ADD COLUMN `showFrom` INTEGER NOT NULL DEFAULT 0")
    }
}
