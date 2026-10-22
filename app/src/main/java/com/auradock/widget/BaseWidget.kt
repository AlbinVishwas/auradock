package com.auradock.widget

import kotlinx.coroutines.flow.Flow

/**
 * Contract every AuraDock widget must implement.
 *
 * Widgets are data-only at this layer — no UI or Compose references.
 * Each widget emits [WidgetData] via [dataFlow] and respects the standby lifecycle.
 */
interface BaseWidget {

    /** Unique reverse-domain identifier, e.g. "com.auradock.widget.clock". */
    val widgetId: String

    /** Human-readable name for logging and debug surfaces. */
    val displayName: String

    /**
     * How often this widget's data should be refreshed by [com.auradock.worker.WidgetUpdateWorker].
     * 0 means the widget self-updates continuously via its own [dataFlow].
     */
    val updateIntervalMs: Long

    /** Called when standby becomes STANDBY_ACTIVE. Initialize resources here. */
    suspend fun onActivate()

    /**
     * Returns a continuous stream of widget data.
     * The flow is collected by [WidgetManager] and dispatched to the ViewModel layer.
     */
    fun dataFlow(): Flow<WidgetData>

    /** Called on STANDBY_SLEEP. Pause heavy operations but retain state. */
    suspend fun onSuspend()

    /** Called on IDLE or CHARGING (without dock). Release all resources. */
    suspend fun onDeactivate()
}
