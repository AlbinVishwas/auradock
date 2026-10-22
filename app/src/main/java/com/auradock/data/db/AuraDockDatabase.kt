package com.auradock.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.auradock.data.entity.SessionEntity
import com.auradock.data.entity.WidgetEntity

@Database(
    entities  = [WidgetEntity::class, SessionEntity::class],
    version   = 1,
    exportSchema = false
)
abstract class AuraDockDatabase : RoomDatabase() {
    abstract fun widgetDao(): WidgetDao
    abstract fun sessionDao(): SessionDao
}
