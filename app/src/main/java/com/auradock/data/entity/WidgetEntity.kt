package com.auradock.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "widgets")
data class WidgetEntity(
    @PrimaryKey val widgetId: String,
    val displayName: String,
    val isEnabled: Boolean = true,
    val lastUpdated: Long = 0L,
    val cachedData: String = ""   // Last known JSON-encoded WidgetData
)
