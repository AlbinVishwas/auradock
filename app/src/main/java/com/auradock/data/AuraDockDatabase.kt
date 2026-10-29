package com.auradock.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.auradock.tasks.TaskDao
import com.auradock.tasks.TaskEntity

@Database(entities = [TaskEntity::class], version = 1, exportSchema = false)
abstract class AuraDockDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
}
