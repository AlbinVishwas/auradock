package com.auradock.dock

import com.auradock.data.CalendarEvent
import com.auradock.tasks.TaskEntity
import com.auradock.weather.Weather

/**
 * Everything [DockContent] needs to draw a template. The live dock builds it from [DockData]; the dashboard thumbnail
 * and the gallery build it from the same data, so they all show the same thing.
 */
data class DockUiState(
    val timeMillis: Long,
    /** Null means "no weather": shown as a quiet dash, never as made-up numbers. */
    val weather: Weather? = null,
    /** Today's calendar events (empty if there are none or calendar access is off). */
    val events: List<CalendarEvent> = emptyList(),
    /** Today's tasks, as the task list holds them (finished ones included). */
    val tasks: List<TaskEntity> = emptyList()
)
