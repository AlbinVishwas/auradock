package com.auradock.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.auradock.tasks.TaskDao
import com.auradock.tasks.TaskEntity

/**
 * `auradock.db`: the data worth backing up (tasks). Everything about how the phone was used (focus sessions, the open-session
 * checkpoint, the morning intention, the event log) lives in [LocalDatabase] and is never backed up.
 */
@Database(entities = [TaskEntity::class], version = 8, exportSchema = false)
abstract class AuraDockDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
}

/**
 * 0.3.2: sessions, checkpoint, intention and event log move to [LocalDatabase]. This step changes nothing in the file: the old
 * tables stay until [DatabaseSplit] has copied them and checked the copy, and only then drops them. (Dropping here would lose
 * them if the copy failed.)
 */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) = Unit
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

/** 0.2.1: when a task was last touched, for the resume prompt. Existing tasks count as touched when they were made. */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `tasks` ADD COLUMN `touchedAt` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("UPDATE `tasks` SET `touchedAt` = `createdAt`")
    }
}

/** 0.2.2: the local event log (what happened and when, no content). */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `app_event` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, " +
                "`at` INTEGER NOT NULL, `detail` TEXT)"
        )
    }
}
