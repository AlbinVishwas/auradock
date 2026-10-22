package com.auradock.widget

/**
 * Sealed class hierarchy for all widget data payloads.
 *
 * UI layer (Compose) will consume these. At this stage they are pure data
 * with no rendering concern. New widget types extend this sealed class.
 */
sealed class WidgetData {

    data class Clock(
        val timeMillis: Long,
        val timezone: String
    ) : WidgetData()

    data class Weather(
        val tempCelsius: Float,
        val condition: String,
        val iconCode: String
    ) : WidgetData()

    data class Timer(
        val remainingMs: Long,
        val label: String
    ) : WidgetData()

    /** Emitted when a widget encounters a non-fatal fetch error. */
    data class Error(
        val widgetId: String,
        val message: String
    ) : WidgetData()
}
