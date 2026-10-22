package com.auradock.widget.impl

import android.util.Log
import com.auradock.widget.BaseWidget
import com.auradock.widget.WidgetData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Countdown timer widget.
 * Emits remaining time via [dataFlow]; timer control (start/pause/reset)
 * will be triggered by gesture events in a future iteration.
 */
class TimerWidget @Inject constructor() : BaseWidget {

    override val widgetId         = "com.auradock.widget.timer"
    override val displayName      = "Timer"
    override val updateIntervalMs = 1_000L

    private val _data = MutableStateFlow<WidgetData>(
        WidgetData.Timer(remainingMs = 0L, label = "Timer")
    )

    override suspend fun onActivate() {
        Log.d(TAG, "[TimerWidget] Initialized — update interval: ${updateIntervalMs}ms, timer ready")
    }

    override fun dataFlow(): Flow<WidgetData> = _data.asStateFlow()

    override suspend fun onSuspend() {
        Log.d(TAG, "[TimerWidget] Suspended — timer state preserved")
    }

    override suspend fun onDeactivate() {
        _data.value = WidgetData.Timer(remainingMs = 0L, label = "Timer")
        Log.d(TAG, "[TimerWidget] Deactivated — timer state reset")
    }

    companion object { private const val TAG = "AuraDock" }
}
