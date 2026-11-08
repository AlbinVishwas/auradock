package com.auradock.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.auradock.sessions.FocusSessionDao
import com.auradock.sessions.FocusSessionEntity
import com.auradock.tasks.TaskDao
import com.auradock.tasks.TaskEntity

@Database(entities = [TaskEntity::class, FocusSessionEntity::class], version = 2, exportSchema = false)
abstract class AuraDockDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun focusSessionDao(): FocusSessionDao
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
