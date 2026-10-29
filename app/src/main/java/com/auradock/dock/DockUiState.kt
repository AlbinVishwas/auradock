package com.auradock.dock

import com.auradock.weather.Weather

/**
 * Everything [DockContent] needs to draw a template. The live dock builds it from [DockData]; the dashboard thumbnail
 * and the gallery build it from the same data, so they all show the same thing.
 */
data class DockUiState(
    val timeMillis: Long,
    /** Null means "no weather": shown as a quiet dash, never as made-up numbers. */
    val weather: Weather? = null
)
