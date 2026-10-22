package com.auradock.widget.impl

import android.util.Log
import com.auradock.widget.BaseWidget
import com.auradock.widget.WidgetData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Weather widget stub.
 * Data is cached in [_data] and refreshed by [com.auradock.worker.WidgetUpdateWorker]
 * every [updateIntervalMs] milliseconds via WorkManager.
 * Real API integration (e.g. OpenWeatherMap) is wired in the [refresh] function.
 */
class WeatherWidget @Inject constructor() : BaseWidget {

    override val widgetId         = "com.auradock.widget.weather"
    override val displayName      = "Weather"
    override val updateIntervalMs = 15 * 60 * 1_000L  // 15 minutes

    private val _data = MutableStateFlow<WidgetData>(
        WidgetData.Weather(tempCelsius = 0f, condition = "Loading…", iconCode = "01d")
    )

    override suspend fun onActivate() {
        Log.d(TAG, "[WeatherWidget] Initialized — update interval: ${updateIntervalMs / 1000 / 60}min, first refresh queued via WorkManager")
    }

    override fun dataFlow(): Flow<WidgetData> = _data.asStateFlow()

    override suspend fun onSuspend()    { Log.d(TAG, "[WeatherWidget] Suspended") }
    override suspend fun onDeactivate() { Log.d(TAG, "[WeatherWidget] Deactivated") }

    /** Called by [com.auradock.worker.WidgetUpdateWorker] on each work cycle. */
    suspend fun refresh() {
        Log.d(TAG, "[WeatherWidget] Refreshing weather data (WorkManager cycle triggered)")
        // TODO: Fetch from weather API and emit to _data
        Log.d(TAG, "[WeatherWidget] Refresh complete — stub, replace with real API call")
    }

    companion object { private const val TAG = "AuraDock" }
}
